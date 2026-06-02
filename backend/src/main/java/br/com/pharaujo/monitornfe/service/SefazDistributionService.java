package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.CompanyConfig;
import br.com.pharaujo.monitornfe.domain.NfeNote;
import br.com.pharaujo.monitornfe.domain.NfeStatus;
import br.com.pharaujo.monitornfe.repository.NfeNoteRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SefazDistributionService {

    private final CompanyConfigService companyConfigService;
    private final NoteLifecycleService noteLifecycleService;
    private final NfeNoteRepository nfeNoteRepository;
    private final SefazLogService sefazLogService;
    private final StorageService storageService;
    private final DanfePdfService danfePdfService;

    @Scheduled(cron = "${app.scheduler-distribution-cron}")
    @Transactional
    public void runScheduled() {
        try {
            CompanyConfig config = companyConfigService.getRequiredEntity();
            sefazLogService.log(config.getCnpj(), config.getAmbiente().name(), config.getUltNsu(), config.getMaxNsu(), "137", "Nenhum documento localizado no modo MVP", null);
        } catch (Exception exception) {
            log.warn("Falha ao executar monitoramento agendado: {}", exception.getMessage());
        }
    }

    @Transactional
    public void bootstrapSampleData() {
        if (nfeNoteRepository.count() > 0) {
            return;
        }
        CompanyConfig company = companyConfigService.getRequiredEntity();
        NfeNote note = new NfeNote();
        note.setCompanyConfig(company);
        note.setChaveAcesso("35260612345678000123550010000000011000000010");
        note.setNsu("1");
        note.setModelo("55");
        note.setEmitenteCnpj("11222333000199");
        note.setEmitenteNome("Fornecedor Exemplo LTDA");
        note.setDestinatarioCnpj(company.getCnpj());
        note.setDataEmissao(LocalDateTime.now().minusDays(1));
        note.setValorTotal(new BigDecimal("1499.90"));
        note.setStatus(NfeStatus.DETECTADA_RESUMO);
        note = noteLifecycleService.save(note);
        noteLifecycleService.registerEvent(note, "210210", "Ciencia da Operacao", "1234567890", OffsetDateTime.now().minusHours(6), "Evento identificado externamente");
        note = noteLifecycleService.updateStatus(note, NfeStatus.MANIFESTADA_EXTERNAMENTE, "Manifestacao externa identificada");
        String xml = """
            <nfeProc>
              <NFe>
                <infNFe Id="%s">
                  <emit><xNome>%s</xNome></emit>
                </infNFe>
              </NFe>
            </nfeProc>
            """.formatted(note.getChaveAcesso(), note.getEmitenteNome());
        note.setXmlStoragePath(storageService.storeXml(note, xml));
        note.setXmlDownloadedAt(OffsetDateTime.now().minusHours(2));
        note = noteLifecycleService.updateStatus(note, NfeStatus.XML_BAIXADO, "XML completo disponibilizado");
        byte[] pdf = danfePdfService.generate(note.getChaveAcesso(), note.getEmitenteNome(), note.getXmlStoragePath());
        note.setPdfStoragePath(storageService.storePdf(note, pdf));
        note.setPdfGeneratedAt(OffsetDateTime.now().minusHours(1));
        noteLifecycleService.updateStatus(note, NfeStatus.PDF_GERADO, "PDF gerado localmente");
    }
}
