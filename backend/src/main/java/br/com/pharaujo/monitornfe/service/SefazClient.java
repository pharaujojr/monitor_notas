package br.com.pharaujo.monitornfe.service;

import br.com.pharaujo.monitornfe.domain.CompanyConfig;
import br.com.pharaujo.monitornfe.domain.EnvironmentType;
import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.nfe.Nfe;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.dom.enuns.AmbienteEnum;
import br.com.swconsultoria.nfe.dom.enuns.ConsultaDFeEnum;
import br.com.swconsultoria.nfe.dom.enuns.EstadosEnum;
import br.com.swconsultoria.nfe.dom.enuns.PessoaEnum;
import br.com.swconsultoria.nfe.schema.retdistdfeint.RetDistDFeInt;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Cliente do serviço NFeDistribuicaoDFe (Ambiente Nacional) via biblioteca Java_NFe.
 * Usa o certificado A1 da empresa como credencial mTLS no handshake TLS com a SEFAZ.
 */
@Slf4j
@Service
public class SefazClient {

    @Value("${CERT_PASSWORD:}")
    private String certPassword;

    /**
     * Consulta documentos fiscais a partir do último NSU consumido.
     *
     * @param config empresa monitorada (CNPJ, UF, ambiente e certificado)
     * @param ultNsu último NSU já processado ("0" para a primeira carga)
     * @return resposta bruta da SEFAZ (cStat, ultNSU, maxNSU e lote de documentos)
     */
    public RetDistDFeInt consultarPorNsu(CompanyConfig config, String ultNsu) {
        if (config.getCertificate() == null || config.getCertificate().getStoragePath() == null) {
            throw new BadRequestException("Certificado A1 não configurado para a empresa");
        }
        if (certPassword == null || certPassword.isBlank()) {
            throw new BadRequestException("CERT_PASSWORD não configurada no ambiente");
        }
        try {
            byte[] pfx = Files.readAllBytes(Path.of(config.getCertificate().getStoragePath()));
            Certificado certificado = CertificadoService.certificadoPfxBytes(pfx, certPassword);
            CertificadoService.inicializaCertificado(certificado);

            AmbienteEnum ambiente = config.getAmbiente() == EnvironmentType.PRODUCAO
                ? AmbienteEnum.PRODUCAO
                : AmbienteEnum.HOMOLOGACAO;
            EstadosEnum estado = EstadosEnum.valueOf(config.getUf());

            // pastaSchemas null: distribuicaoDFe é consulta, não assina nem valida XML local
            ConfiguracoesNfe configuracoes = ConfiguracoesNfe.criarConfiguracoes(estado, ambiente, certificado, null);

            // o schema da SEFAZ exige o NSU com 15 dígitos e zeros à esquerda
            String nsu = String.format("%015d", parseNsu(ultNsu));
            return Nfe.distribuicaoDfe(configuracoes, PessoaEnum.JURIDICA, config.getCnpj(), ConsultaDFeEnum.NSU, nsu);
        } catch (Exception exception) {
            log.error("Falha na consulta DistribuicaoDFe (ultNSU={}): {}", ultNsu, exception.getMessage());
            throw new SefazIntegrationException("Falha na consulta DistribuicaoDFe: " + exception.getMessage(), exception);
        }
    }

    private long parseNsu(String value) {
        try {
            return Long.parseLong(value == null ? "0" : value.trim());
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }
}
