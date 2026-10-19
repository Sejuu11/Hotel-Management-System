/**
 * Main Application Orchestrator & Router
 */

const routes = {
  dashboard: renderDashboard,
  rooms: renderRooms,
  customers: renderCustomers,
  bookings: renderBookings,
  checkin: renderCheckInQueue,
  checkout: renderCheckOutView,
  payments: renderPaymentsView,
  reports: renderReportsView,
  employees: renderEmployeesView,
};

let currentView = 'dashboard';

function navigateTo(viewName) {
  if (!routes[viewName]) {
    viewName = 'dashboard';
  }
  currentView = viewName;
  window.location.hash = viewName;

  // Update active sidebar item
  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach(item => {
    if (item.dataset.view === viewName) {
      item.classList.add('active');
    } else {
      item.classList.remove('active');
    }
  });

  // Close mobile sidebar if open
  const sidebar = document.getElementById('sidebar');
  if (sidebar && sidebar.classList.contains('open')) {
    sidebar.classList.remove('open');
  }

  // Execute view renderer
  routes[viewName]();
}

async function checkInitialSession() {
  const token = API.getToken();
  if (!token) {
    showLoginScreen();
    return;
  }

  try {
    const data = await API.get('/auth/me');
    API.setUser(data.user);
    showAppScreen(data.user);

    // Navigate to initial hash or default to dashboard
    const initialHash = window.location.hash.replace('#', '') || 'dashboard';
    navigateTo(initialHash);
  } catch (err) {
    showLoginScreen();
  }
}

document.addEventListener('DOMContentLoaded', () => {
  initAuth();

  // Mobile sidebar toggle
  const sidebarToggle = document.getElementById('sidebar-toggle');
  const sidebar = document.getElementById('sidebar');
  if (sidebarToggle && sidebar) {
    sidebarToggle.addEventListener('click', () => {
      sidebar.classList.toggle('open');
    });
  }

  // Navigation click listeners
  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach(item => {
    item.addEventListener('click', (e) => {
      e.preventDefault();
      const view = item.dataset.view;
      if (view) {
        navigateTo(view);
      }
    });
  });

  // Handle hash changes
  window.addEventListener('hashchange', () => {
    const hash = window.location.hash.replace('#', '');
    if (hash && hash !== currentView && routes[hash]) {
      navigateTo(hash);
    }
  });

  // Check login session
  checkInitialSession();
});
