let leafletMap = null;
let idEmpleoActual = null;
let favoritosDelUsuario = new Set();

function toast(msg, tipo = 'info') {
    const el = document.createElement('div');
    el.className = `toast ${tipo}`;
    const icon = tipo === 'success' ? '✓' : tipo === 'error' ? '✗' : 'ℹ';
    el.innerHTML = `<span>${icon}</span><span>${msg}</span>`;
    document.getElementById('toast-container').appendChild(el);
    setTimeout(() => el.remove(), 4000);
}

function haySesion() {
    return !!localStorage.getItem('token') || !!localStorage.getItem('refreshToken');
}

function requiereAuth() {
    if (!haySesion()) {
        window.location.href = 'login.html';
        return false;
    }
    return true;
}

document.addEventListener('DOMContentLoaded', async () => {
    const sesion = haySesion();
    document.getElementById('navAnon').style.display = sesion ? 'none' : 'flex';
    document.getElementById('navUser').style.display = sesion ? 'flex' : 'none';

    if (sesion) {
        const nombre = localStorage.getItem('usuarioNombre');
        document.getElementById('nombreUser').innerText = nombre || 'Usuario';
        await cargarFavoritosIniciales();
        verificarCambioRol();
    }

    cargarOfertas();
});

async function cargarOfertas() {
    try {
        const res = await fetch('/api/empleos');
        const empleos = await res.json();
        renderizarCards(empleos.filter(e => e.activo));
    } catch {
        toast('No se pudieron cargar las ofertas.', 'error');
    }
}

async function buscar() {
    const titulo = document.getElementById('busqueda').value;
    const ubicacion = document.getElementById('filtroUbicacion').value;
    const sueldoMin = document.getElementById('filtroSueldoMin').value;
    const sueldoMax = document.getElementById('filtroSueldoMax').value;

    let url = `/api/empleos/buscar?titulo=${encodeURIComponent(titulo)}&soloActivos=true`;
    if (ubicacion) url += `&ubicacion=${encodeURIComponent(ubicacion)}`;
    if (sueldoMin) url += `&sueldoMin=${sueldoMin}`;
    if (sueldoMax) url += `&sueldoMax=${sueldoMax}`;

    try {
        const res = await fetch(url);
        renderizarCards(await res.json());
    } catch { toast('Error en la búsqueda.', 'error'); }
}

function limpiarFiltros() {
    ['busqueda', 'filtroUbicacion', 'filtroSueldoMin', 'filtroSueldoMax']
        .forEach(id => document.getElementById(id).value = '');
    cargarOfertas();
}

function renderizarCards(lista) {
    const cont = document.getElementById('resultados');
    const badge = document.getElementById('countBadge');
    if (badge) badge.innerText = lista.length + ' vacantes';

    if (!lista.length) {
        cont.innerHTML = `
        <div class="empty-state">
            <i data-lucide="search-x"></i>
            <p>No hay vacantes disponibles con estos filtros.</p>
        </div>`;
        lucide.createIcons();
        return;
    }

    cont.innerHTML = lista.map(e => {
        const fotoHTML = e.imagenUrl
            ? `<img class="card-img" src="${e.imagenUrl}" alt="${e.titulo}"
                onerror="this.onerror=null;this.src='https://ui-avatars.com/api/?name=Empresa&background=5b5ef4&color=fff';">`
            : `<div class="card-img-placeholder"><i data-lucide="image" style="width:40px;height:40px"></i></div>`;

        const sueldo = (e.sueldo && e.sueldo > 0)
            ? `$${Number(e.sueldo).toLocaleString('es-AR')}` : 'A convenir';

        const esFav = favoritosDelUsuario.has(e.id);

        return `
        <div class="empleo-card" onclick="abrirDetalle(${e.id})">
            ${fotoHTML}
            <button class="btn-favorito ${esFav ? 'activo' : ''}" id="fav-${e.id}"
                onclick="event.stopPropagation(); toggleFavorito(${e.id})"
                title="${esFav ? 'Quitar de favoritos' : 'Guardar en favoritos'}">
                <i data-lucide="heart" style="width:16px"></i>
            </button>
            <div class="card-body">
                <div class="card-empresa">${e.empresa || 'Empresa'}</div>
                <div class="card-titulo">${e.titulo}</div>
                <div class="card-meta">
                    <span><i data-lucide="map-pin"></i>${e.ubicacion || 'Villa María'}</span>
                </div>
                <div class="sueldo-tag">${sueldo}</div>
                <button class="btn-ver">Ver detalles →</button>
            </div>
        </div>`;
    }).join('');

    if (window.lucide) lucide.createIcons();
}

async function abrirDetalle(id) {
    idEmpleoActual = id;
    try {
        const res = await fetch(`/api/empleos/${id}`);
        const e = await res.json();

        document.getElementById('modalTitulo').innerText = e.titulo;
        document.getElementById('modalEmpresa').innerText = e.empresa || '—';
        document.getElementById('modalUbicacion').innerText = e.ubicacion || '—';
        document.getElementById('modalDescripcion').innerText = e.descripcion || '';

        const foto = document.getElementById('modalFoto');
        if (e.imagenUrl) {
            foto.src = e.imagenUrl;
            foto.style.display = 'block';
        } else {
            foto.style.display = 'none';
        }

        document.getElementById('modalSueldo').innerText = (e.sueldo && e.sueldo > 0)
            ? `$${Number(e.sueldo).toLocaleString('es-AR')}` : 'Sueldo a convenir';

        document.getElementById('avisoPausado').innerHTML = !e.activo
            ? '<div class="aviso-pausado">⚠️ Esta vacante está temporalmente pausada.</div>' : '';

        // Si no hay sesión, mostrar botón de login en vez del formulario de postulación
        const postulacionBox = document.querySelector('.postulacion-box');
        if (!haySesion()) {
            postulacionBox.innerHTML = `
                <h3>¿Te interesa esta vacante?</h3>
                <p style="color:var(--muted);margin-bottom:16px">Iniciá sesión o registrate para postularte y guardar vacantes.</p>
                <div style="display:flex;gap:10px;flex-wrap:wrap">
                    <button onclick="window.location.href='login.html'" style="flex:1;background:var(--primary);color:white;border:none;padding:13px;border-radius:12px;font-weight:700;font-size:0.95rem;cursor:pointer;font-family:inherit">
                        <i data-lucide="log-in" style="width:16px;vertical-align:middle;margin-right:6px"></i> Ingresar
                    </button>
                    <button onclick="window.location.href='registro.html'" style="flex:1;background:transparent;color:var(--primary);border:1.5px solid var(--primary);padding:13px;border-radius:12px;font-weight:700;font-size:0.95rem;cursor:pointer;font-family:inherit">
                        <i data-lucide="user-plus" style="width:16px;vertical-align:middle;margin-right:6px"></i> Registrarse
                    </button>
                </div>
            `;
        } else {
            postulacionBox.innerHTML = `
                <h3>¿Te interesa esta vacante?</h3>
                <p>Subí tu CV actualizado (PDF, JPG o PNG) para aplicar directamente.</p>
                <input type="file" id="cvArchivo" accept=".pdf,.jpg,.jpeg,.png" class="file-input">
                <button class="btn-postular" onclick="enviarPostulacion()">Enviar mi Postulación ✓</button>
            `;
        }

        document.getElementById('modalDetalle').style.display = 'flex';
        setTimeout(() => iniciarMapa(e.ubicacion), 150);
        lucide.createIcons();
    } catch { toast('No se pudo cargar el detalle.', 'error'); }
}

async function iniciarMapa(direccion) {
    if (leafletMap) { leafletMap.remove(); leafletMap = null; }
    const mapaEl = document.getElementById('modalMapa');
    mapaEl.innerHTML = '';

    try {
        const q = encodeURIComponent(`${direccion}, Villa Maria, Cordoba, Argentina`);
        const geo = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${q}`);
        const data = await geo.json();
        const pos = data.length
            ? [parseFloat(data[0].lat), parseFloat(data[0].lon)]
            : [-32.4103, -63.2402];

        leafletMap = L.map('modalMapa').setView(pos, 15);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap'
        }).addTo(leafletMap);
        L.marker(pos).addTo(leafletMap);
        setTimeout(() => leafletMap && leafletMap.invalidateSize(), 300);
    } catch {
        document.getElementById('modalMapa').innerHTML =
            '<div style="display:flex;align-items:center;justify-content:center;height:100%;color:var(--muted);font-size:0.9rem;">No se pudo cargar el mapa</div>';
    }
}

function cerrarModal() {
    document.getElementById('modalDetalle').style.display = 'none';
    const cvInput = document.getElementById('cvArchivo');
    if (cvInput) cvInput.value = '';
    document.getElementById('modalFoto').src = '';
    if (leafletMap) { leafletMap.remove(); leafletMap = null; }
}

async function enviarPostulacion() {
    if (!requiereAuth()) return;

    const file = document.getElementById('cvArchivo').files[0];
    if (!file) { toast('Seleccioná tu CV antes de enviar.', 'error'); return; }

    const idUsuario = localStorage.getItem('usuarioId');

    const fd = new FormData();
    fd.append('archivoCv', file);
    fd.append('idUsuario', idUsuario);
    fd.append('idEmpleo', idEmpleoActual);

    try {
        const res = await apiFetch('/api/postulaciones/aplicar', { method: 'POST', body: fd });
        if (res.ok) {
            toast('¡Postulación enviada con éxito!', 'success');
            cerrarModal();
        } else {
            toast(await res.text(), 'error');
        }
    } catch { toast('Error de conexión.', 'error'); }
}

function irPerfil() {
    if (!requiereAuth()) return;
    window.location.href = 'perfil.html';
}

async function abrirMisPostulaciones() {
    if (!requiereAuth()) return;
    const idUsuario = localStorage.getItem('usuarioId');
    const cont = document.getElementById('listaPostulaciones');
    cont.innerHTML = '<p style="color:var(--muted)">Cargando...</p>';
    document.getElementById('modalPostulaciones').style.display = 'flex';

    try {
        const res = await apiFetch(`/api/postulaciones/mis-postulaciones/${idUsuario}`);
        const datos = await res.json();

        if (!datos.length) {
            cont.innerHTML = `
            <div style="text-align:center;padding:40px;color:var(--muted)">
                <p style="font-size:2rem;margin-bottom:8px">📋</p>
                <p>Aún no te postulaste a ninguna vacante.</p>
            </div>`;
            return;
        }

        cont.innerHTML = datos.map(p => {
            const estadoConfig = {
                'PENDIENTE': { clase: 'badge-pendiente', label: '⏳ Pendiente' },
                'EN_PROCESO': { clase: 'badge-en-proceso', label: '🔄 En proceso' },
                'CONTACTADO': { clase: 'badge-contactado', label: '✓ Te contactaron' },
                'DESCARTADO': { clase: 'badge-descartado', label: '✗ No seleccionado' },
            };
            const estado = estadoConfig[p.estadoCandidato] || estadoConfig['PENDIENTE'];
            const fecha = p.fechaPostulacion
                ? new Date(p.fechaPostulacion).toLocaleDateString('es-AR') : '';
            const mensajeExtra = {
                'CONTACTADO': '🎉 ¡Buenas noticias! La empresa quiere contactarte.',
                'DESCARTADO': 'La empresa decidió no continuar con tu postulación.',
                'EN_PROCESO': 'Tu CV está siendo revisado por la empresa.',
                'PENDIENTE': 'Esperando que la empresa revise tu postulación.',
            }[p.estadoCandidato] || '';

            const vistoBadge = p.visto
                ? '<span style="background:#dcfce7;color:#15803d;padding:2px 8px;border-radius:10px;font-size:0.72rem;font-weight:700;">👁 Leído</span>'
                : '<span style="background:#f1f5f9;color:var(--muted);padding:2px 8px;border-radius:10px;font-size:0.72rem;font-weight:700;">No visto</span>';

            return `
            <div class="postulacion-item">
                <div class="postulacion-item-top">
                    <div>
                        <div class="pi-titulo">${p.empleo?.titulo || '—'}</div>
                        <div class="pi-empresa">${p.empleo?.empresa || '—'}</div>
                    </div>
                    <span class="badge-estado ${estado.clase}">${estado.label}</span>
                </div>
                ${mensajeExtra ? `
                <div style="font-size:0.82rem;color:#475569;background:var(--bg);
                    padding:8px 12px;border-radius:8px;border-left:3px solid var(--primary)">
                    ${mensajeExtra}
                </div>` : ''}
                <div class="postulacion-item-bottom">
                    ${vistoBadge}
                    ${fecha ? `<span>· Enviado el ${fecha}</span>` : ''}
                </div>
            </div>`;
        }).join('');
    } catch { toast('Error al cargar postulaciones.', 'error'); }
}

async function verificarCambioRol() {
    const idUsuario = localStorage.getItem('usuarioId');
    const rolGuardado = localStorage.getItem('usuarioRol');
    if (!idUsuario || rolGuardado !== 'ROLE_USER') return;

    try {
        const res = await apiFetch(`/api/perfil/${idUsuario}`);
        if (!res.ok) return;
        const u = await res.json();
        if (u.tipo === 'ROLE_EMPRESA') {
            localStorage.setItem('usuarioRol', 'ROLE_EMPRESA');
            document.getElementById('modalAprobado').style.display = 'flex';
            lucide.createIcons();
        }
    } catch { }
}

function irAlPanelEmpresa() {
    localStorage.setItem('usuarioRol', 'ROLE_EMPRESA');
    window.location.href = 'pantallaAdmin.html';
}

async function cargarFavoritosIniciales() {
    const idUsuario = localStorage.getItem('usuarioId');
    if (!idUsuario) return;
    try {
        const res = await apiFetch(`/api/favoritos/usuario/${idUsuario}`);
        const lista = await res.json();
        favoritosDelUsuario = new Set(lista.map(f => f.empleo.id));
    } catch { }
}

async function toggleFavorito(idEmpleo) {
    if (!requiereAuth()) return;
    const idUsuario = localStorage.getItem('usuarioId');

    const btn = document.getElementById(`fav-${idEmpleo}`);
    const esFav = favoritosDelUsuario.has(idEmpleo);

    try {
        if (esFav) {
            const res = await apiFetch(
                `/api/favoritos?idUsuario=${idUsuario}&idEmpleo=${idEmpleo}`,
                { method: 'DELETE' }
            );
            if (res.ok) {
                favoritosDelUsuario.delete(idEmpleo);
                if (btn) btn.classList.remove('activo');
                toast('Quitado de favoritos.', 'info');
            }
        } else {
            const res = await apiFetch(
                `/api/favoritos?idUsuario=${idUsuario}&idEmpleo=${idEmpleo}`,
                { method: 'POST' }
            );
            if (res.ok) {
                favoritosDelUsuario.add(idEmpleo);
                if (btn) btn.classList.add('activo');
                toast('¡Guardado en favoritos!', 'success');
            }
        }
    } catch { toast('Error de conexión.', 'error'); }
}

async function abrirFavoritos() {
    if (!requiereAuth()) return;
    const idUsuario = localStorage.getItem('usuarioId');
    const cont = document.getElementById('listaFavoritos');
    cont.innerHTML = '<p style="color:var(--muted)">Cargando...</p>';
    document.getElementById('modalFavoritos').style.display = 'flex';

    try {
        const res = await apiFetch(`/api/favoritos/usuario/${idUsuario}`);
        const lista = await res.json();

        if (!lista.length) {
            cont.innerHTML = `
            <div style="text-align:center;padding:40px;color:var(--muted)">
                <p style="font-size:2rem;margin-bottom:8px">🤍</p>
                <p>Todavía no guardaste ninguna vacante.</p>
                <p style="font-size:0.85rem;margin-top:6px">Tocá el corazón en cada aviso para guardarlo acá.</p>
            </div>`;
            return;
        }

        cont.innerHTML = lista.map(f => {
            const e = f.empleo;
            const sueldo = (e.sueldo && e.sueldo > 0)
                ? `$${Number(e.sueldo).toLocaleString('es-AR')}` : 'A convenir';

            return `
            <div class="favorito-card">
                <div style="flex:1">
                    <div class="favorito-titulo">${e.titulo}</div>
                    <div class="favorito-empresa">${e.empresa || '—'} · ${e.ubicacion || 'Villa María'}</div>
                    <div class="favorito-sueldo">${sueldo}</div>
                </div>
                <div style="display:flex;flex-direction:column;gap:8px;align-items:flex-end">
                    <button class="btn-ver" style="padding:8px 14px;font-size:0.82rem;width:auto"
                        onclick="document.getElementById('modalFavoritos').style.display='none';abrirDetalle(${e.id})">
                        Ver →
                    </button>
                    <button onclick="toggleFavorito(${e.id});this.closest('.favorito-card').remove()"
                        style="background:none;border:none;color:var(--danger);font-size:0.78rem;cursor:pointer;font-family:inherit;font-weight:600">
                        🗑 Quitar
                    </button>
                </div>
            </div>`;
        }).join('');
    } catch { toast('Error al cargar favoritos.', 'error'); }
}
