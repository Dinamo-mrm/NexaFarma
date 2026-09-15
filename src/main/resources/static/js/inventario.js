const API_INVENTARIO = '/api/inventario';
const API_LOTES = '/api/lotes';

document.addEventListener('DOMContentLoaded', () => {
    cargarStockBajo();
    cargarVencidos();
    cargarProximos();
});

async function cargarStockBajo() {
    const tbody = document.getElementById('tablaStockBajo');
    try {
        const resp = await fetch(`${API_INVENTARIO}/stock-bajo`);
        if (!resp.ok) throw new Error();
        const items = await resp.json();
        if (!items.length) {
            tbody.innerHTML = '<tr><td colspan="3" class="text-center text-muted py-4">Sin alertas de stock </td></tr>';
            return;
        }
        tbody.innerHTML = items.map(i => `
            <tr>
                <td>${escapeHtml(i.medicamento?.nombreComercial ?? '—')}</td>
                <td><span class="badge ${i.cantidadDisponible <= 0 ? 'bg-danger' : 'bg-warning text-dark'}">${i.cantidadDisponible}</span></td>
                <td>${i.stockMinimo}</td>
            </tr>
        `).join('');
    } catch {
        tbody.innerHTML = '<tr><td colspan="3" class="text-center text-danger py-4">No fue posible cargar el stock bajo</td></tr>';
    }
}

async function cargarVencidos() {
    const tbody = document.getElementById('tablaVencidos');
    try {
        const resp = await fetch(`${API_LOTES}/vencidos`);
        if (!resp.ok) throw new Error();
        const items = await resp.json();
        if (!items.length) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-4">Sin lotes vencidos </td></tr>';
            return;
        }
        tbody.innerHTML = items.map(l => `
            <tr>
                <td>${escapeHtml(l.medicamento?.nombreComercial ?? '—')}</td>
                <td>${escapeHtml(l.numeroLote)}</td>
                <td>${l.fechaVencimiento}</td>
                <td>${l.cantidadDisponible}</td>
            </tr>
        `).join('');
    } catch {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-4">No fue posible cargar los lotes vencidos</td></tr>';
    }
}

async function cargarProximos() {
    const tbody = document.getElementById('tablaProximos');
    try {
        const resp = await fetch(`${API_LOTES}/proximos-a-vencer`);
        if (!resp.ok) throw new Error();
        const items = await resp.json();
        if (!items.length) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-4">Sin lotes próximos a vencer </td></tr>';
            return;
        }
        tbody.innerHTML = items.map(l => `
            <tr>
                <td>${escapeHtml(l.medicamento?.nombreComercial ?? '—')}</td>
                <td>${escapeHtml(l.numeroLote)}</td>
                <td><span class="badge bg-warning text-dark">${l.fechaVencimiento}</span></td>
                <td>${l.cantidadDisponible}</td>
            </tr>
        `).join('');
    } catch {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-4">No fue posible cargar los próximos vencimientos</td></tr>';
    }
}

async function buscarPorMedicamento() {
    const id = document.getElementById('medicamentoIdBuscar').value;
    const tbody = document.getElementById('tablaBuscarLotes');
    if (!id) return;
    try {
        const resp = await fetch(`${API_LOTES}/medicamento/${id}`);
        if (!resp.ok) throw new Error();
        const lotes = await resp.json();
        if (!lotes.length) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">Sin lotes para este medicamento</td></tr>';
            return;
        }
        tbody.innerHTML = lotes.map(l => `
            <tr>
                <td>${escapeHtml(l.numeroLote)}</td>
                <td>${l.fechaFabricacion}</td>
                <td>${l.fechaVencimiento}</td>
                <td>${l.cantidadDisponible}</td>
                <td>${escapeHtml(l.estado)}</td>
            </tr>
        `).join('');
    } catch {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger py-4">No fue posible buscar los lotes</td></tr>';
    }
}

async function registrarLote(event) {
    event.preventDefault();
    const errorEl = document.getElementById('errorLote');
    const payload = {
        medicamento: {id: parseInt(document.getElementById('loteMedicamentoId').value, 10)},
        numeroLote: document.getElementById('numeroLote').value,
        fechaFabricacion: document.getElementById('fechaFabricacion').value,
        fechaVencimiento: document.getElementById('fechaVencimiento').value,
        cantidadDisponible: parseInt(document.getElementById('cantidadLote').value, 10),
    };
    try {
        const resp = await fetch(API_LOTES, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const problema = await resp.json().catch(() => null);
            throw new Error(problema?.message ?? 'No fue posible registrar el lote');
        }
        document.getElementById('formLote').reset();
        errorEl.textContent = '';
        bootstrap.Modal.getInstance(document.getElementById('modalLote')).hide();
        cargarVencidos();
        cargarProximos();
    } catch (err) {
        errorEl.textContent = err.message;
    }
    return false;
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}


async function cargarCuarentena() {
    const tbody = document.getElementById('tablaCuarentena');
    if (!tbody) return;
    try {
        const resp = await fetch('/api/lotes/cuarentena');
        if (!resp.ok) throw new Error('No se pudo cargar cuarentena');
        const items = await resp.json();
        if (!items.length) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">Sin lotes en cuarentena</td></tr>';
            return;
        }
        tbody.innerHTML = items.map(l => `
            <tr>
                <td>${escapeHtml(l.medicamento?.nombreComercial ?? ('#' + l.medicamento?.id))}</td>
                <td>${escapeHtml(l.numeroLote)}</td>
                <td>${l.fechaVencimiento ?? ''}</td>
                <td>${l.cantidadDisponible ?? 0}</td>
                <td class="text-end">
                    <button type="button" class="btn btn-sm btn-primary" onclick="liberarCuarentena(${l.id})">Liberar</button>
                </td>
            </tr>`).join('');
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-danger text-center py-4">${escapeHtml(e.message)}</td></tr>`;
    }
}

async function liberarCuarentena(loteId) {
    const regenteId = document.getElementById('regenteIdLiberar')?.value;
    if (!regenteId) {
        alert('Ingrese el id del empleado Regente que valida el lote');
        return;
    }
    const resp = await fetch(`/api/lotes/${loteId}/liberar-cuarentena?empleadoRegenteId=${regenteId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ empleadoRegenteId: Number(regenteId) }),
    });
    if (!resp.ok) {
        const err = await resp.json().catch(() => ({}));
        alert(err.message || 'No fue posible liberar el lote');
        return;
    }
    await cargarCuarentena();
}

document.getElementById('tab-cuarentena-btn')?.addEventListener('shown.bs.tab', () => cargarCuarentena());
