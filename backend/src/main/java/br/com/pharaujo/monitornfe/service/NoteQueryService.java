package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.NfeEvent;
import br.com.pharaujo.monitornfe.domain.NfeNote;
import br.com.pharaujo.monitornfe.domain.NoteStatusHistory;
import br.com.pharaujo.monitornfe.repository.NfeEventRepository;
import br.com.pharaujo.monitornfe.repository.NfeNoteRepository;
import br.com.pharaujo.monitornfe.repository.NoteStatusHistoryRepository;
import br.com.pharaujo.monitornfe.web.dto.NfeEventResponse;
import br.com.pharaujo.monitornfe.web.dto.NoteDetailResponse;
import br.com.pharaujo.monitornfe.web.dto.NoteSummaryResponse;
import br.com.pharaujo.monitornfe.web.dto.StatusHistoryResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoteQueryService {

    private final NfeNoteRepository nfeNoteRepository;
    private final NfeEventRepository nfeEventRepository;
    private final NoteStatusHistoryRepository noteStatusHistoryRepository;

    @Transactional(readOnly = true)
    public Page<NoteSummaryResponse> search(NoteFilter filter, Pageable pageable) {
        LocalDateTime start = filter.dataInicial() == null ? null : filter.dataInicial().atStartOfDay();
        LocalDateTime end = filter.dataFinal() == null ? null : filter.dataFinal().plusDays(1).atStartOfDay();
        return nfeNoteRepository.search(
            filter.status(),
            blankToNull(filter.emitenteNome()),
            blankToNull(filter.chave()),
            start,
            end,
            pageable
        ).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public NoteDetailResponse getById(Long id) {
        NfeNote note = nfeNoteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("NF-e nao encontrada"));
        List<NfeEventResponse> events = nfeEventRepository.findByNoteIdOrderByOccurredAtDesc(id)
            .stream()
            .map(this::toEventResponse)
            .toList();
        List<StatusHistoryResponse> history = noteStatusHistoryRepository.findByNoteIdOrderByChangedAtDesc(id)
            .stream()
            .map(this::toHistoryResponse)
            .toList();
        return new NoteDetailResponse(
            note.getId(),
            note.getChaveAcesso(),
            note.getNsu(),
            note.getModelo(),
            note.getEmitenteCnpj(),
            note.getEmitenteNome(),
            note.getDestinatarioCnpj(),
            note.getDataEmissao(),
            note.getValorTotal(),
            note.getStatus(),
            note.getXmlStoragePath(),
            note.getPdfStoragePath(),
            note.getXmlDownloadedAt(),
            note.getPdfGeneratedAt(),
            events,
            history
        );
    }

    public NoteSummaryResponse toSummary(NfeNote note) {
        return new NoteSummaryResponse(
            note.getId(),
            note.getChaveAcesso(),
            note.getEmitenteNome(),
            note.getEmitenteCnpj(),
            note.getDataEmissao(),
            note.getValorTotal(),
            note.getStatus()
        );
    }

    private NfeEventResponse toEventResponse(NfeEvent event) {
        return new NfeEventResponse(
            event.getId(),
            event.getEventCode(),
            event.getEventName(),
            event.getEventProtocol(),
            event.getOccurredAt(),
            event.getDetails()
        );
    }

    private StatusHistoryResponse toHistoryResponse(NoteStatusHistory history) {
        return new StatusHistoryResponse(
            history.getId(),
            history.getPreviousStatus(),
            history.getNewStatus(),
            history.getChangedAt(),
            history.getReason()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
