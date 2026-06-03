package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.CompanyConfig;
import br.com.pharaujo.monitornfe.domain.NfeNote;
import br.com.pharaujo.monitornfe.domain.NfeStatus;
import br.com.pharaujo.monitornfe.repository.NfeNoteRepository;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Descompacta (GZIP) e interpreta cada documento retornado pela DistribuicaoDFe,
 * persistindo como NfeNote / evento conforme o schema do documento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistribuicaoProcessor {

    private final NfeNoteRepository nfeNoteRepository;
    private final NoteLifecycleService noteLifecycleService;
    private final StorageService storageService;
    private final DanfePdfService danfePdfService;

    /**
     * @return true se o documento gerou criação/atualização de nota ou evento
     */
    @Transactional
    public boolean processar(CompanyConfig company, String nsu, String schema, byte[] gzipContent) {
        if (schema == null) {
            return false;
        }
        String xml = descompactar(gzipContent);
        if (xml == null) {
            return false;
        }
        Document doc = parse(xml);
        if (doc == null) {
            return false;
        }
        if (schema.startsWith("resNFe")) {
            return processarResumo(company, nsu, doc);
        }
        if (schema.startsWith("procNFe")) {
            return processarNotaCompleta(company, nsu, doc, xml);
        }
        if (schema.startsWith("resEvento") || schema.startsWith("procEventoNFe")) {
            return processarEvento(doc);
        }
        // resInfCadastro e outros schemas não são relevantes para o monitoramento de entrada
        log.debug("Schema ignorado: {}", schema);
        return false;
    }

    private boolean processarResumo(CompanyConfig company, String nsu, Document doc) {
        String chave = text(doc, "chNFe");
        if (chave == null) {
            return false;
        }
        if (nfeNoteRepository.findByChaveAcesso(chave).isPresent()) {
            return false; // já temos a nota (resumo ou completa)
        }
        NfeNote note = new NfeNote();
        note.setCompanyConfig(company);
        note.setChaveAcesso(chave);
        note.setNsu(nsu);
        note.setModelo("55");
        note.setEmitenteCnpj(defaultText(text(doc, "CNPJ"), "00000000000000"));
        note.setEmitenteNome(defaultText(text(doc, "xNome"), "Emitente não informado"));
        note.setDestinatarioCnpj(company.getCnpj());
        note.setDataEmissao(parseDateTime(text(doc, "dhEmi")));
        note.setValorTotal(parseValor(text(doc, "vNF")));
        note.setStatus(NfeStatus.DETECTADA_RESUMO);
        noteLifecycleService.save(note);
        return true;
    }

    private boolean processarNotaCompleta(CompanyConfig company, String nsu, Document doc, String xml) {
        Element infNFe = firstElement(doc, "infNFe");
        if (infNFe == null) {
            return false;
        }
        String chave = stripChave(infNFe.getAttribute("Id"));
        Element emit = firstElement(doc, "emit");
        Element dest = firstElement(doc, "dest");
        Element ide = firstElement(doc, "ide");
        Element icmsTot = firstElement(doc, "ICMSTot");

        Optional<NfeNote> existing = nfeNoteRepository.findByChaveAcesso(chave);
        NfeNote note = existing.orElseGet(NfeNote::new);
        note.setCompanyConfig(company);
        note.setChaveAcesso(chave);
        if (note.getNsu() == null) {
            note.setNsu(nsu);
        }
        note.setModelo(defaultText(childText(ide, "mod"), "55"));
        note.setEmitenteCnpj(defaultText(childText(emit, "CNPJ"), note.getEmitenteCnpj()));
        note.setEmitenteNome(defaultText(childText(emit, "xNome"), note.getEmitenteNome()));
        note.setDestinatarioCnpj(defaultText(childText(dest, "CNPJ"), company.getCnpj()));
        note.setDataEmissao(parseDateTime(childText(ide, "dhEmi")));
        note.setValorTotal(parseValor(childText(icmsTot, "vNF")));
        if (note.getStatus() == null) {
            note.setStatus(NfeStatus.DETECTADA_RESUMO);
        }
        note = noteLifecycleService.save(note);

        // armazena XML completo
        note.setXmlStoragePath(storageService.storeXml(note, xml));
        note.setXmlDownloadedAt(OffsetDateTime.now());
        note = noteLifecycleService.updateStatus(note, NfeStatus.XML_BAIXADO, "XML completo obtido via DistribuicaoDFe");

        // gera PDF (DANFE simplificada) a partir do XML
        try {
            byte[] pdf = danfePdfService.generate(note.getChaveAcesso(), note.getEmitenteNome(), note.getXmlStoragePath());
            note.setPdfStoragePath(storageService.storePdf(note, pdf));
            note.setPdfGeneratedAt(OffsetDateTime.now());
            noteLifecycleService.updateStatus(note, NfeStatus.PDF_GERADO, "PDF gerado localmente");
        } catch (Exception exception) {
            log.warn("Falha ao gerar PDF da nota {}: {}", chave, exception.getMessage());
        }
        return true;
    }

    private boolean processarEvento(Document doc) {
        String chave = text(doc, "chNFe");
        if (chave == null) {
            return false;
        }
        Optional<NfeNote> note = nfeNoteRepository.findByChaveAcesso(chave);
        if (note.isEmpty()) {
            // evento de uma nota que ainda não conhecemos; será correlacionado quando o resumo/nota chegar
            return false;
        }
        String tpEvento = defaultText(text(doc, "tpEvento"), "000000");
        String xEvento = defaultText(text(doc, "xEvento"), "Evento");
        String nProt = text(doc, "nProt");
        OffsetDateTime occurredAt = parseOffset(text(doc, "dhEvento"));
        noteLifecycleService.registerEvent(note.get(), tpEvento, xEvento, nProt, occurredAt,
            "Evento recebido via DistribuicaoDFe");
        return true;
    }

    // -------- helpers --------

    private String descompactar(byte[] gzip) {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gzip))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            // não fatal: pula o documento sem abortar o lote/transação
            log.warn("Falha ao descompactar documento (GZIP): {}", exception.getMessage());
            return null;
        }
    }

    private Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            log.warn("Falha ao parsear XML do documento: {}", exception.getMessage());
            return null;
        }
    }

    /** Texto do primeiro elemento com o nome informado em todo o documento. */
    private String text(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        String value = nodes.item(0).getTextContent();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Element firstElement(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }

    /** Texto do primeiro filho direto/indireto com o nome informado, dentro de um elemento. */
    private String childText(Element parent, String tag) {
        if (parent == null) {
            return null;
        }
        NodeList nodes = parent.getElementsByTagName(tag);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String value = node.getTextContent();
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String stripChave(String id) {
        if (id == null) {
            return null;
        }
        return id.replaceFirst("^NFe", "").trim();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private LocalDateTime parseDateTime(String value) {
        OffsetDateTime offset = parseOffset(value);
        return offset == null ? LocalDateTime.now() : offset.toLocalDateTime();
    }

    private OffsetDateTime parseOffset(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.trim());
        } catch (Exception exception) {
            log.debug("Data em formato inesperado: {}", value);
            return null;
        }
    }

    private BigDecimal parseValor(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }
}
