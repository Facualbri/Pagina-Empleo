document.addEventListener("DOMContentLoaded", () => {
    // 1. Verificación de sesión y carga inicial
    const nombreEmpresa = localStorage.getItem('usuarioNombre');
    const idEmpresa = localStorage.getItem('usuarioId');

    if (nombreEmpresa && idEmpresa) {
        const header = document.getElementById('nombreEmpresaHeader');
        if (header) header.innerText = nombreEmpresa;
        
        // Cargamos las vacantes de esta empresa
        cargarMisEmpleos(idEmpresa);
    } else {
        window.location.href = "login.html";
    }
});

// FUNCIÓN: Publicar nuevo aviso
async function publicarAviso() {
    const titulo = document.getElementById('titulo').value;
    const descripcion = document.getElementById('descripcion').value;
    const ubicacion = document.getElementById('ubicacion').value;
    const sueldo = document.getElementById('sueldo').value;
    const fotoInput = document.getElementById('fotoArchivo');

    // USAMOS LAS VARIABLES QUE YA TENÉS EN EL STORAGE
    const idEmpresa = localStorage.getItem('usuarioId');
    const nombreEmpresa = localStorage.getItem('usuarioNombre');

    // Si por alguna razón se borraron, frenamos acá
    if (!idEmpresa) {
        alert("⚠️ Error: No se encontró la sesión. Por favor, iniciá sesión de nuevo.");
        window.location.href = "login.html";
        return;
    }

    const formData = new FormData();
    formData.append('titulo', titulo);
    formData.append('descripcion', descripcion);
    formData.append('ubicacion', ubicacion);
    formData.append('sueldo', sueldo);
    formData.append('idUsuario', idEmpresa); // Mandamos el ID directo
    formData.append('empresa', nombreEmpresa); 

    if (fotoInput.files && fotoInput.files[0]) {
        formData.append('archivo', fotoInput.files[0]);
    }

    try {
        const response = await fetch('/api/empleos/con-foto', {
            method: 'POST',
            body: formData 
        });

        if (response.ok) {
            alert("✅ ¡Vacante publicada con éxito!");
            location.reload(); 
        } else {
            const errorText = await response.text();
            alert("❌ Error al publicar: " + errorText);
        }
    } catch (error) {
        console.error("Error crítico:", error);
        alert("❌ Error de conexión con el servidor.");
    }
}

// FUNCIÓN: Cargar solo los empleos de esta empresa
async function cargarMisEmpleos(idEmpresa) {
    const tabla = document.getElementById('tablaOfertas');
    if (!tabla) return;

    try {
        const response = await fetch('/api/empleos');
        const empleos = await response.json();

        // Filtramos comparando strings para no tener líos de tipos
        const misEmpleos = empleos.filter(e => String(e.idUsuario) === String(idEmpresa));

        tabla.innerHTML = "";

        if (misEmpleos.length === 0) {
            tabla.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:20px;">No tienes vacantes publicadas aún.</td></tr>`;
            return;
        }

        misEmpleos.forEach(empleo => {
            const sueldoFormateado = empleo.sueldo ? `$${Number(empleo.sueldo).toLocaleString('es-AR')}` : "A convenir";

            tabla.innerHTML += `
            <tr>
                <td><strong>${empleo.titulo}</strong></td>
                <td>${empleo.ubicacion}</td>
                <td>${sueldoFormateado}</td>
                <td><span style="color: #28a745; font-weight: bold;">Activa</span></td>
                <td>
                    <div style="display: flex; gap: 8px; justify-content: flex-end;">
                        <button class="btn-ver" onclick="verPostulantes(${empleo.id}, '${empleo.titulo}')">
                            Postulantes
                        </button>
                        <button class="btn-ver" 
                                style="background: #fee2e2; color: #ef4444; border-color: #fecaca;" 
                                onclick="eliminarEmpleo(${empleo.id})">
                            Eliminar
                        </button>
                    </div>
                </td>
            </tr>`;
        });
    } catch (error) {
        console.error("Error al cargar empleos:", error);
        tabla.innerHTML = "<tr><td colspan='5' style='color:red;'>Error al conectar con el servidor.</td></tr>";
    }
}

// FUNCIÓN: Ver postulantes
async function verPostulantes(idEmpleo, tituloPuesto) {
    const listaUI = document.getElementById('listaPostulantes');
    const modal = document.getElementById('modalPostulantes');
    if (!modal) return;

    document.querySelector('.modal-header-admin h3').innerText = `Candidatos para: ${tituloPuesto}`;

    try {
        const response = await fetch(`/api/postulaciones/por-empleo/${idEmpleo}`);
        const postulaciones = await response.json();

        listaUI.innerHTML = "";
        if (postulaciones.length === 0) {
            listaUI.innerHTML = "<li style='padding:20px; color:#888; text-align:center;'>Aún no hay postulantes.</li>";
        } else {
            postulaciones.forEach(p => {
                listaUI.innerHTML += `
                    <li style="display:flex; justify-content:space-between; align-items:center; padding:12px; border-bottom:1px solid #eee;">
                        <div>
                            <span style="display:block; font-weight:bold; color:#1e3c72;">${p.postulante.nombre}</span>
                            <span style="font-size:0.85rem; color:#666;">${p.postulante.email}</span>
                        </div>
                        <a href="mailto:${p.postulante.email}" class="btn-ver" style="text-decoration:none; font-size:0.8rem;">
                            Contactar
                        </a>
                    </li>`;
            });
        }
        modal.style.display = "block";
    } catch (error) {
        alert("Error al cargar postulantes.");
    }
}

// FUNCIÓN: Eliminar
async function eliminarEmpleo(id) {
    if (confirm("¿Estás seguro de que querés eliminar esta vacante?")) {
        try {
            const response = await fetch(`/api/empleos/${id}`, {
                method: 'DELETE'
            });

            if (response.ok) {
                alert("✅ Vacante eliminada correctamente.");
                location.reload();
            } else {
                alert("❌ No se pudo eliminar. Es posible que tenga postulaciones activas.");
            }
        } catch (error) {
            console.error("Error de red:", error);
            alert("Error de conexión con el servidor.");
        }
    }
}

function cerrarModal() {
    document.getElementById('modalPostulantes').style.display = "none";
}

function cerrarSesion() {
    localStorage.clear();
    window.location.href = "login.html";
}