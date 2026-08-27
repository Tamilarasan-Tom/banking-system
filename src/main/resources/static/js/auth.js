// Authentication management helpers

const AUTH_TOKEN_KEY = 'banking_jwt_token';
const AUTH_USER_KEY = 'banking_user_info';

function getAuthToken() {
    return localStorage.getItem(AUTH_TOKEN_KEY);
}

function getAuthUser() {
    const userStr = localStorage.getItem(AUTH_USER_KEY);
    return userStr ? JSON.parse(userStr) : null;
}

function setAuth(token, email, fullName, role) {
    localStorage.setItem(AUTH_TOKEN_KEY, token);
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify({ email, fullName, role }));
}

function clearAuth() {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(AUTH_USER_KEY);
}

function authHeaders() {
    const token = getAuthToken();
    const headers = {
        'Content-Type': 'application/json'
    };
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    return headers;
}

function logout() {
    const token = getAuthToken();
    const user = getAuthUser();
    const isAdmin = user && user.role === 'ROLE_ADMIN';
    
    const nextUrl = isAdmin ? 'admin-login.html' : 'login.html';

    if (token) {
        fetch('/api/auth/logout', {
            method: 'POST',
            headers: authHeaders()
        })
        .catch(err => console.error("Error calling logout API", err))
        .finally(() => {
            clearAuth();
            window.location.href = nextUrl;
        });
    } else {
        clearAuth();
        window.location.href = nextUrl;
    }
}

function checkAuth(requiredRole = 'ROLE_USER') {
    const token = getAuthToken();
    const user = getAuthUser();

    if (!token || !user) {
        clearAuth();
        window.location.href = requiredRole === 'ROLE_ADMIN' ? 'admin-login.html' : 'login.html';
        return false;
    }

    if (user.role !== requiredRole) {
        // Forbidden Role
        if (user.role === 'ROLE_ADMIN') {
            window.location.href = 'admin-dashboard.html';
        } else {
            window.location.href = 'dashboard.html';
        }
        return false;
    }

    return true;
}

// Global UI toast notification system
function showToast(message, type = 'success') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <span>${message}</span>
        <span style="cursor:pointer;margin-left:1rem;font-weight:bold" onclick="this.parentElement.remove()">×</span>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(-20px)';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}
