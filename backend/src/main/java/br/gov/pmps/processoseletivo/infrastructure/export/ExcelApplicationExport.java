package br.gov.pmps.processoseletivo.infrastructure.export;

import br.gov.pmps.processoseletivo.application.usecase.export.ApplicationExportFormat;
import br.gov.pmps.processoseletivo.application.usecase.export.ApplicationExportUseCase;
import br.gov.pmps.processoseletivo.application.usecase.export.ExportLabels;
import br.gov.pmps.processoseletivo.shared.OfficialTime;
import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Planilha para a classificação externa. SXSSF mantém em memória só uma janela de linhas e grava o
 * restante em arquivo temporário, descartado ao final (ESPECIFICACAO §61).
 */
@Component
public class ExcelApplicationExport implements ApplicationExportFormat {

    private static final int ROWS_IN_MEMORY = 200;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public ApplicationExportUseCase.Kind kind() {
        return ApplicationExportUseCase.Kind.CLASSIFICATION_SPREADSHEET;
    }

    @Override
    public String contentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public String fileExtension() {
        return "xlsx";
    }

    @Override
    public void write(ExportContext context, RowSource rows, OutputStream output) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(ROWS_IN_MEMORY)) {
            Sheet sheet = workbook.createSheet("Inscrições");
            CellStyle headerStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            List<String> headers = new ArrayList<>(List.of(
                    "Número da inscrição", "Nome", "CPF", "Data de nascimento", "Cargo", "Situação"));
            if (context.includeDisabilityData()) {
                headers.add("Pessoa com deficiência");
                headers.add("Necessidade de adaptações");
            }
            headers.add("Data/hora da inscrição");
            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < headers.size(); column++) {
                headerRow.createCell(column).setCellValue(headers.get(column));
                headerRow.getCell(column).setCellStyle(headerStyle);
            }

            int[] rowIndex = {1};
            rows.forEach(exportRow -> {
                Row row = sheet.createRow(rowIndex[0]++);
                int column = 0;
                // Todas as células são texto: valores nunca são interpretados como fórmulas.
                row.createCell(column++).setCellValue(exportRow.applicationNumber());
                row.createCell(column++).setCellValue(exportRow.fullName());
                row.createCell(column++).setCellValue(formatCpf(exportRow.cpf()));
                row.createCell(column++).setCellValue(DATE.format(exportRow.birthDate()));
                row.createCell(column++).setCellValue(exportRow.positionName());
                row.createCell(column++).setCellValue(ExportLabels.status(exportRow.status()));
                if (context.includeDisabilityData()) {
                    row.createCell(column++).setCellValue(exportRow.hasDisability() ? "Sim" : "Não");
                    row.createCell(column++).setCellValue(ExportLabels.adaptations(exportRow.adaptations()));
                }
                row.createCell(column).setCellValue(OfficialTime.format(exportRow.confirmedAt()));
            });
            workbook.write(output);
        }
    }

    private static String formatCpf(String digits) {
        return digits.substring(0, 3) + "." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-"
                + digits.substring(9);
    }
}
