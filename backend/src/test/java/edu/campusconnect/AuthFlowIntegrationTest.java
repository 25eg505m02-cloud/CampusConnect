package edu.campusconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import edu.campusconnect.dto.ApiDtos.AuthResponse;
import edu.campusconnect.dto.ApiDtos.LoginRequest;
import edu.campusconnect.dto.ApiDtos.PostRequest;
import edu.campusconnect.dto.ApiDtos.RegisterRequest;
import edu.campusconnect.entity.Role;
import edu.campusconnect.entity.User;
import edu.campusconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:campusconnect;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "campus-connect.jwt.secret=integration-test-secret-that-is-over-32-bytes-long"
})
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository users;

    @Test
    void registerLoginAndReadProtectedProfile() throws Exception {
        RegisterRequest registration = new RegisterRequest(
                "Test Student", "student@example.edu", "correct-horse-battery");
        MvcResult registrationResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn();

        AuthResponse created = objectMapper.readValue(
                registrationResult.getResponse().getContentAsString(), AuthResponse.class);
        assertThat(created.token()).isNotBlank();

        LoginRequest credentials = new LoginRequest("student@example.edu", "correct-horse-battery");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + created.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@example.edu"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("student@example.edu", "wrong-password"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + created.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Student event","description":"Not allowed","venue":"Campus",
                                 "startAt":"2027-02-01T10:00:00Z","endAt":"2027-02-01T11:00:00Z"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void groupMembershipAndEventCapacityAreEnforced() throws Exception {
        AuthResponse creator = register("creator");
        AuthResponse member = register("member");
        AuthResponse other = register("other");

        MvcResult groupResult = mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", bearer(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Robotics Club","description":"Campus robotics"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long groupId = readBody(groupResult).get("id").asLong();

        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PostRequest("Before joining", groupId))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", bearer(member)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/groups/" + groupId + "/members")
                        .header("Authorization", bearer(member)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PostRequest("After joining", groupId))))
                .andExpect(status().isCreated());

        AuthResponse facultyAccount = register("faculty");
        User faculty = users.findByEmailIgnoreCase(facultyAccount.user().email()).orElseThrow();
        faculty.setRole(Role.FACULTY);
        users.saveAndFlush(faculty);
        AuthResponse facultyAuth = login(facultyAccount.user().email());
        MvcResult eventResult = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", bearer(facultyAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Workshop","description":"Intro session","venue":"Room 10",
                                 "startAt":"2027-02-01T10:00:00Z","endAt":"2027-02-01T11:00:00Z","capacity":1}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long eventId = readBody(eventResult).get("id").asLong();

        mockMvc.perform(post("/api/v1/events/" + eventId + "/register")
                        .header("Authorization", bearer(member)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/events/" + eventId + "/register")
                        .header("Authorization", bearer(member)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/events/" + eventId + "/register")
                        .header("Authorization", bearer(other)))
                .andExpect(status().isConflict());
    }

    @Test
    void conversationMessagesAreVisibleOnlyToMembers() throws Exception {
        AuthResponse sender = register("sender");
        AuthResponse recipient = register("recipient");
        AuthResponse outsider = register("outsider");

        long senderId = sender.user().id();
        long recipientId = recipient.user().id();
        mockMvc.perform(post("/api/v1/users/" + recipientId + "/connections")
                        .header("Authorization", bearer(sender)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/users/" + senderId + "/connections")
                        .header("Authorization", bearer(recipient)))
                .andExpect(status().isNoContent());

        MvcResult conversationResult = mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", bearer(sender))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participantIds\":[" + recipientId + "]}"))
                .andExpect(status().isCreated())
                .andReturn();
        long conversationId = readBody(conversationResult).get("id").asLong();

        mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(sender))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello from a member\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(recipient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Hello from a member"));
        mockMvc.perform(get("/api/v1/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(outsider)))
                .andExpect(status().isForbidden());
    }

    private AuthResponse register(String label) throws Exception {
        String email = label + "-" + UUID.randomUUID() + "@example.edu";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Test " + label, email, "correct-horse-battery"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    private AuthResponse login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, "correct-horse-battery"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    private String bearer(AuthResponse auth) {
        return "Bearer " + auth.token();
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
