let rolSeleccionado = 'USER';

function setRol(rol) {
    rolSeleccionado = rol;
    document.getElementById('tabUser').classList.toggle('active', rol === 'USER');
    document.getElementById('tabEmpresa').classList.toggle('active', rol === 'EMPRESA');
    document.getElementById('hintEmpresa').classList.toggle('visible', rol === 'EMPRESA');
    document.getElementById('labelNombre').innerText = rol === 'EMPRESA'
        ? 'Nombre de la empresa'
        : 'Nombre completo';
    document.getElementById('nombre').placeholder = rol === 'EMPRESA'
        ? 'Ej: Pizzería El Rancho'
        : 'Tu nombre completo';
}

async function registrar() {
    const nombre   = document.getElementById('nombre').value.trim();
    const email    = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const msg      = document.getElementById('msg');
    const btn      = document.getElementById('btnRegister');

    msg.className = 'msg';

    if (!nombre) { msg.className='msg error'; msg.innerText='El nombre es obligatorio.'; return; }
    if (!email || !email.includes('@')) { msg.className='msg error'; msg.innerText='Ingresá un email válido.'; return; }
    if (!password || password.length < 6) { msg.className='msg error'; msg.innerText='La contraseña debe tener al menos 6 caracteres.'; return; }

    btn.disabled = true;
    btn.innerHTML = 'Creando cuenta...';

    const dto = {
        nombre,
        email,
        password,
        tipo: rolSeleccionado === 'EMPRESA' ? 'ROLE_EMPRESA' : 'ROLE_USER'
    };

    try {
        const res = await fetch('/api/usuarios', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(dto)
        });

        if (res.ok) {
            msg.className = 'msg success';
            msg.innerText = rolSeleccionado === 'EMPRESA'
                ? '✓ Cuenta de empresa creada. ¡Ya podés iniciar sesión!'
                : '✓ ¡Registro exitoso! Redirigiendo...';
            setTimeout(() => window.location.href = 'login.html', 2000);
        } else {
            const txt = await res.text();
            msg.className = 'msg error';
            msg.innerText = txt || 'Error al registrarse.';
            btn.disabled  = false;
            btn.innerHTML = '<i data-lucide="user-plus" style="width:18px"></i> Crear cuenta';
            lucide.createIcons();
        }
    } catch {
        msg.className = 'msg error';
        msg.innerText = 'Error de conexión. Intentá de nuevo.';
        btn.disabled  = false;
        btn.innerHTML = '<i data-lucide="user-plus" style="width:18px"></i> Crear cuenta';
        lucide.createIcons();
    }
}
