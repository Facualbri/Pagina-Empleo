async function iniciarSesion() {
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const msg = document.getElementById('msg');
    const btn = document.getElementById('btnLogin');

    msg.className = 'msg';
    if (!email || !password) {
        msg.className = 'msg error';
        msg.innerText = 'Completá email y contraseña.';
        return;
    }

    btn.disabled = true;
    btn.innerHTML = 'Ingresando...';

    try {
        const res = await fetch('/api/usuarios/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        if (res.ok) {
            const u = await res.json();

            localStorage.setItem('usuarioNombre', u.nombre);
            localStorage.setItem('usuarioId', u.id);
            localStorage.setItem('usuarioRol', u.tipo);
            localStorage.setItem('usuarioEmail', u.email);
            localStorage.setItem('token', u.token);
            if (u.refreshToken) {
                localStorage.setItem('refreshToken', u.refreshToken);
            }

            msg.className = 'msg success';
            msg.innerText = `¡Bienvenido/a, ${u.nombre}!`;

            setTimeout(() => {
                if (u.tipo === 'ROLE_ADMIN') window.location.href = 'pantallaAdminRoot.html';
                else if (u.tipo === 'ROLE_EMPRESA') window.location.href = 'pantallaAdmin.html';
                else window.location.href = 'pantallausuario.html';
            }, 900);

        } else {
            msg.className = 'msg error';
            msg.innerText = 'Email o contraseña incorrectos.';
            btn.disabled = false;
            btn.innerHTML = '<i data-lucide="log-in" style="width:18px"></i> Ingresar';
            lucide.createIcons();
        }
    } catch {
        msg.className = 'msg error';
        msg.innerText = 'Error de conexión. Intentá de nuevo.';
        btn.disabled = false;
        btn.innerHTML = '<i data-lucide="log-in" style="width:18px"></i> Ingresar';
        lucide.createIcons();
    }
}
