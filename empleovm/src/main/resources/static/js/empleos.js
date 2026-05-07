async function buscar() {
    const texto = document.getElementById('busqueda').value;
    const resultadosDiv = document.getElementById('resultados');

    try {
        const response = await fetch(`/api/empleos/buscar?titulo=${texto}`);
        const empleos = await response.json();

        resultadosDiv.innerHTML = ""; 

        if (empleos.length === 0) {
            resultadosDiv.innerHTML = "<p>No se encontraron vacantes.</p>";
            return;
        }

        empleos.forEach(emp => {
    resultadosDiv.innerHTML += `
        <div class="empleo-item">
            <h3>${emp.titulo}</h3>
            <p>📍 ${emp.ubicacion}</p>
            
            <!-- Cambiamos el link por un botón que llama a abrirDetalle -->
            <button onclick="abrirDetalle(${emp.id})" class="btn-postular">
                Postularme
            </button>
        </div>
    `;
});
    } catch (error) {
        console.error("Error al buscar:", error);
    }
}
document.addEventListener("DOMContentLoaded", buscar);