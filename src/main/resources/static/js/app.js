const API = '/api/workflows';
let currentWorkflowId = null;
let pollInterval = null;

// ─── INIT ─────────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  setupNav();
  loadDashboard();
});

function setupNav() {
  document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', e => {
      e.preventDefault();
      switchView(item.dataset.view);
    });
  });
}

function switchView(view) {
  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  document.getElementById('view-' + view).classList.add('active');
  document.querySelector(`[data-view="${view}"]`).classList.add('active');

  if (view === 'dashboard') loadDashboard();
  if (view === 'logs') loadAllLogs();
}

// ─── DASHBOARD ────────────────────────────────────────────────────────────────
async function loadDashboard() {
  await Promise.all([loadStats(), loadWorkflowCards()]);
}

async function loadStats() {
  try {
    const res = await fetch(`${API}/stats/summary`);
    const stats = await res.json();
    document.getElementById('stat-total').textContent = stats.total ?? 0;
    document.getElementById('stat-active').textContent = stats.active ?? 0;
    document.getElementById('stat-completed').textContent = stats.completed ?? 0;
    document.getElementById('stat-failed').textContent = stats.failed ?? 0;
  } catch { /* silent */ }
}

async function loadWorkflowCards() {
  try {
    const res = await fetch(API);
    const workflows = await res.json();
    const container = document.getElementById('workflow-cards');

    if (!workflows.length) {
      container.innerHTML = `<div style="color:var(--text-dim);padding:2rem;font-size:.88rem;">No workflows yet. Create your first one →</div>`;
      return;
    }

    container.innerHTML = workflows.map(wf => `
      <div class="wf-card" onclick="openWorkflow(${wf.id})">
        <div class="wf-card-top">
          <span class="wf-name">${esc(wf.name)}</span>
          <span class="badge badge-${wf.status}">${wf.status}</span>
        </div>
        <p class="wf-desc">${esc(wf.description || 'No description')}</p>
        <div class="wf-meta">
          <span class="wf-stat">Runs: <span>${wf.triggerCount}</span></span>
          <span class="wf-stat">Success: <span>${wf.successCount}</span></span>
          <span class="wf-stat">Rate: <span>${successRate(wf)}</span></span>
        </div>
      </div>
    `).join('');
  } catch (e) {
    console.error(e);
  }
}

// ─── WORKFLOW BUILDER ─────────────────────────────────────────────────────────
async function openWorkflow(id) {
  currentWorkflowId = id;
  switchView('workflows');
  await refreshBuilder();
  startPolling();
}

async function refreshBuilder() {
  if (!currentWorkflowId) return;
  try {
    const [wfRes, stepsRes, logsRes] = await Promise.all([
      fetch(`${API}/${currentWorkflowId}`),
      fetch(`${API}/${currentWorkflowId}/steps`),
      fetch(`${API}/${currentWorkflowId}/logs`)
    ]);

    const wf    = await wfRes.json();
    const steps = await stepsRes.json();
    const logs  = await logsRes.json();

    document.getElementById('builder-wf-name').textContent = wf.name;
    document.getElementById('builder-subtitle').textContent = wf.description || '';

    if (wf.aiSuggestion) {
      document.getElementById('ai-suggestion').innerHTML =
        `<p>${esc(wf.aiSuggestion)}</p>`;
    }

    renderSteps(steps);
    renderLogs('wf-logs', logs);
  } catch (e) { console.error(e); }
}

function renderSteps(steps) {
  const el = document.getElementById('steps-list');
  if (!steps.length) {
    el.innerHTML = `<div class="empty-steps">No steps yet. Add your first step →</div>`;
    return;
  }
  el.innerHTML = steps.map(s => `
    <div class="step-item">
      <span class="step-num">${s.stepOrder}</span>
      <div class="step-info">
        <div class="step-name">${esc(s.name)}</div>
        <div class="step-type">${s.type} ${s.assignee ? '· ' + esc(s.assignee) : ''}</div>
      </div>
      <span class="step-status badge badge-${s.status}">${s.status}</span>
      <button class="step-delete" onclick="deleteStep(${s.id})" title="Remove step">✕</button>
    </div>
  `).join('');
}

function renderLogs(containerId, logs) {
  const el = document.getElementById(containerId);
  if (!logs.length) {
    el.innerHTML = `<div class="empty-logs">No logs yet.</div>`;
    return;
  }
  el.innerHTML = logs.map(l => `
    <div class="log-entry ${l.level}">
      <span class="log-time">${formatTime(l.createdAt)}</span>
      <span class="log-msg">${levelIcon(l.level)} ${esc(l.message)}</span>
    </div>
  `).join('');
}

// ─── ACTIONS ──────────────────────────────────────────────────────────────────
async function createWorkflow() {
  const name = document.getElementById('wf-name-input').value.trim();
  const description = document.getElementById('wf-desc-input').value.trim();
  if (!name) { showToast('Please enter a workflow name'); return; }

  try {
    await fetch(API, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, description })
    });
    closeModal('modal-create');
    document.getElementById('wf-name-input').value = '';
    document.getElementById('wf-desc-input').value = '';
    showToast('Workflow created ✓');
    await loadDashboard();
  } catch { showToast('Failed to create workflow'); }
}

async function addStep() {
  const name     = document.getElementById('step-name-input').value.trim();
  const type     = document.getElementById('step-type-input').value;
  const assignee = document.getElementById('step-assignee-input').value.trim();
  const desc     = document.getElementById('step-desc-input').value.trim();
  const order    = parseInt(document.getElementById('step-order-input').value) || 1;

  if (!name) { showToast('Please enter a step name'); return; }

  try {
    await fetch(`${API}/${currentWorkflowId}/steps`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, type, assignee, description: desc, stepOrder: order })
    });
    closeModal('modal-step');
    document.getElementById('step-name-input').value = '';
    document.getElementById('step-assignee-input').value = '';
    document.getElementById('step-desc-input').value = '';
    showToast('Step added ✓');
    await refreshBuilder();
  } catch { showToast('Failed to add step'); }
}

async function deleteStep(stepId) {
  if (!confirm('Remove this step?')) return;
  await fetch(`${API}/steps/${stepId}`, { method: 'DELETE' });
  showToast('Step removed');
  await refreshBuilder();
}

async function executeCurrentWorkflow() {
  if (!currentWorkflowId) return;
  try {
    await fetch(`${API}/${currentWorkflowId}/execute?triggeredBy=user`, { method: 'POST' });
    showToast('▶ Execution started — watch steps update live');
  } catch { showToast('Failed to trigger workflow'); }
}

async function refreshSuggestion() {
  if (!currentWorkflowId) return;
  document.getElementById('ai-suggestion').innerHTML =
    `<p class="ai-placeholder">Generating suggestion…</p>`;
  try {
    const res = await fetch(`${API}/${currentWorkflowId}/suggest`, { method: 'POST' });
    const data = await res.json();
    document.getElementById('ai-suggestion').innerHTML =
      `<p>${esc(data.suggestion)}</p>`;
  } catch {
    document.getElementById('ai-suggestion').innerHTML =
      `<p class="ai-placeholder">Could not load suggestion. Try again.</p>`;
  }
}

// ─── ALL LOGS ─────────────────────────────────────────────────────────────────
async function loadAllLogs() {
  try {
    const res  = await fetch(`${API}/logs/recent`);
    const logs = await res.json();
    renderLogs('all-logs', logs);
  } catch { /* silent */ }
}

// ─── POLLING ──────────────────────────────────────────────────────────────────
function startPolling() {
  stopPolling();
  pollInterval = setInterval(refreshBuilder, 2500);
}
function stopPolling() {
  if (pollInterval) { clearInterval(pollInterval); pollInterval = null; }
}

// ─── MODALS ───────────────────────────────────────────────────────────────────
function openCreateModal() {
  document.getElementById('modal-create').classList.add('open');
}
function openAddStepModal() {
  if (!currentWorkflowId) { showToast('Select a workflow first'); return; }
  document.getElementById('modal-step').classList.add('open');
}
function closeModal(id) {
  document.getElementById(id).classList.remove('open');
}

// ─── HELPERS ──────────────────────────────────────────────────────────────────
function esc(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function successRate(wf) {
  if (!wf.triggerCount) return 'N/A';
  return Math.round((wf.successCount / wf.triggerCount) * 100) + '%';
}

function formatTime(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

function levelIcon(level) {
  return { INFO: 'ℹ', SUCCESS: '✓', WARNING: '⚠', ERROR: '✕' }[level] || '';
}

let toastTimer;
function showToast(msg) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove('show'), 2800);
}
