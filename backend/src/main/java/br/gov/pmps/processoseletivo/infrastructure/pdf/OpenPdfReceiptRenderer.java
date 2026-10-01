package br.gov.pmps.processoseletivo.infrastructure.pdf;

import br.gov.pmps.processoseletivo.application.usecase.application.ReceiptRenderer;
import br.gov.pmps.processoseletivo.shared.OfficialTime;
import java.io.ByteArrayOutputStream;
import org.openpdf.text.Chunk;
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

/** Comprovante de inscrição em PDF (ESPECIFICACAO §27), gerado em memória: o arquivo é pequeno. */
@Component
public class OpenPdfReceiptRenderer implements ReceiptRenderer {

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font TEXT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8);

    @Override
    public byte[] render(ReceiptData data) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, output);
        document.addTitle("Comprovante de inscrição " + data.applicationNumber());
        document.open();

        centered(document, "PREFEITURA MUNICIPAL DE PARAÍBA DO SUL", TITLE);
        centered(document, "Comprovante de Inscrição", SUBTITLE);
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(new float[] {1.2f, 2.8f});
        table.setWidthPercentage(100);
        row(table, "Processo seletivo", data.processNumber() + " — " + data.processTitle());
        row(table, "Secretaria", data.department());
        row(table, "Número da inscrição", data.applicationNumber());
        row(table, "Cargo", data.positionName());
        row(table, "Data/hora da inscrição", OfficialTime.format(data.confirmedAt()) + " (horário de Brasília)");
        row(table, "Candidato(a)", data.candidateName());
        row(table, "CPF", data.maskedCpf());
        row(table, "Pessoa com deficiência", data.hasDisability() ? "Sim" : "Não");
        if (data.hasDisability()) {
            row(table, "Necessidade de adaptações", String.join(", ", data.adaptations()));
        }
        document.add(table);
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("Declaração", SUBTITLE));
        document.add(new Paragraph(data.declaration(), TEXT));
        document.add(Chunk.NEWLINE);

        document.add(new Paragraph("Código de autenticidade: " + data.verificationCode(), LABEL));
        document.add(new Paragraph(
                "Confira a autenticidade deste comprovante em " + data.verificationUrl()
                        + " informando o código acima.", SMALL));

        document.close();
        return output.toByteArray();
    }

    private static void centered(Document document, String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        document.add(paragraph);
    }

    private static void row(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL));
        labelCell.setPadding(5);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, TEXT));
        valueCell.setPadding(5);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}
