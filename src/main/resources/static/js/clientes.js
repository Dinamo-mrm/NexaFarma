const API_CLIENTES = '/api/clientes';
let modalCliente;

document.addEventListener('DOMContentLoaded', () => {
    modalCliente = new bootstrap.Modal(document.getElementById('modalCliente'));
    cargarClientes();
    document.getElementById('filtroNombre').addEventListener('input', debounce(() => cargarClientes(), 300));
});

function debounce(fn, delay) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), delay);
    };
}

async function cargarClientes() {
    const nombre = document.getElementById('filtroNombre').value.trim();
    const url = nombre ? `${API_CLIENTES}?nombre=${encodeURIComponent(nombre)}` : API_CLIENTES;
    const tbody = document.getElementById('tablaClientes');
    try {
        const resp = await fetch(url);
        if (!resp.ok) throw new Error('No fue posible cargar los clientes');
        const pagina = await resp.json();
        const clientes = pagina.content ?? pagina;
        if (!clientes.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No hay clientes registrados</td></tr>';
            return;
        }
        tbody.innerHTML = clientes.map(c => `
            <tr>
                <td>${escapeHtml(c.documento)}</td>
                <td>${escapeHtml(c.nombreCompleto)}</td>
                <td>${escapeHtml(c.telefono ?? '')}</td>
                <td>${escapeHtml(c.correo ?? '')}</td>
                <td>${escapeHtml(c.eps ?? '')}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-primary" onclick='abrirEdicion(${JSON.stringify(c)})'>Editar</button>
                    <button class="btn btn-sm btn-outline-danger" onclick="desactivarCliente(${c.id})">Desactivar</button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger py-4">${escapeHtml(err.message)}</td></tr>`;
    }
}

function abrirNuevo() {
    document.getElementById('formCliente').reset();
    document.getElementById('clienteId').value = '';
    document.getElementById('tituloModalCliente').textContent = 'Nuevo cliente';
    document.getElementById('errorCliente').textContent = '';
}

function abrirEdicion(cliente) {
    document.getElementById('clienteId').value = cliente.id;
    if (document.getElementById('tipoDocumento')) document.getElementById('tipoDocumento').value = cliente.tipoDocumento ?? 'CC';
    document.getElementById('documento').value = cliente.documento ?? '';
    if (document.getElementById('emailFacturacion')) document.getElementById('emailFacturacion').value = cliente.emailFacturacion ?? '';
    if (document.getElementById('responsabilidadTributaria')) document.getElementById('responsabilidadTributaria').value = cliente.responsabilidadTributaria ?? '';
    document.getElementById('nombreCompleto').value = cliente.nombreCompleto ?? '';
    document.getElementById('telefono').value = cliente.telefono ?? '';
    document.getElementById('correo').value = cliente.correo ?? '';
    document.getElementById('direccion').value = cliente.direccion ?? '';
    document.getElementById('fechaNacimiento').value = cliente.fechaNacimiento ?? '';
    document.getElementById('eps').value = cliente.eps ?? '';
    document.getElementById('alergias').value = cliente.alergias ?? '';
    document.getElementById('autorizaTratamientoDatos').checked = !!cliente.autorizaTratamientoDatos;
    document.getElementById('tituloModalCliente').textContent = 'Editar cliente';
    document.getElementById('errorCliente').textContent = '';
    modalCliente.show();
}

async function guardarCliente(event) {
    event.preventDefault();
    const id = document.getElementById('clienteId').value;
    const payload = {
        tipoDocumento: document.getElementById('tipoDocumento')?.value || 'CC',
        documento: document.getElementById('documento').value,
        emailFacturacion: document.getElementById('emailFacturacion')?.value || null,
        responsabilidadTributaria: document.getElementById('responsabilidadTributaria')?.value || null,
        nombreCompleto: document.getElementById('nombreCompleto').value,
        telefono: document.getElementById('telefono').value || null,
        correo: document.getElementById('correo').value || null,
        direccion: document.getElementById('direccion').value || null,
        fechaNacimiento: document.getElementById('fechaNacimiento').value || null,
        eps: document.getElementById('eps').value || null,
        alergias: document.getElementById('alergias').value || null,
        autorizaTratamientoDatos: document.getElementById('autorizaTratamientoDatos').checked,
    };
    try {
        const resp = await fetch(id ? `${API_CLIENTES}/${id}` : API_CLIENTES, {
            method: id ? 'PUT' : 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const problema = await resp.json().catch(() => null);
            throw new Error(problema?.message ?? 'No fue posible guardar el cliente');
        }
        modalCliente.hide();
        await cargarClientes();
    } catch (err) {
        document.getElementById('errorCliente').textContent = err.message;
    }
    return false;
}

async function desactivarCliente(id) {
    if (!confirm('¿Desactivar este cliente? Se conserva su historial de ventas y fórmulas.')) return;
    const resp = await fetch(`${API_CLIENTES}/${id}`, {method: 'DELETE'});
    if (resp.ok) {
        await cargarClientes();
    } else {
        alert('No fue posible desactivar el cliente');
    }
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}
