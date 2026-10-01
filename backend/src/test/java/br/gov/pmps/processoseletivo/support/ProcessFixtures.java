package br.gov.pmps.processoseletivo.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

/** Cria processos publicados pela API administrativa, como em uso real. */
public final class ProcessFixtures {

    public static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

    public record Requirement(String name, boolean mandatory) {
    }

    public record CreatedProcess(UUID id, String number, List<UUID> positionIds, List<UUID> requirementIds) {
    }

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;
    private final Cookie admin;

    public ProcessFixtures(MockMvc mockMvc, JsonMapper jsonMapper, Cookie admin) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
        this.admin = admin;
    }

    /** Processo com inscrições abertas (início no passado). */
    public CreatedProcess openProcess(boolean multipleApplications, int positions, Requirement... requirements)
            throws Exception {
        String number = "A" + ThreadLocalRandom.current().nextInt(1, 999_999_999);
        Instant start = Instant.now().minus(Duration.ofHours(1)).truncatedTo(ChronoUnit.SECONDS);
        Instant end = Instant.now().plus(Duration.ofDays(10)).truncatedTo(ChronoUnit.SECONDS);
        UUID processId = idFrom(mockMvc.perform(post("/api/admin/processes").with(csrf()).cookie(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"number": "%s", "year": 2026, "title": "Processo de Teste", "department": "Secretaria",
                         "registrationStart": "%s", "registrationEnd": "%s",
                         "multipleApplicationsAllowed": %s, "titleEvaluationEnabled": true}
                        """.formatted(number, start, end, multipleApplications))));

        List<UUID> positionIds = new ArrayList<>();
        for (int i = 1; i <= positions; i++) {
            positionIds.add(idFrom(mockMvc.perform(post("/api/admin/processes/{id}/positions", processId)
                    .with(csrf()).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"Cargo %d\", \"vacancies\": 2}".formatted(i)))));
        }
        List<UUID> requirementIds = new ArrayList<>();
        for (Requirement requirement : requirements) {
            requirementIds.add(idFrom(mockMvc.perform(post("/api/admin/processes/{id}/document-requirements", processId)
                    .with(csrf()).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\": \"%s\", \"description\": null, \"mandatory\": %s, \"title\": false}"
                            .formatted(requirement.name(), requirement.mandatory())))));
        }
        mockMvc.perform(multipart("/api/admin/processes/{id}/notices", processId)
                        .file(new MockMultipartFile("file", "edital.pdf", "application/pdf", PDF))
                        .with(csrf()).cookie(admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/admin/processes/{id}/publish", processId).with(csrf()).cookie(admin))
                .andExpect(status().isNoContent());
        return new CreatedProcess(processId, number, positionIds, requirementIds);
    }

    private UUID idFrom(ResultActions result) throws Exception {
        String body = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(jsonMapper.readTree(body).get("id").asString());
    }
}
