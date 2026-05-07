let map;
let idEmpleoActual = null;

async function abrirDetalle(id) {
    try {
        idEmpleoActual = id; 
        const response = await fetch(`/api/empleos/${id}`);
        if (!response.ok) throw new Error("No se encontró el empleo");
        
        const emp = await response.json();

        document.getElementById('modalTitulo').innerText = emp.titulo;
        document.getElementById('modalEmpresa').innerText = emp.empresa;
        document.getElementById('modalUbicacion').innerText = emp.ubicacion;
        document.getElementById('modalDescripcion').innerText = emp.descripcion;
        
        const fotoModal = document.getElementById('modalFoto');
        
        // --- CORRECCIÓN DE RUTA AQUÍ ---
        if (emp.imagenUrl && emp.imagenUrl !== "null") {
            // Agregamos el prefijo de la carpeta que configuramos en WebConfig
            fotoModal.src = '/uploads/fotos/' + emp.imagenUrl;
            fotoModal.style.display = 'block';
        } else {
            fotoModal.src = 'https://via.placeholder.com/400x200?text=Logo+no+disponible';
            fotoModal.style.display = 'block';
        }

        // --- LÓGICA DE SUELDO ---
        const sueldoSpan = document.getElementById('modalSueldo');
        sueldoSpan.innerText = (emp.sueldo && emp.sueldo > 0) 
            ? `$${emp.sueldo.toLocaleString('es-AR')}` 
            : "Sueldo a convenir";

        document.getElementById('modalDetalle').style.display = 'flex';

        if (emp.ubicacion) {
            setTimeout(() => {
                inicializarMapaGratuito(emp.ubicacion);
            }, 100);
        }

        if (window.lucide) lucide.createIcons();

    } catch (error) {
        console.error("Error al cargar el detalle:", error);
    }
}
async function inicializarMapaGratuito(direccion) {
    if (map) { map.remove(); map = null; }
    try {
        const query = encodeURIComponent(`${direccion}, Villa Maria, Cordoba, Argentina`);
        const geoRes = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${query}`);
        const geoData = await geoRes.json();
        let pos = geoData.length > 0 ? [geoData[0].lat, geoData[0].lon] : [-32.4103, -63.2402];

        map = L.map('modalMapa').setView(pos, 16);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
        L.marker(pos).addTo(map);
        setTimeout(() => map.invalidateSize(), 200);
    } catch (e) { console.error("Error mapa:", e); }
}

async function enviarPostulacionFinal() {
    const fileInput = document.getElementById('cvArchivo');
    if (fileInput.files.length === 0) {
        alert("Por favor, seleccioná tu archivo de CV.");
        return;
    }
    const formData = new FormData();
    formData.append('archivoCv', fileInput.files[0]);
    formData.append('empleoId', idEmpleoActual);

    try {
        const response = await fetch('/api/postulaciones/enviar', { method: 'POST', body: formData });
        if (response.ok) {
            alert("¡Postulación enviada!");
            cerrarModal();
        } else {
            alert("Error al enviar postulación.");
        }
    } catch (error) { console.error(error); }
}

function cerrarModal() {
    document.getElementById('modalDetalle').style.display = 'none';
    document.getElementById('cvArchivo').value = "";
    // Limpiamos la foto al cerrar para que no parpadee la anterior al abrir otro empleo
    document.getElementById('modalFoto').src = "";
}