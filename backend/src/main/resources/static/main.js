const _isFirebaseHosting = window.location.hostname.endsWith('.web.app')
  || window.location.hostname.endsWith('.firebaseapp.com');
const API_BASE = _isFirebaseHosting
  ? 'https://product1-backend-527559107931.us-central1.run.app/api'
  : '/api';
  const LOGIN_API = API_BASE + '/login';

  // ── STATE ──
  let files = [];
  let loggedInUser = '';
  let parsedResults = [];

  // ── ELEMENTS ──
  const loginView     = document.getElementById('login-view');
  const dashView      = document.getElementById('dashboard-view');
  const loginForm     = document.getElementById('loginForm');
  const loginError    = document.getElementById('login-error');
  const loginBtn      = document.getElementById('login-btn');
  const navUsername   = document.getElementById('nav-username');
  const logoutBtn     = document.getElementById('logoutBtn');
  const dropzone      = document.getElementById('dropzone');
  const fileInput     = document.getElementById('fileInput');
  const selectFileBtn = document.getElementById('selectFileBtn');
  const fileItems     = document.getElementById('file-items');
  const fileCount     = document.getElementById('file-count');
  const parseBtn      = document.getElementById('parseBtn');
  const resultEmpty   = document.getElementById('result-empty');
  const resultContent = document.getElementById('result-content');
  const resultBadge   = document.getElementById('result-badge');
  const resultMeta    = document.getElementById('result-meta');
  const resultThead   = document.getElementById('result-thead');
  const resultTbody   = document.getElementById('result-tbody');

  // ── LOGIN ──
  loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    loginError.textContent = '';
    loginBtn.querySelector('.btn-text').hidden = true;
    loginBtn.querySelector('.btn-arrow').hidden = true;
    loginBtn.querySelector('.btn-loader').hidden = false;
    loginBtn.disabled = true;

    try {
      const res = await fetch(LOGIN_API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      if (res.ok) {
        loggedInUser = username;
        navUsername.textContent = username;
        loginView.hidden = true;
        dashView.hidden = false;
      } else {
        const msg = await res.text();
        loginError.textContent = msg || '아이디 또는 비밀번호가 올바르지 않습니다.';
      }
    } catch {
      loginError.textContent = '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.';
    } finally {
      loginBtn.querySelector('.btn-text').hidden = false;
      loginBtn.querySelector('.btn-arrow').hidden = false;
      loginBtn.querySelector('.btn-loader').hidden = true;
      loginBtn.disabled = false;
    }
  });

  // ── LOGOUT ──
  logoutBtn.addEventListener('click', () => {
    files = [];
    parsedResults = [];
    renderFileList();
    resetResult();
    loginForm.reset();
    loginError.textContent = '';
    dashView.hidden = true;
    loginView.hidden = false;
  });

  // ── FILE HANDLING ──
  selectFileBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    fileInput.click();
  });

  dropzone.addEventListener('click', () => fileInput.click());

  dropzone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropzone.classList.add('dragover');
  });
  dropzone.addEventListener('dragleave', () => dropzone.classList.remove('dragover'));
  dropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropzone.classList.remove('dragover');
    addFiles([...e.dataTransfer.files]);
  });

  fileInput.addEventListener('change', () => {
    addFiles([...fileInput.files]);
    fileInput.value = '';
  });

  function addFiles(newFiles) {
    const allowed = ['pdf'];
    newFiles.forEach(f => {
      const ext = f.name.split('.').pop().toLowerCase();
      if (!allowed.includes(ext)) return;
      if (files.find(x => x.name === f.name && x.size === f.size)) return;
      files.push(f);
    });
    renderFileList();
    resetResult();
  }

  function renderFileList() {
    fileItems.innerHTML = '';
    fileCount.textContent = `${files.length}개`;
    parseBtn.disabled = files.length === 0;

    files.forEach((f, i) => {
      const ext = f.name.split('.').pop().toUpperCase();
      const size = f.size > 1024*1024
        ? (f.size/(1024*1024)).toFixed(1)+'MB'
        : (f.size/1024).toFixed(0)+'KB';

      const item = document.createElement('div');
      item.className = 'file-item';
      item.style.animationDelay = `${i * 0.04}s`;
      item.innerHTML = `
        <div class="file-ext">${ext}</div>
        <div class="file-info">
          <div class="file-name">${f.name}</div>
          <div class="file-size">${size}</div>
        </div>
        <button class="file-remove" data-idx="${i}" title="제거">×</button>
      `;
      fileItems.appendChild(item);
    });

    fileItems.querySelectorAll('.file-remove').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        files.splice(+btn.dataset.idx, 1);
        renderFileList();
        resetResult();
      });
    });
  }

  // ── PARSE ──
  parseBtn.addEventListener('click', async () => {
    if (files.length === 0) return;

    parseBtn.disabled = true;
    parseBtn.innerHTML = `
      <svg viewBox="0 0 24 24" width="16" height="16"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2" fill="none" stroke-dasharray="31.4" stroke-dashoffset="10"><animateTransform attributeName="transform" type="rotate" from="0 12 12" to="360 12
  12" dur="0.8s" repeatCount="indefinite"/></circle></svg>
      분석 중...
    `;

    parsedResults = [];
    const errors = [];

    for (const file of files) {
      try {
        const formData = new FormData();
        formData.append('file', file);
        const res = await fetch(API_BASE + '/parse', {
          method: 'POST',
          body: formData,
        });
        if (res.ok) {
          const result = await res.json();
          result._saved = false;
          parsedResults.push(result);
        } else {
          const msg = await res.text();
          errors.push(`${file.name}: ${msg}`);
        }
      } catch (e) {
        errors.push(`${file.name}: 서버 연결 실패`);
      }
    }

    if (parsedResults.length > 0) {
      renderResult();
    } else {
      resetResult();
      alert('파싱 실패:\n' + errors.join('\n'));
    }

    parseBtn.disabled = false;
    parseBtn.innerHTML = `
      <svg viewBox="0 0 24 24" width="16" height="16" fill="none">
        <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
      </svg>
      파싱 시작
    `;
  });

  function renderResult() {
    const cols = ['파일명', '도면번호', '품명', '회사명', '작성', '설계', '제도', '승인', '검도', '신뢰도', 'OCR 원문'];
    const now = new Date().toLocaleString('ko-KR');
    const fileNames = files.map(f => f.name).join(', ');

    resultMeta.innerHTML =
      `<b>파일:</b> ${fileNames} &nbsp;|&nbsp; <b>추출 행:</b> ${parsedResults.length}건 &nbsp;|&nbsp; <b>분석 완료:</b> ${now}`;

    resultThead.innerHTML = `<tr>${cols.map(c => `<th>${c}</th>`).join('')}</tr>`;
    resultTbody.innerHTML = '';

    parsedResults.forEach((row, i) => {
      const tr = document.createElement('tr');
      tr.style.animationDelay = `${i * 0.06}s`;
      tr.dataset.idx = i;
      const confidence = row.confidence || 0;
      const confColor = confidence >= 80 ? '#4ade80' : confidence >= 40 ? '#facc15' : '#f87171';
      const ocrPreview = (row.ocrText || '').substring(0, 40).replace(/\n/g, ' ');
      tr.innerHTML = `
        <td>${row.pdfName || ''}</td>
        <td>${row.dwgNo || '-'}</td>
        <td>${row.partDesc || '-'}</td>
        <td>${row.company || '-'}</td>
        <td>${row.writer || '-'}</td>
        <td>${row.designer || '-'}</td>
        <td>${row.drafter || '-'}</td>
        <td>${row.approver || '-'}</td>
        <td>${row.reviewer || '-'}</td>
        <td style="color:${confColor};font-weight:600">${confidence}%</td>
        <td><button class="btn-ocr-detail" data-idx="${i}" title="OCR 원문 보기">…</button></td>
      `;
      resultTbody.appendChild(tr);

      // OCR 원문 펼침 행
      const detailTr = document.createElement('tr');
      detailTr.className = 'ocr-detail-row';
      detailTr.id = `ocr-detail-${i}`;
      detailTr.hidden = true;
      detailTr.innerHTML = `<td colspan="${cols.length}"><pre class="ocr-pre">${row.ocrText || '(없음)'}</pre></td>`;
      resultTbody.appendChild(detailTr);
    });

    resultTbody.querySelectorAll('.btn-ocr-detail').forEach(btn => {
      btn.addEventListener('click', () => {
        const detailRow = document.getElementById(`ocr-detail-${btn.dataset.idx}`);
        detailRow.hidden = !detailRow.hidden;
        btn.textContent = detailRow.hidden ? '…' : '▲';
      });
    });

    resultEmpty.hidden = true;
    resultContent.hidden = false;
    resultBadge.hidden = false;
  }

  function resetResult() {
    parsedResults = [];
    resultEmpty.hidden = false;
    resultContent.hidden = true;
    resultBadge.hidden = true;
    resultThead.innerHTML = '';
    resultTbody.innerHTML = '';
    resultMeta.textContent = '';
  }

  // ── EXPORT CSV ──
  document.addEventListener('click', async (e) => {
    if (e.target.closest('.btn-export')) {
      if (parsedResults.length === 0) return;
      const cols = ['pdfName','dwgNo','partDesc','company','writer','designer','drafter','approver','reviewer','confidence'];
      const headers = ['파일명','도면번호','품명','회사명','작성','설계','제도','승인','검도','신뢰도'];
      const csv = [
        headers.join(','),
        ...parsedResults.map(r => cols.map(c => `"${r[c] || ''}"`).join(','))
      ].join('\n');
      const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'drawscan_result.csv';
      a.click();
    }

    if (e.target.closest('.btn-save-db')) {
      const btn = e.target.closest('.btn-save-db');
      if (parsedResults.length === 0) return;

      btn.disabled = true;
      btn.textContent = '저장 중...';

      let saved = 0;
      for (const result of parsedResults) {
        if (result._saved) continue;
        try {
          result.savedBy = loggedInUser;
          const res = await fetch(API_BASE + '/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(result),
          });
          if (res.ok) {
            result._saved = true;
            saved++;
          }
        } catch (e) {
          // 개별 실패 무시하고 계속
        }
      }

      btn.textContent = `✓ ${saved}건 저장 완료`;
      btn.style.color = 'var(--green)';
      btn.style.borderColor = 'var(--green)';
      setTimeout(() => {
        btn.innerHTML = `<svg viewBox="0 0 24 24" width="14" height="14" fill="none"><path d="M19 21H5a2 2 0 01-2-2V5a2 2 0 012-2h11l5 5v11a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.5"/><polyline points="17 21 17 13 7 13 7 21" stroke="currentColor"
  stroke-width="1.5"/><polyline points="7 3 7 8 15 8" stroke="currentColor" stroke-width="1.5"/></svg> DB 저장`;
        btn.style.color = '';
        btn.style.borderColor = '';
        btn.disabled = false;
      }, 3000);
    }
  });