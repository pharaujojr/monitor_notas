package br.com.pharaujo.monitornfe.repository;

import br.com.pharaujo.monitornfe.domain.NoteStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteStatusHistoryRepository extends JpaRepository<NoteStatusHistory, Long> {
    List<NoteStatusHistory> findByNoteIdOrderByChangedAtDesc(Long noteId);
}
