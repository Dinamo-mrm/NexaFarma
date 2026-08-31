const API_FORMULAS = '/api/formulas-medicas';
let contadorDetalle = 0;

document.addEventListener('DOMContentLoaded', () => {
    agregarDetalle(); // arranca con una fila
});

function agregarDetalle() {
    contadorDetalle++;
    const id = `detalle-${contadorDetalle}`;
    const cont = document.getElementById('detallesFormula');
    const div = document.createElement('div');
    div.className = 'row align-items-end mb-2';
    div.id = id;
    div.innerHTML = `
        <div class="col-md-3">
            <label class="form-label small">Id medicamento *</label>
            <input type="number" class="form-control form-control-sm med-id" required/>
        </div>
        <div class="col-md-3">
            <label class="form-label small">Dosis</label>
            <input type="text" class="form-control form-control-sm med-dosis" placeholder="Ej: 500mg"/>
        </div>
        <div class="col-md-3">
            <label class="form-label small">Frecuencia</label>
            <input type="text" class="form-control form-control-sm med-frecuencia" placeholder="Ej: Cada 8h"/>
        </div>
        <div class="col-md-2">
            <label class="form-label small">Duración</label>
            <input type="text" class="form-control form-control-sm med-duracion" placeholder="Ej: 5 días"/>
        </div>
        <div class="col-md-1">
            <button type="button" class="btn btn-sm btn-outline-danger" onclick="document.getElementById('${id}').remove()">×</button>
        </div>
    `;
    cont.appendChild(div);
}

function leerDetalles() {
    return [...document.querySelectorAll('#detallesFormula > div')].map(fila => ({
        medicamento: {id: parseInt(fila.querySelector('.med-id').value, 10)},
        dosis: fila.querySelector('.med-dosis').value || null,
        frecuencia: fila.querySelector('.med-frecuencia').value || null,
        duracionTratamiento: fila.querySelector('.med-duracion').value || null,
    })).filter(d => d.medicamento.id);
}

async function registrarFormula(event) {
    event.preventDefault();
    const errorEl = document.getElementById('errorFormula');
    errorEl.textContent = '';

    const detalles = leerDetalles();
    if (!detalles.length) {
        errorEl.textContent = 'Agrega al menos un medicamento amparado por la fórmula.';
        return false;
    }

    const payload = {
        cliente: {id: parseInt(document.getElementById('clienteId').value, 10)},
        nombreMedico: document.getElementById('nombreMedico').value,
        numeroTarjetaProfesional: document.getElementById('numeroTarjetaProfesional').value,
        entidadSalud: document.getElementById('entidadSalud').value || null,
        fechaExpedicion: document.getElementById('fechaExpedicion').value,
        duracionTratamientoDias: parseInt(document.getElementById('duracionTratamientoDias').value, 10),
        archivoAdjuntoUrl: document.getElementById('archivoAdjuntoUrl').value || null,
        detalles,
    };

    try {
        const resp = await fetch(API_FORMULAS, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const problema = await resp.json().catch(() => null);
            throw new Error(problema?.message ?? 'No fue posible registrar la fórmula médica');
        }
        const formula = await resp.json();
        alert(`Fórmula médica #${formula.id} registrada correctamente.`);
        document.getElementById('formFormula').reset();
        document.getElementById('detallesFormula').innerHTML = '';
        agregarDetalle();
    } catch (err) {
        errorEl.textContent = err.message;
    }
    return false;
}

async function listarPorCliente() {
    const clienteId = document.getElementById('clienteIdConsulta').value;
    const cont = document.getElementById('listaFormulas');
    if (!clienteId) return;
    try {
        const resp = await fetch(`${API_FORMULAS}/cliente/${clienteId}`);
        if (!resp.ok) throw new Error('No fue posible consultar las fórmulas de este cliente');
        const formulas = await resp.json();
        if (!formulas.length) {
            cont.innerHTML = '<p class="text-muted">Este cliente no tiene fórmulas registradas</p>';
            return;
        }
        cont.innerHTML = formulas.map(f => `
            <div class="border rounded p-2 mb-2">
                <div class="d-flex justify-content-between">
                    <strong>#${f.id} · ${f.nombreMedico}</strong>
                    <span class="badge ${f.vigente ? 'badge-vigente' : 'badge-vencida'}">
                        ${f.vigente ? 'Vigente' : 'Vencida'}
                    </span>
                </div>
                <div class="small text-muted">Expedida: ${f.fechaExpedicion} · ${f.duracionTratamientoDias} días</div>
                <ul class="small mb-0 mt-1">
                    ${f.detalles.map(d => `<li>${d.medicamento.nombreComercial} — ${d.dosis ?? ''} ${d.frecuencia ?? ''}</li>`).join('')}
                </ul>
            </div>
        `).join('');
    } catch (err) {
        cont.innerHTML = `<p class="text-danger">${err.message}</p>`;
    }
}
