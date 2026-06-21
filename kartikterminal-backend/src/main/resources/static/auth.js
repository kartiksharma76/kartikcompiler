/**
 * auth.js  —  KartikTerminal Authentication Manager
 *
 * Include this FIRST in compiler.html:
 *   <script src="/auth.js"></script>
 *
 * What it does:
 *  1. On page load: checks for ?token= (from Google OAuth redirect)
 *  2. Reads JWT from localStorage
 *  3. Redirects to /login.html if not authenticated
 *  4. Adds Authorization header to every /compiler/run fetch()
 *  5. Shows user info (name, avatar) in the navbar
 *  6. Provides global KTAuth object for other scripts
 */

const KTAuth = (() => {
  const TOKEN_KEY    = 'kt_token';
  const USERNAME_KEY = 'kt_username';
  const EMAIL_KEY    = 'kt_email';
  const FULLNAME_KEY = 'kt_fullname';
  const ROLE_KEY     = 'kt_role';
  const USERID_KEY   = 'kt_userid';

  // ── 1. Grab token from URL if redirected from Google OAuth ──
  function captureOAuthToken() {
    const params = new URLSearchParams(window.location.search);
    const token    = params.get('token');
    const username = params.get('username');
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
      if (username) localStorage.setItem(USERNAME_KEY, username);
      // Clean URL — remove ?token= from address bar
      window.history.replaceState({}, document.title, window.location.pathname);
      // Fetch full profile from backend
      fetchAndStoreProfile(token);
    }
  }

  // ── 2. Fetch user profile after OAuth login ──
  async function fetchAndStoreProfile(token) {
    try {
      const res = await fetch('/api/auth/me', {
        headers: { 'Authorization': 'Bearer ' + token }
      });
      if (res.ok) {
        const data = await res.json();
        localStorage.setItem(USERNAME_KEY, data.username || '');
        localStorage.setItem(EMAIL_KEY,    data.email    || '');
        localStorage.setItem(FULLNAME_KEY, data.fullName || data.username || '');
        localStorage.setItem(ROLE_KEY,     data.role     || 'USER');
        localStorage.setItem(USERID_KEY,   data.id       || '');
        updateNavbarUI();
      }
    } catch (e) {
      console.warn('[KTAuth] Could not fetch profile:', e);
    }
  }

  // ── 3. Validate session — redirect to login if missing/expired ──
  function requireAuth() {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) {
      redirectToLogin('No token found');
      return false;
    }
    // Simple JWT expiry check (decode payload without library)
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      if (payload.exp && Date.now() / 1000 > payload.exp) {
        logout('Session expired. Please login again.');
        return false;
      }
    } catch (e) {
      redirectToLogin('Invalid token');
      return false;
    }
    return true;
  }

  // ── 4. Get stored JWT ──
  function getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  // ── 5. Logout ──
  function logout(message) {
    // Tell backend (fire-and-forget)
    const token = getToken();
    if (token) {
      fetch('/api/auth/logout', {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + token }
      }).catch(() => {});
    }
    // Clear all stored data
    [TOKEN_KEY, USERNAME_KEY, EMAIL_KEY, FULLNAME_KEY, ROLE_KEY, USERID_KEY]
      .forEach(k => localStorage.removeItem(k));
    redirectToLogin(message);
  }

  function redirectToLogin(reason) {
    const msg = reason ? `?msg=${encodeURIComponent(reason)}` : '';
    window.location.href = '/login.html' + msg;
  }

  // ── 6. Patch the global fetch to auto-add Authorization header ──
  // Only patches requests to /compiler/run and /api/ paths
  function patchFetch() {
    const originalFetch = window.fetch;
    window.fetch = function(url, options = {}) {
      const urlStr = typeof url === 'string' ? url : url.toString();
      const needsAuth = urlStr.includes('/compiler/run') || urlStr.includes('/api/');

      if (needsAuth) {
        const token = getToken();
        if (token) {
          options.headers = {
            ...options.headers,
            'Authorization': 'Bearer ' + token
          };
        }
      }
      return originalFetch.call(this, url, options);
    };
  }

  // ── 7. Inject user info into compiler.html navbar ──
  function updateNavbarUI() {
    const username = localStorage.getItem(USERNAME_KEY) || 'User';
    const fullName = localStorage.getItem(FULLNAME_KEY) || username;
    const role     = localStorage.getItem(ROLE_KEY)     || 'USER';

    // Inject user widget into .navbar .controls if not already there
    if (!document.getElementById('kt-user-widget')) {
      const navbar = document.querySelector('.navbar .controls');
      if (!navbar) return;

      const widget = document.createElement('div');
      widget.id = 'kt-user-widget';
      widget.style.cssText = `
        display: flex; align-items: center; gap: 8px;
        background: var(--bg-panel); padding: 5px 12px 5px 8px;
        border: 1px solid var(--border); border-radius: var(--radius-md);
        font-size: 12px; cursor: pointer; position: relative;
      `;

      const initials = fullName.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
      widget.innerHTML = `
        <div style="width:26px;height:26px;border-radius:50%;background:#2563eb;
             display:flex;align-items:center;justify-content:center;
             font-size:11px;font-weight:700;color:#fff;" id="kt-avatar">${initials}</div>
        <div style="line-height:1.3;">
          <div style="color:var(--text-main);font-weight:600;">${username}</div>
          <div style="color:var(--text-muted);font-size:10px;">${role}</div>
        </div>
        <div id="kt-user-menu" style="
          display:none; position:absolute; top:110%; right:0;
          background:var(--bg-panel); border:1px solid var(--border);
          border-radius:var(--radius-md); min-width:160px; z-index:200;
          box-shadow:0 8px 24px rgba(0,0,0,0.4); overflow:hidden;
        ">
          <div style="padding:10px 14px 6px; font-size:11px; color:var(--text-muted);">Signed in as</div>
          <div style="padding:0 14px 10px; font-size:13px; font-weight:600;">${fullName}</div>
          <hr style="border-color:var(--border); margin:0;">
          <a href="/dashboard.html" style="display:block;padding:10px 14px;font-size:12px;
             color:var(--text-main);text-decoration:none;" onmouseover="this.style.background='var(--border)'" onmouseout="this.style.background=''">
            📊 Dashboard
          </a>
          <a href="/quiz.html" style="display:block;padding:10px 14px;font-size:12px;
             color:var(--text-main);text-decoration:none;" onmouseover="this.style.background='var(--border)'" onmouseout="this.style.background=''">
            📝 Quizzes
          </a>
          <a href="/resume.html" style="display:block;padding:10px 14px;font-size:12px;
             color:var(--text-main);text-decoration:none;" onmouseover="this.style.background='var(--border)'" onmouseout="this.style.background=''">
            📄 Resume
          </a>
          <a href="/chat.html" style="display:block;padding:10px 14px;font-size:12px;
             color:var(--text-main);text-decoration:none;" onmouseover="this.style.background='var(--border)'" onmouseout="this.style.background=''">
            💬 Chat
          </a>
          <a href="/leaderboard.html" style="display:block;padding:10px 14px;font-size:12px;
             color:var(--text-main);text-decoration:none;" onmouseover="this.style.background='var(--border)'" onmouseout="this.style.background=''">
            🏆 Leaderboard
          </a>
          <a href="/intelligence.html" style="display:block;padding:10px 14px;font-size:12px;
             color:var(--text-main);text-decoration:none;" onmouseover="this.style.background='var(--border)'" onmouseout="this.style.background=''">
            ✨ AI Suite
          </a>
          <hr style="border-color:var(--border); margin:0;">
          <div onclick="KTAuth.logout()" style="padding:10px 14px;font-size:12px;
             color:#ef4444;cursor:pointer;" onmouseover="this.style.background='var(--border)'" onmouseout="this.style.background=''">
            ⏏ Logout
          </div>
        </div>
      `;

      // Toggle menu on click
      widget.addEventListener('click', (e) => {
        if (!e.target.closest('#kt-user-menu a') && !e.target.closest('[onclick]')) {
          const menu = document.getElementById('kt-user-menu');
          if (menu) menu.style.display = menu.style.display === 'none' ? 'block' : 'none';
        }
      });

      // Close menu when clicking outside
      document.addEventListener('click', (e) => {
        if (!e.target.closest('#kt-user-widget')) {
          const menu = document.getElementById('kt-user-menu');
          if (menu) menu.style.display = 'none';
        }
      });

      // Insert before Run button
      const runBtn = document.getElementById('runBtn');
      if (runBtn) navbar.insertBefore(widget, runBtn);
      else navbar.appendChild(widget);
    }

    // Update avatar URL if Google profile pic exists
    // (Called after profile fetch completes)
  }

  // ── 8. Load avatar image if available ──
  async function loadGoogleAvatar() {
    const token = getToken();
    if (!token) return;
    try {
      const res = await fetch('/api/auth/me', {
        headers: { 'Authorization': 'Bearer ' + token }
      });
      if (!res.ok) return;
      const data = await res.json();
      if (data.avatarUrl) {
        const el = document.getElementById('kt-avatar');
        if (el) {
          el.style.background = 'transparent';
          el.innerHTML = `<img src="${data.avatarUrl}" style="width:26px;height:26px;border-radius:50%;object-fit:cover;" alt="avatar">`;
        }
      }
    } catch (e) { /* silently fail */ }
  }

  // ── INIT — runs on every page that includes this script ──
  function init() {
    captureOAuthToken();   // Step 1: capture ?token= from URL
    if (!requireAuth()) return;  // Step 2: check auth, redirect if needed
    patchFetch();          // Step 3: patch fetch to auto-send JWT
    updateNavbarUI();      // Step 4: show user in navbar

    // Defer avatar load
    window.addEventListener('DOMContentLoaded', () => {
      updateNavbarUI();
      setTimeout(loadGoogleAvatar, 500);
    });
  }

  init();

  // Public API
  return {
    getToken,
    logout: () => logout('You have been logged out.'),
    getUsername:  () => localStorage.getItem(USERNAME_KEY) || 'User',
    getFullName:  () => localStorage.getItem(FULLNAME_KEY) || 'User',
    getRole:      () => localStorage.getItem(ROLE_KEY)     || 'USER',
    getUserId:    () => localStorage.getItem(USERID_KEY)   || null,
    isAdmin:      () => localStorage.getItem(ROLE_KEY) === 'ADMIN',
    updateNavbar: updateNavbarUI,
  };
})();
