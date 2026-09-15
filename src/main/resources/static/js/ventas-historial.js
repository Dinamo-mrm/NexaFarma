const API_VENTAS = '/api/ventas';
let modalDetalle;
let estadoActual = 'PAGADA';

document.addEventListener('DOMContentLoaded', () => {
    modalDetalle = new bootstrap.Modal(document.getElementById('modalDetalle'));

    NfUI.crearPillToggleGroup('filtroEstado', [
        {value: 'PAGADA', label: 'Pagada'},
        {value: 'PENDIENTE', label: 'Pendiente'},
        {value: 'ANULADA', label: 'Anulada'},
        {value: 'DEVUELTA', label: 'Devuelta'},
    ], estadoActual, (valor) => { estadoActual = valor; cargarVentas(); });

    cargarVentas();
});

async function cargarVentas() {
    const tbody = document.getElementById('tablaVentas');
    try {
        const resp = await fetch(`${API_VENTAS}?estado=${estadoActual}&size=50&sort=fecha,desc`);
        if (!resp.ok) throw new Error('No fue posible cargar las ventas');
        const pagina = await resp.json();
        const ventas = pagina.content ?? pagina;
        if (!ventas.length) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">Sin ventas en este estado</td></tr>';
            return;
        }
        tbody.innerHTML = ventas.map(v => {
            const cliente = v.cliente?.nombreCompleto || (v.cliente ? ('#' + v.cliente.id) : 'Consumidor final');
            const emp = v.empleado?.nombreCompleto || (v.empleado?.id ? ('Emp #' + v.empleado.id) : '—');
            const fecha = (v.fecha ?? '').replace('T', ' ').slice(0, 16);
            return `<tr>
                <td><code>${NfUI.escapeHtml(v.numeroVenta || ('#' + v.id))}</code></td>
                <td>${NfUI.escapeHtml(cliente)}</td>
                <td class="small">${NfUI.escapeHtml(emp)}</td>
                <td class="small text-nowrap">${NfUI.escapeHtml(fecha)}</td>
                <td class="text-end fw-semibold">$${Number(v.total || 0).toLocaleString('es-CO')}</td>
                <td class="small">${NfUI.escapeHtml(v.metodoPago ?? '')}</td>
                <td><span class="badge-estado ${NfUI.claseBadgeEstado(v.estado)}">${v.estado || ''}</span></td>
                <td class="text-end text-nowrap">
                    <button class="btn btn-sm btn-outline-primary" onclick="verDetalleVenta(${v.id})">Detalle</button>
                    <a class="btn btn-sm btn-outline-dark" target="_blank" href="/ventas/${v.id}/factura">Factura</a>
                    ${v.estado === 'PAGADA' ? `<button class="btn btn-sm btn-outline-danger" onclick="anularVenta(${v.id})">Anular</button>` : ''}
                </td>
            </tr>`;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">${NfUI.escapeHtml(err.message)}</td></tr>`;
    }
}

function verDetalle(venta) {
    const filas = (venta.detalles ?? []).map(d => `
        <tr>
            <td>${NfUI.escapeHtml(d.medicamento?.nombreComercial ?? '—')}</td>
            <td>${d.lote ? NfUI.escapeHtml(d.lote.numeroLote) : '—'}</td>
            <td>${d.cantidad}</td>
            <td>$${Number(d.precioUnitario).toLocaleString('es-CO')}</td>
            <td>$${Number(d.subtotal).toLocaleString('es-CO')}</td>
        </tr>
    `).join('');
    document.getElementById('detalleVentaBody').innerHTML = `
        <p class="mb-1"><strong>N.º venta:</strong> ${NfUI.escapeHtml(venta.numeroVenta)}</p>
        <p class="mb-1"><strong>Cliente:</strong> ${NfUI.escapeHtml(venta.cliente?.nombreCompleto ?? '—')}</p>
        <p class="mb-3"><strong>Empleado:</strong> ${NfUI.escapeHtml(venta.empleado?.nombreCompleto ?? '—')}</p>
        <table class="table table-sm">
            <thead><tr><th>Medicamento</th><th>Lote</th><th>Cant.</th><th>Precio</th><th>Subtotal</th></tr></thead>
            <tbody>${filas}</tbody>
        </table>
        <p class="mb-0 text-end">
            Subtotal: $${Number(venta.subtotal).toLocaleString('es-CO')} ·
            Descuento: $${Number(venta.descuento).toLocaleString('es-CO')} ·
            Impuestos: $${Number(venta.impuestos).toLocaleString('es-CO')} ·
            <strong>Total: $${Number(venta.total).toLocaleString('es-CO')}</strong>
        </p>
    `;
    modalDetalle.show();
}

async function anularVenta(id) {
    if (!confirm('¿Anular esta venta? Se revertirá el stock consumido. Requiere autorización del administrador.')) return;
    const resp = await fetch(`${API_VENTAS}/${id}/anular`, {method: 'PATCH'});
    if (resp.ok) await cargarVentas();
    else alert('No fue posible anular la venta');
}


async function verDetalleVenta(id) {
    try {
        const resp = await fetch(`${API_VENTAS}/${id}`);
        if (!resp.ok) throw new Error('No se pudo cargar el detalle');
        const v = await resp.json();
        const body = document.getElementById('detalleVentaBody') || document.getElementById('contenidoDetalle');
        const detalles = (v.detalles || []).map(d => {
            const nom = d.medicamento?.nombreComercial || '—';
            const lote = d.lote?.numeroLote || '—';
            return `<tr>
                <td>${NfUI.escapeHtml(nom)}</td>
                <td>${NfUI.escapeHtml(lote)}</td>
                <td class="text-end">${d.cantidad}</td>
                <td class="text-end">$${Number(d.precioUnitario||0).toLocaleString('es-CO')}</td>
                <td class="text-end">$${Number(d.subtotal||0).toLocaleString('es-CO')}</td>
            </tr>`;
        }).join('') || '<tr><td colspan="5" class="text-muted">Sin detalle de ítems</td></tr>';
        const html = `
            <div class="mb-2">
                <strong>${NfUI.escapeHtml(v.numeroVenta || '')}</strong>
                · ${(v.fecha||'').replace('T',' ').slice(0,16)}
                · <span class="badge-estado ${NfUI.claseBadgeEstado(v.estado)}">${v.estado||''}</span>
            </div>
            <div class="small text-muted mb-2">
                Cliente: ${NfUI.escapeHtml(v.cliente?.nombreCompleto || 'Consumidor final')}<br/>
                Empleado: ${NfUI.escapeHtml(v.empleado?.nombreCompleto || '—')}<br/>
                Pago: ${NfUI.escapeHtml(v.metodoPago || '')} · Total: $${Number(v.total||0).toLocaleString('es-CO')}
            </div>
            <div class="table-responsive">
                <table class="table table-sm">
                    <thead><tr><th>Producto</th><th>Lote</th><th class="text-end">Cant.</th><th class="text-end">P.unit</th><th class="text-end">Subtotal</th></tr></thead>
                    <tbody>${detalles}</tbody>
                </table>
            </div>
            <div class="mt-2">
                <a class="btn btn-sm btn-dark" target="_blank" href="/ventas/${v.id}/factura">Factura</a>
                <a class="btn btn-sm btn-outline-dark" href="/ventas/${v.id}/factura.pdf">PDF</a>
            </div>`;
        if (body) body.innerHTML = html;
        else alert('Detalle cargado (abra el modal de detalle en la plantilla)');
        if (typeof modalDetalle !== 'undefined' && modalDetalle) modalDetalle.show();
    } catch (e) {
        alert(e.message);
    }
}
