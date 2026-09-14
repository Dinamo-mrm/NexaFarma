const API_EMPLEADOS = '/api/empleados';
let modalEmpleado;
/** Cache local para editar sin meter JSON en atributos HTML. */
const empleadosCache = new Map();

document.addEventListener('DOMContentLoaded', () => {
    modalEmpleado = new bootstrap.Modal(document.getElementById('modalEmpleado'));
    cargarEmpleados();
    document.getElementById('filtroNombre').addEventListener('input', debounce(cargarEmpleados, 300));
});

function debounce(fn, delay) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), delay);
    };
}

function escapeHtml(texto) {
    if (texto == null) return '';
    return String(texto)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

async function cargarEmpleados() {
    const nombre = document.getElementById('filtroNombre').value.trim();
    const url = nombre
        ? `${API_EMPLEADOS}?nombre=${encodeURIComponent(nombre)}`
        : `${API_EMPLEADOS}?size=100&sort=nombreCompleto,asc`;
    const tbody = document.getElementById('tablaEmpleados');
    try {
        const resp = await fetch(url);
        if (!resp.ok) throw new Error('No fue posible cargar los empleados');
        const pagina = await resp.json();
        const empleados = Array.isArray(pagina) ? pagina : (pagina.content ?? []);
        empleadosCache.clear();
        empleados.forEach((e) => empleadosCache.set(e.id, e));

        if (!empleados.length) {
            tbody.innerHTML =
                '<tr><td colspan="5" class="text-center text-muted py-4">No hay empleados registrados</td></tr>';
            return;
        }
        tbody.innerHTML = empleados
            .map(
                (e) => `
            <tr>
                <td>${escapeHtml(e.documento)}</td>
                <td>${escapeHtml(e.nombreCompleto)} <span class="text-muted small">(id ${e.id})</span></td>
                <td>${escapeHtml(e.cargo ?? '')}</td>
                <td>${escapeHtml(e.correo ?? '')}</td>
                <td class="text-end text-nowrap">
                    <button type="button" class="btn btn-sm btn-outline-primary" onclick="abrirEdicion(${e.id})">Editar</button>
                    <button type="button" class="btn btn-sm btn-outline-danger" onclick="eliminarEmpleado(${e.id})">Desactivar</button>
                </td>
            </tr>`
            )
            .join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center text-danger py-4">${escapeHtml(err.message)}</td></tr>`;
    }
}

function abrirNuevo() {
    document.getElementById('formEmpleado').reset();
    document.getElementById('empleadoId').value = '';
    document.getElementById('tituloModal').textContent = 'Nuevo empleado';
    document.getElementById('errorEmpleado').textContent = '';
    modalEmpleado.show();
}

function abrirEdicion(id) {
    const e = empleadosCache.get(id);
    if (!e) {
        alert('No se encontró el empleado en memoria. Recarga la lista.');
        return;
    }
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
        documento: document.getElementById('documento').value.trim(),
        nombreCompleto: document.getElementById('nombreCompleto').value.trim(),
        cargo: document.getElementById('cargo').value.trim(),
        telefono: document.getElementById('telefono').value.trim() || null,
        correo: document.getElementById('correo').value.trim() || null,
        direccion: document.getElementById('direccion').value.trim() || null,
        salario: document.getElementById('salario').value
            ? parseFloat(document.getElementById('salario').value)
            : null,
        fechaIngreso: document.getElementById('fechaIngreso').value || null,
        activo: true,
    };
    const errorEl = document.getElementById('errorEmpleado');
    errorEl.textContent = '';
    try {
        const resp = await fetch(id ? `${API_EMPLEADOS}/${id}` : API_EMPLEADOS, {
            method: id ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            let msg = 'No se pudo guardar el empleado';
            try {
                const err = await resp.json();
                msg = err.message || err.error || msg;
            } catch (_) {}
            throw new Error(msg);
        }
        modalEmpleado.hide();
        await cargarEmpleados();
    } catch (err) {
        errorEl.textContent = err.message;
    }
    return false;
}

async function eliminarEmpleado(id) {
    if (!confirm('¿Desactivar este empleado?')) return;
    try {
        const resp = await fetch(`${API_EMPLEADOS}/${id}`, { method: 'DELETE' });
        if (!resp.ok) throw new Error('No se pudo desactivar');
        await cargarEmpleados();
    } catch (err) {
        alert(err.message);
    }
}
