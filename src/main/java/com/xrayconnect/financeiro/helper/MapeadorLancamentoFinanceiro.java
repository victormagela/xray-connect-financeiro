package com.xrayconnect.financeiro.helper;

import com.xrayconnect.financeiro.dto.LancamentoFinanceiroResponse;
import com.xrayconnect.financeiro.entidade.LancamentoFinanceiro;

public class MapeadorLancamentoFinanceiro {
    public static LancamentoFinanceiroResponse paraDtoResponse(LancamentoFinanceiro lancamento) {
        return new LancamentoFinanceiroResponse(
                lancamento.getId(),
                lancamento.getCategoria().getId(),
                lancamento.getCategoria().getNome(),
                lancamento.getDescricao(),
                lancamento.getNomeContraparte(),
                lancamento.getValor(),
                lancamento.getTipoLancamento(),
                lancamento.getSituacaoLancamento(),
                lancamento.getDataVencimento(),
                lancamento.getDataLiquidacao(),
                lancamento.getCriadoEm(),
                lancamento.getAtualizadoEm(),
                lancamento.getValorLiquidado()
        );
    }
}
