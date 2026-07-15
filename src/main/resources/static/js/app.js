const API_BASE = '/api';

// State
let currentUser = null;

// DOM Elements
const views = {
    login: document.getElementById('view-login'),
    admin: document.getElementById('view-admin'),
    user: document.getElementById('view-user')
};

// Utils
function showView(viewName) {
    Object.values(views).forEach(v => v.classList.add('hidden'));
    views[viewName].classList.remove('hidden');
}

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast ${type}`;
    setTimeout(() => toast.classList.add('hidden'), 3000);
}

function formatMoney(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

// Number formatting utils
function formatNumberInput(e) {
    // Only format integers for rent and prices. For water/electric, we might need floats but here we keep it simple or allow basic float?
    // The user requirement is mostly about large currency numbers.
    let val = e.target.value.replace(/[^\d]/g, '');
    if (val) {
        val = parseInt(val, 10).toLocaleString('vi-VN');
    }
    e.target.value = val;
}

function parseFormattedNumber(val) {
    if (!val) return 0;
    return parseFloat(val.replace(/\./g, '')); // vi-VN uses dot for thousand separator
}

document.addEventListener('input', (e) => {
    if (e.target.classList.contains('number-format')) {
        formatNumberInput(e);
    }
});

function getHeaders() {
    const headers = {
        'Content-Type': 'application/json'
    };
    if (currentUser) {
        headers['Authorization'] = `Bearer ${currentUser.token}`;
        headers['X-User-Id'] = currentUser.userId; // Giữ lại cho an toàn
    }
    return headers;
}

// Hàm fetch wrapper để bật/tắt loading spinner
async function apiFetch(url, options = {}) {
    const spinner = document.getElementById('global-spinner');
    if (spinner) spinner.classList.remove('hidden');
    try {
        const res = await window.fetch(url, options);
        return res;
    } finally {
        if (spinner) spinner.classList.add('hidden');
    }
}

// ---------------------------------------------------------
// DOM EVENT LISTENERS
// ---------------------------------------------------------
window.addEventListener('DOMContentLoaded', () => {
    const savedUser = localStorage.getItem('nhatro_user');
    if (savedUser) {
        currentUser = JSON.parse(savedUser);
        if (currentUser.role === 'ADMIN') {
            showView('admin');
            initAdminApp();
        } else {
            showView('user');
            initUserApp();
        }
    } else {
        showView('login');
    }
});

// LOGIN
document.getElementById('form-login').addEventListener('submit', async (e) => {
    e.preventDefault();
    const phone = document.getElementById('login-phone').value;
    
    try {
        const res = await apiFetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ phone })
        });
        const data = await res.json();
        
        if (data.success) {
            currentUser = data.data;
            localStorage.setItem('nhatro_user', JSON.stringify(currentUser));
            showToast('Đăng nhập thành công');
            
            if (currentUser.role === 'ADMIN') {
                showView('admin');
                initAdminApp();
            } else {
                showView('user');
                initUserApp();
            }
        } else {
            showToast(data.message, 'error');
        }
    } catch (e) {
        showToast('Lỗi kết nối máy chủ', 'error');
    }
});

function logout() {
    currentUser = null;
    localStorage.removeItem('nhatro_user');
    showView('login');
}

document.getElementById('btn-admin-logout').addEventListener('click', logout);
document.getElementById('btn-user-logout').addEventListener('click', logout);

// MODALS
document.querySelectorAll('.btn-close-modal').forEach(btn => {
    btn.addEventListener('click', (e) => {
        e.target.closest('.modal-overlay').classList.add('hidden');
    });
});

/* =========================================================
   ADMIN APP
========================================================= */
function initAdminApp() {
    document.getElementById('admin-name').textContent = currentUser.fullName;
    
    // Nav routing
    document.querySelectorAll('#view-admin .nav-item').forEach(nav => {
        nav.addEventListener('click', (e) => {
            document.querySelectorAll('#view-admin .nav-item').forEach(n => n.classList.remove('active'));
            e.target.classList.add('active');
            
            const targetId = e.target.getAttribute('data-target');
            document.querySelectorAll('#view-admin .tab-content').forEach(tc => tc.classList.add('hidden'));
            document.getElementById(targetId).classList.remove('hidden');
            
            document.getElementById('admin-page-title').textContent = e.target.textContent.replace(/[^\w\s\u00C0-\u1EF9]/g, '').trim();
            
            // Close sidebar on mobile
            if (window.innerWidth <= 768) {
                document.getElementById('admin-sidebar').classList.remove('active');
            }

            if (targetId === 'admin-dashboard') loadDashboard();
            if (targetId === 'admin-branches') loadBranches();
            if (targetId === 'admin-room-map') loadRoomMap();
            if (targetId === 'admin-rooms') loadRooms();
            if (targetId === 'admin-bookings') loadBookings();
            if (targetId === 'admin-tenants') loadTenants();
            if (targetId === 'admin-reports') loadPendingReports();
            if (targetId === 'admin-history') loadPaidReports();
            if (targetId === 'admin-settings') loadSettings();
        });
    });

    loadDashboard(); // initial load
    
    // Mobile sidebar toggle
    document.getElementById('btn-open-sidebar')?.addEventListener('click', () => {
        document.getElementById('admin-sidebar').classList.add('active');
    });
    document.getElementById('btn-close-sidebar')?.addEventListener('click', () => {
        document.getElementById('admin-sidebar').classList.remove('active');
    });
}

// DASHBOARD
let revenueChart = null;
async function loadDashboard() {
    try {
        const res = await apiFetch(`${API_BASE}/rooms/stats`, { headers: getHeaders() });
        const data = await res.json();
        if (data.data) {
            document.getElementById('dash-total-rooms').textContent = data.data.total;
            document.getElementById('dash-empty-rooms').textContent = data.data.available;
        }

        const resTenants = await apiFetch(`${API_BASE}/tenants`, { headers: getHeaders() });
        const dataTenants = await resTenants.json();
        if (dataTenants.data) {
            document.getElementById('dash-total-tenants').textContent = dataTenants.data.length;
        }

        const resStats = await apiFetch(`${API_BASE}/reports/stats`, { headers: getHeaders() });
        const dataStats = await resStats.json();
        if (dataStats.data) {
            document.getElementById('dash-pending-reports').textContent = dataStats.data.pending;
        }
        
        // Load Revenue Chart
        const resRev = await apiFetch(`${API_BASE}/reports/revenue`, { headers: getHeaders() });
        const dataRev = await resRev.json();
        if (dataRev.data) {
            renderRevenueChart(dataRev.data);
        }
    } catch (e) {
        console.error('Lỗi tải dashboard', e);
    }
}

function renderRevenueChart(revenueData) {
    const sorted = [...revenueData].reverse().slice(-6); 
    const labels = sorted.map(item => `T${item.month}/${item.year}`);
    const data = sorted.map(item => item.revenue);

    const ctx = document.getElementById('revenue-chart').getContext('2d');
    if (revenueChart) revenueChart.destroy();
    
    revenueChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Doanh thu (VNĐ)',
                data: data,
                backgroundColor: '#6366F1',
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            scales: {
                y: { beginAtZero: true }
            }
        }
    });
}

document.getElementById('btn-dash-go-reports')?.addEventListener('click', () => {
    document.querySelector('.nav-item[data-target="admin-reports"]').click();
});

// BRANCHES
async function loadBranches() {
    const res = await apiFetch(`${API_BASE}/branches`, { headers: getHeaders() });
    const data = await res.json();
    const tbody = document.querySelector('#table-branches tbody');
    tbody.innerHTML = '';
    
    data.data.forEach(branch => {
        tbody.innerHTML += `
            <tr>
                <td><strong>${branch.name}</strong></td>
                <td>${branch.address || ''}</td>
                <td>
                    <button class="btn btn-outline btn-sm" onclick="openEditBranchModal('${branch.id}', '${branch.name}', '${branch.address || ''}')">Sửa</button>
                    <button class="btn btn-danger btn-sm" onclick="deleteBranch('${branch.id}')">Xóa</button>
                </td>
            </tr>
        `;
    });
}

document.getElementById('btn-show-add-branch').addEventListener('click', () => {
    document.getElementById('modal-add-branch').classList.remove('hidden');
});

document.getElementById('form-add-branch').addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        name: document.getElementById('add-branch-name').value,
        address: document.getElementById('add-branch-address').value
    };
    const res = await apiFetch(`${API_BASE}/branches`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (data.success) {
        showToast('Thêm cơ sở thành công');
        document.getElementById('modal-add-branch').classList.add('hidden');
        document.getElementById('form-add-branch').reset();
        loadBranches();
    } else showToast(data.message, 'error');
});

window.deleteBranch = async (id) => {
    if(!confirm('Xác nhận xóa cơ sở này?')) return;
    const res = await apiFetch(`${API_BASE}/branches/${id}`, { method: 'DELETE', headers: getHeaders() });
    const data = await res.json();
    if(data.success) {
        showToast('Đã xóa cơ sở');
        loadBranches();
    } else showToast(data.message, 'error');
};

window.openEditBranchModal = (id, name, address) => {
    document.getElementById('edit-branch-id').value = id;
    document.getElementById('edit-branch-name').value = name;
    document.getElementById('edit-branch-address').value = address;
    document.getElementById('modal-edit-branch').classList.remove('hidden');
};

document.getElementById('form-edit-branch').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('edit-branch-id').value;
    const payload = {
        name: document.getElementById('edit-branch-name').value,
        address: document.getElementById('edit-branch-address').value
    };
    const res = await apiFetch(`${API_BASE}/branches/${id}`, {
        method: 'PUT',
        headers: getHeaders(),
        body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (data.success) {
        showToast('Cập nhật cơ sở thành công');
        document.getElementById('modal-edit-branch').classList.add('hidden');
        loadBranches();
    } else showToast(data.message, 'error');
});

// ROOMS
document.getElementById('filter-room-status').addEventListener('change', loadRooms);

async function loadRooms() {
    const res = await apiFetch(`${API_BASE}/rooms`, { headers: getHeaders() });
    const data = await res.json();
    const tbody = document.querySelector('#table-rooms tbody');
    tbody.innerHTML = '';
    
    const filter = document.getElementById('filter-room-status').value;
    
    data.data.forEach(room => {
        if (filter === 'EMPTY' && room.isOccupied) return;
        if (filter === 'OCCUPIED' && !room.isOccupied) return;
        
        tbody.innerHTML += `
            <tr>
                <td><strong>${room.roomNumber}</strong></td>
                <td>${room.floor}</td>
                <td>Tháng: ${formatMoney(room.monthlyRent)}<br>Ngày: ${formatMoney(room.dailyRent)}</td>
                <td><span class="badge ${room.isOccupied ? 'TRUE' : 'FALSE'}">${room.isOccupied ? 'Có người' : 'Trống'}</span></td>
                <td>
                    <button class="btn btn-outline btn-sm" onclick="openEditRoomModal('${room.id}', '${room.branch.id}', '${room.roomNumber}', ${room.floor}, ${room.monthlyRent}, ${room.dailyRent || 0})">Sửa</button>
                    ${!room.isOccupied ? `<button class="btn btn-danger btn-sm" onclick="deleteRoom('${room.id}')">Xóa</button>` : ''}
                </td>
            </tr>
        `;
    });
}

document.getElementById('btn-show-add-room').addEventListener('click', async () => {
    // load branches
    const res = await apiFetch(`${API_BASE}/branches`, { headers: getHeaders() });
    const data = await res.json();
    const select = document.getElementById('add-room-branch');
    select.innerHTML = '';
    data.data.forEach(b => {
        select.innerHTML += `<option value="${b.id}">${b.name}</option>`;
    });
    document.getElementById('modal-add-room').classList.remove('hidden');
});

document.getElementById('form-add-room').addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        branchId: document.getElementById('add-room-branch').value,
        roomNumber: document.getElementById('add-room-number').value,
        floor: parseInt(document.getElementById('add-room-floor').value),
        monthlyRent: parseFormattedNumber(document.getElementById('add-room-rent').value),
        dailyRent: parseFormattedNumber(document.getElementById('add-room-daily-rent').value)
    };
    
    const res = await apiFetch(`${API_BASE}/rooms`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (data.success) {
        showToast('Thêm phòng thành công');
        document.getElementById('modal-add-room').classList.add('hidden');
        document.getElementById('form-add-room').reset();
        loadRooms();
    } else {
        showToast(data.message, 'error');
    }
});

window.deleteRoom = async (id) => {
    if(!confirm('Xác nhận xóa phòng này?')) return;
    const res = await apiFetch(`${API_BASE}/rooms/${id}`, { method: 'DELETE', headers: getHeaders() });
    const data = await res.json();
    if(data.success) {
        showToast('Đã xóa phòng');
        loadRooms();
    } else showToast(data.message, 'error');
};

window.openEditRoomModal = async (id, branchId, roomNumber, floor, rent, dailyRent) => {
    // load branches first
    const res = await apiFetch(`${API_BASE}/branches`, { headers: getHeaders() });
    const data = await res.json();
    const select = document.getElementById('edit-room-branch');
    select.innerHTML = '';
    data.data.forEach(b => {
        select.innerHTML += `<option value="${b.id}" ${b.id === branchId ? 'selected' : ''}>${b.name}</option>`;
    });

    document.getElementById('edit-room-id').value = id;
    document.getElementById('edit-room-number').value = roomNumber;
    document.getElementById('edit-room-floor').value = floor;
    document.getElementById('edit-room-rent').value = rent.toLocaleString('vi-VN');
    document.getElementById('edit-room-daily-rent').value = dailyRent.toLocaleString('vi-VN');
    document.getElementById('modal-edit-room').classList.remove('hidden');
};

document.getElementById('form-edit-room').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('edit-room-id').value;
    const payload = {
        branchId: document.getElementById('edit-room-branch').value,
        roomNumber: document.getElementById('edit-room-number').value,
        floor: parseInt(document.getElementById('edit-room-floor').value),
        monthlyRent: parseFormattedNumber(document.getElementById('edit-room-rent').value),
        dailyRent: parseFormattedNumber(document.getElementById('edit-room-daily-rent').value)
    };
    const res = await apiFetch(`${API_BASE}/rooms/${id}`, {
        method: 'PUT',
        headers: getHeaders(),
        body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (data.success) {
        showToast('Cập nhật phòng thành công');
        document.getElementById('modal-edit-room').classList.add('hidden');
        loadRooms();
    } else showToast(data.message, 'error');
});

// TENANTS
async function loadTenants() {
    const res = await apiFetch(`${API_BASE}/tenants`, { headers: getHeaders() });
    const data = await res.json();
    const tbody = document.querySelector('#table-tenants tbody');
    tbody.innerHTML = '';
    
    data.data.forEach(tenant => {
        tbody.innerHTML += `
            <tr>
                <td><strong>${tenant.room.roomNumber}</strong></td>
                <td>${tenant.user.fullName}</td>
                <td>${tenant.user.phone}</td>
                <td>${tenant.moveInDate}</td>
                <td>
                    <button class="btn btn-danger btn-sm" onclick="removeTenant('${tenant.id}')">Trả phòng</button>
                </td>
            </tr>
        `;
    });
}

document.getElementById('btn-show-add-tenant').addEventListener('click', async () => {
    // load empty rooms for select
    const res = await apiFetch(`${API_BASE}/rooms`, { headers: getHeaders() });
    const data = await res.json();
    const select = document.getElementById('add-tenant-room');
    select.innerHTML = '';
    data.data.filter(r => !r.isOccupied).forEach(r => {
        select.innerHTML += `<option value="${r.id}">${r.roomNumber}</option>`;
    });
    
    document.getElementById('modal-add-tenant').classList.remove('hidden');
});

document.getElementById('form-add-tenant').addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        phone: document.getElementById('add-tenant-phone').value,
        fullName: document.getElementById('add-tenant-name').value,
        roomId: document.getElementById('add-tenant-room').value
    };
    
    const res = await apiFetch(`${API_BASE}/tenants`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (data.success) {
        showToast('Thêm người thuê thành công');
        document.getElementById('modal-add-tenant').classList.add('hidden');
        document.getElementById('form-add-tenant').reset();
        loadTenants();
    } else {
        showToast(data.message, 'error');
    }
});

window.removeTenant = async (id) => {
    if(!confirm('Xác nhận trả phòng?')) return;
    const res = await apiFetch(`${API_BASE}/tenants/${id}`, { method: 'DELETE', headers: getHeaders() });
    const data = await res.json();
    if (data.success) {
        showToast('Trả phòng thành công');
        loadTenants();
    } else {
        showToast(data.message, 'error');
    }
}

// ==========================================
// ROOM MAP (Sơ đồ phòng)
// ==========================================
async function loadRoomMap() {
    const [resRooms, resTenants, resReports, resBookings] = await Promise.all([
        apiFetch(`${API_BASE}/rooms`, { headers: getHeaders() }),
        apiFetch(`${API_BASE}/tenants`, { headers: getHeaders() }),
        apiFetch(`${API_BASE}/reports`, { headers: getHeaders() }),
        apiFetch(`${API_BASE}/bookings`, { headers: getHeaders() })
    ]);
    
    const dataRooms = await resRooms.json();
    const dataTenants = await resTenants.json();
    const dataReports = await resReports.json();
    const dataBookings = await resBookings.json();
    
    if(!dataRooms.success) return;
    
    let rooms = dataRooms.data || [];
    const tenants = dataTenants.data || [];
    const reports = dataReports.data || [];
    const bookings = dataBookings.data || [];
    
    // Populate branch filter if empty
    const filterSelect = document.getElementById('room-map-branch-filter');
    if (filterSelect.options.length === 0) {
        const resBranches = await apiFetch(`${API_BASE}/branches`, { headers: getHeaders() });
        const dataBranches = await resBranches.json();
        filterSelect.innerHTML = '<option value="">-- Tất cả cơ sở --</option>';
        if (dataBranches.success) {
            dataBranches.data.forEach(b => {
                filterSelect.innerHTML += `<option value="${b.id}">${b.name}</option>`;
            });
        }
    }
    
    // Filter by branch
    const branchFilter = filterSelect.value;
    if(branchFilter) {
        rooms = rooms.filter(r => r.branch && r.branch.id === branchFilter);
    }
    
    // Render
    const grid = document.getElementById('room-map-grid');
    grid.innerHTML = '';
    
    rooms.forEach(room => {
        let status = 'empty';
        let tenantName = 'Trống';
        let subText = 'Sẵn sàng cho thuê';
        let amount = '';
        
        if (room.isOccupied) {
            // Check if it's a short-term booking first
            const activeBooking = bookings.find(b => b.room && b.room.id === room.id && b.status === 'CHECKED_IN');
            if (activeBooking) {
                status = 'booked';
                tenantName = activeBooking.guestName;
                subText = 'Ngày ra: ' + new Date(activeBooking.checkOutDate).toLocaleDateString('vi-VN');
            } else {
                status = 'occupied';
                const tenant = tenants.find(t => t.room && t.room.id === room.id);
                if (tenant) {
                    tenantName = tenant.fullName;
                    subText = tenant.phone;
                }
                
                // Check reports
                const pendingRep = reports.find(r => r.room && r.room.id === room.id && r.status === 'PENDING');
                if (pendingRep) {
                    status = 'pending';
                    subText = `T${pendingRep.reportMonth} chờ duyệt`;
                } else {
                    const unpaidRep = reports.find(r => r.room && r.room.id === room.id && r.status === 'APPROVED' && !r.isPaid);
                    if (unpaidRep) {
                        status = 'approved';
                        subText = `Nợ T${unpaidRep.reportMonth}`;
                        amount = formatMoney(unpaidRep.totalCost);
                    }
                }
            }
        }
        
        grid.innerHTML += `
            <div class="room-block status-${status}" onclick="alert('Phòng ${room.roomNumber}\\nKhách: ${tenantName}\\nTrạng thái: ${subText}')">
                <h4>${room.roomNumber}</h4>
                <p><strong>${tenantName}</strong></p>
                <p>${subText}</p>
                ${amount ? `<p class="text-primary font-bold mt-1">${amount}</p>` : ''}
            </div>
        `;
    });
}
document.getElementById('btn-refresh-room-map')?.addEventListener('click', loadRoomMap);


// REPORTS
async function loadPaidReports() {
    const res = await apiFetch(`${API_BASE}/reports/history`, { headers: getHeaders() });
    const data = await res.json();
    const tbody = document.querySelector('#table-history tbody');
    tbody.innerHTML = '';
    if (data.data) {
        data.data.forEach(rep => {
            let paymentDate = rep.paymentDate ? new Date(rep.paymentDate).toLocaleDateString('vi-VN') : '';
            tbody.innerHTML += `
                <tr>
                    <td>T${rep.reportMonth}/${rep.reportYear}</td>
                    <td><strong>${rep.room.roomNumber}</strong></td>
                    <td>${rep.tenant ? rep.tenant.fullName : ''}</td>
                    <td class="text-primary font-bold">${formatMoney(rep.totalCost)}</td>
                    <td>${paymentDate}</td>
                </tr>
            `;
        });
    }
}

document.getElementById('btn-refresh-history-admin')?.addEventListener('click', loadPaidReports);

async function loadPendingReports() {
    // Fetch all reports, then filter for PENDING and APPROVED (unpaid)
    const res = await apiFetch(`${API_BASE}/reports`, { headers: getHeaders() });
    const data = await res.json();
    const tbody = document.querySelector('#table-reports tbody');
    tbody.innerHTML = '';
    
    // Filter out PAID or REJECTED
    const pendingAndUnpaid = data.data.filter(r => r.status === 'PENDING' || (r.status === 'APPROVED' && !r.isPaid));
    window.currentPendingReports = pendingAndUnpaid; // cache for modal
    
    pendingAndUnpaid.forEach(rep => {
        let statusBadge = rep.status === 'PENDING' ? '<span class="badge PENDING">Chờ duyệt</span>' : '<span class="badge APPROVED">Chờ thu tiền</span>';
        
        tbody.innerHTML += `
            <tr>
                <td>${rep.reportMonth}/${rep.reportYear}</td>
                <td><strong>${rep.room.roomNumber}</strong></td>
                <td>${rep.waterOld} ➔ ${rep.waterNew}</td>
                <td>${rep.electricOld} ➔ ${rep.electricNew}</td>
                <td>${formatMoney(rep.totalCost)}</td>
                <td>
                    ${statusBadge}
                    <button class="btn btn-secondary btn-sm" onclick="openReviewModal('${rep.id}')">Chi tiết</button>
                </td>
            </tr>
        `;
    });
}
document.getElementById('btn-refresh-reports').addEventListener('click', loadPendingReports);

window.openReviewModal = async (reportId) => {
    const rep = window.currentPendingReports.find(r => r.id === reportId);
    if(!rep) return;
    
    document.getElementById('review-report-id').value = rep.id;
    document.getElementById('review-water').textContent = `${rep.waterOld} -> ${rep.waterNew} (Dùng: ${rep.waterUsage})`;
    document.getElementById('review-electric').textContent = `${rep.electricOld} -> ${rep.electricNew} (Dùng: ${rep.electricUsage})`;
    
    document.getElementById('review-water-img').src = await getPhotoUrl(rep.waterPhotoKey);
    document.getElementById('review-electric-img').src = await getPhotoUrl(rep.electricPhotoKey);
    document.getElementById('review-cost-room').textContent = formatMoney(rep.roomRent);
    document.getElementById('review-cost-water').textContent = formatMoney(rep.waterCost);
    document.getElementById('review-cost-electric').textContent = formatMoney(rep.electricCost);
    document.getElementById('review-cost-internet').textContent = formatMoney(rep.internetFee || 0);
    document.getElementById('review-cost-trash').textContent = formatMoney(rep.trashFee || 0);
    document.getElementById('review-cost-total').textContent = formatMoney(rep.totalCost);
    
    // Get presigned urls for photos
    document.getElementById('review-water-link').href = await getPhotoUrl(rep.waterPhotoKey);
    document.getElementById('review-electric-link').href = await getPhotoUrl(rep.electricPhotoKey);
    
    // Action buttons visibility
    const btnApprove = document.getElementById('btn-approve-report');
    const btnReject = document.getElementById('btn-reject-report');
    const btnMarkPaid = document.getElementById('btn-mark-paid');
    const btnPrint = document.getElementById('btn-print-invoice');
    
    if (rep.status === 'PENDING') {
        btnApprove.classList.remove('hidden');
        btnReject.classList.remove('hidden');
        btnMarkPaid.classList.add('hidden');
        btnPrint.classList.add('hidden');
    } else {
        btnApprove.classList.add('hidden');
        btnReject.classList.add('hidden');
        btnMarkPaid.classList.remove('hidden');
        btnPrint.classList.remove('hidden');
        btnPrint.onclick = () => openInvoiceModal(rep);
    }
    
    document.getElementById('modal-review-report').classList.remove('hidden');
}

window.openInvoiceModal = (rep) => {
    document.getElementById('modal-review-report').classList.add('hidden');
    document.getElementById('modal-invoice').classList.remove('hidden');
    
    document.getElementById('invoice-period').textContent = `Kỳ thanh toán: Tháng ${rep.reportMonth} / ${rep.reportYear}`;
    document.getElementById('invoice-tenant').textContent = rep.tenant ? rep.tenant.fullName : 'Trống';
    document.getElementById('invoice-room').textContent = rep.room.roomNumber;
    document.getElementById('invoice-date').textContent = new Date().toLocaleDateString('vi-VN');
    
    document.getElementById('invoice-room-rent').textContent = formatMoney(rep.roomRent);
    document.getElementById('invoice-electric-usage').textContent = `(${rep.electricOld} -> ${rep.electricNew} = ${rep.electricUsage} kWh)`;
    document.getElementById('invoice-electric-cost').textContent = formatMoney(rep.electricCost);
    
    document.getElementById('invoice-water-usage').textContent = `(${rep.waterOld} -> ${rep.waterNew} = ${rep.waterUsage} m3)`;
    document.getElementById('invoice-water-cost').textContent = formatMoney(rep.waterCost);
    
    document.getElementById('invoice-internet-fee').textContent = formatMoney(rep.internetFee || 0);
    document.getElementById('invoice-trash-fee').textContent = formatMoney(rep.trashFee || 0);
    
    document.getElementById('invoice-total').textContent = formatMoney(rep.totalCost);
    
    // QR Code for Invoice
    const qrContainer = document.getElementById('invoice-qr-container');
    if (window.currentPriceConfig && window.currentPriceConfig.bankName && window.currentPriceConfig.bankAccount) {
        const bankId = window.currentPriceConfig.bankName.trim();
        const accNo = window.currentPriceConfig.bankAccount.trim();
        const accName = encodeURIComponent(window.currentPriceConfig.accountName || '');
        const amount = Math.round(rep.totalCost);
        const info = encodeURIComponent(`Thu tien phong ${rep.room.roomNumber} T${rep.reportMonth}`);
        const qrUrl = `https://img.vietqr.io/image/${bankId}-${accNo}-compact2.png?amount=${amount}&addInfo=${info}&accountName=${accName}`;
        
        document.getElementById('invoice-qr').src = qrUrl;
        qrContainer.classList.remove('hidden');
    } else {
        qrContainer.classList.add('hidden');
    }
}

async function getPhotoUrl(key) {
    if (!key) return '#';
    const res = await apiFetch(`${API_BASE}/upload/url?key=${encodeURIComponent(key)}`, { headers: getHeaders() });
    const data = await res.json();
    return data.data.url;
}

document.getElementById('btn-approve-report').addEventListener('click', async () => {
    const id = document.getElementById('review-report-id').value;
    const res = await apiFetch(`${API_BASE}/reports/${id}/approve`, {
        method: 'PUT',
        headers: getHeaders()
    });
    const data = await res.json();
    if(data.success) {
        showToast('Đã duyệt báo cáo');
        document.getElementById('modal-review-report').classList.add('hidden');
        loadPendingReports();
    } else {
        showToast(data.message, 'error');
    }
});

document.getElementById('btn-reject-report').addEventListener('click', async () => {
    const id = document.getElementById('review-report-id').value;
    const reason = document.getElementById('review-reject-reason').value;
    if(!reason) return showToast('Vui lòng nhập lý do từ chối', 'error');
    
    const res = await apiFetch(`${API_BASE}/reports/${id}/reject`, {
        method: 'PUT',
        headers: getHeaders(),
        body: JSON.stringify({ rejectReason: reason })
    });
    const data = await res.json();
    if(data.success) {
        showToast('Đã từ chối báo cáo');
        document.getElementById('modal-review-report').classList.add('hidden');
        loadPendingReports();
    } else {
        showToast(data.message, 'error');
    }
});

document.getElementById('btn-mark-paid').addEventListener('click', async () => {
    const id = document.getElementById('review-report-id').value;
    const res = await apiFetch(`${API_BASE}/reports/${id}/pay`, {
        method: 'PUT',
        headers: getHeaders()
    });
    const data = await res.json();
    if(data.success) {
        showToast('Đã xác nhận thu tiền');
        document.getElementById('modal-review-report').classList.add('hidden');
        loadPendingReports();
    } else {
        showToast(data.message, 'error');
    }
});

// SETTINGS
window.currentPriceConfig = {};
async function loadSettings() {
    const res = await apiFetch(`${API_BASE}/prices`, { headers: getHeaders() });
    const data = await res.json();
    if(data.data) {
        window.currentPriceConfig = data.data;
        document.getElementById('setting-water').value = data.data.waterPricePerUnit ? data.data.waterPricePerUnit.toLocaleString('vi-VN') : '';
        document.getElementById('setting-electric').value = data.data.electricPricePerUnit ? data.data.electricPricePerUnit.toLocaleString('vi-VN') : '';
        document.getElementById('setting-internet').value = data.data.internetFee ? data.data.internetFee.toLocaleString('vi-VN') : '0';
        document.getElementById('setting-trash').value = data.data.trashFee ? data.data.trashFee.toLocaleString('vi-VN') : '0';
        
        document.getElementById('setting-bank-name').value = data.data.bankName || '';
        document.getElementById('setting-bank-account').value = data.data.bankAccount || '';
        document.getElementById('setting-account-name').value = data.data.accountName || '';
    }
}

document.getElementById('form-settings').addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        ...window.currentPriceConfig,
        waterPricePerUnit: parseFormattedNumber(document.getElementById('setting-water').value),
        electricPricePerUnit: parseFormattedNumber(document.getElementById('setting-electric').value),
        internetFee: parseFormattedNumber(document.getElementById('setting-internet').value),
        trashFee: parseFormattedNumber(document.getElementById('setting-trash').value)
    };
    saveSettingsAPI(payload);
});

document.getElementById('form-settings-bank').addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        ...window.currentPriceConfig,
        bankName: document.getElementById('setting-bank-name').value,
        bankAccount: document.getElementById('setting-bank-account').value,
        accountName: document.getElementById('setting-account-name').value
    };
    saveSettingsAPI(payload);
});

async function saveSettingsAPI(payload) {
    const res = await apiFetch(`${API_BASE}/prices`, {
        method: 'PUT',
        headers: getHeaders(),
        body: JSON.stringify(payload)
    });
    const data = await res.json();
    if(data.success) {
        showToast('Lưu thông tin thành công');
        window.currentPriceConfig = data.data;
    }
    else showToast(data.message, 'error');
}

/* =========================================================
   USER APP
========================================================= */
function initUserApp() {
    document.getElementById('user-name').textContent = currentUser.fullName;
    document.getElementById('user-room-number').textContent = currentUser.roomNumber || 'Chưa gán';
    
    // set current month
    const now = new Date();
    document.getElementById('report-month').value = now.getMonth() + 1;
    document.getElementById('report-year').value = now.getFullYear();

    if(!currentUser.roomId) {
        document.getElementById('btn-submit-report').disabled = true;
        showToast('Bạn chưa được gán phòng nào', 'error');
    }
    
    // fetch settings to render QR
    apiFetch(`${API_BASE}/prices`, { headers: getHeaders() })
        .then(res => res.json())
        .then(data => {
            if (data.data) {
                window.currentPriceConfig = data.data;
            }
            loadUserHistory();
        })
        .catch(err => {
            console.error(err);
            loadUserHistory();
        });
}

document.getElementById('btn-refresh-history').addEventListener('click', loadUserHistory);

async function loadUserHistory() {
    const res = await apiFetch(`${API_BASE}/reports/my`, { headers: getHeaders() });
    const data = await res.json();
    
    const list = document.getElementById('user-history-list');
    list.innerHTML = '';
    
    if(!data.data || data.data.length === 0) {
        list.innerHTML = '<p class="text-muted">Chưa có lịch sử báo cáo nào.</p>';
        return;
    }
    
    data.data.forEach(rep => {
        let statusText = rep.status === 'PENDING' ? 'Chờ duyệt' : (rep.isPaid ? 'Đã thanh toán' : 'Chờ thu tiền');
        let statusClass = rep.status === 'PENDING' ? 'PENDING' : (rep.isPaid ? 'APPROVED' : 'PENDING');
        if (rep.status === 'REJECTED') { statusText = 'Từ chối'; statusClass = 'REJECTED'; }
        
        let qrCodeHtml = '';
        if (rep.status === 'APPROVED' && !rep.isPaid && window.currentPriceConfig && window.currentPriceConfig.bankName && window.currentPriceConfig.bankAccount) {
            const bankId = window.currentPriceConfig.bankName.trim();
            const accNo = window.currentPriceConfig.bankAccount.trim();
            const accName = encodeURIComponent(window.currentPriceConfig.accountName || '');
            const amount = Math.round(rep.totalCost);
            const info = encodeURIComponent(`Thu tien phong ${rep.room.roomNumber} T${rep.reportMonth}`);
            
            const qrUrl = `https://img.vietqr.io/image/${bankId}-${accNo}-compact2.png?amount=${amount}&addInfo=${info}&accountName=${accName}`;
            qrCodeHtml = `
                <div class="mt-4 text-center p-4" style="background: white; border-radius: 8px; border: 1px dashed var(--primary)">
                    <p class="font-bold mb-2">Quét mã VietQR để thanh toán</p>
                    <img src="${qrUrl}" alt="VietQR" style="max-width: 200px; margin: 0 auto; display: block; border-radius: 8px;">
                </div>
            `;
        }
        
        list.innerHTML += `
            <div class="card" style="margin-bottom: 0;">
                <div class="flex justify-between items-center mb-2">
                    <h4 style="font-size: 1.1rem">Tháng ${rep.reportMonth}/${rep.reportYear}</h4>
                    <span class="badge ${statusClass}">${statusText}</span>
                </div>
                <div class="grid-2 text-sm mb-4">
                    <div>
                        <span class="text-muted">Nước:</span> ${rep.waterOld} ➔ ${rep.waterNew} (${rep.waterUsage} m3)
                    </div>
                    <div>
                        <span class="text-muted">Điện:</span> ${rep.electricOld} ➔ ${rep.electricNew} (${rep.electricUsage} kWh)
                    </div>
                    ${rep.internetFee > 0 ? `<div><span class="text-muted">Mạng:</span> ${formatMoney(rep.internetFee)}</div>` : ''}
                    ${rep.trashFee > 0 ? `<div><span class="text-muted">Rác:</span> ${formatMoney(rep.trashFee)}</div>` : ''}
                </div>
                <div class="flex justify-between items-center" style="border-top: 1px solid var(--border-color); padding-top: 0.5rem">
                    <span class="text-muted">Tổng thanh toán:</span>
                    <strong style="font-size: 1.25rem; color: var(--primary)">${formatMoney(rep.totalCost)}</strong>
                </div>
                ${rep.status === 'REJECTED' ? `<div class="mt-2 text-sm text-danger"><strong>Lý do từ chối:</strong> ${rep.rejectReason}</div>` : ''}
                ${qrCodeHtml}
            </div>
        `;
    });
}

// Upload file helper
async function uploadFile(file) {
    const fd = new FormData();
    fd.append('file', file);
    const res = await apiFetch(`${API_BASE}/upload/photo`, {
        method: 'POST',
        headers: { 'X-User-Id': currentUser.userId },
        body: fd
    });
    const data = await res.json();
    if(data.success) return data.data.objectKey;
    throw new Error(data.message);
}

document.getElementById('form-submit-report').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('btn-submit-report');
    
    try {
        btn.disabled = true;
        btn.textContent = 'Đang upload ảnh...';
        
        const waterFile = document.getElementById('report-water-file').files[0];
        const electricFile = document.getElementById('report-electric-file').files[0];
        
        const waterKey = await uploadFile(waterFile);
        const electricKey = await uploadFile(electricFile);
        
        btn.textContent = 'Đang gửi báo cáo...';
        
        const waterOld = parseFormattedNumber(document.getElementById('report-water-old').value);
        const waterNew = parseFormattedNumber(document.getElementById('report-water-new').value);
        const electricOld = parseFormattedNumber(document.getElementById('report-electric-old').value);
        const electricNew = parseFormattedNumber(document.getElementById('report-electric-new').value);
        
        if (waterNew < waterOld) {
            btn.disabled = false;
            btn.textContent = 'Gửi báo cáo';
            return showToast('Chỉ số nước mới không được nhỏ hơn số cũ', 'error');
        }
        if (electricNew < electricOld) {
            btn.disabled = false;
            btn.textContent = 'Gửi báo cáo';
            return showToast('Chỉ số điện mới không được nhỏ hơn số cũ', 'error');
        }
        
        const payload = {
            reportMonth: parseInt(document.getElementById('report-month').value),
            reportYear: parseInt(document.getElementById('report-year').value),
            waterOld: waterOld,
            waterNew: waterNew,
            waterPhotoKey: waterKey,
            electricOld: electricOld,
            electricNew: electricNew,
            electricPhotoKey: electricKey,
            note: document.getElementById('report-note').value
        };
        
        const res = await apiFetch(`${API_BASE}/reports`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        
        if (data.success) {
            showToast('Gửi báo cáo thành công!');
            document.getElementById('form-submit-report').reset();
            // reset date
            const now = new Date();
            document.getElementById('report-month').value = now.getMonth() + 1;
            document.getElementById('report-year').value = now.getFullYear();
            loadUserHistory();
        } else {
            showToast(data.message, 'error');
        }
    } catch (err) {
        showToast(err.message || 'Có lỗi xảy ra', 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Gửi báo cáo';
    }
});

// ==========================================
// BOOKINGS (Ngắn hạn)
// ==========================================
async function loadBookings() {
    const res = await apiFetch(`${API_BASE}/bookings`, { headers: getHeaders() });
    const data = await res.json();
    const tbody = document.querySelector('#table-bookings tbody');
    tbody.innerHTML = '';
    
    if (data.success && data.data) {
        data.data.forEach(b => {
            const ci = new Date(b.checkInDate).toLocaleDateString('vi-VN');
            const co = new Date(b.checkOutDate).toLocaleDateString('vi-VN');
            
            let statusBadge = '';
            let actions = '';
            
            if (b.status === 'PENDING') {
                statusBadge = '<span class="badge FALSE">Chờ nhận phòng</span>';
                actions = `
                    <button class="btn btn-primary btn-sm" onclick="bookingAction('${b.id}', 'checkin')">Nhận phòng</button>
                    <button class="btn btn-danger btn-sm" onclick="bookingAction('${b.id}', 'cancel')">Hủy</button>
                `;
            } else if (b.status === 'CHECKED_IN') {
                statusBadge = '<span class="badge TRUE">Đang ở</span>';
                actions = `
                    <button class="btn btn-secondary btn-sm" onclick="bookingAction('${b.id}', 'checkout')">Trả phòng</button>
                    <button class="btn btn-danger btn-sm" onclick="bookingAction('${b.id}', 'cancel')">Hủy</button>
                `;
            } else if (b.status === 'CHECKED_OUT') {
                statusBadge = '<span class="badge" style="background: var(--success); color: white;">Đã trả</span>';
                actions = `<span class="text-muted">Hoàn thành</span>`;
            } else if (b.status === 'CANCELLED') {
                statusBadge = '<span class="badge" style="background: #999; color: white;">Đã hủy</span>';
                actions = '';
            }

            tbody.innerHTML += `
                <tr>
                    <td><strong>${b.room.roomNumber}</strong></td>
                    <td>${b.guestName}<br><small>${b.guestPhone || ''}</small></td>
                    <td>${ci} - ${co}</td>
                    <td class="font-bold text-primary">${formatMoney(b.totalPrice)}</td>
                    <td>${statusBadge}</td>
                    <td>${actions}</td>
                </tr>
            `;
        });
    }
}

window.openAddBookingModal = async () => {
    const res = await apiFetch(`${API_BASE}/rooms`, { headers: getHeaders() });
    const data = await res.json();
    const select = document.getElementById('booking-room');
    select.innerHTML = '';
    
    if (data.success && data.data) {
        data.data.filter(r => !r.isOccupied).forEach(r => {
            select.innerHTML += `<option value="${r.id}">Phòng ${r.roomNumber} - ${r.branch.name} (${formatMoney(r.dailyRent)}/ngày)</option>`;
        });
    }
    
    document.getElementById('form-add-booking').reset();
    document.getElementById('modal-add-booking').classList.remove('hidden');
}

document.getElementById('form-add-booking')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        roomId: document.getElementById('booking-room').value,
        guestName: document.getElementById('booking-guest-name').value,
        guestPhone: document.getElementById('booking-guest-phone').value,
        guestIdentity: document.getElementById('booking-guest-identity').value,
        checkInDate: document.getElementById('booking-check-in').value,
        checkOutDate: document.getElementById('booking-check-out').value
    };
    
    const res = await apiFetch(`${API_BASE}/bookings`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(payload)
    });
    
    const data = await res.json();
    if (data.success) {
        showToast('Tạo booking thành công');
        document.getElementById('modal-add-booking').classList.add('hidden');
        loadBookings();
        loadRoomMap(); // Update room map automatically
    } else {
        showToast(data.message, 'error');
    }
});

window.bookingAction = async (id, action) => {
    if (action === 'cancel' && !confirm('Bạn có chắc chắn muốn hủy booking này?')) return;
    if (action === 'checkout' && !confirm('Xác nhận khách trả phòng và thanh toán?')) return;
    
    const res = await apiFetch(`${API_BASE}/bookings/${id}/${action}`, {
        method: 'PUT',
        headers: getHeaders()
    });
    
    const data = await res.json();
    if (data.success) {
        showToast(data.message);
        loadBookings();
        loadRoomMap(); // Update map after check-in / check-out
    } else {
        showToast(data.message, 'error');
    }
}
