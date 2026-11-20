package com.xrayconnect.financeiro.controller;

import com.xrayconnect.financeiro.dto.DashboardResponse;
import com.xrayconnect.financeiro.dto.FluxoCaixaResponse;
import com.xrayconnect.financeiro.servico.RelatorioFinanceiroService;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
public class RelatorioFinanceiroController {

    private final RelatorioFinanceiroService servico;

    @GetMapping("/dashboard")
    public DashboardResponse obterDashboard() {
        return servico.obterDashboard();
    }

    @GetMapping("/fluxo-caixa")
    public FluxoCaixaResponse obterFluxoCaixa(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate inicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fim
    ) {
        return servico.obterFluxoCaixa(inicio, fim);
    }
}