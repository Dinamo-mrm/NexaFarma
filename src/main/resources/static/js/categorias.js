const API_CATEGORIAS = '/api/categorias';
let modalCategoria;

document.addEventListener('DOMContentLoaded', () => {
    modalCategoria = new bootstrap.Modal(document.getElementById('modalCategoria'));
    cargarCategorias();
});

async function cargarCategorias() {
    const tbody = document.getElementById('tablaCategorias');
    try {
        const resp = await fetch(API_CATEGORIAS);
        if (!resp.ok) throw new Error('No fue posible cargar las categorías');
        const categorias = await resp.json();
        if (!categorias.length) {
            tbody.innerHTML = '<tr><td colspan="3" class="text-center text-muted py-4">No hay categorías registradas</td></tr>';
            return;
        }
        tbody.innerHTML = categorias.map(c => `
            <tr>
                <td>${escapeHtml(c.nombre)}</td>
                <td>${escapeHtml(c.descripcion ?? '')}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-primary" onclick='abrirEdicion(${JSON.stringify(c)})'>Editar</button>
                    <button class="btn btn-sm btn-outline-danger" onclick="eliminarCategoria(${c.id})">Eliminar</button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="3" class="text-center text-danger py-4">${escapeHtml(err.message)}</td></tr>`;
    }
}

function abrirNuevo() {
    document.getElementById('formCategoria').reset();
    document.getElementById('categoriaId').value = '';
    document.getElementById('tituloModal').textContent = 'Nueva categoría';
    document.getElementById('errorCategoria').textContent = '';
}

function abrirEdicion(c) {
    document.getElementById('categoriaId').value = c.id;
    document.getElementById('nombre').value = c.nombre ?? '';
    document.getElementById('descripcion').value = c.descripcion ?? '';
    document.getElementById('tituloModal').textContent = 'Editar categoría';
    document.getElementById('errorCategoria').textContent = '';
    modalCategoria.show();
}

async function guardarCategoria(event) {
    event.preventDefault();
    const id = document.getElementById('categoriaId').value;
    const payload = {
        nombre: document.getElementById('nombre').value,
        descripcion: document.getElementById('descripcion').value || null,
    };
    try {
        const resp = await fetch(id ? `${API_CATEGORIAS}/${id}` : API_CATEGORIAS, {
            method: id ? 'PUT' : 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            const problema = await resp.json().catch(() => null);
            throw new Error(problema?.message ?? 'No fue posible guardar la categoría');
        }
        modalCategoria.hide();
        await cargarCategorias();
    } catch (err) {
        document.getElementById('errorCategoria').textContent = err.message;
    }
    return false;
}

async function eliminarCategoria(id) {
    if (!confirm('¿Eliminar esta categoría?')) return;
    const resp = await fetch(`${API_CATEGORIAS}/${id}`, {method: 'DELETE'});
    if (resp.ok) await cargarCategorias();
    else alert('No fue posible eliminar la categoría (puede tener medicamentos asociados)');
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}
