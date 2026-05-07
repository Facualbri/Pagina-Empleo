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
        renderizarTarjetas(empleos);
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
        renderizarTarjetas(empleos);
    } catch (error) {
        console.error("Error en la búsqueda:", error);
    }
}

function renderizarTarjetas(listaEmpleos) {
    const contenedor = document.getElementById('resultados');
    if (!contenedor) return;
    contenedor.innerHTML = "";

    listaEmpleos.forEach(empleo => {
        // --- CORRECCIÓN DE RUTA AQUÍ ---
        // Si hay imagenUrl, armamos la ruta correcta, si no, usamos el placeholder
        const fotoURL = empleo.imagenUrl 
            ? `/uploads/fotos/${empleo.imagenUrl}` 
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