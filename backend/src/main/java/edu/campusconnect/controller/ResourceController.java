package edu.campusconnect.controller;

import edu.campusconnect.dto.ApiDtos.ResourceResponse;
import edu.campusconnect.service.ResourceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {
    private final ResourceService resources;

    public ResourceController(ResourceService resources) {
        this.resources = resources;
    }

    @GetMapping
    public Page<ResourceResponse> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return resources.list(q, pageable);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceResponse upload(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file) {
        return resources.upload(currentUser.getUsername(), title, description, file);
    }

    @GetMapping("/{id}")
    public ResourceResponse get(@PathVariable Long id) {
        return resources.get(id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        ResourceService.Download download = resources.download(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(download.contentType()));
        headers.setContentLength(download.content().length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build());
        headers.set("X-Content-Type-Options", "nosniff");
        return new ResponseEntity<>(download.content(), headers, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails currentUser, @PathVariable Long id) {
        resources.delete(currentUser.getUsername(), id);
    }
}
