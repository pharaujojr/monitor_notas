package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.CommentAttachment;
import br.com.pharaujo.monitornfe.domain.NfeNote;
import br.com.pharaujo.monitornfe.domain.NoteComment;
import br.com.pharaujo.monitornfe.repository.CommentAttachmentRepository;
import br.com.pharaujo.monitornfe.repository.NfeNoteRepository;
import br.com.pharaujo.monitornfe.repository.NoteCommentRepository;
import br.com.pharaujo.monitornfe.web.dto.AttachmentResponse;
import br.com.pharaujo.monitornfe.web.dto.CommentResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final long MAX_ATTACHMENT_BYTES = 25 * 1024 * 1024L;

    private final NoteCommentRepository noteCommentRepository;
    private final CommentAttachmentRepository commentAttachmentRepository;
    private final NfeNoteRepository nfeNoteRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long noteId) {
        return noteCommentRepository.findByNoteIdOrderByCreatedAtAsc(noteId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public CommentResponse create(Long noteId, String autor, String body, MultipartFile[] files) {
        NfeNote note = nfeNoteRepository.findById(noteId)
            .orElseThrow(() -> new ResourceNotFoundException("NF-e nao encontrada"));
        boolean temArquivos = files != null && hasContent(files);
        if ((body == null || body.isBlank()) && !temArquivos) {
            throw new BadRequestException("Informe um texto ou ao menos um anexo");
        }
        NoteComment comment = new NoteComment();
        comment.setNote(note);
        comment.setAutor(defaultAutor(autor));
        comment.setBody(body == null ? "" : body.trim());
        comment = noteCommentRepository.save(comment);
        anexar(comment, files);
        return toResponse(noteCommentRepository.save(comment));
    }

    @Transactional
    public CommentResponse update(Long commentId, String autor, String body) {
        NoteComment comment = getComment(commentId);
        if (body == null || body.isBlank()) {
            throw new BadRequestException("O texto do comentario nao pode ficar vazio");
        }
        comment.setBody(body.trim());
        if (autor != null && !autor.isBlank()) {
            comment.setAutor(autor.trim());
        }
        return toResponse(noteCommentRepository.save(comment));
    }

    @Transactional
    public void delete(Long commentId) {
        NoteComment comment = getComment(commentId);
        comment.getAttachments().forEach(a -> storageService.deleteQuietly(a.getStoragePath()));
        noteCommentRepository.delete(comment);
    }

    @Transactional
    public CommentResponse addAttachments(Long commentId, MultipartFile[] files) {
        NoteComment comment = getComment(commentId);
        if (files == null || !hasContent(files)) {
            throw new BadRequestException("Nenhum arquivo enviado");
        }
        anexar(comment, files);
        return toResponse(noteCommentRepository.save(comment));
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) {
        CommentAttachment attachment = commentAttachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Anexo nao encontrado"));
        storageService.deleteQuietly(attachment.getStoragePath());
        commentAttachmentRepository.delete(attachment);
    }

    @Transactional(readOnly = true)
    public CommentAttachment getAttachment(Long attachmentId) {
        return commentAttachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Anexo nao encontrado"));
    }

    private void anexar(NoteComment comment, MultipartFile[] files) {
        if (files == null) {
            return;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (file.getSize() > MAX_ATTACHMENT_BYTES) {
                throw new BadRequestException("Anexo excede o limite de 25MB: " + file.getOriginalFilename());
            }
            try {
                String path = storageService.storeCommentAttachment(
                    comment.getNote().getId(), file.getOriginalFilename(), file.getBytes());
                CommentAttachment attachment = new CommentAttachment();
                attachment.setComment(comment);
                attachment.setOriginalFilename(file.getOriginalFilename());
                attachment.setStoragePath(path);
                attachment.setContentType(file.getContentType());
                attachment.setFileSizeBytes(file.getSize());
                comment.getAttachments().add(attachment);
            } catch (IOException exception) {
                throw new BadRequestException("Falha ao ler o anexo: " + file.getOriginalFilename());
            }
        }
    }

    private NoteComment getComment(Long commentId) {
        return noteCommentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comentario nao encontrado"));
    }

    private boolean hasContent(MultipartFile[] files) {
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private String defaultAutor(String autor) {
        return autor == null || autor.isBlank() ? "Operador" : autor.trim();
    }

    private CommentResponse toResponse(NoteComment comment) {
        boolean editado = comment.getCreatedAt() != null && comment.getUpdatedAt() != null
            && comment.getUpdatedAt().isAfter(comment.getCreatedAt().plus(Duration.ofSeconds(1)));
        List<AttachmentResponse> anexos = comment.getAttachments().stream()
            .map(a -> new AttachmentResponse(
                a.getId(),
                a.getOriginalFilename(),
                a.getContentType(),
                a.getFileSizeBytes(),
                "/api/attachments/" + a.getId()))
            .toList();
        return new CommentResponse(
            comment.getId(),
            comment.getAutor(),
            comment.getBody(),
            comment.getCreatedAt(),
            comment.getUpdatedAt(),
            editado,
            anexos
        );
    }
}
