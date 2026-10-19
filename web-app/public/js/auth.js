/**
 * Authentication Module & Session Management
 */

let cachedDemoCreds = {
  admin: { username: 'admin', password: 'admin123' },
  receptionist: { username: 'receptionist', password: 'recep123' },
};

async function fetchDemoCredentials() {
  try {
    const data = await API.get('/auth/demo-credentials');
    if (data && data.accounts) {
      if (data.accounts.admin) cachedDemoCreds.admin = data.accounts.admin;
      if (data.accounts.receptionist) cachedDemoCreds.receptionist = data.accounts.receptionist;
    }
  } catch (err) {
    // Non-fatal; fallback to defaults
  }
}

function initAuth() {
  const loginForm = document.getElementById('login-form');
  const demoAdminBtn = document.getElementById('demo-admin-btn');
  const demoRecepBtn = document.getElementById('demo-recep-btn');
  const logoutBtn = document.getElementById('logout-btn');

  // Pre-fetch dynamic demo credentials configured on the server
  fetchDemoCredentials();

  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      await handleLogin();
    });
  }

  // 1-Click Demo Buttons for Interviewers
  if (demoAdminBtn) {
    demoAdminBtn.addEventListener('click', async () => {
      const creds = cachedDemoCreds.admin || { username: 'admin', password: 'admin123' };
      document.getElementById('username').value = creds.username;
      document.getElementById('password').value = creds.password;
      await handleLogin();
    });
  }

  if (demoRecepBtn) {
    demoRecepBtn.addEventListener('click', async () => {
      const creds = cachedDemoCreds.receptionist || { username: 'receptionist', password: 'recep123' };
      document.getElementById('username').value = creds.username;
      document.getElementById('password').value = creds.password;
      await handleLogin();
    });
  }

  if (logoutBtn) {
    logoutBtn.addEventListener('click', async () => {
      try {
        await API.post('/auth/logout', {});
      } catch (e) {}
      API.setToken(null);
      API.setUser(null);
      showLoginScreen();
      showToast('You have signed out successfully.', 'info');
    });
  }
}

async function handleLogin() {
  const usernameInput = document.getElementById('username');
  const passwordInput = document.getElementById('password');
  const submitBtn = document.getElementById('login-submit-btn');
  const alertBox = document.getElementById('login-alert');

  alertBox.style.display = 'none';
  submitBtn.disabled = true;
  submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Authenticating...';

  try {
    const data = await API.post('/auth/login', {
      username: usernameInput.value,
      password: passwordInput.value,
    });

    API.setToken(data.token);
    API.setUser(data.user);

    showToast(`Welcome back, ${data.user.fullName}!`, 'success');
    showAppScreen(data.user);
    navigateTo('dashboard');
  } catch (err) {
    let msg = err.message || 'Login failed. Please check credentials.';
    alertBox.innerHTML = `<strong><i class="fa-solid fa-triangle-exclamation"></i> Error:</strong> ${msg}`;
    alertBox.style.display = 'block';
  } finally {
    submitBtn.disabled = false;
    submitBtn.innerHTML = '<i class="fa-solid fa-right-to-bracket"></i> Sign In to Portal';
  }
}

function showLoginScreen() {
  document.getElementById('login-screen').style.display = 'flex';
  document.getElementById('app-screen').style.display = 'none';
  document.getElementById('password').value = '';
}

function showAppScreen(user) {
  document.getElementById('login-screen').style.display = 'none';
  document.getElementById('app-screen').style.display = 'flex';

  // Update user header details
  const nameEl = document.getElementById('user-display-name');
  const roleEl = document.getElementById('user-display-role');

  if (nameEl) nameEl.textContent = user.fullName || user.username;
  if (roleEl) {
    roleEl.textContent = user.role;
    roleEl.className = `badge badge-${user.role.toLowerCase() === 'admin' ? 'role' : 'accent'}`;
  }

  // Toggle admin-only features
  const adminElements = document.querySelectorAll('.admin-only');
  adminElements.forEach((el) => {
    el.style.display = user.role === 'ADMIN' ? 'flex' : 'none';
  });
}
