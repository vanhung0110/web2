/**
 * HotelChain Pro — Testing Dashboard JavaScript Controller
 */

// Global State
const state = {
    accessToken: localStorage.getItem('hc_access_token'),
    refreshToken: localStorage.getItem('hc_refresh_token'),
    user: JSON.parse(localStorage.getItem('hc_user') || 'null'),
    stompClient: null,
    activeTab: 'dashboard-tab',
    selectedPropertyId: null,
    selectedBooking: null,
    properties: [],
    bookings: []
};

// Config & API base
const API_BASE = '/api/v1';

// Initialize App
document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    initAuthForm();
    initPropertySection();
    initBookingSection();
    initPaymentSection();
    initUtilitySection();
    initStaffSection();
    initReportSection();
    initLogsSection();

    // Check if logged in
    updateSessionUI();
    if (state.accessToken) {
        connectWebSocket();
        loadDashboardData();
    }
});

// Helper for UI alerts
function showAlert(message, type = 'success') {
    const banner = document.getElementById('alert-banner');
    banner.className = `alert-banner ${type}`;
    banner.innerText = message;
    banner.classList.remove('hidden');
    setTimeout(() => {
        banner.classList.add('hidden');
    }, 5000);
}

// Log to real-time logs terminal
function logToTerminal(message, type = 'info') {
    const term = document.getElementById('terminal-logs');
    if (!term) return;
    const line = document.createElement('div');
    line.className = `log-line ${type}-msg`;
    const time = new Date().toLocaleTimeString();
    line.innerText = `[${time}] ${message}`;
    term.appendChild(line);
    term.scrollTop = term.scrollHeight;
}

// Secure API call with Auto Token Refresh
async function apiCall(endpoint, options = {}) {
    let headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    if (state.accessToken) {
        headers['Authorization'] = `Bearer ${state.accessToken}`;
    }

    let url = endpoint.startsWith('http') ? endpoint : `${endpoint}`;
    let fetchOptions = {
        ...options,
        headers
    };

    try {
        let response = await fetch(url, fetchOptions);

        // Access token expired, attempt refresh
        if (response.status === 401 && state.refreshToken) {
            logToTerminal('Access token expired. Attempting token refresh...', 'warn');
            const refreshed = await performTokenRefresh();
            if (refreshed) {
                // Retry request with new token
                headers['Authorization'] = `Bearer ${state.accessToken}`;
                response = await fetch(url, fetchOptions);
            } else {
                logout();
                throw new Error('Session expired. Please log in again.');
            }
        }

        if (!response.ok) {
            let errorText = await response.text();
            let errorMessage = response.statusText;
            try {
                const errJson = JSON.parse(errorText);
                errorMessage = errJson.message || errorMessage;
            } catch(e) {}
            throw new Error(errorMessage);
        }

        const text = await response.text();
        return text ? JSON.parse(text) : null;
    } catch (error) {
        logToTerminal(`API Error: ${error.message}`, 'danger');
        throw error;
    }
}

// Perform Token Refresh Rotation
async function performTokenRefresh() {
    try {
        const res = await fetch(`${API_BASE}/auth/refresh`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken: state.refreshToken })
        });
        if (res.ok) {
            const data = await res.json();
            const payload = data.data;
            state.accessToken = payload.accessToken;
            state.refreshToken = payload.refreshToken;
            localStorage.setItem('hc_access_token', payload.accessToken);
            localStorage.setItem('hc_refresh_token', payload.refreshToken);
            logToTerminal('Token refreshed successfully.', 'success');
            return true;
        }
    } catch (err) {
        console.error('Refresh token failed:', err);
    }
    return false;
}

// Login
async function login(username, password) {
    try {
        const payload = { username, password };
        logToTerminal(`Sending login request for user: ${username}...`, 'info');
        const res = await apiCall(`${API_BASE}/auth/login`, {
            method: 'POST',
            body: JSON.stringify(payload)
        });

        const data = res.data;
        state.accessToken = data.accessToken;
        state.refreshToken = data.refreshToken;
        state.user = data.user;

        localStorage.setItem('hc_access_token', data.accessToken);
        localStorage.setItem('hc_refresh_token', data.refreshToken);
        localStorage.setItem('hc_user', JSON.stringify(data.user));

        showAlert('Đăng nhập thành công!');
        logToTerminal(`Logged in successfully as ${state.user.fullName} (${state.user.role})`, 'success');

        updateSessionUI();
        connectWebSocket();
        loadDashboardData();
        switchTab('dashboard-tab');
    } catch (err) {
        showAlert(err.message, 'danger');
    }
}

// Logout
function logout() {
    if (state.accessToken) {
        fetch(`${API_BASE}/auth/logout`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${state.accessToken}` }
        }).catch(() => {});
    }

    state.accessToken = null;
    state.refreshToken = null;
    state.user = null;
    localStorage.clear();

    if (state.stompClient) {
        state.stompClient.disconnect();
        state.stompClient = null;
    }

    updateSessionUI();
    showAlert('Đã đăng xuất khỏi hệ thống.');
    logToTerminal('Session ended.', 'info');
    switchTab('auth-tab');
}

// Update authentication UI elements
function updateSessionUI() {
    const loggedIn = !!state.accessToken;
    const nameEl = document.querySelector('#user-profile-header .user-name');
    const badgeEl = document.querySelector('#user-profile-header .user-role-badge');
    const sessionStatus = document.getElementById('session-status');
    const sessionRole = document.getElementById('session-role');
    const sessionProps = document.getElementById('session-properties');
    const tokenTextarea = document.getElementById('token-textarea');
    const refreshTokenTextarea = document.getElementById('refresh-token-textarea');

    const btnLogout = document.getElementById('btn-logout');
    const btnRefresh = document.getElementById('btn-refresh-token');
    const btn2fa = document.getElementById('btn-setup-2fa');

    if (loggedIn && state.user) {
        nameEl.innerText = state.user.fullName;
        badgeEl.innerText = state.user.role;
        badgeEl.className = 'user-role-badge active';
        sessionStatus.innerText = 'Đã đăng nhập';
        sessionStatus.className = 'info-value text-success';
        sessionRole.innerText = state.user.role;
        
        const propsStr = state.user.assignedProperties && state.user.assignedProperties.length > 0
            ? state.user.assignedProperties.map(p => `${p.name} (${p.code})`).join(', ')
            : 'Tất cả chi nhánh (SUPER_ADMIN)';
        sessionProps.innerText = propsStr;

        tokenTextarea.value = state.accessToken;
        refreshTokenTextarea.value = state.refreshToken;

        btnLogout.removeAttribute('disabled');
        btnRefresh.removeAttribute('disabled');
        btn2fa.removeAttribute('disabled');
    } else {
        nameEl.innerText = 'Khách';
        badgeEl.innerText = 'Chưa đăng nhập';
        badgeEl.className = 'user-role-badge';
        sessionStatus.innerText = 'Chưa đăng nhập';
        sessionStatus.className = 'info-value text-danger';
        sessionRole.innerText = '-';
        sessionProps.innerText = '-';
        tokenTextarea.value = '';
        refreshTokenTextarea.value = '';

        btnLogout.setAttribute('disabled', 'true');
        btnRefresh.setAttribute('disabled', 'true');
        btn2fa.setAttribute('disabled', 'true');
    }
}

// STOMP WebSocket Connection Manager
function connectWebSocket() {
    if (!state.accessToken) return;
    if (state.stompClient && state.stompClient.connected) return;

    logToTerminal('Initializing WebSocket STOMP handshake...', 'info');
    const socket = new SockJS('/ws');
    state.stompClient = Stomp.over(socket);

    // Suppress internal debugging in console
    state.stompClient.debug = (str) => {
        if (str.includes('ERROR') || str.includes('message')) {
            console.log('STOMP DEBUG:', str);
        }
    };

    const headers = {
        'Authorization': `Bearer ${state.accessToken}`
    };

    state.stompClient.connect(headers, (frame) => {
        logToTerminal('STOMP WebSocket Connection established.', 'success');
        updateWsStatusUI(true);

        // Subscribe to global notifications per user
        if (state.user && state.user.id) {
            const userDest = `/user/queue/notifications`;
            logToTerminal(`Subscribing to user-specific channel: ${userDest}`, 'info');
            state.stompClient.subscribe(userDest, (message) => {
                const body = JSON.parse(message.body);
                logToTerminal(`[User Notify] Received: ${JSON.stringify(body)}`, 'success');
                showAlert(`[Thông báo] ${body.message || JSON.stringify(body)}`, 'info');
            });
        }

        // Subscribe to property-specific channels if a property is selected
        subscribePropertyChannels();
    }, (error) => {
        logToTerminal(`WebSocket Connection Failed: ${error}`, 'danger');
        updateWsStatusUI(false);
    });
}

function subscribePropertyChannels() {
    if (!state.stompClient || !state.stompClient.connected) return;

    // Unsubscribe previous property subs if any (STOMP handles this automatically on re-sub if done cleanly)
    if (state.selectedPropertyId) {
        const eventDest = `/topic/property/${state.selectedPropertyId}/events`;
        const dashDest = `/topic/property/${state.selectedPropertyId}/dashboard`;

        logToTerminal(`Subscribing to Property Events channel: ${eventDest}`, 'info');
        state.stompClient.subscribe(eventDest, (message) => {
            const body = JSON.parse(message.body);
            logToTerminal(`[Realtime Event] ${body.type}: ${JSON.stringify(body.data)}`, 'success');
            
            // Add to system events panel in dashboard
            addEventToDashboardPanel(body);
            
            // Refresh stats dynamically
            loadDashboardData();
        });

        logToTerminal(`Subscribing to Property Dashboard channel: ${dashDest}`, 'info');
        state.stompClient.subscribe(dashDest, (message) => {
            const body = JSON.parse(message.body);
            logToTerminal(`[Dashboard Update Pushed]`, 'info');
            updateDashboardStatsUI(body);
        });
    }
}

function updateWsStatusUI(connected) {
    const el = document.getElementById('websocket-status');
    if (connected) {
        el.className = 'connection-status online';
        el.querySelector('.status-text').innerText = 'WS Online';
    } else {
        el.className = 'connection-status offline';
        el.querySelector('.status-text').innerText = 'WS Offline';
    }
}

// Add WS event to dashboard events logger panel
function addEventToDashboardPanel(event) {
    const container = document.getElementById('dash-recent-events');
    const placeholder = container.querySelector('.placeholder-text');
    if (placeholder) placeholder.remove();

    const entry = document.createElement('div');
    entry.className = 'event-entry-item';
    const time = new Date().toLocaleTimeString();
    entry.innerHTML = `<strong>[${time}] ${event.type}</strong> - ${JSON.stringify(event.data)}`;
    container.prepend(entry);
    
    // Cap log lines to 10
    if (container.children.length > 10) {
        container.removeChild(container.lastChild);
    }
}

// Update dashboard numbers
function updateDashboardStatsUI(data) {
    if (!data) return;
    
    // Revenue format
    const rev = data.today && data.today.revenue !== undefined ? data.today.revenue : 0;
    document.getElementById('dash-revenue').innerText = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(rev);
    
    // Occupancy rate
    const occ = data.occupancy && data.occupancy.occupancyRate !== undefined ? data.occupancy.occupancyRate : 0.0;
    document.getElementById('dash-occupancy').innerText = `${occ.toFixed(1)}%`;
}

// Load current dashboard overview data
async function loadDashboardData() {
    if (!state.accessToken) return;
    try {
        // Find properties list to display
        const res = await apiCall(`${API_BASE}/properties`);
        state.properties = res.data || [];
        document.getElementById('dash-properties').innerText = state.properties.length;

        // Auto-select first property if none selected
        if (!state.selectedPropertyId && state.properties.length > 0) {
            state.selectedPropertyId = state.properties[0].id;
            subscribePropertyChannels();
        }

        if (state.selectedPropertyId) {
            const dashRes = await apiCall(`${API_BASE}/properties/${state.selectedPropertyId}/dashboard`);
            updateDashboardStatsUI(dashRes.data);
            populatePropertyDropdowns();
        }
    } catch(err) {
        console.error('Load dashboard error:', err);
    }
}

function populatePropertyDropdowns() {
    const dropdownIds = [
        'bank-config-property-select',
        'price-config-property-select',
        'staff-property-select',
        'report-property-select'
    ];

    dropdownIds.forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        const currentVal = el.value;
        el.innerHTML = '';
        state.properties.forEach(p => {
            const opt = document.createElement('option');
            opt.value = p.id;
            opt.innerText = `${p.name} (${p.code})`;
            el.appendChild(opt);
        });
        if (currentVal && Array.from(el.options).some(o => o.value === currentVal)) {
            el.value = currentVal;
        } else if (state.selectedPropertyId) {
            el.value = state.selectedPropertyId;
        }
    });
}

// Tab Switching Navigation
function initTabs() {
    const menuItems = document.querySelectorAll('.nav-menu .nav-item');
    const panes = document.querySelectorAll('.content-body .tab-pane');

    menuItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const tabId = item.getAttribute('data-tab');
            switchTab(tabId);
        });
    });
}

function switchTab(tabId) {
    const menuItems = document.querySelectorAll('.nav-menu .nav-item');
    const panes = document.querySelectorAll('.content-body .tab-pane');

    menuItems.forEach(i => i.classList.remove('active'));
    panes.forEach(p => p.classList.remove('active'));

    const activeItem = Array.from(menuItems).find(i => i.getAttribute('data-tab') === tabId);
    const activePane = document.getElementById(tabId);

    if (activeItem) activeItem.classList.add('active');
    if (activePane) activePane.classList.add('active');

    state.activeTab = tabId;

    // Set page title
    const titleText = activeItem ? activeItem.querySelector('span').innerText : 'HotelChain Pro';
    document.getElementById('page-title').innerText = titleText;

    // Load data based on tab selection
    if (state.accessToken) {
        if (tabId === 'properties-tab') loadPropertiesTab();
        else if (tabId === 'bookings-tab') loadBookingsTab();
        else if (tabId === 'payments-tab') loadPaymentsTab();
        else if (tabId === 'utility-tab') loadUtilityTab();
        else if (tabId === 'staff-tab') loadStaffTab();
        else if (tabId === 'dashboard-tab') loadDashboardData();
    }
}

// 2. Auth Tab Section
function initAuthForm() {
    const form = document.getElementById('login-form');
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        const user = document.getElementById('login-username').value;
        const pass = document.getElementById('login-password').value;
        login(user, pass);
    });

    document.getElementById('btn-logout').addEventListener('click', logout);
    document.getElementById('btn-refresh-token').addEventListener('click', performTokenRefresh);

    // 2FA Setup Flow
    document.getElementById('btn-setup-2fa').addEventListener('click', async () => {
        try {
            logToTerminal('Initializing 2FA Setup...', 'info');
            const res = await apiCall(`${API_BASE}/auth/2fa/setup`, { method: 'POST' });
            const setup = res.data;

            document.getElementById('qr-placeholder-2fa').innerHTML = `<img src="https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(setup.qrCodeUri)}" alt="2FA QR">`;
            document.getElementById('secret-key-2fa').innerText = `Secret Key: ${setup.secret}`;
            document.getElementById('2fa-qr-code').classList.remove('hidden');
            logToTerminal('Scan the generated QR code in Authenticator App.', 'info');
        } catch(err) {
            showAlert(err.message, 'danger');
        }
    });

    document.getElementById('btn-verify-2fa').addEventListener('click', async () => {
        const otpVal = document.getElementById('otp-2fa-verify').value;
        if (!otpVal) return showAlert('Nhập mã OTP', 'danger');

        try {
            const payload = {
                otp: parseInt(otpVal),
                refreshToken: state.refreshToken
            };
            const res = await apiCall(`${API_BASE}/auth/2fa/verify`, {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            showAlert('Xác thực 2FA kích hoạt thành công!');
            logToTerminal('2FA is now fully active on this account.', 'success');
            document.getElementById('2fa-qr-code').classList.add('hidden');
        } catch(err) {
            showAlert(err.message, 'danger');
        }
    });
}

// 3. Properties Tab Section
function initPropertySection() {
    document.getElementById('btn-show-create-property').addEventListener('click', () => {
        const panel = document.getElementById('property-form-panel');
        panel.classList.toggle('hidden');
    });

    document.getElementById('create-property-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            name: document.getElementById('prop-name').value,
            code: document.getElementById('prop-code').value,
            address: document.getElementById('prop-address').value,
            ward: document.getElementById('prop-district').value || 'Phường 1',
            district: document.getElementById('prop-district').value || 'Quận 1',
            city: document.getElementById('prop-city').value,
            latitude: parseFloat(document.getElementById('prop-lat').value),
            longitude: parseFloat(document.getElementById('prop-lng').value),
            phone: document.getElementById('prop-phone').value,
            email: document.getElementById('prop-email').value,
            type: document.getElementById('prop-type').value,
            starRating: parseInt(document.getElementById('prop-stars').value)
        };

        try {
            logToTerminal(`Creating property: ${payload.name}...`, 'info');
            await apiCall(`${API_BASE}/properties`, {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            showAlert('Tạo chi nhánh thành công! Các phòng mặc định đã được khởi tạo.');
            document.getElementById('property-form-panel').classList.add('hidden');
            loadPropertiesTab();
        } catch(err) {
            showAlert(err.message, 'danger');
        }
    });

    // Add Room button inside floor map panel
    document.getElementById('btn-show-create-room').addEventListener('click', () => {
        if (!state.selectedPropertyId) return;
        openCreateRoomPrompt();
    });
}

async function loadPropertiesTab() {
    try {
        const res = await apiCall(`${API_BASE}/properties`);
        state.properties = res.data || [];
        populatePropertyDropdowns();

        const container = document.getElementById('properties-list');
        container.innerHTML = '';

        if (state.properties.length === 0) {
            container.innerHTML = `<p class="placeholder-text">Chưa có chi nhánh nào. Hãy tạo chi nhánh mới!</p>`;
            return;
        }

        state.properties.forEach(p => {
            const card = document.createElement('div');
            card.className = `property-list-card ${state.selectedPropertyId === p.id ? 'active' : ''}`;
            card.innerHTML = `
                <div class="prop-card-header">
                    <h4>${p.name} <span class="badge badge-purple">${p.code}</span></h4>
                    <span class="badge ${p.isActive ? 'badge-success' : 'badge-danger'}">${p.isActive ? 'Active' : 'Inactive'}</span>
                </div>
                <p class="text-xs text-secondary"><i class="fa-solid fa-map-location-dot"></i> ${p.address}, ${p.city}</p>
                <div class="prop-card-footer margin-top-xs">
                    <button class="btn btn-secondary btn-xs btn-view-rooms" data-id="${p.id}"><i class="fa-solid fa-bed"></i> Quản lý phòng</button>
                    <button class="btn btn-warning btn-xs btn-edit-property" data-id="${p.id}"><i class="fa-solid fa-pen"></i> Sửa</button>
                    <button class="btn btn-primary btn-xs btn-set-active-ws" data-id="${p.id}"><i class="fa-solid fa-plug"></i> Kết nối WS</button>
                </div>
            `;
            container.appendChild(card);
        });

        // Add event listeners
        container.querySelectorAll('.btn-view-rooms').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const propId = btn.getAttribute('data-id');
                selectPropertyForRooms(propId);
            });
        });

        container.querySelectorAll('.btn-edit-property').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const propId = btn.getAttribute('data-id');
                const prop = state.properties.find(p => p.id === propId);
                if (prop) openEditPropertyModal(prop);
            });
        });

        container.querySelectorAll('.btn-set-active-ws').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const propId = btn.getAttribute('data-id');
                state.selectedPropertyId = propId;
                subscribePropertyChannels();
                showAlert(`Đã chuyển kênh WebSocket sang chi nhánh đang chọn.`);
            });
        });

        // Auto display rooms of currently selected property
        if (state.selectedPropertyId) {
            selectPropertyForRooms(state.selectedPropertyId);
        }
    } catch(err) {
        console.error(err);
    }
}

// ── Edit Property Modal ─────────────────────────────────────────────
function openEditPropertyModal(prop) {
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.id = 'edit-property-modal';
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 520px;">
            <span class="close-modal" id="btn-close-edit-prop">&times;</span>
            <h3><i class="fa-solid fa-pen-to-square"></i> Cập nhật chi nhánh</h3>
            <form id="edit-property-form" style="margin-top:16px;">
                <div class="form-group">
                    <label>Tên chi nhánh <span style="color:red">*</span></label>
                    <input type="text" id="edit-prop-name" value="${prop.name || ''}" required>
                </div>
                <div class="form-group">
                    <label>Địa chỉ <span style="color:red">*</span></label>
                    <input type="text" id="edit-prop-address" value="${prop.address || ''}" required>
                </div>
                <div class="grid-2">
                    <div class="form-group">
                        <label>Quận/Huyện</label>
                        <input type="text" id="edit-prop-district" value="${prop.district || ''}">
                    </div>
                    <div class="form-group">
                        <label>Thành phố</label>
                        <input type="text" id="edit-prop-city" value="${prop.city || ''}">
                    </div>
                </div>
                <div class="grid-2">
                    <div class="form-group">
                        <label>Số điện thoại</label>
                        <input type="text" id="edit-prop-phone" value="${prop.phone || ''}">
                    </div>
                    <div class="form-group">
                        <label>Email</label>
                        <input type="email" id="edit-prop-email" value="${prop.email || ''}">
                    </div>
                </div>
                <div class="grid-2">
                    <div class="form-group">
                        <label>Loại hình <span style="color:red">*</span></label>
                        <select id="edit-prop-type">
                            <option value="HOTEL" ${prop.type === 'HOTEL' ? 'selected' : ''}>HOTEL</option>
                            <option value="MOTEL" ${prop.type === 'MOTEL' ? 'selected' : ''}>MOTEL</option>
                            <option value="RESORT" ${prop.type === 'RESORT' ? 'selected' : ''}>RESORT</option>
                            <option value="APARTMENT" ${prop.type === 'APARTMENT' ? 'selected' : ''}>APARTMENT</option>
                            <option value="HOSTEL" ${prop.type === 'HOSTEL' ? 'selected' : ''}>HOSTEL</option>
                            <option value="VILLA" ${prop.type === 'VILLA' ? 'selected' : ''}>VILLA</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Số sao (1-5)</label>
                        <input type="number" id="edit-prop-stars" min="1" max="5" value="${prop.starRating || 3}">
                    </div>
                </div>
                <div class="form-group">
                    <label>Mô tả</label>
                    <textarea id="edit-prop-desc" rows="3">${prop.description || ''}</textarea>
                </div>
                <button type="submit" class="btn btn-primary btn-block" style="margin-top:12px;">
                    <i class="fa-solid fa-floppy-disk"></i> Lưu thay đổi
                </button>
            </form>
        </div>
    `;
    document.body.appendChild(modal);

    document.getElementById('btn-close-edit-prop').addEventListener('click', () => modal.remove());
    modal.addEventListener('click', (e) => { if (e.target === modal) modal.remove(); });

    document.getElementById('edit-property-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            name: document.getElementById('edit-prop-name').value,
            address: document.getElementById('edit-prop-address').value,
            ward: '',
            district: document.getElementById('edit-prop-district').value,
            city: document.getElementById('edit-prop-city').value,
            phone: document.getElementById('edit-prop-phone').value,
            email: document.getElementById('edit-prop-email').value,
            description: document.getElementById('edit-prop-desc').value,
            type: document.getElementById('edit-prop-type').value,
            starRating: parseInt(document.getElementById('edit-prop-stars').value) || 3
        };

        try {
            logToTerminal(`Updating property ${prop.name}...`, 'info');
            await apiCall(`${API_BASE}/properties/${prop.id}`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });
            showAlert('Cập nhật chi nhánh thành công!');
            logToTerminal(`Property "${payload.name}" updated successfully.`, 'success');
            modal.remove();
            loadPropertiesTab();
        } catch(err) {
            showAlert(err.message, 'danger');
            logToTerminal(`Update property failed: ${err.message}`, 'danger');
        }
    });
}

async function selectPropertyForRooms(propertyId) {
    state.selectedPropertyId = propertyId;
    
    // Highlight active card
    const cards = document.querySelectorAll('.property-list-card');
    cards.forEach(c => c.classList.remove('active'));
    // Find matching button card parent
    const activeBtn = document.querySelector(`.btn-view-rooms[data-id="${propertyId}"]`);
    if (activeBtn) {
        activeBtn.closest('.property-list-card').classList.add('active');
    }

    const prop = state.properties.find(p => p.id === propertyId);
    document.getElementById('rooms-section-title').innerText = `Sơ đồ phòng - ${prop ? prop.name : 'Chi nhánh'}`;
    document.getElementById('rooms-section').classList.remove('hidden');

    await renderFloorMap(propertyId);
}

async function renderFloorMap(propertyId) {
    const area = document.getElementById('floor-map-area');
    area.innerHTML = '<p class="placeholder-text">Đang tải sơ đồ tầng...</p>';

    try {
        const res = await apiCall(`${API_BASE}/properties/${propertyId}/rooms/floor-map`);
        const mapData = res.data;
        area.innerHTML = '';

        if (!mapData.floors || mapData.floors.length === 0) {
            area.innerHTML = `<p class="placeholder-text">Chi nhánh này chưa được thêm phòng. Hãy tạo phòng!</p>`;
            return;
        }

        mapData.floors.forEach(f => {
            const floorDiv = document.createElement('div');
            floorDiv.className = 'floor-row';
            floorDiv.innerHTML = `
                <div class="floor-label">Tầng ${f.floor}</div>
                <div class="rooms-grid"></div>
            `;
            const grid = floorDiv.querySelector('.rooms-grid');

            f.rooms.forEach(r => {
                const roomBtn = document.createElement('div');
                roomBtn.className = `room-block-card status-${r.status.toLowerCase()}`;
                
                let colorCode = mapData.statusColors[r.status] || '#6B7280';
                roomBtn.style.borderTop = `4px solid ${colorCode}`;

                roomBtn.innerHTML = `
                    <div class="room-number">P. ${r.roomNumber}</div>
                    <div class="room-type-lbl">${r.roomTypeName}</div>
                    <div class="room-status-lbl">${r.status}</div>
                `;

                roomBtn.addEventListener('click', () => {
                    openRoomDetailsModal(r);
                });

                grid.appendChild(roomBtn);
            });

            area.appendChild(floorDiv);
        });
    } catch(err) {
        area.innerHTML = `<p class="placeholder-text text-danger">Lỗi tải sơ đồ: ${err.message}</p>`;
    }
}

// Display dialog for room info & management
function openRoomDetailsModal(room) {
    const statusOpts = ['AVAILABLE', 'OCCUPIED', 'CLEANING', 'MAINTENANCE', 'OUT_OF_ORDER']
        .map(s => `<option value="${s}" ${room.status === s ? 'selected' : ''}>${s}</option>`)
        .join('');

    const htmlContent = `
        <div style="padding: 10px;">
            <p><strong>Loại phòng:</strong> ${room.roomTypeName}</p>
            <p><strong>Vị trí:</strong> Tầng ${room.floor}</p>
            <p><strong>Chỉ số Nước / Điện ban đầu:</strong> ${room.initialWaterIndex} m³ / ${room.initialElectricIndex} kWh</p>
            <p><strong>Tiện ích:</strong> Ban công: ${room.hasBalcony ? 'Có' : 'Không'}, Cửa sổ: ${room.hasWindow ? 'Có' : 'Không'}, View: ${room.viewType}</p>
            <div class="form-group margin-top-sm">
                <label>Trạng thái phòng</label>
                <select id="popup-room-status-select">${statusOpts}</select>
            </div>
            <button class="btn btn-primary btn-sm margin-top-sm btn-block" id="btn-update-room-status-popup">Cập nhật Trạng thái</button>
            <button class="btn btn-secondary btn-sm margin-top-xs btn-block" id="btn-view-room-utility-history">Lịch sử đồng hồ</button>
            <div class="grid-2 margin-top-xs">
                <button class="btn btn-warning btn-sm" id="btn-edit-room-popup"><i class="fa-solid fa-pen"></i> Sửa phòng</button>
                <button class="btn btn-danger btn-sm" id="btn-delete-room-popup"><i class="fa-solid fa-trash"></i> Xóa phòng</button>
            </div>
        </div>
    `;

    // Simple Alert Modal replacement
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 400px;">
            <span class="close-modal" id="btn-close-temp-modal">&times;</span>
            <h3>Chi tiết phòng ${room.roomNumber}</h3>
            <div id="temp-modal-body"></div>
        </div>
    `;
    document.body.appendChild(modal);
    document.getElementById('temp-modal-body').innerHTML = htmlContent;

    document.getElementById('btn-close-temp-modal').addEventListener('click', () => modal.remove());
    modal.addEventListener('click', (e) => { if (e.target === modal) modal.remove(); });

    document.getElementById('btn-update-room-status-popup').addEventListener('click', async () => {
        const newStatus = document.getElementById('popup-room-status-select').value;
        try {
            await apiCall(`${API_BASE}/rooms/${room.id}/status?status=${newStatus}`, { method: 'PUT' });
            showAlert('Cập nhật trạng thái phòng thành công!');
            modal.remove();
            renderFloorMap(state.selectedPropertyId);
        } catch(err) {
            showAlert(err.message, 'danger');
        }
    });

    document.getElementById('btn-view-room-utility-history').addEventListener('click', async () => {
        try {
            const res = await apiCall(`${API_BASE}/utility/rooms/${room.id}/history`);
            const history = res.data || [];
            let histText = history.length > 0 
                ? history.map(h => `[${h.recordedAtEnd || h.createdAt}] Nước: ${h.waterIndexStart}->${h.waterIndexEnd} | Điện: ${h.electricIndexStart}->${h.electricIndexEnd}`).join('\n')
                : 'Chưa có lịch sử tiêu thụ.';
            alert(`Lịch sử chỉ số phòng ${room.roomNumber}:\n\n${histText}`);
        } catch(err) {
            showAlert(err.message, 'danger');
        }
    });

    document.getElementById('btn-edit-room-popup').addEventListener('click', () => {
        openEditRoomModal(room);
    });

    document.getElementById('btn-delete-room-popup').addEventListener('click', () => {
        deleteRoomDirect(room.id, room.roomNumber, modal);
    });
}

async function openEditRoomModal(room) {
    try {
        const res = await apiCall(`${API_BASE}/properties/${state.selectedPropertyId}/room-types`);
        const types = res.data || [];
        if (types.length === 0) {
            return showAlert('Không tìm thấy Loại phòng!', 'danger');
        }

        const typeOpts = types.map(t => `<option value="${t.id}" ${room.roomTypeId === t.id ? 'selected' : ''}>${t.name} (Giá: ${t.basePrice}đ)</option>`).join('');

        const modal = document.createElement('div');
        modal.className = 'modal';
        modal.id = 'edit-room-modal';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 450px;">
                <span class="close-modal" id="btn-close-room-edit-modal">&times;</span>
                <h3>Cập nhật phòng P. ${room.roomNumber}</h3>
                <form id="edit-room-direct-form" style="margin-top: 16px;">
                    <div class="form-group">
                        <label>Số phòng</label>
                        <input type="text" id="edit-room-number" value="${room.roomNumber}" required>
                    </div>
                    <div class="form-group">
                        <label>Tầng</label>
                        <input type="number" id="edit-room-floor" value="${room.floor}" required>
                    </div>
                    <div class="form-group">
                        <label>Loại phòng</label>
                        <select id="edit-room-type-select">${typeOpts}</select>
                    </div>
                    <div class="grid-2">
                        <div class="form-group">
                            <label>Chỉ số nước đầu</label>
                            <input type="number" step="0.1" id="edit-room-water" value="${room.initialWaterIndex}">
                        </div>
                        <div class="form-group">
                            <label>Chỉ số điện đầu</label>
                            <input type="number" step="0.1" id="edit-room-electric" value="${room.initialElectricIndex}">
                        </div>
                    </div>
                    <div class="form-group">
                        <label>View hướng phòng</label>
                        <select id="edit-room-view">
                            <option value="STREET" ${room.viewType === 'STREET' ? 'selected' : ''}>STREET (Đường phố)</option>
                            <option value="GARDEN" ${room.viewType === 'GARDEN' ? 'selected' : ''}>GARDEN (Sân vườn)</option>
                            <option value="SEA" ${room.viewType === 'SEA' ? 'selected' : ''}>SEA (Biển)</option>
                            <option value="CITY" ${room.viewType === 'CITY' ? 'selected' : ''}>CITY (Thành phố)</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Mô tả phòng</label>
                        <textarea id="edit-room-desc" rows="2">${room.description || ''}</textarea>
                    </div>
                    <button type="submit" class="btn btn-primary btn-block margin-top-sm">Lưu thay đổi</button>
                </form>
            </div>
        `;
        document.body.appendChild(modal);

        document.getElementById('btn-close-room-edit-modal').addEventListener('click', () => modal.remove());
        modal.addEventListener('click', (e) => { if (e.target === modal) modal.remove(); });

        document.getElementById('edit-room-direct-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const payload = {
                roomNumber: document.getElementById('edit-room-number').value,
                floor: parseInt(document.getElementById('edit-room-floor').value),
                roomTypeId: document.getElementById('edit-room-type-select').value,
                description: document.getElementById('edit-room-desc').value,
                initialWaterIndex: parseFloat(document.getElementById('edit-room-water').value),
                initialElectricIndex: parseFloat(document.getElementById('edit-room-electric').value),
                hasBalcony: room.hasBalcony || true,
                hasWindow: room.hasWindow || true,
                viewType: document.getElementById('edit-room-view').value
            };

            try {
                logToTerminal(`Updating room P. ${room.roomNumber} details...`, 'info');
                await apiCall(`${API_BASE}/rooms/${room.id}`, {
                    method: 'PUT',
                    body: JSON.stringify(payload)
                });
                showAlert('Cập nhật thông tin phòng thành công!');
                modal.remove();
                
                // Close parent details modal if open
                const parentModal = document.querySelector('.modal:not(#edit-room-modal)');
                if (parentModal) parentModal.remove();

                renderFloorMap(state.selectedPropertyId);
            } catch(err) {
                showAlert(err.message, 'danger');
            }
        });

    } catch(err) {
        showAlert(err.message, 'danger');
    }
}

async function deleteRoomDirect(roomId, roomNumber, detailsModal) {
    if (!confirm(`Bạn có chắc chắn muốn xóa phòng ${roomNumber} không?`)) return;
    try {
        logToTerminal(`Deleting room P. ${roomNumber}...`, 'info');
        await apiCall(`${API_BASE}/rooms/${roomId}`, { method: 'DELETE' });
        showAlert(`Đã xóa phòng ${roomNumber} thành công!`);
        detailsModal.remove();
        renderFloorMap(state.selectedPropertyId);
    } catch(err) {
        showAlert(err.message, 'danger');
    }
}


// Room creation prompt overlay
async function openCreateRoomPrompt() {
    try {
        const res = await apiCall(`${API_BASE}/properties/${state.selectedPropertyId}/room-types`);
        const types = res.data || [];
        if (types.length === 0) {
            return showAlert('Bạn phải tạo Loại phòng (RoomType) trước!', 'danger');
        }

        const typeOpts = types.map(t => `<option value="${t.id}">${t.name} (Giá: ${t.basePrice}đ)</option>`).join('');

        const modal = document.createElement('div');
        modal.className = 'modal';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 450px;">
                <span class="close-modal" id="btn-close-room-create-modal">&times;</span>
                <h3>Thêm phòng mới</h3>
                <form id="create-room-direct-form">
                    <div class="form-group">
                        <label>Số phòng</label>
                        <input type="text" id="add-room-number" required placeholder="301">
                    </div>
                    <div class="form-group">
                        <label>Tầng</label>
                        <input type="number" id="add-room-floor" required value="3">
                    </div>
                    <div class="form-group">
                        <label>Loại phòng</label>
                        <select id="add-room-type-select">${typeOpts}</select>
                    </div>
                    <div class="grid-2">
                        <div class="form-group">
                            <label>Chỉ số nước đầu</label>
                            <input type="number" step="0.1" id="add-room-water" value="100.0">
                        </div>
                        <div class="form-group">
                            <label>Chỉ số điện đầu</label>
                            <input type="number" step="0.1" id="add-room-electric" value="1000.0">
                        </div>
                    </div>
                    <div class="form-group">
                        <label>View hướng phòng</label>
                        <select id="add-room-view">
                            <option value="STREET">STREET (Đường phố)</option>
                            <option value="GARDEN">GARDEN (Sân vườn)</option>
                            <option value="SEA">SEA (Biển)</option>
                            <option value="CITY">CITY (Thành phố)</option>
                        </select>
                    </div>
                    <button type="submit" class="btn btn-primary btn-block margin-top-sm">Thêm phòng</button>
                </form>
            </div>
        `;
        document.body.appendChild(modal);

        document.getElementById('btn-close-room-create-modal').addEventListener('click', () => modal.remove());

        document.getElementById('create-room-direct-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const payload = {
                roomNumber: document.getElementById('add-room-number').value,
                floor: parseInt(document.getElementById('add-room-floor').value),
                roomTypeId: document.getElementById('add-room-type-select').value,
                description: 'Phòng mới tạo thủ công',
                initialWaterIndex: parseFloat(document.getElementById('add-room-water').value),
                initialElectricIndex: parseFloat(document.getElementById('add-room-electric').value),
                hasBalcony: true,
                hasWindow: true,
                viewType: document.getElementById('add-room-view').value
            };

            try {
                await apiCall(`${API_BASE}/properties/${state.selectedPropertyId}/rooms`, {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                showAlert('Tạo phòng mới thành công!');
                modal.remove();
                renderFloorMap(state.selectedPropertyId);
            } catch(err) {
                showAlert(err.message, 'danger');
            }
        });

    } catch(err) {
        showAlert(err.message, 'danger');
    }
}

// 4. Bookings Tab Section
function initBookingSection() {
    // Filter and search
    document.getElementById('btn-apply-booking-filter').addEventListener('click', loadBookingsTab);

    // Modal show/hide
    document.getElementById('btn-show-create-booking').addEventListener('click', openBookingModal);
    document.getElementById('btn-close-booking-modal').addEventListener('click', () => {
        document.getElementById('create-booking-modal').classList.add('hidden');
    });

    document.getElementById('create-booking-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            roomId: document.getElementById('booking-room-select').value,
            fullName: document.getElementById('booking-guest-name').value,
            phone: document.getElementById('booking-guest-phone').value,
            email: document.getElementById('booking-guest-email').value,
            checkInPlan: document.getElementById('booking-checkin-plan').value + ':00',
            checkOutPlan: document.getElementById('booking-checkout-plan').value + ':00',
            adultsCount: parseInt(document.getElementById('booking-adults').value),
            childrenCount: parseInt(document.getElementById('booking-children').value),
            roomRatePerNight: parseFloat(document.getElementById('booking-rate').value),
            depositAmount: parseFloat(document.getElementById('booking-deposit').value),
            specialRequests: document.getElementById('booking-requests').value,
            source: 'WALK_IN'
        };

        try {
            logToTerminal(`Creating booking reservation for customer: ${payload.fullName}...`, 'info');
            await apiCall(`${API_BASE}/bookings`, {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            showAlert('Tạo đặt phòng thành công!');
            document.getElementById('create-booking-modal').classList.add('hidden');
            loadBookingsTab();
        } catch(err) {
            showAlert(err.message, 'danger');
        }
    });

    // Subform submit operations
    document.getElementById('btn-op-confirm-submit').addEventListener('click', async () => {
        if (!state.selectedBooking) return;
        try {
            await apiCall(`${API_BASE}/bookings/${state.selectedBooking.id}/confirm`, { method: 'PUT' });
            showAlert('Xác nhận đặt phòng thành công!');
            selectBookingDetails(state.selectedBooking.id);
            loadBookingsTab();
        } catch(err) {
            showAlert(err.message, 'danger');
        }
    });

    document.getElementById('btn-op-checkin-submit').addEventListener('click', async () => {
        if (!state.selectedBooking) return;
        const payload = {
            actualCheckIn: new Date().toISOString().substring(0, 19),
            guestIdNumber: document.getElementById('checkin-guest-id').value || '012345678901',
            guestIdType: 'CCCD',
            guestIdImageFrontKey: 'mock-cccd-front.jpg',
            guestIdImageBackKey: 'mock-cccd-back.jpg',
            waterIndexStart: parseFloat(document.getElementById('checkin-water').value),
            waterPhotoStartKey: document.getElementById('checkin-water-photo').value || 'mock-water-meter-key.jpg',
            electricIndexStart: parseFloat(document.getElementById('checkin-electric').value),
            electricPhotoStartKey: document.getElementById('checkin-electric-photo').value || 'mock-electric-meter-key.jpg',
            depositAmount: parseFloat(document.getElementById('checkin-deposit').value),
            depositPaymentMethod: document.getElementById('checkin-deposit-method').value,
            notes: 'Check-in simulated via Testing Dashboard'
        };

        try {
            logToTerminal(`Submitting check-in for booking ID: ${state.selectedBooking.id}...`, 'info');
            await apiCall(`${API_BASE}/bookings/${state.selectedBooking.id}/check-in`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });
            showAlert('Khách đã Check-in và bàn giao phòng!');
            selectBookingDetails(state.selectedBooking.id);
            loadBookingsTab();
        } catch(err) {
            showAlert(err.message, 'danger');
        }
    });

    document.getElementById('btn-op-checkout-submit').addEventListener('click', async () => {
        if (!state.selectedBooking) return;
        const payload = {
            actualCheckOut: new Date().toISOString().substring(0, 19),
            waterIndexEnd: parseFloat(document.getElementById('checkout-water').value),
            waterPhotoEndKey: document.getElementById('checkout-water-photo').value,
            electricIndexEnd: parseFloat(document.getElementById('checkout-electric').value),
            electricPhotoEndKey: document.getElementById('checkout-electric-photo').value,
            checkoutNotes: 'Check-out completed via dashboard',
            roomCondition: document.getElementById('checkout-condition').value
        };

        try {
            logToTerminal(`Calculating checkout invoice for booking ID: ${state.selectedBooking.id}...`, 'info');
            const res = await apiCall(`${API_BASE}/bookings/${state.selectedBooking.id}/check-out`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });
            
            showAlert('Khách đã Check-out! Vui lòng hoàn tất thanh toán hóa đơn.');
            renderInvoiceReceipt(res.data);
            selectBookingDetails(state.selectedBooking.id);
            loadBookingsTab();
        } catch(err) {
            showAlert(err.message, 'danger');
        }
    });

    // Webhook simulation click
    document.getElementById('btn-sim-webhook').addEventListener('click', () => {
        if (!state.selectedBooking) return;
        triggerNapasWebhookSimulation(state.selectedBooking.bookingCode);
    });
}

async function openBookingModal() {
    if (!state.selectedPropertyId) {
        return showAlert('Vui lòng chọn hoặc tạo chi nhánh trước!', 'danger');
    }

    try {
        // Load rooms of the property to show selection
        const res = await apiCall(`${API_BASE}/properties/${state.selectedPropertyId}/rooms`);
        const rooms = res.data || [];
        const select = document.getElementById('booking-room-select');
        select.innerHTML = '';

        if (rooms.length === 0) {
            return showAlert('Chi nhánh chưa có phòng nào. Hãy tạo phòng trước!', 'danger');
        }

        rooms.forEach(r => {
            const opt = document.createElement('option');
            opt.value = r.id;
            opt.innerText = `Phòng ${r.roomNumber} - ${r.roomTypeName} (${r.status})`;
            select.appendChild(opt);
        });

        // Set default dates
        const now = new Date();
        const tomorrow = new Date();
        tomorrow.setDate(now.getDate() + 1);

        document.getElementById('booking-checkin-plan').value = now.toISOString().substring(0, 16);
        document.getElementById('booking-checkout-plan').value = tomorrow.toISOString().substring(0, 16);

        document.getElementById('create-booking-modal').classList.remove('hidden');
    } catch(err) {
        showAlert(err.message, 'danger');
    }
}

async function loadBookingsTab() {
    try {
        const propId = state.selectedPropertyId;
        const status = document.getElementById('filter-booking-status').value;
        const query = document.getElementById('filter-booking-query').value;

        let url = `${API_BASE}/bookings?size=50`;
        if (propId) url += `&propertyId=${propId}`;
        if (status) url += `&status=${status}`;
        if (query) url += `&q=${encodeURIComponent(query)}`;

        const res = await apiCall(url);
        state.bookings = res.content || [];

        const container = document.getElementById('bookings-list');
        container.innerHTML = '';

        if (state.bookings.length === 0) {
            container.innerHTML = `<p class="placeholder-text">Không có booking nào khớp với bộ lọc.</p>`;
            return;
        }

        state.bookings.forEach(b => {
            const card = document.createElement('div');
            card.className = `booking-list-card ${state.selectedBooking && state.selectedBooking.id === b.id ? 'active' : ''}`;
            
            let statusBadge = 'badge-secondary';
            if (b.status === 'PENDING') statusBadge = 'badge-purple';
            else if (b.status === 'CONFIRMED') statusBadge = 'badge-success';
            else if (b.status === 'CHECKED_IN') statusBadge = 'badge-success';
            else if (b.status === 'CHECKED_OUT') statusBadge = 'badge-secondary';
            else if (b.status === 'CANCELLED') statusBadge = 'badge-danger';

            card.innerHTML = `
                <div class="prop-card-header">
                    <strong>P. ${b.roomNumber} - ${b.guestName}</strong>
                    <span class="badge ${statusBadge}">${b.status}</span>
                </div>
                <p class="text-xs text-secondary font-mono">${b.bookingCode}</p>
                <p class="text-xs text-secondary">
                    ${new Date(b.checkInPlan).toLocaleString('vi-VN')} &rarr; ${new Date(b.checkOutPlan).toLocaleString('vi-VN')}
                </p>
                <div class="prop-card-footer margin-top-xs">
                    <button class="btn btn-secondary btn-xs btn-booking-ops" data-id="${b.id}"><i class="fa-solid fa-cog"></i> Tác nghiệp</button>
                </div>
            `;
            container.appendChild(card);
        });

        container.querySelectorAll('.btn-booking-ops').forEach(btn => {
            btn.addEventListener('click', () => {
                const bId = btn.getAttribute('data-id');
                selectBookingDetails(bId);
            });
        });

    } catch(err) {
        console.error(err);
    }
}

async function selectBookingDetails(bookingId) {
    try {
        const res = await apiCall(`${API_BASE}/bookings/${bookingId}`);
        const b = res.data;
        state.selectedBooking = b;

        // Highlight active booking card
        document.querySelectorAll('.booking-list-card').forEach(c => c.classList.remove('active'));
        const activeBtn = document.querySelector(`.btn-booking-ops[data-id="${bookingId}"]`);
        if (activeBtn) activeBtn.closest('.booking-list-card').classList.add('active');

        // Render details panel
        const detailsBox = document.getElementById('booking-selected-details');
        detailsBox.innerHTML = `
            <div class="info-row">
                <span class="info-label">Mã đặt phòng:</span>
                <span class="info-value font-mono font-bold text-purple">${b.bookingCode}</span>
            </div>
            <div class="info-row">
                <span class="info-label">Khách hàng:</span>
                <span class="info-value">${b.guestName} (${b.guestPhone})</span>
            </div>
            <div class="info-row">
                <span class="info-label">Phòng hiện tại:</span>
                <span class="info-value">P. ${b.roomNumber}</span>
            </div>
            <div class="info-row">
                <span class="info-label">Check-in dự kiến:</span>
                <span class="info-value">${new Date(b.checkInPlan).toLocaleString()}</span>
            </div>
            <div class="info-row">
                <span class="info-label">Check-out dự kiến:</span>
                <span class="info-value">${new Date(b.checkOutPlan).toLocaleString()}</span>
            </div>
            <div class="info-row">
                <span class="info-label">Tổng tiền phòng:</span>
                <span class="info-value">${new Intl.NumberFormat('vi-VN').format(b.totalRoomFee)} đ</span>
            </div>
            <div class="info-row">
                <span class="info-label">Tiền cọc:</span>
                <span class="info-value">${new Intl.NumberFormat('vi-VN').format(b.depositAmount)} đ</span>
            </div>
            <div class="info-row">
                <span class="info-label">Còn lại phải trả:</span>
                <span class="info-value text-danger font-bold">${new Intl.NumberFormat('vi-VN').format(b.remainingAmount)} đ</span>
            </div>
        `;

        document.getElementById('booking-actions-panel').classList.remove('hidden');

        // Hide all subforms first
        document.querySelectorAll('.op-subform').forEach(f => f.classList.add('hidden'));
        document.getElementById('invoice-receipt-area').classList.add('hidden');

        // Display operations based on status
        const opsArea = document.getElementById('booking-ops-area');
        opsArea.innerHTML = '';

        if (b.status === 'PENDING') {
            opsArea.innerHTML = `
                <button class="btn btn-success btn-xs" onclick="toggleOpSubform('confirm')"><i class="fa-solid fa-check"></i> Xác nhận</button>
                <button class="btn btn-secondary btn-xs" onclick="toggleOpSubform('checkin')"><i class="fa-solid fa-door-open"></i> Check-in</button>
                <button class="btn btn-danger btn-xs" onclick="cancelBookingDirect('${b.id}')"><i class="fa-solid fa-ban"></i> Hủy phòng</button>
            `;
        } else if (b.status === 'CONFIRMED') {
            opsArea.innerHTML = `
                <button class="btn btn-success btn-xs" onclick="toggleOpSubform('checkin')"><i class="fa-solid fa-door-open"></i> Bắt đầu Check-in</button>
                <button class="btn btn-danger btn-xs" onclick="cancelBookingDirect('${b.id}')"><i class="fa-solid fa-ban"></i> Hủy phòng</button>
            `;
        } else if (b.status === 'CHECKED_IN') {
            opsArea.innerHTML = `
                <button class="btn btn-primary btn-xs" onclick="toggleOpSubform('checkout')"><i class="fa-solid fa-receipt"></i> Quy trình Check-out</button>
                <button class="btn btn-secondary btn-xs" onclick="openExtendPrompt('${b.id}')"><i class="fa-solid fa-clock"></i> Gia hạn</button>
                <button class="btn btn-secondary btn-xs" onclick="openRoomChangePrompt('${b.id}')"><i class="fa-solid fa-exchange-alt"></i> Đổi phòng</button>
            `;
            // Set initial readings in check-out forms dynamically for comfort
            document.getElementById('checkout-water').value = b.initialWaterIndex ? b.initialWaterIndex + 12.5 : 112.5;
            document.getElementById('checkout-electric').value = b.initialElectricIndex ? b.initialElectricIndex + 45.2 : 1045.2;
        } else if (b.status === 'CHECKED_OUT') {
            opsArea.innerHTML = `
                <button class="btn btn-secondary btn-xs" id="btn-view-invoice-btn"><i class="fa-solid fa-file-invoice"></i> Xem hóa đơn</button>
                <button class="btn btn-danger btn-xs" id="btn-download-pdf-btn"><i class="fa-solid fa-file-pdf"></i> Tải PDF</button>
            `;
            setTimeout(() => {
                document.getElementById('btn-view-invoice-btn').addEventListener('click', () => loadAndDisplayInvoice(b.id));
                document.getElementById('btn-download-pdf-btn').addEventListener('click', () => downloadInvoicePDF(b.id));
            }, 100);
        }

    } catch(err) {
        showAlert(err.message, 'danger');
    }
}

// Global action toggler helper
window.toggleOpSubform = function(subformId) {
    document.querySelectorAll('.op-subform').forEach(f => f.classList.add('hidden'));
    document.getElementById('invoice-receipt-area').classList.add('hidden');
    document.getElementById(`subform-${subformId}`).classList.remove('hidden');
};

window.cancelBookingDirect = async function(id) {
    const reason = prompt('Nhập lý do hủy đặt phòng:');
    if (reason === null) return;
    try {
        await apiCall(`${API_BASE}/bookings/${id}/cancel`, {
            method: 'PUT',
            body: JSON.stringify({ reason })
        });
        showAlert('Đã hủy đặt phòng.');
        selectBookingDetails(id);
        loadBookingsTab();
    } catch(err) {
        showAlert(err.message, 'danger');
    }
};

window.openExtendPrompt = async function(id) {
    const hours = prompt('Nhập số ngày muốn gia hạn (vd: 1, 2, 3):', '1');
    if (!hours) return;
    const newCheckout = new Date(state.selectedBooking.checkOutPlan);
    newCheckout.setDate(newCheckout.getDate() + parseInt(hours));

    try {
        await apiCall(`${API_BASE}/bookings/${id}/extend`, {
            method: 'PUT',
            body: JSON.stringify({ newCheckOutPlan: newCheckout.toISOString().substring(0, 16) + ':00' })
        });
        showAlert('Gia hạn lưu trú thành công!');
        selectBookingDetails(id);
        loadBookingsTab();
    } catch(err) {
        showAlert(err.message, 'danger');
    }
};

window.openRoomChangePrompt = async function(id) {
    try {
        // Load available rooms
        const res = await apiCall(`${API_BASE}/properties/${state.selectedPropertyId}/rooms`);
        const rooms = res.data.filter(r => r.status === 'AVAILABLE');
        if (rooms.length === 0) return showAlert('Không có phòng trống nào khả dụng để đổi!', 'danger');

        const roomOpts = rooms.map(r => `<option value="${r.id}">Phòng ${r.roomNumber} (${r.roomTypeName})</option>`).join('');

        const modal = document.createElement('div');
        modal.className = 'modal';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 400px;">
                <span class="close-modal" id="btn-close-rm-change">&times;</span>
                <h3>Chọn phòng cần đổi</h3>
                <div class="form-group margin-top-sm">
                    <label>Chọn phòng mới</label>
                    <select id="change-room-select">${roomOpts}</select>
                </div>
                <div class="form-group">
                    <label>Lý do đổi phòng</label>
                    <input type="text" id="change-room-reason" value="Khách muốn chuyển phòng">
                </div>
                <button class="btn btn-primary btn-block margin-top-sm" id="btn-change-room-submit">Đổi phòng</button>
            </div>
        `;
        document.body.appendChild(modal);

        document.getElementById('btn-close-rm-change').addEventListener('click', () => modal.remove());

        document.getElementById('btn-change-room-submit').addEventListener('click', async () => {
            const newRoomId = document.getElementById('change-room-select').value;
            const reason = document.getElementById('change-room-reason').value;

            try {
                await apiCall(`${API_BASE}/bookings/${id}/change-room`, {
                    method: 'PUT',
                    body: JSON.stringify({ newRoomId, reason })
                });
                showAlert('Đổi phòng thành công!');
                modal.remove();
                selectBookingDetails(id);
                loadBookingsTab();
            } catch(err) {
                showAlert(err.message, 'danger');
            }
        });

    } catch(err) {
        showAlert(err.message, 'danger');
    }
};

// Render invoice HTML response
function renderInvoiceReceipt(checkoutData) {
    const details = document.getElementById('invoice-details');
    details.innerHTML = `
        <div class="info-row">
            <span class="info-label">Tiền phòng (${checkoutData.nights} đêm):</span>
            <span class="info-value">${new Intl.NumberFormat('vi-VN').format(checkoutData.roomFee)} đ</span>
        </div>
        <div class="info-row">
            <span class="info-label">Chỉ số Nước tiêu thụ:</span>
            <span class="info-value">${checkoutData.waterConsumption} m³ (${new Intl.NumberFormat('vi-VN').format(checkoutData.waterCost)} đ)</span>
        </div>
        <div class="info-row">
            <span class="info-label">Chỉ số Điện tiêu thụ:</span>
            <span class="info-value">${checkoutData.electricConsumption} kWh (${new Intl.NumberFormat('vi-VN').format(checkoutData.electricCost)} đ)</span>
        </div>
        <hr class="form-divider">
        <div class="info-row">
            <span class="info-label font-bold text-lg">Tổng hóa đơn:</span>
            <span class="info-value font-bold text-lg text-danger">${new Intl.NumberFormat('vi-VN').format(checkoutData.totalBill)} đ</span>
        </div>
        <div class="info-row">
            <span class="info-label">Đã khấu trừ tiền cọc:</span>
            <span class="info-value text-success">-${new Intl.NumberFormat('vi-VN').format(checkoutData.depositDeducted)} đ</span>
        </div>
        <div class="info-row">
            <span class="info-label font-bold text-xl">Số tiền cần trả:</span>
            <span class="info-value font-bold text-xl text-purple">${new Intl.NumberFormat('vi-VN').format(checkoutData.amountDue)} đ</span>
        </div>
    `;

    document.getElementById('invoice-receipt-area').classList.remove('hidden');

    // Load QR
    const qrContainer = document.getElementById('checkout-payment-qr-container');
    const qrBox = document.getElementById('checkout-qr-image-box');
    qrBox.innerHTML = '';

    if (checkoutData.amountDue > 0) {
        qrBox.innerHTML = '<p class="placeholder-text">Đang tạo mã QR thanh toán...</p>';
        qrContainer.classList.remove('hidden');
        
        apiCall(`${API_BASE}/payments/generate-qr`, {
            method: 'POST',
            body: JSON.stringify({
                bookingId: state.selectedBooking.id,
                amount: checkoutData.amountDue
            })
        }).then(res => {
            const qr = res.data;
            if (qr && qr.qrDataUrl) {
                qrBox.innerHTML = `
                    <img src="${qr.qrDataUrl}" style="max-width: 250px; border-radius: 8px;">
                    <p class="text-xs text-secondary margin-top-xs">Quét mã QR để thanh toán qua tài khoản ngân hàng của chi nhánh.</p>
                `;
            } else {
                // Fallback
                qrBox.innerHTML = `
                    <img src="https://api.vietqr.io/image/970407-1903657891001-compact2.jpg?amount=${checkoutData.amountDue}&addInfo=THANH%20TOAN%20PHONG%20${state.selectedBooking.bookingCode}" style="max-width: 250px; border-radius: 8px;">
                    <p class="text-xs text-secondary margin-top-xs">Ngân hàng Techcombank (Fallback) · Số tài khoản: 1903657891001</p>
                `;
            }
        }).catch(err => {
            console.error(err);
            // Fallback
            qrBox.innerHTML = `
                <img src="https://api.vietqr.io/image/970407-1903657891001-compact2.jpg?amount=${checkoutData.amountDue}&addInfo=THANH%20TOAN%20PHONG%20${state.selectedBooking.bookingCode}" style="max-width: 250px; border-radius: 8px;">
                <p class="text-xs text-secondary margin-top-xs">Ngân hàng Techcombank (Fallback) · Số tài khoản: 1903657891001</p>
            `;
        });
    } else {
        qrContainer.classList.add('hidden');
    }
}

async function loadAndDisplayInvoice(bookingId) {
    try {
        const res = await apiCall(`${API_BASE}/bookings/${bookingId}/invoice`);
        const inv = res.data;
        
        // Structure map details
        const mockCheckout = {
            nights: inv.nights || 1,
            roomFee: inv.totalRoomFee || 0,
            waterConsumption: inv.waterUsage || 0,
            waterCost: inv.waterTotal || 0,
            electricConsumption: inv.electricUsage || 0,
            electricCost: inv.electricTotal || 0,
            totalBill: inv.totalAmount || 0,
            depositDeducted: inv.depositAmount || 0,
            amountDue: inv.remainingAmount || 0
        };

        toggleOpSubform('checkout');
        renderInvoiceReceipt(mockCheckout);
        document.getElementById('checkout-payment-qr-container').classList.add('hidden'); // already paid
    } catch(err) {
        showAlert(err.message, 'danger');
    }
}

function downloadInvoicePDF(bookingId) {
    window.open(`${API_BASE}/bookings/${bookingId}/invoice/pdf?access_token=${state.accessToken}`);
}

// 5. Payments Tab Section
function initPaymentSection() {
    document.getElementById('bank-config-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const propId = document.getElementById('bank-config-property-select').value;
        const payload = {
            accountHolderName: document.getElementById('bank-holder').value.toUpperCase(),
            accountNumber: document.getElementById('bank-number').value,
            bankCode: document.getElementById('bank-code-select').value,
            templateDescription: document.getElementById('bank-template').value,
            requireUtilityPhoto: document.getElementById('bank-req-photo').checked,
            requireUtilityInput: document.getElementById('bank-req-input').checked,
            autoConfirmEnabled: document.getElementById('bank-auto-webhook').checked
        };

        try {
            logToTerminal(`Updating Bank Account settings for branch: ${propId}...`, 'info');
            await apiCall(`${API_BASE}/admin/bank-configs/${propId}`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });
            showAlert('Cập nhật cấu hình tài khoản ngân hàng thành công!');
            loadPaymentsTab();
        } catch(err) {
            showAlert(err.message, 'danger');
        }
    });

    document.getElementById('btn-trigger-webhook-mock').addEventListener('click', () => {
        const code = document.getElementById('webhook-sim-booking-code').value;
        if (!code) return showAlert('Vui lòng nhập mã đặt phòng!', 'danger');
        triggerNapasWebhookSimulation(code);
    });
}

async function loadPaymentsTab() {
    if (!state.selectedPropertyId) return;
    try {
        const res = await apiCall(`${API_BASE}/admin/bank-configs/${state.selectedPropertyId}`);
        const c = res.data;

        if (c) {
            document.getElementById('bank-holder').value = c.accountHolderName;
            document.getElementById('bank-number').value = c.accountNumber;
            document.getElementById('bank-code-select').value = c.bankCode;
            document.getElementById('bank-template').value = c.templateDescription;
            document.getElementById('bank-req-photo').checked = c.requireUtilityPhoto;
            document.getElementById('bank-req-input').checked = c.requireUtilityInput;
            document.getElementById('bank-auto-webhook').checked = c.autoConfirmEnabled;

            // Load static QR image
            document.getElementById('static-qr-placeholder').innerHTML = `
                <img src="https://api.vietqr.io/image/${c.bankCode}-${c.accountNumber}-compact.jpg?accountName=${encodeURIComponent(c.accountHolderName)}&addInfo=${encodeURIComponent(c.templateDescription)}" style="max-width: 250px; border-radius: 8px;">
                <p class="text-xs text-secondary margin-top-xs">Quét QR tĩnh của cơ sở để thanh toán nhanh tự động.</p>
            `;
        } else {
            document.getElementById('static-qr-placeholder').innerHTML = `Chưa có cấu hình ngân hàng. Hãy lưu cấu hình phía bên trái để tạo QR!`;
        }
    } catch(err) {
        console.error(err);
    }
}

// Simulated automated bank gateway API webhook trigger
async function triggerNapasWebhookSimulation(bookingCode) {
    try {
        logToTerminal(`Simulating Napas webhook transaction callback for: ${bookingCode}...`, 'info');
        const payloadStr = `NAPAS TRANSFER RECEIVED FOR: ${bookingCode}. REFERENCE TXN: WEB2-TEST-${Date.now()}`;
        
        const response = await fetch(`${API_BASE}/payments/webhook`, {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain',
                'X-Bank-Code': 'TCB'
            },
            body: payloadStr
        });

        if (response.ok) {
            showAlert('Giả lập gọi Webhook ngân hàng thành công! Vui lòng đợi WebSocket thông báo cập nhật.');
            logToTerminal('Bank Webhook response: 200 OK', 'success');
            
            // Reload active view
            if (state.activeTab === 'bookings-tab' && state.selectedBooking) {
                setTimeout(() => selectBookingDetails(state.selectedBooking.id), 1500);
            }
        } else {
            showAlert('Lỗi webhook: ' + response.statusText, 'danger');
        }
    } catch(err) {
        showAlert(err.message, 'danger');
    }
}

// 6. Utility meter readings tab
function initUtilitySection() {
    document.getElementById('price-use-fixed-electric').addEventListener('change', (e) => {
        const isFixed = e.target.checked;
        document.getElementById('fixed-electric-group').className = isFixed ? 'form-group' : 'form-group hidden';
        document.getElementById('tiered-electric-group').className = isFixed ? 'hidden' : 'margin-top-sm';
    });

    document.getElementById('utility-price-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const propId = document.getElementById('price-config-property-select').value;
        const payload = {
            waterPricePerUnit: parseFloat(document.getElementById('price-water').value),
            useFixedElectricPrice: document.getElementById('price-use-fixed-electric').checked,
            fixedElectricPrice: parseFloat(document.getElementById('price-electric-fixed').value),
            electricTier1Price: parseFloat(document.getElementById('price-electric-t1').value),
            electricTier2Price: parseFloat(document.getElementById('price-electric-t2').value),
            electricTier3Price: parseFloat(document.getElementById('price-electric-t3').value),
            electricTier4Price: parseFloat(document.getElementById('price-electric-t4').value),
            electricTier5Price: parseFloat(document.getElementById('price-electric-t5').value),
            electricTier6Price: parseFloat(document.getElementById('price-electric-t6').value)
        };

        try {
            logToTerminal(`Updating utility billing configuration for property: ${propId}...`, 'info');
            await apiCall(`${API_BASE}/utility/prices?propertyId=${propId}`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });
            showAlert('Lưu cấu hình đơn giá điện nước thành công!');
        } catch(err) {
            showAlert(err.message, 'danger');
        }
    });

    // Handle dummy file upload photo
    document.getElementById('utility-photo-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const bookingId = document.getElementById('photo-booking-select').value;
        const type = document.getElementById('photo-type').value;
        const period = document.getElementById('photo-period').value;
        const fileInput = document.getElementById('photo-file');

        if (fileInput.files.length === 0) return showAlert('Chọn tệp ảnh', 'danger');

        const formData = new FormData();
        formData.append('file', fileInput.files[0]);
        formData.append('type', type);
        formData.append('bookingId', bookingId);
        formData.append('period', period);

        try {
            logToTerminal(`Uploading meter photo (${type} - ${period}) for verification...`, 'info');
            
            // Native multipart fetch
            const response = await fetch(`${API_BASE}/utility/upload-photo`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${state.accessToken}`
                },
                body: formData
            });

            if (!response.ok) throw new Error(response.statusText);
            const res = await response.json();
            
            showAlert('Upload và phân tích ảnh đồng hồ thành công!');
            document.getElementById('photo-upload-result').classList.remove('hidden');
            document.getElementById('photo-upload-json').innerText = JSON.stringify(res.data, null, 2);
            logToTerminal(`EXIF check: GPS ${res.data.gpsLatitude},${res.data.gpsLongitude} | SHA-256: ${res.data.hashSha256}`, 'success');
        } catch(err) {
            showAlert(err.message, 'danger');
        }
    });

    // Submit manual reading form
    document.getElementById('utility-reading-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const bookingId = document.getElementById('reading-booking-select').value;
        const period = document.getElementById('reading-period').value;
        const waterIndex = parseFloat(document.getElementById('reading-water-index').value);
        const electricIndex = parseFloat(document.getElementById('reading-electric-index').value);
        const waterPhoto = document.getElementById('reading-water-photo-key').value;
        const electricPhoto = document.getElementById('reading-electric-photo-key').value;

        if (period === 'START') {
            const payload = {
                bookingId,
                waterIndexStart: waterIndex,
                waterPhotoStartKey: waterPhoto || 'manual-water-start.jpg',
                electricIndexStart: electricIndex,
                electricPhotoStartKey: electricPhoto || 'manual-electric-start.jpg'
            };
            try {
                logToTerminal(`Recording START utility readings for Booking ID: ${bookingId}...`, 'info');
                await apiCall(`${API_BASE}/utility/readings/start`, {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                showAlert('Ghi chỉ số đầu kỳ (START) thành công!');
                loadBookingReadings(bookingId);
            } catch(err) {
                showAlert(err.message, 'danger');
            }
        } else {
            const payload = {
                bookingId,
                waterIndexEnd: waterIndex,
                waterPhotoEndKey: waterPhoto || 'manual-water-end.jpg',
                electricIndexEnd: electricIndex,
                electricPhotoEndKey: electricPhoto || 'manual-electric-end.jpg'
            };
            try {
                logToTerminal(`Recording END utility readings for Booking ID: ${bookingId}...`, 'info');
                await apiCall(`${API_BASE}/utility/readings/end`, {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                showAlert('Ghi chỉ số cuối kỳ (END) thành công!');
                loadBookingReadings(bookingId);
            } catch(err) {
                showAlert(err.message, 'danger');
            }
        }
    });

    // Lookup readings button
    document.getElementById('btn-lookup-readings').addEventListener('click', () => {
        const bookingId = document.getElementById('lookup-booking-select').value;
        if (!bookingId) return showAlert('Chọn một Booking!', 'danger');
        loadBookingReadings(bookingId);
    });
}

async function loadUtilityTab() {
    if (!state.selectedPropertyId) return;
    try {
        const res = await apiCall(`${API_BASE}/utility/prices?propertyId=${state.selectedPropertyId}`);
        const c = res.data;
        if (c) {
            document.getElementById('price-water').value = c.waterPricePerUnit;
            document.getElementById('price-use-fixed-electric').checked = c.useFixedElectricPrice;
            document.getElementById('price-electric-fixed').value = c.fixedElectricPrice;
            
            document.getElementById('price-electric-t1').value = c.electricTier1Price || 1728;
            document.getElementById('price-electric-t2').value = c.electricTier2Price || 1786;
            document.getElementById('price-electric-t3').value = c.electricTier3Price || 2074;
            document.getElementById('price-electric-t4').value = c.electricTier4Price || 2612;
            document.getElementById('price-electric-t5').value = c.electricTier5Price || 2919;
            document.getElementById('price-electric-t6').value = c.electricTier6Price || 3015;

            // Trigger change event to set visibility
            document.getElementById('price-use-fixed-electric').dispatchEvent(new Event('change'));
        }

        // Load active booking options for dropdowns
        const bookingsRes = await apiCall(`${API_BASE}/bookings?size=100&propertyId=${state.selectedPropertyId}`);
        const list = bookingsRes.content || [];
        
        const photoSelect = document.getElementById('photo-booking-select');
        const readingSelect = document.getElementById('reading-booking-select');
        const lookupSelect = document.getElementById('lookup-booking-select');

        photoSelect.innerHTML = '';
        readingSelect.innerHTML = '';
        lookupSelect.innerHTML = '';

        list.forEach(b => {
            const opt = document.createElement('option');
            opt.value = b.id;
            opt.innerText = `P. ${b.roomNumber} - ${b.guestName} (${b.bookingCode})`;
            
            photoSelect.appendChild(opt.cloneNode(true));
            readingSelect.appendChild(opt.cloneNode(true));
            lookupSelect.appendChild(opt.cloneNode(true));
        });

    } catch(err) {
        console.error(err);
    }
}

async function loadBookingReadings(bookingId) {
    const container = document.getElementById('lookup-readings-content');
    const box = document.getElementById('lookup-readings-box');
    container.innerHTML = '<p class="placeholder-text">Đang tải chỉ số...</p>';
    box.classList.remove('hidden');

    try {
        const res = await apiCall(`${API_BASE}/utility/readings/${bookingId}`);
        const r = res.data;
        if (!r) {
            container.innerHTML = `<p class="placeholder-text">Chưa có chỉ số nào được ghi nhận cho Booking này.</p>`;
            return;
        }

        // Render readings info & status
        let html = `
            <div style="font-size: 14px; line-height: 1.6;">
                <p><strong>ID đồng hồ:</strong> <span class="font-mono text-xs">${r.id}</span></p>
                <p><strong>Trạng thái xác minh:</strong> 
                    <span class="badge ${r.verified ? 'badge-success' : (r.disputed ? 'badge-danger' : 'badge-purple')}">
                        ${r.verified ? 'ĐÃ XÁC MINH' : (r.disputed ? 'TRANH CHẤP' : 'CHỜ XÁC MINH')}
                    </span>
                </p>
                <div class="grid-2 margin-top-xs" style="background: rgba(0,0,0,0.2); padding: 8px; border-radius: 4px;">
                    <div>
                        <h5 class="text-purple font-bold">Chỉ số Đầu (Start)</h5>
                        <p>Nước: <strong>${r.waterIndexStart} m³</strong></p>
                        <p>Điện: <strong>${r.electricIndexStart} kWh</strong></p>
                        <p class="text-xs text-secondary font-mono">${r.waterPhotoStartKey || 'Không có ảnh'}</p>
                    </div>
                    <div>
                        <h5 class="text-purple font-bold">Chỉ số Cuối (End)</h5>
                        <p>Nước: <strong>${r.waterIndexEnd !== null ? r.waterIndexEnd + ' m³' : 'Chưa ghi'}</strong></p>
                        <p>Điện: <strong>${r.electricIndexEnd !== null ? r.electricIndexEnd + ' kWh' : 'Chưa ghi'}</strong></p>
                        <p class="text-xs text-secondary font-mono">${r.waterPhotoEndKey || 'Không có ảnh'}</p>
                    </div>
                </div>
        `;

        if (r.disputed && r.disputeReason) {
            html += `<p class="margin-top-xs text-danger"><strong>Lý do tranh chấp:</strong> ${r.disputeReason}</p>`;
        }

        // Action buttons
        html += `
            <div class="margin-top-sm flex-row" style="gap: 8px;">
                <button class="btn btn-success btn-xs" id="btn-verify-reading-api" ${r.verified ? 'disabled' : ''}><i class="fa-solid fa-check"></i> Xác minh (Verify)</button>
                <button class="btn btn-danger btn-xs" id="btn-dispute-reading-api" ${r.verified ? 'disabled' : ''}><i class="fa-solid fa-triangle-exclamation"></i> Báo tranh chấp (Dispute)</button>
            </div>
        </div>
        `;

        container.innerHTML = html;

        // Attach event listeners to verify/dispute
        if (!r.verified) {
            document.getElementById('btn-verify-reading-api').addEventListener('click', async () => {
                try {
                    logToTerminal(`Verifying utility readings ID ${r.id}...`, 'info');
                    await apiCall(`${API_BASE}/utility/readings/${r.id}/verify`, { method: 'PUT' });
                    showAlert('Xác minh chỉ số điện nước thành công!');
                    loadBookingReadings(bookingId);
                } catch(err) {
                    showAlert(err.message, 'danger');
                }
            });

            document.getElementById('btn-dispute-reading-api').addEventListener('click', async () => {
                const reason = prompt('Nhập lý do tranh chấp chỉ số:');
                if (!reason) return;
                try {
                    logToTerminal(`Disputing utility readings ID ${r.id}...`, 'info');
                    await apiCall(`${API_BASE}/utility/readings/${r.id}/dispute`, {
                        method: 'PUT',
                        body: JSON.stringify({ disputeReason: reason })
                    });
                    showAlert('Ghi nhận tranh chấp chỉ số thành công!');
                    loadBookingReadings(bookingId);
                } catch(err) {
                    showAlert(err.message, 'danger');
                }
            });
        }

    } catch(err) {
        container.innerHTML = `<p class="placeholder-text text-danger">Lỗi: ${err.message}</p>`;
    }
}

// 7. Staff Tab Section
function initStaffSection() {
    document.getElementById('btn-show-create-staff').addEventListener('click', () => {
        document.getElementById('staff-form-panel').classList.toggle('hidden');
    });

    document.getElementById('create-staff-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            fullName: document.getElementById('staff-name').value,
            email: document.getElementById('staff-email').value,
            phone: document.getElementById('staff-phone').value,
            role: document.getElementById('staff-role').value,
            propertyId: document.getElementById('staff-property-select').value,
            address: document.getElementById('staff-address').value || 'TPHCM'
        };

        try {
            logToTerminal(`Creating staff: ${payload.fullName}...`, 'info');
            await apiCall(`${API_BASE}/staff`, {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            showAlert('Thêm nhân sự mới thành công! Tài khoản đăng nhập đã được cấp.');
            document.getElementById('staff-form-panel').classList.add('hidden');
            loadStaffTab();
        } catch(err) {
            showAlert(err.message, 'danger');
        }
    });
}

async function loadStaffTab() {
    try {
        let url = `${API_BASE}/staff?size=100`;
        if (state.selectedPropertyId) url += `&propertyId=${state.selectedPropertyId}`;
        
        const res = await apiCall(url);
        const staff = res.content || [];

        const container = document.getElementById('staff-list');
        container.innerHTML = '';

        if (staff.length === 0) {
            container.innerHTML = `<p class="placeholder-text">Không có nhân sự nào đăng ký ở chi nhánh này.</p>`;
            return;
        }

        staff.forEach(s => {
            const card = document.createElement('div');
            card.className = 'staff-list-card';
            card.innerHTML = `
                <div class="prop-card-header">
                    <strong>${s.fullName}</strong>
                    <span class="badge badge-purple">${s.role}</span>
                </div>
                <p class="text-xs text-secondary"><i class="fa-solid fa-envelope"></i> ${s.email} | <i class="fa-solid fa-phone"></i> ${s.phone}</p>
                <div class="prop-card-footer margin-top-xs">
                    <button class="btn btn-secondary btn-xs" onclick="simulateClockIn('${s.id}')"><i class="fa-solid fa-check-double"></i> Ca & Chấm công</button>
                    <button class="btn btn-danger btn-xs" onclick="resetStaffPassword('${s.id}')"><i class="fa-solid fa-lock-open"></i> Đổi pass</button>
                </div>
            `;
            container.appendChild(card);
        });
    } catch(err) {
        console.error(err);
    }
}

window.resetStaffPassword = async function(id) {
    try {
        const res = await apiCall(`${API_BASE}/staff/${id}/reset-password`, { method: 'PUT' });
        alert(`Mật khẩu đã reset thành công. Mật khẩu tạm thời mới:\n\n${res.data.temporaryPassword}`);
        logToTerminal(`Password reset for user ${id}. Temp password generated.`, 'info');
    } catch(err) {
        showAlert(err.message, 'danger');
    }
};

window.simulateClockIn = async function(staffId) {
    // Simulated shift creation and clock in/out for testing attendance
    const lat = prompt('Vĩ độ GPS chấm công:', '10.776889');
    if (!lat) return;
    const lng = prompt('Kinh độ GPS chấm công:', '106.700806');

    try {
        logToTerminal('Generating a shift schedule...', 'info');
        // First create a shift schedule
        const shiftRes = await apiCall(`${API_BASE}/shifts`, {
            method: 'POST',
            body: JSON.stringify({
                staffId,
                propertyId: state.selectedPropertyId,
                name: 'Ca Sáng kiểm tra',
                startTime: new Date().toISOString().substring(0, 11) + '08:00:00',
                endTime: new Date().toISOString().substring(0, 11) + '17:00:00'
            })
        });

        const shift = shiftRes.data;
        logToTerminal(`Shift created: ID ${shift.id}. Sending clock-in payload...`, 'info');

        // Clock in
        await apiCall(`${API_BASE}/shifts/${shift.id}/clock-in`, {
            method: 'POST',
            body: JSON.stringify({
                latitude: parseFloat(lat),
                longitude: parseFloat(lng),
                deviceInfo: 'Testing Dashboard SPA client'
            })
        });

        showAlert('Chấm công ca làm việc (Clock-in) thành công!');
        logToTerminal(`Staff ID ${staffId} clocked in to shift ID ${shift.id}`, 'success');

        // Offer immediate clock-out simulation
        if (confirm('Bạn có muốn mô phỏng Chấm công Ra (Clock-out) cho ca này luôn không?')) {
            await apiCall(`${API_BASE}/shifts/${shift.id}/clock-out`, {
                method: 'POST',
                body: JSON.stringify({
                    latitude: parseFloat(lat),
                    longitude: parseFloat(lng),
                    deviceInfo: 'Testing Dashboard SPA client'
                })
            });
            showAlert('Chấm công Ra (Clock-out) thành công!');
            logToTerminal(`Staff ID ${staffId} clocked out from shift ID ${shift.id}`, 'success');
        }

    } catch(err) {
        showAlert(err.message, 'danger');
    }
};

// 8. Reports Tab Section
function initReportSection() {
    const todayStr = new Date().toISOString().substring(0, 10);
    document.getElementById('report-from').value = todayStr;
    document.getElementById('report-to').value = todayStr;

    document.getElementById('btn-view-report-revenue').addEventListener('click', () => loadReport('revenue'));
    document.getElementById('btn-view-report-occupancy').addEventListener('click', () => loadReport('occupancy'));
    document.getElementById('btn-view-report-utility').addEventListener('click', () => loadReport('utility'));

    document.getElementById('btn-export-excel').addEventListener('click', () => triggerExport('excel'));
    document.getElementById('btn-export-pdf').addEventListener('click', () => triggerExport('pdf'));
}

async function loadReport(type) {
    const propId = document.getElementById('report-property-select').value;
    const from = document.getElementById('report-from').value;
    const to = document.getElementById('report-to').value;
    const group = document.getElementById('report-groupby').value;

    if (!propId) return showAlert('Chọn chi nhánh', 'danger');

    let endpoint = `${API_BASE}/reports/${type}?propertyId=${propId}&from=${from}&to=${to}`;
    if (type === 'revenue') endpoint += `&groupBy=${group}`;

    try {
        logToTerminal(`Fetching analytics report JSON for: ${type}...`, 'info');
        const res = await apiCall(endpoint);
        document.getElementById('report-output-json').innerText = JSON.stringify(res.data, null, 2);
    } catch(err) {
        showAlert(err.message, 'danger');
    }
}

function triggerExport(format) {
    const propId = document.getElementById('report-property-select').value;
    const from = document.getElementById('report-from').value;
    const to = document.getElementById('report-to').value;
    if (!propId) return showAlert('Chọn chi nhánh', 'danger');

    const downloadUrl = `${API_BASE}/reports/export/${format}?propertyId=${propId}&from=${from}&to=${to}&type=revenue&access_token=${state.accessToken}`;
    window.open(downloadUrl);
}

// 9. Real-time STOMP log terminal
function initLogsSection() {
    document.getElementById('btn-clear-logs').addEventListener('click', () => {
        document.getElementById('terminal-logs').innerHTML = '';
        logToTerminal('Bảng log đã được dọn sạch.', 'info');
    });

    document.getElementById('btn-reconnect-ws').addEventListener('click', () => {
        if (state.stompClient) {
            state.stompClient.disconnect();
            state.stompClient = null;
        }
        connectWebSocket();
    });
}
