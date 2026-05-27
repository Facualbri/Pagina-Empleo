/**
 * auth.js — Módulo central de autenticación
 * ==========================================
 * Incluir en TODOS los HTML protegidos ANTES que cualquier otro script:
 *
 *   <script src="js/auth.js"></script>
 *
 * Provee:
 *   apiFetch(url, options)  → reemplaza fetch() con renovación automática de token
 *   cerrarSesion()          → logout completo (invalida refresh token en servidor)
 *   getAuthHeaders()        → { Authorization: 'Bearer ...' }
 */

// ── Claves de localStorage ────────────────────────────────────────────────────
const TOKEN_KEY         = 'token';
const REFRESH_TOKEN_KEY = 'refreshToken';
const USUARIO_ID_KEY    = 'usuarioId';
const USUARIO_ROL_KEY   = 'usuarioRol';
const USUARIO_NOMBRE_KEY= 'usuarioNombre';
const USUARIO_EMAIL_KEY = 'usuarioEmail';

// ── Estado interno ────────────────────────────────────────────────────────────
let _refreshPromise = null; // evita múltiples refreshes en paralelo

// ── Helper anti-XSS ────────────────────────────────────────────────────────────
function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// ── Helpers de storage ────────────────────────────────────────────────────────
function getToken()        { return localStorage.getItem(TOKEN_KEY); }
function getRefreshToken() { return localStorage.getItem(REFRESH_TOKEN_KEY); }

function guardarTokens(data) {
    if (data.token)        localStorage.setItem(TOKEN_KEY,          data.token);
    if (data.refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY,  data.refreshToken);
    if (data.tipo)         localStorage.setItem(USUARIO_ROL_KEY,    data.tipo);
    if (data.nombre)       localStorage.setItem(USUARIO_NOMBRE_KEY, data.nombre);
    if (data.email)        localStorage.setItem(USUARIO_EMAIL_KEY,  data.email);
    if (data.id)           localStorage.setItem(USUARIO_ID_KEY,     data.id);
}

function limpiarStorage() {
    [TOKEN_KEY, REFRESH_TOKEN_KEY, USUARIO_ID_KEY,
     USUARIO_ROL_KEY, USUARIO_NOMBRE_KEY, USUARIO_EMAIL_KEY].forEach(k => localStorage.removeItem(k));
}

// ── Headers de autorización ───────────────────────────────────────────────────
function getAuthHeaders() {
    const token = getToken();
    return token ? { 'Authorization': 'Bearer ' + token } : {};
}

// ── Renovar access token usando el refresh token ──────────────────────────────
async function renovarToken() {
    // Si ya hay un refresh en curso, reutilizamos esa promesa
    if (_refreshPromise) return _refreshPromise;

    const refreshToken = getRefreshToken();
    if (!refreshToken) {
        redirigirLogin();
        return Promise.reject(new Error('Sin refresh token'));
    }

    _refreshPromise = fetch('/api/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken })
    })
    .then(async res => {
        if (!res.ok) {
            // Refresh token inválido o expirado → forzar login
            limpiarStorage();
            redirigirLogin();
            throw new Error('Refresh token inválido o expirado.');
        }
        const data = await res.json();
        guardarTokens(data);
        return data.token;
    })
    .finally(() => {
        _refreshPromise = null;
    });

    return _refreshPromise;
}

// ── apiFetch — reemplaza fetch() con renovación automática ───────────────────
/**
 * Uso idéntico a fetch():
 *   const res = await apiFetch('/api/empleos', { method: 'GET' });
 *   const res = await apiFetch('/api/postulaciones', {
 *       method: 'POST',
 *       headers: { 'Content-Type': 'application/json' },
 *       body: JSON.stringify(data)
 *   });
 *
 * IMPORTANTE con FormData: NO pongas Content-Type en headers (el browser lo pone solo).
 *   const res = await apiFetch('/api/empleos/con-foto', { method: 'POST', body: formData });
 */
async function apiFetch(url, options = {}) {
    // Inyectar Authorization en los headers
    const headers = {
        ...(options.headers || {}),
        ...getAuthHeaders()
    };

    let res = await fetch(url, { ...options, headers });

    // Si el servidor responde 401 o 403, intentar renovar y reintentar UNA vez
    if (res.status === 401 || res.status === 403) {
        try {
            await renovarToken();
        } catch {
            // renovarToken() ya redirigió al login
            return res;
        }

        // Reintentar con el nuevo token
        const headersNuevos = {
            ...(options.headers || {}),
            ...getAuthHeaders()
        };
        res = await fetch(url, { ...options, headers: headersNuevos });

        // Si sigue fallando después del refresh, redirigir al login
        if (res.status === 401 || res.status === 403) {
            limpiarStorage();
            redirigirLogin();
        }
    }

    return res;
}

// ── Cerrar sesión ─────────────────────────────────────────────────────────────
async function cerrarSesion() {
    const refreshToken = getRefreshToken();

    try {
        // Invalidar el refresh token en el servidor (fire-and-forget)
        await fetch('/api/auth/logout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken: refreshToken || '' })
        });
    } catch {
        // Si falla la red igual limpiamos y redirigimos
    }

    limpiarStorage();
    window.location.href = 'pantallausuario.html';
}

// ── Redirigir al login ────────────────────────────────────────────────────────
function redirigirLogin() {
    // Evitar bucle si ya estamos en login.html
    if (!window.location.pathname.endsWith('login.html')) {
        window.location.href = 'login.html';
    }
}

// ── Verificar sesión al cargar la página ──────────────────────────────────────
// Llamar esto en páginas protegidas para asegurarse de que hay sesión activa.
function verificarSesion() {
    if (!getToken() && !getRefreshToken()) {
        redirigirLogin();
        return false;
    }
    return true;
}

// ── Redirigir automáticamente si ya hay sesión activa ─────────────────────────
// Llamar en páginas públicas (login, index) para saltar el login si ya estás autenticado.
async function redirigirSiSesionActiva() {
    const token = getToken();
    const rol = localStorage.getItem(USUARIO_ROL_KEY);

    if (token && rol) {
        redirigirPorRol(rol);
        return true;
    }

    // Si no hay token pero hay refresh token, intentar renovar
    const refreshToken = getRefreshToken();
    if (refreshToken) {
        try {
            await renovarToken();
            const nuevoRol = localStorage.getItem(USUARIO_ROL_KEY);
            if (nuevoRol) {
                redirigirPorRol(nuevoRol);
                return true;
            }
        } catch {
            // No se pudo renovar, dejamos que vea el login
        }
    }

    return false;
}

function redirigirPorRol(rol) {
    if (rol === 'ROLE_ADMIN') {
        window.location.href = 'pantallaAdminRoot.html';
    } else if (rol === 'ROLE_EMPRESA') {
        window.location.href = 'pantallaAdmin.html';
    } else {
        window.location.href = 'pantallausuario.html';
    }
}