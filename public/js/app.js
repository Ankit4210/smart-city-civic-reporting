/**
 * SAFE & SMART CITY - CIVIC ISSUE REPORTING SYSTEM
 * Frontend Application Controller (Vanilla JS ES6+)
 * Java Servlet API frontend with static demo content when the API is unavailable
 */

// Global State
let currentUser = null;
let authToken = localStorage.getItem('ssc_token') || null;
let currentCategories = [];
let allIssues = [];
let mainMap = null;
let reportPickerMap = null;
let reportMarker = null;
let mapMarkers = [];
let categoryChartInstance = null;
let statusChartInstance = null;
let isStaticMode = false;
let currentTrackingId = null;
let currentTrackingSnapshot = null;
let currentTrackingIsLive = false;
let trackingRefreshTimer = null;
let trackingRefreshInFlight = false;
let trackingRefreshErrorShown = false;
const MAX_PHOTO_SIZE_BYTES = 25 * 1024 * 1024;
const APP_CONTEXT_PATH = (() => {
  const firstSegment = window.location.pathname.split('/').filter(Boolean)[0] || '';
  return firstSegment && !firstSegment.includes('.') ? `/${firstSegment}` : '';
})();
const CONFIGURED_API_BASE_URL = (window.SMARTCITY_API_BASE_URL || '').trim().replace(/\/+$/, '');

function apiUrl(path) {
  if (CONFIGURED_API_BASE_URL) {
    return `${CONFIGURED_API_BASE_URL}/api${path}`;
  }
  return `${APP_CONTEXT_PATH}/api${path}`;
}

function assetUrl(path) {
  if (!path) return '';
  if (/^(data:|https?:\/\/)/i.test(path)) return path;
  if (CONFIGURED_API_BASE_URL) {
    return `${CONFIGURED_API_BASE_URL}/${path.replace(/^\/+/, '')}`;
  }
  return `${APP_CONTEXT_PATH}/${path.replace(/^\/+/, '')}`;
}

async function requestApi(path, options = {}) {
  let response;
  try {
    response = await fetch(apiUrl(path), options);
  } catch (error) {
    setBackendStatus(false);
    throw new Error('Cannot reach the backend. Start Tomcat and check the MySQL connection.');
  }
  let data;
  try {
    data = await response.json();
  } catch {
    const contentType = response.headers.get('content-type') || 'unknown content type';
    throw new Error(
      `The API returned a non-JSON response (${response.status}, ${contentType}). `
      + 'If this site is on GitHub Pages, deploy the Java backend and set SMARTCITY_API_BASE_URL in js/api-config.js.'
    );
  }
  setBackendStatus(true);
  if (!response.ok || !data || data.success !== true) {
    throw new Error(data?.message || data?.error || `Request failed (${response.status}).`);
  }
  return data;
}

function setBackendStatus(isOnline) {
  const banner = document.getElementById('backend-status');
  if (!banner) return;
  banner.hidden = isOnline;
}

// Default Seed Data for GitHub Pages / Static Hosting
const DEFAULT_STATIC_CATEGORIES = [
  { id: 1, name: 'Garbage & Waste Accumulation', code: 'GARBAGE', icon: 'fa-trash-can', department: 'Solid Waste Management', sla_hours: 24, description: 'Overflowing bins, illegal garbage dumps, bio-waste' },
  { id: 2, name: 'Potholes & Damaged Roads', code: 'ROADS', icon: 'fa-road-circle-exclamation', department: 'Roads & Infrastructure', sla_hours: 72, description: 'Road craters, broken tarmac, missing manhole covers' },
  { id: 3, name: 'Streetlight Failure & Dark Spots', code: 'LIGHTING', icon: 'fa-lightbulb', department: 'Electrical Engineering', sla_hours: 24, description: 'Non-functional lamps, hanging wires, low lighting' },
  { id: 4, name: 'Water Leakage & Pipeline Burst', code: 'WATER', icon: 'fa-faucet-drip', department: 'Water Supply & Sewerage', sla_hours: 12, description: 'Drinking water pipeline bursts, contaminated supply' },
  { id: 5, name: 'Drainage & Sewage Overflow', code: 'DRAINAGE', icon: 'fa-water', department: 'Public Health Engineering', sla_hours: 24, description: 'Blocked storm drains, open sewage overflow' },
  { id: 6, name: 'Public Littering & Urination', code: 'HYGIENE', icon: 'fa-hand-sparkles', department: 'Sanitation & Health', sla_hours: 48, description: 'Public hygiene violations, unsanitary public spaces' },
  { id: 7, name: 'Broken Footpath & Encroachment', code: 'FOOTPATH', icon: 'fa-person-walking-dashed-line-arrow-right', department: 'Town Planning & Enforcement', sla_hours: 96, description: 'Destroyed pedestrian walkways, unauthorized stalls' }
];

const DEFAULT_STATIC_ISSUES = [
  {
    id: 1,
    tracking_id: 'SSC-2026-8812',
    user_id: 3,
    category_id: 1,
    category_name: 'Garbage & Waste Accumulation',
    category_icon: 'fa-trash-can',
    department_name: 'Solid Waste Management',
    sla_hours: 24,
    title: 'Overflowing garbage bin near Central Market Gate 2',
    description: 'Garbage has not been collected for the past 4 days. Foul smell and stray animals causing severe public hazard.',
    landmark: 'Opposite State Bank ATM, Central Market',
    address: 'Market Road, Ward 12 - Central',
    ward: 'Ward 12 - Central',
    latitude: 28.6139,
    longitude: 77.2090,
    before_photo: 'sample_garbage_before.jpg',
    after_photo: null,
    priority: 'High',
    status: 'In Progress',
    upvotes: 14,
    created_at: '2026-09-04T10:30:00Z',
    timeline: [
      { action: 'Report Submitted', actor_name: 'Demo Citizen One', details: 'Civic complaint registered with Tracking ID: SSC-2026-8812', created_at: '2026-09-04T10:30:00Z' },
      { action: 'Status Changed to In Progress', actor_name: 'Ward Officer Rajesh Verma', details: 'Sanitation truck #14 dispatched to Central Market area.', created_at: '2026-09-05T09:15:00Z' }
    ]
  },
  {
    id: 2,
    tracking_id: 'SSC-2026-9041',
    user_id: 4,
    category_id: 2,
    category_name: 'Potholes & Damaged Roads',
    category_icon: 'fa-road-circle-exclamation',
    department_name: 'Roads & Infrastructure',
    sla_hours: 72,
    title: 'Deep hazardous pothole on Main Ring Road curve',
    description: 'Large crater on the road causing traffic slowdowns and two-wheeler skids especially during nighttime.',
    landmark: 'Near Metro Pillar 142',
    address: 'North Ring Road, Ward 7 - North',
    ward: 'Ward 7 - North',
    latitude: 28.6328,
    longitude: 77.2197,
    before_photo: 'sample_pothole_before.jpg',
    after_photo: 'sample_pothole_after.jpg',
    resolution_remarks: 'Patching work completed with cold-mix asphalt. Traffic flow restored.',
    action_taken: 'Asphalt Filling & Roller Compaction',
    resolved_at: '2026-09-05T14:20:00Z',
    resolved_by_admin: 'Municipal Commissioner Sharma',
    priority: 'Critical',
    status: 'Resolved',
    upvotes: 28,
    created_at: '2026-09-03T08:00:00Z',
    timeline: [
      { action: 'Report Submitted', actor_name: 'Demo Citizen Two', details: 'Civic complaint registered with Tracking ID: SSC-2026-9041', created_at: '2026-09-03T08:00:00Z' },
      { action: 'Status Changed to In Progress', actor_name: 'Roads Dept Engineer', details: 'Road repair team dispatched with asphalt mixture.', created_at: '2026-09-04T11:00:00Z' },
      { action: 'Issue Resolved', actor_name: 'Municipal Commissioner Sharma', details: 'Official After-Photo uploaded. Road repaved.', created_at: '2026-09-05T14:20:00Z' }
    ],
    feedback: {
      rating: 5,
      is_satisfied: 1,
      comments: 'Very quick resolution within 24 hours! Thank you municipal team.'
    }
  },
  {
    id: 3,
    tracking_id: 'SSC-2026-6734',
    user_id: 3,
    category_id: 3,
    category_name: 'Streetlight Failure & Dark Spots',
    category_icon: 'fa-lightbulb',
    department_name: 'Electrical Engineering',
    sla_hours: 24,
    title: 'Entire row of 4 streetlights dark near Girls Hostel lane',
    description: 'Streetlights out for over a week creating unsafe dark spot for pedestrians at night.',
    landmark: 'Lane 4 behind Community Center',
    address: 'Shanti Path, Ward 12 - Central',
    ward: 'Ward 12 - Central',
    latitude: 28.6180,
    longitude: 77.2150,
    before_photo: 'sample_streetlight_before.jpg',
    after_photo: null,
    priority: 'High',
    status: 'Pending',
    upvotes: 9,
    created_at: '2026-09-05T16:45:00Z',
    timeline: [
      { action: 'Report Submitted', actor_name: 'Demo Citizen One', details: 'Civic complaint registered with Tracking ID: SSC-2026-6734', created_at: '2026-09-05T16:45:00Z' }
    ]
  },
  {
    id: 4,
    tracking_id: 'SSC-2026-4190',
    user_id: 5,
    category_id: 4,
    category_name: 'Water Leakage & Pipeline Burst',
    category_icon: 'fa-faucet-drip',
    department_name: 'Water Supply & Sewerage',
    sla_hours: 12,
    title: 'Major main supply water leakage flooding street',
    description: 'Fresh clean drinking water bursting from underground joint and wasting thousands of liters per hour.',
    landmark: 'Corner of Sector 4 Park',
    address: 'Park Avenue, Ward 4 - East',
    ward: 'Ward 4 - East',
    latitude: 28.6050,
    longitude: 77.2280,
    before_photo: 'sample_water_before.jpg',
    after_photo: null,
    priority: 'Critical',
    status: 'In Progress',
    upvotes: 19,
    created_at: '2026-09-06T09:00:00Z',
    timeline: [
      { action: 'Report Submitted', actor_name: 'Demo Citizen Three', details: 'Civic complaint registered with Tracking ID: SSC-2026-4190', created_at: '2026-09-06T09:00:00Z' },
      { action: 'Status Changed to In Progress', actor_name: 'Water Works Division', details: 'Supply valve isolated, repair team excavating pipe joint.', created_at: '2026-09-06T10:30:00Z' }
    ]
  }
];

function initLocalStorageData() {
  if (!localStorage.getItem('ssc_static_issues')) {
    localStorage.setItem('ssc_static_issues', JSON.stringify(DEFAULT_STATIC_ISSUES));
  }
  if (!localStorage.getItem('ssc_static_categories')) {
    localStorage.setItem('ssc_static_categories', JSON.stringify(DEFAULT_STATIC_CATEGORIES));
  }
}

// Initialize on DOM Ready
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  initLocalStorageData();
  checkAuthSession();
  loadCategories();
  loadStats();
  loadIssues();
  loadLeaderboard();
  setupEventListeners();
});

// -------------------------------------------------------------
// 1. THEME & INITIALIZATION
// -------------------------------------------------------------

function initTheme() {
  const savedTheme = localStorage.getItem('ssc_theme') || 'light';
  document.documentElement.setAttribute('data-theme', savedTheme);
  updateThemeToggleIcon(savedTheme);

  const themeBtn = document.getElementById('theme-toggle-btn');
  if (themeBtn) {
    themeBtn.addEventListener('click', () => {
      const current = document.documentElement.getAttribute('data-theme');
      const next = current === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      localStorage.setItem('ssc_theme', next);
      updateThemeToggleIcon(next);
    });
  }
}

function updateThemeToggleIcon(theme) {
  const btn = document.getElementById('theme-toggle-btn');
  if (!btn) return;
  if (theme === 'dark') {
    btn.innerHTML = '<i class="fa-solid fa-sun" style="color: #fbbf24;"></i>';
  } else {
    btn.innerHTML = '<i class="fa-solid fa-moon"></i>';
  }
}

function setupEventListeners() {
  window.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) {
      closeModal(e.target.id);
    }
  });

  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      document.querySelectorAll('.modal-overlay.open').forEach((modal) => closeModal(modal.id));
      const drawer = document.getElementById('notif-drawer');
      if (drawer) drawer.classList.remove('open');
    }
  });
}

// -------------------------------------------------------------
// 2. AUTHENTICATION & SESSION MANAGEMENT
// -------------------------------------------------------------

async function checkAuthSession() {
  if (!authToken) {
    currentUser = null;
    localStorage.removeItem('ssc_user');
    updateAuthUI(null);
    return;
  }

  try {
    const data = await requestApi('/auth/me', {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    if (data.success) {
      currentUser = data.user;
      localStorage.setItem('ssc_user', JSON.stringify(currentUser));
      updateAuthUI(currentUser);
      loadNotifications();
    } else {
      handleLogout();
    }
  } catch (err) {
    authToken = null;
    currentUser = null;
    localStorage.removeItem('ssc_token');
    localStorage.removeItem('ssc_user');
    updateAuthUI(null);
    showToast(err.message || 'Your session has expired. Please log in again.', 'warning');
  }
}

function updateAuthUI(user) {
  const loggedOutDiv = document.getElementById('auth-logged-out');
  const loggedInDiv = document.getElementById('auth-logged-in');
  const pointsBadge = document.getElementById('user-points-badge');
  const pointsVal = document.getElementById('user-points-val');
  const notifBell = document.getElementById('notif-bell-btn');
  const navUserName = document.getElementById('nav-user-name');
  const navMyReports = document.getElementById('nav-my-reports');
  const navAdmin = document.getElementById('nav-admin');

  if (user) {
    if (loggedOutDiv) loggedOutDiv.style.display = 'none';
    if (loggedInDiv) loggedInDiv.style.display = 'flex';
    if (pointsBadge) pointsBadge.style.display = 'inline-flex';
    if (pointsVal) pointsVal.textContent = user.civic_points || 50;
    if (notifBell) notifBell.style.display = 'flex';
    if (navUserName) navUserName.textContent = user.name.split(' ')[0];
    if (navMyReports) navMyReports.style.display = 'flex';

    if (user.role === 'admin') {
      if (navAdmin) navAdmin.style.display = 'flex';
    } else {
      if (navAdmin) navAdmin.style.display = 'none';
    }
  } else {
    if (loggedOutDiv) loggedOutDiv.style.display = 'block';
    if (loggedInDiv) loggedInDiv.style.display = 'none';
    if (pointsBadge) pointsBadge.style.display = 'none';
    if (notifBell) notifBell.style.display = 'none';
    if (navMyReports) navMyReports.style.display = 'none';
    if (navAdmin) navAdmin.style.display = 'none';
  }
}

function openAuthModal(tab = 'login') {
  toggleAuthTab(tab);
  openModal('modal-auth');
}

function toggleAuthTab(tab) {
  const tabLogin = document.getElementById('auth-tab-login');
  const tabRegister = document.getElementById('auth-tab-register');
  const formLogin = document.getElementById('login-form');
  const formRegister = document.getElementById('register-form');
  const title = document.getElementById('auth-modal-title');

  if (tab === 'login') {
    if (tabLogin) tabLogin.classList.add('active');
    if (tabRegister) tabRegister.classList.remove('active');
    if (formLogin) formLogin.style.display = 'block';
    if (formRegister) formRegister.style.display = 'none';
    if (title) title.textContent = 'Citizen & Municipal Portal Access';
  } else {
    if (tabRegister) tabRegister.classList.add('active');
    if (tabLogin) tabLogin.classList.remove('active');
    if (formRegister) formRegister.style.display = 'block';
    if (formLogin) formLogin.style.display = 'none';
    if (title) title.textContent = 'Join as a Proactive Smart Citizen';
  }
}

async function handleLoginSubmit(e) {
  e.preventDefault();
  const email = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-password').value;

  try {
    const data = await requestApi('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    authToken = data.token;
    localStorage.setItem('ssc_token', authToken);
    currentUser = data.user;
    localStorage.setItem('ssc_user', JSON.stringify(currentUser));
    updateAuthUI(currentUser);
    closeModal('modal-auth');
    showToast(data.message, 'success');
    loadNotifications();
    loadIssues();

    if (currentUser.role === 'admin') {
      switchTab('admin');
    }
    return;
  } catch (err) {
    showToast(err.message || 'Unable to log in. Check the server connection and try again.', 'error');
    return;
  }
}

async function handleRegisterSubmit(e) {
  e.preventDefault();
  const name = document.getElementById('reg-name').value;
  const email = document.getElementById('reg-email').value;
  const phone = document.getElementById('reg-phone').value;
  const ward = document.getElementById('reg-ward').value;
  const password = document.getElementById('reg-password').value;

  try {
    const data = await requestApi('/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, phone, ward, password })
    });

    authToken = data.token;
    localStorage.setItem('ssc_token', authToken);
    currentUser = data.user;
    localStorage.setItem('ssc_user', JSON.stringify(currentUser));
    updateAuthUI(currentUser);
    closeModal('modal-auth');
    showToast(data.message, 'success');
    loadNotifications();
    loadIssues();
    loadLeaderboard();
    return;
  } catch (err) {
    showToast(err.message || 'Unable to create your account. Please try again.', 'error');
    return;
  }
}

function quickLogin(type) {
  if (type === 'admin') {
    document.getElementById('login-email').value = 'admin@example.test';
    document.getElementById('login-password').value = 'admin123';
  } else {
    document.getElementById('login-email').value = 'citizen.one@example.test';
    document.getElementById('login-password').value = 'citizen123';
  }
  document.getElementById('login-form').dispatchEvent(new Event('submit'));
}

function handleLogout() {
  authToken = null;
  currentUser = null;
  localStorage.removeItem('ssc_token');
  localStorage.removeItem('ssc_user');
  updateAuthUI(null);
  showToast('Logged out successfully.', 'info');
  switchTab('explore');
}

// -------------------------------------------------------------
// 3. NAVIGATION TABS
// -------------------------------------------------------------

function switchTab(tabId) {
  document.querySelectorAll('.tab-content').forEach((tab) => {
    tab.style.display = 'none';
  });

  document.querySelectorAll('.nav-link').forEach((link) => {
    link.classList.remove('active');
    if (link.getAttribute('data-tab') === tabId) {
      link.classList.add('active');
    }
  });

  const activeContent = document.getElementById(`tab-${tabId}`);
  if (activeContent) {
    activeContent.style.display = 'block';
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  if (tabId === 'map') {
    setTimeout(initMainMap, 150);
  } else if (tabId === 'my-reports') {
    loadMyReports();
  } else if (tabId === 'leaderboard') {
    loadLeaderboard();
  } else if (tabId === 'admin') {
    loadAdminDashboard();
  }
}

// -------------------------------------------------------------
// 4. CATEGORIES & STATS
// -------------------------------------------------------------

async function loadCategories() {
  try {
    const data = await requestApi('/categories');
    if (data.success && data.categories && data.categories.length > 0) {
      currentCategories = data.categories;
      renderCategoryCatalog(data.categories);
      populateCategorySelects(data.categories);
      return;
    }
  } catch (err) {
    isStaticMode = true;
  }

  // Fallback static categories
  currentCategories = DEFAULT_STATIC_CATEGORIES;
  renderCategoryCatalog(DEFAULT_STATIC_CATEGORIES);
  populateCategorySelects(DEFAULT_STATIC_CATEGORIES);
}

function renderCategoryCatalog(categories) {
  const grid = document.getElementById('category-catalog-grid');
  if (!grid) return;

  grid.innerHTML = categories
    .map(
      (cat) => `
    <div class="category-card" onclick="filterByCategory(${cat.id})">
      <div class="cat-icon-box" style="background: var(--primary-light); color: var(--primary);">
        <i class="fa-solid ${cat.icon}"></i>
      </div>
      <div class="cat-title">${cat.name}</div>
      <div class="cat-dept">${cat.department}</div>
      <div class="cat-sla"><i class="fa-solid fa-clock"></i> SLA: ${cat.sla_hours} Hours</div>
    </div>
  `
    )
    .join('');
}

function populateCategorySelects(categories) {
  const filterCat = document.getElementById('filter-category');
  const reportCat = document.getElementById('report-category');

  if (filterCat) {
    filterCat.innerHTML = '<option value="all">All Categories</option>' +
      categories.map((c) => `<option value="${c.id}">${c.name}</option>`).join('');
  }

  if (reportCat) {
    reportCat.innerHTML = '<option value="">-- Select Problem Category --</option>' +
      categories.map((c) => `<option value="${c.id}">${c.name} (${c.department})</option>`).join('');
  }
}

function filterByCategory(categoryId) {
  const filterCat = document.getElementById('filter-category');
  if (filterCat) {
    filterCat.value = categoryId;
  }
  switchTab('explore');
  loadIssues();
  const issuesContainer = document.getElementById('issues-container');
  if (issuesContainer) {
    issuesContainer.scrollIntoView({ behavior: 'smooth' });
  }
}

async function loadStats() {
  try {
    const data = await requestApi('/stats', {
      headers: authToken ? { Authorization: `Bearer ${authToken}` } : {}
    });
    if (data.success && data.stats) {
      document.getElementById('stat-total-issues').textContent = data.stats.total;
      document.getElementById('stat-resolved-issues').textContent = data.stats.resolved;
      document.getElementById('stat-critical-hotspots').textContent = data.stats.critical;
      document.getElementById('stat-active-citizens').textContent = data.stats.citizens || 5;
      return;
    }
  } catch (err) {}

  // Static stats calculation
  const issues = getStoredIssues();
  const resolved = issues.filter((i) => i.status === 'Resolved').length;
  const critical = issues.filter((i) => i.priority === 'Critical' && i.status !== 'Resolved').length;

  const totalEl = document.getElementById('stat-total-issues');
  const resEl = document.getElementById('stat-resolved-issues');
  const critEl = document.getElementById('stat-critical-hotspots');
  const citEl = document.getElementById('stat-active-citizens');

  if (totalEl) totalEl.textContent = issues.length;
  if (resEl) resEl.textContent = resolved;
  if (critEl) critEl.textContent = critical;
  if (citEl) citEl.textContent = '5';
}

function getStoredIssues() {
  try {
    const data = localStorage.getItem('ssc_static_issues');
    return data ? JSON.parse(data) : DEFAULT_STATIC_ISSUES;
  } catch (e) {
    return DEFAULT_STATIC_ISSUES;
  }
}

function saveStoredIssues(issues) {
  localStorage.setItem('ssc_static_issues', JSON.stringify(issues));
}

// -------------------------------------------------------------
// 5. COMMUNITY ISSUES BOARD
// -------------------------------------------------------------

async function loadIssues() {
  const search = document.getElementById('filter-search')?.value || '';
  const categoryId = document.getElementById('filter-category')?.value || 'all';
  const ward = document.getElementById('filter-ward')?.value || 'all';
  const sort = document.getElementById('filter-sort')?.value || 'newest';
  
  const activeStatusBtn = document.querySelector('.pill-btn.active[data-status]');
  const status = activeStatusBtn ? activeStatusBtn.getAttribute('data-status') : 'all';

  try {
    let url = `/api/issues?category_id=${categoryId}&status=${status}&ward=${encodeURIComponent(ward)}&search=${encodeURIComponent(search)}&sort=${sort}`;
    const headers = authToken ? { Authorization: `Bearer ${authToken}` } : {};

    const data = await requestApi(url.replace('/api', ''), { headers });
    if (data.success && data.issues) {
      allIssues = data.issues;
      renderIssues(data.issues, 'issues-container');
      if (mainMap) {
        updateMapMarkers(data.issues);
      }
      return;
    }
  } catch (err) {
    isStaticMode = true;
  }

  // Static mode filter
  let issues = getStoredIssues();

  if (categoryId !== 'all') {
    issues = issues.filter((i) => String(i.category_id) === String(categoryId));
  }
  if (status !== 'all') {
    issues = issues.filter((i) => i.status === status);
  }
  if (ward !== 'all') {
    issues = issues.filter((i) => i.ward === ward);
  }
  if (search.trim() !== '') {
    const q = search.toLowerCase().trim();
    issues = issues.filter(
      (i) =>
        i.title.toLowerCase().includes(q) ||
        i.description.toLowerCase().includes(q) ||
        (i.landmark && i.landmark.toLowerCase().includes(q)) ||
        i.tracking_id.toLowerCase().includes(q)
    );
  }

  if (sort === 'upvotes') {
    issues.sort((a, b) => (b.upvotes || 0) - (a.upvotes || 0));
  } else if (sort === 'oldest') {
    issues.sort((a, b) => new Date(a.created_at) - new Date(b.created_at));
  } else {
    issues.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
  }

  allIssues = issues;
  renderIssues(issues, 'issues-container');
  if (mainMap) {
    updateMapMarkers(issues);
  }
}

function renderIssues(issues, containerId) {
  const container = document.getElementById(containerId);
  if (!container) return;

  if (issues.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; padding: 4rem 1rem; color: var(--text-muted);">
        <i class="fa-solid fa-shield-cat" style="font-size: 3rem; margin-bottom: 1rem; color: var(--text-light);"></i>
        <h3>No Civic Issues Found</h3>
        <p>No complaints match the selected filter criteria or ward.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = issues
    .map((issue) => {
      let statusClass = 'status-pending';
      if (issue.status === 'In Progress') statusClass = 'status-in-progress';
      else if (issue.status === 'Resolved') statusClass = 'status-resolved';

      let priorityClass = '';
      if (issue.priority === 'Critical') priorityClass = 'priority-critical';

      const photoUrl = issue.before_photo ? assetUrl(issue.before_photo) : assetUrl('uploads/sample_garbage_before.jpg');
      const isUpvoted = !!issue.has_upvoted;

      return `
      <div class="issue-card">
        <div class="issue-thumb-wrapper">
          <img src="${photoUrl}" alt="${escapeHtml(issue.title)}" class="issue-thumb" onerror="this.src='uploads/sample_garbage_before.jpg'">
          <span class="status-badge ${statusClass}">${issue.status}</span>
          <span class="priority-pill ${priorityClass}"><i class="fa-solid fa-flag"></i> ${issue.priority}</span>
        </div>

        <div class="issue-body">
          <div class="issue-meta-row">
            <span class="issue-tracking-tag">${issue.tracking_id}</span>
            <span><i class="fa-regular fa-calendar"></i> ${formatDate(issue.created_at)}</span>
          </div>

          <h3 class="issue-card-title">${escapeHtml(issue.title)}</h3>
          <p class="issue-card-desc">${escapeHtml(issue.description)}</p>

          <div class="issue-location-row">
            <i class="fa-solid fa-location-dot" style="color: var(--primary);"></i>
            <span>${escapeHtml(issue.landmark || issue.ward || 'Central Ward')}</span>
          </div>
        </div>

        <div class="issue-footer">
          <button class="upvote-btn ${isUpvoted ? 'upvoted' : ''}" onclick="handleUpvote(${issue.id}, this)">
            <i class="${isUpvoted ? 'fa-solid' : 'fa-regular'} fa-thumbs-up"></i>
            <span class="upvote-count">${issue.upvotes || 1}</span>
          </button>
          
          <button class="btn btn-outline btn-sm" onclick="openIssueDetails(${issue.id})">
            <i class="fa-solid fa-eye"></i> Track & Proof
          </button>
        </div>
      </div>
    `;
    })
    .join('');
}

function filterByStatus(btn, status) {
  document.querySelectorAll('.pill-btn[data-status]').forEach((b) => b.classList.remove('active'));
  btn.classList.add('active');
  loadIssues();
}

let searchDebounceTimeout = null;
function handleSearchFilter() {
  clearTimeout(searchDebounceTimeout);
  searchDebounceTimeout = setTimeout(loadIssues, 300);
}

// -------------------------------------------------------------
// 6. UPVOTING SYSTEM & CIVIC POINTS
// -------------------------------------------------------------

async function handleUpvote(issueId, btn) {
  if (!currentUser) {
    showToast('Please log in to upvote and validate community issues.', 'warning');
    openAuthModal('login');
    return;
  }

  try {
    const data = await requestApi(`/issues/${issueId}/upvote`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${authToken}` }
    });

    if (data.success) {
      const countSpan = btn.querySelector('.upvote-count');
      if (countSpan) countSpan.textContent = data.upvotes;

      const icon = btn.querySelector('i');
      if (data.upvoted) {
        btn.classList.add('upvoted');
        if (icon) icon.className = 'fa-solid fa-thumbs-up';
        showToast(data.message || 'Issue upvoted.', 'success');
      } else {
        btn.classList.remove('upvoted');
        if (icon) icon.className = 'fa-regular fa-thumbs-up';
        showToast(data.message || 'Upvote removed.', 'info');
      }

      checkAuthSession();
      return;
    }
  } catch (err) {
    showToast(err.message || 'Could not update the upvote. Please try again.', 'error');
  }
}

// -------------------------------------------------------------
// 7. REPORT ISSUE MODAL & GEOLOCATION CAPTURE
// -------------------------------------------------------------

function openReportModal() {
  if (!currentUser) {
    showToast('Please log in or register to report a civic complaint.', 'info');
    openAuthModal('login');
    return;
  }
  openModal('modal-report');
  setTimeout(initReportPickerMap, 150);
}

function initReportPickerMap() {
  const container = document.getElementById('report-picker-map');
  if (!container) return;

  const defaultLat = 28.6139;
  const defaultLng = 77.2090;

  if (!reportPickerMap) {
    reportPickerMap = L.map('report-picker-map').setView([defaultLat, defaultLng], 14);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(reportPickerMap);

    reportMarker = L.marker([defaultLat, defaultLng], { draggable: true }).addTo(reportPickerMap);

    reportMarker.on('dragend', (e) => {
      const pos = e.target.getLatLng();
      setCoordinates(pos.lat, pos.lng);
    });

    reportPickerMap.on('click', (e) => {
      reportMarker.setLatLng(e.latlng);
      setCoordinates(e.latlng.lat, e.latlng.lng);
    });

  } else {
    reportPickerMap.invalidateSize();
  }
}

function setCoordinates(lat, lng) {
  const latEl = document.getElementById('report-lat');
  const lngEl = document.getElementById('report-lng');
  if (latEl) latEl.value = lat.toFixed(6);
  if (lngEl) lngEl.value = lng.toFixed(6);
}

function detectLiveGPS() {
  if (!navigator.geolocation) {
    showToast('Live location is not supported by this browser. Click the map to choose the issue location.', 'error');
    return;
  }
  if (!window.isSecureContext) {
    showToast('Live location requires HTTPS or localhost. Open this site securely, or click the map to choose a location.', 'error');
    return;
  }

  showToast('Detecting current live GPS coordinates...', 'info');
  navigator.geolocation.getCurrentPosition(
    (position) => {
      const lat = position.coords.latitude;
      const lng = position.coords.longitude;
      setCoordinates(lat, lng);

      if (reportPickerMap && reportMarker) {
        reportPickerMap.setView([lat, lng], 16);
        reportMarker.setLatLng([lat, lng]);
      }
      showToast('Live GPS location locked successfully!', 'success');
    },
    (err) => {
      const message = err.code === err.PERMISSION_DENIED
        ? 'Location permission was denied. Allow location access in your browser, or click the map to choose a location.'
        : err.code === err.POSITION_UNAVAILABLE
          ? 'Your current location is unavailable. Check device location services, or click the map to choose a location.'
          : 'Getting your location timed out. Try again or click the map to choose a location.';
      showToast(message, 'error');
    },
    { enableHighAccuracy: true, maximumAge: 0, timeout: 15000 }
  );
}

let currentPhotoDataUrl = null;
function validatePhotoFile(file) {
  if (!/^image\/(jpeg|png|webp)$/.test(file.type)) {
    showToast('Choose a JPG, PNG, or WEBP photo.', 'warning');
    return false;
  }
  if (file.size > MAX_PHOTO_SIZE_BYTES) {
    showToast('Each photo must be 25 MB or smaller.', 'warning');
    return false;
  }
  return true;
}

function previewPhoto(input) {
  const previewBox = document.getElementById('photo-preview-box');
  const previewImg = document.getElementById('photo-preview-img');

  if (input.files && input.files[0]) {
    if (!validatePhotoFile(input.files[0])) {
      input.value = '';
      currentPhotoDataUrl = null;
      if (previewBox) previewBox.style.display = 'none';
      if (previewImg) previewImg.removeAttribute('src');
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => {
      currentPhotoDataUrl = e.target.result;
      if (previewImg) previewImg.src = e.target.result;
      if (previewBox) previewBox.style.display = 'block';
    };
    reader.readAsDataURL(input.files[0]);
  }
}

async function handleReportSubmit(e) {
  e.preventDefault();

  const categoryId = document.getElementById('report-category').value;
  const title = document.getElementById('report-title').value;
  const description = document.getElementById('report-description').value;
  const lat = document.getElementById('report-lat').value;
  const lng = document.getElementById('report-lng').value;
  const landmark = document.getElementById('report-landmark').value;
  const ward = document.getElementById('report-ward').value;
  const priority = document.querySelector('input[name="priority"]:checked')?.value || 'Medium';
  const photoInput = document.getElementById('report-photo-input');
  if (!photoInput.files || !photoInput.files[0]) {
    showToast('Add a clear photo of the issue before submitting.', 'warning');
    photoInput.click();
    return;
  }
  if (!validatePhotoFile(photoInput.files[0])) {
    return;
  }
  if (!Number.isFinite(Number(lat)) || !Number.isFinite(Number(lng))
    || Number(lat) < -90 || Number(lat) > 90 || Number(lng) < -180 || Number(lng) > 180) {
    showToast('Set a valid location by selecting a point on the map or using GPS.', 'warning');
    return;
  }

  const formData = new FormData();
  formData.append('category_id', categoryId);
  formData.append('title', title);
  formData.append('description', description);
  formData.append('latitude', lat);
  formData.append('longitude', lng);
  formData.append('landmark', landmark);
  formData.append('ward', ward);
  formData.append('priority', priority);

  formData.append('photo', photoInput.files[0]);

  try {
    const data = await requestApi('/issues', {
      method: 'POST',
      headers: { Authorization: `Bearer ${authToken}` },
      body: formData
    });

    if (data.success) {
      closeModal('modal-report');
      document.getElementById('report-issue-form').reset();
      document.getElementById('photo-preview-box').style.display = 'none';

      if (data.duplicate) {
        showToast(data.message, data.upvoted ? 'success' : 'info');
      } else {
        showToast(`Complaint registered! Tracking ID: ${data.tracking_id} (+50 Points)`, 'success');
      }
      await checkAuthSession();
      loadIssues();
      loadStats();
      currentPhotoDataUrl = null;

      setTimeout(() => {
        quickFillTrack(data.tracking_id);
      }, 500);
      return;
    }
  } catch (err) {
    showToast(err.message || 'Could not submit the report. Please try again.', 'error');
    return;
  }

}

// -------------------------------------------------------------
// 8. PUBLIC TRACKING & DETAILS MODAL
// -------------------------------------------------------------

function handleQuickTrack(e) {
  e.preventDefault();
  const input = document.getElementById('quick-track-input').value.trim();
  if (!input) return;
  quickFillTrack(input);
}

function quickFillTrack(trackingId) {
  const quickInput = document.getElementById('quick-track-input');
  if (quickInput) quickInput.value = trackingId;
  fetchAndOpenTracking(trackingId);
}

async function fetchAndOpenTracking(trackingId) {
  let requestError = null;
  try {
    const data = await requestApi(`/track/${encodeURIComponent(trackingId)}`);
    if (data.success && data.issue) {
      currentTrackingIsLive = true;
      renderTrackDetails(data.issue);
      openModal('modal-track');
      return;
    }
  } catch (err) {
    requestError = err;
  }

  // Static tracking fallback
  const issues = getStoredIssues();
  const issue = issues.find((i) => i.tracking_id.toUpperCase() === trackingId.toUpperCase());
  if (issue) {
    currentTrackingIsLive = false;
    renderTrackDetails(issue);
    openModal('modal-track');
  } else {
    showToast(requestError?.message || `No complaint found for Tracking ID '${trackingId}'. Please check the ID.`, 'error');
  }
}

async function openIssueDetails(issueId) {
  try {
    const headers = authToken ? { Authorization: `Bearer ${authToken}` } : {};
    const data = await requestApi(`/issues/${issueId}`, { headers });
    if (data.success && data.issue) {
      currentTrackingIsLive = true;
      renderTrackDetails(data.issue);
      openModal('modal-track');
      return;
    }
  } catch (err) {}

  // Static details fallback
  const issues = getStoredIssues();
  const issue = issues.find((i) => i.id === issueId);
  if (issue) {
    currentTrackingIsLive = false;
    renderTrackDetails(issue);
    openModal('modal-track');
  } else {
    showToast('Failed to load issue details.', 'error');
  }
}

function renderTrackDetails(issue) {
  currentTrackingId = issue.tracking_id;
  currentTrackingSnapshot = JSON.stringify(issue);
  const modalIdEl = document.getElementById('track-modal-id');
  const modalTitleEl = document.getElementById('track-modal-title');
  if (modalIdEl) modalIdEl.textContent = issue.tracking_id;
  if (modalTitleEl) modalTitleEl.textContent = issue.title;

  const container = document.getElementById('track-modal-content');
  if (!container) return;

  let step1Class = 'completed';
  let step2Class = 'completed';
  let step3Class = '';
  let step4Class = '';

  if (issue.status === 'In Progress') {
    step3Class = 'active';
  } else if (issue.status === 'Resolved') {
    step3Class = 'completed';
    step4Class = 'completed';
  } else {
    step2Class = 'active';
  }

  const beforePhoto = issue.before_photo ? assetUrl(issue.before_photo) : assetUrl('uploads/sample_garbage_before.jpg');
  const afterPhoto = issue.after_photo ? assetUrl(issue.after_photo) : null;
  const feedback = issue.feedback || (issue.feedback_rating > 0 ? {
    rating: issue.feedback_rating,
    is_satisfied: issue.feedback_satisfied,
    comments: issue.feedback_comments
  } : null);

  const timelineHtml = (issue.timeline || [])
    .map(
      (log) => `
    <div style="padding: 0.6rem 0; border-left: 2px solid var(--primary); padding-left: 1rem; margin-left: 0.5rem; position: relative;">
      <div style="position: absolute; left: -6px; top: 12px; width: 10px; height: 10px; border-radius: 50%; background: var(--primary);"></div>
      <div style="font-weight: 700; font-size: 0.85rem;">${escapeHtml(log.action)} <span style="font-weight: 400; color: var(--text-muted); font-size: 0.75rem;">by ${escapeHtml(log.actor_name)} • ${formatDate(log.created_at)}</span></div>
      <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 0.2rem;">${escapeHtml(log.details)}</div>
    </div>
  `
    )
    .join('');

  let feedbackHtml = '';
  if (issue.status === 'Resolved') {
    if (feedback) {
      feedbackHtml = `
        <div style="background: var(--emerald-light); border: 1px solid var(--emerald); border-radius: var(--radius-md); padding: 1rem; margin-top: 1.5rem;">
          <div style="font-weight: 700; color: var(--emerald-hover); font-size: 0.9rem;">
            <i class="fa-solid fa-star"></i> Citizen Resolution Feedback: ${feedback.rating}/5 Stars (${feedback.is_satisfied ? 'Satisfied' : 'Not Satisfied'})
          </div>
          <p style="font-size: 0.85rem; margin-top: 0.25rem;">"${escapeHtml(feedback.comments || 'No comments left.')}"</p>
        </div>
      `;
    } else if (currentUser && currentUser.role === 'citizen' && String(currentUser.id) === String(issue.user_id)) {
      feedbackHtml = `
        <div style="background: var(--bg-card-subtle); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 1.25rem; margin-top: 1.5rem;">
          <h4 style="font-size: 1rem; margin-bottom: 0.5rem;"><i class="fa-solid fa-comment-dots" style="color: var(--primary);"></i> Rate Official Municipal Resolution</h4>
          <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 0.75rem;">Are you satisfied with the photographic proof and work completed?</p>
          <div style="display: flex; gap: 0.75rem; flex-wrap: wrap;">
            <button class="btn btn-emerald btn-sm" onclick="submitFeedbackPrompt(${issue.id}, true)">
              <i class="fa-solid fa-thumbs-up"></i> Satisfied (Award +20 Pts)
            </button>
            <button class="btn btn-outline btn-sm" style="color: var(--rose); border-color: var(--rose);" onclick="submitFeedbackPrompt(${issue.id}, false)">
              <i class="fa-solid fa-rotate-left"></i> Not Satisfied (Re-open Complaint)
            </button>
          </div>
        </div>
      `;
    }
  }

  container.innerHTML = `
    <!-- Stepper -->
    <div class="stepper">
      <div class="step-item ${step1Class}">
        <div class="step-circle"><i class="fa-solid fa-file-lines"></i></div>
        <div class="step-title">Submitted</div>
      </div>
      <div class="step-item ${step2Class}">
        <div class="step-circle"><i class="fa-solid fa-user-gear"></i></div>
        <div class="step-title">Triage</div>
      </div>
      <div class="step-item ${step3Class}">
        <div class="step-circle"><i class="fa-solid fa-person-digging"></i></div>
        <div class="step-title">In Progress</div>
      </div>
      <div class="step-item ${step4Class}">
        <div class="step-circle"><i class="fa-solid fa-circle-check"></i></div>
        <div class="step-title">Resolved</div>
      </div>
    </div>

    <!-- Description & Meta -->
    <div style="background: var(--bg-card-subtle); padding: 1rem 1.25rem; border-radius: var(--radius-md); margin-bottom: 1.5rem;">
      <div style="display: flex; justify-content: space-between; margin-bottom: 0.5rem; font-size: 0.85rem;">
        <span><strong>Category:</strong> ${escapeHtml(issue.category_name)} (${escapeHtml(issue.department_name || 'Municipal Works')})</span>
        <span><strong>SLA:</strong> ${issue.sla_hours || 48} Hours</span>
      </div>
      <p style="font-size: 0.95rem; color: var(--text-main);">${escapeHtml(issue.description)}</p>
      <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 0.5rem;">
        <i class="fa-solid fa-location-pin" style="color: var(--rose);"></i> <strong>Location:</strong> ${escapeHtml(issue.landmark || '')} ${escapeHtml(issue.address || '')} (GPS: ${Number(issue.latitude).toFixed(4)}, ${Number(issue.longitude).toFixed(4)})
      </div>
    </div>

    <!-- Photographic Proof Comparison -->
    <div class="comparison-grid">
      <div class="comparison-box">
        <div class="comparison-label" style="color: var(--rose);"><i class="fa-solid fa-camera"></i> Before Photo (Reported)</div>
        <img src="${beforePhoto}" class="comparison-img" alt="Before Proof" onerror="this.src='${assetUrl('uploads/sample_garbage_before.jpg')}'">
      </div>

      <div class="comparison-box">
        <div class="comparison-label" style="color: var(--emerald);"><i class="fa-solid fa-circle-check"></i> After Photo (Official Proof)</div>
        ${
          afterPhoto
            ? `<img src="${afterPhoto}" class="comparison-img" alt="After Proof" onerror="this.src='${assetUrl('uploads/sample_pothole_after.jpg')}'">`
            : `<div style="height: 180px; display: flex; flex-direction: column; align-items: center; justify-content: center; background: var(--bg-main); border-radius: var(--radius-sm); color: var(--text-muted); text-align: center; padding: 1rem;">
                <i class="fa-solid fa-hourglass-half" style="font-size: 1.75rem; margin-bottom: 0.5rem; color: var(--amber);"></i>
                <span style="font-size: 0.85rem;">Resolution in progress. Officer will upload verified after-photo upon completion.</span>
               </div>`
        }
      </div>
    </div>

    ${
      issue.resolution_remarks
        ? `
      <div style="background: var(--emerald-light); border-left: 4px solid var(--emerald); padding: 1rem; border-radius: var(--radius-sm); margin-bottom: 1.5rem;">
        <div style="font-weight: 700; font-size: 0.85rem; color: var(--emerald-hover);"><i class="fa-solid fa-clipboard-check"></i> Municipal Resolution Report</div>
        <div style="font-size: 0.9rem; margin-top: 0.25rem;"><strong>Action Taken:</strong> ${escapeHtml(issue.action_taken || 'Site rectified')}</div>
        <div style="font-size: 0.9rem; margin-top: 0.25rem;"><strong>Remarks:</strong> ${escapeHtml(issue.resolution_remarks)}</div>
        <div style="font-size: 0.75rem; color: var(--text-muted); margin-top: 0.4rem;">Resolved at: ${formatDate(issue.resolved_at)} by ${escapeHtml(issue.resolved_by_admin || 'Ward Officer')}</div>
      </div>
    `
        : ''
    }

    <!-- Timeline Activity Logs -->
    <div style="margin-top: 1.5rem;">
      <h4 style="font-size: 1rem; margin-bottom: 0.75rem;"><i class="fa-solid fa-timeline"></i> Action & Audit Timeline</h4>
      <div style="display: flex; flex-direction: column; gap: 0.25rem;">
        ${timelineHtml}
      </div>
    </div>

    ${feedbackHtml}
  `;
}

async function submitFeedbackPrompt(issueId, isSatisfied) {
  let comments = '';
  if (isSatisfied) {
    comments = prompt('Thank you! Please enter any feedback or appreciation for the municipal staff (Optional):', 'Issue resolved promptly.');
  } else {
    comments = prompt('Please explain why the resolution was unsatisfactory so we can escalate to supervisors:', 'Area still not fully cleared / issue persists.');
    if (!comments) return;
  }

  try {
    const data = await requestApi(`/issues/${issueId}/feedback`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${authToken}`
      },
      body: JSON.stringify({
        rating: isSatisfied ? 5 : 1,
        is_satisfied: isSatisfied,
        comments: comments || ''
      })
    });
    if (data.success) {
      showToast(data.message, 'success');
      closeModal('modal-track');
      checkAuthSession();
      loadIssues();
      return;
    }
  } catch (err) {
    showToast(err.message || 'Could not submit feedback. Please try again.', 'error');
  }
}

// -------------------------------------------------------------
// 9. LIVE GIS CITY MAP (LEAFLET)
// -------------------------------------------------------------

function initMainMap() {
  const container = document.getElementById('leaflet-map');
  if (!container) return;

  const defaultCenter = [28.6150, 77.2150];

  if (!mainMap) {
    mainMap = L.map('leaflet-map').setView(defaultCenter, 13);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(mainMap);

    updateMapMarkers(allIssues.length > 0 ? allIssues : getStoredIssues());
  } else {
    mainMap.invalidateSize();
  }
}

function updateMapMarkers(issues) {
  if (!mainMap) return;

  mapMarkers.forEach((m) => mainMap.removeLayer(m));
  mapMarkers = [];

  issues.forEach((issue) => {
    let pinColor = '#ef4444';
    if (issue.status === 'In Progress') pinColor = '#2563eb';
    else if (issue.status === 'Resolved') pinColor = '#10b981';

    const customIcon = L.divIcon({
      className: 'custom-map-pin',
      html: `<div style="background-color: ${pinColor}; width: 28px; height: 28px; border-radius: 50%; border: 3px solid #fff; box-shadow: 0 4px 10px rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 12px;"><i class="fa-solid ${issue.category_icon || 'fa-triangle-exclamation'}"></i></div>`,
      iconSize: [28, 28],
      iconAnchor: [14, 14]
    });

    const marker = L.marker([issue.latitude, issue.longitude], { icon: customIcon }).addTo(mainMap);

    const popupHtml = `
      <div style="font-family: var(--font-body); width: 220px;">
        <div style="font-weight: 700; font-size: 13px; margin-bottom: 4px;">${escapeHtml(issue.title)}</div>
        <div style="font-size: 11px; color: #64748b; margin-bottom: 6px;"><i class="fa-solid fa-tag"></i> ${escapeHtml(issue.category_name)}</div>
        <div style="font-size: 11px; margin-bottom: 8px;"><strong>Status:</strong> <span style="color: ${pinColor}; font-weight: 700;">${issue.status}</span></div>
        <button class="btn btn-primary btn-sm" style="width: 100%; font-size: 11px; padding: 4px 8px;" onclick="openIssueDetails(${issue.id})">
          View & Track Issue
        </button>
      </div>
    `;

    marker.bindPopup(popupHtml);
    mapMarkers.push(marker);
  });
}

// -------------------------------------------------------------
// 10. CITIZEN "MY REPORTS" DASHBOARD
// -------------------------------------------------------------

async function loadMyReports() {
  if (!currentUser) {
    showToast('Please login to view your submitted reports.', 'info');
    openAuthModal('login');
    return;
  }

  try {
    const data = await requestApi('/issues/my-reports', {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    if (data.success && data.issues) {
      renderIssues(data.issues, 'my-reports-container');
      return;
    }
  } catch (err) {
    const container = document.getElementById('my-reports-container');
    if (container) container.innerHTML = '<p class="notif-empty">Your reports could not be loaded.</p>';
    showToast(err.message || 'Could not load your reports.', 'error');
  }
}

// -------------------------------------------------------------
// 11. CIVIC GAMIFICATION & LEADERBOARD
// -------------------------------------------------------------

async function loadLeaderboard() {
  try {
    const data = await requestApi('/leaderboard');
    if (data.success && data.leaderboard) {
      renderLeaderboardTable(data.leaderboard);
      return;
    }
  } catch (err) {}

  // Static leaderboard
  const defaultLeaderboard = [
    { rank: 1, name: 'Demo Citizen One', ward: 'Ward 12 - Central', civic_points: 320, resolved_count: 3, badge: '👑 Grand Civic Champion' },
    { rank: 2, name: 'Demo Citizen Two', ward: 'Ward 7 - North', civic_points: 210, resolved_count: 2, badge: '🥈 Eco Guardian Master' },
    { rank: 3, name: 'Demo Citizen Three', ward: 'Ward 4 - East', civic_points: 150, resolved_count: 1, badge: '🥉 Neighborhood Sentinel' },
    { rank: 4, name: 'Kavita Singh', ward: 'Ward 9 - West', civic_points: 110, resolved_count: 1, badge: '⭐ Star Contributor' },
    { rank: 5, name: 'Amit Joshi', ward: 'Ward 2 - South', civic_points: 90, resolved_count: 0, badge: 'Civic Volunteer' }
  ];
  renderLeaderboardTable(defaultLeaderboard);
}

function renderLeaderboardTable(leaderboard) {
  const tbody = document.getElementById('leaderboard-tbody');
  if (!tbody) return;

  tbody.innerHTML = leaderboard
    .map((citizen) => {
      let rankBadge = `#${citizen.rank}`;
      if (citizen.rank === 1) rankBadge = '🥇 #1';
      else if (citizen.rank === 2) rankBadge = '🥈 #2';
      else if (citizen.rank === 3) rankBadge = '🥉 #3';

      return `
      <tr>
        <td><strong>${rankBadge}</strong></td>
        <td>
          <div style="font-weight: 700;">${escapeHtml(citizen.name)}</div>
          <div style="font-size: 0.75rem; color: var(--text-muted);">${escapeHtml(citizen.ward || 'Central')}</div>
        </td>
        <td><span class="civic-points-pill"><i class="fa-solid fa-star"></i> ${citizen.civic_points} Pts</span></td>
        <td><span style="font-weight: 600; color: var(--emerald);">${citizen.resolved_count || 0} Resolved</span></td>
        <td><span style="font-size: 0.85rem; font-weight: 600; color: var(--primary);">${citizen.badge}</span></td>
      </tr>
    `;
    })
    .join('');
}

// -------------------------------------------------------------
// 12. MUNICIPAL ADMIN COMMAND PORTAL & CHARTS
// -------------------------------------------------------------

async function loadAdminDashboard() {
  if (!currentUser || currentUser.role !== 'admin') {
    showToast('Municipal Administrator login required.', 'warning');
    openAuthModal('login');
    return;
  }

  try {
    const data = await requestApi('/admin/dashboard', {
      headers: { Authorization: `Bearer ${authToken}` }
    });

    if (data.success && data.stats) {
      document.getElementById('admin-kpi-pending').textContent = data.stats.pending;
      document.getElementById('admin-kpi-in-progress').textContent = data.stats.in_progress;
      document.getElementById('admin-kpi-resolved').textContent = data.stats.resolved;
      document.getElementById('admin-kpi-rate').textContent = `${data.stats.resolution_rate}%`;
    }
  } catch (err) {
    ['admin-kpi-pending', 'admin-kpi-in-progress', 'admin-kpi-resolved', 'admin-kpi-rate']
      .forEach((id) => {
        const element = document.getElementById(id);
        if (element) element.textContent = '—';
      });
    showToast(err.message || 'Could not load admin dashboard.', 'error');
  }

  loadAdminHotspots();
  loadAdminTriageTable();
  renderAdminCharts();
}

async function loadAdminHotspots() {
  const container = document.getElementById('admin-hotspots-container');
  if (!container) return;

  try {
    const data = await requestApi('/admin/hotspots', {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    if (data.success && data.hotspots) {
      renderHotspotsUI(data.hotspots);
      return;
    }
  } catch (err) {
    container.innerHTML = '<div class="notif-empty">Hotspots could not be loaded.</div>';
    showToast(err.message || 'Could not load hotspots.', 'error');
  }
}

function renderHotspotsUI(hotspots) {
  const container = document.getElementById('admin-hotspots-container');
  if (!container) return;

  if (hotspots.length === 0) {
    container.innerHTML = '<div style="color: var(--text-muted); font-size: 0.9rem;">No active high-density problem hotspots detected.</div>';
    return;
  }

  container.innerHTML = hotspots
    .map(
      (h) => `
    <div style="background: var(--bg-card-subtle); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 1rem;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.4rem;">
        <strong style="font-size: 0.95rem;">${escapeHtml(h.ward)}</strong>
        <span style="background: #fee2e2; color: #991b1b; font-size: 0.75rem; font-weight: 700; padding: 0.2rem 0.5rem; border-radius: 4px;">
          ${h.issue_count} Active Complaints
        </span>
      </div>
      <div style="font-size: 0.85rem; color: var(--text-muted);"><i class="fa-solid ${h.category_icon}"></i> ${escapeHtml(h.category_name)}</div>
      <div style="font-size: 0.75rem; font-family: var(--font-mono); color: var(--text-light); margin-top: 0.5rem;">
        GPS Cluster: ${Number(h.center_lat).toFixed(4)}, ${Number(h.center_lng).toFixed(4)}
      </div>
    </div>
  `
    )
    .join('');
}

async function loadAdminTriageTable() {
  const tbody = document.getElementById('admin-issues-tbody');
  if (!tbody) return;

  let issues;
  try {
    const data = await requestApi('/issues', {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    issues = data.issues;
  } catch (err) {
    tbody.innerHTML = '<tr><td colspan="7">Reports could not be loaded.</td></tr>';
    showToast(err.message || 'Could not load reports.', 'error');
    return;
  }

  if (issues.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7">No reports have been submitted yet.</td></tr>';
    return;
  }

  tbody.innerHTML = issues
    .map((issue) => {
      let statusBadge = `<span class="pill-btn active" style="font-size: 0.75rem; padding: 0.2rem 0.6rem;">${issue.status}</span>`;

      let actionBtn = '';
      if (issue.status === 'Pending') {
        actionBtn = `
          <button class="btn btn-primary btn-sm" onclick="handleAdminStatusChange(${issue.id}, 'In Progress')">
            <i class="fa-solid fa-play"></i> Start Work
          </button>
        `;
      } else if (issue.status === 'In Progress') {
        actionBtn = `
          <button class="btn btn-emerald btn-sm" onclick="openResolveModal(${issue.id})">
            <i class="fa-solid fa-camera-rotate"></i> Upload Proof & Resolve
          </button>
        `;
      } else {
        actionBtn = `
          <button class="btn btn-outline btn-sm" onclick="openIssueDetails(${issue.id})">
            <i class="fa-solid fa-check"></i> View Verified Proof
          </button>
        `;
      }

      return `
      <tr>
        <td><span class="issue-tracking-tag">${issue.tracking_id}</span></td>
        <td>
          <div style="font-weight: 700; font-size: 0.9rem;">${escapeHtml(issue.title)}</div>
          <div style="font-size: 0.75rem; color: var(--text-muted);">${formatDate(issue.created_at)}</div>
        </td>
        <td><span style="font-size: 0.85rem;"><i class="fa-solid ${issue.category_icon || 'fa-triangle-exclamation'}"></i> ${escapeHtml(issue.category_name)}</span></td>
        <td><span style="font-size: 0.85rem;">${escapeHtml(issue.landmark || issue.ward || 'Central')}</span></td>
        <td><span style="font-weight: 700; font-size: 0.8rem; color: ${issue.priority === 'Critical' ? 'var(--rose)' : 'inherit'};">${issue.priority}</span></td>
        <td>${statusBadge}</td>
        <td>${actionBtn}</td>
      </tr>
    `;
    })
    .join('');
}

async function handleAdminStatusChange(issueId, newStatus) {
  const remarks = prompt(`Enter work dispatch remarks for status '${newStatus}':`, 'Field maintenance team dispatched to site.');
  if (remarks === null) return;

  try {
    const data = await requestApi(`/admin/issues/${issueId}/status`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${authToken}`
      },
      body: JSON.stringify({ status: newStatus, remarks: remarks || 'Work initiated.' })
    });

    if (data.success) {
      showToast(data.message, 'success');
      loadAdminDashboard();
      loadIssues();
      return;
    }
  } catch (err) {
    showToast(err.message || 'Could not update report status.', 'error');
  }
}

function openResolveModal(issueId) {
  document.getElementById('resolve-report-id').value = issueId;
  openModal('modal-resolve');
}

let currentResolvePhotoDataUrl = null;
function previewResolvePhoto(input) {
  const previewBox = document.getElementById('resolve-preview-box');
  const previewImg = document.getElementById('resolve-preview-img');

  if (input.files && input.files[0]) {
    if (!validatePhotoFile(input.files[0])) {
      input.value = '';
      currentResolvePhotoDataUrl = null;
      if (previewBox) previewBox.style.display = 'none';
      if (previewImg) previewImg.removeAttribute('src');
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => {
      currentResolvePhotoDataUrl = e.target.result;
      if (previewImg) previewImg.src = e.target.result;
      if (previewBox) previewBox.style.display = 'block';
    };
    reader.readAsDataURL(input.files[0]);
  }
}

async function handleResolveSubmit(e) {
  e.preventDefault();
  const reportId = document.getElementById('resolve-report-id').value;
  const actionTaken = document.getElementById('resolve-action').value;
  const remarks = document.getElementById('resolve-remarks').value;
  const photoInput = document.getElementById('resolve-photo-input');
  if (!photoInput.files || !photoInput.files[0]) {
    showToast('Upload an official after-photo before resolving this report.', 'warning');
    photoInput.click();
    return;
  }
  if (!validatePhotoFile(photoInput.files[0])) {
    return;
  }

  const formData = new FormData();
  formData.append('action_taken', actionTaken);
  formData.append('remarks', remarks);

  formData.append('after_photo', photoInput.files[0]);

  try {
    const data = await requestApi(`/admin/issues/${reportId}/resolve`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${authToken}` },
      body: formData
    });

    if (data.success) {
      closeModal('modal-resolve');
      document.getElementById('admin-resolve-form').reset();
      document.getElementById('resolve-preview-box').style.display = 'none';

      showToast(data.message, 'success');
      loadAdminDashboard();
      loadIssues();
      return;
    }
  } catch (err) {
    showToast(err.message || 'Could not resolve this report. Please try again.', 'error');
  }
}

async function renderAdminCharts() {
  let analytics;
  try {
    analytics = await requestApi('/admin/analytics', {
      headers: { Authorization: `Bearer ${authToken}` }
    });
  } catch (err) {
    showToast(err.message || 'Could not load dashboard analytics.', 'error');
    return;
  }
  const categoryRows = analytics.category_wise || [];
  const catLabels = categoryRows.map((row) => row[0]);
  const catData = categoryRows.map((row) => Number(row[1]));
  const statusMap = { Pending: 0, 'In Progress': 0, Resolved: 0 };
  (analytics.status_wise || []).forEach((row) => {
    if (Object.prototype.hasOwnProperty.call(statusMap, row[0])) {
      statusMap[row[0]] = Number(row[1]);
    }
  });

  const ctxCat = document.getElementById('categoryChart')?.getContext('2d');
  if (ctxCat) {
    if (categoryChartInstance) categoryChartInstance.destroy();
    categoryChartInstance = new Chart(ctxCat, {
      type: 'doughnut',
      data: {
        labels: catLabels,
        datasets: [
          {
            data: catData,
            backgroundColor: ['#2563eb', '#10b981', '#f59e0b', '#06b6d4', '#6366f1', '#ec4899', '#8b5cf6'],
            borderWidth: 2
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'right', labels: { boxWidth: 12, font: { family: 'Plus Jakarta Sans' } } }
        }
      }
    });
  }

  const ctxStatus = document.getElementById('statusChart')?.getContext('2d');
  if (ctxStatus) {
    if (statusChartInstance) statusChartInstance.destroy();
    statusChartInstance = new Chart(ctxStatus, {
      type: 'bar',
      data: {
        labels: Object.keys(statusMap),
        datasets: [
          {
            label: 'Complaints',
            data: Object.values(statusMap),
            backgroundColor: ['#f59e0b', '#2563eb', '#10b981'],
            borderRadius: 6
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          y: { beginAtZero: true, ticks: { stepSize: 1 } }
        }
      }
    });
  }
}

// -------------------------------------------------------------
// 13. NOTIFICATIONS SYSTEM
// -------------------------------------------------------------

async function loadNotifications() {
  const badge = document.getElementById('notif-badge-count');
  const list = document.getElementById('notif-list-container');
  if (!list) return;
  if (!authToken || !currentUser) {
    if (badge) badge.style.display = 'none';
    list.innerHTML = '<div class="notif-empty">Log in to see your notifications.</div>';
    return;
  }

  try {
    const data = await requestApi('/notifications', {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    const notifications = data.notifications || [];
    if (badge) {
      badge.textContent = data.unread_count || 0;
      badge.style.display = data.unread_count > 0 ? 'flex' : 'none';
    }
    list.innerHTML = notifications.length
      ? notifications.map((n) => `
    <div class="notif-item ${n.is_read ? '' : 'unread'}" onclick="handleNotifClick(${n.id}, ${n.report_id || 'null'})">
      <div style="font-size: 1.1rem; color: var(--primary);">
        <i class="fa-solid fa-circle-info"></i>
      </div>
      <div style="flex: 1;">
        <div style="font-weight: 700; font-size: 0.85rem;">${escapeHtml(n.title)}</div>
        <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.2rem;">${escapeHtml(n.message)}</div>
        <div style="font-size: 0.7rem; color: var(--text-light); margin-top: 0.3rem;">${formatDate(n.created_at)}</div>
      </div>
    </div>
  `
      ).join('')
      : '<div class="notif-empty">You are all caught up. No notifications yet.</div>';
  } catch (err) {
    list.innerHTML = '<div class="notif-empty">Notifications could not be loaded.</div>';
    showToast(err.message || 'Could not load notifications.', 'error');
  }
}

function toggleNotifDrawer() {
  const drawer = document.getElementById('notif-drawer');
  if (drawer) {
    drawer.classList.toggle('open');
  }
}

async function handleNotifClick(notifId, reportId) {
  try {
    await requestApi(`/notifications/${notifId}/read`, {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${authToken}` }
    });
    await loadNotifications();
  } catch (err) {
    showToast(err.message || 'Could not update notification.', 'error');
  }
  if (reportId) {
    openIssueDetails(reportId);
  }
}

async function markAllNotificationsRead() {
  try {
    await requestApi('/notifications/read-all', {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${authToken}` }
    });
    await loadNotifications();
    showToast('All notifications marked as read.', 'info');
  } catch (err) {
    showToast(err.message || 'Could not update notifications.', 'error');
  }
}

// -------------------------------------------------------------
// 14. SDLC DOCUMENTATION VIEWER MODAL
// -------------------------------------------------------------

const SDLC_DOCS_CONTENT = {
  1: {
    title: 'Phase 1: Requirement Analysis & Software Requirement Specification (SRS)',
    content: `
      <h4>1.1 Purpose & Problem Statement</h4>
      <p>Indian municipal corporations face immense operational overhead due to informal and untracked complaints made through phone calls or social media. Citizens lack transparency regarding resolution progress and verification proof.</p>
      
      <h4>1.2 Scope of the System</h4>
      <p>The <strong>Safe & Smart City Platform</strong> automates civic complaint management using HTML5 Geolocation, multipart photo verification, unique tracking IDs (<code>SSC-YYYY-XXXX</code>), municipal triage, and GIS hotspot analytics.</p>
      
      <h4>1.3 Functional Requirements</h4>
      <ul>
        <li><strong>FR-1: Geolocation Auto-Detection:</strong> Auto-capture latitude & longitude via HTML5 Geolocation API.</li>
        <li><strong>FR-2: Photographic Evidence:</strong> Mandatory Before-Photo and Resolution After-Photo proof.</li>
        <li><strong>FR-3: Public Tracking ID:</strong> Unique tracking code generated for live stepper tracking without mandatory login.</li>
        <li><strong>FR-4: Upvoting & Hotspot Flagging:</strong> Upvoting prevents duplicate reporting and elevates issues to High Priority.</li>
        <li><strong>FR-5: Gamification:</strong> Civic Karma points awarded for reporting (+50), upvoting (+10), and feedback (+20).</li>
      </ul>

      <h4>1.4 Non-Functional Requirements</h4>
      <ul>
        <li><strong>Security:</strong> BCrypt password hashing and signed, expiring bearer-token authorization.</li>
        <li><strong>Performance:</strong> REST API latency < 150ms; responsive across mobile and desktop.</li>
        <li><strong>Reliability:</strong> MySQL relational database with JDBC and foreign-key integrity.</li>
      </ul>
    `
  },
  2: {
    title: 'Phase 2: System Architecture & UML Design',
    content: `
      <h4>2.1 High-Level Tier Architecture</h4>
      <p>The system follows a modular 3-Tier Architecture:</p>
      <ul>
        <li><strong>Presentation Tier:</strong> HTML5, CSS3 Glassmorphic Design System, Vanilla JavaScript ES6+, Leaflet GIS, Chart.js.</li>
        <li><strong>Application Tier:</strong> Java 17, Jakarta Servlets, JDBC DAOs, multipart photo handling, signed bearer-token authentication, and GIS hotspot queries.</li>
        <li><strong>Data Tier:</strong> MySQL 8 relational database with ACID transactions.</li>
      </ul>

      <h4>2.2 UML Use Case Analysis</h4>
      <p><strong>Actors:</strong> Citizen User, Municipal Admin/Ward Officer, Guest Public.</p>
      <ul>
        <li><strong>Citizen:</strong> Register, Login, Report Civic Issue (GPS + Photo), Track Complaint, Upvote Issue, Rate Feedback.</li>
        <li><strong>Admin:</strong> Authenticate, View GIS Hotspots, Triage Complaints, Dispatch Field Staff, Upload After-Photo Proof, Mark Resolved.</li>
      </ul>

      <h4>2.3 Data Flow Diagrams (DFD)</h4>
      <ul>
        <li><strong>DFD Level 0 (Context Diagram):</strong> Citizen/Admin &harr; Smart City Core System &harr; Municipal Database.</li>
        <li><strong>DFD Level 1:</strong> Processes 1.0 (Auth) &rarr; 2.0 (Issue Ingestion) &rarr; 3.0 (GIS Clustering) &rarr; 4.0 (Resolution Proof Verification) &rarr; 5.0 (Gamification).</li>
      </ul>
    `
  },
  3: {
    title: 'Phase 3: Database Design, Schema & Data Dictionary',
    content: `
      <h4>3.1 Relational Schema (Normalized to 3NF)</h4>
      <ul>
        <li><code>users</code> (id, name, email, password, phone, role, civic_points, ward, created_at)</li>
        <li><code>categories</code> (id, name, code, icon, department, sla_hours, description)</li>
        <li><code>reports</code> (id, tracking_id, user_id, category_id, title, description, landmark, address, ward, latitude, longitude, before_photo, priority, status, upvotes, created_at, updated_at)</li>
        <li><code>resolutions</code> (id, report_id, admin_id, after_photo, remarks, action_taken, resolved_at)</li>
        <li><code>upvotes</code> (id, report_id, user_id, created_at)</li>
        <li><code>feedback</code> (id, report_id, user_id, rating, is_satisfied, comments, created_at)</li>
        <li><code>notifications</code> (id, user_id, report_id, title, message, type, is_read, created_at)</li>
        <li><code>activity_logs</code> (id, report_id, actor_name, action, details, created_at)</li>
      </ul>

      <h4>3.2 Integrity Constraints</h4>
      <p>Foreign Keys with <code>ON DELETE CASCADE</code> for upvotes and activity logs. Unique constraint on <code>(report_id, user_id)</code> prevents duplicate upvoting.</p>
    `
  },
  4: {
    title: 'Phase 4: Implementation & Algorithms',
    content: `
      <h4>4.1 Core Algorithms</h4>
      <p><strong>1. Tracking ID Generator:</strong> <code>SSC-YYYY-RANDOM(1000-9999)</code> ensures zero collision and human-readable IDs.</p>
      <p><strong>2. GIS Hotspot Clustering:</strong> Groups complaints within the same ward and coordinates having &ge; 2 active reports to flag high-density zones for immediate municipal dispatch.</p>
      <p><strong>3. Civic Points Gamification:</strong> Automatically computes civic score increments (+50 for report, +10 for upvote, +25 for resolution, +20 for feedback).</p>

      <h4>4.2 REST API Specification</h4>
      <ul>
        <li><code>POST /api/auth/register</code> - Register Citizen</li>
        <li><code>POST /api/auth/login</code> - Authenticate Citizen & Admin</li>
        <li><code>GET /api/issues</code> - Community issues with multi-parameter filter & search</li>
        <li><code>POST /api/issues</code> - Multipart file upload with GPS coordinates</li>
        <li><code>GET /api/track/:trackingId</code> - Public stepper & before/after photo comparison</li>
        <li><code>POST /api/admin/issues/:id/resolve</code> - Upload after-photo and trigger citizen push alert</li>
      </ul>
    `
  },
  5: {
    title: 'Phase 5: Software Testing & QA Verification',
    content: `
      <h4>5.1 Testing Strategy</h4>
      <p>The Java 17 Maven build, JavaScript diagnostics, servlet configuration, and required WAR contents have been verified. Database and browser end-to-end cases require a running MySQL and Tomcat deployment and have not been run in this environment.</p>

      <h4>5.2 Test Cases Matrix</h4>
      <table class="custom-table" style="font-size: 0.85rem;">
        <thead>
          <tr>
            <th>Test ID</th>
            <th>Scenario</th>
            <th>Input</th>
            <th>Expected Output</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>TC-01</td>
            <td>Citizen Registration</td>
            <td>Valid credentials</td>
            <td>User created, 50 points awarded, bearer token issued</td>
            <td><span style="color: var(--text-muted); font-weight: 700;">NOT RUN</span></td>
          </tr>
          <tr>
            <td>TC-02</td>
            <td>Geolocation Tagging</td>
            <td>GPS Auto-Detect</td>
            <td>Accurate Lat/Lng captured</td>
            <td><span style="color: var(--text-muted); font-weight: 700;">NOT RUN</span></td>
          </tr>
          <tr>
            <td>TC-03</td>
            <td>Report Submission</td>
            <td>Photo + Category + Coords</td>
            <td>Tracking ID generated (SSC-2026-XXXX)</td>
            <td><span style="color: var(--text-muted); font-weight: 700;">NOT RUN</span></td>
          </tr>
          <tr>
            <td>TC-04</td>
            <td>Admin Resolution Proof</td>
            <td>After-Photo upload</td>
            <td>Status &rarr; Resolved, Citizen notified</td>
            <td><span style="color: var(--text-muted); font-weight: 700;">NOT RUN</span></td>
          </tr>
          <tr>
            <td>TC-05</td>
            <td>Unsatisfactory Feedback</td>
            <td>is_satisfied = false</td>
            <td>Issue automatically reopened to In Progress</td>
            <td><span style="color: var(--text-muted); font-weight: 700;">NOT RUN</span></td>
          </tr>
        </tbody>
      </table>
    `
  },
  6: {
    title: 'Phase 6: Deployment, User Manual & Viva Voce Guide',
    content: `
      <h4>6.1 Installation & Execution Guide</h4>
      <pre style="background: var(--bg-card-subtle); padding: 1rem; border-radius: 8px; font-family: var(--font-mono); font-size: 0.85rem;">
# 1. Create the MySQL database
CREATE DATABASE smart_city_db;

# 2. Build the Java web application
mvn -f .\\SmartCityJava\\pom.xml clean package

# 3. Copy SmartCityJava\\target\\SmartCity.war to Tomcat\\webapps
# 4. Start Tomcat and open:
http://localhost:8080/SmartCity/

      </pre>

      <h4>6.2 Sample Academic Demo Credentials</h4>
      <ul>
        <li><strong>Citizen Account:</strong> <code>citizen.one@example.test</code> | Password: <code>citizen123</code></li>
        <li><strong>Municipal Admin Account:</strong> <code>admin@example.test</code> | Password: <code>admin123</code></li>
      </ul>

      <h4>6.3 Viva Voce Frequently Asked Questions (Q&A)</h4>
      <p><strong>Q1: Why use HTML5 Geolocation instead of manual address typing?</strong><br>
      <em>Ans:</em> Manual typing often leads to ambiguous landmarks. GPS coordinates provide exact GIS pins for municipal field workers and GIS hotspot clustering.</p>
      
      <p><strong>Q2: How does the system prevent duplicate complaints?</strong><br>
      <em>Ans:</em> Citizens can view live issues on the map/feed and upvote existing complaints rather than submitting duplicates. When upvotes exceed threshold (&ge; 10), priority is automatically escalated.</p>
      
      <p><strong>Q3: How is resolution accountability enforced?</strong><br>
      <em>Ans:</em> Authorities cannot mark an issue resolved without uploading a mandatory <strong>After-Photo</strong> proof. If citizens find the work incomplete, they can mark "Not Satisfied" which automatically reopens the issue.</p>
    `
  },
  7: {
    title: 'Consolidated B.Sc. Computer Science Project Report Summary',
    content: `
      <div style="text-align: center; margin-bottom: 1.5rem; border-bottom: 2px solid var(--border-color); padding-bottom: 1rem;">
        <h3 style="font-size: 1.4rem;">SAFE & SMART CITY - CIVIC ISSUE REPORTING SYSTEM</h3>
        <p style="color: var(--text-muted); font-size: 0.9rem;">A Full-Stack Web Mini Project for B.Sc. Computer Science Curriculum</p>
      </div>

      <h4>Abstract</h4>
      <p>The Safe & Smart City Civic Issue Reporting System bridges the gap between urban citizens and municipal governance. The application enables citizens to report garbage accumulation, road craters, dark streetlights, and water leakages with automatic GPS geolocation and photographic evidence. The system features unique tracking IDs, live status steppers, GIS hotspot mapping, resolution proof verification, and civic gamification.</p>

      <h4>Complete Project Documentation Files in Workspace:</h4>
      <ul>
        <li><code>docs/SDLC_PHASE_1_REQUIREMENT_ANALYSIS.md</code></li>
        <li><code>docs/SDLC_PHASE_2_SYSTEM_DESIGN.md</code></li>
        <li><code>docs/SDLC_PHASE_3_DATABASE_DESIGN.md</code></li>
        <li><code>docs/SDLC_PHASE_4_IMPLEMENTATION.md</code></li>
        <li><code>docs/SDLC_PHASE_5_TESTING_AND_QA.md</code></li>
        <li><code>docs/SDLC_PHASE_6_DEPLOYMENT_AND_USER_GUIDE.md</code></li>
        <li><code>PROJECT_REPORT.md</code></li>
        <li><code>database.sql</code> (Complete MySQL DDL & DML Schema)</li>
      </ul>
    `
  }
};

function openDocModal(phaseNumber) {
  const doc = SDLC_DOCS_CONTENT[phaseNumber];
  if (!doc) return;

  document.getElementById('doc-modal-title').innerHTML = `<i class="fa-solid fa-book-bookmark" style="color: var(--primary);"></i> ${doc.title}`;
  document.getElementById('doc-modal-body').innerHTML = doc.content;
  openModal('modal-doc-viewer');
}

// -------------------------------------------------------------
// 15. UI UTILITIES & MODAL HELPERS
// -------------------------------------------------------------

function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.add('open');
    document.body.style.overflow = 'hidden';
    if (modalId === 'modal-track' && currentTrackingId && currentTrackingIsLive) {
      startTrackingRefresh();
    }
  }
}

function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.remove('open');
    document.body.style.overflow = '';
    if (modalId === 'modal-track') {
      stopTrackingRefresh();
    }
  }
}

function startTrackingRefresh() {
  stopTrackingRefresh();
  trackingRefreshErrorShown = false;
  trackingRefreshTimer = setInterval(refreshTrackedIssue, 10000);
}

function stopTrackingRefresh() {
  if (trackingRefreshTimer) {
    clearInterval(trackingRefreshTimer);
    trackingRefreshTimer = null;
  }
}

async function refreshTrackedIssue() {
  if (trackingRefreshInFlight || !currentTrackingId) return;
  const trackingId = currentTrackingId;
  trackingRefreshInFlight = true;
  try {
    const data = await requestApi(`/track/${encodeURIComponent(trackingId)}`);
    if (data.issue && currentTrackingId === trackingId
      && document.getElementById('modal-track')?.classList.contains('open')) {
      if (JSON.stringify(data.issue) !== currentTrackingSnapshot) {
        renderTrackDetails(data.issue);
      }
      trackingRefreshErrorShown = false;
    }
  } catch (err) {
    if (currentTrackingId === trackingId && !trackingRefreshErrorShown
      && document.getElementById('modal-track')?.classList.contains('open')) {
      showToast(`Live tracking update failed: ${err.message}`, 'error');
      trackingRefreshErrorShown = true;
    }
  } finally {
    trackingRefreshInFlight = false;
  }
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;

  let icon = 'fa-info-circle';
  if (type === 'success') icon = 'fa-circle-check';
  else if (type === 'error') icon = 'fa-circle-xmark';
  else if (type === 'warning') icon = 'fa-triangle-exclamation';

  toast.innerHTML = `
    <i class="fa-solid ${icon}" style="font-size: 1.25rem;"></i>
    <span style="font-size: 0.9rem; font-weight: 600;">${escapeHtml(message)}</span>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.animation = 'slideIn 0.3s ease reverse forwards';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  });
}

function escapeHtml(str) {
  if (!str) return '';
  return str
    .toString()
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
