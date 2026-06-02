package br.com.pharaujo.monitornfe.web;

import br.com.pharaujo.monitornfe.service.SefazLogService;
import br.com.pharaujo.monitornfe.web.dto.SefazLogResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final SefazLogService sefazLogService;

    @GetMapping
    public List<SefazLogResponse> list() {
        return sefazLogService.listRecent();
    }
}
