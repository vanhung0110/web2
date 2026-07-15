const API_BASE = 'http://localhost:8888/api';

async function fetchJSON(path, method = 'GET', body = null, userId = null) {
    const headers = { 'Content-Type': 'application/json' };
    if (userId) headers['X-User-Id'] = userId;
    
    const options = { method, headers };
    if (body) options.body = JSON.stringify(body);
    
    const res = await fetch(`${API_BASE}${path}`, options);
    const text = await res.text();
    try {
        const json = JSON.parse(text);
        return { status: res.status, data: json };
    } catch(e) {
        return { status: res.status, text };
    }
}

async function runTests() {
    let adminId = null;
    let branchId = null;
    let roomId = null;
    let tenantId = null;
    let tenantUserId = null;
    let reportId = null;
    
    console.log('--- STARTING TESTS ---');
    
    // 1. Admin Login
    console.log('[1] Admin Login');
    let res = await fetchJSON('/auth/login', 'POST', { phone: '0962750432' });
    if (!res.data.success) { console.error('Admin login failed:', res.data); return; }
    adminId = res.data.data.userId;
    console.log('Admin login OK. Admin ID:', adminId);

    // 2. Configure Prices
    console.log('[2] Configure Prices & Bank');
    res = await fetchJSON('/prices', 'PUT', {
        waterPricePerUnit: 15000,
        electricPricePerUnit: 3500,
        bankName: 'MB',
        bankAccount: '123456789',
        accountName: 'TEST ADMIN'
    }, adminId);
    if (!res.data.success) { console.error('Config price failed:', res.data); return; }
    console.log('Config price OK.');

    // 3. Create Branch
    console.log('[3] Create Branch');
    res = await fetchJSON('/branches', 'POST', {
        name: 'Test Branch ' + Date.now(),
        address: 'Test Address'
    }, adminId);
    if (!res.data.success) { console.error('Create branch failed:', res.data); return; }
    branchId = res.data.data.id;
    console.log('Create branch OK. Branch ID:', branchId);

    // 4. Create Room
    console.log('[4] Create Room');
    res = await fetchJSON('/rooms', 'POST', {
        branchId: branchId,
        roomNumber: 'T102',
        floor: 1,
        monthlyRent: 2000000
    }, adminId);
    if (!res.data.success) { console.error('Create room failed:', res.data); return; }
    roomId = res.data.data.id;
    console.log('Create room OK. Room ID:', roomId);

    // 5. Create Tenant (Register User)
    console.log('[5] Create Tenant');
    res = await fetchJSON('/tenants', 'POST', {
        phone: '0123456789',
        fullName: 'Test User',
        roomId: roomId
    }, adminId);
    if (!res.data.success) { console.error('Create tenant failed:', res.data); return; }
    tenantId = res.data.data.id;
    console.log('Create tenant OK. Tenant ID:', tenantId);

    // 6. User Login
    console.log('[6] User Login');
    res = await fetchJSON('/auth/login', 'POST', { phone: '0123456789' });
    if (!res.data.success) { console.error('User login failed:', res.data); return; }
    tenantUserId = res.data.data.userId;
    console.log('User login OK. User ID:', tenantUserId);

    // 7. User Submit Report
    console.log('[7] Submit Report');
    res = await fetchJSON('/reports', 'POST', {
        reportMonth: 7,
        reportYear: 2026,
        waterOld: 10,
        waterNew: 20,
        waterPhotoKey: 'dummy.jpg',
        electricOld: 100,
        electricNew: 150,
        electricPhotoKey: 'dummy2.jpg',
        note: 'Test note'
    }, tenantUserId);
    if (!res.data.success) { console.error('Submit report failed:', res.data); return; }
    reportId = res.data.data.id;
    console.log('Submit report OK. Report ID:', reportId);

    // 8. Admin Approve Report
    console.log('[8] Admin Approve Report');
    res = await fetchJSON(`/reports/${reportId}/approve`, 'PUT', {}, adminId);
    if (!res.data.success) { console.error('Approve report failed:', res.data); return; }
    console.log('Approve report OK.');

    // 9. Admin Mark Paid
    console.log('[9] Admin Mark Paid');
    res = await fetchJSON(`/reports/${reportId}/pay`, 'PUT', {}, adminId);
    if (!res.data.success) { console.error('Mark paid failed:', res.data); return; }
    console.log('Mark paid OK.');

    // 10. Clean up
    console.log('[10] Cleanup');
    console.log('Deleting Tenant...');
    res = await fetchJSON(`/tenants/${tenantId}`, 'DELETE', {}, adminId);
    console.log('Delete Tenant:', res.data.success);
    
    console.log('Deleting Room...');
    res = await fetchJSON(`/rooms/${roomId}`, 'DELETE', {}, adminId);
    console.log('Delete Room:', res.data.success, res.data.message);
    
    console.log('Deleting Branch...');
    res = await fetchJSON(`/branches/${branchId}`, 'DELETE', {}, adminId);
    console.log('Delete Branch:', res.data.success, res.data.message);

    console.log('--- ALL TESTS PASSED SUCCESSFULLY ---');
}

runTests();
