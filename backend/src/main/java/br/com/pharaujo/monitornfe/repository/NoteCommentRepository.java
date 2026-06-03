package br.com.pharaujo.monitornfe.repository;

import br.com.pharaujo.monitornfe.domain.NoteComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteCommentRepository extends JpaRepository<NoteComment, Long> {
    List<NoteComment> findByNoteIdOrderByCreatedAtAsc(Long noteId);
}
