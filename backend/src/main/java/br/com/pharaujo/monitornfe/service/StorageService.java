package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.config.AppProperties;
import br.com.pharaujo.monitornfe.domain.NfeNote;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StorageService {

    private static final DateTimeFormatter YEAR_FORMAT = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MM");

    private final AppProperties appProperties;

    public String storeXml(NfeNote note, String xmlContent) {
        return writeForNote(note, "xml", ".xml", xmlContent.getBytes(StandardCharsets.UTF_8));
    }

    public String storePdf(NfeNote note, byte[] pdfBytes) {
        return writeForNote(note, "pdf", ".pdf", pdfBytes);
    }

    public String storeExport(String prefix, byte[] bytes) {
        try {
            Path dir = Path.of(appProperties.getStoragePath(), "exports").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path file = dir.resolve(prefix + "-" + UUID.randomUUID() + ".zip");
            Files.write(file, bytes);
            return file.toString();
        } catch (IOException exception) {
            throw new BadRequestException("Nao foi possivel gerar o arquivo de exportacao");
        }
    }

    public Resource loadAsResource(String filePath) {
        Path path = Path.of(filePath).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Arquivo nao encontrado");
        }
        return new FileSystemResource(path);
    }

    /** Armazena um anexo de comentário em disco, organizado por nota. */
    public String storeCommentAttachment(Long noteId, String originalFilename, byte[] content) {
        try {
            Path dir = Path.of(appProperties.getStoragePath(), "comments", String.valueOf(noteId))
                .toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String safe = (originalFilename == null || originalFilename.isBlank() ? "arquivo" : originalFilename)
                .replaceAll("[^a-zA-Z0-9._-]", "_");
            Path file = dir.resolve(UUID.randomUUID() + "-" + safe);
            Files.write(file, content);
            return file.toString();
        } catch (IOException exception) {
            throw new BadRequestException("Nao foi possivel armazenar o anexo do comentario");
        }
    }

    /** Remove um arquivo do disco; silencioso se já não existir. */
    public void deleteQuietly(String filePath) {
        if (filePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(filePath).toAbsolutePath().normalize());
        } catch (IOException ignored) {
            // melhor esforço: a remoção do registro no banco é a fonte de verdade
        }
    }

    private String writeForNote(NfeNote note, String folder, String extension, byte[] content) {
        try {
            Path path = Path.of(
                appProperties.getStoragePath(),
                note.getDestinatarioCnpj(),
                note.getDataEmissao().format(YEAR_FORMAT),
                note.getDataEmissao().format(MONTH_FORMAT),
                note.getChaveAcesso(),
                folder
            ).toAbsolutePath().normalize();
            Files.createDirectories(path);
            Path file = path.resolve(note.getChaveAcesso() + extension);
            Files.write(file, content);
            return file.toString();
        } catch (IOException exception) {
            throw new BadRequestException("Nao foi possivel persistir o arquivo da NF-e");
        }
    }
}
