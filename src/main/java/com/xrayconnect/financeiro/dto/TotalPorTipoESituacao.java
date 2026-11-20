package com.xrayconnect.financeiro.dto;

import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import com.xrayconnect.financeiro.enumeracao.SituacaoLancamento;

import java.math.BigDecimal;

public record TotalPorTipoESituacao(
        TipoLancamento tipo,
        SituacaoLancamento situacao,
        BigDecimal total
) {
}