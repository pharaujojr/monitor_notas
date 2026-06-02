package br.com.pharaujo.monitornfe.web;

import br.com.pharaujo.monitornfe.domain.NfeStatus;
import br.com.pharaujo.monitornfe.repository.NfeNoteRepository;
import br.com.pharaujo.monitornfe.service.NoteFilter;
import br.com.pharaujo.monitornfe.service.NoteQueryService;
import br.com.pharaujo.monitornfe.service.ResourceNotFoundException;
import br.com.pharaujo.monitornfe.service.StorageService;
import br.com.pharaujo.monitornfe.web.dto.NoteDetailResponse;
import br.com.pharaujo.monitornfe.web.dto.NoteSummaryResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteQueryService noteQueryService;
    private final NfeNoteRepository nfeNoteRepository;
    private final StorageService storageService;

    @GetMapping
    public Page<NoteSummaryResponse> list(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
        @RequestParam(required = false) NfeStatus status,
        @RequestParam(required = false) String emitenteNome,
        @RequestParam(required = false) String chave,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return noteQueryService.search(new NoteFilter(dataInicial, dataFinal, status, emitenteNome, chave), PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public NoteDetailResponse detail(@PathVariable Long id) {
        return noteQueryService.getById(id);
    }

    @GetMapping("/{id}/xml")
    public ResponseEntity<Resource> downloadXml(@PathVariable Long id) {
        String path = nfeNoteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("NF-e nao encontrada"))
            .getXmlStoragePath();
        if (path == null) {
            throw new ResourceNotFoundException("XML nao disponivel");
        }
        Resource resource = storageService.loadAsResource(path);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
            .contentType(MediaType.APPLICATION_XML)
            .body(resource);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long id) {
        String path = nfeNoteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("NF-e nao encontrada"))
            .getPdfStoragePath();
        if (path == null) {
            throw new ResourceNotFoundException("PDF nao disponivel");
        }
        Resource resource = storageService.loadAsResource(path);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(resource);
    }
}
