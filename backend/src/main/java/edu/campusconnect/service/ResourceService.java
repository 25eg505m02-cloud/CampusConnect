package edu.campusconnect.service;

import edu.campusconnect.dto.ApiDtos.ResourceResponse;
import edu.campusconnect.entity.Role;
import edu.campusconnect.entity.StudyResource;
import edu.campusconnect.entity.User;
import edu.campusconnect.exception.ApiException;
import edu.campusconnect.repository.StudyResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ResourceService {
    private static final Logger log = LoggerFactory.getLogger(ResourceService.class);
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            ".pdf", "application/pdf",
            ".txt", "text/plain",
            ".md", "text/markdown",
            ".csv", "text/csv",
            ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final StudyResourceRepository resources;
    private final EntityLookup lookup;
    private final Path storageDirectory;

    public ResourceService(StudyResourceRepository resources, EntityLookup lookup,
                           @Value("${campus-connect.resources.storage-directory:./uploads}") String storageDirectory) {
        this.resources = resources;
        this.lookup = lookup;
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public Page<ResourceResponse> list(String query, Pageable pageable) {
        String term = query == null ? "" : query.trim();
        Page<StudyResource> page = term.isEmpty()
                ? resources.findAllByOrderByCreatedAtDesc(pageable)
                : resources.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByCreatedAtDesc(
                        term, term, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ResourceResponse get(Long id) {
        return toResponse(lookup.resource(id));
    }

    @Transactional
    public ResourceResponse upload(String email, String title, String description, MultipartFile file) {
        User uploader = lookup.userByEmail(email);
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("A non-empty file is required.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw ApiException.badRequest("File size must not exceed 10 MB.");
        }
        String fileName = safeFileName(file.getOriginalFilename());
        String extension = extensionOf(fileName);
        String contentType = ALLOWED_TYPES.get(extension);
        if (contentType == null) {
            throw ApiException.badRequest("Allowed file types are PDF, TXT, MD, CSV, DOCX, PPTX, and XLSX.");
        }
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException exception) {
            throw ApiException.badRequest("The uploaded file could not be read.");
        }
        validateFileContent(extension, fileBytes);
        if (title == null || title.isBlank() || title.length() > 200) {
            throw ApiException.badRequest("Title is required and must be at most 200 characters.");
        }
        if (description != null && description.length() > 1000) {
            throw ApiException.badRequest("Description must be at most 1000 characters.");
        }

        String storageKey = UUID.randomUUID().toString();
        Path storedFile = storageDirectory.resolve(storageKey).normalize();
        if (!storedFile.getParent().equals(storageDirectory)) {
            throw ApiException.badRequest("Invalid upload path.");
        }
        try {
            Files.createDirectories(storageDirectory);
            try (var output = Files.newOutputStream(storedFile, StandardOpenOption.CREATE_NEW)) {
                output.write(fileBytes);
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        deleteStoredFile(storedFile, "incomplete uploaded resource");
                    }
                }
            });
            StudyResource resource = new StudyResource();
            resource.setTitle(title.trim());
            resource.setDescription(description == null ? null : description.trim());
            resource.setOriginalFileName(fileName);
            resource.setContentType(contentType);
            resource.setFileSize(file.getSize());
            resource.setStorageKey(storageKey);
            resource.setUploader(uploader);
            return toResponse(resources.save(resource));
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(storedFile);
            } catch (IOException cleanupException) {
                log.error("Could not remove incomplete uploaded resource file.", cleanupException);
            }
            log.error("Could not store uploaded study resource.", exception);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "The resource could not be stored.");
        } catch (RuntimeException exception) {
            try {
                Files.deleteIfExists(storedFile);
            } catch (IOException cleanupException) {
                log.error("Could not remove incomplete uploaded resource file.", cleanupException);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Download download(Long id) {
        StudyResource resource = lookup.resource(id);
        Path storedFile = storageDirectory.resolve(resource.getStorageKey()).normalize();
        if (!storedFile.getParent().equals(storageDirectory)) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Resource storage metadata is invalid.");
        }
        try {
            return new Download(resource.getOriginalFileName(), resource.getContentType(),
                    Files.readAllBytes(storedFile));
        } catch (IOException exception) {
            log.error("Could not read stored study resource {}.", id, exception);
            throw ApiException.notFound("The resource file is unavailable.");
        }
    }

    @Transactional
    public void delete(String email, Long id) {
        User user = lookup.userByEmail(email);
        StudyResource resource = lookup.resource(id);
        if (!resource.getUploader().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("You can only delete resources you uploaded.");
        }
        Path storedFile = storageDirectory.resolve(resource.getStorageKey()).normalize();
        resources.delete(resource);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteStoredFile(storedFile, "study resource " + id);
            }
        });
    }

    private String safeFileName(String original) {
        if (original == null || original.isBlank()) {
            throw ApiException.badRequest("File name is required.");
        }
        String normalized = original.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (name.isBlank() || name.length() > 255) {
            throw ApiException.badRequest("File name must be between 1 and 255 characters.");
        }
        return name;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot).toLowerCase(Locale.ROOT);
    }

    private void validateFileContent(String extension, byte[] content) {
        if (extension.equals(".pdf")) {
            if (content.length < 5 || content[0] != '%' || content[1] != 'P'
                    || content[2] != 'D' || content[3] != 'F' || content[4] != '-') {
                throw ApiException.badRequest("The uploaded file is not a valid PDF.");
            }
            return;
        }
        if (extension.equals(".docx") || extension.equals(".pptx") || extension.equals(".xlsx")) {
            if (content.length < 4 || content[0] != 'P' || content[1] != 'K'
                    || content[2] != 3 || content[3] != 4) {
                throw ApiException.badRequest("The uploaded office document has an invalid file signature.");
            }
            return;
        }
        try {
            for (byte value : content) {
                if (value == 0) {
                    throw ApiException.badRequest("Text resources must contain valid UTF-8 text.");
                }
            }
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content));
        } catch (CharacterCodingException exception) {
            throw ApiException.badRequest("Text resources must contain valid UTF-8 text.");
        }
    }

    private void deleteStoredFile(Path path, String description) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.error("Could not delete stored file for {}.", description, exception);
        }
    }

    private ResourceResponse toResponse(StudyResource resource) {
        return new ResourceResponse(resource.getId(), resource.getTitle(), resource.getDescription(),
                resource.getOriginalFileName(), resource.getContentType(), resource.getFileSize(),
                resource.getUploader().getId(), resource.getUploader().getFullName(), resource.getCreatedAt());
    }

    public record Download(String fileName, String contentType, byte[] content) {
    }
}
