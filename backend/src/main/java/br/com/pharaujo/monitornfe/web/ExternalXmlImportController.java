package br.com.pharaujo.monitornfe.web;

import br.com.pharaujo.monitornfe.service.ExternalXmlImportService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/external-xml")
@RequiredArgsConstructor
public class ExternalXmlImportController {

    private final ExternalXmlImportService externalXmlImportService;

    @PostMapping("/importar")
    public Map<String, Integer> importar() {
        return Map.of("documentosImportados", externalXmlImportService.importar());
    }
}
