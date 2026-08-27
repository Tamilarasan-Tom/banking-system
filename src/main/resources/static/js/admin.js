// Admin Dashboard Logic

let allUsers = [];
let allAccounts = [];
let allTransactions = [];
let allLoans = [];

document.addEventListener('DOMContentLoaded', () => {
    if (!checkAuth('ROLE_ADMIN')) return;

    const user = getAuthUser();
    document.getElementById('adminNameLabel').textContent = user.fullName || user.email;

    initAdminDashboard();
});

function switchAdminTab(tabName, linkElement = null) {
    document.querySelectorAll('.tab-section').forEach(s => s.style.display = 'none');
    const target = document.getElementById(`tab-${tabName}`);
    if (target) target.style.display = 'block';

    document.querySelectorAll('.sidebar-link').forEach(l => l.classList.remove('active'));
    if (linkElement) {
        linkElement.classList.add('active');
    } else {
        const found = Array.from(document.querySelectorAll('.sidebar-link')).find(l =>
            l.getAttribute('onclick') && l.getAttribute('onclick').includes(`'${tabName}'`)
        );
        if (found) found.classList.add('active');
    }

    if (tabName === 'dashboard') loadDashboard();
    else if (tabName === 'users') loadUsers();
    else if (tabName === 'accounts') loadAccounts();
    else if (tabName === 'transactions') loadTransactions();
    else if (tabName === 'loans') loadLoans();
}

async function initAdminDashboard() {
    await loadDashboard();
}

async function loadDashboard() {
    await loadStats();
    await loadPendingLoansForDashboard();
}

async function loadStats() {
    try {
        const res = await fetch('/api/admin/stats', { headers: authHeaders() });
        if (res.ok) {
            const s = await res.json();
            document.getElementById('stat-totalUsers').textContent = s.totalUsers;
            document.getElementById('stat-totalAccounts').textContent = s.totalAccounts;
            document.getElementById('stat-activeAccounts').textContent = s.activeAccounts;
            document.getElementById('stat-totalTransactions').textContent = s.totalTransactions;
            document.getElementById('stat-pendingLoans').textContent = s.pendingLoans;
            document.getElementById('stat-approvedLoans').textContent = s.approvedLoans;
            document.getElementById('stat-rejectedLoans').textContent = s.rejectedLoans;
            document.getElementById('stat-transferAmount').textContent = '$' + parseFloat(s.totalTransferAmount || 0).toFixed(2);
        } else if (res.status === 401 || res.status === 403) {
            logout();
        }
    } catch (err) {
        console.error('Error loading stats:', err);
        showToast('Failed to load statistics.', 'error');
    }
}

async function loadPendingLoansForDashboard() {
    try {
        const res = await fetch('/api/admin/loans', { headers: authHeaders() });
        if (res.ok) {
            const loans = await res.json();
            const pending = loans.filter(l => l.loanStatus === 'PENDING');
            const tbody = document.getElementById('dashboardPendingLoansTable');
            if (!tbody) return;
            if (pending.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:var(--color-text-secondary)">No pending loan applications.</td></tr>`;
                return;
            }
            tbody.innerHTML = '';
            pending.slice(0, 5).forEach(l => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${new Date(l.applicationDate).toLocaleDateString()}</td>
                    <td><strong>${l.user.fullName}</strong><br><span style="font-size:0.8rem;color:var(--color-text-secondary)">${l.user.email}</span></td>
                    <td>${l.loanType}</td>
                    <td>$${parseFloat(l.requestedAmount).toFixed(2)}</td>
                    <td><span class="badge badge-warning">PENDING</span></td>
                    <td>
                        <button class="btn btn-sm btn-success" onclick="openLoanDecision(${l.id},'approve')">✓ Approve</button>
                        <button class="btn btn-sm btn-danger" onclick="openLoanDecision(${l.id},'reject')">✗ Reject</button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (err) {
        console.error('Error loading pending loans:', err);
    }
}

async function loadUsers() {
    try {
        const res = await fetch('/api/admin/users', { headers: authHeaders() });
        if (res.ok) {
            allUsers = await res.json();
            renderUsersTable(allUsers);
        }
    } catch (err) {
        console.error('Error loading users:', err);
        showToast('Failed to load users.', 'error');
    }
}

function renderUsersTable(users) {
    const tbody = document.getElementById('usersTable');
    if (!tbody) return;
    if (users.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:var(--color-text-secondary)">No users found.</td></tr>`;
        return;
    }
    tbody.innerHTML = '';
    users.forEach(u => {
        const statusClass = u.status === 'ACTIVE' ? 'badge-success' : 'badge-danger';
        const roleClass = u.role === 'ROLE_ADMIN' ? 'badge-info' : 'badge-warning';
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${u.id}</td>
            <td><strong>${u.fullName}</strong></td>
            <td>${u.email}</td>
            <td>${u.phone}</td>
            <td><span class="badge ${roleClass}">${u.role.replace('ROLE_', '')}</span></td>
            <td><span class="badge ${statusClass}">${u.status}</span></td>
            <td>${new Date(u.createdAt).toLocaleDateString()}</td>
            <td>
                ${u.role !== 'ROLE_ADMIN' ? `<button class="btn btn-sm btn-secondary" onclick="openStatusModal('user', ${u.id}, '${u.status}')">Change Status</button>` : '<span style="color:var(--color-text-muted)">Protected</span>'}
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function filterUsers(query) {
    const q = query.toLowerCase();
    const filtered = allUsers.filter(u =>
        u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)
    );
    renderUsersTable(filtered);
}

async function loadAccounts() {
    try {
        const res = await fetch('/api/admin/accounts', { headers: authHeaders() });
        if (res.ok) {
            allAccounts = await res.json();
            renderAccountsTable(allAccounts);
        }
    } catch (err) {
        console.error('Error loading accounts:', err);
    }
}

function renderAccountsTable(accounts) {
    const tbody = document.getElementById('accountsTable');
    if (!tbody) return;
    if (accounts.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;color:var(--color-text-secondary)">No accounts found.</td></tr>`;
        return;
    }
    tbody.innerHTML = '';
    accounts.forEach(a => {
        const statusClass = a.status === 'ACTIVE' ? 'badge-success' : (a.status === 'BLOCKED' ? 'badge-warning' : 'badge-danger');
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>${a.accountNumber}</strong></td>
            <td>${a.user.fullName}<br><span style="font-size:0.8rem;color:var(--color-text-secondary)">${a.user.email}</span></td>
            <td>${a.accountType}</td>
            <td><strong>$${parseFloat(a.balance).toFixed(2)}</strong></td>
            <td><span class="badge ${statusClass}">${a.status}</span></td>
            <td>${new Date(a.createdAt).toLocaleDateString()}</td>
            <td><button class="btn btn-sm btn-secondary" onclick="openStatusModal('account', ${a.id}, '${a.status}')">Change Status</button></td>
        `;
        tbody.appendChild(tr);
    });
}

function filterAccounts(query) {
    const q = query.toLowerCase();
    const filtered = allAccounts.filter(a => a.accountNumber.toLowerCase().includes(q));
    renderAccountsTable(filtered);
}

async function loadTransactions() {
    try {
        const res = await fetch('/api/admin/transactions', { headers: authHeaders() });
        if (res.ok) {
            allTransactions = await res.json();
            renderTransactionsTable(allTransactions);
        }
    } catch (err) {
        console.error('Error loading transactions:', err);
    }
}

function renderTransactionsTable(transactions) {
    const tbody = document.getElementById('transactionsTable');
    if (!tbody) return;
    if (transactions.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;color:var(--color-text-secondary)">No transactions found.</td></tr>`;
        return;
    }
    tbody.innerHTML = '';
    transactions.forEach(t => {
        const isDebit = t.transactionType === 'WITHDRAW' || (t.transactionType === 'TRANSFER' && parseFloat(t.previousBalance) > parseFloat(t.newBalance));
        const amtColor = isDebit ? 'var(--color-danger)' : 'var(--color-success)';
        const amtPrefix = isDebit ? '-' : '+';
        const typeClass = t.transactionType === 'DEPOSIT' ? 'badge-success' : (t.transactionType === 'WITHDRAW' ? 'badge-danger' : 'badge-info');
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${new Date(t.transactionDate).toLocaleString()}</td>
            <td><strong>${t.account.accountNumber}</strong></td>
            <td>${t.account.user.fullName}</td>
            <td><span class="badge ${typeClass}">${t.transactionType}</span></td>
            <td style="color:${amtColor};font-weight:bold;">${amtPrefix}$${parseFloat(t.amount).toFixed(2)}</td>
            <td>$${parseFloat(t.newBalance).toFixed(2)}</td>
            <td><span class="badge badge-success">${t.status}</span></td>
        `;
        tbody.appendChild(tr);
    });
}

function filterTransactions(type) {
    const filtered = type === 'ALL' ? allTransactions : allTransactions.filter(t => t.transactionType === type);
    renderTransactionsTable(filtered);
}

async function loadLoans() {
    try {
        const res = await fetch('/api/admin/loans', { headers: authHeaders() });
        if (res.ok) {
            allLoans = await res.json();
            renderLoansTable(allLoans);
        }
    } catch (err) {
        console.error('Error loading loans:', err);
    }
}

function renderLoansTable(loans) {
    const tbody = document.getElementById('loansTable');
    if (!tbody) return;
    if (loans.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:var(--color-text-secondary)">No loan applications found.</td></tr>`;
        return;
    }
    tbody.innerHTML = '';
    loans.forEach(l => {
        const statusClass = l.loanStatus === 'APPROVED' ? 'badge-success' : (l.loanStatus === 'PENDING' ? 'badge-warning' : (l.loanStatus === 'REJECTED' ? 'badge-danger' : 'badge-info'));
        const isPending = l.loanStatus === 'PENDING';
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${new Date(l.applicationDate).toLocaleDateString()}</td>
            <td><strong>${l.user.fullName}</strong><br><span style="font-size:0.8rem;color:var(--color-text-secondary)">${l.user.email}</span></td>
            <td>${l.loanType}</td>
            <td>$${parseFloat(l.requestedAmount).toFixed(2)}</td>
            <td>${parseFloat(l.interestRate).toFixed(1)}%</td>
            <td>${l.loanDuration} mo.</td>
            <td><span class="badge ${statusClass}">${l.loanStatus}</span></td>
            <td>
                ${isPending
                ? `<button class="btn btn-sm btn-success" onclick="openLoanDecision(${l.id},'approve')">Approve</button>
                       <button class="btn btn-sm btn-danger" onclick="openLoanDecision(${l.id},'reject')" style="margin-left:4px;">Reject</button>`
                : `<span style="color:var(--color-text-muted);font-size:0.85rem">${l.adminRemarks || 'Decided'}</span>`
            }
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function filterLoans(status) {
    const filtered = status === 'ALL' ? allLoans : allLoans.filter(l => l.loanStatus === status);
    renderLoansTable(filtered);
}

// Loan Decision Modal
function openLoanDecision(loanId, action) {
    document.getElementById('loanModalId').value = loanId;
    document.getElementById('loanModalAction').value = action;
    document.getElementById('loanModalTitle').textContent = action === 'approve' ? '✅ Approve Loan Application' : '❌ Reject Loan Application';
    document.getElementById('loanModalSubmitBtn').className = `btn ${action === 'approve' ? 'btn-success' : 'btn-danger'}`;
    document.getElementById('loanModalSubmitBtn').textContent = action === 'approve' ? 'Confirm Approval' : 'Confirm Rejection';
    document.getElementById('loanModalRemarks').value = '';
    document.getElementById('loanDecisionModal').classList.add('active');
}

function closeLoanDecisionModal() {
    document.getElementById('loanDecisionModal').classList.remove('active');
}

async function submitLoanDecision() {
    const id = document.getElementById('loanModalId').value;
    const action = document.getElementById('loanModalAction').value;
    const adminRemarks = document.getElementById('loanModalRemarks').value || (action === 'approve' ? 'Approved by administrator.' : 'Rejected by administrator.');

    const endpoint = `/api/admin/loans/${id}/${action}`;
    try {
        const res = await fetch(endpoint, {
            method: 'PUT',
            headers: authHeaders(),
            body: JSON.stringify({ adminRemarks })
        });

        if (res.ok) {
            showToast(`Loan application ${action === 'approve' ? 'approved' : 'rejected'} successfully.`, 'success');
            closeLoanDecisionModal();
            loadLoans();
            loadDashboard();
        } else {
            const data = await res.json();
            showToast(data.message || 'Failed to process loan decision.', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Network error.', 'error');
    }
}

// User / Account Status Modal
function openStatusModal(type, id, currentStatus) {
    document.getElementById('statusModalId').value = id;
    document.getElementById('statusModalType').value = type;
    document.getElementById('statusModalTitle').textContent = type === 'user' ? '👤 Update User Status' : '🏦 Update Account Status';

    const select = document.getElementById('statusModalValue');
    select.innerHTML = '';

    const statuses = type === 'user' ? ['ACTIVE', 'BLOCKED'] : ['ACTIVE', 'BLOCKED', 'CLOSED'];
    statuses.forEach(s => {
        const opt = document.createElement('option');
        opt.value = s;
        opt.textContent = s;
        if (s === currentStatus) opt.selected = true;
        select.appendChild(opt);
    });

    document.getElementById('statusModal').classList.add('active');
}

function closeStatusModal() {
    document.getElementById('statusModal').classList.remove('active');
}

async function submitStatusChange() {
    const id = document.getElementById('statusModalId').value;
    const type = document.getElementById('statusModalType').value;
    const status = document.getElementById('statusModalValue').value;

    const endpoint = type === 'user' ? `/api/admin/users/${id}/status` : `/api/admin/accounts/${id}/status`;

    try {
        const res = await fetch(endpoint, {
            method: 'PUT',
            headers: authHeaders(),
            body: JSON.stringify({ status })
        });

        if (res.ok) {
            showToast(`${type === 'user' ? 'User' : 'Account'} status updated to ${status}.`, 'success');
            closeStatusModal();
            if (type === 'user') loadUsers();
            else loadAccounts();
            loadStats();
        } else {
            const data = await res.json();
            showToast(data.message || 'Failed to update status.', 'error');
        }
    } catch (err) {
        console.error(err);
        showToast('Network error.', 'error');
    }
}
document.getElementById("totalUsersCard").addEventListener("click", function () {
    switchAdminTab("users");
});
document.getElementById("activeAccountsCard").addEventListener("click", function () {
    switchAdminTab("accounts");
});
document.getElementById("totalTransactionsCard").addEventListener("click", function () {
    switchAdminTab("transactions");
});
document.getElementById("pendingLoansCard").addEventListener("click", function () {
    switchAdminTab("loans");
});
document.getElementById("totalAccountsCard").addEventListener("click", function () {
    switchAdminTab("accounts");
});
document.getElementById("approvedLoansCard").addEventListener("click", function () {
    switchAdminTab("loans");
});
document.getElementById("rejectedLoansCard").addEventListener("click", function () {
    switchAdminTab("loans");
});
document.getElementById("transferVolumeCard").addEventListener("click", function () {
    switchAdminTab("transactions");
});