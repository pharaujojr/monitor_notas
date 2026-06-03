package br.com.pharaujo.monitornfe.repository;

import br.com.pharaujo.monitornfe.domain.CommentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentAttachmentRepository extends JpaRepository<CommentAttachment, Long> {
}
