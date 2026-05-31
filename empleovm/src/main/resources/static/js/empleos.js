function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#039;');
}

async function buscar() {
    const texto = document.getElementById('busqueda').value;
    const resultadosDiv = document.getElementById('resultados');

    try {
        const response = await fetch(`/api/empleos/buscar?titulo=${encodeURIComponent(texto)}&soloActivos=true`);
        const empleos = await response.json();

        resultadosDiv.innerHTML = "";

        if (empleos.length === 0) {
            resultadosDiv.innerHTML = "<p style='color:#64748b;text-align:center;padding:20px;'>No se encontraron vacantes.</p>";
            return;
        }

        empleos.forEach(emp => {
            resultadosDiv.innerHTML += `
                <div class="empleo-item">
                    <h3>${escapeHtml(emp.titulo)}</h3>
                    <p>📍 ${escapeHtml(emp.ubicacion)}</p>
                    <button onclick="window.location.href='detalleEmpleo.html?id=${emp.id}'" class="btn-ver-detalle">
                        Ver detalle
                    </button>
                </div>
            `;
        });
    } catch (error) {
        console.error("Error al buscar:", error);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    const params = new URLSearchParams(window.location.search);
    const id = params.get('id');
    if (id) {
        window.location.href = `detalleEmpleo.html?id=${id}`;
    }
    buscar();
});
