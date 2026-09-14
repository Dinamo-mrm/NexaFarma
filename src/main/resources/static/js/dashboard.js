const API_VENTAS = '/api/ventas';
const API_CLIENTES = '/api/clientes';
const API_ALERTAS = '/api/alertas';

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('fechaHoy').textContent = new Date().toLocaleDateString('es-CO', {
        weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
    });
    cargarVentasYGrafica();
    cargarClientes();
    cargarAlertas();
});

function hoyISO() {
    return new Date().toISOString().slice(0, 10);
}

function ultimos7Dias() {
    const dias = [];
    for (let i = 6; i >= 0; i--) {
        const d = new Date();
        d.setDate(d.getDate() - i);
        dias.push(d.toISOString().slice(0, 10));
    }
    return dias;
}

async function cargarVentasYGrafica() {
    try {
        // Trae las ventas pagadas mas recientes y agrega en el cliente por dia.
        // No existe (todavia) un endpoint de reporte agregado en el backend;
        // esta es una aproximacion basada en las ultimas N ventas.
        const resp = await fetch(`${API_VENTAS}?estado=PAGADA&size=300&sort=fecha,desc`);
        if (!resp.ok) throw new Error('No fue posible cargar las ventas');
        const pagina = await resp.json();
        const ventas = pagina.content ?? pagina;

        const hoy = hoyISO();
        const ventasHoy = ventas.filter(v => (v.fecha ?? '').startsWith(hoy));
        const totalHoy = ventasHoy.reduce((acc, v) => acc + Number(v.total ?? 0), 0);
        document.getElementById('kpiVentasHoyMonto').textContent = `$${totalHoy.toLocaleString('es-CO')}`;
        document.getElementById('kpiVentasHoyCantidad').textContent = `${ventasHoy.length} ventas`;

        const dias = ultimos7Dias();
        const totalesPorDia = dias.map(dia =>
            ventas.filter(v => (v.fecha ?? '').startsWith(dia))
                  .reduce((acc, v) => acc + Number(v.total ?? 0), 0)
        );
        dibujarGrafico(dias, totalesPorDia);
    } catch (err) {
        document.getElementById('kpiVentasHoyMonto').textContent = '—';
        console.error(err);
    }
}

function dibujarGrafico(dias, totales) {
    const etiquetas = dias.map(d => new Date(d + 'T00:00:00').toLocaleDateString('es-CO', {weekday: 'short', day: 'numeric'}));
    const ctx = document.getElementById('graficoVentas');
    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: etiquetas,
            datasets: [{
                label: 'Ventas ($)',
                data: totales,
                backgroundColor: '#0047AB',
                borderRadius: 6,
            }],
        },
        options: {
            responsive: true,
            plugins: {legend: {display: false}},
            scales: {y: {beginAtZero: true, ticks: {callback: (v) => '$' + v.toLocaleString('es-CO')}}},
        },
    });
}

async function cargarClientes() {
    try {
        const resp = await fetch(`${API_CLIENTES}?size=1`);
        if (!resp.ok) throw new Error();
        const pagina = await resp.json();
        document.getElementById('kpiClientes').textContent = pagina.totalElements ?? '—';
    } catch {
        document.getElementById('kpiClientes').textContent = '—';
    }
}

async function cargarAlertas() {
    // Stock bajo / agotado
    try {
        const resp = await fetch(`${API_ALERTAS}/stock-bajo`);
        if (!resp.ok) throw new Error();
        const items = await resp.json();
        document.getElementById('kpiStockBajo').textContent = items.length;
        renderListaStockBajo(items);
    } catch (err) {
        document.getElementById('kpiStockBajo').textContent = '—';
        document.getElementById('listaStockBajo').innerHTML = '<p class="text-danger">No fue posible cargar las alertas de stock.</p>';
    }

    // Próximos a vencer
    try {
        const resp = await fetch(`${API_ALERTAS}/proximos-vencimientos`);
        if (!resp.ok) throw new Error();
        const items = await resp.json();
        document.getElementById('kpiProximosVencer').textContent = items.length;
        renderListaProximosVencer(items);
    } catch (err) {
        document.getElementById('kpiProximosVencer').textContent = '—';
        document.getElementById('listaProximosVencer').innerHTML = '<p class="text-danger">No fue posible cargar los vencimientos.</p>';
    }
}

function renderListaStockBajo(items) {
    const cont = document.getElementById('listaStockBajo');
    if (!items.length) {
        cont.innerHTML = '<p class="text-muted">Sin alertas de stock bajo </p>';
        return;
    }
    cont.innerHTML = `
        <ul class="list-group list-group-flush">
            ${items.slice(0, 8).map(i => `
                <li class="list-group-item d-flex justify-content-between align-items-center px-0">
                    <span>${escapeHtml(i.medicamento?.nombreComercial ?? 'Medicamento #' + i.medicamento?.id)}</span>
                    <span class="badge ${i.cantidadDisponible <= 0 ? 'bg-danger' : 'bg-warning text-dark'}">
                        ${i.cantidadDisponible} / mín. ${i.stockMinimo}
                    </span>
                </li>
            `).join('')}
        </ul>
        ${items.length > 8 ? `<p class="text-muted small mt-2 mb-0">+${items.length - 8} más</p>` : ''}
    `;
}

function renderListaProximosVencer(items) {
    const cont = document.getElementById('listaProximosVencer');
    if (!items.length) {
        cont.innerHTML = '<p class="text-muted">Sin lotes próximos a vencer </p>';
        return;
    }
    const ordenados = [...items].sort((a, b) => (a.fechaVencimiento < b.fechaVencimiento ? -1 : 1));
    cont.innerHTML = `
        <ul class="list-group list-group-flush">
            ${ordenados.slice(0, 8).map(l => `
                <li class="list-group-item d-flex justify-content-between align-items-center px-0">
                    <span>${escapeHtml(l.medicamento?.nombreComercial ?? 'Medicamento #' + l.medicamento?.id)}
                        <span class="text-muted small">(lote ${escapeHtml(l.numeroLote)})</span></span>
                    <span class="badge bg-warning text-dark">${l.fechaVencimiento}</span>
                </li>
            `).join('')}
        </ul>
        ${items.length > 8 ? `<p class="text-muted small mt-2 mb-0">+${items.length - 8} más</p>` : ''}
    `;
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}
