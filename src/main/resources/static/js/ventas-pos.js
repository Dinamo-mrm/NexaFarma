const API_CLIENTES = '/api/clientes';
const API_MEDICAMENTOS = '/api/medicamentos';
const API_INVENTARIO = '/api/inventario';
const API_VENTAS = '/api/ventas';
const API_LOTES = '/api/lotes';
const API_DOMICILIOS = '/api/domicilios';
const API_FORMULAS = '/api/formulas-medicas';

let carrito = [];
let clienteSeleccionado = null;
let productoFicha = null;
let editandoIdx = null;
let formulaMedicaIdActiva = null;
let formulasPorMedicamento = {};
let pendienteFormula = null;
let ultimaVenta = null;
let modalPostVenta = null;

document.addEventListener('DOMContentLoaded', () => {
    const fc = document.getElementById('fichaCantidad');
    if (fc) fc.addEventListener('input', actualizarEquivalenciaFicha);
    const input = document.getElementById('buscarMedicamento');
    if (input) {
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                buscarMedicamentos();
            }
        });
    }
    const el = document.getElementById('modalPostVenta');
    if (el) modalPostVenta = new bootstrap.Modal(el);
    toggleVentaAnonima();
});

function money(n) {
    return '$' + Number(n || 0).toLocaleString('es-CO', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}
function escapeHtml(v) {
    return String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function toggleVentaAnonima() {
    const anon = document.getElementById('ventaAnonima')?.checked;
    const info = document.getElementById('clienteEncontrado');
    if (anon) {
        clienteSeleccionado = null;
        if (document.getElementById('clienteDocumento')) document.getElementById('clienteDocumento').value = '';
        if (info) {
            info.className = 'form-text text-muted';
            info.textContent = 'Consumidor final — sin cliente asociado';
        }
    } else if (info && !clienteSeleccionado) {
        info.textContent = 'Busque o registre un cliente (obligatorio si hay controlados/fórmula)';
    }
}

function togglePagosMixtos() {
    const mix = document.getElementById('metodoPago')?.value === 'PAGO_MIXTO';
    document.getElementById('bloquePagosMixtos')?.classList.toggle('d-none', !mix);
}

function unidadesDesdeModo(cantidadUi, modo, factorCaja, factorBlister) {
    const q = cantidadUi || 1;
    const fc = factorCaja > 0 ? factorCaja : 1;
    const fb = factorBlister > 0 ? factorBlister : 1;
    if (modo === 'CAJA') return q * fc;
    if (modo === 'BLISTER') return q * fb;
    return q;
}

function onCambioModoVenta() { actualizarEquivalenciaFicha(); }

function actualizarEquivalenciaFicha() {
    if (!productoFicha) return;
    const factor = productoFicha.factorConversion || 1;
    const factorBlister = productoFicha.factorBlister || 1;
    const unidad = productoFicha.unidadMinima || 'UNIDAD';
    const modo = document.getElementById('fichaModoVenta')?.value || 'UNIDAD';
    const qty = parseInt(document.getElementById('fichaCantidad')?.value || '1', 10) || 1;
    const unidades = unidadesDesdeModo(qty, modo, factor, factorBlister);
    const info = document.getElementById('fichaFactorInfo');
    const eq = document.getElementById('fichaEquivale');
    if (info) {
        const parts = [];
        if (factor > 1) parts.push(`1 caja = ${factor} ${unidad}`);
        if (factorBlister > 1) parts.push(`1 blíster = ${factorBlister} ${unidad}`);
        if (!productoFicha.aptoFraccionamiento) parts.push('No fraccionable');
        info.textContent = parts.join(' · ') || 'Por unidad';
    }
    if (eq) eq.textContent = `= ${unidades} ${unidad}`;
    const sel = document.getElementById('fichaModoVenta');
    if (sel) {
        const optCaja = sel.querySelector('option[value="CAJA"]');
        const optBlister = sel.querySelector('option[value="BLISTER"]');
        if (optCaja) optCaja.disabled = factor <= 1;
        if (optBlister) optBlister.disabled = factorBlister <= 1;
    }
}

async function cargarLotesFicha(medicamentoId) {
    const el = document.getElementById('fichaLotes');
    if (!el) return;
    try {
        const resp = await fetch(`${API_LOTES}?medicamentoId=${medicamentoId}`);
        if (!resp.ok) throw new Error();
        const lotes = await resp.json();
        const activos = (lotes || []).filter(l => l.estado === 'ACTIVO' && (l.cantidadDisponible > 0));
        activos.sort((a, b) => String(a.fechaVencimiento || '').localeCompare(String(b.fechaVencimiento || '')));
        if (!activos.length) {
            el.innerHTML = '<span class="text-warning">Sin lotes activos con stock</span>';
            return;
        }
        el.innerHTML = activos.slice(0, 5).map(l =>
            `<code>${escapeHtml(l.numeroLote)}</code> venc.${escapeHtml(l.fechaVencimiento || '—')} <strong>${l.cantidadDisponible}</strong>ud`
        ).join(' · ');
    } catch {
        el.textContent = 'No se pudieron cargar lotes';
    }
}

function renderCarrito() {
    const cont = document.getElementById('carrito');
    const subtotal = carrito.reduce((s, i) => s + i.precioVenta * i.cantidad, 0);
    const items = carrito.reduce((s, i) => s + i.cantidad, 0);
    document.getElementById('carritoSubtotal').textContent = money(subtotal);
    document.getElementById('carritoItems').textContent = String(items);
    if (!carrito.length) {
        cont.innerHTML = '<p class="text-muted mb-0">Sin ítems</p>';
        return;
    }
    cont.innerHTML = carrito.map((item, idx) => `
        <div class="border rounded p-2 mb-2">
            <div class="d-flex justify-content-between gap-2">
                <div>
                    <div class="fw-semibold">${escapeHtml(item.nombre)}</div>
                    <div class="text-muted small">${money(item.precioVenta)} c/u
                        ${item.proveedor ? ' · ' + escapeHtml(item.proveedor) : ''}
                        ${item.stock != null ? ' · Stock: ' + item.stock : ''}
                        ${item.requiereFormula ? ' · <span class="badge text-bg-warning">Fórmula</span>' : ''}
                    </div>
                </div>
                <div class="btn-group btn-group-sm">
                    <button type="button" class="btn btn-outline-secondary" title="Editar" onclick="editarItemCarrito(${idx})">✎</button>
                    <button type="button" class="btn btn-outline-danger" onclick="quitarDelCarrito(${idx})">×</button>
                </div>
            </div>
            <div class="d-flex align-items-center gap-2 mt-2">
                <button type="button" class="btn btn-sm btn-outline-primary" onclick="cambiarCantidadCarrito(${idx}, -1)">−</button>
                <input type="number" class="form-control form-control-sm text-center" style="width:4.5rem"
                       min="1" value="${item.cantidad}" onchange="setCantidadCarrito(${idx}, this.value)"/>
                <button type="button" class="btn btn-sm btn-outline-primary" onclick="cambiarCantidadCarrito(${idx}, 1)">+</button>
                <span class="ms-auto fw-semibold">${money(item.precioVenta * item.cantidad)}</span>
            </div>
        </div>`).join('');
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

function editarItemCarrito(idx) {
    const item = carrito[idx];
    if (!item) return;
    editandoIdx = idx;
    // Reabrir ficha con datos del ítem
    productoFicha = {
        id: item.medicamentoId,
        medicamentoId: item.medicamentoId,
        nombre: item.nombre,
        precioVenta: item.precioVenta,
        stock: item.stock,
        stockMinimo: item.stockMinimo || 0,
        requiereFormula: item.requiereFormula,
        usoControlado: item.usoControlado,
        factorConversion: item.factorConversion || 1,
        factorBlister: item.factorBlister || 1,
        unidadMinima: item.unidadMinima || 'UNIDAD',
        aptoFraccionamiento: item.aptoFraccionamiento,
        proveedor: item.proveedor,
    };
    document.getElementById('fichaProducto').classList.remove('d-none');
    document.getElementById('fichaNombre').textContent = item.nombre;
    document.getElementById('fichaPrecio').textContent = money(item.precioVenta);
    document.getElementById('fichaStock').textContent = item.stock != null ? String(item.stock) : '—';
    document.getElementById('fichaProveedor').textContent = item.proveedor ? 'Proveedor: ' + item.proveedor : '';
    document.getElementById('fichaCantidad').value = String(item.cantidad);
    document.getElementById('fichaModoVenta').value = 'UNIDAD';
    document.getElementById('fichaFormula').classList.toggle('d-none', !item.requiereFormula && !item.usoControlado);
    actualizarEquivalenciaFicha();
    cargarLotesFicha(item.medicamentoId);
    document.getElementById('fichaError').textContent = 'Editando ítem del carrito — al agregar se actualizará';
}

async function buscarMedicamentos() {
    const nombre = document.getElementById('buscarMedicamento').value.trim();
    const cont = document.getElementById('resultadosBusqueda');
    if (nombre.length < 2) {
        cont.innerHTML = '<p class="text-muted small">Escriba al menos 2 caracteres</p>';
        return;
    }
    try {
        const resp = await fetch(`${API_MEDICAMENTOS}?nombre=${encodeURIComponent(nombre)}&size=15`);
        if (!resp.ok) throw new Error('Error al buscar');
        const page = await resp.json();
        const list = page.content || page;
        if (!list.length) {
            cont.innerHTML = '<p class="text-muted small">Sin resultados</p>';
            return;
        }
        cont.innerHTML = list.map(m => {
            const prov = m.proveedor?.razonSocial || m.proveedor?.nombre || '';
            return `<button type="button" class="list-group-item list-group-item-action"
                onclick='seleccionarMedicamento(${JSON.stringify(m).replace(/'/g, "&#39;")})'>
                <div class="fw-semibold">${escapeHtml(m.nombreComercial)}</div>
                <div class="small text-muted">${escapeHtml(m.presentacion || '')}
                    ${prov ? ' · ' + escapeHtml(prov) : ''}
                    · ${money(m.precioVenta)}</div>
            </button>`;
        }).join('');
    } catch (e) {
        cont.innerHTML = `<p class="text-danger small">${escapeHtml(e.message)}</p>`;
    }
}

async function seleccionarMedicamento(m) {
    // Prefer re-fetch for proveedor + stock
    let med = m;
    try {
        const r = await fetch(`${API_MEDICAMENTOS}/${m.id}`);
        if (r.ok) med = await r.json();
    } catch (_) {}

    editandoIdx = null;
    productoFicha = {
        id: med.id,
        medicamentoId: med.id,
        nombre: med.nombreComercial,
        precioVenta: Number(med.precioVenta),
        requiereFormula: !!med.requiereFormula,
        usoControlado: !!med.usoControlado,
        factorConversion: med.factorConversion != null ? Number(med.factorConversion) : 1,
        factorBlister: med.factorBlister != null ? Number(med.factorBlister) : 1,
        unidadMinima: med.unidadMinima || 'UNIDAD',
        aptoFraccionamiento: !!med.aptoFraccionamiento,
        stockMinimo: med.stockMinimo != null ? Number(med.stockMinimo) : 0,
        proveedor: med.proveedor?.razonSocial || med.proveedor?.nombre || null,
        stock: null,
    };

    document.getElementById('fichaProducto').classList.remove('d-none');
    document.getElementById('fichaNombre').textContent = productoFicha.nombre;
    document.getElementById('fichaMeta').textContent =
        [med.nombreGenerico, med.concentracion, med.presentacion].filter(Boolean).join(' · ');
    document.getElementById('fichaProveedor').innerHTML = productoFicha.proveedor
        ? `<span class="text-primary">Proveedor: ${escapeHtml(productoFicha.proveedor)}</span>`
        : '<span class="text-muted">Proveedor: no disponible</span>';
    document.getElementById('fichaPrecio').textContent = money(productoFicha.precioVenta);
    document.getElementById('fichaCantidad').value = '1';
    document.getElementById('fichaModoVenta').value = 'UNIDAD';
    document.getElementById('fichaFormula').classList.toggle('d-none', !(productoFicha.requiereFormula || productoFicha.usoControlado));
    document.getElementById('fichaError').textContent = '';
    document.getElementById('fichaStock').textContent = '…';
    actualizarEquivalenciaFicha();
    cargarLotesFicha(med.id);

    // Stock
    try {
        const invResp = await fetch(`${API_INVENTARIO}/medicamento/${med.id}`);
        if (invResp.ok) {
            const inv = await invResp.json();
            const stock = inv.cantidadDisponible ?? 0;
            productoFicha.stock = stock;
            if (inv.stockMinimo != null) productoFicha.stockMinimo = inv.stockMinimo;
            document.getElementById('fichaStock').textContent = String(stock);
            const badge = document.getElementById('fichaBajoStock');
            if (badge) {
                const min = productoFicha.stockMinimo || 0;
                if (stock <= 0) {
                    badge.classList.remove('d-none');
                    badge.textContent = 'Sin stock';
                    badge.className = 'badge bg-danger';
                } else if (min > 0 && stock <= min) {
                    badge.classList.remove('d-none');
                    badge.textContent = 'Bajo stock';
                    badge.className = 'badge bg-warning text-dark';
                } else {
                    badge.classList.add('d-none');
                }
            }
        } else {
            productoFicha.stock = 0;
            document.getElementById('fichaStock').textContent = '0';
            document.getElementById('fichaBajoStock').classList.remove('d-none');
            document.getElementById('fichaBajoStock').textContent = 'Sin inventario';
            document.getElementById('fichaBajoStock').className = 'badge bg-secondary';
        }
    } catch {
        document.getElementById('fichaStock').textContent = '—';
    }
}

function cerrarFicha() {
    document.getElementById('fichaProducto').classList.add('d-none');
    productoFicha = null;
    editandoIdx = null;
}

function ajustarFichaCantidad(delta) {
    const el = document.getElementById('fichaCantidad');
    let n = parseInt(el.value, 10) || 1;
    n = Math.max(1, n + delta);
    if (productoFicha && productoFicha.stock != null) {
        const modo = document.getElementById('fichaModoVenta')?.value || 'UNIDAD';
        const und = unidadesDesdeModo(n, modo, productoFicha.factorConversion || 1, productoFicha.factorBlister || 1);
        if (und > productoFicha.stock) return;
    }
    el.value = String(n);
    actualizarEquivalenciaFicha();
}

async function agregarFichaAlCarrito() {
    const err = document.getElementById('fichaError');
    err.textContent = '';
    if (!productoFicha) return;

    const cantidadUi = parseInt(document.getElementById('fichaCantidad').value, 10) || 1;
    const modo = document.getElementById('fichaModoVenta')?.value || 'UNIDAD';
    const factor = productoFicha.factorConversion || 1;
    const factorBlister = productoFicha.factorBlister || 1;
    if ((modo === 'CAJA' && factor <= 1) || (modo === 'BLISTER' && factorBlister <= 1)) {
        err.textContent = 'Este producto no tiene factor para ese modo de venta.';
        return;
    }
    if (modo === 'UNIDAD' && !productoFicha.aptoFraccionamiento && factor > 1 && (cantidadUi % factor !== 0)) {
        err.textContent = `No admite fraccionamiento. Use múltiplos de ${factor} o modo Caja.`;
        return;
    }
    const cantidad = unidadesDesdeModo(cantidadUi, modo, factor, factorBlister);

    if (productoFicha.stock != null && productoFicha.stock <= 0) {
        err.textContent = 'Sin stock disponible.';
        return;
    }
    if (productoFicha.stock != null && cantidad > productoFicha.stock) {
        err.textContent = `Stock insuficiente. Disponible: ${productoFicha.stock}`;
        return;
    }

    const necesitaFormula = !!(productoFicha.requiereFormula || productoFicha.usoControlado);
    if (necesitaFormula && !formulasPorMedicamento[productoFicha.medicamentoId] && !formulaMedicaIdActiva) {
        if (!clienteSeleccionado) {
            err.textContent = 'Seleccione o registre un cliente para medicamentos con fórmula/controlados.';
            document.getElementById('ventaAnonima').checked = false;
            return;
        }
        pendienteFormula = { ...productoFicha, cantidad, cantidadUi, modo };
        abrirModalFormula(productoFicha);
        return;
    }

    const itemData = {
        medicamentoId: productoFicha.medicamentoId || productoFicha.id,
        nombre: productoFicha.nombre,
        precioVenta: productoFicha.precioVenta,
        cantidad,
        stock: productoFicha.stock,
        stockMinimo: productoFicha.stockMinimo,
        requiereFormula: productoFicha.requiereFormula,
        usoControlado: productoFicha.usoControlado,
        factorConversion: factor,
        factorBlister,
        unidadMinima: productoFicha.unidadMinima || 'UNIDAD',
        aptoFraccionamiento: productoFicha.aptoFraccionamiento,
        proveedor: productoFicha.proveedor,
        modoVenta: modo,
        cantidadUi,
    };

    if (editandoIdx != null && carrito[editandoIdx]) {
        carrito[editandoIdx] = itemData;
        editandoIdx = null;
    } else {
        const existente = carrito.find(i => i.medicamentoId === itemData.medicamentoId);
        if (existente) {
            const total = existente.cantidad + cantidad;
            if (itemData.stock != null && total > itemData.stock) {
                err.textContent = `En carrito ya hay ${existente.cantidad}. Máximo: ${itemData.stock}`;
                return;
            }
            existente.cantidad = total;
            existente.stock = itemData.stock;
        } else {
            carrito.push(itemData);
        }
    }
    renderCarrito();
    cerrarFicha();
}

function abrirModalFormula(prod) {
    document.getElementById('formulaMedNombre').textContent = prod.nombre || '';
    document.getElementById('errorFormulaPos').textContent = '';
    new bootstrap.Modal(document.getElementById('modalFormulaPos')).show();
}

function cancelarFormulaPos() {
    pendienteFormula = null;
}

async function guardarFormulaPos(event) {
    event.preventDefault();
    const err = document.getElementById('errorFormulaPos');
    err.textContent = '';
    if (!clienteSeleccionado) {
        err.textContent = 'Cliente requerido';
        return false;
    }
    const payload = {
        cliente: { id: clienteSeleccionado.id },
        nombreMedico: document.getElementById('fMedico').value,
        tarjetaProfesional: document.getElementById('fTarjeta').value,
        entidadSalud: document.getElementById('fEntidad').value || null,
        fechaExpedicion: document.getElementById('fFecha').value,
        dosis: document.getElementById('fDosis').value || null,
        frecuencia: document.getElementById('fFrecuencia').value || null,
        adjuntoUrl: document.getElementById('fAdjunto').value || null,
        detalles: pendienteFormula ? [{
            medicamento: { id: pendienteFormula.medicamentoId || pendienteFormula.id },
            cantidad: pendienteFormula.cantidad || 1,
        }] : [],
    };
    try {
        const resp = await fetch(API_FORMULAS, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const e = await resp.json().catch(() => ({}));
            throw new Error(e.message || 'No se pudo registrar la fórmula');
        }
        const f = await resp.json();
        formulaMedicaIdActiva = f.id;
        if (pendienteFormula) {
            formulasPorMedicamento[pendienteFormula.medicamentoId || pendienteFormula.id] = f.id;
            const p = pendienteFormula;
            pendienteFormula = null;
            bootstrap.Modal.getInstance(document.getElementById('modalFormulaPos'))?.hide();
            // force add
            const prev = productoFicha;
            productoFicha = p;
            await agregarFichaAlCarrito();
            productoFicha = prev;
        }
    } catch (ex) {
        err.textContent = ex.message;
    }
    return false;
}

async function buscarCliente() {
    document.getElementById('ventaAnonima').checked = false;
    const documento = document.getElementById('clienteDocumento').value.trim();
    const info = document.getElementById('clienteEncontrado');
    if (!documento) {
        info.textContent = 'Ingrese un documento';
        return;
    }
    try {
        const resp = await fetch(`${API_CLIENTES}/documento/${encodeURIComponent(documento)}`);
        if (!resp.ok) throw new Error('Cliente no encontrado');
        clienteSeleccionado = await resp.json();
        info.className = 'form-text text-success';
        info.textContent = `✓ ${clienteSeleccionado.nombreCompleto} (${clienteSeleccionado.documento})`;
    } catch (e) {
        clienteSeleccionado = null;
        info.className = 'form-text text-danger';
        info.textContent = e.message + ' — puede crear uno con + Nuevo';
    }
}

async function guardarClientePos(event) {
    event.preventDefault();
    const err = document.getElementById('errorClientePos');
    err.textContent = '';
    const payload = {
        tipoDocumento: document.getElementById('cPosTipoDoc')?.value || 'CC',
        documento: document.getElementById('cPosDoc').value,
        nombreCompleto: document.getElementById('cPosNombre').value,
        telefono: document.getElementById('cPosTel').value || null,
        direccion: document.getElementById('cPosDir').value || null,
        correo: document.getElementById('cPosCorreo').value || null,
        autorizaTratamientoDatos: document.getElementById('cPosHabeas').checked,
    };
    try {
        const resp = await fetch(API_CLIENTES, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const e = await resp.json().catch(() => ({}));
            throw new Error(e.message || 'No se pudo crear');
        }
        const c = await resp.json();
        clienteSeleccionado = c;
        document.getElementById('ventaAnonima').checked = false;
        document.getElementById('clienteDocumento').value = c.documento || '';
        const info = document.getElementById('clienteEncontrado');
        info.className = 'form-text text-success';
        info.textContent = `✓ ${c.nombreCompleto}`;
        bootstrap.Modal.getInstance(document.getElementById('modalClientePos'))?.hide();
    } catch (ex) {
        err.textContent = ex.message;
    }
    return false;
}

async function confirmarVenta() {
    const out = document.getElementById('resultadoVenta');
    out.innerHTML = '';
    if (!carrito.length) {
        out.innerHTML = '<div class="alert alert-warning py-2">El carrito está vacío</div>';
        return;
    }
    const empleadoId = parseInt(document.getElementById('empleadoId').value, 10);
    if (!empleadoId) {
        out.innerHTML = '<div class="alert alert-warning py-2">Indique el empleado</div>';
        return;
    }
    const anon = document.getElementById('ventaAnonima')?.checked;
    if (!anon && !clienteSeleccionado) {
        // allow if no controlled drugs
        const needClient = carrito.some(i => i.requiereFormula || i.usoControlado);
        if (needClient) {
            out.innerHTML = '<div class="alert alert-warning py-2">Cliente obligatorio por medicamentos controlados/fórmula</div>';
            return;
        }
    }

    const metodo = document.getElementById('metodoPago').value;
    const descuento = parseFloat(document.getElementById('descuento').value || '0') || 0;
    const payload = {
        empleado: { id: empleadoId },
        metodoPago: metodo,
        descuento,
        detalles: carrito.map(i => ({
            medicamento: { id: i.medicamentoId },
            cantidad: i.cantidad,
        })),
    };
    if (!anon && clienteSeleccionado) {
        payload.cliente = { id: clienteSeleccionado.id };
    }
    if (formulaMedicaIdActiva) {
        payload.formulaMedica = { id: formulaMedicaIdActiva };
    }
    if (metodo === 'PAGO_MIXTO') {
        payload.pagos = [];
        const e = parseFloat(document.getElementById('pagoEfectivo').value || '0') || 0;
        const t = parseFloat(document.getElementById('pagoTarjeta').value || '0') || 0;
        const tr = parseFloat(document.getElementById('pagoTransfer').value || '0') || 0;
        if (e > 0) payload.pagos.push({ metodoPago: 'EFECTIVO', monto: e });
        if (t > 0) payload.pagos.push({ metodoPago: 'TARJETA_DEBITO', monto: t });
        if (tr > 0) payload.pagos.push({ metodoPago: 'TRANSFERENCIA', monto: tr });
    }

    try {
        const resp = await fetch(API_VENTAS, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const e = await resp.json().catch(() => ({}));
            throw new Error(e.message || 'No se pudo registrar la venta');
        }
        const venta = await resp.json();
        ultimaVenta = venta;
        carrito = [];
        renderCarrito();
        formulaMedicaIdActiva = null;
        formulasPorMedicamento = {};

        document.getElementById('postVentaResumen').innerHTML =
            `Venta <strong>${escapeHtml(venta.numeroVenta || ('#' + venta.id))}</strong> · Total ${money(venta.total)}`;
        document.getElementById('postVentaLinks').innerHTML =
            `<a class="btn btn-sm btn-dark me-1" target="_blank" href="/ventas/${venta.id}/factura">Ver factura</a>
             <a class="btn btn-sm btn-outline-dark" href="/ventas/${venta.id}/factura.pdf">PDF</a>`;
        document.getElementById('formDomicilioPost').classList.add('d-none');
        modalPostVenta?.show();
        out.innerHTML = `<div class="alert alert-success py-2">Venta ${escapeHtml(venta.numeroVenta || '')} OK</div>`;
    } catch (ex) {
        out.innerHTML = `<div class="alert alert-danger py-2">${escapeHtml(ex.message)}</div>`;
    }
}

function marcarEntregadoMostrador() {
    modalPostVenta?.hide();
    document.getElementById('resultadoVenta').innerHTML +=
        '<div class="alert alert-info py-2 mt-2">Entrega en mostrador confirmada</div>';
}

function mostrarFormDomicilioPost() {
    document.getElementById('formDomicilioPost').classList.remove('d-none');
    if (clienteSeleccionado) {
        if (clienteSeleccionado.direccion) document.getElementById('postDir').value = clienteSeleccionado.direccion;
        if (clienteSeleccionado.telefono) document.getElementById('postTel').value = clienteSeleccionado.telefono;
    }
}

async function crearDomicilioPost() {
    const err = document.getElementById('errorPostDom');
    err.textContent = '';
    if (!ultimaVenta) {
        err.textContent = 'No hay venta reciente';
        return;
    }
    // Cliente opcional: domicilio puede ir a consumidor final
    const dir = document.getElementById('postDir').value.trim();
    const tel = document.getElementById('postTel').value.trim();
    if (!dir || !tel) {
        err.textContent = 'Dirección y teléfono obligatorios';
        return;
    }
    const payload = {
        venta: { id: ultimaVenta.id },
        direccionEntrega: dir,
        telefonoContacto: tel,
        valorDomicilio: parseFloat(document.getElementById('postValorDom').value || '0') || 0,
    };
    if (clienteSeleccionado && clienteSeleccionado.id) {
        payload.cliente = { id: clienteSeleccionado.id };
    }
    try {
        const resp = await fetch(API_DOMICILIOS, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const e = await resp.json().catch(() => ({}));
            throw new Error(e.message || 'No se pudo crear el domicilio');
        }
        modalPostVenta?.hide();
        document.getElementById('resultadoVenta').innerHTML +=
            '<div class="alert alert-info py-2 mt-2">Domicilio creado · <a href="/domicilios">Ver tablero</a></div>';
    } catch (ex) {
        err.textContent = ex.message;
    }
}
