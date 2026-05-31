let map;
let idEmpleoActual = null;

function toast(msg, tipo = 'info') {
    const el = document.createElement('div');
    el.className = `toast ${tipo}`;
    const icon = tipo === 'success' ? '✓' : tipo === 'error' ? '✗' : 'ℹ';
    el.innerHTML = `<span>${icon}</span><span>${msg}</span>`;
    const cont = document.getElementById('toast-container');
    if (cont) cont.appendChild(el);
    setTimeout(() => el.remove(), 4000);
}

async function abrirDetalle(id) {
    try {
        idEmpleoActual = id;
        const response = await fetch(`/api/empleos/${id}`);
        if (!response.ok) throw new Error("No se encontró el empleo");

        const emp = await response.json();

        document.getElementById('tituloPuesto').innerText = emp.titulo;
        document.getElementById('nombreEmpresa').innerText = emp.empresa;
        document.getElementById('ubicacionTexto').innerText = emp.ubicacion;
        document.getElementById('descripcionLarga').innerText = emp.descripcion;

        const fotoModal = document.getElementById('modalFoto');
        if (emp.imagenUrl && emp.imagenUrl !== "null") {
            fotoModal.src = emp.imagenUrl;
            fotoModal.style.display = 'block';
        } else {
            fotoModal.style.display = 'none';
        }

        const sueldoSpan = document.getElementById('sueldoTexto');
        sueldoSpan.innerText = (emp.sueldo && emp.sueldo > 0)
            ? `$${emp.sueldo.toLocaleString('es-AR')}`
            : "Sueldo a convenir";

        if (emp.ubicacion) {
            setTimeout(() => inicializarMapaGratuito(emp.ubicacion), 100);
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
        toast("Por favor, seleccioná tu archivo de CV.", "error");
        return;
    }

    const idUsuario = localStorage.getItem('usuarioId');
    if (!idUsuario) {
        toast("Debés iniciar sesión para postularte.", "error");
        return;
    }

    const formData = new FormData();
    formData.append('archivoCv', fileInput.files[0]);
    formData.append('idUsuario', idUsuario);
    formData.append('idEmpleo', idEmpleoActual);

    try {
        const response = await fetch('/api/postulaciones/aplicar', {
            method: 'POST',
            body: formData
        });

        if (response.ok) {
            toast("¡Postulación enviada con éxito!", "success");
            cerrarModal();
        } else {
            const errorMsg = await response.text();
            toast("Error: " + errorMsg, "error");
        }
    } catch (error) {
        console.error("Error de red:", error);
        toast("No se pudo conectar con el servidor.", "error");
    }
}

function cerrarModal() {
    document.getElementById('cvArchivo').value = "";
    document.getElementById('modalFoto').src = "";
}
