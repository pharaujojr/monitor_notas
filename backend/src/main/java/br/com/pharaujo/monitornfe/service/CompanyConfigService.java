package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.CertificateRecord;
import br.com.pharaujo.monitornfe.domain.CompanyConfig;
import br.com.pharaujo.monitornfe.repository.CompanyConfigRepository;
import br.com.pharaujo.monitornfe.web.dto.CertificateResponse;
import br.com.pharaujo.monitornfe.web.dto.CompanyConfigRequest;
import br.com.pharaujo.monitornfe.web.dto.CompanyConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CompanyConfigService {

    private final CompanyConfigRepository companyConfigRepository;
    private final CertificateStorageService certificateStorageService;

    @Transactional(readOnly = true)
    public CompanyConfigResponse getCurrent() {
        return companyConfigRepository.findTopByOrderByIdAsc()
            .map(this::toResponse)
            .orElse(null);
    }

    @Transactional
    public CompanyConfigResponse save(CompanyConfigRequest request) {
        CompanyConfig entity = companyConfigRepository.findTopByOrderByIdAsc().orElseGet(CompanyConfig::new);
        entity.setCnpj(request.cnpj());
        entity.setRazaoSocial(request.razaoSocial());
        entity.setUf(request.uf());
        entity.setAmbiente(request.ambiente());
        entity.setStatus(request.status());
        return toResponse(companyConfigRepository.save(entity));
    }

    @Transactional
    public CompanyConfigResponse uploadCertificate(MultipartFile file) {
        CompanyConfig config = companyConfigRepository.findTopByOrderByIdAsc()
            .orElseThrow(() -> new BadRequestException("Cadastre a empresa antes de enviar o certificado"));
        CertificateRecord record = certificateStorageService.store(file);
        config.setCertificate(record);
        return toResponse(companyConfigRepository.save(config));
    }

    @Transactional(readOnly = true)
    public CompanyConfig getRequiredEntity() {
        return companyConfigRepository.findTopByOrderByIdAsc()
            .orElseThrow(() -> new ResourceNotFoundException("Configuracao da empresa nao encontrada"));
    }

    public CompanyConfigResponse toResponse(CompanyConfig entity) {
        return new CompanyConfigResponse(
            entity.getId(),
            entity.getCnpj(),
            entity.getRazaoSocial(),
            entity.getUf(),
            entity.getAmbiente(),
            entity.getStatus(),
            entity.getUltNsu(),
            entity.getMaxNsu(),
            entity.getCertificate() == null ? null : new CertificateResponse(
                entity.getCertificate().getId(),
                entity.getCertificate().getOriginalFilename(),
                entity.getCertificate().getFileSizeBytes(),
                entity.getCertificate().getValidFrom(),
                entity.getCertificate().getValidTo(),
                entity.getCertificate().getUploadedAt()
            )
        );
    }
}
