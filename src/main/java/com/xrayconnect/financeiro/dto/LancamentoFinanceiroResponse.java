package com.xrayconnect.financeiro.dto;

import com.xrayconnect.financeiro.enumeracao.SituacaoLancamento;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LancamentoFinanceiroResponse(
        Long id,
        Long categoriaId,
        String categoriaNome,
        String descricao,
        String nomeContraparte,
        BigDecimal valor,
        TipoLancamento tipoLancamento,
        SituacaoLancamento situacaoLancamento,
        LocalDate dataVencimento,
        LocalDate dataLiquidacao,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}
