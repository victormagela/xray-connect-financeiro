/**
 * X-Ray Connect — navegação inicial da SPA (sem frameworks).
 * Nesta etapa, os cinco destinos são placeholders; nenhum valor fictício é exibido.
 * Na próxima etapa, conectaremos as views aos endpoints do Spring Boot.
 */

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

function renderDashboard() {
  const indicadores = [
    ['Total a receber', 'Receitas pendentes', true],
    ['Total a pagar', 'Despesas pendentes', false],
    ['Saldo realizado', 'Recebido menos pago', true],
    ['Contas vencidas', 'Pendentes após o vencimento', false],
  ];

  return `
    ${avisoIntegracao('Os indicadores serão obtidos de GET /api/relatorios/dashboard. Não exibimos números inventados antes da integração.')}
    <section class="metric-grid" aria-label="Indicadores financeiros">
      ${indicadores.map(([titulo, descricao, destaque]) => `
        <article class="metric-card ${destaque ? 'accent' : ''}">
          <div class="metric-label">${titulo}</div>
          <div class="metric-number" aria-label="Aguardando dados">—</div>
          <p class="metric-note">${descricao}</p>
        </article>
      `).join('')}
    </section>
    <div class="dashboard-columns">
      ${painel('Movimentação financeira', 'Visão mensal de receitas e despesas', vazio('Gráfico ainda não conectado', 'A visualização será alimentada por GET /api/relatorios/fluxo-caixa.'))}
      ${painel('Atalhos do módulo', 'Acesse as áreas operacionais', `
        <div class="actions-row">
          <a class="button primary" href="#receber">Contas a receber</a>
          <a class="button" href="#pagar">Contas a pagar</a>
          <a class="button" href="#fluxo-caixa">Fluxo de caixa</a>
          <a class="button" href="#categorias">Categorias</a>
        </div>
        <div style="height:22px"></div>
        <div class="info-list">
          <div class="info-list-row"><span>API</span><strong>Spring Boot</strong></div>
          <div class="info-list-row"><span>Banco de dados</span><strong>MySQL</strong></div>
          <div class="info-list-row"><span>Interface</span><strong>HTML, CSS e JS</strong></div>
        </div>
      `)}
    </div>
  `;
}

function renderLancamentos(tipo) {
  const receita = tipo === 'RECEITA';
  const nome = receita ? 'Contas a receber' : 'Contas a pagar';
  return `
    ${avisoIntegracao('O cadastro, a edição, a exclusão, a liquidação e a paginação serão conectados nesta tela na próxima etapa.')}
    <div class="card-stack">
      <div class="route-banner">
        <strong>${nome}</strong>
        <p>Esta tela consultará os lançamentos do tipo ${tipo}, já filtrados e paginados pelo back-end.</p>
        <code>GET /api/lancamentos?tipo=${tipo}&amp;page=0&amp;size=10</code>
      </div>
      <section class="panel">
        <div class="panel-header">
          <div><h2 class="panel-title">Lançamentos financeiros</h2><p class="panel-subtitle">Cadastro, situação, vencimentos e ações</p></div>
          <span class="pill red">${tipo}</span>
        </div>
        <div class="table-wrap">
          <table class="data-table">
            <thead><tr><th>Descrição</th><th>Categoria</th><th>Valor</th><th>Vencimento</th><th>Situação</th><th>Ações</th></tr></thead>
            <tbody><tr><td colspan="6" class="empty-table">Tabela aguardando conexão com a API.<br>Os lançamentos existentes serão exibidos aqui.</td></tr></tbody>
          </table>
        </div>
      </section>
    </div>
  `;
}

function renderCategorias() {
  return `
    ${avisoIntegracao('Os formulários de cadastro e edição, as ações de exclusão e a paginação serão adicionados durante a integração.')}
    <div class="route-banner">
      <strong>Gerenciamento de categorias</strong>
      <p>As categorias organizam receitas e despesas e são compartilhadas com as telas de lançamentos.</p>
      <code>GET /api/categorias?page=0&amp;size=10</code>
    </div>
    <div style="height:20px"></div>
    <section class="panel">
      <div class="panel-header">
        <div><h2 class="panel-title">Categorias cadastradas</h2><p class="panel-subtitle">Lista de categorias financeiras</p></div>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>Nome</th><th>Tipo</th><th>Descrição</th><th>Ações</th></tr></thead>
          <tbody><tr><td colspan="4" class="empty-table">Tabela aguardando conexão com a API de categorias.</td></tr></tbody>
        </table>
      </div>
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
  dashboard: { titulo: 'Dashboard financeiro', kicker: 'VISÃO GERAL', descricao: 'Acompanhe os principais indicadores financeiros da X-Ray Connect.', render: renderDashboard },
  receber: { titulo: 'Contas a receber', kicker: 'GESTÃO DE RECEITAS', descricao: 'Gerencie vendas e outros valores que a empresa tem a receber.', render: () => renderLancamentos('RECEITA') },
  pagar: { titulo: 'Contas a pagar', kicker: 'GESTÃO DE DESPESAS', descricao: 'Acompanhe as obrigações e os pagamentos da empresa.', render: () => renderLancamentos('DESPESA') },
  'fluxo-caixa': { titulo: 'Fluxo de caixa', kicker: 'RELATÓRIOS', descricao: 'Consulte as entradas, saídas e o saldo realizado por período.', render: renderFluxoCaixa },
  categorias: { titulo: 'Categorias', kicker: 'CONFIGURAÇÕES', descricao: 'Organize e classifique os lançamentos financeiros.', render: renderCategorias },
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
  document.getElementById('view-content').innerHTML = rota.render();

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
