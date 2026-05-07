
async function registrarNuevo() {
    const nombre = document.getElementById('nombre').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const msg = document.getElementById('msg');

    // Validamos que no haya campos vacíos
    if (!nombre || !email || !password) {
        msg.innerText = "⚠️ Por favor, completa todos los campos.";
        msg.style.color = "red";
        return;
    }

    // registro.js - Línea 20 aproximadamente
    const usuarioDTO = {
        nombre: nombre,
        email: email,
        password: password,
        tipo: (rolSeleccionado === 'EMPRESA') ? 'ROLE_EMPRESA' : 'ROLE_USER' // ✅
    };
    try {
        console.log("Enviando registro:", JSON.stringify(usuarioDTO));

        const response = await fetch('/api/usuarios', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(usuarioDTO)
        });

        if (response.ok) {
            // Mensaje personalizado según el rol
            if (rolSeleccionado === 'EMPRESA') {
                msg.innerText = "✅ Solicitud enviada. Un administrador revisará tu empresa pronto.";
                msg.style.color = "#0f172a";
            } else {
                msg.innerText = "✅ ¡Registro exitoso! Ya podés iniciar sesión.";
                msg.style.color = "green";
            }

            // Limpiamos los campos
            document.querySelectorAll('input').forEach(i => i.value = '');

            // Redirigir al login después de un momento
            setTimeout(() => {
                window.location.href = "login.html";
            }, 3500);

        } else {
            // ESTO ES CLAVE: Vamos a ver qué dice el servidor
            const textoError = await response.text();
            console.error("EL SERVIDOR DICE:", textoError);
            msg.innerText = "Error 500: Revisá la consola del navegador.";
            msg.style.color = "red";
        }
    } catch (error) {
        console.error("Error de conexión:", error);
        msg.innerText = "Error de conexión con el servidor.";
        msg.style.color = "red";
    }

    console.log("ROL SELECCIONADO:", rolSeleccionado);
}