const API_USUARIOS = '/api/usuarios';

async function crearUsuario(event) {
    event.preventDefault();
    const errorEl = document.getElementById('errorCrear');
    const payload = {
        empleadoId: parseInt(document.getElementById('empleadoId').value, 10),
        username: document.getElementById('username').value,
        password: document.getElementById('password').value,
        rol: document.getElementById('rol').value,
    };
    try {
        const resp = await fetch(API_USUARIOS, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const problema = await resp.json().catch(() => null);
            throw new Error(problema?.message ?? 'No fue posible crear el usuario');
        }
        const usuario = await resp.json();
        errorEl.textContent = '';
        alert(`Usuario creado con id ${usuario.id}. La contraseña no se muestra por seguridad.`);
        document.getElementById('formCrearUsuario').reset();
    } catch (err) {
        errorEl.textContent = err.message;
    }
    return false;
}

async function consultarUsuario() {
    const id = document.getElementById('usuarioIdConsulta').value;
    const cont = document.getElementById('detalleUsuario');
    if (!id) return;
    try {
        const resp = await fetch(`${API_USUARIOS}/${id}`);
        if (!resp.ok) throw new Error('Usuario no encontrado');
        const u = await resp.json();
        cont.innerHTML = `
            <p class="mb-1"><strong>Usuario:</strong> ${escapeHtml(u.username)}</p>
            <p class="mb-1"><strong>Rol:</strong> ${escapeHtml(u.rol?.nombre ?? '—')}</p>
            <p class="mb-1"><strong>Estado:</strong>
                <span class="badge ${u.activo ? 'bg-success' : 'bg-secondary'}">${u.activo ? 'Activo' : 'Inactivo'}</span>
            </p>
            <button class="btn btn-sm ${u.activo ? 'btn-outline-danger' : 'btn-outline-success'}"
                    onclick="cambiarEstado(${u.id}, ${!u.activo})">
                ${u.activo ? 'Desactivar' : 'Activar'}
            </button>
        `;
    } catch (err) {
        cont.innerHTML = `<p class="text-danger">${escapeHtml(err.message)}</p>`;
    }
}

async function cambiarEstado(id, nuevoEstado) {
    const resp = await fetch(`${API_USUARIOS}/${id}/estado?activo=${nuevoEstado}`, {method: 'PATCH'});
    if (resp.ok) {
        document.getElementById('usuarioIdConsulta').value = id;
        await consultarUsuario();
    } else {
        alert('No fue posible cambiar el estado del usuario');
    }
}

async function cambiarPassword(event) {
    event.preventDefault();
    const errorEl = document.getElementById('errorPassword');
    const id = document.getElementById('passUsuarioId').value;
    const payload = {
        passwordActual: document.getElementById('passActual').value,
        passwordNueva: document.getElementById('passNueva').value,
    };
    try {
        const resp = await fetch(`${API_USUARIOS}/${id}/password`, {
            method: 'PATCH',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const problema = await resp.json().catch(() => null);
            throw new Error(problema?.message ?? 'No fue posible cambiar la contraseña');
        }
        errorEl.textContent = '';
        alert('Contraseña actualizada correctamente.');
        document.getElementById('formPassword').reset();
    } catch (err) {
        errorEl.textContent = err.message;
    }
    return false;
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}
