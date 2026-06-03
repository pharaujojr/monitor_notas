package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.CompanyConfig;
import br.com.pharaujo.monitornfe.config.AppProperties;
import br.com.pharaujo.monitornfe.repository.CompanyConfigRepository;
import br.com.pharaujo.monitornfe.web.dto.SyncResultResponse;
import br.com.swconsultoria.nfe.schema.retdistdfeint.RetDistDFeInt;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra a sincronização com a SEFAZ (NFeDistribuicaoDFe), iterando pelo NSU
 * a partir do último consumido até esgotar os documentos disponíveis.
 *
 * O agendamento é totalmente automático e persistido no banco (sobrevive a restarts):
 * um ticker verifica a cada minuto se já é hora de consultar, respeitando o intervalo
 * mínimo da SEFAZ (656/consumo indevido) e a cadência configurada de 6 horas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SefazDistributionService {

    /** cStat 138 = documento(s) localizado(s). */
    private static final String CSTAT_DOCUMENTOS = "138";
    /** cStat 656 = consumo indevido (bloqueio temporário). */
    private static final String CSTAT_CONSUMO_INDEVIDO = "656";
    /** Intervalo mínimo entre consultas exigido pela SEFAZ (NT 2014.002) sem novidades. */
    private static final Duration INTERVALO_MINIMO = Duration.ofMinutes(65);
    /** Cadência normal de sincronização. */
    private static final Duration CADENCIA = Duration.ofHours(6);
    /** Backoff base após 656; dobra a cada 656 consecutivo até o teto. */
    private static final Duration BACKOFF_BASE = Duration.ofHours(1);
    /** Teto do backoff (a SEFAZ pode suspender por até 12h em reincidência). */
    private static final Duration BACKOFF_MAX = Duration.ofHours(12);

    private final CompanyConfigService companyConfigService;
    private final CompanyConfigRepository companyConfigRepository;
    private final AppProperties appProperties;
    private final SefazClient sefazClient;
    private final DistribuicaoProcessor distribuicaoProcessor;
    private final SefazLogService sefazLogService;

    /** Evita execuções simultâneas (manual + agendada) sobre o mesmo certificado. */
    private final java.util.concurrent.atomic.AtomicBoolean emExecucao =
        new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * Consulta a SEFAZ repetidamente até alcançar o maxNSU (ou o teto de chamadas),
     * processando cada documento e persistindo o avanço do NSU e o próximo agendamento.
     */
    @Transactional
    public SyncResultResponse sincronizar() {
        if (!appProperties.isSefazFallbackEnabled()) {
            CompanyConfig config = companyConfigService.getRequiredEntity();
            return new SyncResultResponse("disabled",
                "Fallback SEFAZ desativado. Importacao via pasta do ERP e a fonte primaria.",
                config.getUltNsu(), config.getMaxNsu(), 0, 0);
        }
        CompanyConfig config = companyConfigService.getRequiredEntity();
        Instant agora = Instant.now();
        if (config.getProximaConsultaPermitida() != null && agora.isBefore(config.getProximaConsultaPermitida())) {
            return new SyncResultResponse("cooldown",
                "Intervalo mínimo da SEFAZ ainda não decorrido. Próxima consulta liberada em " + config.getProximaConsultaPermitida(),
                config.getUltNsu(), config.getMaxNsu(), 0, 0);
        }
        if (!emExecucao.compareAndSet(false, true)) {
            return new SyncResultResponse("em-execucao",
                "Já existe uma sincronização em andamento.",
                config.getUltNsu(), config.getMaxNsu(), 0, 0);
        }
        try {
            return executar(config);
        } finally {
            emExecucao.set(false);
        }
    }

    private SyncResultResponse executar(CompanyConfig config) {
        String ultNsu = config.getUltNsu();
        String maxNsu = config.getMaxNsu();
        String cstat = null;
        String motivo = null;
        int processados = 0;
        int consultas = 0;
        boolean limiteAtingido = false;

        int maxConsultas = Math.max(1, Math.min(appProperties.getSefazMaxConsultasPorExecucao(), 20));
        while (consultas < maxConsultas) {
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
                config.setUltNsu(ultNsu);
                config.setMaxNsu(maxNsu);

                if (compararNsu(ultNsu, maxNsu) >= 0) {
                    break; // alcançou o último documento disponível
                }
                if (consultas >= maxConsultas) {
                    limiteAtingido = true; // ainda há documentos; retomar logo após o intervalo
                }
            } else {
                // 137 (nada novo), 656 (consumo indevido) ou erro: para o laço
                config.setUltNsu(ultNsu);
                config.setMaxNsu(maxNsu);
                break;
            }
        }

        agendarProxima(config, cstat, limiteAtingido);
        companyConfigRepository.save(config);

        if (CSTAT_CONSUMO_INDEVIDO.equals(cstat)) {
            log.warn("SEFAZ retornou 656 (consumo indevido). Próxima consulta liberada em {}", config.getProximaConsultaPermitida());
        }
        return new SyncResultResponse(cstat, motivo, ultNsu, maxNsu, processados, consultas);
    }

    /**
     * Define o intervalo mínimo da SEFAZ e quando a próxima sincronização deve ocorrer.
     * <ul>
     *   <li>656 (consumo indevido): backoff exponencial (1h, 2h, 4h… até 12h) por reincidência;</li>
     *   <li>backlog pendente (atingiu o teto de consultas): retoma logo após o intervalo mínimo;</li>
     *   <li>caso normal (137 ou nada pendente): cadência de 6h.</li>
     * </ul>
     */
    private void agendarProxima(CompanyConfig config, String cstat, boolean limiteAtingido) {
        Instant agora = Instant.now();
        if (CSTAT_CONSUMO_INDEVIDO.equals(cstat)) {
            int bloqueios = config.getBloqueiosConsecutivos() + 1;
            config.setBloqueiosConsecutivos(bloqueios);
            Duration espera = backoff(bloqueios);
            config.setProximaConsultaPermitida(agora.plus(espera));
            config.setProximaSincronizacao(agora.plus(espera).plus(Duration.ofMinutes(1)));
            return;
        }

        // Qualquer resposta válida (137/138) zera o backoff.
        config.setBloqueiosConsecutivos(0);
        config.setProximaConsultaPermitida(agora.plus(INTERVALO_MINIMO));
        if (limiteAtingido) {
            config.setProximaSincronizacao(agora.plus(INTERVALO_MINIMO).plus(Duration.ofMinutes(1)));
        } else {
            config.setProximaSincronizacao(agora.plus(CADENCIA));
        }
    }

    /** Backoff exponencial: BASE × 2^(n-1), limitado a {@link #BACKOFF_MAX}. */
    private Duration backoff(int bloqueiosConsecutivos) {
        int expoente = Math.max(0, bloqueiosConsecutivos - 1);
        // limita o deslocamento para não estourar o long antes de aplicar o teto
        long fator = expoente >= 5 ? 32L : (1L << expoente);
        Duration espera = BACKOFF_BASE.multipliedBy(fator);
        return espera.compareTo(BACKOFF_MAX) > 0 ? BACKOFF_MAX : espera;
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
