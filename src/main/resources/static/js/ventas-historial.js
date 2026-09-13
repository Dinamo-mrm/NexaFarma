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
        tbody.innerHTML = ventas.map(v => `
            <tr>
                <td>${NfUI.escapeHtml(v.numeroVenta)}</td>
                <td>${NfUI.escapeHtml(v.cliente?.nombreCompleto ?? '—')}</td>
                <td>${(v.fecha ?? '').replace('T', ' ').slice(0, 16)}</td>
                <td>$${Number(v.total).toLocaleString('es-CO')}</td>
                <td>${NfUI.escapeHtml(v.metodoPago ?? '')}</td>
                <td><span class="badge-estado ${NfUI.claseBadgeEstado(v.estado)}">${v.estado}</span></td>
                <td class="text-end">
                    ${NfUI.menuAcciones([
                        {label: 'Ver detalle', onClick: `verDetalle(${NfUI.escapeHtml(JSON.stringify(v))})`},
                        ...(v.estado === 'PAGADA' ? [{label: 'Anular', onClick: `anularVenta(${v.id})`, peligro: true}] : []),
                    ])}
                </td>
            </tr>
        `).join('');
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
