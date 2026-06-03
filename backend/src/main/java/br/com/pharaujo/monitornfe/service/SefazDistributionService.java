package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.CompanyConfig;
import br.com.pharaujo.monitornfe.domain.CompanyStatus;
import br.com.pharaujo.monitornfe.repository.CompanyConfigRepository;
import br.com.pharaujo.monitornfe.web.dto.SyncResultResponse;
import br.com.swconsultoria.nfe.schema.retdistdfeint.RetDistDFeInt;
import java.time.Duration;
import java.time.Instant;
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
    /** cStat 137 = nenhum documento localizado. */
    private static final String CSTAT_NADA = "137";
    /** cStat 656 = consumo indevido (bloqueio temporário). */
    private static final String CSTAT_CONSUMO_INDEVIDO = "656";
    /** Teto de chamadas por execução, evita laços longos e consumo excessivo. */
    private static final int MAX_CONSULTAS = 50;
    /** Intervalo mínimo entre consultas exigido pela SEFAZ (NT 2014.002) quando não há novidades. */
    private static final Duration INTERVALO_MINIMO = Duration.ofMinutes(65);

    private final CompanyConfigService companyConfigService;
    private final CompanyConfigRepository companyConfigRepository;
    private final SefazClient sefazClient;
    private final DistribuicaoProcessor distribuicaoProcessor;
    private final SefazLogService sefazLogService;

    /** Momento a partir do qual uma nova consulta é permitida (respeita o intervalo da SEFAZ). */
    private volatile Instant proximaConsultaPermitida = Instant.EPOCH;

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
        Instant agora = Instant.now();
        if (agora.isBefore(proximaConsultaPermitida)) {
            return new SyncResultResponse("cooldown",
                "Intervalo mínimo da SEFAZ ainda não decorrido. Próxima consulta liberada às " + proximaConsultaPermitida,
                config.getUltNsu(), config.getMaxNsu(), 0, 0);
        }

        String ultNsu = config.getUltNsu();
        String maxNsu = config.getMaxNsu();
        String cstat = null;
        String motivo = null;
        int processados = 0;
        int consultas = 0;
        boolean limiteAtingido = false;

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
                if (consultas >= MAX_CONSULTAS) {
                    limiteAtingido = true; // ainda há documentos; pode retomar antes de 1h
                }
            } else {
                // 137 (nada novo), 656 (consumo indevido) ou erro: registra avanço e para
                persistirNsu(config, ultNsu, maxNsu);
                break;
            }
        }

        // Respeita o intervalo mínimo da SEFAZ: só dispensa o cooldown se paramos por
        // ter batido o teto de consultas ainda havendo documentos pendentes.
        if (!limiteAtingido) {
            proximaConsultaPermitida = Instant.now().plus(INTERVALO_MINIMO);
        }
        if (CSTAT_CONSUMO_INDEVIDO.equals(cstat)) {
            log.warn("SEFAZ retornou 656 (consumo indevido). Próxima consulta liberada às {}", proximaConsultaPermitida);
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
