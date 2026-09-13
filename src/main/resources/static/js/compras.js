const API_COMPRAS = '/api/compras';
const API_PROVEEDORES = '/api/proveedores';

let modalCompra, modalRecibir;
let contadorItemCompra = 0;
let compraEnRecepcion = null;

document.addEventListener('DOMContentLoaded', async () => {
    modalCompra = new bootstrap.Modal(document.getElementById('modalCompra'));
    modalRecibir = new bootstrap.Modal(document.getElementById('modalRecibir'));
    await cargarSelectProveedores();
    agregarItemCompra();
    cargarCompras();
    document.getElementById('modalCompra').addEventListener('show.bs.modal', () => {
        document.getElementById('itemsCompra').innerHTML = '';
        contadorItemCompra = 0;
        agregarItemCompra();
        document.getElementById('errorCompra').textContent = '';
    });
});

async function cargarSelectProveedores() {
    const select = document.getElementById('proveedorId');
    try {
        const resp = await fetch(`${API_PROVEEDORES}?size=100`);
        const pagina = await resp.json();
        const proveedores = pagina.content ?? pagina;
        select.innerHTML = proveedores.map(p => `<option value="${p.id}">${escapeHtml(p.razonSocial)}</option>`).join('');
    } catch {
        select.innerHTML = '<option value="">No fue posible cargar proveedores</option>';
    }
}

function agregarItemCompra() {
    contadorItemCompra++;
    const id = `item-compra-${contadorItemCompra}`;
    const cont = document.getElementById('itemsCompra');
    const div = document.createElement('div');
    div.className = 'row align-items-end mb-2';
    div.id = id;
    div.innerHTML = `
        <div class="col-md-4">
            <label class="form-label small">Id medicamento *</label>
            <input type="number" class="form-control form-control-sm item-medicamento"/>
        </div>
        <div class="col-md-3">
            <label class="form-label small">Cantidad *</label>
            <input type="number" class="form-control form-control-sm item-cantidad" min="1"/>
        </div>
        <div class="col-md-4">
            <label class="form-label small">Precio unitario *</label>
            <input type="number" class="form-control form-control-sm item-precio" min="0.01" step="0.01"/>
        </div>
        <div class="col-md-1">
            <button type="button" class="btn btn-sm btn-outline-danger" onclick="document.getElementById('${id}').remove()">×</button>
        </div>
    `;
    cont.appendChild(div);
}

async function registrarCompra() {
    const errorEl = document.getElementById('errorCompra');
    const items = [...document.querySelectorAll('#itemsCompra > div')].map(fila => ({
        medicamento: {id: parseInt(fila.querySelector('.item-medicamento').value, 10)},
        cantidad: parseInt(fila.querySelector('.item-cantidad').value, 10),
        precioUnitario: parseFloat(fila.querySelector('.item-precio').value),
    })).filter(i => i.medicamento.id && i.cantidad);

    if (!items.length) {
        errorEl.textContent = 'Agrega al menos un item con medicamento y cantidad válidos.';
        return;
    }
    const subtotal = items.reduce((acc, i) => acc + i.precioUnitario * i.cantidad, 0);
    const payload = {
        proveedor: {id: parseInt(document.getElementById('proveedorId').value, 10)},
        empleadoResponsable: {id: parseInt(document.getElementById('empleadoResponsableId').value, 10)},
        formaPago: document.getElementById('formaPago').value,
        detalles: items,
        subtotal, impuestos: 0, total: subtotal,
    };
    try {
        const resp = await fetch(API_COMPRAS, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const problema = await resp.json().catch(() => null);
            throw new Error(problema?.message ?? 'No fue posible registrar la compra');
        }
        modalCompra.hide();
        await cargarCompras();
    } catch (err) {
        errorEl.textContent = err.message;
    }
}

async function cargarCompras() {
    const estado = document.getElementById('filtroEstado').value;
    const tbody = document.getElementById('tablaCompras');
    try {
        const resp = await fetch(`${API_COMPRAS}?estado=${estado}&size=50`);
        if (!resp.ok) throw new Error('No fue posible cargar las compras');
        const pagina = await resp.json();
        const compras = pagina.content ?? pagina;
        if (!compras.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Sin compras en este estado</td></tr>';
            return;
        }
        tbody.innerHTML = compras.map(c => `
            <tr>
                <td>${escapeHtml(c.numeroCompra)}</td>
                <td>${escapeHtml(c.proveedor?.razonSocial ?? '—')}</td>
                <td>${(c.fechaCompra ?? '').replace('T', ' ').slice(0, 16)}</td>
                <td>$${Number(c.total).toLocaleString('es-CO')}</td>
                <td><span class="badge bg-secondary">${c.estado}</span></td>
                <td class="text-end">
                    ${c.estado === 'PENDIENTE' ? `
                        <button class="btn btn-sm btn-outline-primary" onclick='abrirRecepcion(${JSON.stringify(c)})'>Recibir</button>
                        <button class="btn btn-sm btn-outline-danger" onclick="cancelarCompra(${c.id})">Cancelar</button>
                    ` : ''}
                </td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger py-4">${escapeHtml(err.message)}</td></tr>`;
    }
}

function abrirRecepcion(compra) {
    compraEnRecepcion = compra;
    const cont = document.getElementById('detallesRecepcion');
    cont.innerHTML = compra.detalles.map(d => `
        <div class="border rounded p-2 mb-2" data-detalle-id="${d.id}">
            <div class="fw-bold mb-1">${escapeHtml(d.medicamento?.nombreComercial ?? 'Medicamento #' + d.medicamento?.id)} — ${d.cantidad} unidades</div>
            <div class="row">
                <div class="col-md-4 mb-1">
                    <label class="form-label small">Número de lote *</label>
                    <input type="text" class="form-control form-control-sm rec-numero-lote"/>
                </div>
                <div class="col-md-4 mb-1">
                    <label class="form-label small">Fecha fabricación *</label>
                    <input type="date" class="form-control form-control-sm rec-fabricacion"/>
                </div>
                <div class="col-md-4 mb-1">
                    <label class="form-label small">Fecha vencimiento *</label>
                    <input type="date" class="form-control form-control-sm rec-vencimiento"/>
                </div>
            </div>
        </div>
    `).join('');
    document.getElementById('errorRecibir').textContent = '';
    modalRecibir.show();
}

async function confirmarRecepcion() {
    const errorEl = document.getElementById('errorRecibir');
    const datosLotePorDetalle = {};
    document.querySelectorAll('#detallesRecepcion > div').forEach(div => {
        const detalleId = div.dataset.detalleId;
        datosLotePorDetalle[detalleId] = {
            numeroLote: div.querySelector('.rec-numero-lote').value,
            fechaFabricacion: div.querySelector('.rec-fabricacion').value,
            fechaVencimiento: div.querySelector('.rec-vencimiento').value,
        };
    });
    try {
        const resp = await fetch(`${API_COMPRAS}/${compraEnRecepcion.id}/recibir`, {
            method: 'PATCH',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(datosLotePorDetalle),
        });
        if (!resp.ok) {
            const problema = await resp.json().catch(() => null);
            throw new Error(problema?.message ?? 'No fue posible registrar la recepción');
        }
        modalRecibir.hide();
        await cargarCompras();
    } catch (err) {
        errorEl.textContent = err.message;
    }
}

async function cancelarCompra(id) {
    if (!confirm('¿Cancelar esta orden de compra?')) return;
    const resp = await fetch(`${API_COMPRAS}/${id}/cancelar`, {method: 'PATCH'});
    if (resp.ok) await cargarCompras();
    else alert('No fue posible cancelar la compra');
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}
