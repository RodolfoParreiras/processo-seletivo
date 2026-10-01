package br.gov.pmps.processoseletivo.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.pmps.processoseletivo.PostgresIntegrationTest;
import br.gov.pmps.processoseletivo.support.AdminTestAccounts;
import br.gov.pmps.processoseletivo.support.AuthTestClient;
import br.gov.pmps.processoseletivo.support.AuthTestClient.RegisteredCandidate;
import br.gov.pmps.processoseletivo.support.ProcessFixtures;
import br.gov.pmps.processoseletivo.support.ProcessFixtures.CreatedProcess;
import jakarta.servlet.http.Cookie;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

@AutoConfigureMockMvc
class ExportTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JsonMapper jsonMapper;

    private AdminTestAccounts admins;
    private CreatedProcess process;
    private final List<RegisteredCandidate> candidates = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        admins = new AdminTestAccounts(jdbcTemplate, passwordEncoder, mockMvc);
        Cookie processAdmin = admins.sessionWith(
                "PROCESSO_VISUALIZAR", "PROCESSO_CRIAR", "PROCESSO_EDITAR", "PROCESSO_PUBLICAR", "PROCESSO_ENCERRAR");
        process = new ProcessFixtures(mockMvc, jsonMapper, processAdmin).openProcess(false, 1);

        AuthTestClient auth = new AuthTestClient(mockMvc);
        for (int i = 0; i < 2; i++) {
            RegisteredCandidate candidate = auth.registerCandidate();
            candidates.add(candidate);
            Cookie session = auth.login(candidate.cpf(), candidate.password());
            String body = mockMvc.perform(post("/api/candidate/applications").with(csrf()).cookie(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"processId\": \"%s\", \"positionId\": \"%s\"}"
                                    .formatted(process.id(), process.positionIds().get(0))))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            UUID applicationId = UUID.fromString(jsonMapper.readTree(body).get("id").asString());
            mockMvc.perform(post("/api/candidate/applications/{id}/confirm", applicationId).with(csrf()).cookie(session))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void spreadsheetHasOneRowPerApplicationWithoutDisabilityDataByDefault() throws Exception {
        Cookie exporter = admins.sessionWith("EXPORTACAO_GERAR");

        byte[] content = download(exporter, "/api/admin/processes/{id}/exports/applications.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(3);
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Número da inscrição");
            assertThat(header.getLastCellNum()).isEqualTo((short) 7);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo(process.number() + "/2026-00001");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Maria Teste Silva");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
            assertThat(sheet.getRow(1).getCell(5).getStringCellValue()).isEqualTo("Recebida");
        }
        assertThat(jdbcTemplate.queryForObject("""
                select details->>'records' from audit_log
                 where action = 'EXPORT_GENERATED' and target_id = ? order by id desc limit 1
                """, String.class, process.id().toString())).isEqualTo("2");
    }

    @Test
    void disabilityColumnsRequireSpecificPermission() throws Exception {
        Cookie exporter = admins.sessionWith("EXPORTACAO_GERAR", "DADOS_PCD_VISUALIZAR");

        byte[] content = download(exporter, "/api/admin/processes/{id}/exports/applications.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Row header = workbook.getSheetAt(0).getRow(0);
            assertThat(header.getLastCellNum()).isEqualTo((short) 9);
            assertThat(header.getCell(6).getStringCellValue()).isEqualTo("Pessoa com deficiência");
        }
    }

    @Test
    void statusFilterLimitsRows() throws Exception {
        Cookie exporter = admins.sessionWith("EXPORTACAO_GERAR");

        byte[] content = download(exporter, "/api/admin/processes/{id}/exports/applications.xlsx?status=DEFERIDA");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            assertThat(workbook.getSheetAt(0).getPhysicalNumberOfRows()).isEqualTo(1);
        }
    }

    @Test
    void pdfReportIsGenerated() throws Exception {
        Cookie reporter = admins.sessionWith("RELATORIO_GERAR");

        byte[] content = download(reporter, "/api/admin/processes/{id}/reports/applications.pdf");

        assertThat(new String(content, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void eachExportRequiresItsPermission() throws Exception {
        Cookie reporter = admins.sessionWith("RELATORIO_GERAR");
        Cookie exporter = admins.sessionWith("EXPORTACAO_GERAR");

        mockMvc.perform(get("/api/admin/processes/{id}/exports/applications.xlsx", process.id()).cookie(reporter))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/processes/{id}/reports/applications.pdf", process.id()).cookie(exporter))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/processes/{id}/exports/applications.xlsx", process.id()))
                .andExpect(status().isUnauthorized());
    }

    private byte[] download(Cookie session, String path) throws Exception {
        MvcResult started = mockMvc.perform(get(path, process.id()).cookie(session))
                .andExpect(request().asyncStarted())
                .andReturn();
        return mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andReturn().getResponse().getContentAsByteArray();
    }
}
