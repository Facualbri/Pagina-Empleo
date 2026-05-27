function authHeaders() {
    const token = localStorage.getItem('token');
    return token ? { 'Authorization': 'Bearer ' + token } : {};
}

function toast(msg, tipo = 'info') {
    const el = document.createElement('div');
    el.className = `toast ${tipo}`;
    const icon = tipo === 'success' ? '✓' : tipo === 'error' ? '✗' : 'ℹ';
    el.innerHTML = `<span>${icon}</span><span>${msg}</span>`;
    document.getElementById('toast-container').appendChild(el);
    setTimeout(() => el.remove(), 4000);
}

function contarCaracteres(idTextarea, idSpan, max) {
    const val = document.getElementById(idTextarea).value.length;
    document.getElementById(idSpan).innerText = val;
    if (val > max) document.getElementById(idTextarea).value =
        document.getElementById(idTextarea).value.substring(0, max);
}

const idUsuario = localStorage.getItem('usuarioId');

document.addEventListener('DOMContentLoaded', () => {
    if (!idUsuario) { window.location.href = 'login.html'; return; }
    cargarPerfil();
    verificarEstadoSolicitud();

    const rol = localStorage.getItem('usuarioRol');
    if (rol === 'ROLE_EMPRESA') {
        const btn = document.getElementById('btnVolverAdmin');
        if (btn) btn.style.display = 'flex';
    }
});

async function cargarPerfil() {
    try {
        const res = await fetch(`/api/perfil/${idUsuario}`, {
            headers: authHeaders()
        });
        if (!res.ok) throw new Error('No se pudo cargar el perfil.');
        const u = await res.json();

        localStorage.setItem('usuarioRol', u.tipo);

        const btnAdmin = document.getElementById('btnVolverAdmin');
        const seccionSolicitud = document.getElementById('seccionSolicitudEmpresa');

        if (u.tipo === 'ROLE_EMPRESA') {
            if (btnAdmin) btnAdmin.style.display = 'flex';
            if (seccionSolicitud) seccionSolicitud.style.display = 'none';
        } else {
            if (btnAdmin) btnAdmin.style.display = 'none';
            if (seccionSolicitud) {
                seccionSolicitud.style.display = 'block';
                const btnSolicitar = document.getElementById('btnSolicitarEmpresa');
                if (u.estadoSolicitud === 'PENDIENTE' && btnSolicitar) {
                    btnSolicitar.disabled = true;
                    btnSolicitar.innerHTML = '<i data-lucide="clock" style="width:16px"></i> Solicitud en revisión...';
                    btnSolicitar.style.background = '#94a3b8';
                    btnSolicitar.style.cursor = 'not-allowed';
                }
            }
        }

        document.getElementById('heroNombre').innerText = u.nombre || '—';
        document.getElementById('heroEmail').innerText = u.email || '—';

        if (u.localidad) {
            document.getElementById('localidadTexto').innerText = u.localidad;
            document.getElementById('heroLocalidad').style.display = 'flex';
        }
        if (u.telefono) {
            document.getElementById('telefonoTexto').innerText = u.telefono;
            document.getElementById('heroTelefono').style.display = 'flex';
        }

        const esEmpresa = u.tipo === 'ROLE_EMPRESA';
        document.getElementById('chipTipo').innerHTML = esEmpresa
            ? '<i data-lucide="building-2" style="width:13px"></i> Empresa'
            : '<i data-lucide="user" style="width:13px"></i> Candidato';

        if (u.fotoPerfil) {
            const img = document.getElementById('fotoImg');
            img.src = u.fotoPerfil;
            img.style.display = 'block';
            document.getElementById('fotoPlaceholder').style.display = 'none';
        }

        document.getElementById('nombre').value = u.nombre || '';
        document.getElementById('email').value = u.email || '';
        document.getElementById('telefono').value = u.telefono || '';
        document.getElementById('localidad').value = u.localidad || '';
        document.getElementById('descripcion').value = u.descripcion || '';
        document.getElementById('experiencia').value = u.experiencia || '';

        contarCaracteres('descripcion', 'countDesc', 600);
        contarCaracteres('experiencia', 'countExp', 1200);

        calcularCompletitud(u);
        lucide.createIcons();

    } catch (err) {
        toast('Error al cargar el perfil.', 'error');
    }
}

function calcularCompletitud(u) {
    const campos = [u.nombre, u.email, u.fotoPerfil, u.descripcion, u.experiencia, u.telefono, u.localidad];
    const llenos = campos.filter(c => c && c.trim && c.trim() !== '').length;
    const pct = Math.round((llenos / campos.length) * 100);
    document.getElementById('pctLabel').innerText = pct + '%';
    document.getElementById('barraFill').style.width = pct + '%';
}

async function guardarDatos() {
    const datos = {
        nombre: document.getElementById('nombre').value.trim(),
        email: document.getElementById('email').value.trim(),
        telefono: document.getElementById('telefono').value.trim(),
        localidad: document.getElementById('localidad').value.trim(),
        descripcion: document.getElementById('descripcion').value.trim(),
        experiencia: document.getElementById('experiencia').value.trim(),
    };

    if (!datos.nombre) { toast('El nombre no puede estar vacío.', 'error'); return; }
    if (!datos.email || !datos.email.includes('@')) { toast('Ingresá un email válido.', 'error'); return; }

    try {
        const res = await fetch(`/api/perfil/${idUsuario}/datos`, {
            method: 'PATCH',
            headers: { ...authHeaders(), 'Content-Type': 'application/json' },
            body: JSON.stringify(datos)
        });
        if (res.ok) {
            const u = await res.json();
            localStorage.setItem('usuarioNombre', u.nombre);
            document.getElementById('heroNombre').innerText = u.nombre;
            document.getElementById('heroEmail').innerText = u.email;
            if (u.localidad) {
                document.getElementById('localidadTexto').innerText = u.localidad;
                document.getElementById('heroLocalidad').style.display = 'flex';
            }
            if (u.telefono) {
                document.getElementById('telefonoTexto').innerText = u.telefono;
                document.getElementById('heroTelefono').style.display = 'flex';
            }
            calcularCompletitud(u);
            toast('¡Perfil actualizado correctamente!', 'success');
        } else {
            const err = await res.text();
            toast(err || 'Error al guardar.', 'error');
        }
    } catch {
        toast('Error de conexión.', 'error');
    }
}

async function subirFoto(input) {
    const file = input.files[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
        toast('La imagen no puede superar los 5 MB.', 'error'); return;
    }

    const reader = new FileReader();
    reader.onload = e => {
        document.getElementById('fotoImg').src = e.target.result;
        document.getElementById('fotoImg').style.display = 'block';
        document.getElementById('fotoPlaceholder').style.display = 'none';
    };
    reader.readAsDataURL(file);

    const fd = new FormData();
    fd.append('foto', file);

    try {
        const res = await fetch(`/api/perfil/${idUsuario}/foto`, {
            method: 'POST',
            headers: authHeaders(),
            body: fd
        });
        if (res.ok) {
            toast('¡Foto de perfil actualizada!', 'success');
        } else {
            const err = await res.json().catch(() => ({}));
            toast(err.error || 'Error al subir la foto.', 'error');
        }
    } catch {
        toast('Error de conexión.', 'error');
    }
}

async function cambiarPassword() {
    const passwordActual = document.getElementById('passActual').value;
    const passwordNueva = document.getElementById('passNueva').value;
    const passwordConfirm = document.getElementById('passConfirm').value;

    if (!passwordActual || !passwordNueva || !passwordConfirm) {
        toast('Completá todos los campos de contraseña.', 'error'); return;
    }
    if (passwordNueva !== passwordConfirm) {
        toast('Las contraseñas nuevas no coinciden.', 'error'); return;
    }
    if (passwordNueva.length < 6) {
        toast('La contraseña debe tener al menos 6 caracteres.', 'error'); return;
    }

    try {
        const res = await fetch(`/api/perfil/${idUsuario}/password`, {
            method: 'PATCH',
            headers: { ...authHeaders(), 'Content-Type': 'application/json' },
            body: JSON.stringify({ passwordActual, passwordNueva, passwordConfirm })
        });
        if (res.ok) {
            toast('¡Contraseña actualizada correctamente!', 'success');
            ['passActual', 'passNueva', 'passConfirm'].forEach(id => document.getElementById(id).value = '');
        } else {
            const err = await res.json().catch(() => ({}));
            toast(err.error || 'Error al cambiar la contraseña.', 'error');
        }
    } catch {
        toast('Error de conexión.', 'error');
    }
}

function cerrarSesion() {
    localStorage.clear();
    window.location.href = 'login.html';
}

async function enviarSolicitudEmpresa() {
    const id = localStorage.getItem('usuarioId');

    if (!id || id === "null" || id === "undefined") {
        toast("No se detectó tu sesión. Volvé a iniciar sesión.", "error");
        return;
    }

    if (!confirm("¿Querés enviar la solicitud para convertir tu cuenta a Empresa?")) return;

    try {
        const res = await fetch(`/api/usuarios/${id}/solicitar-empresa`, {
            method: 'PUT',
            headers: authHeaders()
        });
        const data = await res.json();

        if (res.ok) {
            toast("¡Solicitud enviada! Los administradores la revisarán pronto.", "success");
            const btn = document.getElementById('btnSolicitarEmpresa');
            if (btn) {
                btn.disabled = true;
                btn.innerHTML = '<i data-lucide="clock" style="width:16px"></i> Solicitud en revisión...';
                btn.style.background = "#94a3b8";
                btn.style.cursor = "not-allowed";
                lucide.createIcons();
            }
        } else {
            toast(data.error || "Error al enviar la solicitud.", "error");
        }
    } catch (err) {
        toast("No se pudo conectar con el servidor.", "error");
    }
}

async function verificarEstadoSolicitud() {
    const id = localStorage.getItem('usuarioId');
    if (!id) return;
    try {
        const res = await fetch(`/api/perfil/${id}`, {
            headers: authHeaders()
        });
        const u = await res.json();
        const btn = document.getElementById('btnSolicitarEmpresa');
        const seccion = document.getElementById('seccionSolicitudEmpresa');

        if (u.tipo === 'ROLE_EMPRESA') {
            if (seccion) seccion.style.display = 'none';
            return;
        }
        if (u.estadoSolicitud === 'PENDIENTE' && btn) {
            btn.disabled = true;
            btn.innerHTML = '<i data-lucide="clock" style="width:16px"></i> Solicitud en revisión...';
            btn.style.background = "#94a3b8";
            btn.style.cursor = "not-allowed";
            lucide.createIcons();
        }
    } catch (e) { }
}
