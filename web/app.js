// ---------- tiny helpers ----------

async function api(method, path, formData) {
  const opts = { method };
  if (formData) {
    opts.headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
    opts.body = new URLSearchParams(formData).toString();
  }
  const res = await fetch(path, opts);
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || `Request failed (${res.status})`);
  return data;
}

function toast(message, kind = 'success') {
  const el = document.getElementById('toast');
  el.textContent = message;
  el.className = 'show ' + kind;
  clearTimeout(toast._t);
  toast._t = setTimeout(() => { el.className = ''; }, 3200);
}

function formToObject(form) {
  const obj = {};
  new FormData(form).forEach((v, k) => (obj[k] = v));
  return obj;
}

// ---------- tabs ----------

document.querySelectorAll('.tab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById(btn.dataset.tab).classList.add('active');
  });
});

// ---------- state / caches (used to populate <select> dropdowns) ----------

let cachedStudents = [];
let cachedSkills = [];

function studentOptionsHtml() {
  return cachedStudents.map(s => `<option value="${s.id}">#${s.id} ${s.name}</option>`).join('');
}
function skillOptionsHtml() {
  return cachedSkills.map(s => `<option value="${s.id}">#${s.id} ${s.name}</option>`).join('');
}

function refreshDropdowns() {
  const studentSelects = ['ow-student', 'find-requester', 'req-requester', 'req-provider'];
  const skillSelects = ['ow-skill', 'find-skill', 'req-skill'];
  studentSelects.forEach(id => (document.getElementById(id).innerHTML = studentOptionsHtml()));
  skillSelects.forEach(id => (document.getElementById(id).innerHTML = skillOptionsHtml()));
}

// ---------- loaders ----------

async function loadStudents() {
  cachedStudents = await api('GET', '/api/students');
  refreshDropdowns();
  const list = document.getElementById('students-list');
  list.innerHTML = cachedStudents.map(s => `
    <div class="item">
      <div class="title">#${s.id} ${s.name} <span class="meta">— ${s.branch}, Yr ${s.year}</span></div>
      <div class="meta">⭐ ${s.rating.toFixed(1)} (${s.ratingCount} reviews) · 💰 ${s.credits} credits</div>
      <div class="meta">Can teach: ${s.offers.length ? s.offers.join(', ') : '—'}</div>
      <div class="meta">Wants to learn: ${s.wants.length ? s.wants.join(', ') : '—'}</div>
    </div>
  `).join('') || '<p class="meta">No students yet.</p>';
}

async function loadSkills() {
  cachedSkills = await api('GET', '/api/skills');
  refreshDropdowns();
  const list = document.getElementById('skills-list');
  list.innerHTML = cachedSkills.map(s => `
    <div class="item">
      <div class="title">#${s.id} ${s.name}</div>
      <div class="meta">${s.categoryLabel}${s.description ? ' · ' + s.description : ''}</div>
    </div>
  `).join('') || '<p class="meta">No skills yet.</p>';
}

async function loadRequests() {
  const requests = await api('GET', '/api/requests');
  const list = document.getElementById('requests-list');
  list.innerHTML = requests.map(r => `
    <div class="item">
      <div class="title">Request #${r.id} <span class="status ${r.status}">${r.status}</span></div>
      <div class="meta">${r.requesterName} → ${r.providerName} · "${r.skillName}" for ${r.credits} credits</div>
    </div>
  `).join('') || '<p class="meta">No requests yet.</p>';
}

function populateCategoryDropdown() {
  const categories = ['TECHNOLOGY', 'MUSIC', 'SPORTS', 'ART_AND_DESIGN', 'LANGUAGE', 'ACADEMICS', 'LIFE_SKILLS', 'OTHER'];
  document.getElementById('skill-category').innerHTML =
    categories.map(c => `<option value="${c}">${c.replaceAll('_', ' ')}</option>`).join('');
}

async function refreshAll() {
  await Promise.all([loadStudents(), loadSkills(), loadRequests()]);
}

// ---------- form handlers ----------

document.getElementById('form-register').addEventListener('submit', async e => {
  e.preventDefault();
  try {
    await api('POST', '/api/students', formToObject(e.target));
    e.target.reset();
    toast('Student registered!');
    await loadStudents();
  } catch (err) { toast(err.message, 'error'); }
});

document.getElementById('form-add-skill').addEventListener('submit', async e => {
  e.preventDefault();
  try {
    await api('POST', '/api/skills', formToObject(e.target));
    e.target.reset();
    toast('Skill added!');
    await loadSkills();
  } catch (err) { toast(err.message, 'error'); }
});

document.getElementById('form-offer-want').addEventListener('submit', async e => {
  e.preventDefault();
  const data = formToObject(e.target);
  const endpoint = data.kind === 'offer' ? '/api/offer' : '/api/want';
  try {
    await api('POST', endpoint, { studentId: data.studentId, skillId: data.skillId, level: data.level });
    toast('Saved!');
    await loadStudents();
  } catch (err) { toast(err.message, 'error'); }
});

document.getElementById('form-find-teachers').addEventListener('submit', async e => {
  e.preventDefault();
  const data = formToObject(e.target);
  try {
    const teachers = await api('GET', `/api/teachers?skillId=${data.skillId}&requesterId=${data.requesterId}`);
    const list = document.getElementById('teachers-list');
    list.innerHTML = teachers.map(t => `
      <div class="item">
        <div class="title">#${t.id} ${t.name}</div>
        <div class="meta">⭐ ${t.rating.toFixed(1)} (${t.ratingCount} reviews) · Teaches: ${t.offers.join(', ')}</div>
      </div>
    `).join('') || '<p class="meta">No teachers found for that skill yet.</p>';
  } catch (err) { toast(err.message, 'error'); }
});

document.getElementById('form-create-request').addEventListener('submit', async e => {
  e.preventDefault();
  try {
    const r = await api('POST', '/api/requests', formToObject(e.target));
    e.target.reset();
    toast(`Request #${r.id} created!`);
    await loadRequests();
  } catch (err) { toast(err.message, 'error'); }
});

document.getElementById('form-accept').addEventListener('submit', async e => {
  e.preventDefault();
  try {
    const r = await api('POST', '/api/accept', formToObject(e.target));
    toast(r.accepted ? 'Accepted!' : 'Could not accept (not pending?)', r.accepted ? 'success' : 'error');
    await loadRequests();
  } catch (err) { toast(err.message, 'error'); }
});

document.getElementById('form-complete').addEventListener('submit', async e => {
  e.preventDefault();
  try {
    await api('POST', '/api/complete', formToObject(e.target));
    toast('Marked complete!');
    await refreshAll();
  } catch (err) { toast(err.message, 'error'); }
});

document.getElementById('form-rate').addEventListener('submit', async e => {
  e.preventDefault();
  try {
    await api('POST', '/api/rate', formToObject(e.target));
    toast('Rating submitted!');
    await loadStudents();
  } catch (err) { toast(err.message, 'error'); }
});

document.getElementById('btn-export').addEventListener('click', async () => {
  try {
    const r = await api('GET', '/api/export');
    toast('Exported to ' + r.path);
  } catch (err) { toast(err.message, 'error'); }
});

document.getElementById('form-concurrency').addEventListener('submit', async e => {
  e.preventDefault();
  const logEl = document.getElementById('concurrency-log');
  logEl.textContent = 'Running...';
  try {
    const r = await api('POST', '/api/concurrency-demo', formToObject(e.target));
    logEl.textContent = r.log.join('\n');
    await loadRequests();
  } catch (err) {
    logEl.textContent = 'Error: ' + err.message;
  }
});

// ---------- init ----------

populateCategoryDropdown();
refreshAll().catch(err => toast(err.message, 'error'));
