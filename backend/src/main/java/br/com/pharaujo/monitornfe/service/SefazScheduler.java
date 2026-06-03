package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.CompanyConfig;
import br.com.pharaujo.monitornfe.domain.CompanyStatus;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara a sincronização com a SEFAZ de forma autônoma. Fica em um bean separado
 * (e não dentro do {@link SefazDistributionService}) para que a chamada a
 * {@code sincronizar()} passe pelo proxy do Spring e o {@code @Transactional} seja
 * efetivamente aplicado.
 *
 * A decisão de "quando consultar" mora nas datas persistidas em company_configs,
 * portanto o agendamento sobrevive a reinícios do servidor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SefazScheduler {

    private final CompanyConfigService companyConfigService;
    private final SefazDistributionService sefazDistributionService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void tick() {
        try {
            CompanyConfig config = companyConfigService.getCurrent() == null ? null : companyConfigService.getRequiredEntity();
            if (config == null || config.getStatus() != CompanyStatus.ATIVO) {
                return;
            }
            Instant agora = Instant.now();
            if (config.getProximaConsultaPermitida() != null && agora.isBefore(config.getProximaConsultaPermitida())) {
                return;
            }
            if (config.getProximaSincronizacao() != null && agora.isBefore(config.getProximaSincronizacao())) {
                return;
            }
            sefazDistributionService.sincronizar();
        } catch (Exception exception) {
            log.warn("Falha ao executar sincronização agendada: {}", exception.getMessage());
        }
    }
}
