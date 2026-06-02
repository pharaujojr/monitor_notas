package br.com.pharaujo.monitornfe.web;

import br.com.pharaujo.monitornfe.service.CompanyConfigService;
import br.com.pharaujo.monitornfe.web.dto.CompanyConfigRequest;
import br.com.pharaujo.monitornfe.web.dto.CompanyConfigResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/settings/company")
@RequiredArgsConstructor
public class CompanyConfigController {

    private final CompanyConfigService companyConfigService;

    @GetMapping
    public CompanyConfigResponse getCurrent() {
        return companyConfigService.getCurrent();
    }

    @PutMapping
    public CompanyConfigResponse save(@Valid @org.springframework.web.bind.annotation.RequestBody CompanyConfigRequest request) {
        return companyConfigService.save(request);
    }

    @PostMapping(value = "/certificate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompanyConfigResponse uploadCertificate(@RequestPart("file") MultipartFile file) {
        return companyConfigService.uploadCertificate(file);
    }
}
