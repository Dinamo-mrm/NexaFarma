const API_EMPLEADOS = '/api/empleados';
let modalEmpleado;

document.addEventListener('DOMContentLoaded', () => {
    modalEmpleado = new bootstrap.Modal(document.getElementById('modalEmpleado'));
    cargarEmpleados();
    document.getElementById('filtroNombre').addEventListener('input', debounce(cargarEmpleados, 300));
});

function debounce(fn, delay) {
    let timer;
    return (...args) => { clearTimeout(timer); timer = setTimeout(() => fn(...args), delay); };
}

async function cargarEmpleados() {
    const nombre = document.getElementById('filtroNombre').value.trim();
    const url = nombre ? `${API_EMPLEADOS}?nombre=${encodeURIComponent(nombre)}` : API_EMPLEADOS;
    const tbody = document.getElementById('tablaEmpleados');
    try {
        const resp = await fetch(url);
        if (!resp.ok) throw new Error('No fue posible cargar los empleados');
        const pagina = await resp.json();
        const empleados = pagina.content ?? pagina;
        if (!empleados.length) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">No hay empleados registrados</td></tr>';
            return;
        }
        tbody.innerHTML = empleados.map(e => `
            <tr>
                <td>${escapeHtml(e.documento)}</td>
                <td>${escapeHtml(e.nombreCompleto)} <span class="text-muted small">(id ${e.id})</span></td>
                <td>${escapeHtml(e.cargo ?? '')}</td>
                <td>${escapeHtml(e.correo ?? '')}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-primary" onclick='abrirEdicion(${JSON.stringify(e)})'>Editar</button>
                    <button class="btn btn-sm btn-outline-danger" onclick="eliminarEmpleado(${e.id})">Desactivar</button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center text-danger py-4">${escapeHtml(err.message)}</td></tr>`;
    }
}

function abrirNuevo() {
    document.getElementById('formEmpleado').reset();
    document.getElementById('empleadoId').value = '';
    document.getElementById('tituloModal').textContent = 'Nuevo empleado';
    document.getElementById('errorEmpleado').textContent = '';
}

function abrirEdicion(e) {
    document.getElementById('empleadoId').value = e.id;
    document.getElementById('documento').value = e.documento ?? '';
    document.getElementById('cargo').value = e.cargo ?? '';
    document.getElementById('nombreCompleto').value = e.nombreCompleto ?? '';
    document.getElementById('telefono').value = e.telefono ?? '';
    document.getElementById('correo').value = e.correo ?? '';
    document.getElementById('direccion').value = e.direccion ?? '';
    document.getElementById('salario').value = e.salario ?? '';
    document.getElementById('fechaIngreso').value = e.fechaIngreso ?? '';
    document.getElementById('tituloModal').textContent = 'Editar empleado';
    document.getElementById('errorEmpleado').textContent = '';
    modalEmpleado.show();
}

async function guardarEmpleado(event) {
    event.preventDefault();
    const id = document.getElementById('empleadoId').value;
    const payload = {
        documento: document.getElementById('documento').value,
        cargo: document.getElementById('cargo').value,
        nombreCompleto: document.getElementById('nombreCompleto').value,
        telefono: document.getElementById('telefono').value || null,
        correo: document.getElementById('correo').value || null,
        direccion: document.getElementById('direccion').value || null,
        salario: document.getElementById('salario').value ? parseFloat(document.getElementById('salario').value) : null,
        fechaIngreso: document.getElementById('fechaIngreso').value || null,
    };
    try {
        const resp = await fetch(id ? `${API_EMPLEADOS}/${id}` : API_EMPLEADOS, {
            method: id ? 'PUT' : 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const problema = await resp.json().catch(() => null);
            throw new Error(problema?.message ?? 'No fue posible guardar el empleado');
        }
        modalEmpleado.hide();
        await cargarEmpleados();
    } catch (err) {
        document.getElementById('errorEmpleado').textContent = err.message;
    }
    return false;
}

async function eliminarEmpleado(id) {
    if (!confirm('¿Desactivar este empleado?')) return;
    const resp = await fetch(`${API_EMPLEADOS}/${id}`, {method: 'DELETE'});
    if (resp.ok) await cargarEmpleados();
    else alert('No fue posible desactivar el empleado');
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}
