package com.xrayconnect.financeiro.controller;

import com.xrayconnect.financeiro.dto.LancamentoFinanceiroRequest;
import com.xrayconnect.financeiro.dto.LancamentoFinanceiroResponse;
import com.xrayconnect.financeiro.dto.LiquidacaoRequest;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import com.xrayconnect.financeiro.servico.LancamentoFinanceiroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lancamentos")
@RequiredArgsConstructor
public class LancamentoFinanceiroController {

    private final LancamentoFinanceiroService lancamentoFinanceiroService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoFinanceiroResponse postLancamento(@RequestBody @Valid LancamentoFinanceiroRequest dados) {
        return lancamentoFinanceiroService.cadastrarLancamentoFinanceiro(dados);
    }

    @GetMapping
    public Page<LancamentoFinanceiroResponse> getAllLancamento(
            Pageable pageable,
            @RequestParam(required = false) TipoLancamento tipo
    ) {
        return lancamentoFinanceiroService.listar(pageable, tipo);
    }

    @GetMapping("/{id}")
    public LancamentoFinanceiroResponse getLancamento(@PathVariable Long id) {
        return lancamentoFinanceiroService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public LancamentoFinanceiroResponse putLancamento(@PathVariable Long id, @RequestBody @Valid LancamentoFinanceiroRequest dados) {
        return lancamentoFinanceiroService.atualizarLancamentoFinanceiro(id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLancamento(@PathVariable Long id) {
        lancamentoFinanceiroService.excluirLancamentoFinanceiro(id);
    }

    @PatchMapping("/{id}/liquidar")
    public LancamentoFinanceiroResponse patchLancamento(@PathVariable Long id, @RequestBody @Valid LiquidacaoRequest dados) {
        return lancamentoFinanceiroService.liquidarLancamento(id, dados.dataLiquidacao());
    }
}
