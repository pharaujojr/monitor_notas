package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.CompanyConfig;
import br.com.pharaujo.monitornfe.domain.CompanyStatus;
import br.com.pharaujo.monitornfe.repository.CompanyConfigRepository;
import br.com.pharaujo.monitornfe.web.dto.SyncResultResponse;
import br.com.swconsultoria.nfe.schema.retdistdfeint.RetDistDFeInt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra a sincronização com a SEFAZ (NFeDistribuicaoDFe), iterando pelo NSU
 * a partir do último consumido até esgotar os documentos disponíveis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SefazDistributionService {

    /** cStat 138 = documento(s) localizado(s). */
    private static final String CSTAT_DOCUMENTOS = "138";
    /** Teto de chamadas por execução, evita laços longos e consumo excessivo. */
    private static final int MAX_CONSULTAS = 50;

    private final CompanyConfigService companyConfigService;
    private final CompanyConfigRepository companyConfigRepository;
    private final SefazClient sefazClient;
    private final DistribuicaoProcessor distribuicaoProcessor;
    private final SefazLogService sefazLogService;

    @Scheduled(cron = "${app.scheduler-distribution-cron}")
    public void runScheduled() {
        try {
            CompanyConfig config = companyConfigService.getRequiredEntity();
            if (config.getStatus() != CompanyStatus.ATIVO) {
                log.debug("Empresa inativa, sincronização agendada ignorada.");
                return;
            }
            sincronizar();
        } catch (Exception exception) {
            log.warn("Falha ao executar monitoramento agendado: {}", exception.getMessage());
        }
    }

    /**
     * Consulta a SEFAZ repetidamente até alcançar o maxNSU (ou o teto de chamadas),
     * processando cada documento e persistindo o avanço do NSU na empresa.
     */
    @Transactional
    public SyncResultResponse sincronizar() {
        CompanyConfig config = companyConfigService.getRequiredEntity();
        String ultNsu = config.getUltNsu();
        String maxNsu = config.getMaxNsu();
        String cstat = null;
        String motivo = null;
        int processados = 0;
        int consultas = 0;

        while (consultas < MAX_CONSULTAS) {
            RetDistDFeInt ret = sefazClient.consultarPorNsu(config, ultNsu);
            consultas++;
            cstat = ret.getCStat();
            motivo = ret.getXMotivo();
            String respUltNsu = nvl(ret.getUltNSU(), ultNsu);
            maxNsu = nvl(ret.getMaxNSU(), maxNsu);

            sefazLogService.log(config.getCnpj(), config.getAmbiente().name(), ultNsu, respUltNsu, cstat, motivo, null);

            if (CSTAT_DOCUMENTOS.equals(cstat) && ret.getLoteDistDFeInt() != null) {
                for (RetDistDFeInt.LoteDistDFeInt.DocZip doc : ret.getLoteDistDFeInt().getDocZip()) {
                    try {
                        if (distribuicaoProcessor.processar(config, doc.getNSU(), doc.getSchema(), doc.getValue())) {
                            processados++;
                        }
                    } catch (Exception exception) {
                        log.warn("Falha ao processar documento NSU {}: {}", doc.getNSU(), exception.getMessage());
                    }
                }
                ultNsu = respUltNsu;
                persistirNsu(config, ultNsu, maxNsu);

                if (compararNsu(ultNsu, maxNsu) >= 0) {
                    break; // alcançou o último documento disponível
                }
            } else {
                // 137 (nada novo), 656 (consumo indevido) ou erro: registra avanço e para
                persistirNsu(config, ultNsu, maxNsu);
                break;
            }
        }

        return new SyncResultResponse(cstat, motivo, ultNsu, maxNsu, processados, consultas);
    }

    private void persistirNsu(CompanyConfig config, String ultNsu, String maxNsu) {
        config.setUltNsu(ultNsu);
        config.setMaxNsu(maxNsu);
        companyConfigRepository.save(config);
    }

    private int compararNsu(String a, String b) {
        return Long.compare(parseNsu(a), parseNsu(b));
    }

    private long parseNsu(String value) {
        try {
            return Long.parseLong(value == null ? "0" : value.trim());
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private String nvl(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
