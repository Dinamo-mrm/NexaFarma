const API_DOMICILIOS = '/api/domicilios';
const ESTADOS = ['PENDIENTE', 'EN_PREPARACION', 'EN_CAMINO', 'ENTREGADO'];
let modalDomicilio;

document.addEventListener('DOMContentLoaded', () => {
    const el = document.getElementById('modalDomicilio');
    if (el) modalDomicilio = new bootstrap.Modal(el);
    cargarKanban();
    setInterval(cargarKanban, 30000);
});

function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, (c) => (
        {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]
    ));
}

function badgeTermico(d) {
    if (!d.requiereTransporteTermico) return '';
    return `<span class="badge bg-info text-dark">❄ Térmico</span>
        ${d.neveraPortatilConfirmada ? '<span class="badge bg-success">Nevera OK</span>' : '<span class="badge bg-warning text-dark">Pend. nevera</span>'}`;
}

function infoCambio(d) {
    if (d.cambioEnRuta == null && d.montoPagaCliente == null) return '';
    return `<div class="small text-muted">Cambio en ruta: <strong>$${Number(d.cambioEnRuta || 0).toLocaleString('es-CO')}</strong></div>`;
}

function cardHtml(d) {
    return `
    <div class="kanban-card" data-id="${d.id}">
      <div class="d-flex justify-content-between gap-1 flex-wrap">
        <strong>#${d.id}</strong>
        <div>${badgeTermico(d)}</div>
      </div>
      <div class="small mt-1">${escapeHtml(d.direccionEntrega || '')}</div>
      <div class="small text-muted">${escapeHtml(d.telefonoContacto || '')}</div>
      <div class="small text-muted">Domiciliario: ${escapeHtml(d.domiciliario?.nombreCompleto ?? 'sin asignar')}</div>
      ${infoCambio(d)}
      <div class="actions">
        ${d.estado === 'PENDIENTE' || d.estado === 'EN_PREPARACION' ? `
          <input type="number" class="form-control form-control-sm" style="max-width:90px" placeholder="Id dom." id="dom-${d.id}"/>
          <button class="btn btn-sm btn-outline-primary" onclick="asignarConTermico(${d.id})">Asignar</button>
          <button class="btn btn-sm btn-outline-secondary" onclick="registrarCambioEfectivo(${d.id})">Efectivo</button>
        ` : ''}
        ${d.estado === 'EN_PREPARACION' || d.estado === 'PENDIENTE' ? `
          <button class="btn btn-sm btn-outline-info" onclick="cambiarEstado(${d.id}, 'en-camino')">En camino</button>
        ` : ''}
        ${d.estado === 'EN_CAMINO' ? `
          <button class="btn btn-sm btn-outline-success" onclick="entregarConPod(${d.id})">Entregar + PoD</button>
        ` : ''}
        ${d.estado !== 'ENTREGADO' && d.estado !== 'CANCELADO' ? `
          <button class="btn btn-sm btn-outline-danger" onclick="cambiarEstado(${d.id}, 'cancelar')">Cancelar</button>
        ` : ''}
      </div>
    </div>`;
}

async function cargarKanban() {
    for (const estado of ESTADOS) {
        const col = document.getElementById('col-' + estado);
        const cnt = document.getElementById('cnt-' + estado);
        if (!col) continue;
        try {
            const resp = await fetch(`${API_DOMICILIOS}?estado=${estado}`);
            if (!resp.ok) throw new Error();
            const items = await resp.json();
            if (cnt) cnt.textContent = items.length;
            col.innerHTML = items.length
                ? items.map(cardHtml).join('')
                : '<p class="text-muted small mb-0">Sin pedidos</p>';
        } catch {
            col.innerHTML = '<p class="text-danger small">Error al cargar</p>';
        }
    }
}

async function registrarDomicilio(event) {
    event.preventDefault();
    const errorEl = document.getElementById('errorDomicilio');
    errorEl.textContent = '';
    const payload = {
        venta: { id: parseInt(document.getElementById('ventaId').value, 10) },
        cliente: { id: parseInt(document.getElementById('clienteId').value, 10) },
        direccionEntrega: document.getElementById('direccionEntrega').value,
        telefonoContacto: document.getElementById('telefonoContacto').value,
        valorDomicilio: document.getElementById('valorDomicilio').value
            ? parseFloat(document.getElementById('valorDomicilio').value) : 0,
        montoPagaCliente: document.getElementById('montoPagaCliente')?.value
            ? parseFloat(document.getElementById('montoPagaCliente').value) : null,
    };
    try {
        const resp = await fetch(API_DOMICILIOS, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const problema = await resp.json().catch(() => null);
            throw new Error(problema?.message ?? 'No fue posible registrar el domicilio');
        }
        modalDomicilio.hide();
        document.getElementById('formDomicilio').reset();
        await cargarKanban();
    } catch (err) {
        errorEl.textContent = err.message;
    }
    return false;
}

async function asignarConTermico(id) {
    const input = document.getElementById(`dom-${id}`);
    const domiciliarioId = input?.value;
    if (!domiciliarioId) { alert('Ingresa el id del domiciliario.'); return; }
    const card = input?.closest('.kanban-card');
    const esTermico = card && card.innerHTML.includes('Térmico');
    let confirmar = false;
    if (esTermico) {
        confirmar = confirm('Este pedido REQUIERE TRANSPORTE TÉRMICO.\n¿Confirma nevera portátil?');
        if (!confirmar) return;
    }
    const resp = await fetch(
        `${API_DOMICILIOS}/${id}/asignar?domiciliarioId=${domiciliarioId}&confirmarNeveraPortatil=${confirmar}`,
        { method: 'PATCH' }
    );
    if (resp.ok) cargarKanban();
    else {
        const e = await resp.json().catch(() => ({}));
        alert(e.message || 'No fue posible asignar');
    }
}

async function cambiarEstado(id, accion) {
    const resp = await fetch(`${API_DOMICILIOS}/${id}/${accion}`, { method: 'PATCH' });
    if (resp.ok) cargarKanban();
    else {
        const e = await resp.json().catch(() => ({}));
        alert(e.message || 'No fue posible actualizar el estado');
    }
}

async function entregarConPod(id) {
    const evidencia = prompt('PoD — URL foto guía firmada (vacío si usa firma):') || '';
    const firma = evidencia ? '' : (prompt('URL firma digital:') || '');
    if (!evidencia && !firma) {
        alert('Debe proporcionar foto o firma digital.');
        return;
    }
    const resp = await fetch(`${API_DOMICILIOS}/${id}/entregado`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ evidenciaEntregaUrl: evidencia || null, firmaDigitalUrl: firma || null }),
    });
    if (resp.ok) cargarKanban();
    else {
        const e = await resp.json().catch(() => ({}));
        alert(e.message || 'No fue posible marcar entregado');
    }
}

async function registrarCambioEfectivo(id) {
    const monto = prompt('¿Con cuánto paga el cliente (efectivo)?');
    if (!monto) return;
    const resp = await fetch(`${API_DOMICILIOS}/${id}/pago-efectivo`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ montoPagaCliente: parseFloat(monto) }),
    });
    if (resp.ok) {
        const d = await resp.json();
        alert(`Cambio en ruta: $${Number(d.cambioEnRuta || 0).toLocaleString('es-CO')}`);
        cargarKanban();
    } else {
        const e = await resp.json().catch(() => ({}));
        alert(e.message || 'No fue posible registrar el pago');
    }
}
