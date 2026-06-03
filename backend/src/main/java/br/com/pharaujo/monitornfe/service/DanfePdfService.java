package br.com.pharaujo.monitornfe.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Service
@RequiredArgsConstructor
public class DanfePdfService {

    public byte[] generate(String chaveAcesso, String emitenteNome, String xmlPath) {
        try {
            NfeXml nfe = parseXml(xmlPath, chaveAcesso, emitenteNome);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 28, 28, 26, 26);
            PdfWriter.getInstance(document, output);
            document.open();
            addHeader(document, nfe);
            addParties(document, nfe);
            addTotals(document, nfe);
            addItems(document, nfe);
            addFooter(document, nfe);
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new BadRequestException("Nao foi possivel gerar o PDF da DANFE");
        }
    }

    private void addHeader(Document document, NfeXml nfe) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[] { 2.4f, 1.2f, 2.4f });
        table.setWidthPercentage(100);

        PdfPCell emitente = cell();
        emitente.addElement(title(nfe.emitenteNome()));
        emitente.addElement(line("CNPJ", nfe.emitenteCnpj()));
        emitente.addElement(line("IE", nfe.emitenteIe()));
        emitente.addElement(small(nfe.emitenteEndereco()));
        table.addCell(emitente);

        PdfPCell center = cell();
        center.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        center.addElement(centerTitle("DANFE"));
        center.addElement(centerSmall("Documento Auxiliar da Nota Fiscal Eletronica"));
        center.addElement(centerTitle("NF-e"));
        center.addElement(centerSmall("Entrada"));
        center.addElement(centerSmall("No " + nfe.numero() + "  Serie " + nfe.serie()));
        table.addCell(center);

        PdfPCell key = cell();
        key.addElement(label("CHAVE DE ACESSO"));
        key.addElement(mono(formatKey(nfe.chave())));
        key.addElement(Chunk.NEWLINE);
        key.addElement(line("Natureza", nfe.naturezaOperacao()));
        key.addElement(line("Emissao", nfe.emissao()));
        key.addElement(line("Protocolo", nfe.protocolo()));
        table.addCell(key);

        document.add(table);
        document.add(spacer(8));
    }

    private void addParties(Document document, NfeXml nfe) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.addCell(sectionCell("DESTINATARIO / REMETENTE", new String[][] {
            { "Nome", nfe.destinatarioNome() },
            { "CNPJ", nfe.destinatarioCnpj() },
            { "Endereco", nfe.destinatarioEndereco() }
        }));
        table.addCell(sectionCell("DADOS DA NF-e", new String[][] {
            { "Modelo", nfe.modelo() },
            { "Finalidade", nfe.finalidade() },
            { "Saida/Entrada", nfe.saidaEntrada() }
        }));
        document.add(table);
        document.add(spacer(8));
    }

    private void addTotals(Document document, NfeXml nfe) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[] { 1, 1, 1, 1 });
        table.setWidthPercentage(100);
        table.addCell(box("Base ICMS", money(nfe.baseIcms())));
        table.addCell(box("Valor ICMS", money(nfe.valorIcms())));
        table.addCell(box("Valor Produtos", money(nfe.valorProdutos())));
        table.addCell(box("Valor Total NF", money(nfe.valorTotal())));
        document.add(table);
        document.add(spacer(8));
    }

    private void addItems(Document document, NfeXml nfe) throws DocumentException {
        document.add(label("DADOS DOS PRODUTOS / SERVICOS"));
        PdfPTable table = new PdfPTable(new float[] { 1.1f, 4.2f, 0.8f, 0.9f, 1.1f, 1.2f });
        table.setWidthPercentage(100);
        header(table, "Codigo");
        header(table, "Descricao");
        header(table, "NCM");
        header(table, "Qtd");
        header(table, "Unitario");
        header(table, "Total");

        int count = 0;
        for (Item item : nfe.items()) {
            if (count++ >= 35) {
                break;
            }
            table.addCell(data(item.codigo()));
            table.addCell(data(item.descricao()));
            table.addCell(data(item.ncm()));
            table.addCell(data(item.quantidade()));
            table.addCell(data(money(item.valorUnitario())));
            table.addCell(data(money(item.valorTotal())));
        }
        if (nfe.items().isEmpty()) {
            PdfPCell empty = data("Sem itens detalhados no XML importado.");
            empty.setColspan(6);
            table.addCell(empty);
        }
        document.add(table);
    }

    private void addFooter(Document document, NfeXml nfe) throws DocumentException {
        document.add(spacer(8));
        PdfPCell info = cell();
        info.addElement(label("INFORMACOES COMPLEMENTARES"));
        info.addElement(small(empty(nfe.informacoesComplementares(), "Sem informacoes complementares.")));
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.addCell(info);
        document.add(table);
        Paragraph generated = new Paragraph("PDF gerado localmente a partir do XML da NF-e pelo monitor-nfe-entrada.", smallFont());
        generated.setSpacingBefore(8);
        generated.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        document.add(generated);
    }

    private NfeXml parseXml(String xmlPath, String fallbackChave, String fallbackEmitente) {
        try {
            String xml = Files.readString(Path.of(xmlPath), StandardCharsets.UTF_8);
            org.w3c.dom.Document doc = parse(xml);
            Element ide = first(doc, "ide");
            Element emit = first(doc, "emit");
            Element dest = first(doc, "dest");
            Element total = first(doc, "ICMSTot");
            Element infProt = first(doc, "infProt");
            Element infAdic = first(doc, "infAdic");
            Element infNFe = first(doc, "infNFe");
            String chave = infNFe == null ? fallbackChave : stripChave(infNFe.getAttribute("Id"));
            return new NfeXml(
                empty(chave, fallbackChave),
                empty(child(ide, "natOp"), "Operacao fiscal"),
                empty(child(ide, "mod"), "55"),
                empty(child(ide, "serie"), "-"),
                empty(child(ide, "nNF"), "-"),
                formatDate(child(ide, "dhEmi")),
                empty(child(ide, "tpNF"), "0").equals("1") ? "Saida" : "Entrada",
                finalidade(child(ide, "finNFe")),
                empty(child(emit, "xNome"), fallbackEmitente),
                empty(child(emit, "CNPJ"), "-"),
                empty(child(emit, "IE"), "-"),
                endereco(first(emit, "enderEmit")),
                empty(child(dest, "xNome"), "-"),
                empty(child(dest, "CNPJ"), "-"),
                endereco(first(dest, "enderDest")),
                decimal(child(total, "vBC")),
                decimal(child(total, "vICMS")),
                decimal(child(total, "vProd")),
                decimal(child(total, "vNF")),
                empty(child(infProt, "nProt"), "-"),
                child(infAdic, "infCpl"),
                items(doc)
            );
        } catch (Exception exception) {
            return NfeXml.fallback(fallbackChave, fallbackEmitente);
        }
    }

    private java.util.List<Item> items(org.w3c.dom.Document doc) {
        java.util.ArrayList<Item> items = new java.util.ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("det");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element prod = first((Element) nodes.item(i), "prod");
            if (prod == null) {
                continue;
            }
            items.add(new Item(
                child(prod, "cProd"),
                child(prod, "xProd"),
                child(prod, "NCM"),
                child(prod, "qCom"),
                decimal(child(prod, "vUnCom")),
                decimal(child(prod, "vProd"))
            ));
        }
        return items;
    }

    private org.w3c.dom.Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private Element first(org.w3c.dom.Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    private Element first(Element parent, String tag) {
        if (parent == null) {
            return null;
        }
        NodeList nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    private String child(Element parent, String tag) {
        Element element = first(parent, tag);
        if (element == null || element.getTextContent() == null || element.getTextContent().isBlank()) {
            return null;
        }
        return element.getTextContent().trim();
    }

    private String endereco(Element element) {
        if (element == null) {
            return "-";
        }
        return String.join(", ",
            empty(child(element, "xLgr"), "-") + ", " + empty(child(element, "nro"), "S/N"),
            empty(child(element, "xBairro"), "-"),
            empty(child(element, "xMun"), "-") + "/" + empty(child(element, "UF"), "-"),
            "CEP " + empty(child(element, "CEP"), "-")
        );
    }

    private BigDecimal decimal(String value) {
        try {
            return value == null || value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private String formatDate(String value) {
        try {
            return value == null ? "-" : OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception exception) {
            return empty(value, "-");
        }
    }

    private String finalidade(String value) {
        return switch (empty(value, "")) {
            case "1" -> "Normal";
            case "2" -> "Complementar";
            case "3" -> "Ajuste";
            case "4" -> "Devolucao";
            default -> "-";
        };
    }

    private String stripChave(String id) {
        return id == null ? null : id.replaceFirst("^NFe", "").trim();
    }

    private String formatKey(String key) {
        if (key == null || key.length() != 44) {
            return empty(key, "-");
        }
        return key.replaceAll("(.{4})(?!$)", "$1 ");
    }

    private String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value == null ? BigDecimal.ZERO : value);
    }

    private String empty(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private PdfPCell cell() {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(7);
        cell.setBorderColor(new java.awt.Color(70, 70, 70));
        return cell;
    }

    private PdfPCell sectionCell(String title, String[][] rows) {
        PdfPCell cell = cell();
        cell.addElement(label(title));
        for (String[] row : rows) {
            cell.addElement(line(row[0], row[1]));
        }
        return cell;
    }

    private PdfPCell box(String title, String value) {
        PdfPCell cell = cell();
        cell.addElement(label(title));
        cell.addElement(new Paragraph(value, normalFont()));
        return cell;
    }

    private PdfPCell data(String value) {
        PdfPCell cell = new PdfPCell(new Phrase(empty(value, "-"), smallFont()));
        cell.setPadding(5);
        cell.setBorderColor(new java.awt.Color(205, 205, 205));
        return cell;
    }

    private void header(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, headerFont()));
        cell.setPadding(5);
        cell.setBackgroundColor(new java.awt.Color(235, 238, 245));
        cell.setBorderColor(new java.awt.Color(160, 160, 160));
        table.addCell(cell);
    }

    private Paragraph title(String value) {
        return new Paragraph(empty(value, "-"), titleFont());
    }

    private Paragraph centerTitle(String value) {
        Paragraph paragraph = new Paragraph(value, titleFont());
        paragraph.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        return paragraph;
    }

    private Paragraph centerSmall(String value) {
        Paragraph paragraph = small(value);
        paragraph.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        return paragraph;
    }

    private Paragraph label(String value) {
        return new Paragraph(value, headerFont());
    }

    private Paragraph line(String label, String value) {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Chunk(label + ": ", headerFont()));
        paragraph.add(new Chunk(empty(value, "-"), smallFont()));
        return paragraph;
    }

    private Paragraph small(String value) {
        return new Paragraph(empty(value, "-"), smallFont());
    }

    private Paragraph mono(String value) {
        return new Paragraph(empty(value, "-"), monoFont());
    }

    private Paragraph spacer(int height) {
        Paragraph paragraph = new Paragraph(" ");
        paragraph.setSpacingAfter(height);
        return paragraph;
    }

    private Font titleFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    }

    private Font headerFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7);
    }

    private Font normalFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 9);
    }

    private Font smallFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 7);
    }

    private Font monoFont() {
        return FontFactory.getFont(FontFactory.COURIER, 8);
    }

    private record NfeXml(
        String chave,
        String naturezaOperacao,
        String modelo,
        String serie,
        String numero,
        String emissao,
        String saidaEntrada,
        String finalidade,
        String emitenteNome,
        String emitenteCnpj,
        String emitenteIe,
        String emitenteEndereco,
        String destinatarioNome,
        String destinatarioCnpj,
        String destinatarioEndereco,
        BigDecimal baseIcms,
        BigDecimal valorIcms,
        BigDecimal valorProdutos,
        BigDecimal valorTotal,
        String protocolo,
        String informacoesComplementares,
        java.util.List<Item> items
    ) {
        static NfeXml fallback(String chave, String emitenteNome) {
            return new NfeXml(chave, "Operacao fiscal", "55", "-", "-", "-", "Entrada", "-",
                emitenteNome, "-", "-", "-", "-", "-", "-", BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, "-", null, java.util.List.of());
        }
    }

    private record Item(
        String codigo,
        String descricao,
        String ncm,
        String quantidade,
        BigDecimal valorUnitario,
        BigDecimal valorTotal
    ) {
    }
}
