const API_PROVEEDORES = '/api/proveedores';
let modalProveedor;

document.addEventListener('DOMContentLoaded', () => {
    modalProveedor = new bootstrap.Modal(document.getElementById('modalProveedor'));
    cargarProveedores();
    document.getElementById('filtroRazonSocial').addEventListener('input', debounce(cargarProveedores, 300));
});

function debounce(fn, delay) {
    let timer;
    return (...args) => { clearTimeout(timer); timer = setTimeout(() => fn(...args), delay); };
}

async function cargarProveedores() {
    const razonSocial = document.getElementById('filtroRazonSocial').value.trim();
    const url = razonSocial ? `${API_PROVEEDORES}?razonSocial=${encodeURIComponent(razonSocial)}` : API_PROVEEDORES;
    const tbody = document.getElementById('tablaProveedores');
    try {
        const resp = await fetch(url);
        if (!resp.ok) throw new Error('No fue posible cargar los proveedores');
        const pagina = await resp.json();
        const proveedores = pagina.content ?? pagina;
        if (!proveedores.length) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No hay proveedores registrados</td></tr>';
            return;
        }
        // Enriquecer con resumen de stock por proveedor
        const filas = await Promise.all(proveedores.map(async (p) => {
            let productos = '—';
            let alertas = '<span class="text-muted">—</span>';
            try {
                const r = await fetch(`${API_PROVEEDORES}/${p.id}/resumen-stock`);
                if (r.ok) {
                    const s = await r.json();
                    productos = String(s.productosActivos ?? 0);
                    const bajo = s.conStockBajo ?? 0;
                    const ago = s.agotados ?? 0;
                    if (ago > 0 || bajo > 0) {
                        alertas = `${ago ? `<span class="badge bg-danger me-1">${ago} agotado(s)</span>` : ''}
                                   ${bajo ? `<span class="badge bg-warning text-dark">${bajo} bajo(s)</span>` : ''}`;
                    } else {
                        alertas = '<span class="badge bg-success">OK</span>';
                    }
                }
            } catch (_) {}
            return `<tr>
                <td>${escapeHtml(p.nit)}</td>
                <td>
                    <div class="fw-semibold">${escapeHtml(p.razonSocial)}</div>
                    <div class="small text-muted">${escapeHtml(p.correo ?? '')}</div>
                </td>
                <td>${escapeHtml(p.nombreContacto ?? '')}</td>
                <td>${escapeHtml(p.telefono ?? '')}</td>
                <td class="text-center">${productos}</td>
                <td>${alertas}</td>
                <td class="text-end text-nowrap">
                    <button class="btn btn-sm btn-outline-secondary" onclick="verAlertasProveedor(${p.id}, '${escapeHtml(p.razonSocial)}')">Ver</button>
                    <button class="btn btn-sm btn-outline-primary" onclick='abrirEdicion(${JSON.stringify(p)})'>Editar</button>
                    <button class="btn btn-sm btn-outline-danger" onclick="eliminarProveedor(${p.id})">Desactivar</button>
                </td>
            </tr>`;
        }));
        tbody.innerHTML = filas.join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">${escapeHtml(err.message)}</td></tr>`;
    }
}

function abrirNuevo() {
    document.getElementById('formProveedor').reset();
    document.getElementById('proveedorId').value = '';
    document.getElementById('tituloModal').textContent = 'Nuevo proveedor';
    document.getElementById('errorProveedor').textContent = '';
}

function abrirEdicion(p) {
    document.getElementById('proveedorId').value = p.id;
    document.getElementById('nit').value = p.nit ?? '';
    document.getElementById('razonSocial').value = p.razonSocial ?? '';
    document.getElementById('nombreContacto').value = p.nombreContacto ?? '';
    document.getElementById('telefono').value = p.telefono ?? '';
    document.getElementById('correo').value = p.correo ?? '';
    document.getElementById('direccion').value = p.direccion ?? '';
    document.getElementById('tituloModal').textContent = 'Editar proveedor';
    document.getElementById('errorProveedor').textContent = '';
    modalProveedor.show();
}

async function guardarProveedor(event) {
    event.preventDefault();
    const id = document.getElementById('proveedorId').value;
    const payload = {
        nit: document.getElementById('nit').value,
        razonSocial: document.getElementById('razonSocial').value,
        nombreContacto: document.getElementById('nombreContacto').value || null,
        telefono: document.getElementById('telefono').value || null,
        correo: document.getElementById('correo').value || null,
        direccion: document.getElementById('direccion').value || null,
    };
    try {
        const resp = await fetch(id ? `${API_PROVEEDORES}/${id}` : API_PROVEEDORES, {
            method: id ? 'PUT' : 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const problema = await resp.json().catch(() => null);
            throw new Error(problema?.message ?? 'No fue posible guardar el proveedor');
        }
        modalProveedor.hide();
        await cargarProveedores();
    } catch (err) {
        document.getElementById('errorProveedor').textContent = err.message;
    }
    return false;
}

async function eliminarProveedor(id) {
    if (!confirm('¿Desactivar este proveedor?')) return;
    const resp = await fetch(`${API_PROVEEDORES}/${id}`, {method: 'DELETE'});
    if (resp.ok) await cargarProveedores();
    else alert('No fue posible desactivar el proveedor');
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}
