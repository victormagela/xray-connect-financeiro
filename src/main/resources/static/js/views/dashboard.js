/**
 * Dashboard — primeira view integrada do ERP X-Ray Connect.
 * O back-end calcula todos os totais; aqui apenas exibimos os dados.
 */
import api from '../api.js';
import {
  formatarMoeda,
  formatarMes,
  mensagemDeErro,
  escaparHtml,
} from '../utils.js';

const MESES_NO_GRAFICO = 6;

/** Monta o período do gráfico: mês atual e cinco meses anteriores. */
export function periodoDashboard(referencia = new Date()) {
  const primeiroDia = new Date(
    referencia.getFullYear(),
    referencia.getMonth() - (MESES_NO_GRAFICO - 1),
    1,
  );
  const ano = primeiroDia.getFullYear();
  const mes = String(primeiroDia.getMonth() + 1).padStart(2, '0');
  return { inicio: `${ano}-${mes}-01`, fim: fimMesDaData(referencia) };
}

function fimMesDaData(data) {
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const ultimoDia = new Date(ano, data.getMonth() + 1, 0).getDate();
  return `${ano}-${mes}-${String(ultimoDia).padStart(2, '0')}`;
}

/** Retorna apenas a estrutura HTML. Os valores reais chegam na inicialização. */
export function renderDashboard() {
  return `
    <div class="dashboard-toolbar">
      <p>Resumo dos lançamentos registrados.</p>
      <button class="button dashboard-refresh" type="button" data-dashboard-refresh>
        Atualizar dados
      </button>
    </div>

    <section class="metric-grid" aria-label="Indicadores financeiros">
      <article class="metric-card accent">
        <div class="metric-label">Total a receber</div>
        <div class="metric-number" data-dashboard-valor="totalAReceber">—</div>
        <p class="metric-note">Receitas ainda pendentes</p>
      </article>
      <article class="metric-card">
        <div class="metric-label">Total a pagar</div>
        <div class="metric-number" data-dashboard-valor="totalAPagar">—</div>
        <p class="metric-note">Despesas ainda pendentes</p>
      </article>
      <article class="metric-card accent">
        <div class="metric-label">Saldo realizado</div>
        <div class="metric-number" data-dashboard-valor="saldoRealizado">—</div>
        <p class="metric-note">Recebimentos menos pagamentos</p>
      </article>
      <article class="metric-card">
        <div class="metric-label">Contas vencidas</div>
        <div class="metric-number" data-dashboard-valor="quantidadeVencidos">—</div>
        <p class="metric-note">Pendentes após o vencimento</p>
      </article>
    </section>

    <div class="dashboard-columns">
      <section class="panel" aria-label="Gráfico de movimentações">
        <div class="panel-header">
          <div>
            <h2 class="panel-title">Movimentação financeira</h2>
            <p class="panel-subtitle">Receitas e despesas liquidadas por mês</p>
          </div>
          <span class="pill" data-dashboard-periodo>Últimos 6 meses</span>
        </div>
        <div class="panel-content dashboard-chart-content">
          <div class="chart-legend" aria-hidden="true">
            <span><i class="chart-key receita"></i> Receitas</span>
            <span><i class="chart-key despesa"></i> Despesas</span>
          </div>
          <div data-dashboard-chart role="status" aria-live="polite">
            <div class="dashboard-loading">Carregando movimentações...</div>
          </div>
        </div>
      </section>

      <section class="panel" aria-label="Resumo e atalhos">
        <div class="panel-header">
          <div>
            <h2 class="panel-title">Resultados acumulados</h2>
            <p class="panel-subtitle">Todos os lançamentos cadastrados</p>
          </div>
        </div>
        <div class="panel-content dashboard-summary">
          <div class="info-list">
            <div class="info-list-row">
              <span>Total recebido</span>
              <strong data-dashboard-valor="totalRecebido">—</strong>
            </div>
            <div class="info-list-row">
              <span>Total pago</span>
              <strong data-dashboard-valor="totalPago">—</strong>
            </div>
          </div>
          <p class="dashboard-help">O saldo realizado é a diferença entre as receitas e despesas liquidadas registradas; não representa necessariamente o saldo bancário.</p>
          <div class="dashboard-shortcuts">
            <h3>Acesso rápido</h3>
            <a class="button primary" href="#receber">Contas a receber</a>
            <a class="button" href="#pagar">Contas a pagar</a>
            <a class="button" href="#fluxo-caixa">Fluxo de caixa</a>
            <a class="button" href="#categorias">Categorias</a>
          </div>
        </div>
      </section>
    </div>
  `;
}

function exibirDashboard(container, dados) {
  const camposMonetarios = [
    'totalAReceber', 'totalAPagar', 'saldoRealizado',
    'totalRecebido', 'totalPago',
  ];

  for (const campo of camposMonetarios) {
    const elemento = container.querySelector(`[data-dashboard-valor="${campo}"]`);
    if (!elemento) continue;
    elemento.textContent = formatarMoeda(dados[campo]);
    elemento.classList.toggle('is-negative',
      campo === 'saldoRealizado' && Number(dados[campo]) < 0);
  }

  const vencidos = container.querySelector('[data-dashboard-valor="quantidadeVencidos"]');
  if (vencidos) {
    const quantidade = Number(dados.quantidadeVencidos);
    vencidos.textContent = Number.isFinite(quantidade)
      ? quantidade.toLocaleString('pt-BR') : '—';
  }
}

/** Monta barras com CSS, sem bibliotecas de gráficos e sem dados inventados. */
function gerarGrafico(meses) {
  if (!Array.isArray(meses)) {
    throw new Error('Os dados do gráfico vieram em um formato inesperado.');
  }

  const valores = meses.flatMap(item => [Number(item.receitas), Number(item.despesas)]);
  const totalMovimentado = valores.reduce((total, valor) => total + (Number.isFinite(valor) ? Math.max(0, valor) : 0), 0);

  if (meses.length === 0 || totalMovimentado === 0) {
    return `<div class="dashboard-empty">
      <strong>Sem movimentações no período</strong>
      <p>Não há recebimentos nem pagamentos liquidados nos últimos seis meses.</p>
    </div>`;
  }

  const maiorValor = Math.max(1, ...valores.filter(Number.isFinite));
  const colunas = meses.map(item => {
    const receitas = Math.max(0, Number(item.receitas) || 0);
    const despesas = Math.max(0, Number(item.despesas) || 0);
    const alturaReceitas = Math.min(100, receitas / maiorValor * 100);
    const alturaDespesas = Math.min(100, despesas / maiorValor * 100);
    const rotulo = escaparHtml(formatarMes(item.mes));
    const tituloReceitas = escaparHtml(formatarMoeda(receitas));
    const tituloDespesas = escaparHtml(formatarMoeda(despesas));

    return `
      <div class="chart-column" role="listitem"
        aria-label="${rotulo}: receitas ${tituloReceitas}; despesas ${tituloDespesas}">
        <div class="chart-bars" aria-hidden="true">
          <div class="chart-bar receita" style="height:${alturaReceitas.toFixed(2)}%" title="Receitas: ${tituloReceitas}"></div>
          <div class="chart-bar despesa" style="height:${alturaDespesas.toFixed(2)}%" title="Despesas: ${tituloDespesas}"></div>
        </div>
        <span class="chart-month">${rotulo}</span>
      </div>`;
  }).join('');

  return `<div class="chart-scroll">
    <div class="chart-plot" role="list" aria-label="Receitas e despesas mensais">
      ${colunas}
    </div>
  </div>`;
}

/**
 * Liga a tela às APIs. A checagem isConnected evita atualizar uma view já fechada.
 * O contador impede que uma requisição antiga sobrescreva uma atualização mais recente.
 */
export function inicializarDashboard(container) {
  const botao = container.querySelector('[data-dashboard-refresh]');
  const grafico = container.querySelector('[data-dashboard-chart]');
  const periodoLabel = container.querySelector('[data-dashboard-periodo]');
  let ultimaExecucao = 0;

  async function atualizar() {
    const execucaoAtual = ++ultimaExecucao;
    const { inicio, fim } = periodoDashboard();

    botao.disabled = true;
    botao.textContent = 'Atualizando...';
    grafico.classList.remove('dashboard-error');
    grafico.innerHTML = '<div class="dashboard-loading">Carregando movimentações...</div>';
    periodoLabel.textContent = 'Últimos 6 meses';

    // Duas requisições independentes: uma falha no gráfico não apaga os indicadores.
    const [resultadoDashboard, resultadoFluxo] = await Promise.allSettled([
      api.relatorios.dashboard(),
      api.relatorios.fluxoCaixa(inicio, fim),
    ]);

    if (!container.isConnected || execucaoAtual !== ultimaExecucao) return;

    if (resultadoDashboard.status === 'fulfilled') {
      exibirDashboard(container, resultadoDashboard.value);
    } else {
      // Nunca reutilizamos valores de uma consulta anterior que acabou de falhar.
      container.querySelectorAll('[data-dashboard-valor]').forEach(el => {
        el.textContent = '—';
        el.classList.remove('is-negative');
      });
      const aviso = document.createElement('div');
      aviso.className = 'dashboard-error';
      aviso.setAttribute('role', 'alert');
      aviso.textContent = `Não foi possível carregar os indicadores: ${mensagemDeErro(resultadoDashboard.reason)}`;
      const cartoes = container.querySelector('.metric-grid');
      cartoes.querySelector('.dashboard-error')?.remove();
      cartoes.append(aviso);
    }

    if (resultadoDashboard.status === 'fulfilled') {
      container.querySelector('.metric-grid .dashboard-error')?.remove();
    }

    if (resultadoFluxo.status === 'fulfilled') {
      try {
        grafico.classList.remove('dashboard-error');
        grafico.innerHTML = gerarGrafico(resultadoFluxo.value.meses);
      } catch (erro) {
        grafico.textContent = mensagemDeErro(erro);
        grafico.classList.add('dashboard-error');
      }
    } else {
      grafico.textContent = `Não foi possível carregar o gráfico: ${mensagemDeErro(resultadoFluxo.reason)}`;
      grafico.className = 'dashboard-error';
    }

    botao.disabled = false;
    botao.textContent = 'Atualizar dados';
  }

  botao.addEventListener('click', atualizar);
  atualizar();
}
