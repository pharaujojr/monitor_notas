package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.NfeEvent;
import br.com.pharaujo.monitornfe.domain.NfeNote;
import br.com.pharaujo.monitornfe.domain.NfeStatus;
import br.com.pharaujo.monitornfe.domain.NoteStatusHistory;
import br.com.pharaujo.monitornfe.repository.NfeEventRepository;
import br.com.pharaujo.monitornfe.repository.NfeNoteRepository;
import br.com.pharaujo.monitornfe.repository.NoteStatusHistoryRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoteLifecycleService {

    private final NfeNoteRepository nfeNoteRepository;
    private final NfeEventRepository nfeEventRepository;
    private final NoteStatusHistoryRepository noteStatusHistoryRepository;

    @Transactional
    public NfeNote save(NfeNote note) {
        NfeNote saved = nfeNoteRepository.save(note);
        ensureStatusHistory(saved, null, note.getStatus(), "Nota detectada ou atualizada");
        return saved;
    }

    @Transactional
    public NfeNote updateStatus(NfeNote note, NfeStatus newStatus, String reason) {
        NfeStatus previous = note.getStatus();
        note.setStatus(newStatus);
        NfeNote saved = nfeNoteRepository.save(note);
        ensureStatusHistory(saved, previous, newStatus, reason);
        return saved;
    }

    @Transactional
    public NfeEvent registerEvent(NfeNote note, String code, String name, String protocol, OffsetDateTime occurredAt, String details) {
        NfeEvent event = new NfeEvent();
        event.setNote(note);
        event.setEventCode(code);
        event.setEventName(name);
        event.setEventProtocol(protocol);
        event.setOccurredAt(occurredAt);
        event.setDetails(details);
        return nfeEventRepository.save(event);
    }

    private void ensureStatusHistory(NfeNote note, NfeStatus previous, NfeStatus current, String reason) {
        if (current == null) {
            return;
        }
        NoteStatusHistory history = new NoteStatusHistory();
        history.setNote(note);
        history.setPreviousStatus(previous);
        history.setNewStatus(current);
        history.setReason(reason);
        history.setChangedAt(OffsetDateTime.now());
        noteStatusHistoryRepository.save(history);
    }
}
