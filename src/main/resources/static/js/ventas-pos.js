const API_CLIENTES = '/api/clientes';
const API_MEDICAMENTOS = '/api/medicamentos';
const API_INVENTARIO = '/api/inventario';
const API_VENTAS = '/api/ventas';

/** @type {{ medicamentoId:number, nombre:string, precioVenta:number, cantidad:number, stock?:number, requiereFormula?:boolean }[]} */
let carrito = [];
let clienteSeleccionado = null;
/** Producto actualmente en la ficha de detalle */
let productoFicha = null;
const medicamentosCache = new Map();

function escapeHtml(texto) {
    if (texto == null) return '';
    return String(texto)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function money(n) {
    return '$' + Number(n || 0).toLocaleString('es-CO');
}

function renderCarrito() {
    const cont = document.getElementById('carrito');
    const subtotal = carrito.reduce((s, i) => s + i.precioVenta * i.cantidad, 0);
    const items = carrito.reduce((s, i) => s + i.cantidad, 0);
    document.getElementById('carritoSubtotal').textContent = money(subtotal);
    document.getElementById('carritoItems').textContent = String(items);

    if (!carrito.length) {
        cont.innerHTML = '<p class="text-muted mb-0">Sin items todavia</p>';
        return;
    }

    cont.innerHTML = carrito
        .map(
            (item, idx) => `
        <div class="carrito-item">
            <div class="d-flex justify-content-between align-items-start gap-2">
                <div>
                    <div class="fw-semibold">${escapeHtml(item.nombre)}</div>
                    <div class="text-muted small">${money(item.precioVenta)} c/u
                        ${item.requiereFormula ? ' · <span class="badge badge-pendiente">Formula</span>' : ''}
                    </div>
                </div>
                <button type="button" class="btn btn-sm btn-outline-danger" onclick="quitarDelCarrito(${idx})">Quitar</button>
            </div>
            <div class="d-flex align-items-center gap-2 mt-2">
                <button type="button" class="btn btn-sm btn-outline-primary" onclick="cambiarCantidadCarrito(${idx}, -1)">−</button>
                <input type="number" class="form-control form-control-sm text-center" style="width:4.5rem"
                       min="1" value="${item.cantidad}"
                       onchange="setCantidadCarrito(${idx}, this.value)"/>
                <button type="button" class="btn btn-sm btn-outline-primary" onclick="cambiarCantidadCarrito(${idx}, 1)">+</button>
                <span class="ms-auto fw-semibold">${money(item.precioVenta * item.cantidad)}</span>
            </div>
        </div>`
        )
        .join('');
}

function cambiarCantidadCarrito(idx, delta) {
    const item = carrito[idx];
    if (!item) return;
    const nueva = item.cantidad + delta;
    if (nueva < 1) return;
    if (item.stock != null && nueva > item.stock) {
        alert('Stock insuficiente. Disponible: ' + item.stock);
        return;
    }
    item.cantidad = nueva;
    renderCarrito();
}

function setCantidadCarrito(idx, valor) {
    const item = carrito[idx];
    if (!item) return;
    let n = parseInt(valor, 10);
    if (isNaN(n) || n < 1) n = 1;
    if (item.stock != null && n > item.stock) {
        alert('Stock insuficiente. Disponible: ' + item.stock);
        n = item.stock;
    }
    item.cantidad = n;
    renderCarrito();
}

function quitarDelCarrito(idx) {
    carrito.splice(idx, 1);
    renderCarrito();
}

async function buscarMedicamentos() {
    const nombre = document.getElementById('buscarMedicamento').value.trim();
    const cont = document.getElementById('resultadosMedicamento');
    if (!nombre) {
        cont.innerHTML = '';
        return;
    }
    try {
        const resp = await fetch(
            `${API_MEDICAMENTOS}?nombre=${encodeURIComponent(nombre)}&size=20&sort=nombreComercial,asc`
        );
        if (!resp.ok) throw new Error('No fue posible buscar medicamentos');
        const pagina = await resp.json();
        const medicamentos = Array.isArray(pagina) ? pagina : (pagina.content ?? []);
        medicamentos.forEach((m) => medicamentosCache.set(m.id, m));

        if (!medicamentos.length) {
            cont.innerHTML = '<p class="text-muted small mb-0">Sin resultados</p>';
            return;
        }

        cont.innerHTML = medicamentos
            .map(
                (m) => `
            <button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
                    onclick="verProducto(${m.id})">
                <span>
                    ${escapeHtml(m.nombreComercial)}
                    ${m.requiereFormula ? '<span class="badge badge-pendiente ms-1">Formula</span>' : ''}
                    ${m.nombreGenerico ? `<span class="text-muted small d-block">${escapeHtml(m.nombreGenerico)}</span>` : ''}
                </span>
                <span class="text-nowrap ms-2">${money(m.precioVenta)}</span>
            </button>`
            )
            .join('');
    } catch (err) {
        cont.innerHTML = `<p class="text-danger small mb-0">${escapeHtml(err.message)}</p>`;
    }
}

async function verProducto(id) {
    const m = medicamentosCache.get(id);
    if (!m) return;
    productoFicha = {
        id: m.id,
        nombre: m.nombreComercial,
        precioVenta: Number(m.precioVenta),
        requiereFormula: !!m.requiereFormula,
        nombreGenerico: m.nombreGenerico,
        concentracion: m.concentracion,
        presentacion: m.presentacion,
        stock: null,
    };

    document.getElementById('fichaNombre').textContent = m.nombreComercial;
    const meta = [
        m.nombreGenerico,
        m.concentracion,
        m.presentacion,
        m.laboratorioFabricante,
    ]
        .filter(Boolean)
        .join(' · ');
    document.getElementById('fichaMeta').textContent = meta || 'Sin detalle adicional';
    document.getElementById('fichaPrecio').textContent = money(m.precioVenta);
    document.getElementById('fichaStock').textContent = '…';
    document.getElementById('fichaCantidad').value = '1';
    document.getElementById('fichaError').textContent = '';
    const badge = document.getElementById('fichaFormula');
    if (m.requiereFormula) badge.classList.remove('d-none');
    else badge.classList.add('d-none');
    document.getElementById('fichaProducto').classList.remove('d-none');
    document.getElementById('fichaProducto').scrollIntoView({ behavior: 'smooth', block: 'nearest' });

    try {
        const resp = await fetch(`${API_INVENTARIO}/medicamento/${id}`);
        if (resp.ok) {
            const inv = await resp.json();
            const stock = inv.cantidadDisponible ?? 0;
            productoFicha.stock = stock;
            document.getElementById('fichaStock').textContent = String(stock);
            if (stock <= 0) {
                document.getElementById('fichaStock').classList.add('text-danger');
            } else {
                document.getElementById('fichaStock').classList.remove('text-danger');
            }
        } else {
            document.getElementById('fichaStock').textContent = 'N/D';
        }
    } catch (_) {
        document.getElementById('fichaStock').textContent = 'N/D';
    }
}

function ajustarFichaCantidad(delta) {
    const input = document.getElementById('fichaCantidad');
    let n = parseInt(input.value, 10) || 1;
    n = Math.max(1, n + delta);
    if (productoFicha && productoFicha.stock != null && n > productoFicha.stock) {
        n = Math.max(1, productoFicha.stock);
    }
    input.value = String(n);
}

function cerrarFicha() {
    productoFicha = null;
    document.getElementById('fichaProducto').classList.add('d-none');
}

function agregarFichaAlCarrito() {
    if (!productoFicha) return;
    const err = document.getElementById('fichaError');
    err.textContent = '';
    let cantidad = parseInt(document.getElementById('fichaCantidad').value, 10);
    if (isNaN(cantidad) || cantidad < 1) {
        err.textContent = 'Indica una cantidad valida (minimo 1).';
        return;
    }
    if (productoFicha.stock != null && productoFicha.stock <= 0) {
        err.textContent = 'Sin stock disponible para este producto.';
        return;
    }
    if (productoFicha.stock != null && cantidad > productoFicha.stock) {
        err.textContent = `Stock insuficiente. Disponible: ${productoFicha.stock}`;
        return;
    }

    const existente = carrito.find((i) => i.medicamentoId === productoFicha.id);
    if (existente) {
        const total = existente.cantidad + cantidad;
        if (productoFicha.stock != null && total > productoFicha.stock) {
            err.textContent = `En carrito ya hay ${existente.cantidad}. Stock maximo: ${productoFicha.stock}`;
            return;
        }
        existente.cantidad = total;
        existente.stock = productoFicha.stock;
    } else {
        carrito.push({
            medicamentoId: productoFicha.id,
            nombre: productoFicha.nombre,
            precioVenta: productoFicha.precioVenta,
            cantidad,
            stock: productoFicha.stock,
            requiereFormula: productoFicha.requiereFormula,
        });
    }
    renderCarrito();
    cerrarFicha();
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
    el.innerHTML = `<div class="alert alert-${tipo} py-2 mb-0">${escapeHtml(texto)}</div>`;
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

    const payload = {
        cliente: { id: clienteSeleccionado.id },
        empleado: { id: empleadoId },
        metodoPago: document.getElementById('metodoPago').value,
        descuento: parseFloat(document.getElementById('descuento').value || '0'),
        detalles: carrito.map((i) => ({
            medicamento: { id: i.medicamentoId },
            cantidad: i.cantidad,
        })),
    };

    try {
        const resp = await fetch(API_VENTAS, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            let msg = 'No se pudo registrar la venta';
            try {
                const err = await resp.json();
                msg = err.message || err.error || msg;
            } catch (_) {}
            throw new Error(msg);
        }
        const venta = await resp.json();
        carrito = [];
        renderCarrito();
        mostrarMensaje('Venta registrada correctamente.', 'success');
        const tarjeta = document.getElementById('tarjetaResultado');
        tarjeta.classList.remove('d-none');
        document.getElementById('resultadoVenta').innerHTML = `
            <p class="mb-1"><strong>N.º:</strong> ${escapeHtml(venta.numeroVenta ?? venta.id)}</p>
            <p class="mb-1"><strong>Total:</strong> ${money(venta.total)}</p>
            <p class="mb-0 text-muted small">Estado: ${escapeHtml(venta.estado ?? '')}</p>`;
    } catch (err) {
        mostrarMensaje(err.message, 'danger');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    renderCarrito();
    let timer;
    const input = document.getElementById('buscarMedicamento');
    if (input) {
        input.addEventListener('input', () => {
            clearTimeout(timer);
            timer = setTimeout(() => {
                if (input.value.trim().length >= 2) buscarMedicamentos();
            }, 350);
        });
    }
});
