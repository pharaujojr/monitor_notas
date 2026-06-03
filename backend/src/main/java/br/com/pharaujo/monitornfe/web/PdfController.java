package br.com.pharaujo.monitornfe.web;

import br.com.pharaujo.monitornfe.service.PdfRegenerationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pdfs")
@RequiredArgsConstructor
public class PdfController {

    private final PdfRegenerationService pdfRegenerationService;

    @PostMapping("/regenerar")
    public Map<String, Integer> regenerar() {
        return Map.of("pdfsRegenerados", pdfRegenerationService.regenerateAll());
    }
}
