// Customer Dashboard Logic

let cachedUser = null;
let cachedAccounts = [];

document.addEventListener('DOMContentLoaded', () => {
    // 1. Enforce Authentication check
    if (!checkAuth('ROLE_USER')) return;

    // 2. Initialize View
    initDashboard();

    // 3. Register Event Handlers
    registerFormHandlers();
});

// Switch visible tabs
function switchTab(tabName, linkElement = null) {
    // Hide all sections
    const sections = document.querySelectorAll('.tab-section');
    sections.forEach(s => s.style.display = 'none');

    // Show selected section
    const targetSection = document.getElementById(`tab-${tabName}`);
    if (targetSection) {
        targetSection.style.display = 'block';
    }

    // Update active link styling
    const links = document.querySelectorAll('.sidebar-link');
    links.forEach(l => l.classList.remove('active'));

    if (linkElement) {
        linkElement.classList.add('active');
    } else {
        // Find matching sidebar link by text search or indexing if triggered programmatically
        const matchingLink = Array.from(links).find(l => l.innerText.toLowerCase().includes(tabName));
        if (matchingLink) matchingLink.classList.add('active');
    }

    // Refresh data if specific tab is loaded
    if (tabName === 'overview') {
        loadAccountsAndHistory();
    } else if (tabName === 'notifications') {
        loadNotifications();
    } else if (tabName === 'loans') {
        selectLoanType(currentSelectedLoanType || 'PERSONAL');
        loadLoans();
    }
}

// Fetch initial data
async function initDashboard() {
    await fetchUserProfile();
    await loadAccountsAndHistory();
    await loadNotificationsCount();
}

async function fetchUserProfile() {
    try {
        const response = await fetch('/api/users/profile', {
            headers: authHeaders()
        });

        if (response.ok) {
            cachedUser = await response.json();
            document.getElementById('userNameLabel').textContent = cachedUser.fullName;
            document.getElementById('welcomeHeader').textContent = `Hello, ${cachedUser.fullName}`;

            // Populate profile forms
            document.getElementById('profileEmail').value = cachedUser.email;
            document.getElementById('profileRole').value = cachedUser.role.replace('ROLE_', '');
            document.getElementById('profileName').value = cachedUser.fullName;
            document.getElementById('profilePhone').value = cachedUser.phone;
            document.getElementById('profileAddress').value = cachedUser.address;
            document.getElementById('profileDob').value = cachedUser.dateOfBirth;
        } else {
            if (response.status === 401 || response.status === 403) logout();
        }
    } catch (err) {
        console.error("Error loading user profile", err);
        showToast("Error loading user profile. Re-authenticating...", "error");
        logout();
    }
}

async function loadAccountsAndHistory() {
    try {
        const response = await fetch('/api/accounts', {
            headers: authHeaders()
        });

        if (response.ok) {
            cachedAccounts = await response.json();
            renderAccountsGrid();
            populateAccountDropdowns();
            loadTransactionHistory();
        }
    } catch (err) {
        console.error("Error loading accounts", err);
    }
}

function renderAccountsGrid() {
    const grid = document.getElementById('accountsGrid');
    if (!grid) return;

    if (cachedAccounts.length === 0) {
        grid.innerHTML = `
            <div class="glass-card" style="grid-column: 1 / -1; text-align: center; padding: 3rem 1.5rem;">
                <p style="color: var(--color-text-secondary); margin-bottom: 1.5rem;">You do not have any active bank accounts. Click below to create one.</p>
                <button class="btn btn-primary" onclick="openCreateAccountModal()">Open Bank Account</button>
            </div>
        `;
        return;
    }

    grid.innerHTML = '';
    cachedAccounts.forEach(account => {
        const statusClass = account.status === 'ACTIVE' ? 'badge-success' : (account.status === 'BLOCKED' ? 'badge-warning' : 'badge-danger');

        const card = document.createElement('div');
        card.className = 'balance-card';
        card.innerHTML = `
            <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                <div>
                    <p style="font-size: 0.85rem; opacity: 0.8; text-transform: uppercase; letter-spacing:0.05em;">${account.accountType} Account</p>
                    <h3 style="font-size:1.15rem; font-weight:600; margin-top:0.25rem;">${account.accountNumber}</h3>
                </div>
                <span class="badge ${statusClass}">${account.status}</span>
            </div>
            <div style="margin-top: 1.5rem;">
                <p style="font-size: 0.8rem; opacity: 0.7;">Available Balance</p>
                <h2 style="font-size:2.2rem; font-weight:800; margin-top:0.25rem;">$${parseFloat(account.balance).toFixed(2)}</h2>
            </div>
            <div style="margin-top: 1rem; font-size: 0.75rem; opacity: 0.6;">
                Opened: ${new Date(account.createdAt).toLocaleDateString()}
            </div>
        `;
        grid.appendChild(card);
    });
}

function populateAccountDropdowns() {
    const depSelect = document.getElementById('depositAccountSelect');
    const wdrSelect = document.getElementById('withdrawAccountSelect');
    const trsSelect = document.getElementById('transferSenderSelect');

    const activeAccounts = cachedAccounts.filter(a => a.status === 'ACTIVE');

    const makeOptions = (select) => {
        if (!select) return;
        select.innerHTML = '';
        if (activeAccounts.length === 0) {
            select.innerHTML = '<option value="">No Active Accounts Available</option>';
            return;
        }
        activeAccounts.forEach(a => {
            const opt = document.createElement('option');
            opt.value = a.accountNumber;
            opt.textContent = `${a.accountType} - ${a.accountNumber} ($${parseFloat(a.balance).toFixed(2)})`;
            select.appendChild(opt);
        });
    };

    makeOptions(depSelect);
    makeOptions(wdrSelect);
    makeOptions(trsSelect);
}

async function loadTransactionHistory() {
    try {
        const response = await fetch('/api/transactions', {
            headers: authHeaders()
        });

        if (response.ok) {
            const txns = await response.json();
            const tbody = document.getElementById('recentTransactionsTable');
            if (!tbody) return;

            if (txns.length === 0) {
                tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--color-text-secondary);">No transactions recorded.</td></tr>`;
                return;
            }

            tbody.innerHTML = '';
            // Show only the 10 most recent transactions on Overview
            txns.slice(0, 10).forEach(t => {
                const tr = document.createElement('tr');
                const isNegative = t.transactionType === 'WITHDRAW' || (t.transactionType === 'TRANSFER' && t.previousBalance > t.newBalance);
                const amtColor = isNegative ? 'var(--color-danger)' : 'var(--color-success)';
                const amtPrefix = isNegative ? '-' : '+';

                tr.innerHTML = `
                    <td>${new Date(t.transactionDate).toLocaleString()}</td>
                    <td style="font-weight:600;">${t.account.accountNumber}</td>
                    <td><span class="badge ${t.transactionType === 'DEPOSIT' ? 'badge-success' : (t.transactionType === 'WITHDRAW' ? 'badge-danger' : 'badge-info')}">${t.transactionType}</span></td>
                    <td style="color:var(--color-text-secondary); max-width:250px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;" title="${t.description || ''}">${t.description || 'N/A'}</td>
                    <td style="color:${amtColor}; font-weight:bold">${amtPrefix}$${parseFloat(t.amount).toFixed(2)}</td>
                    <td>$${parseFloat(t.newBalance).toFixed(2)}</td>
                    <td><span class="badge badge-success">${t.status}</span></td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (err) {
        console.error("Error loading transactions", err);
    }
}

async function loadLoans() {
    try {
        const response = await fetch('/api/loans', {
            headers: authHeaders()
        });

        if (response.ok) {
            const loans = await response.json();
            const tbody = document.getElementById('myLoansTable');
            if (!tbody) return;

            if (loans.length === 0) {
                tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--color-text-secondary);">No loans filed.</td></tr>`;
                return;
            }

            tbody.innerHTML = '';
            loans.forEach(l => {
                const statusClass = l.loanStatus === 'APPROVED' ? 'badge-success' : (l.loanStatus === 'PENDING' ? 'badge-warning' : (l.loanStatus === 'REJECTED' ? 'badge-danger' : 'badge-info'));
                const tr = document.createElement('tr');
                const amtFormatted = l.requestedAmount ? `₹${parseFloat(l.requestedAmount).toLocaleString('en-IN')}` : 'N/A';
                const emiFormatted = l.emi ? `₹${parseFloat(l.emi).toLocaleString('en-IN')}/mo` : 'N/A';
                const acctNo = l.accountNumber || '';
                const last4 = acctNo.length >= 4 ? acctNo.slice(-4) : acctNo;
                const loanIdDisplay = l.customLoanId || (`MASHA` + last4);

                tr.innerHTML = `
                    <td style="font-weight:bold; color:var(--color-accent);">${loanIdDisplay}</td>
                    <td>${l.accountNumber || 'N/A'}</td>
                    <td><strong>${l.loanType}</strong></td>
                    <td>${amtFormatted}</td>
                    <td>${l.interestRate}%</td>
                    <td>${l.loanDuration} Months</td>
                    <td style="color:var(--color-success); font-weight:600;">${emiFormatted}</td>
                    <td><span class="badge ${statusClass}">${l.loanStatus}</span></td>
                    <td style="color:var(--color-text-secondary); font-size:0.85rem">${l.adminRemarks || 'Awaiting decision...'}</td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (err) {
        console.error("Error loading loans", err);
    }
}

// ==========================================
// LOAN SYSTEM MANAGER LOGIC
// ==========================================

let currentSelectedLoanType = 'PERSONAL';
let currentCalculatedEmi = 0;
let currentLoanPrincipal = 0;

const LOAN_CONFIGS = {
    PERSONAL: {
        title: 'Personal Loan',
        subtitle: 'Unsecured personal financing with fixed 10% interest rate.',
        amounts: [
            { label: '₹1,00,000', value: 100000 },
            { label: '₹2,00,000', value: 200000 },
            { label: '₹4,00,000', value: 400000 },
            { label: '₹5,00,000', value: 500000 },
            { label: '₹8,00,000', value: 800000 }
        ],
        tenures: [
            { label: '2 Years', years: 2 },
            { label: '5 Years', years: 5 },
            { label: '8 Years', years: 8 }
        ]
    },
    HOME: {
        title: 'Home Loan',
        subtitle: 'Finance your home with fixed 10% interest rate.',
        amounts: [
            { label: '₹1,00,000', value: 100000 },
            { label: '₹2,00,000', value: 200000 },
            { label: '₹4,00,000', value: 400000 },
            { label: '₹5,00,000', value: 500000 },
            { label: '₹8,00,000', value: 800000 }
        ],
        tenures: [
            { label: '2 Years', years: 2 },
            { label: '5 Years', years: 5 },
            { label: '8 Years', years: 8 }
        ]
    },
    BIKE: {
        title: 'Bike Loan',
        subtitle: 'Quick two-wheeler loan with fixed 10% interest rate.',
        amounts: [
            { label: '₹1,00,000', value: 100000 },
            { label: '₹2,00,000', value: 200000 },
            { label: '₹3,00,000', value: 300000 }
        ],
        tenures: [
            { label: '1 Year', years: 1 },
            { label: '2 Years', years: 2 },
            { label: '3 Years', years: 3 },
            { label: '5 Years', years: 5 }
        ]
    },
    CAR: {
        title: 'Car Loan',
        subtitle: 'Premium vehicle loan with fixed 10% interest rate.',
        amounts: [
            { label: '₹10,00,000', value: 1000000 },
            { label: '₹15,00,000', value: 1500000 },
            { label: '₹20,00,000', value: 2000000 },
            { label: '₹30,00,000', value: 3000000 }
        ],
        tenures: [
            { label: '2 Years', years: 2 },
            { label: '3 Years', years: 3 },
            { label: '4 Years', years: 4 },
            { label: '5 Years', years: 5 }
        ]
    },
    GOLD: {
        title: 'Gold Loan',
        subtitle: 'Instant liquidity against gold ornaments at ₹15,000 / gram valuation.',
        tenures: [
            { label: '2 Years', years: 2 },
            { label: '3 Years', years: 3 },
            { label: '5 Years', years: 5 }
        ]
    }
};

function toggleVehicleLoanMenu(event) {
    if (event) event.stopPropagation();
    const subMenu = document.getElementById('vehicleSubMenu');
    const chevron = document.getElementById('vehicleChevron');
    if (!subMenu) return;

    const isHidden = subMenu.style.display === 'none' || subMenu.style.display === '';
    if (isHidden) {
        subMenu.style.display = 'flex';
        if (chevron) chevron.classList.add('open');
        if (currentSelectedLoanType !== 'BIKE' && currentSelectedLoanType !== 'CAR') {
            selectLoanType('BIKE');
        }
    } else {
        subMenu.style.display = 'none';
        if (chevron) chevron.classList.remove('open');
    }
}

function selectLoanType(type) {
    currentSelectedLoanType = type;

    // Update active state styling
    document.querySelectorAll('.loan-menu-item').forEach(el => el.classList.remove('active'));
    const activeItem = document.getElementById(`loan-item-${type}`);
    if (activeItem) activeItem.classList.add('active');

    if (type === 'BIKE' || type === 'CAR') {
        const vehicleMenu = document.getElementById('loan-item-VEHICLE');
        if (vehicleMenu) vehicleMenu.classList.add('active');
        const subMenu = document.getElementById('vehicleSubMenu');
        const chevron = document.getElementById('vehicleChevron');
        if (subMenu) subMenu.style.display = 'flex';
        if (chevron) chevron.classList.add('open');
    }

    const config = LOAN_CONFIGS[type];
    if (!config) return;

    document.getElementById('selectedLoanTitle').textContent = config.title;
    document.getElementById('selectedLoanSubtitle').textContent = config.subtitle;

    const goldBanner = document.getElementById('goldBanner');
    const goldFields = document.getElementById('goldFieldsContainer');
    const loanAmountContainer = document.getElementById('loanAmountContainer');

    if (type === 'GOLD') {
        goldBanner.style.display = 'block';
        goldFields.style.display = 'block';
        loanAmountContainer.style.display = 'none';

        // Populate Gold Weight dropdown (1 to 20 grams, default 1 gram)
        const weightSelect = document.getElementById('goldWeightSelect');
        weightSelect.innerHTML = '';
        for (let g = 1; g <= 20; g++) {
            const opt = document.createElement('option');
            opt.value = g;
            opt.textContent = `${g} Gram${g > 1 ? 's' : ''}`;
            weightSelect.appendChild(opt);
        }
        weightSelect.value = 1;
    } else {
        goldBanner.style.display = 'none';
        goldFields.style.display = 'none';
        loanAmountContainer.style.display = 'block';

        // Populate Loan Amount dropdown
        const amtSelect = document.getElementById('loanAmountSelect');
        amtSelect.innerHTML = '';
        config.amounts.forEach(a => {
            const opt = document.createElement('option');
            opt.value = a.value;
            opt.textContent = a.label;
            amtSelect.appendChild(opt);
        });
    }

    // Populate Tenure dropdown
    const tenureSelect = document.getElementById('tenureSelect');
    tenureSelect.innerHTML = '';
    config.tenures.forEach(t => {
        const opt = document.createElement('option');
        opt.value = t.years;
        opt.textContent = t.label;
        tenureSelect.appendChild(opt);
    });

    calculateLoanDetails();
}

function handleAccountNumberChange() {
    const acctInput = document.getElementById('accountNumberInput');
    const loanIdInput = document.getElementById('loanIdInput');
    if (!acctInput || !loanIdInput) return;

    let val = acctInput.value.trim();
    if (val.length === 0) {
        loanIdInput.value = '';
    } else {
        const last4 = val.length >= 4 ? val.slice(-4) : val;
        loanIdInput.value = `MASHA${last4}`;
    }

    validateLoanForm();
}

function calculateLoanDetails() {
    let P = 0;
    if (currentSelectedLoanType === 'GOLD') {
        const weight = parseInt(document.getElementById('goldWeightSelect').value) || 1;
        P = weight * 15000;
        document.getElementById('goldValueDisplay').value = `₹${P.toLocaleString('en-IN')}`;
    } else {
        P = parseFloat(document.getElementById('loanAmountSelect').value) || 0;
    }

    currentLoanPrincipal = P;

    const tenureYears = parseInt(document.getElementById('tenureSelect').value) || 1;
    const N = tenureYears * 12;

    // Fixed Interest Rate R = 10% annual / (12 * 100)
    const R = 10 / (12 * 100);

    let emi = 0;
    if (P > 0 && N > 0) {
        const pow = Math.pow(1 + R, N);
        emi = (P * R * pow) / (pow - 1);
    }

    currentCalculatedEmi = Math.round(emi);
    document.getElementById('calculatedEmiDisplay').textContent = `₹${currentCalculatedEmi.toLocaleString('en-IN')}/month`;

    validateLoanForm();
}

function validateLoanForm() {
    const acctNo = document.getElementById('accountNumberInput').value.trim();
    const proofInput = document.getElementById('proofInput');
    if (!proofInput) return;

    // Numbers only restriction
    proofInput.value = proofInput.value.replace(/\D/g, '');
    const proofVal = proofInput.value.trim();

    const applyContainer = document.getElementById('applyButtonContainer');
    if (!applyContainer) return;

    const isAcctValid = acctNo.length > 0;
    const isProofValid = proofVal.length === 12 && /^\d{12}$/.test(proofVal);
    const isPrincipalValid = currentLoanPrincipal > 0;

    if (isAcctValid && isProofValid && isPrincipalValid) {
        applyContainer.style.display = 'block';
    } else {
        applyContainer.style.display = 'none';
    }
}

async function loadNotificationsCount() {
    try {
        const response = await fetch('/api/notifications/unread-count', {
            headers: authHeaders()
        });
        if (response.ok) {
            const data = await response.json();
            document.getElementById('unreadBadge').textContent = data.unreadCount;
        }
    } catch (err) {
        console.error(err);
    }
}

async function loadNotifications() {
    try {
        const response = await fetch('/api/notifications', {
            headers: authHeaders()
        });

        if (response.ok) {
            const alerts = await response.json();
            const list = document.getElementById('notificationsList');
            if (!list) return;

            if (alerts.length === 0) {
                list.innerHTML = `<p style="text-align:center; color:var(--color-text-secondary); padding: 2rem;">No alerts registered.</p>`;
                return;
            }

            list.innerHTML = '';
            alerts.forEach(a => {
                const item = document.createElement('div');
                item.className = `notification-item ${!a.isRead ? 'unread' : ''}`;

                let typeEmoji = '🔔';
                if (a.notificationType === 'TRANSACTION') typeEmoji = '💸';
                else if (a.notificationType === 'TRANSFER') typeEmoji = '🔄';
                else if (a.notificationType === 'LOAN') typeEmoji = '🏦';
                else if (a.notificationType === 'SYSTEM') typeEmoji = '⚙️';

                item.innerHTML = `
                    <div style="display:flex; gap: 1rem; align-items:flex-start;">
                        <div style="font-size:1.5rem;">${typeEmoji}</div>
                        <div style="flex-grow: 1;">
                            <div class="notification-title">${a.title}</div>
                            <div class="notification-desc">${a.message}</div>
                            <div class="notification-meta">
                                <span>${new Date(a.createdAt).toLocaleString()}</span>
                                ${!a.isRead ? `<button class="btn btn-sm btn-secondary" onclick="markNotificationRead(${a.id})">Mark Read</button>` : '<span style="color:var(--color-text-muted)">Read</span>'}
                            </div>
                        </div>
                    </div>
                `;
                list.appendChild(item);
            });
        }
    } catch (err) {
        console.error(err);
    }
}

async function markNotificationRead(id) {
    try {
        const response = await fetch(`/api/notifications/${id}/read`, {
            method: 'PUT',
            headers: authHeaders()
        });
        if (response.ok) {
            showToast("Notification marked as read", "success");
            loadNotifications();
            loadNotificationsCount();
        }
    } catch (err) {
        console.error(err);
    }
}

// Modal actions
function openCreateAccountModal() {
    const modal = document.getElementById('createAccountModal');
    if (modal) {
        modal.style.display = 'flex';
        modal.classList.add('active');
    }
}

function closeCreateAccountModal() {
    const modal = document.getElementById('createAccountModal');
    if (modal) {
        modal.classList.remove('active');
        modal.style.display = 'none';
    }
    const pinEl = document.getElementById('modalTransactionPin');
    if (pinEl) pinEl.value = '';
    const acctEl = document.getElementById('modalAccountNumber');
    if (acctEl) acctEl.value = '';
}


// Form submissions handlers
function registerFormHandlers() {
    // Create Account Form
    document.getElementById('createAccountForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const accountType = document.getElementById('modalAccountType').value;
        const initialBalance = document.getElementById('modalInitialBalance').value;
        const pin = document.getElementById('modalTransactionPin').value;
        const accountNumber = document.getElementById('modalAccountNumber').value.trim();

        if (!/^\d{4}$/.test(pin)) {
            showToast("Transaction PIN must be exactly 4 digits.", "error");
            return;
        }

        if (accountNumber && !/^\d{10}$/.test(accountNumber)) {
            showToast("Account Number must be exactly 10 digits.", "error");
            return;
        }

        try {
            const response = await fetch('/api/accounts', {
                method: 'POST',
                headers: authHeaders(),
                body: JSON.stringify({ accountType, initialBalance, pin, accountNumber })
            });

            if (response.ok || response.status === 201) {
                showToast("Bank account created successfully!", "success");
                document.getElementById('modalTransactionPin').value = '';
                document.getElementById('modalAccountNumber').value = '';
                closeCreateAccountModal();
                loadAccountsAndHistory();
            } else {
                const data = await response.json();
                showToast(data.message || "Failed to create account.", "error");
            }
        } catch (err) {
            console.error(err);
            showToast("Network error creating bank account.", "error");
        }
    });

    // Deposit Form
    document.getElementById('depositForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const accountNumber = document.getElementById('depositAccountSelect').value;
        const amount = document.getElementById('depositAmount').value;
        const description = document.getElementById('depositDesc').value;
        const pin = document.getElementById('depositPin').value;

        if (!accountNumber) {
            showToast("Please create an active account first.", "error");
            return;
        }

        if (!/^\d{4}$/.test(pin)) {
            showToast("Transaction PIN must be exactly 4 digits.", "error");
            return;
        }

        try {
            const response = await fetch('/api/transactions/deposit', {
                method: 'POST',
                headers: authHeaders(),
                body: JSON.stringify({ accountNumber, amount, description, pin })
            });

            if (response.ok) {
                showToast(`Deposited $${parseFloat(amount).toFixed(2)} successfully!`, "success");
                document.getElementById('depositAmount').value = '';
                document.getElementById('depositDesc').value = '';
                document.getElementById('depositPin').value = '';
                loadAccountsAndHistory();
                loadNotificationsCount();
            } else {
                const data = await response.json();
                showToast(data.message || "Deposit transaction failed", "error");
            }
        } catch (err) {
            console.error(err);
        }
    });

    // Withdraw Form
    document.getElementById('withdrawForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const accountNumber = document.getElementById('withdrawAccountSelect').value;
        const amount = document.getElementById('withdrawAmount').value;
        const description = document.getElementById('withdrawDesc').value;
        const pin = document.getElementById('withdrawPin').value;

        if (!accountNumber) {
            showToast("Please create an active account first.", "error");
            return;
        }

        if (!/^\d{4}$/.test(pin)) {
            showToast("Transaction PIN must be exactly 4 digits.", "error");
            return;
        }

        try {
            const response = await fetch('/api/transactions/withdraw', {
                method: 'POST',
                headers: authHeaders(),
                body: JSON.stringify({ accountNumber, amount, description, pin })
            });

            if (response.ok) {
                showToast(`Withdrew $${parseFloat(amount).toFixed(2)} successfully!`, "success");
                document.getElementById('withdrawAmount').value = '';
                document.getElementById('withdrawDesc').value = '';
                document.getElementById('withdrawPin').value = '';
                loadAccountsAndHistory();
                loadNotificationsCount();
            } else {
                const data = await response.json();
                showToast(data.message || "Withdrawal transaction failed", "error");
            }
        } catch (err) {
            console.error(err);
        }
    });

    // Transfer Form
    document.getElementById('transferForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const senderAccountNumber = document.getElementById('transferSenderSelect').value;
        const recipientAccountNumber = document.getElementById('transferRecipient').value;
        const amount = document.getElementById('transferAmount').value;
        const description = document.getElementById('transferDesc').value;
        const pin = document.getElementById('transferPin').value;

        if (!senderAccountNumber) {
            showToast("Please create an active account first.", "error");
            return;
        }

        if (!/^\d{4}$/.test(pin)) {
            showToast("Transaction PIN must be exactly 4 digits.", "error");
            return;
        }

        try {
            const response = await fetch('/api/transfers', {
                method: 'POST',
                headers: authHeaders(),
                body: JSON.stringify({ senderAccountNumber, recipientAccountNumber, amount, description, pin })
            });

            if (response.ok) {
                showToast(`Transferred $${parseFloat(amount).toFixed(2)} successfully!`, "success");
                document.getElementById('transferRecipient').value = '';
                document.getElementById('transferAmount').value = '';
                document.getElementById('transferDesc').value = '';
                document.getElementById('transferPin').value = '';
                loadAccountsAndHistory();
                loadNotificationsCount();
            } else {
                const data = await response.json();
                showToast(data.message || "Transfer failed. Check recipient account number and sender balance.", "error");
            }
        } catch (err) {
            console.error(err);
        }
    });

    // New Loan Application Form Handler
    const newLoanForm = document.getElementById('newLoanForm');
    if (newLoanForm) {
        newLoanForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const accountNumber = document.getElementById('accountNumberInput').value.trim();
            const customLoanId = document.getElementById('loanIdInput').value.trim();
            const proofNumber = document.getElementById('proofInput').value.trim();
            const tenureYears = parseInt(document.getElementById('tenureSelect').value) || 1;
            const loanDuration = tenureYears * 12;

            let goldWeight = null;
            let goldPurity = null;
            if (currentSelectedLoanType === 'GOLD') {
                goldWeight = parseInt(document.getElementById('goldWeightSelect').value) || 1;
                goldPurity = '24K';
            }

            const payload = {
                loanType: currentSelectedLoanType,
                accountNumber: accountNumber,
                customLoanId: customLoanId,
                requestedAmount: currentLoanPrincipal,
                loanDuration: loanDuration,
                proofNumber: proofNumber,
                emi: currentCalculatedEmi,
                goldWeight: goldWeight,
                goldPurity: goldPurity
            };

            try {
                const response = await fetch('/api/loans', {
                    method: 'POST',
                    headers: authHeaders(),
                    body: JSON.stringify(payload)
                });

                if (response.ok) {
                    showToast(`Loan Application (${customLoanId}) Submitted Successfully!`, "success");
                    document.getElementById('proofInput').value = '';
                    validateLoanForm();
                    loadLoans();
                    loadNotificationsCount();
                } else {
                    const data = await response.json();
                    showToast(data.message || "Failed to submit loan application.", "error");
                }
            } catch (err) {
                console.error("Error submitting loan application:", err);
                showToast("Network error submitting loan application.", "error");
            }
        });
    }

    // Profile update form
    document.getElementById('profileForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const fullName = document.getElementById('profileName').value;
        const phone = document.getElementById('profilePhone').value;
        const address = document.getElementById('profileAddress').value;
        const dateOfBirth = document.getElementById('profileDob').value;

        try {
            const response = await fetch('/api/users/profile', {
                method: 'PUT',
                headers: authHeaders(),
                body: JSON.stringify({ fullName, phone, address, dateOfBirth })
            });

            if (response.ok) {
                showToast("Profile settings updated successfully!", "success");
                fetchUserProfile();
            } else {
                const data = await response.json();
                showToast(data.message || "Failed to update profile settings.", "error");
            }
        } catch (err) {
            console.error(err);
        }
    });

    // Transaction PIN Form
    document.getElementById('pinForm').addEventListener('submit', async (e) => {
        e.preventDefault();

        const pin = document.getElementById('transactionPin').value;
        const confirmPin = document.getElementById('confirmTransactionPin').value;

        // Check 4 digits
        if (!/^\d{4}$/.test(pin)) {
            showToast("PIN must contain exactly 4 digits.", "error");
            return;
        }

        // Check confirmation
        if (pin !== confirmPin) {
            showToast("PINs do not match.", "error");
            return;
        }

        try {

            const response = await fetch('/api/users/transaction-pin', {
                method: 'PUT',
                headers: authHeaders(),
                body: JSON.stringify({
                    pin: pin
                })
            });

            if (response.ok) {

                showToast("Transaction PIN created successfully!", "success");

                closePinModal();

            } else {

                const data = await response.json();

                showToast(
                    data.message || "Failed to create Transaction PIN.",
                    "error"
                );
            }

        } catch (err) {

            console.error("Error creating transaction PIN:", err);

            showToast(
                "Network error while creating Transaction PIN.",
                "error"
            );
        }
    });

}
// ==========================================
// DEPOSIT / WITHDRAW TAB
// ==========================================

function showTransactionTab(type, clickedElement) {

    if (type === 'deposit') {
        switchTab('deposit', clickedElement);
    }

    if (type === 'withdraw') {
        switchTab('withdraw', clickedElement);
    }
}


// ==========================================
// TRANSACTION PIN MODAL
// ==========================================

function openTransactionPinModal() {
    const modal = document.getElementById("pinModal");
    if (!modal) return;
    modal.style.display = "flex";
    modal.classList.add("active");
}

function closePinModal() {
    const modal = document.getElementById("pinModal");
    if (modal) {
        modal.classList.remove("active");
        modal.style.display = "none";
    }

    const pin = document.getElementById("transactionPin");
    const confirmPin = document.getElementById("confirmTransactionPin");
    if (pin) pin.value = "";
    if (confirmPin) confirmPin.value = "";
}
