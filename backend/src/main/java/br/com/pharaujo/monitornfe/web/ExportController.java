package br.com.pharaujo.monitornfe.web;

import br.com.pharaujo.monitornfe.domain.ExportType;
import br.com.pharaujo.monitornfe.service.ExportService;
import br.com.pharaujo.monitornfe.web.dto.ExportRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @PostMapping("/xml")
    public ResponseEntity<Resource> exportXml(@RequestBody ExportRequest request) {
        return export(ExportType.XML, request, "xml-export.zip");
    }

    @PostMapping("/pdf")
    public ResponseEntity<Resource> exportPdf(@RequestBody ExportRequest request) {
        return export(ExportType.PDF, request, "pdf-export.zip");
    }

    private ResponseEntity<Resource> export(ExportType type, ExportRequest request, String filename) {
        Resource resource = exportService.export(type, request);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }
}
