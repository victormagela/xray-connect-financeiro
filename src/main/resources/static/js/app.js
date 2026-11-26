/** X-Ray Connect — navegação da SPA. Views conectadas à API. */
import { renderDashboard, inicializarDashboard } from './views/dashboard.js';
import { renderLancamentos, inicializarLancamentos } from './views/lancamentos.js';
import { renderCategorias, inicializarCategorias } from './views/categorias.js';
import { renderFluxoCaixa, inicializarFluxoCaixa } from './views/fluxo-caixa.js';

const rotas = {
  dashboard: { titulo: 'Dashboard financeiro', kicker: 'VISÃO GERAL', descricao: 'Acompanhe os principais indicadores financeiros da X-Ray Connect.', render: renderDashboard, inicializar: inicializarDashboard },
  receber: { titulo: 'Contas a receber', kicker: 'GESTÃO DE RECEITAS', descricao: 'Gerencie vendas e outros valores que a empresa tem a receber.', render: () => renderLancamentos('RECEITA'), inicializar: conteudo => inicializarLancamentos(conteudo, 'RECEITA') },
  pagar: { titulo: 'Contas a pagar', kicker: 'GESTÃO DE DESPESAS', descricao: 'Acompanhe as obrigações e os pagamentos da empresa.', render: () => renderLancamentos('DESPESA'), inicializar: conteudo => inicializarLancamentos(conteudo, 'DESPESA') },
  'fluxo-caixa': { titulo: 'Fluxo de caixa', kicker: 'RELATÓRIOS', descricao: 'Consulte entradas, saídas e saldos realizados ou projetados por período.', render: renderFluxoCaixa, inicializar: inicializarFluxoCaixa },
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
