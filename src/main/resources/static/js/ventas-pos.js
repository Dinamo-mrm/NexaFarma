const API_CLIENTES = '/api/clientes';
const API_MEDICAMENTOS = '/api/medicamentos';
const API_VENTAS = '/api/ventas';

let carrito = []; // { medicamentoId, nombre, precioVenta, cantidad }
let clienteSeleccionado = null;

function renderCarrito() {
    const cont = document.getElementById('carrito');
    if (!carrito.length) {
        cont.innerHTML = '<p class="text-muted">Sin items todavía</p>';
        return;
    }
    cont.innerHTML = carrito.map((item, idx) => `
        <div class="carrito-item d-flex justify-content-between align-items-center">
            <span>${escapeHtml(item.nombre)} × ${item.cantidad}</span>
            <div>
                <span class="text-muted small me-2">$${(item.precioVenta * item.cantidad).toLocaleString('es-CO')}</span>
                <button class="btn btn-sm btn-outline-danger" onclick="quitarDelCarrito(${idx})">Quitar</button>
            </div>
        </div>
    `).join('');
}

async function buscarMedicamentos() {
    const nombre = document.getElementById('buscarMedicamento').value.trim();
    const cont = document.getElementById('resultadosMedicamento');
    if (!nombre) {
        cont.innerHTML = '';
        return;
    }
    try {
        const resp = await fetch(`${API_MEDICAMENTOS}?nombre=${encodeURIComponent(nombre)}`);
        if (!resp.ok) throw new Error('No fue posible buscar medicamentos');
        const pagina = await resp.json();
        const medicamentos = pagina.content ?? pagina;
        if (!medicamentos.length) {
            cont.innerHTML = '<p class="text-muted small">Sin resultados</p>';
            return;
        }
        cont.innerHTML = medicamentos.map(m => `
            <button type="button" class="list-group-item list-group-item-action d-flex justify-content-between"
                    onclick='agregarAlCarrito(${JSON.stringify({id: m.id, nombre: m.nombreComercial, precioVenta: m.precioVenta, requiereFormula: m.requiereFormula})})'>
                <span>${escapeHtml(m.nombreComercial)} ${m.requiereFormula ? '<span class="badge bg-warning text-dark ms-1">Requiere fórmula</span>' : ''}</span>
                <span class="text-muted">$${Number(m.precioVenta).toLocaleString('es-CO')}</span>
            </button>
        `).join('');
    } catch (err) {
        cont.innerHTML = `<p class="text-danger small">${escapeHtml(err.message)}</p>`;
    }
}

function agregarAlCarrito(medicamento) {
    const existente = carrito.find(i => i.medicamentoId === medicamento.id);
    if (existente) {
        existente.cantidad += 1;
    } else {
        carrito.push({
            medicamentoId: medicamento.id,
            nombre: medicamento.nombre,
            precioVenta: medicamento.precioVenta,
            cantidad: 1,
        });
    }
    renderCarrito();
}

function quitarDelCarrito(idx) {
    carrito.splice(idx, 1);
    renderCarrito();
}

async function buscarCliente() {
    const documento = document.getElementById('clienteDocumento').value.trim();
    const info = document.getElementById('clienteEncontrado');
    if (!documento) return;
    try {
        const resp = await fetch(`${API_CLIENTES}/documento/${encodeURIComponent(documento)}`);
        if (!resp.ok) throw new Error('Cliente no encontrado');
        clienteSeleccionado = await resp.json();
        info.className = 'form-text text-success';
        info.textContent = `✓ ${clienteSeleccionado.nombreCompleto} (id ${clienteSeleccionado.id})`;
    } catch (err) {
        clienteSeleccionado = null;
        info.className = 'form-text text-danger';
        info.textContent = err.message;
    }
}

function mostrarMensaje(texto, tipo) {
    const el = document.getElementById('mensajeVenta');
    el.innerHTML = `<div class="alert alert-${tipo} py-2 mb-0">${texto}</div>`;
}

async function confirmarVenta() {
    if (!clienteSeleccionado) {
        mostrarMensaje('Busca y selecciona un cliente antes de confirmar la venta.', 'danger');
        return;
    }
    const empleadoId = parseInt(document.getElementById('empleadoId').value, 10);
    if (!empleadoId) {
        mostrarMensaje('Ingresa el id del empleado que atiende la venta.', 'danger');
        return;
    }
    if (!carrito.length) {
        mostrarMensaje('Agrega al menos un medicamento al carrito.', 'danger');
        return;
    }

    // El controlador espera la entidad Venta tal cual (no un DTO): las
    // asociaciones solo necesitan traer el id, el service resuelve el resto.
    const payload = {
        cliente: {id: clienteSeleccionado.id},
        empleado: {id: empleadoId},
        metodoPago: document.getElementById('metodoPago').value,
        descuento: parseFloat(document.getElementById('descuento').value || '0'),
        detalles: carrito.map(i => ({medicamento: {id: i.medicamentoId}, cantidad: i.cantidad})),
    };

    try {
        const resp = await fetch(API_VENTAS, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const problema = await resp.json().catch(() => null);
            throw new Error(problema?.message ?? 'No fue posible registrar la venta. Verifica stock y fórmula médica.');
        }
        const venta = await resp.json();
        mostrarMensaje(`Venta ${venta.numeroVenta} registrada correctamente.`, 'success');
        mostrarResultado(venta);
        carrito = [];
        renderCarrito();
        document.getElementById('resultadosMedicamento').innerHTML = '';
        document.getElementById('buscarMedicamento').value = '';
    } catch (err) {
        mostrarMensaje(escapeHtml(err.message), 'danger');
    }
}

function mostrarResultado(venta) {
    const tarjeta = document.getElementById('tarjetaResultado');
    const cont = document.getElementById('resultadoVenta');
    tarjeta.style.display = 'block';
    const filas = venta.detalles.map(d => `
        <tr>
            <td>${escapeHtml(d.medicamento.nombreComercial)}</td>
            <td>${d.lote ? escapeHtml(d.lote.numeroLote) : '—'}</td>
            <td>${d.cantidad}</td>
            <td>$${Number(d.precioUnitario).toLocaleString('es-CO')}</td>
            <td>$${Number(d.subtotal).toLocaleString('es-CO')}</td>
        </tr>
    `).join('');
    cont.innerHTML = `
        <p class="mb-1"><strong>N.º venta:</strong> ${escapeHtml(venta.numeroVenta)}</p>
        <p class="mb-1"><strong>Cliente:</strong> ${escapeHtml(venta.cliente.nombreCompleto)}</p>
        <table class="table table-sm">
            <thead><tr><th>Medicamento</th><th>Lote</th><th>Cant.</th><th>Precio</th><th>Subtotal</th></tr></thead>
            <tbody>${filas}</tbody>
        </table>
        <p class="mb-0 text-end">
            Subtotal: $${Number(venta.subtotal).toLocaleString('es-CO')} ·
            Descuento: $${Number(venta.descuento).toLocaleString('es-CO')} ·
            <strong>Total: $${Number(venta.total).toLocaleString('es-CO')}</strong>
        </p>
    `;
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}

document.addEventListener('DOMContentLoaded', renderCarrito);
