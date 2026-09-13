const API_DOMICILIOS = '/api/domicilios';
let modalDomicilio;

document.addEventListener('DOMContentLoaded', () => {
    modalDomicilio = new bootstrap.Modal(document.getElementById('modalDomicilio'));
    cargarDomicilios();
});

async function registrarDomicilio(event) {
    event.preventDefault();
    const errorEl = document.getElementById('errorDomicilio');
    const payload = {
        venta: {id: parseInt(document.getElementById('ventaId').value, 10)},
        cliente: {id: parseInt(document.getElementById('clienteId').value, 10)},
        direccionEntrega: document.getElementById('direccionEntrega').value,
        telefonoContacto: document.getElementById('telefonoContacto').value,
        valorDomicilio: document.getElementById('valorDomicilio').value ? parseFloat(document.getElementById('valorDomicilio').value) : 0,
    };
    try {
        const resp = await fetch(API_DOMICILIOS, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const problema = await resp.json().catch(() => null);
            throw new Error(problema?.message ?? 'No fue posible registrar el domicilio');
        }
        modalDomicilio.hide();
        document.getElementById('formDomicilio').reset();
        await cargarDomicilios();
    } catch (err) {
        errorEl.textContent = err.message;
    }
    return false;
}

async function cargarDomicilios() {
    const estado = document.getElementById('filtroEstado').value;
    const cont = document.getElementById('listaDomicilios');
    try {
        const resp = await fetch(`${API_DOMICILIOS}?estado=${estado}`);
        if (!resp.ok) throw new Error();
        const items = await resp.json();
        if (!items.length) {
            cont.innerHTML = '<p class="text-muted">Sin domicilios en este estado</p>';
            return;
        }
        cont.innerHTML = items.map(d => `
            <div class="card mb-2"><div class="card-body">
                <div class="d-flex justify-content-between">
                    <strong>#${d.id} · ${escapeHtml(d.cliente?.nombreCompleto ?? 'Cliente #' + d.cliente?.id)}</strong>
                    <span class="badge bg-secondary">${d.estado}</span>
                </div>
                <div class="small text-muted">${escapeHtml(d.direccionEntrega)} · ${escapeHtml(d.telefonoContacto)}</div>
                <div class="small text-muted">Domiciliario: ${d.domiciliario?.nombreCompleto ?? 'sin asignar'}</div>
                <div class="mt-2 d-flex gap-2 flex-wrap">
                    ${d.estado === 'PENDIENTE' ? `
                        <div class="input-group input-group-sm" style="max-width:260px;">
                            <input type="number" class="form-control" placeholder="Id domiciliario" id="dom-${d.id}"/>
                            <button class="btn btn-outline-primary" onclick="asignarDomiciliario(${d.id})">Asignar</button>
                        </div>
                    ` : ''}
                    ${d.estado === 'EN_PREPARACION' || d.estado === 'PENDIENTE' ? `<button class="btn btn-sm btn-outline-primary" onclick="cambiarEstado(${d.id}, 'en-camino')">Marcar en camino</button>` : ''}
                    ${d.estado === 'EN_CAMINO' ? `<button class="btn btn-sm btn-outline-success" onclick="cambiarEstado(${d.id}, 'entregado')">Marcar entregado</button>` : ''}
                    ${d.estado !== 'ENTREGADO' && d.estado !== 'CANCELADO' ? `<button class="btn btn-sm btn-outline-danger" onclick="cambiarEstado(${d.id}, 'cancelar')">Cancelar</button>` : ''}
                </div>
            </div></div>
        `).join('');
    } catch {
        cont.innerHTML = '<p class="text-danger">No fue posible cargar los domicilios</p>';
    }
}

async function asignarDomiciliario(id) {
    const domiciliarioId = document.getElementById(`dom-${id}`).value;
    if (!domiciliarioId) { alert('Ingresa el id del domiciliario.'); return; }
    const resp = await fetch(`${API_DOMICILIOS}/${id}/asignar?domiciliarioId=${domiciliarioId}`, {method: 'PATCH'});
    if (resp.ok) await cargarDomicilios();
    else alert('No fue posible asignar el domiciliario');
}

async function cambiarEstado(id, accion) {
    const resp = await fetch(`${API_DOMICILIOS}/${id}/${accion}`, {method: 'PATCH'});
    if (resp.ok) await cargarDomicilios();
    else alert('No fue posible actualizar el estado del domicilio');
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}
