const API_MEDICAMENTOS = '/api/medicamentos';
const API_CATEGORIAS = '/api/categorias';
const API_PROVEEDORES = '/api/proveedores';
let modalMedicamento;

document.addEventListener('DOMContentLoaded', async () => {
    modalMedicamento = new bootstrap.Modal(document.getElementById('modalMedicamento'));
    await Promise.all([cargarSelectCategorias(), cargarSelectProveedores()]);
    cargarMedicamentos();
    document.getElementById('filtroNombre').addEventListener('input', debounce(cargarMedicamentos, 300));
});

function debounce(fn, delay) {
    let timer;
    return (...args) => { clearTimeout(timer); timer = setTimeout(() => fn(...args), delay); };
}

async function cargarSelectCategorias() {
    const select = document.getElementById('categoriaId');
    try {
        const resp = await fetch(API_CATEGORIAS);
        const categorias = await resp.json();
        select.innerHTML = categorias.map(c => `<option value="${c.id}">${escapeHtml(c.nombre)}</option>`).join('');
    } catch {
        select.innerHTML = '<option value="">No fue posible cargar categorías</option>';
    }
}

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

async function cargarMedicamentos() {
    const nombre = document.getElementById('filtroNombre').value.trim();
    const url = nombre ? `${API_MEDICAMENTOS}?nombre=${encodeURIComponent(nombre)}` : API_MEDICAMENTOS;
    const tbody = document.getElementById('tablaMedicamentos');
    try {
        const resp = await fetch(url);
        if (!resp.ok) throw new Error('No fue posible cargar los medicamentos');
        const pagina = await resp.json();
        const medicamentos = pagina.content ?? pagina;
        if (!medicamentos.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No hay medicamentos registrados</td></tr>';
            return;
        }
        tbody.innerHTML = medicamentos.map(m => `
            <tr>
                <td>${escapeHtml(m.nombreComercial)}</td>
                <td>${escapeHtml(m.categoria?.nombre ?? '—')}</td>
                <td>${escapeHtml(m.registroInvima ?? '')}</td>
                <td>$${Number(m.precioVenta).toLocaleString('es-CO')}</td>
                <td>
                    ${m.requiereFormula ? '<span class="badge bg-warning text-dark">Fórmula</span>' : ''}
                    ${m.usoControlado ? '<span class="badge bg-danger">Controlado</span>' : ''}
                    ${m.requiereRefrigeracion ? '<span class="badge bg-info text-dark">Refrigeración</span>' : ''}
                </td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-primary" onclick='abrirEdicion(${JSON.stringify(m)})'>Editar</button>
                    <button class="btn btn-sm btn-outline-danger" onclick="eliminarMedicamento(${m.id})">Desactivar</button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger py-4">${escapeHtml(err.message)}</td></tr>`;
    }
}

function abrirNuevo() {
    document.getElementById('formMedicamento').reset();
    document.getElementById('medicamentoId').value = '';
    document.getElementById('ventaLibre').checked = true;
    document.getElementById('stockMinimo').value = 10;
    document.getElementById('tituloModal').textContent = 'Nuevo medicamento';
    document.getElementById('errorMedicamento').textContent = '';
}

function abrirEdicion(m) {
    document.getElementById('medicamentoId').value = m.id;
    document.getElementById('nombreComercial').value = m.nombreComercial ?? '';
    document.getElementById('nombreGenerico').value = m.nombreGenerico ?? '';
    document.getElementById('codigoInterno').value = m.codigoInterno ?? '';
    document.getElementById('codigoBarras').value = m.codigoBarras ?? '';
    document.getElementById('registroInvima').value = m.registroInvima ?? '';
    document.getElementById('categoriaId').value = m.categoria?.id ?? '';
    document.getElementById('proveedorId').value = m.proveedor?.id ?? '';
    document.getElementById('presentacion').value = m.presentacion ?? '';
    document.getElementById('concentracion').value = m.concentracion ?? '';
    document.getElementById('laboratorioFabricante').value = m.laboratorioFabricante ?? '';
    document.getElementById('precioCompra').value = m.precioCompra ?? '';
    document.getElementById('precioVenta').value = m.precioVenta ?? '';
    document.getElementById('stockMinimo').value = m.stockMinimo ?? 10;
    document.getElementById('ubicacion').value = m.ubicacion ?? '';
    document.getElementById('ventaLibre').checked = !!m.ventaLibre;
    document.getElementById('requiereFormula').checked = !!m.requiereFormula;
    document.getElementById('usoControlado').checked = !!m.usoControlado;
    document.getElementById('requiereRefrigeracion').checked = !!m.requiereRefrigeracion;
    document.getElementById('tituloModal').textContent = 'Editar medicamento';
    document.getElementById('errorMedicamento').textContent = '';
    modalMedicamento.show();
}

async function guardarMedicamento(event) {
    event.preventDefault();
    const id = document.getElementById('medicamentoId').value;
    const payload = {
        nombreComercial: document.getElementById('nombreComercial').value,
        nombreGenerico: document.getElementById('nombreGenerico').value || null,
        codigoInterno: document.getElementById('codigoInterno').value,
        codigoBarras: document.getElementById('codigoBarras').value || null,
        registroInvima: document.getElementById('registroInvima').value,
        categoria: {id: parseInt(document.getElementById('categoriaId').value, 10)},
        proveedor: {id: parseInt(document.getElementById('proveedorId').value, 10)},
        presentacion: document.getElementById('presentacion').value || null,
        concentracion: document.getElementById('concentracion').value || null,
        laboratorioFabricante: document.getElementById('laboratorioFabricante').value || null,
        precioCompra: parseFloat(document.getElementById('precioCompra').value),
        precioVenta: parseFloat(document.getElementById('precioVenta').value),
        stockMinimo: parseInt(document.getElementById('stockMinimo').value || '10', 10),
        ubicacion: document.getElementById('ubicacion').value || null,
        ventaLibre: document.getElementById('ventaLibre').checked,
        requiereFormula: document.getElementById('requiereFormula').checked,
        usoControlado: document.getElementById('usoControlado').checked,
        requiereRefrigeracion: document.getElementById('requiereRefrigeracion').checked,
    };
    try {
        const resp = await fetch(id ? `${API_MEDICAMENTOS}/${id}` : API_MEDICAMENTOS, {
            method: id ? 'PUT' : 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const problema = await resp.json().catch(() => null);
            throw new Error(problema?.message ?? 'No fue posible guardar el medicamento');
        }
        modalMedicamento.hide();
        await cargarMedicamentos();
    } catch (err) {
        document.getElementById('errorMedicamento').textContent = err.message;
    }
    return false;
}

async function eliminarMedicamento(id) {
    if (!confirm('¿Desactivar este medicamento?')) return;
    const resp = await fetch(`${API_MEDICAMENTOS}/${id}`, {method: 'DELETE'});
    if (resp.ok) await cargarMedicamentos();
    else alert('No fue posible desactivar el medicamento');
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}
