package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.ExportAudit;
import br.com.pharaujo.monitornfe.domain.ExportType;
import br.com.pharaujo.monitornfe.domain.NfeNote;
import br.com.pharaujo.monitornfe.repository.ExportAuditRepository;
import br.com.pharaujo.monitornfe.repository.NfeNoteRepository;
import br.com.pharaujo.monitornfe.web.dto.ExportRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final NfeNoteRepository nfeNoteRepository;
    private final ExportAuditRepository exportAuditRepository;
    private final StorageService storageService;

    @Transactional
    public Resource export(ExportType type, ExportRequest request) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
                LocalDateTime start = request.dataInicial() == null ? null : request.dataInicial().atStartOfDay();
                LocalDateTime end = request.dataFinal() == null ? null : request.dataFinal().plusDays(1).atStartOfDay();
                for (NfeNote note : nfeNoteRepository.search(request.status() == null ? null : request.status().name(), request.emitenteNome(), null, start, end, PageRequest.of(0, 500)).getContent()) {
                    String filePath = type == ExportType.XML ? note.getXmlStoragePath() : note.getPdfStoragePath();
                    if (filePath == null || filePath.isBlank()) {
                        continue;
                    }
                    Path path = Path.of(filePath);
                    if (!Files.exists(path)) {
                        continue;
                    }
                    zip.putNextEntry(new ZipEntry(path.getFileName().toString()));
                    zip.write(Files.readAllBytes(path));
                    zip.closeEntry();
                }
            }
            String generatedPath = storageService.storeExport(type.name().toLowerCase(), output.toByteArray());
            saveAudit(type, request, generatedPath);
            return storageService.loadAsResource(generatedPath);
        } catch (IOException exception) {
            throw new BadRequestException("Nao foi possivel gerar o ZIP de exportacao");
        }
    }

    private void saveAudit(ExportType type, ExportRequest request, String generatedPath) {
        ExportAudit audit = new ExportAudit();
        audit.setExportType(type);
        audit.setPeriodStart(request.dataInicial());
        audit.setPeriodEnd(request.dataFinal());
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("status", request.status());
        filters.put("emitenteNome", request.emitenteNome());
        audit.setFiltersJson(filters.toString());
        audit.setGeneratedFilePath(generatedPath);
        audit.setGeneratedAt(OffsetDateTime.now());
        exportAuditRepository.save(audit);
    }
}
