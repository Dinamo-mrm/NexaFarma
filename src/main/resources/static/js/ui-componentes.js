/**
 * Componentes reutilizables del "Sistema de Color y Estilo" de NexaFarma.
 * Sin dependencias más allá de Bootstrap 5 (ya cargado en cada página).
 */
const NfUI = (() => {

    function debounce(fn, delay) {
        let timer;
        return (...args) => {
            clearTimeout(timer);
            timer = setTimeout(() => fn(...args), delay);
        };
    }

    function escapeHtml(value) {
        return String(value).replace(/[&<>"']/g, (c) => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
        }[c]));
    }

    /**
     * Buscador Inteligente de Medicamentos: barra blanca redondeada, icono
     * de lupa, botón "Filtros avanzados" opcional, con panel de
     * autocompletado predictivo.
     *
     * @param {string} inputId          id del <input> ya presente en el DOM
     * @param {(term:string)=>Promise<Array>} buscar   recibe el texto y devuelve resultados
     * @param {(item:any)=>string} renderItem  arma el HTML de cada resultado
     * @param {(item:any)=>void} onSeleccionar callback al hacer click en un resultado
     * @param {number} minLength  caracteres mínimos antes de buscar (default 2)
     */
    function initBuscadorInteligente({inputId, buscar, renderItem, onSeleccionar, minLength = 2}) {
        const input = document.getElementById(inputId);
        if (!input) return;

        const panelId = `${inputId}-autocompletado`;
        let panel = document.getElementById(panelId);
        if (!panel) {
            panel = document.createElement('div');
            panel.id = panelId;
            panel.className = 'autocompletado-lista';
            panel.style.display = 'none';
            input.closest('.autocompletado-panel')?.appendChild(panel)
                ?? input.parentElement.appendChild(panel);
        }

        const ejecutarBusqueda = debounce(async () => {
            const termino = input.value.trim();
            if (termino.length < minLength) {
                panel.style.display = 'none';
                panel.innerHTML = '';
                return;
            }
            try {
                const resultados = await buscar(termino);
                if (!resultados || !resultados.length) {
                    panel.innerHTML = '<div class="autocompletado-item text-muted">Sin resultados</div>';
                } else {
                    panel.innerHTML = resultados.map((item, idx) => `
                        <div class="autocompletado-item" data-idx="${idx}">${renderItem(item)}</div>
                    `).join('');
                    panel.querySelectorAll('.autocompletado-item[data-idx]').forEach(el => {
                        el.addEventListener('click', () => {
                            onSeleccionar(resultados[parseInt(el.dataset.idx, 10)]);
                            panel.style.display = 'none';
                        });
                    });
                }
                panel.style.display = 'block';
            } catch {
                panel.style.display = 'none';
            }
        }, 300);

        input.addEventListener('input', ejecutarBusqueda);
        input.addEventListener('focus', () => { if (panel.innerHTML) panel.style.display = 'block'; });
        document.addEventListener('click', (e) => {
            if (!input.contains(e.target) && !panel.contains(e.target)) panel.style.display = 'none';
        });
    }

    /**
     * Selector tipo píldora (equivalente al "Dose Scheduler Toggle"):
     * gris claro en reposo, naranja vibrante con texto blanco al
     * seleccionarse. Reemplaza los <select> de filtro por estado/tipo.
     *
     * @param {string} containerId  contenedor vacío donde se pintan las píldoras
     * @param {Array<{value:string,label:string}>} opciones
     * @param {string} valorInicial
     * @param {(value:string)=>void} onChange
     */
    function crearPillToggleGroup(containerId, opciones, valorInicial, onChange) {
        const cont = document.getElementById(containerId);
        if (!cont) return;
        cont.classList.add('pill-toggle-group');

        function pintar(valorActivo) {
            cont.innerHTML = opciones.map(op => `
                <button type="button" class="pill-toggle ${op.value === valorActivo ? 'activo' : ''}"
                        data-value="${escapeHtml(op.value)}">${escapeHtml(op.label)}</button>
            `).join('');
            cont.querySelectorAll('.pill-toggle').forEach(btn => {
                btn.addEventListener('click', () => {
                    pintar(btn.dataset.value);
                    onChange(btn.dataset.value);
                });
            });
        }
        pintar(valorInicial);
    }

    /**
     * Menú de acciones "..." (patrón de la tabla de actividad reciente).
     * Devuelve el HTML de un dropdown de Bootstrap; el contenedor de la
     * tabla debe permitir overflow visible para que no se recorte.
     *
     * @param {Array<{label:string, onClick:string, peligro?:boolean}>} acciones
     *        onClick es un string con la llamada JS (mismo patrón que el resto del proyecto)
     */
    function menuAcciones(acciones) {
        const id = `menu-${Math.random().toString(36).slice(2, 9)}`;
        return `
            <div class="dropdown">
                <button class="menu-acciones-btn" type="button" id="${id}" data-bs-toggle="dropdown" aria-expanded="false">⋯</button>
                <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="${id}">
                    ${acciones.map(a => `
                        <li><a class="dropdown-item ${a.peligro ? 'text-danger' : ''}" href="#" onclick="${a.onClick}; return false;">${escapeHtml(a.label)}</a></li>
                    `).join('')}
                </ul>
            </div>
        `;
    }

    /** Clase de badge de estado según el patrón de la tabla de actividad reciente. */
    function claseBadgeEstado(estado) {
        const completados = ['PAGADA', 'RECIBIDA', 'ENTREGADO', 'VALIDADA', 'ACTIVO'];
        const pendientes = ['PENDIENTE'];
        const enProgreso = ['EN_CAMINO', 'EN_PREPARACION', 'PARCIALMENTE_RECIBIDA'];
        const cancelados = ['ANULADA', 'CANCELADA', 'CANCELADO', 'DEVUELTA', 'VENCIDO', 'RETIRADO'];
        if (completados.includes(estado)) return 'badge-completado';
        if (pendientes.includes(estado)) return 'badge-pendiente';
        if (enProgreso.includes(estado)) return 'badge-progreso';
        if (cancelados.includes(estado)) return 'badge-cancelado';
        return 'badge-progreso';
    }

    return {debounce, escapeHtml, initBuscadorInteligente, crearPillToggleGroup, menuAcciones, claseBadgeEstado};
})();
