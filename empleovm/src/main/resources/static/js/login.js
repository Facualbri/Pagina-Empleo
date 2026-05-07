async function iniciarSesion() {
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const msg = document.getElementById('msg');

    const loginDTO = { email, password };

    try {
        const response = await fetch('/api/usuarios/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(loginDTO)
        });

        if (response.ok) {
            const usuario = await response.json();
            
            // Guardamos datos clave en el navegador
            localStorage.setItem('usuarioNombre', usuario.nombre);
            localStorage.setItem('usuarioId', usuario.id);
            localStorage.setItem('usuarioRol', usuario.tipo);
            
            msg.innerText = "¡Bienvenido, " + usuario.nombre + "!";
            msg.style.color = "green";

            // Redirección inteligente
            setTimeout(() => {
                if (usuario.tipo === "ROLE_EMPRESA") {
                    window.location.href = "pantallaAdmin.html";
                } else {
                    window.location.href = "pantallausuario.html";
                }
            }, 1000); // Esperamos 1 segundo para que el usuario vea el mensaje verde
            
        } else {
            msg.innerText = "Email o contraseña incorrectos.";
            msg.style.color = "red";
        }
    } catch (error) {
        msg.innerText = "Error de conexión al servidor.";
    }
}