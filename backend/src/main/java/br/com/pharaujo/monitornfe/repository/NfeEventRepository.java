package br.com.pharaujo.monitornfe.repository;

import br.com.pharaujo.monitornfe.domain.NfeEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NfeEventRepository extends JpaRepository<NfeEvent, Long> {
    List<NfeEvent> findByNoteIdOrderByOccurredAtDesc(Long noteId);
}
