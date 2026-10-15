package edu.campusconnect;

import edu.campusconnect.config.JsonSeedLoader;
import edu.campusconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:campusconnect-seed;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "campus-connect.jwt.secret=integration-test-secret-that-is-over-32-bytes-long",
        "campus-connect.seed.enabled=true",
        "campus-connect.seed.directory=../collections"
})
class JsonSeedLoaderIntegrationTest {
    @Autowired
    private JsonSeedLoader seedLoader;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Test
    void loadsCollectionsHashesPasswordsAndSkipsExistingIdsOnRepeatRun() throws Exception {
        assertThat(users.count()).isEqualTo(15);
        var seededUser = users.findByEmailIgnoreCase("aarav@mail.com").orElseThrow();
        assertThat(seededUser.getPasswordHash()).isNotEqualTo("12345678");
        assertThat(passwordEncoder.matches("12345678", seededUser.getPasswordHash())).isTrue();
        assertThat(count("Event")).isEqualTo(15);
        assertThat(count("Conversation")).isEqualTo(15);
        assertThat(count("Post")).isEqualTo(15);
        assertThat(count("CampusGroup")).isEqualTo(15);
        assertThat(count("Comment")).isEqualTo(15);
        assertThat(count("EventRegistration")).isEqualTo(15);
        assertThat(count("GroupMembership")).isEqualTo(15);
        assertThat(count("PostLike")).isEqualTo(15);
        assertThat(count("UserConnection")).isEqualTo(15);
        assertThat(count("ConversationMember")).isEqualTo(30);
        assertThat(count("Message")).isEqualTo(15);
        assertThat(count("Notification")).isEqualTo(15);
        assertThat(count("Notice")).isEqualTo(15);
        assertThat(count("StudyResource")).isEqualTo(15);
        assertThat(count("ContentReport")).isEqualTo(15);
        assertThat(count("AdminAudit")).isEqualTo(15);

        seedLoader.run(new DefaultApplicationArguments(new String[0]));

        assertThat(users.count()).isEqualTo(15);
        assertThat(count("Post")).isEqualTo(15);
        assertThat(count("ConversationMember")).isEqualTo(30);
    }

    private long count(String entityName) {
        return entityManager.createQuery("select count(e) from " + entityName + " e", Long.class)
                .getSingleResult();
    }
}
