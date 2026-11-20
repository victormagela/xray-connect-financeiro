/** X-Ray Connect — navegação da SPA. Views conectadas à API. */
import { renderDashboard, inicializarDashboard } from './views/dashboard.js';
import { renderLancamentos, inicializarLancamentos } from './views/lancamentos.js';
import { renderCategorias, inicializarCategorias } from './views/categorias.js';

const ICONS = {
  dashboard: '<svg class="icon" aria-hidden="true"><use href="#i-grid"/></svg>',
  chart: '<svg class="icon" aria-hidden="true"><use href="#i-chart"/></svg>',
  info: '<svg class="icon" aria-hidden="true"><use href="#i-info"/></svg>',
};

function avisoIntegracao(texto) {
  return `
    <div class="notice" role="note">
      ${ICONS.info}
      <div>
        <strong>Estrutura visual pronta</strong>
        <p>${texto}</p>
      </div>
    </div>
  `;
}

function vazio(titulo, descricao, compacto = false) {
  return `
    <div class="placeholder ${compacto ? 'compact' : ''}">
      <span class="placeholder-icon">${ICONS.chart}</span>
      <strong>${titulo}</strong>
      <p>${descricao}</p>
    </div>
  `;
}

function painel(titulo, subtitulo, conteudo, etiqueta = '') {
  return `
    <section class="panel">
      <div class="panel-header">
        <div><h2 class="panel-title">${titulo}</h2><p class="panel-subtitle">${subtitulo}</p></div>
        ${etiqueta ? `<span class="pill">${etiqueta}</span>` : ''}
      </div>
      <div class="panel-content">${conteudo}</div>
    </section>
  `;
}

function renderFluxoCaixa() {
  return `
    ${avisoIntegracao('O seletor de período, os totais e o gráfico mensal serão conectados ao endpoint de fluxo de caixa.')}
    <div class="card-stack">
      <div class="route-banner">
        <strong>Movimentações realizadas por período</strong>
        <p>Serão consideradas apenas contas liquidadas, agrupadas pela data de liquidação.</p>
        <code>GET /api/relatorios/fluxo-caixa?inicio=AAAA-MM-DD&amp;fim=AAAA-MM-DD</code>
      </div>
      ${painel('Receitas × despesas', 'Gráfico mensal do intervalo selecionado', vazio('Dados ainda não carregados', 'O gráfico exibirá os totais retornados pelo serviço de relatórios.'))}
    </div>
  `;
}

const rotas = {
  dashboard: { titulo: 'Dashboard financeiro', kicker: 'VISÃO GERAL', descricao: 'Acompanhe os principais indicadores financeiros da X-Ray Connect.', render: renderDashboard, inicializar: inicializarDashboard },
  receber: { titulo: 'Contas a receber', kicker: 'GESTÃO DE RECEITAS', descricao: 'Gerencie vendas e outros valores que a empresa tem a receber.', render: () => renderLancamentos('RECEITA'), inicializar: conteudo => inicializarLancamentos(conteudo, 'RECEITA') },
  pagar: { titulo: 'Contas a pagar', kicker: 'GESTÃO DE DESPESAS', descricao: 'Acompanhe as obrigações e os pagamentos da empresa.', render: () => renderLancamentos('DESPESA'), inicializar: conteudo => inicializarLancamentos(conteudo, 'DESPESA') },
  'fluxo-caixa': { titulo: 'Fluxo de caixa', kicker: 'RELATÓRIOS', descricao: 'Consulte as entradas, saídas e o saldo realizado por período.', render: renderFluxoCaixa },
  categorias: { titulo: 'Categorias', kicker: 'CONFIGURAÇÕES', descricao: 'Organize e classifique os lançamentos financeiros.', render: renderCategorias, inicializar: inicializarCategorias },
};

function navegar() {
  let identificador = window.location.hash.replace(/^#/, '');
  if (!Object.hasOwn(rotas, identificador)) {
    identificador = 'dashboard';
    window.history.replaceState(null, '', '#dashboard');
  }

  const rota = rotas[identificador];
  document.title = `${rota.titulo} | X-Ray Connect`;
  document.getElementById('page-title').textContent = rota.titulo;
  document.getElementById('page-kicker').textContent = rota.kicker;
  document.getElementById('page-subtitle').textContent = rota.descricao;
  const conteudo = document.getElementById('view-content');
  conteudo.innerHTML = rota.render();
  rota.inicializar?.(conteudo);

  document.querySelectorAll('.nav-link').forEach(link => {
    const ativo = link.dataset.view === identificador;
    link.classList.toggle('is-active', ativo);
    if (ativo) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  });
}

const hoje = document.querySelector('[data-today]');
hoje.textContent = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' }).format(new Date());
document.querySelector('[data-year]').textContent = String(new Date().getFullYear());
window.addEventListener('hashchange', () => {
  navegar();
  window.scrollTo(0, 0);
});
navegar();
