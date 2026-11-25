package com.xrayconnect.financeiro.servico;

import com.xrayconnect.financeiro.dto.LancamentoFinanceiroRequest;
import com.xrayconnect.financeiro.dto.LancamentoFinanceiroResponse;
import com.xrayconnect.financeiro.entidade.LancamentoFinanceiro;
import com.xrayconnect.financeiro.enumeracao.SituacaoLancamento;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import com.xrayconnect.financeiro.exception.ConflitoNegocioException;
import com.xrayconnect.financeiro.exception.RecursoNaoEncontradoException;
import com.xrayconnect.financeiro.helper.MapeadorLancamentoFinanceiro;
import com.xrayconnect.financeiro.repositorio.CategoriaRepository;
import com.xrayconnect.financeiro.repositorio.LancamentoFinanceiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LancamentoFinanceiroService {

    private final CategoriaRepository categoriaRepository;
    private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;

    @Transactional
    public LancamentoFinanceiroResponse cadastrarLancamentoFinanceiro(LancamentoFinanceiroRequest dados) {
        var categoria = categoriaRepository.findById(dados.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("A categoria não foi encontrada."));

        var lancamento = lancamentoFinanceiroRepository.save(new LancamentoFinanceiro(
                categoria,
                dados.descricao(),
                dados.nomeContraparte(),
                dados.valor(),
                dados.tipoLancamento(),
                dados.dataVencimento()
        ));

        return MapeadorLancamentoFinanceiro.paraDtoResponse(lancamento);
    }

    @Transactional(readOnly = true)
    public LancamentoFinanceiroResponse buscarPorId(Long id) {
        var lancamento = lancamentoFinanceiroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("O lançamento financeiro não foi encontrado."));

        return MapeadorLancamentoFinanceiro.paraDtoResponse(lancamento);
    }

    @Transactional(readOnly = true)
    public Page<LancamentoFinanceiroResponse> listar(
            Pageable pageable,
            TipoLancamento tipo
    ) {
        Page<LancamentoFinanceiro> lancamentos;

        if (tipo != null) {
            lancamentos = lancamentoFinanceiroRepository
                    .findByTipoLancamento(tipo, pageable);
        } else {
            lancamentos = lancamentoFinanceiroRepository
                    .findAll(pageable);
        }

        return lancamentos.map(
                MapeadorLancamentoFinanceiro::paraDtoResponse
        );
    }

    @Transactional
    public LancamentoFinanceiroResponse atualizarLancamentoFinanceiro(Long id, LancamentoFinanceiroRequest dados) {
        var lancamento = lancamentoFinanceiroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lançamento Financeiro não encontrado."));

        var categoria = categoriaRepository.findById(dados.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));

        lancamento.atualizar(
                categoria,
                dados.descricao(),
                dados.nomeContraparte(),
                dados.valor(),
                dados.tipoLancamento(),
                dados.dataVencimento()
        );

        return MapeadorLancamentoFinanceiro.paraDtoResponse(lancamento);
    }

    @Transactional
    public void excluirLancamentoFinanceiro(Long id) {
        var lancamento = lancamentoFinanceiroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lançamento Financeiro não encontrado"));

        if (lancamento.getSituacaoLancamento() != SituacaoLancamento.PENDENTE) {
            throw new ConflitoNegocioException("Apenas lançamentos pendentes podem ser excluídos.");
        }

        lancamentoFinanceiroRepository.delete(lancamento);
    }

    @Transactional
    public LancamentoFinanceiroResponse liquidarLancamento(
            Long id,
            LocalDate dataLiquidacao,
            BigDecimal valorLiquidado) {
        var lancamento = lancamentoFinanceiroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lançamento financeiro não encontrado"));

        lancamento.liquidar(dataLiquidacao, valorLiquidado);

        return MapeadorLancamentoFinanceiro.paraDtoResponse(lancamento);
    }
}
