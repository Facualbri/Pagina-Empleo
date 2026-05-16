document.addEventListener("DOMContentLoaded", () => {
    const idEmpresa = localStorage.getItem('usuarioId');
    const nombreEmpresa = localStorage.getItem('usuarioNombre');
    if (idEmpresa) {
        if (document.getElementById('nombreEmpresaHeader')) document.getElementById('nombreEmpresaHeader').innerText = nombreEmpresa;
        cargarMisEmpleos(idEmpresa);
    } else { window.location.href = "login.html"; }
});

async function publicarAviso() {
    // Capturamos los datos
    const titulo = document.getElementById('titulo').value;
    const descripcion = document.getElementById('descripcion').value;
    const ubicacion = document.getElementById('ubicacion').value;
    const sueldo = document.getElementById('sueldo').value;
    const fotoInput = document.getElementById('fotoArchivo');

    const idEmpresa = localStorage.getItem('usuarioId');
    const nombreEmpresa = localStorage.getItem('usuarioNombre');

    // Validación básica para que no tire 400
    if (!titulo || !descripcion || !sueldo) {
        alert("⚠️ Completá título, descripción y sueldo.");
        return;
    }

    const formData = new FormData();
    formData.append('titulo', titulo);
    formData.append('descripcion', descripcion);
    formData.append('ubicacion', ubicacion);
    formData.append('sueldo', sueldo); // IMPORTANTE: que coincida con Java
    formData.append('idUsuario', idEmpresa);
    formData.append('empresa', nombreEmpresa);

    if (fotoInput.files[0]) {
        formData.append('archivo', fotoInput.files[0]);
    }

    try {
        const response = await apiFetch('/api/empleos/con-foto', {
            method: 'POST',
            body: formData
        });

        if (response.ok) {
            alert("✅ Publicado con éxito!");
            location.reload();
        } else {
            const errorText = await response.text();
            alert("❌ Error: " + errorText);
        }
    } catch (e) { console.error(e); }
}

async function cargarMisEmpleos(idEmpresa) {
    const tabla = document.getElementById('tablaOfertas');
    if (!tabla) return;
    try {
        const response = await apiFetch('/api/empleos');
        const lista = await response.json();
        const misEmpleos = lista.filter(e => String(e.idUsuario) === String(idEmpresa));

        tabla.innerHTML = "";
        misEmpleos.forEach(empleo => {
            const estadoTexto = empleo.activo ? 'ACTIVA' : 'PAUSADA';
            const estadoClase = empleo.activo ? 'badge-activa' : 'badge-pausada';
            const total = empleo.cantidadPostulantes || 0;

            tabla.innerHTML += `
            <tr>
                <td><strong>${empleo.titulo}</strong></td>
                <td><small>${empleo.ubicacion}</small></td>
                <td>$${Number(empleo.sueldo).toLocaleString('es-AR')}</td>
                <td><span class="${estadoClase}">${estadoTexto}</span></td>
                <td>
                    <div style="display: flex; gap: 5px; justify-content: flex-end;">
                        <button class="btn-tabla btn-azul" onclick="verPostulantes(${empleo.id}, '${empleo.titulo}')">
                            Candidatos (${total})
                        </button>
                        <button class="btn-tabla btn-gris" onclick="cambiarEstadoVacante(${empleo.id})">
                            ${empleo.activo ? 'Pausar' : 'Activar'}
                        </button>
                        <button class="btn-tabla btn-rojo" onclick="eliminarEmpleo(${empleo.id})">
                            Borrar
                        </button>
                    </div>
                </td>
            </tr>`;
        });
    } catch (e) { console.error(e); }
}

async function cambiarEstadoVacante(id) {
    await fetch(`/api/empleos/${id}/cambiar-estado`, { method: 'PATCH' });
    cargarMisEmpleos(localStorage.getItem('usuarioId'));
}

async function eliminarEmpleo(id) {
    if (confirm("¿Borrar?")) {
        const response = await fetch(`/api/empleos/${id}`, { method: 'DELETE' });
        if (response.ok) {
            alert("✅ Empleo eliminado correctamente.");
            location.reload(); // Recarga la página para actualizar la lista
        } else {
            // Asumimos que el backend devuelve un JSON con un campo 'error'
            const errorData = await response.json(); 
            alert("❌ Error al eliminar el empleo: " + (errorData.error || "Error desconocido."));
            console.error("Error al eliminar empleo:", errorData);
            // No recargamos la página para que el usuario pueda ver el mensaje de error
        }
    }
}

async function verPostulantes(id, titulo) {
    const res = await apiFetch(`/api/postulaciones/por-empleo/${id}`);
    const postulaciones = await res.json();
    const contenedor = document.getElementById('listaPostulantes');
    document.getElementById('modalTituloPuesto').innerText = titulo;
    contenedor.innerHTML = "";
    postulaciones.forEach(p => {
        contenedor.innerHTML += `
            <div class="postulante-card">
                <div><strong>${p.postulante.nombre}</strong></div>
                <div class="card-actions">
                    <a href="${p.archivoCv}" target="_blank" class="btn-cv-view">CV</a>
                </div>
            </div>`;
    });
    document.getElementById('modalPostulantes').style.display = 'flex';
}

function cerrarModal() { document.getElementById('modalPostulantes').style.display = 'none'; }
function cerrarSesion() { localStorage.clear(); window.location.href = "login.html"; }

async function verMisPostulaciones() {
    const idUsuario = localStorage.getItem('usuarioId');
    const contenedor = document.getElementById('listaMisPostulaciones');
    const modal = document.getElementById('modalMisPostulaciones');

    try {
        const response = await fetch(`/api/postulaciones/mis-postulaciones/${idUsuario}`);
        const datos = await response.json();

        contenedor.innerHTML = "";

        if (datos.length === 0) {
            contenedor.innerHTML = "<p>Aún no te postulaste a ninguna vacante.</p>";
        }

        datos.forEach(p => {
            // Lógica de "Visto"
            const estadoTexto = p.visto ? "Leído por la empresa" : "Enviado (Pendiente)";
            const estadoClase = p.visto ? "estado-visto" : "estado-pendiente";

            contenedor.innerHTML += `
                <div class="postulante-card">
                    <div class="p-info">
                        <h4>${p.empleo.titulo}</h4>
                        <p>Empresa: ${p.empleo.empresa}</p>
                        <span class="badge-estado ${estadoClase}">${estadoTexto}</span>
                    </div>
                </div>
            `;
        });

        modal.style.display = 'flex';
    } catch (error) {
        console.error("Error:", error);
        alert("No se pudieron cargar tus postulaciones.");
    }
}
