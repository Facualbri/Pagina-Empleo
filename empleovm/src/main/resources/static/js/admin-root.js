let todosLosUsuarios = [];

function avatarHTML(u) {
    const inicial = (u.nombre || '?').charAt(0).toUpperCase();
    return `<div class="avatar">${inicial}</div>`;
}

function toast(msg, tipo = 'info') {
    const el = document.createElement('div');
    el.className = `toast ${tipo}`;
    const icon = tipo === 'success' ? '✓' : tipo === 'error' ? '✗' : 'ℹ';
    el.innerHTML = `<span>${icon}</span><span>${msg}</span>`;
    document.getElementById('toast-container').appendChild(el);
    setTimeout(() => el.remove(), 4000);
}

document.addEventListener('DOMContentLoaded', () => {
    if (!verificarSesion()) return;
    const rol = localStorage.getItem('usuarioRol');
    if (rol !== 'ROLE_ADMIN') {
        window.location.href = 'login.html';
        return;
    }
    document.getElementById('nombreAdmin').innerText =
        localStorage.getItem('usuarioNombre') || 'Admin';
    cargarTodo();
});

async function cargarTodo() {
    await Promise.all([cargarSolicitudes(), cargarUsuarios(), cargarStats()]);
}

async function cargarStats() {
    try {
        const res = await apiFetch('/api/usuarios');
        const lista = await res.json();
        todosLosUsuarios = lista;

        document.getElementById('statUsuarios').innerText = lista.filter(u => u.tipo === 'ROLE_USER').length;
        document.getElementById('statEmpresas').innerText = lista.filter(u => u.tipo === 'ROLE_EMPRESA').length;
        const pendientes = lista.filter(u => u.estadoSolicitud === 'PENDIENTE').length;
        document.getElementById('statPendientes').innerText = pendientes;
        document.getElementById('badgeSolicitudes').innerText = pendientes;

        const resE = await fetch('/api/empleos');
        const empleos = await resE.json();
        document.getElementById('statEmpleos').innerText = empleos.filter(e => e.activo).length;
    } catch { }
}

async function cargarSolicitudes() {
    const cont = document.getElementById('listaSolicitudes');
    try {
        const res = await apiFetch('/api/usuarios/solicitudes-empresa');
        const lista = await res.json();

        if (!lista.length) {
            cont.innerHTML = '<div class="empty">No hay solicitudes pendientes. 🎉</div>';
            return;
        }

        cont.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>Usuario</th>
                    <th>Localidad</th>
                    <th>Registro</th>
                    <th style="text-align:right">Acciones</th>
                </tr>
            </thead>
            <tbody>
                ${lista.map(u => `
                <tr>
                    <td>
                        <div class="user-info">
                            ${avatarHTML(u)}
                            <div>
                                <div class="user-nombre">${u.nombre}</div>
                                <div class="user-email">${u.email}</div>
                            </div>
                        </div>
                    </td>
                    <td>${u.localidad || '—'}</td>
                    <td>${u.fechaRegistro ? new Date(u.fechaRegistro).toLocaleDateString('es-AR') : '—'}</td>
                    <td>
                        <div class="acciones" style="justify-content:flex-end">
                            <button class="btn-accion btn-gris" onclick="verPerfil(${u.id})">
                                <i data-lucide="eye" style="width:13px"></i> Ver perfil
                            </button>
                            <button class="btn-accion btn-verde" onclick="aprobar(${u.id}, '${u.nombre.replace(/'/g, "\\'")}')">
                                <i data-lucide="check" style="width:13px"></i> Aprobar
                            </button>
                            <button class="btn-accion btn-rojo" onclick="rechazar(${u.id}, '${u.nombre.replace(/'/g, "\\'")}')">
                                <i data-lucide="x" style="width:13px"></i> Rechazar
                            </button>
                        </div>
                    </td>
                </tr>`).join('')}
            </tbody>
        </table>`;
        lucide.createIcons();
    } catch {
        cont.innerHTML = '<div class="empty">Error al cargar solicitudes.</div>';
    }
}

async function aprobar(id, nombre) {
    if (!confirm(`¿Aprobar a "${nombre}" como empresa?`)) return;
    try {
        const res = await apiFetch(`/api/usuarios/${id}/aprobar-empresa`, { method: 'PUT' });
        if (res.ok) {
            toast(`✓ ${nombre} aprobado/a como empresa.`, 'success');
            cerrarModal();
            cargarTodo();
        } else {
            toast('Error al aprobar.', 'error');
        }
    } catch { toast('Error de conexión.', 'error'); }
}

async function rechazar(id, nombre) {
    if (!confirm(`¿Rechazar la solicitud de "${nombre}"?`)) return;
    try {
        const res = await apiFetch(`/api/usuarios/${id}/rechazar-empresa`, { method: 'PUT' });
        if (res.ok) {
            toast(`Solicitud de ${nombre} rechazada.`, 'info');
            cerrarModal();
            cargarTodo();
        } else {
            toast('Error al rechazar.', 'error');
        }
    } catch { toast('Error de conexión.', 'error'); }
}

async function verPerfil(id) {
    try {
        const res = await apiFetch(`/api/perfil/${id}`);
        const u = await res.json();

        document.getElementById('modalNombre').innerText = u.nombre;
        document.getElementById('modalEmail').innerText = u.email;

        document.getElementById('modalDetalle').innerHTML = `
        <div class="perfil-row"><strong>Localidad:</strong> ${u.localidad || '—'}</div>
        <div class="perfil-row"><strong>Teléfono:</strong>  ${u.telefono || '—'}</div>
        <div class="perfil-row"><strong>Estado:</strong>
            <span class="badge badge-pendiente">${u.estadoSolicitud || '—'}</span>
        </div>
        ${u.descripcion ? `
            <div style="font-size:0.78rem;font-weight:700;color:var(--muted);text-transform:uppercase;margin-top:4px">Sobre mí</div>
            <div class="perfil-desc">${u.descripcion}</div>
        ` : ''}
        ${u.experiencia ? `
            <div style="font-size:0.78rem;font-weight:700;color:var(--muted);text-transform:uppercase;margin-top:4px">Experiencia</div>
            <div class="perfil-desc">${u.experiencia}</div>
        ` : ''}`;

        const nombreEsc = u.nombre.replace(/'/g, "\\'");
        document.getElementById('modalAcciones').innerHTML = `
        <button class="btn-accion btn-verde" style="flex:1;justify-content:center" onclick="aprobar(${u.id}, '${nombreEsc}')">
            <i data-lucide="check" style="width:14px"></i> Aprobar como empresa
        </button>
        <button class="btn-accion btn-rojo" style="flex:1;justify-content:center" onclick="rechazar(${u.id}, '${nombreEsc}')">
            <i data-lucide="x" style="width:14px"></i> Rechazar
        </button>`;

        document.getElementById('modalPerfil').style.display = 'flex';
        lucide.createIcons();
    } catch { toast('Error al cargar el perfil.', 'error'); }
}

function cerrarModal() {
    document.getElementById('modalPerfil').style.display = 'none';
}

async function cargarUsuarios() {
    const cont = document.getElementById('listaUsuarios');
    try {
        const res = await apiFetch('/api/usuarios');
        const lista = await res.json();
        todosLosUsuarios = lista;
        renderUsuarios(lista);
    } catch {
        cont.innerHTML = '<div class="empty">Error al cargar usuarios.</div>';
    }
}

function renderUsuarios(lista) {
    const cont = document.getElementById('listaUsuarios');
    if (!lista.length) {
        cont.innerHTML = '<div class="empty">No se encontraron usuarios.</div>';
        return;
    }
    cont.innerHTML = `
    <table>
        <thead>
            <tr>
                <th>Usuario</th><th>Rol</th><th>Solicitud</th><th>Registro</th>
                <th style="text-align:right">Acciones</th>
            </tr>
        </thead>
        <tbody>
            ${lista.map(u => {
        const rolBadge = u.tipo === 'ROLE_ADMIN'
            ? `<span class="badge badge-admin">Admin</span>`
            : u.tipo === 'ROLE_EMPRESA'
                ? `<span class="badge badge-empresa">Empresa</span>`
                : `<span class="badge badge-user">Usuario</span>`;
        const solicitudBadge = u.estadoSolicitud === 'PENDIENTE'
            ? `<span class="badge badge-pendiente">Pendiente</span>`
            : u.estadoSolicitud === 'APROBADO'
                ? `<span class="badge badge-aprobado">Aprobado</span>`
                : `<span class="badge badge-ninguna">—</span>`;
        return `<tr>
                    <td>
                        <div class="user-info">
                            ${avatarHTML(u)}
                            <div>
                                <div class="user-nombre">${u.nombre}</div>
                                <div class="user-email">${u.email}</div>
                            </div>
                        </div>
                    </td>
                    <td>${rolBadge}</td>
                    <td>${solicitudBadge}</td>
                    <td>${u.fechaRegistro ? new Date(u.fechaRegistro).toLocaleDateString('es-AR') : '—'}</td>
                    <td>
                        <div class="acciones" style="justify-content:flex-end">
                            <button class="btn-accion btn-gris" onclick="verPerfil(${u.id})">
                                <i data-lucide="eye" style="width:13px"></i> Ver
                            </button>
                        </div>
                    </td>
                </tr>`;
    }).join('')}
        </tbody>
    </table>`;
    lucide.createIcons();
}

function filtrarUsuarios() {
    const q = document.getElementById('buscadorUsuarios').value.toLowerCase();
    renderUsuarios(todosLosUsuarios.filter(u =>
        u.nombre.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)
    ));
}

function cambiarTab(nombre, btn) {
    document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.getElementById(`tab-${nombre}`).classList.add('active');
    btn.classList.add('active');
}
