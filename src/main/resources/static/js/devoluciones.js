const API_DEVOLUCIONES = '/api/devoluciones';
let modalDevolucion;

document.addEventListener('DOMContentLoaded', () => {
    modalDevolucion = new bootstrap.Modal(document.getElementById('modalDevolucion'));
    cargarPendientes();
});

function alternarCampoOrigen() {
    const esCliente = document.getElementById('tipo').value === 'CLIENTE';
    document.getElementById('campoVentaOrigen').style.display = esCliente ? 'block' : 'none';
    document.getElementById('campoCompraOrigen').style.display = esCliente ? 'none' : 'block';
}

async function registrarDevolucion(event) {
    event.preventDefault();
    const errorEl = document.getElementById('errorDevolucion');
    const tipo = document.getElementById('tipo').value;
    const payload = {
        tipo,
        ventaOrigen: tipo === 'CLIENTE' ? {id: parseInt(document.getElementById('ventaOrigenId').value, 10)} : null,
        compraOrigen: tipo === 'PROVEEDOR' ? {id: parseInt(document.getElementById('compraOrigenId').value, 10)} : null,
        medicamento: {id: parseInt(document.getElementById('medicamentoId').value, 10)},
        cantidad: parseInt(document.getElementById('cantidad').value, 10),
        motivo: document.getElementById('motivo').value,
        estadoProducto: document.getElementById('estadoProducto').value || null,
    };
    try {
        const resp = await fetch(API_DEVOLUCIONES, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const problema = await resp.json().catch(() => null);
            throw new Error(problema?.message ?? 'No fue posible registrar la devolución');
        }
        modalDevolucion.hide();
        document.getElementById('formDevolucion').reset();
        await cargarPendientes();
    } catch (err) {
        errorEl.textContent = err.message;
    }
    return false;
}

function renderTarjeta(d) {
    const origen = d.tipo === 'CLIENTE'
        ? `Venta #${d.ventaOrigen?.id ?? '—'}`
        : `Compra #${d.compraOrigen?.id ?? '—'}`;
    return `
        <div class="card mb-2"><div class="card-body">
            <div class="d-flex justify-content-between">
                <strong>#${d.id} · ${escapeHtml(d.medicamento?.nombreComercial ?? 'Medicamento #' + d.medicamento?.id)}</strong>
                <span class="badge ${d.validadoPorFarmaceutico ? 'bg-success' : 'bg-warning text-dark'}">
                    ${d.validadoPorFarmaceutico ? 'Validada' : 'Pendiente'}
                </span>
            </div>
            <div class="small text-muted">${origen} · Cantidad: ${d.cantidad} · Motivo: ${escapeHtml(d.motivo)}</div>
            ${!d.validadoPorFarmaceutico ? `
                <div class="input-group input-group-sm mt-2" style="max-width:300px;">
                    <input type="number" class="form-control" placeholder="Id farmacéutico" id="farm-${d.id}"/>
                    <button class="btn btn-outline-primary" onclick="validarDevolucion(${d.id})">Validar</button>
                </div>
            ` : ''}
        </div></div>
    `;
}

async function cargarPendientes() {
    const cont = document.getElementById('listaPendientes');
    try {
        const resp = await fetch(`${API_DEVOLUCIONES}/pendientes`);
        if (!resp.ok) throw new Error();
        const items = await resp.json();
        cont.innerHTML = items.length ? items.map(renderTarjeta).join('') : '<p class="text-muted">Sin devoluciones pendientes </p>';
    } catch {
        cont.innerHTML = '<p class="text-danger">No fue posible cargar las devoluciones pendientes</p>';
    }
}

async function cargarPorTipo(tipo) {
    const cont = document.getElementById(tipo === 'CLIENTE' ? 'listaCliente' : 'listaProveedor');
    try {
        const resp = await fetch(`${API_DEVOLUCIONES}?tipo=${tipo}`);
        if (!resp.ok) throw new Error();
        const items = await resp.json();
        cont.innerHTML = items.length ? items.map(renderTarjeta).join('') : '<p class="text-muted">Sin registros</p>';
    } catch {
        cont.innerHTML = '<p class="text-danger">No fue posible cargar las devoluciones</p>';
    }
}

async function validarDevolucion(id) {
    const farmaceuticoId = document.getElementById(`farm-${id}`).value;
    if (!farmaceuticoId) { alert('Ingresa el id del farmacéutico que valida.'); return; }
    const resp = await fetch(`${API_DEVOLUCIONES}/${id}/validar?farmaceuticoId=${farmaceuticoId}`, {method: 'PATCH'});
    if (resp.ok) await cargarPendientes();
    else alert('No fue posible validar la devolución');
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}
