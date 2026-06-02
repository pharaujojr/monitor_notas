package br.com.pharaujo.monitornfe.web.dto;

import br.com.pharaujo.monitornfe.domain.CompanyStatus;
import br.com.pharaujo.monitornfe.domain.EnvironmentType;

public record CompanyConfigResponse(
    Long id,
    String cnpj,
    String razaoSocial,
    String uf,
    EnvironmentType ambiente,
    CompanyStatus status,
    String ultNsu,
    String maxNsu,
    CertificateResponse certificate
) {
}
