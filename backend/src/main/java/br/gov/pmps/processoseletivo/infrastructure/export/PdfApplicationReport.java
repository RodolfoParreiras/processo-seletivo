package br.gov.pmps.processoseletivo.infrastructure.export;

import br.gov.pmps.processoseletivo.application.usecase.export.ApplicationExportFormat;
import br.gov.pmps.processoseletivo.application.usecase.export.ApplicationExportUseCase;
import br.gov.pmps.processoseletivo.application.usecase.export.ExportLabels;
import br.gov.pmps.processoseletivo.domain.rule.Cpf;
import br.gov.pmps.processoseletivo.shared.OfficialTime;
import java.io.IOException;
import java.io.OutputStream;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

/**
 * Relatório administrativo em PDF: resumo por cargo e situação e relação de inscrições.
 * Somente CPF mascarado, sem dados de pessoa com deficiência (relatório tem circulação mais ampla).
 * A tabela é enviada ao documento em blocos para não acumular todas as linhas em memória.
 */
@Component
public class PdfApplicationReport implements ApplicationExportFormat {

    private static final int ROWS_PER_BLOCK = 200;
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font TEXT = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Font HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

    @Override
    public ApplicationExportUseCase.Kind kind() {
        return ApplicationExportUseCase.Kind.APPLICATIONS_REPORT;
    }

    @Override
    public String contentType() {
        return "application/pdf";
    }

    @Override
    public String fileExtension() {
        return "pdf";
    }

    @Override
    public void write(ExportContext context, RowSource rows, OutputStream output) throws IOException {
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        PdfWriter.getInstance(document, output);
        document.open();

        Paragraph title = new Paragraph("Prefeitura Municipal de Paraíba do Sul — Relatório de inscrições", TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("Processo Seletivo " + context.processNumber() + " — " + context.processTitle(), SUBTITLE));
        document.add(new Paragraph("Situação: " + (context.statusFilter() == null ? "todas"
                : ExportLabels.status(context.statusFilter())) + " · Gerado em "
                + OfficialTime.format(context.generatedAt()) + " (horário de Brasília)", TEXT));

        document.add(new Paragraph("Resumo por cargo e situação", SUBTITLE));
        PdfPTable summary = table(new float[] {4, 2, 1}, "Cargo", "Situação", "Inscrições");
        long total = 0;
        for (SummaryLine line : context.summary()) {
            cells(summary, line.positionName(), ExportLabels.status(line.status()), String.valueOf(line.count()));
            total += line.count();
        }
        cells(summary, "Total", "", String.valueOf(total));
        document.add(summary);

        document.add(new Paragraph("Relação de inscrições", SUBTITLE));
        PdfPTable[] current = {table(new float[] {2, 5, 2, 4, 2}, "Inscrição", "Nome", "CPF", "Cargo", "Situação")};
        int[] rowsInBlock = {0};
        rows.forEach(row -> {
            cells(current[0], row.applicationNumber(), row.fullName(), Cpf.mask(row.cpf()), row.positionName(),
                    ExportLabels.status(row.status()));
            if (++rowsInBlock[0] == ROWS_PER_BLOCK) {
                document.add(current[0]);
                current[0] = table(new float[] {2, 5, 2, 4, 2});
                rowsInBlock[0] = 0;
            }
        });
        document.add(current[0]);
        document.close();
    }

    private static PdfPTable table(float[] widths, String... headers) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setSpacingAfter(8);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER));
            cell.setPadding(3);
            table.addCell(cell);
        }
        if (headers.length > 0) {
            table.setHeaderRows(1);
        }
        return table;
    }

    private static void cells(PdfPTable table, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, TEXT));
            cell.setPadding(3);
            table.addCell(cell);
        }
    }
}
