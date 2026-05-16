document.addEventListener("DOMContentLoaded", () => {
    const nombre = localStorage.getItem('usuarioNombre');
    const nombreUserElement = document.getElementById('nombreUser');
    if (nombreUserElement) {
        nombreUserElement.innerText = nombre ? `Hola, ${nombre}` : "Invitado";
    }
    cargarOfertas();
});

async function cargarOfertas() {
    const contenedor = document.getElementById('resultados');
    try {
        const response = await fetch('/api/empleos');
        const empleos = await response.json();

        // --- FILTRO: Solo pasamos los activos a renderizar ---
        const activos = empleos.filter(e => e.activo === true);
        renderizarTarjetas(activos);

    } catch (error) {
        console.error("Error al obtener empleos:", error);
        if (contenedor) contenedor.innerHTML = "<p>Error al cargar las ofertas.</p>";
    }
}

async function buscar() {
    const titulo = document.getElementById('busqueda').value;
    try {
        const response = await fetch(`/api/empleos/buscar?titulo=${encodeURIComponent(titulo)}`);
        const empleos = await response.json();

        // --- FILTRO: También filtramos en la búsqueda ---
        const activos = empleos.filter(e => e.activo === true);
        renderizarTarjetas(activos);

    } catch (error) {
        console.error("Error en la búsqueda:", error);
    }
}

function renderizarTarjetas(listaEmpleos) {
    const contenedor = document.getElementById('resultados');
    if (!contenedor) return;
    contenedor.innerHTML = "";

    // Si después de filtrar no queda nada
    if (listaEmpleos.length === 0) {
        contenedor.innerHTML = "<p style='text-align:center; grid-column: 1/-1; padding: 20px;'>No hay ofertas disponibles por el momento.</p>";
        return;
    }

    listaEmpleos.forEach(empleo => {
        const fotoURL = empleo.imagenUrl 
            ? empleo.imagenUrl 
            : 'https://placehold.co/400x200?text=Sin+Imagen';

        const card = `
            <div class="oferta-card">
                <div class="card-image-container" style="width: 100%; height: 180px; background: #eee; overflow: hidden;">
                    <img src="${fotoURL}" 
                         onerror="this.onerror=null; this.src='https://placehold.co/400x200?text=Error+Carga';" 
                         style="width:100%; height:100%; object-fit:cover;">
                </div>
                <div style="padding: 15px;">
                    <h3 style="margin: 0 0 10px 0; color: #1e3c72;">${empleo.titulo}</h3>
                    <p style="margin: 5px 0;"><strong>${empleo.empresa}</strong></p>
                    <p style="margin: 5px 0; color: #666;">📍 ${empleo.ubicacion}</p>
                    <button class="btn-primary" onclick="abrirDetalle(${empleo.id})"
                            style="width: 100%; background: #1e3c72; color: white; border: none; padding: 10px; border-radius: 5px; cursor: pointer; margin-top: 10px; font-weight: bold;">
                        Ver detalles
                    </button>
                </div>
            </div>
        `;
        contenedor.innerHTML += card;
    });
    if (window.lucide) lucide.createIcons();
}

function cerrarSesion() {
    localStorage.clear();
    window.location.href = "login.html";
}

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

function cerrarModalPostulaciones() {
    document.getElementById('modalMisPostulaciones').style.display = 'none';
}
