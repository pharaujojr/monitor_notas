package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.NfeStatus;
import br.com.pharaujo.monitornfe.repository.NfeNoteRepository;
import br.com.pharaujo.monitornfe.web.dto.DashboardResponse;
import br.com.pharaujo.monitornfe.web.dto.NoteSummaryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final NfeNoteRepository nfeNoteRepository;
    private final NoteQueryService noteQueryService;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        List<NoteSummaryResponse> latest = nfeNoteRepository.findTop10ByOrderByDataEmissaoDesc()
            .stream()
            .map(noteQueryService::toSummary)
            .toList();
        return new DashboardResponse(
            nfeNoteRepository.count(),
            nfeNoteRepository.countByStatus(NfeStatus.MANIFESTADA_EXTERNAMENTE)
                + nfeNoteRepository.countByStatus(NfeStatus.XML_DISPONIVEL),
            nfeNoteRepository.countByStatus(NfeStatus.XML_BAIXADO),
            nfeNoteRepository.countByStatus(NfeStatus.PDF_GERADO),
            nfeNoteRepository.countByStatus(NfeStatus.CANCELADA),
            latest
        );
    }
}
