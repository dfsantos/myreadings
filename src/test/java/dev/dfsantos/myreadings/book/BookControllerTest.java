package dev.dfsantos.myreadings.book;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookControllerTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + tempDir.resolve("book-controller-test.db"));
    }

    @Test
    void createWithAllFieldsReturns201WithGeneratedIdAndPersistedValues() throws Exception {
        String accessToken = registerAndLogin("leitor-livros@example.com", "senha-forte-123");

        MvcResult result = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Duna",
                                  "author": "Frank Herbert",
                                  "publisher": "Aleph",
                                  "publicationYear": 1965,
                                  "pageCount": 688,
                                  "genre": "Ficção científica",
                                  "coverUrl": "https://exemplo.com/duna.jpg",
                                  "status": "LENDO",
                                  "startDate": "2026-09-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title").value("Duna"))
                .andExpect(jsonPath("$.author").value("Frank Herbert"))
                .andExpect(jsonPath("$.publisher").value("Aleph"))
                .andExpect(jsonPath("$.publicationYear").value(1965))
                .andExpect(jsonPath("$.pageCount").value(688))
                .andExpect(jsonPath("$.genre").value("Ficção científica"))
                .andExpect(jsonPath("$.coverUrl").value("https://exemplo.com/duna.jpg"))
                .andExpect(jsonPath("$.status").value("LENDO"))
                .andExpect(jsonPath("$.startDate").value("2026-09-01"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(body.get("id").asString());

        Optional<Book> persisted = bookRepository.findById(bookId);
        assertTrue(persisted.isPresent());
        assertEquals("https://exemplo.com/duna.jpg", persisted.get().getCoverUrl());
    }

    @Test
    void createWithoutStatusDefaultsToQueroLer() throws Exception {
        String accessToken = registerAndLogin("sem-status@example.com", "senha-forte-123");

        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "1984",
                                  "author": "George Orwell"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("1984"))
                .andExpect(jsonPath("$.author").value("George Orwell"))
                .andExpect(jsonPath("$.status").value("QUERO_LER"))
                .andExpect(jsonPath("$.publisher").value(nullValue()))
                .andExpect(jsonPath("$.publicationYear").value(nullValue()))
                .andExpect(jsonPath("$.pageCount").value(nullValue()))
                .andExpect(jsonPath("$.genre").value(nullValue()))
                .andExpect(jsonPath("$.coverUrl").value(nullValue()))
                .andExpect(jsonPath("$.startDate").value(nullValue()))
                .andExpect(jsonPath("$.endDate").value(nullValue()))
                .andExpect(jsonPath("$.rating").value(nullValue()));
    }

    @Test
    void createWithTitleAuthorAndCoverUrlReturns201WithCoverUrlAndRemainingFieldsNull() throws Exception {
        String accessToken = registerAndLogin("com-capa@example.com", "senha-forte-123");

        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Fahrenheit 451",
                                  "author": "Ray Bradbury",
                                  "coverUrl": "https://exemplo.com/fahrenheit451.jpg"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Fahrenheit 451"))
                .andExpect(jsonPath("$.author").value("Ray Bradbury"))
                .andExpect(jsonPath("$.coverUrl").value("https://exemplo.com/fahrenheit451.jpg"))
                .andExpect(jsonPath("$.status").value("QUERO_LER"))
                .andExpect(jsonPath("$.publisher").value(nullValue()))
                .andExpect(jsonPath("$.publicationYear").value(nullValue()))
                .andExpect(jsonPath("$.pageCount").value(nullValue()))
                .andExpect(jsonPath("$.genre").value(nullValue()))
                .andExpect(jsonPath("$.startDate").value(nullValue()))
                .andExpect(jsonPath("$.endDate").value(nullValue()))
                .andExpect(jsonPath("$.rating").value(nullValue()));
    }

    @Test
    void createIgnoresUserIdSentInRequestBodyAndUsesAuthenticatedUser() throws Exception {
        RegisteredUser user = register("dono-do-token@example.com", "senha-forte-123");
        String accessToken = login(user.email(), "senha-forte-123");
        UUID arbitraryUserId = UUID.randomUUID();

        MvcResult result = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "O Hobbit",
                                  "author": "J. R. R. Tolkien",
                                  "userId": "%s"
                                }
                                """.formatted(arbitraryUserId)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(body.get("id").asString());

        Book persisted = bookRepository.findById(bookId).orElseThrow();
        assertEquals(user.id(), persisted.getUserId());
        assertNotEquals(arbitraryUserId, persisted.getUserId());
    }

    @Test
    void createWithoutAuthorizationHeaderReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Sem token",
                                  "author": "Alguém"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithoutTitleReturns400() throws Exception {
        String accessToken = registerAndLogin("sem-titulo@example.com", "senha-forte-123");

        MvcResult result = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "author": "Alguém"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertInvalidFields(result, "title");
    }

    @Test
    void createWithoutAuthorReturns400CitingAuthorField() throws Exception {
        String accessToken = registerAndLogin("sem-autor@example.com", "senha-forte-123");

        MvcResult result = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Alguma coisa"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertInvalidFields(result, "author");
    }

    @Test
    void createWithoutTitleAndAuthorReturns400CitingBothFields() throws Exception {
        String accessToken = registerAndLogin("sem-titulo-e-autor@example.com", "senha-forte-123");

        MvcResult result = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertInvalidFields(result, "title", "author");
    }

    @Test
    void patchChangingOnlyStatusReturns200AndPreservesRemainingFields() throws Exception {
        String accessToken = registerAndLogin("atualiza-status@example.com", "senha-forte-123");

        MvcResult createResult = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Duna",
                                  "author": "Frank Herbert",
                                  "publisher": "Aleph",
                                  "publicationYear": 1965,
                                  "pageCount": 688,
                                  "genre": "Ficção científica",
                                  "coverUrl": "https://exemplo.com/duna.jpg",
                                  "status": "QUERO_LER",
                                  "startDate": "2026-09-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(createBody.get("id").asString());

        mockMvc.perform(patch("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "LENDO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookId.toString()))
                .andExpect(jsonPath("$.status").value("LENDO"))
                .andExpect(jsonPath("$.title").value("Duna"))
                .andExpect(jsonPath("$.author").value("Frank Herbert"))
                .andExpect(jsonPath("$.publisher").value("Aleph"))
                .andExpect(jsonPath("$.publicationYear").value(1965))
                .andExpect(jsonPath("$.pageCount").value(688))
                .andExpect(jsonPath("$.genre").value("Ficção científica"))
                .andExpect(jsonPath("$.coverUrl").value("https://exemplo.com/duna.jpg"))
                .andExpect(jsonPath("$.startDate").value("2026-09-01"));

        Book persisted = bookRepository.findById(bookId).orElseThrow();
        assertEquals(ReadingStatus.LENDO, persisted.getStatus());
        assertEquals("Duna", persisted.getTitle());
        assertEquals("Aleph", persisted.getPublisher());
    }

    @Test
    void patchWithInvalidStatusValueReturns400() throws Exception {
        String accessToken = registerAndLogin("status-invalido@example.com", "senha-forte-123");

        MvcResult createResult = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "1984",
                                  "author": "George Orwell"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(createBody.get("id").asString());

        mockMvc.perform(patch("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "LENDO_DEMAIS"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchBookOfAnotherUserReturns404() throws Exception {
        String ownerToken = registerAndLogin("dono-do-livro@example.com", "senha-forte-123");
        String otherUserToken = registerAndLogin("outro-usuario@example.com", "senha-forte-123");

        MvcResult createResult = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "O Hobbit",
                                  "author": "J. R. R. Tolkien"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(createBody.get("id").asString());

        mockMvc.perform(patch("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "LENDO"
                                }
                                """))
                .andExpect(status().isNotFound());

        Book persisted = bookRepository.findById(bookId).orElseThrow();
        assertEquals(ReadingStatus.QUERO_LER, persisted.getStatus());
    }

    @Test
    void patchNonExistentBookReturns404() throws Exception {
        String accessToken = registerAndLogin("patch-inexistente@example.com", "senha-forte-123");

        mockMvc.perform(patch("/api/v1/books/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "LENDO"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchWithEndDateBeforeStartDateReturns400() throws Exception {
        String accessToken = registerAndLogin("datas-invertidas@example.com", "senha-forte-123");

        MvcResult createResult = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Duna",
                                  "author": "Frank Herbert"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(createBody.get("id").asString());

        mockMvc.perform(patch("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-09-10",
                                  "endDate": "2026-09-01"
                                }
                                """))
                .andExpect(status().isBadRequest());

        Book persisted = bookRepository.findById(bookId).orElseThrow();
        assertEquals(null, persisted.getStartDate());
        assertEquals(null, persisted.getEndDate());
    }

    @Test
    void patchWithValidStartDateAndEndDateReturns200AndPersistsBothDates() throws Exception {
        String accessToken = registerAndLogin("datas-validas@example.com", "senha-forte-123");

        MvcResult createResult = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Duna",
                                  "author": "Frank Herbert"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(createBody.get("id").asString());

        mockMvc.perform(patch("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-09-01",
                                  "endDate": "2026-09-10"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2026-09-01"))
                .andExpect(jsonPath("$.endDate").value("2026-09-10"));

        Book persisted = bookRepository.findById(bookId).orElseThrow();
        assertEquals(java.time.LocalDate.of(2026, 9, 1), persisted.getStartDate());
        assertEquals(java.time.LocalDate.of(2026, 9, 10), persisted.getEndDate());
    }

    @Test
    void patchWithOnlyStartDateIsAcceptedWhenNoEndDateIsPersisted() throws Exception {
        String accessToken = registerAndLogin("somente-data-inicio@example.com", "senha-forte-123");

        MvcResult createResult = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Duna",
                                  "author": "Frank Herbert"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(createBody.get("id").asString());

        mockMvc.perform(patch("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-09-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2026-09-01"))
                .andExpect(jsonPath("$.endDate").value(nullValue()));

        Book persisted = bookRepository.findById(bookId).orElseThrow();
        assertEquals(java.time.LocalDate.of(2026, 9, 1), persisted.getStartDate());
        assertEquals(null, persisted.getEndDate());
    }

    @Test
    void patchWithOnlyStartDateReturns400WhenItConflictsWithAlreadyPersistedEndDate() throws Exception {
        String accessToken = registerAndLogin("conflito-com-data-persistida@example.com", "senha-forte-123");

        MvcResult createResult = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Duna",
                                  "author": "Frank Herbert",
                                  "startDate": "2026-09-01",
                                  "endDate": "2026-09-10"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(createBody.get("id").asString());

        // Envia só startDate, posterior ao endDate já persistido: o estado final após o
        // merge parcial fica inconsistente mesmo sem o PATCH tocar em endDate.
        mockMvc.perform(patch("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-09-15"
                                }
                                """))
                .andExpect(status().isBadRequest());

        Book persisted = bookRepository.findById(bookId).orElseThrow();
        assertEquals(java.time.LocalDate.of(2026, 9, 1), persisted.getStartDate());
        assertEquals(java.time.LocalDate.of(2026, 9, 10), persisted.getEndDate());
    }

    @Test
    void patchSettingRatingToFiveReturns200AndPersistsRating() throws Exception {
        String accessToken = registerAndLogin("avalia-livro@example.com", "senha-forte-123");

        MvcResult createResult = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Duna",
                                  "author": "Frank Herbert"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(createBody.get("id").asString());

        mockMvc.perform(patch("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));

        Book persisted = bookRepository.findById(bookId).orElseThrow();
        assertEquals(5, persisted.getRating());
    }

    @Test
    void patchSettingRatingAboveFiveReturns400() throws Exception {
        String accessToken = registerAndLogin("avaliacao-acima-do-limite@example.com", "senha-forte-123");

        MvcResult createResult = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Duna",
                                  "author": "Frank Herbert"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(createBody.get("id").asString());

        mockMvc.perform(patch("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 6
                                }
                                """))
                .andExpect(status().isBadRequest());

        Book persisted = bookRepository.findById(bookId).orElseThrow();
        assertEquals(null, persisted.getRating());
    }

    @Test
    void patchSettingRatingBelowOneReturns400() throws Exception {
        String accessToken = registerAndLogin("avaliacao-abaixo-do-limite@example.com", "senha-forte-123");

        MvcResult createResult = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Duna",
                                  "author": "Frank Herbert"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(createBody.get("id").asString());

        mockMvc.perform(patch("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 0
                                }
                                """))
                .andExpect(status().isBadRequest());

        Book persisted = bookRepository.findById(bookId).orElseThrow();
        assertEquals(null, persisted.getRating());
    }

    @Test
    void sequentialPatchesEachChangingOneFieldAccumulateAllChangesWithoutLosingPreviousOnes() throws Exception {
        String accessToken = registerAndLogin("patches-sequenciais@example.com", "senha-forte-123");

        MvcResult createResult = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Duna",
                                  "author": "Frank Herbert",
                                  "status": "QUERO_LER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(createBody.get("id").asString());

        mockMvc.perform(patch("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "LENDO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LENDO"));

        mockMvc.perform(patch("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LENDO"))
                .andExpect(jsonPath("$.rating").value(4));

        mockMvc.perform(patch("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-09-01",
                                  "endDate": "2026-09-10"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LENDO"))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.startDate").value("2026-09-01"))
                .andExpect(jsonPath("$.endDate").value("2026-09-10"));

        Book persisted = bookRepository.findById(bookId).orElseThrow();
        assertEquals(ReadingStatus.LENDO, persisted.getStatus());
        assertEquals(4, persisted.getRating());
        assertEquals(java.time.LocalDate.of(2026, 9, 1), persisted.getStartDate());
        assertEquals(java.time.LocalDate.of(2026, 9, 10), persisted.getEndDate());
        assertEquals("Duna", persisted.getTitle());
        assertEquals("Frank Herbert", persisted.getAuthor());
    }

    @Test
    void patchSendingOnlyRatingPreservesAllOtherPreviouslyPersistedFields() throws Exception {
        String accessToken = registerAndLogin("patch-so-rating@example.com", "senha-forte-123");

        MvcResult createResult = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Duna",
                                  "author": "Frank Herbert",
                                  "publisher": "Aleph",
                                  "publicationYear": 1965,
                                  "pageCount": 688,
                                  "genre": "Ficção científica",
                                  "coverUrl": "https://exemplo.com/duna.jpg",
                                  "status": "LENDO",
                                  "startDate": "2026-09-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(createBody.get("id").asString());

        mockMvc.perform(patch("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 4
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.title").value("Duna"))
                .andExpect(jsonPath("$.author").value("Frank Herbert"))
                .andExpect(jsonPath("$.publisher").value("Aleph"))
                .andExpect(jsonPath("$.publicationYear").value(1965))
                .andExpect(jsonPath("$.pageCount").value(688))
                .andExpect(jsonPath("$.genre").value("Ficção científica"))
                .andExpect(jsonPath("$.coverUrl").value("https://exemplo.com/duna.jpg"))
                .andExpect(jsonPath("$.status").value("LENDO"))
                .andExpect(jsonPath("$.startDate").value("2026-09-01"));

        Book persisted = bookRepository.findById(bookId).orElseThrow();
        assertEquals(4, persisted.getRating());
        assertEquals("Duna", persisted.getTitle());
        assertEquals("Frank Herbert", persisted.getAuthor());
        assertEquals("Aleph", persisted.getPublisher());
        assertEquals(1965, persisted.getPublicationYear());
        assertEquals(688, persisted.getPageCount());
        assertEquals("Ficção científica", persisted.getGenre());
        assertEquals("https://exemplo.com/duna.jpg", persisted.getCoverUrl());
        assertEquals(ReadingStatus.LENDO, persisted.getStatus());
        assertEquals(java.time.LocalDate.of(2026, 9, 1), persisted.getStartDate());
    }

    @Test
    void patchWithEmptyBodyReturns200WithoutChangingAnyFieldButUpdatesUpdatedAt() throws Exception {
        String accessToken = registerAndLogin("patch-vazio@example.com", "senha-forte-123");

        MvcResult createResult = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Duna",
                                  "author": "Frank Herbert",
                                  "publisher": "Aleph",
                                  "publicationYear": 1965,
                                  "pageCount": 688,
                                  "genre": "Ficção científica",
                                  "coverUrl": "https://exemplo.com/duna.jpg",
                                  "status": "LENDO",
                                  "startDate": "2026-09-01",
                                  "rating": 5
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(createBody.get("id").asString());
        Instant createdUpdatedAt = Instant.parse(createBody.get("updatedAt").asString());

        // Garante que o relógio avance o suficiente para o teste de updatedAt ser
        // determinístico mesmo em execuções muito rápidas.
        Thread.sleep(5);

        mockMvc.perform(patch("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Duna"))
                .andExpect(jsonPath("$.author").value("Frank Herbert"))
                .andExpect(jsonPath("$.publisher").value("Aleph"))
                .andExpect(jsonPath("$.publicationYear").value(1965))
                .andExpect(jsonPath("$.pageCount").value(688))
                .andExpect(jsonPath("$.genre").value("Ficção científica"))
                .andExpect(jsonPath("$.coverUrl").value("https://exemplo.com/duna.jpg"))
                .andExpect(jsonPath("$.status").value("LENDO"))
                .andExpect(jsonPath("$.startDate").value("2026-09-01"))
                .andExpect(jsonPath("$.rating").value(5));

        Book persisted = bookRepository.findById(bookId).orElseThrow();
        assertEquals("Duna", persisted.getTitle());
        assertEquals("Frank Herbert", persisted.getAuthor());
        assertEquals("Aleph", persisted.getPublisher());
        assertEquals(1965, persisted.getPublicationYear());
        assertEquals(688, persisted.getPageCount());
        assertEquals("Ficção científica", persisted.getGenre());
        assertEquals("https://exemplo.com/duna.jpg", persisted.getCoverUrl());
        assertEquals(ReadingStatus.LENDO, persisted.getStatus());
        assertEquals(java.time.LocalDate.of(2026, 9, 1), persisted.getStartDate());
        assertEquals(5, persisted.getRating());
        assertTrue(persisted.getUpdatedAt().isAfter(createdUpdatedAt),
                "updatedAt deve avançar mesmo em um PATCH com corpo vazio, pois a semântica "
                        + "é \"nenhum campo de negócio alterado\", não \"nenhuma chamada de update\"");
    }

    @Test
    void listWithoutParametersReturnsAllBooksOfAuthenticatedUserPaginated() throws Exception {
        String accessToken = registerAndLogin("lista-todos-os-livros@example.com", "senha-forte-123");

        createBook(accessToken, "Duna", "Frank Herbert");
        createBook(accessToken, "1984", "George Orwell");
        createBook(accessToken, "O Hobbit", "J. R. R. Tolkien");

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void listDoesNotReturnBooksRegisteredByAnotherUser() throws Exception {
        String ownerToken = registerAndLogin("dono-da-lista@example.com", "senha-forte-123");
        String otherUserToken = registerAndLogin("outro-usuario-na-lista@example.com", "senha-forte-123");

        createBook(ownerToken, "Duna", "Frank Herbert");
        createBook(ownerToken, "1984", "George Orwell");
        createBook(otherUserToken, "O Hobbit", "J. R. R. Tolkien");

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].title").value("1984"))
                .andExpect(jsonPath("$.content[1].title").value("Duna"));
    }

    @Test
    void listRespectsPageAndSizeQueryParameters() throws Exception {
        String accessToken = registerAndLogin("paginacao-de-livros@example.com", "senha-forte-123");

        createBook(accessToken, "Livro 1", "Autor 1");
        createBook(accessToken, "Livro 2", "Autor 2");
        createBook(accessToken, "Livro 3", "Autor 3");

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Livro 3"))
                .andExpect(jsonPath("$.content[1].title").value("Livro 2"));

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Livro 1"));
    }

    @Test
    void listWithQMatchingTitleReturnsOnlyMatchingBook() throws Exception {
        String accessToken = registerAndLogin("busca-por-titulo@example.com", "senha-forte-123");

        createBook(accessToken, "Duna", "Frank Herbert");
        createBook(accessToken, "1984", "George Orwell");

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("q", "Duna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Duna"));
    }

    @Test
    void listWithQMatchingAuthorReturnsOnlyMatchingBook() throws Exception {
        String accessToken = registerAndLogin("busca-por-autor@example.com", "senha-forte-123");

        createBook(accessToken, "Duna", "Frank Herbert");
        createBook(accessToken, "1984", "George Orwell");

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("q", "Orwell"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("1984"));
    }

    @Test
    void listWithQIsCaseInsensitive() throws Exception {
        String accessToken = registerAndLogin("busca-case-insensitive@example.com", "senha-forte-123");

        createBook(accessToken, "Duna", "Frank Herbert");
        createBook(accessToken, "1984", "George Orwell");

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("q", "duna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Duna"));
    }

    @Test
    void listWithStatusLidoReturnsOnlyBooksWithThatStatus() throws Exception {
        String accessToken = registerAndLogin("filtra-por-status@example.com", "senha-forte-123");

        createBookWithStatus(accessToken, "Duna", "Frank Herbert", "LIDO");
        createBookWithStatus(accessToken, "1984", "George Orwell", "LENDO");
        createBookWithStatus(accessToken, "O Hobbit", "J. R. R. Tolkien", "QUERO_LER");

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("status", "LIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Duna"))
                .andExpect(jsonPath("$.content[0].status").value("LIDO"));
    }

    @Test
    void listWithGenreReturnsOnlyBooksOfThatGenre() throws Exception {
        String accessToken = registerAndLogin("filtra-por-genero@example.com", "senha-forte-123");

        createBookWithGenre(accessToken, "Duna", "Frank Herbert", "Ficção científica");
        createBookWithGenre(accessToken, "1984", "George Orwell", "Distopia");

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("genre", "Ficção científica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Duna"))
                .andExpect(jsonPath("$.content[0].genre").value("Ficção científica"));
    }

    @Test
    void listWithGenreIsCaseInsensitive() throws Exception {
        String accessToken = registerAndLogin("genero-case-insensitive@example.com", "senha-forte-123");

        createBookWithGenre(accessToken, "Duna", "Frank Herbert", "Ficção científica");
        createBookWithGenre(accessToken, "1984", "George Orwell", "Distopia");

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("genre", "ficção científica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Duna"))
                .andExpect(jsonPath("$.content[0].genre").value("Ficção científica"));
    }

    @Test
    void listWithMinRatingReturnsOnlyBooksWithRatingGreaterThanOrEqual() throws Exception {
        String accessToken = registerAndLogin("filtra-por-nota-minima@example.com", "senha-forte-123");

        createBookWithRating(accessToken, "Duna", "Frank Herbert", 5);
        createBookWithRating(accessToken, "1984", "George Orwell", 4);
        createBookWithRating(accessToken, "O Hobbit", "J. R. R. Tolkien", 2);

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("minRating", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("1984"))
                .andExpect(jsonPath("$.content[1].title").value("Duna"));
    }

    @Test
    void listWithMaxRatingReturnsOnlyBooksWithRatingLessThanOrEqual() throws Exception {
        String accessToken = registerAndLogin("filtra-por-nota-maxima@example.com", "senha-forte-123");

        createBookWithRating(accessToken, "Duna", "Frank Herbert", 5);
        createBookWithRating(accessToken, "1984", "George Orwell", 3);
        createBookWithRating(accessToken, "O Hobbit", "J. R. R. Tolkien", 2);

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("maxRating", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("O Hobbit"))
                .andExpect(jsonPath("$.content[1].title").value("1984"));
    }

    @Test
    void listWithMinRatingAndMaxRatingCombinedFiltersTheCorrectRange() throws Exception {
        String accessToken = registerAndLogin("filtra-por-faixa-de-nota@example.com", "senha-forte-123");

        createBookWithRating(accessToken, "Duna", "Frank Herbert", 5);
        createBookWithRating(accessToken, "1984", "George Orwell", 4);
        createBookWithRating(accessToken, "O Hobbit", "J. R. R. Tolkien", 2);

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("minRating", "4")
                        .param("maxRating", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("1984"))
                .andExpect(jsonPath("$.content[1].title").value("Duna"));
    }

    @Test
    void listWithMinRatingBelowOneReturns400() throws Exception {
        String accessToken = registerAndLogin("nota-minima-abaixo-do-limite@example.com", "senha-forte-123");

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("minRating", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listWithMaxRatingAboveFiveReturns400() throws Exception {
        String accessToken = registerAndLogin("nota-maxima-acima-do-limite@example.com", "senha-forte-123");

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("maxRating", "6"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listWithInvalidStatusValueReturns400() throws Exception {
        String accessToken = registerAndLogin("status-invalido-na-listagem@example.com", "senha-forte-123");

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("status", "VALOR_INVALIDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listWithGenreAndStatusCombinedAppliesAndBetweenBothCriteria() throws Exception {
        String accessToken = registerAndLogin("combina-genero-e-status@example.com", "senha-forte-123");

        // Cobre as 4 combinações possíveis entre os dois critérios para garantir que o
        // filtro aplica AND (apenas o livro que satisfaz ambos) e não OR (que também
        // retornaria livros que satisfazem só um dos dois).
        createBookWithGenreAndStatus(accessToken, "Duna", "Frank Herbert", "Ficção científica", "LIDO");
        createBookWithGenreAndStatus(accessToken, "1984", "George Orwell", "Distopia", "LIDO");
        createBookWithGenreAndStatus(accessToken, "O Hobbit", "J. R. R. Tolkien", "Ficção científica", "QUERO_LER");
        createBookWithGenreAndStatus(accessToken, "Admirável Mundo Novo", "Aldous Huxley", "Distopia", "QUERO_LER");

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("genre", "Ficção científica")
                        .param("status", "LIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Duna"))
                .andExpect(jsonPath("$.content[0].genre").value("Ficção científica"))
                .andExpect(jsonPath("$.content[0].status").value("LIDO"));
    }

    @Test
    void listWithFourFiltersCombinedReturnsOnlyTheSubsetMatchingAllOfThem() throws Exception {
        String accessToken = registerAndLogin("combina-quatro-filtros@example.com", "senha-forte-123");

        // Satisfaz os 4 filtros abaixo (q=Duna, status=LIDO, genre=Ficção científica,
        // minRating=4).
        createBookWithGenreStatusAndRating(
                accessToken, "Duna", "Frank Herbert", "Ficção científica", "LIDO", 5);
        // Satisfaz q (substring "Duna" no título) e genre/status, mas falha minRating.
        createBookWithGenreStatusAndRating(
                accessToken, "Duna Mensageiro", "Frank Herbert", "Ficção científica", "LIDO", 3);
        // Satisfaz q, status e minRating, mas falha genre.
        createBookWithGenreStatusAndRating(
                accessToken, "Duna", "Frank Herbert", "Distopia", "LIDO", 5);
        // Satisfaz q, genre e minRating, mas falha status.
        createBookWithGenreStatusAndRating(
                accessToken, "Duna", "Frank Herbert", "Ficção científica", "QUERO_LER", 5);
        // Satisfaz status, genre e minRating, mas falha q.
        createBookWithGenreStatusAndRating(
                accessToken, "1984", "George Orwell", "Ficção científica", "LIDO", 5);

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("q", "Duna")
                        .param("status", "LIDO")
                        .param("genre", "Ficção científica")
                        .param("minRating", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Duna"))
                .andExpect(jsonPath("$.content[0].author").value("Frank Herbert"))
                .andExpect(jsonPath("$.content[0].genre").value("Ficção científica"))
                .andExpect(jsonPath("$.content[0].status").value("LIDO"))
                .andExpect(jsonPath("$.content[0].rating").value(5));
    }

    @Test
    void listWithoutAnyFilterIgnoresAllOfThemAndBehavesLikeSimpleListing() throws Exception {
        String accessToken = registerAndLogin("sem-filtro-comporta-como-lista-simples@example.com", "senha-forte-123");

        // Atributos deliberadamente variados (gênero, status e rating distintos) para
        // garantir que a ausência de filtros não restringe o resultado por nenhum deles.
        createBookWithGenreStatusAndRating(
                accessToken, "Duna", "Frank Herbert", "Ficção científica", "LIDO", 5);
        createBookWithGenreStatusAndRating(
                accessToken, "1984", "George Orwell", "Distopia", "QUERO_LER", 2);
        createBookWithGenreStatusAndRating(
                accessToken, "O Hobbit", "J. R. R. Tolkien", "Fantasia", "LENDO", null);

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void listWithQNotMatchingAnyBookReturns200WithEmptyContentAndZeroTotalElements() throws Exception {
        String accessToken = registerAndLogin("busca-sem-resultado@example.com", "senha-forte-123");

        // Cria pelo menos um livro para garantir que o resultado vazio é efeito do filtro
        // "q", não de ausência total de dados para o usuário.
        createBook(accessToken, "Duna", "Frank Herbert");

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("q", "termo-inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listWithValidFilterCombinationMatchingNoBookReturns200WithEmptyContent() throws Exception {
        String accessToken = registerAndLogin("combinacao-sem-resultado@example.com", "senha-forte-123");

        // Nenhum dos livros cadastrados satisfaz status=ABANDONADO combinado com
        // minRating=5: a combinação é válida, mas o conjunto resultante é vazio.
        createBookWithStatus(accessToken, "Duna", "Frank Herbert", "LIDO");
        createBookWithRating(accessToken, "1984", "George Orwell", 2);

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("status", "ABANDONADO")
                        .param("minRating", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listWithoutAuthorizationHeaderReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOwnBookReturns200WithBookData() throws Exception {
        String accessToken = registerAndLogin("busca-livro-por-id@example.com", "senha-forte-123");
        UUID bookId = createBook(accessToken, "Duna", "Frank Herbert");

        mockMvc.perform(get("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookId.toString()))
                .andExpect(jsonPath("$.title").value("Duna"))
                .andExpect(jsonPath("$.author").value("Frank Herbert"));
    }

    @Test
    void getBookOfAnotherUserReturns404() throws Exception {
        String ownerToken = registerAndLogin("dono-do-livro-get@example.com", "senha-forte-123");
        String otherUserToken = registerAndLogin("outro-usuario-get@example.com", "senha-forte-123");
        UUID bookId = createBook(ownerToken, "O Hobbit", "J. R. R. Tolkien");

        mockMvc.perform(get("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getNonExistentBookReturns404() throws Exception {
        String accessToken = registerAndLogin("get-inexistente@example.com", "senha-forte-123");

        mockMvc.perform(get("/api/v1/books/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOwnBookReturns204AndGetSubsequentReturns404() throws Exception {
        String accessToken = registerAndLogin("remove-livro-proprio@example.com", "senha-forte-123");
        UUID bookId = createBook(accessToken, "Duna", "Frank Herbert");

        mockMvc.perform(delete("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());

        assertTrue(bookRepository.findById(bookId).isEmpty());
    }

    @Test
    void deleteRemovesBookFromSubsequentListing() throws Exception {
        String accessToken = registerAndLogin("remocao-some-da-lista@example.com", "senha-forte-123");
        UUID bookId = createBook(accessToken, "Duna", "Frank Herbert");
        createBook(accessToken, "1984", "George Orwell");

        mockMvc.perform(delete("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("1984"));
    }

    @Test
    void deleteBookOfAnotherUserReturns404AndDoesNotRemoveIt() throws Exception {
        String ownerToken = registerAndLogin("dono-do-livro-delete@example.com", "senha-forte-123");
        String otherUserToken = registerAndLogin("outro-usuario-delete@example.com", "senha-forte-123");
        UUID bookId = createBook(ownerToken, "O Hobbit", "J. R. R. Tolkien");

        mockMvc.perform(delete("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/books/{id}", bookId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("O Hobbit"));
    }

    @Test
    void deleteNonExistentBookReturns404() throws Exception {
        String accessToken = registerAndLogin("delete-inexistente@example.com", "senha-forte-123");

        mockMvc.perform(delete("/api/v1/books/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteWithoutAuthorizationHeaderReturns401() throws Exception {
        mockMvc.perform(delete("/api/v1/books/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private UUID createBook(String accessToken, String title, String author) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "author": "%s"
                                }
                                """.formatted(title, author)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(body.get("id").asString());

        // Garante que createdAt avance entre livros criados em sequência no mesmo teste,
        // já que a ordenação default da listagem (createdAt,desc) depende disso para ser
        // determinística mesmo em execuções muito rápidas.
        Thread.sleep(5);

        return bookId;
    }

    private UUID createBookWithStatus(String accessToken, String title, String author, String status) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "author": "%s",
                                  "status": "%s"
                                }
                                """.formatted(title, author, status)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asString());
    }

    private UUID createBookWithGenre(String accessToken, String title, String author, String genre) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "author": "%s",
                                  "genre": "%s"
                                }
                                """.formatted(title, author, genre)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asString());
    }

    private UUID createBookWithGenreAndStatus(
            String accessToken, String title, String author, String genre, String status) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "author": "%s",
                                  "genre": "%s",
                                  "status": "%s"
                                }
                                """.formatted(title, author, genre, status)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asString());
    }

    private UUID createBookWithGenreStatusAndRating(
            String accessToken, String title, String author, String genre, String status, Integer rating)
            throws Exception {
        String ratingField = rating != null ? ",\n  \"rating\": " + rating : "";
        MvcResult result = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "author": "%s",
                                  "genre": "%s",
                                  "status": "%s"%s
                                }
                                """.formatted(title, author, genre, status, ratingField)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asString());
    }

    private UUID createBookWithRating(String accessToken, String title, String author, int rating) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "author": "%s",
                                  "rating": %d
                                }
                                """.formatted(title, author, rating)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID bookId = UUID.fromString(body.get("id").asString());

        // Ver comentário equivalente em createBook: garante ordenação determinística pelo
        // default createdAt,desc da listagem.
        Thread.sleep(5);

        return bookId;
    }

    private void assertInvalidFields(MvcResult result, String... expectedFields) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode errors = body.get("errors");
        assertTrue(errors != null && errors.isArray(), "resposta deve conter um array \"errors\"");

        Set<String> actualFields = new HashSet<>();
        for (JsonNode error : errors) {
            actualFields.add(error.get("field").asString());
        }

        for (String expectedField : expectedFields) {
            assertTrue(actualFields.contains(expectedField),
                    "esperava que \"errors\" citasse o campo \"" + expectedField + "\", mas continha " + actualFields);
        }
        assertEquals(expectedFields.length, actualFields.size());
    }

    private String registerAndLogin(String email, String password) throws Exception {
        register(email, password);
        return login(email, password);
    }

    private RegisteredUser register(String email, String password) throws Exception {
        String payload = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registerBody = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        UUID id = UUID.fromString(registerBody.get("id").asString());
        return new RegisteredUser(id, email);
    }

    private String login(String email, String password) throws Exception {
        String payload = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginBody.get("accessToken").asString();
    }

    private record RegisteredUser(UUID id, String email) {
    }
}
