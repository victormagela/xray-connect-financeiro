/**
 * Fluxo de Caixa — X-Ray Connect.
 * Permite alternar entre movimentações realizadas (data de liquidação e valor efetivo)
 * e projetadas (data de vencimento e valor previsto). O back-end faz os cálculos.
 */
import api from '../api.js';
import { formatarMoeda, formatarMes, formatarData, mensagemDeErro, escaparHtml,
  inicioMesISO, fimMesISO } from '../utils.js';

/** Retorna o primeiro dia do mês de n-1 meses atrás e o último dia deste mês. */
export function periodoUltimosMeses(quantidade = 6, referencia = new Date()) {
  const inicio = new Date(referencia.getFullYear(), referencia.getMonth() - (quantidade - 1), 1);
  const ano = inicio.getFullYear();
  const mes = String(inicio.getMonth() + 1).padStart(2, '0');
  return { inicio: `${ano}-${mes}-01`, fim: fimMesDaData(referencia) };
}

function fimMesDaData(data) {
  const ultimoDia = new Date(data.getFullYear(), data.getMonth() + 1, 0).getDate();
  return `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(ultimoDia).padStart(2, '0')}`;
}

/** Gera apenas o HTML estrutural; nenhum dado financeiro é simulado. */
export function renderFluxoCaixa() {
  return `
    <div class="fc-page">
      <div class="lf-toolbar">
        <div>
          <p class="lf-eyebrow" data-fc-mode-eyebrow>RELATÓRIO FINANCEIRO · REALIZADO</p>
          <p class="lf-toolbar-description" data-fc-intro>Analise os valores efetivamente recebidos e pagos no período selecionado.</p>
        </div>
        <button type="button" class="button" data-fc-refresh>Atualizar dados</button>
      </div>

      <fieldset class="fc-mode-switch" aria-label="Modalidade do fluxo de caixa">
        <legend>Tipo de fluxo</legend>
        <label class="fc-mode-choice"><input type="radio" name="fcModo" value="realizado" checked> <span>Realizado</span></label>
        <label class="fc-mode-choice"><input type="radio" name="fcModo" value="projetado"> <span>Projetado</span></label>
      </fieldset>

      <section class="panel fc-filters" aria-label="Filtros do fluxo de caixa">
        <div class="panel-header">
          <div>
            <h2 class="panel-title">Selecionar período</h2>
            <p class="panel-subtitle" data-fc-dates-info>As datas são inclusivas e consideram a liquidação dos lançamentos.</p>
          </div>
        </div>
        <div class="panel-content">
          <form data-fc-form class="fc-filter-form">
            <div class="lf-field">
              <label for="fc-inicio">Data inicial <span aria-hidden="true">*</span></label>
              <input id="fc-inicio" data-fc-inicio name="inicio" type="date" required>
            </div>
            <div class="lf-field">
              <label for="fc-fim">Data final <span aria-hidden="true">*</span></label>
              <input id="fc-fim" data-fc-fim name="fim" type="date" required>
            </div>
            <button type="submit" class="button primary fc-apply" data-fc-apply>Aplicar filtro</button>
          </form>
          <div class="fc-presets" aria-label="Períodos rápidos">
            <span>Atalhos:</span>
            <button type="button" class="fc-preset" data-fc-preset="mes">Mês atual</button>
            <button type="button" class="fc-preset" data-fc-preset="seis">Últimos 6 meses</button>
            <button type="button" class="fc-preset" data-fc-preset="ano">Ano atual</button>
          </div>
        </div>
      </section>

      <div data-fc-feedback class="lf-feedback lf-feedback-error" role="alert" aria-live="assertive" hidden></div>

      <section class="metric-grid fc-metrics" aria-label="Totais do período">
        <article class="metric-card">
          <div class="metric-label" data-fc-receitas-label>Receitas realizadas</div>
          <div class="metric-number" data-fc-valor="totalReceitas">—</div>
          <p class="metric-note" data-fc-receitas-note>Valores recebidos dentro do período</p>
        </article>
        <article class="metric-card accent">
          <div class="metric-label" data-fc-despesas-label>Despesas realizadas</div>
          <div class="metric-number" data-fc-valor="totalDespesas">—</div>
          <p class="metric-note" data-fc-despesas-note>Valores pagos dentro do período</p>
        </article>
        <article class="metric-card">
          <div class="metric-label">Saldo do período</div>
          <div class="metric-number" data-fc-valor="saldoPeriodo">—</div>
          <p class="metric-note" data-fc-saldo-note>Recebimentos menos pagamentos</p>
        </article>
      </section>

      <section class="panel" aria-label="Gráfico mensal de receitas e despesas">
        <div class="panel-header fc-panel-header">
          <div>
            <h2 class="panel-title">Receitas × despesas</h2>
            <p class="panel-subtitle" data-fc-chart-info>Comparativo mensal das movimentações liquidadas</p>
          </div>
          <span class="pill" data-fc-periodo>—</span>
        </div>
        <div class="panel-content">
          <div class="chart-legend" aria-hidden="true">
            <span><i class="chart-key receita"></i> Receitas</span>
            <span><i class="chart-key despesa"></i> Despesas</span>
          </div>
          <div class="fc-chart-region" data-fc-chart aria-live="polite">
            <div class="dashboard-loading">Carregando gráfico...</div>
          </div>
        </div>
      </section>

      <section class="panel" aria-label="Detalhamento mensal de fluxo de caixa">
        <div class="panel-header fc-panel-header">
          <div>
            <h2 class="panel-title">Detalhamento mensal</h2>
            <p class="panel-subtitle" data-fc-table-info>Resumo dos valores realizados em cada mês</p>
          </div>
          <span class="pill" data-fc-meses>—</span>
        </div>
        <div class="table-wrap">
          <table class="data-table fc-table">
            <thead>
              <tr>
                <th scope="col">Mês</th>
                <th scope="col">Receitas</th>
                <th scope="col">Despesas</th>
                <th scope="col">Saldo</th>
              </tr>
            </thead>
            <tbody data-fc-rows>
              <tr><td class="empty-table" colspan="4">Carregando resumo mensal...</td></tr>
            </tbody>
          </table>
        </div>
      </section>

      <p class="lf-footnote" data-fc-footnote>Este relatório considera somente lançamentos LIQUIDADOS, na respectiva data de liquidação. O saldo calculado não equivale necessariamente ao saldo bancário, pois não inclui saldo inicial ou conciliação.</p>
    </div>
  `;
}

function montarGrafico(meses, modo) {
  if (!Array.isArray(meses)) throw new Error('O servidor retornou um gráfico em formato inesperado.');
  const valores = meses.flatMap(mes => [Number(mes.receitas), Number(mes.despesas)]);
  const maior = Math.max(0, ...valores.filter(Number.isFinite));

  if (meses.length === 0 || maior === 0) {
    return `<div class="dashboard-empty">
      <strong>Sem movimentações no período</strong>
      <p>Não há receitas ou despesas ${modo === 'projetado' ? 'pendentes com vencimento' : 'liquidadas'} entre as datas selecionadas.</p>
    </div>`;
  }

  const colunas = meses.map(item => {
    const receitas = Math.max(0, Number(item.receitas) || 0);
    const despesas = Math.max(0, Number(item.despesas) || 0);
    const alturaReceitas = Math.min(100, receitas / maior * 100).toFixed(2);
    const alturaDespesas = Math.min(100, despesas / maior * 100).toFixed(2);
    const rotulo = escaparHtml(formatarMes(item.mes));
    const receitaMoeda = escaparHtml(formatarMoeda(item.receitas));
    const despesaMoeda = escaparHtml(formatarMoeda(item.despesas));

    return `
      <div class="fc-chart-column" role="listitem"
        aria-label="${rotulo}: receitas ${receitaMoeda}, despesas ${despesaMoeda}">
        <div class="fc-bars" aria-hidden="true">
          <div class="chart-bar receita" style="height:${alturaReceitas}%" title="Receitas: ${receitaMoeda}"></div>
          <div class="chart-bar despesa" style="height:${alturaDespesas}%" title="Despesas: ${despesaMoeda}"></div>
        </div>
        <span class="fc-chart-month">${rotulo}</span>
      </div>`;
  }).join('');

  // Largura mínima por mês para que intervalos longos possam rolar horizontalmente.
  const larguraMinima = Math.max(510, meses.length * 82);
  return `<div class="fc-chart-scroll" tabindex="0" aria-label="Gráfico mensal; role horizontalmente para visualizar mais meses">
    <div class="fc-chart-plot" role="list" style="min-width:${larguraMinima}px">
      ${colunas}
    </div>
  </div>`;
}

function montarLinhasMensais(meses) {
  if (!Array.isArray(meses)) throw new Error('O servidor retornou um resumo mensal em formato inesperado.');
  if (meses.length === 0) {
    return '<tr><td colspan="4" class="empty-table">Nenhum mês disponível no período selecionado.</td></tr>';
  }
  return meses.map(item => {
    const saldoNegativo = Number(item.saldo) < 0;
    return `<tr>
      <th scope="row">${escaparHtml(formatarMes(item.mes))}</th>
      <td class="fc-money">${escaparHtml(formatarMoeda(item.receitas))}</td>
      <td class="fc-money">${escaparHtml(formatarMoeda(item.despesas))}</td>
      <td class="fc-money ${saldoNegativo ? 'fc-negative' : ''}">${escaparHtml(formatarMoeda(item.saldo))}</td>
    </tr>`;
  }).join('');
}

/** Ativa filtros, busca dados reais e renderiza o resultado. */
export function inicializarFluxoCaixa(container) {
  const $ = seletor => container.querySelector(seletor);
  const elementos = {
    formulario: $('[data-fc-form]'),
    inicio: $('[data-fc-inicio]'),
    fim: $('[data-fc-fim]'),
    aplicar: $('[data-fc-apply]'),
    atualizar: $('[data-fc-refresh]'),
    feedback: $('[data-fc-feedback]'),
    grafico: $('[data-fc-chart]'),
    linhas: $('[data-fc-rows]'),
    periodo: $('[data-fc-periodo]'),
    meses: $('[data-fc-meses]'),
  };
  let ultimaConsulta = 0;

  function modoAtual() {
    return container.querySelector('input[name="fcModo"]:checked')?.value || 'realizado';
  }

  function atualizarTextosModo() {
    const projetado = modoAtual() === 'projetado';
    const textos = projetado ? {
      'mode-eyebrow': 'RELATÓRIO FINANCEIRO · PROJETADO',
      intro: 'Visualize valores ainda previstos para receber e pagar conforme a data de vencimento.',
      'dates-info': 'As datas são inclusivas e consideram o vencimento de contas pendentes.',
      'receitas-label': 'Receitas previstas',
      'receitas-note': 'Valores pendentes a receber no período',
      'despesas-label': 'Despesas previstas',
      'despesas-note': 'Valores pendentes a pagar no período',
      'saldo-note': 'Receitas previstas menos despesas previstas',
      'chart-info': 'Comparativo mensal dos lançamentos pendentes por vencimento',
      'table-info': 'Resumo dos valores previstos em cada mês',
      footnote: 'O fluxo projetado inclui somente lançamentos PENDENTES pela data de vencimento, inclusive vencidos dentro do período. Não representa o saldo bancário futuro e não considera lançamentos já liquidados.',
    } : {
      'mode-eyebrow': 'RELATÓRIO FINANCEIRO · REALIZADO',
      intro: 'Analise os valores efetivamente recebidos e pagos no período selecionado.',
      'dates-info': 'As datas são inclusivas e consideram a liquidação dos lançamentos.',
      'receitas-label': 'Receitas realizadas',
      'receitas-note': 'Valores recebidos dentro do período',
      'despesas-label': 'Despesas realizadas',
      'despesas-note': 'Valores pagos dentro do período',
      'saldo-note': 'Recebimentos menos pagamentos',
      'chart-info': 'Comparativo mensal das movimentações liquidadas',
      'table-info': 'Resumo dos valores realizados em cada mês',
      footnote: 'Este relatório considera somente lançamentos LIQUIDADOS, na respectiva data de liquidação, utilizando o valor efetivo. O saldo não equivale necessariamente ao saldo bancário, pois não inclui saldo inicial ou conciliação.',
    };
    for (const [chave, texto] of Object.entries(textos)) {
      const elemento = container.querySelector(`[data-fc-${chave}]`);
      if (elemento) elemento.textContent = texto;
    }
  }

  function mostrarErro(mensagem) {
    elementos.feedback.hidden = false;
    elementos.feedback.textContent = mensagem;
  }

  function limparErro() {
    elementos.feedback.hidden = true;
    elementos.feedback.textContent = '';
  }

  function redefinirResultado() {
    container.querySelectorAll('[data-fc-valor]').forEach(el => {
      el.textContent = '—';
      el.classList.remove('is-negative');
    });
    elementos.periodo.textContent = '—';
    elementos.meses.textContent = '—';
    elementos.grafico.innerHTML = '<div class="dashboard-loading">Carregando gráfico...</div>';
    elementos.linhas.innerHTML = '<tr><td class="empty-table" colspan="4">Carregando resumo mensal...</td></tr>';
  }

  function exibirResultado(dados) {
    if (!dados || !Array.isArray(dados.meses)) {
      throw new Error('A API retornou dados do fluxo de caixa em formato inesperado.');
    }

    // Calcula apenas alturas visuais das barras. Totais monetários vêm da API.
    const htmlGrafico = montarGrafico(dados.meses, modoAtual());
    const htmlLinhas = montarLinhasMensais(dados.meses);

    for (const campo of ['totalReceitas', 'totalDespesas', 'saldoPeriodo']) {
      const valor = container.querySelector(`[data-fc-valor="${campo}"]`);
      valor.textContent = formatarMoeda(dados[campo]);
      valor.classList.toggle('is-negative', campo === 'saldoPeriodo' && Number(dados[campo]) < 0);
    }
    elementos.periodo.textContent = `${formatarData(dados.inicio)} a ${formatarData(dados.fim)}`;
    elementos.meses.textContent = `${dados.meses.length} ${dados.meses.length === 1 ? 'mês' : 'meses'}`;
    elementos.grafico.innerHTML = htmlGrafico;
    elementos.linhas.innerHTML = htmlLinhas;
  }

  async function consultar() {
    const inicio = elementos.inicio.value;
    const fim = elementos.fim.value;
    // Invalida resultados de consultas anteriores mesmo que o novo período seja inválido.
    const consultaAtual = ++ultimaConsulta;
    limparErro();

    if (!inicio || !fim) {
      mostrarErro('Informe a data inicial e a data final.');
      return;
    }
    if (inicio > fim) {
      mostrarErro('A data inicial não pode ser posterior à data final.');
      return;
    }

    // Limite de usabilidade: não renderizar gráficos de centenas de meses.
    const [anoInicial, mesInicial] = inicio.split('-').map(Number);
    const [anoFinal, mesFinal] = fim.split('-').map(Number);
    const qtdMeses = (anoFinal - anoInicial) * 12 + mesFinal - mesInicial + 1;
    if (qtdMeses > 36) {
      mostrarErro('Selecione um período de até 36 meses para visualizar o gráfico.');
      return;
    }

    elementos.aplicar.disabled = true;
    elementos.atualizar.disabled = true;
    elementos.aplicar.textContent = 'Consultando...';
    redefinirResultado();

    try {
      const dados = modoAtual() === 'projetado'
        ? await api.relatorios.fluxoCaixaProjetado(inicio, fim)
        : await api.relatorios.fluxoCaixa(inicio, fim);
      if (!container.isConnected || consultaAtual !== ultimaConsulta) return;
      exibirResultado(dados);
    } catch (erro) {
      if (!container.isConnected || consultaAtual !== ultimaConsulta) return;
      // Erros são mostrados como texto, nunca inseridos como HTML.
      elementos.grafico.innerHTML = '<div class="dashboard-empty">Não foi possível carregar o gráfico.</div>';
      elementos.linhas.innerHTML = '<tr><td class="empty-table" colspan="4">Não foi possível carregar o detalhamento.</td></tr>';
      mostrarErro(mensagemDeErro(erro));
    } finally {
      if (container.isConnected && consultaAtual === ultimaConsulta) {
        elementos.aplicar.disabled = false;
        elementos.atualizar.disabled = false;
        elementos.aplicar.textContent = 'Aplicar filtro';
      }
    }
  }

  function aplicarAtalho(tipo) {
    const hoje = new Date();
    let periodo;
    if (tipo === 'mes') {
      periodo = { inicio: inicioMesISO(), fim: fimMesISO() };
    } else if (tipo === 'seis') {
      periodo = periodoUltimosMeses(6, hoje);
    } else if (tipo === 'ano') {
      periodo = { inicio: `${hoje.getFullYear()}-01-01`, fim: `${hoje.getFullYear()}-12-31` };
    } else {
      return;
    }
    elementos.inicio.value = periodo.inicio;
    elementos.fim.value = periodo.fim;
    consultar();
  }

  elementos.formulario.addEventListener('submit', evento => {
    evento.preventDefault();
    consultar();
  });
  elementos.atualizar.addEventListener('click', consultar);
  container.querySelectorAll('input[name="fcModo"]').forEach(campo => {
    campo.addEventListener('change', () => {
      atualizarTextosModo();
      consultar();
    });
  });
  container.querySelectorAll('[data-fc-preset]').forEach(botao => {
    botao.addEventListener('click', () => aplicarAtalho(botao.dataset.fcPreset));
  });

  const periodoInicial = periodoUltimosMeses();
  elementos.inicio.value = periodoInicial.inicio;
  elementos.fim.value = periodoInicial.fim;
  atualizarTextosModo();
  consultar();
}
