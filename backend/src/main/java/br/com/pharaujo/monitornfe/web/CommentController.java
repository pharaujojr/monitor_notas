package br.com.pharaujo.monitornfe.web;

import br.com.pharaujo.monitornfe.domain.CommentAttachment;
import br.com.pharaujo.monitornfe.service.CommentService;
import br.com.pharaujo.monitornfe.service.StorageService;
import br.com.pharaujo.monitornfe.web.dto.CommentResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final StorageService storageService;

    @GetMapping("/api/notes/{noteId}/comments")
    public List<CommentResponse> list(@PathVariable Long noteId) {
        return commentService.list(noteId);
    }

    @PostMapping(value = "/api/notes/{noteId}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommentResponse create(
        @PathVariable Long noteId,
        @RequestParam(required = false) String autor,
        @RequestParam(required = false) String body,
        @RequestPart(value = "files", required = false) MultipartFile[] files
    ) {
        return commentService.create(noteId, autor, body, files);
    }

    @PutMapping("/api/comments/{commentId}")
    public CommentResponse update(
        @PathVariable Long commentId,
        @RequestParam(required = false) String autor,
        @RequestParam String body
    ) {
        return commentService.update(commentId, autor, body);
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long commentId) {
        commentService.delete(commentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/api/comments/{commentId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommentResponse addAttachments(
        @PathVariable Long commentId,
        @RequestPart("files") MultipartFile[] files
    ) {
        return commentService.addAttachments(commentId, files);
    }

    @DeleteMapping("/api/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId) {
        commentService.deleteAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/attachments/{attachmentId}")
    public ResponseEntity<Resource> download(@PathVariable Long attachmentId) {
        CommentAttachment attachment = commentService.getAttachment(attachmentId);
        Resource resource = storageService.loadAsResource(attachment.getStoragePath());
        String contentType = attachment.getContentType() == null
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
            : attachment.getContentType();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + attachment.getOriginalFilename() + "\"")
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource);
    }
}
