package br.com.pharaujo.monitornfe.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DanfePdfService {

    public byte[] generate(String chaveAcesso, String emitenteNome, String xmlPath) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph("DANFE simplificado - monitor-nfe-entrada"));
            document.add(new Paragraph("Chave: " + chaveAcesso));
            document.add(new Paragraph("Emitente: " + emitenteNome));
            document.add(new Paragraph("XML origem: " + xmlPath));
            document.close();
            return output.toByteArray();
        } catch (DocumentException exception) {
            throw new BadRequestException("Nao foi possivel gerar o PDF da DANFE");
        }
    }
}
