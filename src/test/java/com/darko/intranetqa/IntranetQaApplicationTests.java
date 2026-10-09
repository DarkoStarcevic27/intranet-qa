package com.darko.intranetqa;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the real application context against a throwaway Postgres+pgvector
 * container. The chat/embedding model beans are mocked out so the test
 * suite never makes a real network call or needs a real API key — what's
 * under test here is the wiring (security, persistence, vector store
 * autoconfiguration), not model output quality, which belongs in a
 * separate evaluation set.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class IntranetQaApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockitoBean
    ChatModel chatModel;

    @MockitoBean
    EmbeddingModel embeddingModel;

    @org.springframework.beans.factory.annotation.Autowired
    MockMvc mockMvc;

    @Test
    void contextLoads() {
        // Fails fast on any wiring problem (security filter chain,
        // Flyway migration, PgVectorStore autoconfiguration, MCP server
        // bean) before we ever try it against a real model.
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void chatEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType("application/json")
                        .content("{\"question\":\"What is our leave policy?\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/documents"))
                .andExpect(status().isUnauthorized());
    }
}
