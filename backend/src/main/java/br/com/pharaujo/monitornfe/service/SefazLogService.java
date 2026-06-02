package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.SefazQueryLog;
import br.com.pharaujo.monitornfe.repository.SefazQueryLogRepository;
import br.com.pharaujo.monitornfe.web.dto.SefazLogResponse;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SefazLogService {

    private final SefazQueryLogRepository sefazQueryLogRepository;

    @Transactional(readOnly = true)
    public List<SefazLogResponse> listRecent() {
        return sefazQueryLogRepository.findTop50ByOrderByOccurredAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public void log(String cnpj, String ambiente, String nsuInicial, String nsuFinal, String cstat, String motivo, String errorMessage) {
        SefazQueryLog log = new SefazQueryLog();
        log.setCnpj(cnpj);
        log.setAmbiente(ambiente);
        log.setNsuInicial(nsuInicial);
        log.setNsuFinal(nsuFinal);
        log.setCstat(cstat);
        log.setMotivo(motivo);
        log.setOccurredAt(OffsetDateTime.now());
        log.setErrorMessage(errorMessage);
        sefazQueryLogRepository.save(log);
    }

    private SefazLogResponse toResponse(SefazQueryLog entity) {
        return new SefazLogResponse(
            entity.getId(),
            entity.getCnpj(),
            entity.getAmbiente(),
            entity.getNsuInicial(),
            entity.getNsuFinal(),
            entity.getCstat(),
            entity.getMotivo(),
            entity.getOccurredAt(),
            entity.getErrorMessage()
        );
    }
}
