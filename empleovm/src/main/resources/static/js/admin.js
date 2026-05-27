let misEmpleos = [];
let empleosFiltrados = [];
let candidatosActuales = [];
let empleoIdActual = null;

const POR_PAGINA = 8;
let paginaActual = 1;
let sortCol = 'fecha';
let sortDir = 'desc';

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
    const id = localStorage.getItem('usuarioId');
    const nombre = localStorage.getItem('usuarioNombre');
    document.getElementById('nombreEmpresa').innerText = nombre || '—';
    cargarEstadisticas(id);
    cargarMisEmpleos(id);
});

async function cargarEstadisticas(id) {
    try {
        const res = await apiFetch(`/api/estadisticas/empresa/${id}`);
        const s = await res.json();
        document.getElementById('statTotal').innerText = s.totalAvisos ?? '—';
        document.getElementById('statActivos').innerText = s.activos ?? '—';
        document.getElementById('statPausados').innerText = s.pausados ?? '—';
        document.getElementById('statPostulantes').innerText = s.totalPostulantes ?? '—';
        document.getElementById('statNuevos').innerText = s.postulantesNuevos ?? '—';
        const topEl = document.getElementById('statTop');
        topEl.innerText = s.avisoMasPopular ? `★ ${s.avisoMasPopular}` : '';
        topEl.title = s.avisoMasPopular || '';
    } catch { }
}

async function publicarAviso() {
    const titulo = document.getElementById('titulo').value.trim();
    const descripcion = document.getElementById('descripcion').value.trim();
    const ubicacion = document.getElementById('ubicacion').value.trim();
    const sueldo = document.getElementById('sueldo').value;
    const foto = document.getElementById('fotoArchivo').files[0];
    const idEmpresa = localStorage.getItem('usuarioId');
    const nombreEmp = localStorage.getItem('usuarioNombre');

    if (!titulo || !descripcion || !sueldo) {
        toast('Completá título, descripción y sueldo.', 'error'); return;
    }

    const btn = document.getElementById('btnPublicar');
    btn.disabled = true;
    btn.innerHTML = '<i data-lucide="loader" style="width:18px"></i> Publicando...';
    lucide.createIcons();

    const fd = new FormData();
    fd.append('titulo', titulo);
    fd.append('descripcion', descripcion);
    fd.append('ubicacion', ubicacion);
    fd.append('empresa', nombreEmp);
    fd.append('idUsuario', idEmpresa);
    fd.append('sueldo', sueldo);
    if (foto) fd.append('archivo', foto);

    try {
        const res = await apiFetch('/api/empleos/con-foto', { method: 'POST', body: fd });
        if (res.ok) {
            toast('¡Vacante publicada con éxito!', 'success');
            ['titulo', 'descripcion', 'ubicacion', 'sueldo'].forEach(id => document.getElementById(id).value = '');
            document.getElementById('fotoArchivo').value = '';
            actualizarPreview();
            const id = localStorage.getItem('usuarioId');
            await cargarMisEmpleos(id);
            cargarEstadisticas(id);
        } else {
            const err = await res.json().catch(() => ({}));
            toast(err.error || 'Error al publicar.', 'error');
        }
    } catch { toast('Error de conexión.', 'error'); }
    finally {
        btn.disabled = false;
        btn.innerHTML = '<i data-lucide="send" style="width:18px"></i> Publicar Vacante';
        lucide.createIcons();
    }
}

let previewVisible = false;
function togglePreview() {
    previewVisible = !previewVisible;
    document.getElementById('previewContainer').style.display = previewVisible ? 'flex' : 'none';
    document.getElementById('btnPreview').innerHTML = previewVisible
        ? '<i data-lucide="eye-off" style="width:13px"></i> Ocultar previa'
        : '<i data-lucide="eye" style="width:13px"></i> Vista previa';
    if (previewVisible) actualizarPreview();
    lucide.createIcons();
}

function actualizarPreview() {
    if (!previewVisible) return;
    const t = document.getElementById('titulo').value.trim() || 'Nombre del puesto';
    const u = document.getElementById('ubicacion').value.trim() || 'Ubicación';
    const s = document.getElementById('sueldo').value;
    const d = document.getElementById('descripcion').value.trim() || 'Descripción del puesto...';
    document.getElementById('pvTitulo').innerText = t;
    document.getElementById('pvEmpresa').innerText = localStorage.getItem('usuarioNombre') || 'Tu empresa';
    document.getElementById('pvUbicacion').innerText = u;
    document.getElementById('pvSueldo').innerText = s ? `$${Number(s).toLocaleString('es-AR')} / mes` : 'Sueldo a convenir';
    document.getElementById('pvDesc').innerText = d;
}

async function cargarMisEmpleos(idEmpresa) {
    try {
        const res = await fetch('/api/empleos');
        const lista = await res.json();
        misEmpleos = lista.filter(e => String(e.idUsuario) === String(idEmpresa));
        filtrarTabla();
    } catch {
        document.getElementById('tablaOfertas').innerHTML =
            `<tr><td colspan="6" class="empty">Error al cargar las publicaciones.</td></tr>`;
    }
}

function filtrarTabla() {
    const q = (document.getElementById('searchTabla').value || '').toLowerCase();
    const estado = document.getElementById('filtroEstado').value;
    const orden = document.getElementById('filtroOrden').value;

    let lista = misEmpleos.filter(e => {
        const matchQ = !q || e.titulo.toLowerCase().includes(q) || (e.ubicacion || '').toLowerCase().includes(q);
        const matchE = !estado || (estado === 'activa' ? e.activo : !e.activo);
        return matchQ && matchE;
    });

    lista.sort((a, b) => {
        if (orden === 'fecha_desc') return new Date(b.fechaPublicacion || 0) - new Date(a.fechaPublicacion || 0);
        if (orden === 'fecha_asc') return new Date(a.fechaPublicacion || 0) - new Date(b.fechaPublicacion || 0);
        if (orden === 'postulantes_desc') return (b.cantidadPostulantes || 0) - (a.cantidadPostulantes || 0);
        if (orden === 'sueldo_desc') return (b.sueldo || 0) - (a.sueldo || 0);
        return 0;
    });

    empleosFiltrados = lista;
    paginaActual = 1;
    renderTabla();
}

function sortTabla(col) {
    if (sortCol === col) sortDir = sortDir === 'asc' ? 'desc' : 'asc';
    else { sortCol = col; sortDir = 'asc'; }
    empleosFiltrados.sort((a, b) => {
        let va = a[col] ?? '', vb = b[col] ?? '';
        if (col === 'postulantes') { va = a.cantidadPostulantes || 0; vb = b.cantidadPostulantes || 0; }
        if (typeof va === 'string') return sortDir === 'asc' ? va.localeCompare(vb) : vb.localeCompare(va);
        return sortDir === 'asc' ? va - vb : vb - va;
    });
    document.querySelectorAll('thead th').forEach(th => th.classList.remove('sorted'));
    renderTabla();
}

function renderTabla() {
    const tbody = document.getElementById('tablaOfertas');
    const inicio = (paginaActual - 1) * POR_PAGINA;
    const pagina = empleosFiltrados.slice(inicio, inicio + POR_PAGINA);

    if (!empleosFiltrados.length) {
        tbody.innerHTML = `<tr><td colspan="6" class="empty">No se encontraron vacantes con esos filtros.</td></tr>`;
        document.getElementById('paginacion').innerHTML = '';
        return;
    }

    tbody.innerHTML = pagina.map(e => {
        const sueldo = e.sueldo ? `$${Number(e.sueldo).toLocaleString('es-AR')}` : '—';
        const badge = e.activo
            ? '<span class="badge badge-activa">● Activa</span>'
            : '<span class="badge badge-pausada">● Pausada</span>';
        const fecha = e.fechaPublicacion
            ? new Date(e.fechaPublicacion).toLocaleDateString('es-AR') : '';
        const nuevos = e.postulantesNuevos > 0
            ? `<span class="badge-nuevo">+${e.postulantesNuevos} nuevo${e.postulantesNuevos > 1 ? 's' : ''}</span>` : '';

        return `<tr>
            <td>
                <div class="td-titulo">${escHtml(e.titulo)}</div>
                ${fecha ? `<div class="td-fecha">Publicado: ${fecha}</div>` : ''}
            </td>
            <td class="td-ubicacion">${escHtml(e.ubicacion || '—')}</td>
            <td>${sueldo}</td>
            <td>
                <button class="btn-accion btn-azul" onclick="verCandidatos(${e.id},'${escHtml(e.titulo)}')">
                    <i data-lucide="users" style="width:13px"></i> ${e.cantidadPostulantes || 0}
                </button>
                ${nuevos}
            </td>
            <td>${badge}</td>
            <td>
                <div class="acciones">
                    <button class="btn-accion btn-amarillo" onclick="abrirEditar(${e.id})" title="Editar">
                        <i data-lucide="pencil" style="width:13px"></i>
                    </button>
                    <button class="btn-accion btn-gris" onclick="cambiarEstado(${e.id})">
                        ${e.activo ? 'Pausar' : 'Activar'}
                    </button>
                    <button class="btn-accion btn-rojo" onclick="eliminarEmpleo(${e.id})" title="Eliminar">
                        <i data-lucide="trash-2" style="width:13px"></i>
                    </button>
                </div>
            </td>
        </tr>`;
    }).join('');

    lucide.createIcons();
    renderPaginacion();
}

function renderPaginacion() {
    const total = Math.ceil(empleosFiltrados.length / POR_PAGINA);
    const cont = document.getElementById('paginacion');
    if (total <= 1) { cont.innerHTML = ''; return; }

    let btns = `<button class="page-btn" onclick="irPagina(${paginaActual - 1})" ${paginaActual === 1 ? 'disabled' : ''}>‹</button>`;
    for (let i = 1; i <= total; i++) {
        btns += `<button class="page-btn ${i === paginaActual ? 'active' : ''}" onclick="irPagina(${i})">${i}</button>`;
    }
    btns += `<button class="page-btn" onclick="irPagina(${paginaActual + 1})" ${paginaActual === total ? 'disabled' : ''}>›</button>`;
    cont.innerHTML = btns;
}

function irPagina(n) {
    const total = Math.ceil(empleosFiltrados.length / POR_PAGINA);
    if (n < 1 || n > total) return;
    paginaActual = n;
    renderTabla();
}

function escHtml(str) {
    if (!str) return '';
    return String(str).replace(/'/g, "&#39;").replace(/"/g, "&quot;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

async function cambiarEstado(id) {
    try {
        const res = await apiFetch(`/api/empleos/${id}/cambiar-estado`, { method: 'PATCH' });
        const data = await res.json();
        toast(data.mensaje || 'Estado actualizado.', 'success');
        const idE = localStorage.getItem('usuarioId');
        await cargarMisEmpleos(idE);
        cargarEstadisticas(idE);
    } catch { toast('Error al cambiar el estado.', 'error'); }
}

async function eliminarEmpleo(id) {
    if (!confirm('¿Borrar esta vacante? Esta acción no se puede deshacer.')) return;
    try {
        await apiFetch(`/api/empleos/${id}`, { method: 'DELETE' });
        toast('Vacante eliminada.', 'info');
        const idE = localStorage.getItem('usuarioId');
        await cargarMisEmpleos(idE);
        cargarEstadisticas(idE);
    } catch { toast('Error al eliminar.', 'error'); }
}

function abrirEditar(id) {
    const e = misEmpleos.find(x => x.id === id);
    if (!e) return;
    document.getElementById('editId').value = e.id;
    document.getElementById('editTitulo').value = e.titulo || '';
    document.getElementById('editUbicacion').value = e.ubicacion || '';
    document.getElementById('editSueldo').value = e.sueldo || '';
    document.getElementById('editDescripcion').value = e.descripcion || '';
    document.getElementById('modalEditar').style.display = 'flex';
}

function cerrarModalEditar() { document.getElementById('modalEditar').style.display = 'none'; }

async function guardarEdicion() {
    const id = document.getElementById('editId').value;
    const titulo = document.getElementById('editTitulo').value.trim();
    const ubicacion = document.getElementById('editUbicacion').value.trim();
    const sueldo = document.getElementById('editSueldo').value;
    const descripcion = document.getElementById('editDescripcion').value.trim();

    if (!titulo || !descripcion) {
        toast('El título y la descripción son obligatorios.', 'error'); return;
    }

    try {
        const res = await apiFetch(`/api/empleos/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ titulo, ubicacion, sueldo: Number(sueldo), descripcion })
        });
        if (res.ok) {
            toast('Vacante actualizada correctamente.', 'success');
            cerrarModalEditar();
            const idE = localStorage.getItem('usuarioId');
            await cargarMisEmpleos(idE);
            cargarEstadisticas(idE);
        } else {
            const err = await res.json().catch(() => ({}));
            toast(err.error || 'Error al guardar.', 'error');
        }
    } catch { toast('Error de conexión.', 'error'); }
}

async function verCandidatos(id, titulo) {
    empleoIdActual = id;
    document.getElementById('modalCandidatosTitulo').innerText = titulo;
    document.getElementById('modalCandidatosSubtitulo').innerText = 'Postulantes para esta vacante';
    document.getElementById('listaCandidatos').innerHTML = '<p class="empty">Cargando...</p>';
    document.getElementById('searchCandidatos').value = '';
    document.getElementById('filtroCandidatoEstado').value = '';
    document.getElementById('modalCandidatos').style.display = 'flex';

    try {
        const res = await apiFetch(`/api/postulaciones/por-empleo/${id}`);
        candidatosActuales = await res.json();
        renderCandidatos(candidatosActuales);
        lucide.createIcons();
    } catch { toast('Error al cargar candidatos.', 'error'); }
}

function filtrarCandidatos() {
    const q = (document.getElementById('searchCandidatos').value || '').toLowerCase();
    const estado = document.getElementById('filtroCandidatoEstado').value;
    const filtrados = candidatosActuales.filter(p => {
        const nombre = (p.postulante?.nombre || '').toLowerCase();
        const email = (p.postulante?.email || '').toLowerCase();
        const matchQ = !q || nombre.includes(q) || email.includes(q);
        const matchE = !estado || p.estadoCandidato === estado;
        return matchQ && matchE;
    });
    renderCandidatos(filtrados);
}

function renderCandidatos(lista) {
    const cont = document.getElementById('listaCandidatos');
    document.getElementById('modalCandidatosSubtitulo').innerText =
        `${lista.length} postulante${lista.length !== 1 ? 's' : ''}`;

    if (!lista.length) {
        cont.innerHTML = `<div style="text-align:center;padding:30px;color:var(--muted)">No se encontraron postulantes.</div>`;
        return;
    }

    const ESTADO_CLASS = { PENDIENTE: 'badge-pendiente', EN_PROCESO: 'badge-en-proceso', CONTACTADO: 'badge-contactado', DESCARTADO: 'badge-descartado' };
    const ESTADO_LABEL = { PENDIENTE: '⏳ Pendiente', EN_PROCESO: '🔄 En proceso', CONTACTADO: '✓ Contactado', DESCARTADO: '✗ Descartado' };

    cont.innerHTML = lista.map(p => {
        const fecha = p.fechaPostulacion ? new Date(p.fechaPostulacion).toLocaleDateString('es-AR') : '';
        const estadoC = ESTADO_CLASS[p.estadoCandidato] || 'badge-pendiente';
        const estadoL = ESTADO_LABEL[p.estadoCandidato] || '⏳ Pendiente';
        const esNuevo = !p.visto;

        return `
        <div class="candidato-card ${esNuevo ? 'nuevo-postulante' : ''}" id="card-postulacion-${p.id}">
            <div class="candidato-card-row">
                <div>
                    <div class="candidato-nombre">
                        ${escHtml(p.postulante?.nombre || 'Sin nombre')}
                        ${esNuevo ? '<span class="badge-nuevo">NUEVO</span>' : ''}
                    </div>
                    <div class="candidato-fecha">
                        ${escHtml(p.postulante?.email || '')} ${fecha ? '· ' + fecha : ''}
                    </div>
                </div>
                <span class="badge ${estadoC}" id="badge-estado-${p.id}">${estadoL}</span>
            </div>
            <div class="candidato-card-row">
                <select class="estado-select" onchange="cambiarEstadoCandidato(${p.id}, this.value)">
                    <option value="PENDIENTE"  ${p.estadoCandidato === 'PENDIENTE' ? 'selected' : ''}>⏳ Pendiente</option>
                    <option value="EN_PROCESO" ${p.estadoCandidato === 'EN_PROCESO' ? 'selected' : ''}>🔄 En proceso</option>
                    <option value="CONTACTADO" ${p.estadoCandidato === 'CONTACTADO' ? 'selected' : ''}>✓ Contactado</option>
                    <option value="DESCARTADO" ${p.estadoCandidato === 'DESCARTADO' ? 'selected' : ''}>✗ Descartado</option>
                </select>
                <button class="btn-cv" onclick="verCV('${p.archivoCv}')">
                    <i data-lucide="file-text" style="width:14px"></i> Ver CV
                </button>
            </div>
        </div>`;
    }).join('');

    lucide.createIcons();
}

async function verCV(filename) {
    if (!filename) { toast('No hay CV disponible.', 'error'); return; }
    if (filename.startsWith('http')) {
        window.open(filename, '_blank');
        return;
    }
    try {
        const res = await apiFetch(`/api/postulaciones/cv/${filename}`);
        if (!res.ok) { toast('No se pudo cargar el CV.', 'error'); return; }
        const blob = await res.blob();
        window.open(URL.createObjectURL(blob), '_blank');
    } catch { toast('Error al abrir el CV.', 'error'); }
}

async function cambiarEstadoCandidato(idPostulacion, nuevoEstado) {
    try {
        const res = await apiFetch(`/api/postulaciones/${idPostulacion}/estado`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ estado: nuevoEstado })
        });
        if (res.ok) {
            const ESTADO_CLASS = { PENDIENTE: 'badge-pendiente', EN_PROCESO: 'badge-en-proceso', CONTACTADO: 'badge-contactado', DESCARTADO: 'badge-descartado' };
            const ESTADO_LABEL = { PENDIENTE: '⏳ Pendiente', EN_PROCESO: '🔄 En proceso', CONTACTADO: '✓ Contactado', DESCARTADO: '✗ Descartado' };
            const badge = document.getElementById(`badge-estado-${idPostulacion}`);
            if (badge) { badge.className = `badge ${ESTADO_CLASS[nuevoEstado]}`; badge.innerText = ESTADO_LABEL[nuevoEstado]; }
            const p = candidatosActuales.find(x => x.id === idPostulacion);
            if (p) { p.estadoCandidato = nuevoEstado; p.visto = true; }
            const card = document.getElementById(`card-postulacion-${idPostulacion}`);
            if (card) card.classList.remove('nuevo-postulante');
            toast('Estado actualizado.', 'success');
        } else { toast('Error al actualizar el estado.', 'error'); }
    } catch { toast('Error de conexión.', 'error'); }
}

function cerrarModalCandidatos() {
    document.getElementById('modalCandidatos').style.display = 'none';
    const idE = localStorage.getItem('usuarioId');
    cargarMisEmpleos(idE);
    cargarEstadisticas(idE);
}

function exportarCSV() {
    if (!empleosFiltrados.length) { toast('No hay datos para exportar.', 'error'); return; }
    const headers = ['ID', 'Título', 'Ubicación', 'Sueldo', 'Estado', 'Postulantes', 'Fecha publicación'];
    const filas = empleosFiltrados.map(e => [
        e.id,
        `"${(e.titulo || '').replace(/"/g, '""')}"`,
        `"${(e.ubicacion || '').replace(/"/g, '""')}"`,
        e.sueldo || '',
        e.activo ? 'Activo' : 'Pausado',
        e.cantidadPostulantes || 0,
        e.fechaPublicacion ? new Date(e.fechaPublicacion).toLocaleDateString('es-AR') : ''
    ]);
    descargarCSV([headers, ...filas], 'mis-vacantes.csv');
    toast('CSV descargado.', 'success');
}

function exportarCandidatosCSV() {
    if (!candidatosActuales.length) { toast('No hay candidatos para exportar.', 'error'); return; }
    const headers = ['Nombre', 'Email', 'Estado', 'Visto', 'Fecha postulación', 'CV'];
    const filas = candidatosActuales.map(p => [
        `"${(p.postulante?.nombre || '').replace(/"/g, '""')}"`,
        `"${(p.postulante?.email || '').replace(/"/g, '""')}"`,
        p.estadoCandidato || 'PENDIENTE',
        p.visto ? 'Sí' : 'No',
        p.fechaPostulacion ? new Date(p.fechaPostulacion).toLocaleDateString('es-AR') : '',
        p.archivoCv || ''
    ]);
    const empleo = misEmpleos.find(e => e.id === empleoIdActual);
    const nombre = empleo ? empleo.titulo.replace(/[^a-z0-9]/gi, '_').toLowerCase() : 'candidatos';
    descargarCSV([headers, ...filas], `candidatos_${nombre}.csv`);
    toast('CSV de candidatos descargado.', 'success');
}

function descargarCSV(filas, nombre) {
    const contenido = '\uFEFF' + filas.map(f => f.join(',')).join('\n');
    const blob = new Blob([contenido], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = nombre; a.click();
    URL.revokeObjectURL(url);
}

function abrirPerfil() {
    document.getElementById('perfilNombre').value = localStorage.getItem('usuarioNombre') || '';
    document.getElementById('perfilEmail').value = localStorage.getItem('usuarioEmail') || '';
    document.getElementById('perfilPassActual').value = '';
    document.getElementById('perfilPassNueva').value = '';
    document.getElementById('perfilPassConfirm').value = '';
    document.getElementById('modalPerfil').style.display = 'flex';
}

function cerrarModalPerfil() { document.getElementById('modalPerfil').style.display = 'none'; }

async function guardarPerfil() {
    const id = localStorage.getItem('usuarioId');
    const nombre = document.getElementById('perfilNombre').value.trim();
    const email = document.getElementById('perfilEmail').value.trim();
    const actual = document.getElementById('perfilPassActual').value;
    const nueva = document.getElementById('perfilPassNueva').value;
    const confirm = document.getElementById('perfilPassConfirm').value;

    try {
        const res = await apiFetch(`/api/usuarios/${id}/perfil`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nombre, email })
        });
        if (res.ok) {
            const u = await res.json();
            localStorage.setItem('usuarioNombre', u.nombre);
            localStorage.setItem('usuarioEmail', u.email);
            document.getElementById('nombreEmpresa').innerText = u.nombre;
            toast('Datos actualizados correctamente.', 'success');
        } else {
            const err = await res.text();
            toast(err || 'Error al actualizar.', 'error'); return;
        }
    } catch { toast('Error de conexión.', 'error'); return; }

    if (actual || nueva || confirm) {
        if (nueva !== confirm) { toast('Las contraseñas nuevas no coinciden.', 'error'); return; }
        try {
            const res = await apiFetch(`/api/usuarios/${id}/cambiar-password`, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ passwordActual: actual, passwordNueva: nueva })
            });
            if (res.ok) toast('Contraseña actualizada.', 'success');
            else { const err = await res.text(); toast(err || 'Error al cambiar contraseña.', 'error'); return; }
        } catch { toast('Error de conexión.', 'error'); return; }
    }

    cerrarModalPerfil();
}
