(function () {
  'use strict';

  const $ = (sel) => document.querySelector(sel);
  const $$ = (sel) => Array.from(document.querySelectorAll(sel));

  function hoyISO() {
    return new Date().toISOString().slice(0, 10);
  }

  function haceDias(n) {
    const d = new Date();
    d.setDate(d.getDate() - n);
    return d.toISOString().slice(0, 10);
  }

  function inicioMes() {
    const d = new Date();
    return new Date(d.getFullYear(), d.getMonth(), 1).toISOString().slice(0, 10);
  }

  function params() {
    const desde = $('#filtroDesde').value || inicioMes();
    const hasta = $('#filtroHasta').value || hoyISO();
    return new URLSearchParams({ desde, hasta });
  }

  function money(v) {
    if (v == null) return '—';
    const n = Number(v);
    return isNaN(n) ? v : n.toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
  }

  async function api(path) {
    const q = params().toString();
    const sep = path.includes('?') ? '&' : '?';
    const res = await fetch(path + sep + q);
    if (!res.ok) throw new Error('Error ' + res.status);
    return res.json();
  }

  function setPeriodo(tipo) {
    const hasta = hoyISO();
    let desde;
    if (tipo === '0') desde = hasta;
    else if (tipo === 'mes') desde = inicioMes();
    else desde = haceDias(Number(tipo));
    $('#filtroDesde').value = desde;
    $('#filtroHasta').value = hasta;
  }

  let lastRows = [];
  let lastHeaders = [];
  let lastTitle = '';

  function showResult(title, headers, rows, kpis) {
    lastTitle = title;
    lastHeaders = headers;
    lastRows = rows;
    $('#tituloResultado').textContent = title;
    $('#panelResultado').classList.remove('d-none');

    const kpiBox = $('#kpisResultado');
    kpiBox.innerHTML = '';
    (kpis || []).forEach((k) => {
      const col = document.createElement('div');
      col.className = 'col-6 col-md-3';
      col.innerHTML = `<div class="card kpi-card h-100"><div class="card-body py-3">
        <div class="kpi-etiqueta">${k.label}</div>
        <div class="kpi-valor" style="font-size:1.35rem">${k.value}</div>
      </div></div>`;
      kpiBox.appendChild(col);
    });

    const thead = $('#tablaResultado thead');
    const tbody = $('#tablaResultado tbody');
    thead.innerHTML = '<tr>' + headers.map((h) => `<th>${h}</th>`).join('') + '</tr>';
    tbody.innerHTML = '';
    if (!rows.length) {
      $('#msgVacio').textContent = 'No hay datos para el periodo seleccionado.';
      return;
    }
    $('#msgVacio').textContent = '';
    rows.forEach((r) => {
      const tr = document.createElement('tr');
      tr.innerHTML = r.map((c) => `<td>${c == null ? '—' : c}</td>`).join('');
      tbody.appendChild(tr);
    });
    $('#panelResultado').scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  async function cargarVentas() {
    const [resumen, detalle, metodos, top] = await Promise.all([
      api('/api/reportes/ventas/resumen'),
      api('/api/reportes/ventas/detalle'),
      api('/api/reportes/ventas/metodo-pago'),
      api('/api/reportes/medicamentos/mas-vendidos?limite=10'),
    ]);
    const kpis = [
      { label: 'Ventas', value: resumen.cantidadVentas },
      { label: 'Total', value: money(resumen.total) },
      { label: 'Ticket prom.', value: money(resumen.ticketPromedio) },
      { label: 'Métodos', value: metodos.length },
    ];
    const headers = ['Nº', 'Fecha', 'Cliente', 'Empleado', 'Método', 'Total', 'Estado'];
    const rows = detalle.map((v) => [
      v.numeroVenta, (v.fecha || '').replace('T', ' ').slice(0, 16),
      v.cliente, v.empleado, v.metodoPago, money(v.total), v.estado,
    ]);
    // Añadir bloque top productos al final como filas extra en msg
    showResult('Ventas del periodo', headers, rows, kpis);
    if (top.length) {
      const extra = document.createElement('div');
      extra.className = 'mt-3';
      extra.innerHTML = '<h6 class="text-muted">Top medicamentos</h6><ul class="mb-0">' +
        top.map((t) => `<li>${t.nombre || '—'} — ${t.unidades} uds · ${money(t.monto)}</li>`).join('') +
        '</ul>';
      $('#panelResultado .card-body').appendChild(extra);
    }
  }

  async function cargarInventario() {
    const [stock, venc, val] = await Promise.all([
      api('/api/reportes/inventario/stock-bajo'),
      api('/api/reportes/inventario/vencimientos'),
      api('/api/reportes/inventario/valorizado'),
    ]);
    const totalValor = val.reduce((s, r) => s + Number(r.valorVenta || 0), 0);
    const kpis = [
      { label: 'Stock bajo/agotado', value: stock.length },
      { label: 'Vencimientos', value: venc.length },
      { label: 'Ítems valorizados', value: val.length },
      { label: 'Valor inventario', value: money(totalValor) },
    ];
    const headers = ['Tipo', 'Producto / Lote', 'Detalle', 'Cantidad', 'Estado'];
    const rows = [
      ...stock.map((s) => ['Stock', s.nombre, `Mín: ${s.stockMinimo}`, s.disponible, s.estado]),
      ...venc.map((v) => ['Vencimiento', v.medicamento, `Lote ${v.numeroLote} · ${v.fechaVencimiento}`, v.cantidad, v.estado]),
    ];
    showResult('Inventario y vencimientos', headers, rows, kpis);
  }

  async function cargarCompras() {
    const data = await api('/api/reportes/compras/por-proveedor');
    const total = data.reduce((s, r) => s + Number(r.total || 0), 0);
    showResult('Compras por proveedor',
      ['Proveedor', 'Nº compras', 'Total'],
      data.map((r) => [r.nombre, r.compras, money(r.total)]),
      [
        { label: 'Proveedores', value: data.length },
        { label: 'Total compras', value: money(total) },
      ]);
  }

  async function cargarPersonas() {
    const [cli, emp] = await Promise.all([
      api('/api/reportes/clientes/frecuentes?limite=15'),
      api('/api/reportes/empleados/mayores-ventas?limite=15'),
    ]);
    const headers = ['Tipo', 'Nombre', 'Operaciones', 'Monto'];
    const rows = [
      ...cli.map((c) => ['Cliente', c.nombre, c.compras, money(c.monto)]),
      ...emp.map((e) => ['Empleado', e.nombre, e.ventas, money(e.monto)]),
    ];
    showResult('Clientes frecuentes y empleados', headers, rows, [
      { label: 'Clientes top', value: cli.length },
      { label: 'Empleados', value: emp.length },
    ]);
  }

  async function cargarGanancias() {
    const g = await api('/api/reportes/ganancias');
    showResult('Ganancias del periodo',
      ['Concepto', 'Valor'],
      [
        ['Ingresos por ventas', money(g.ingresos)],
        ['Costo estimado', money(g.costoEstimado)],
        ['Ganancia', money(g.ganancia)],
      ],
      [
        { label: 'Ingresos', value: money(g.ingresos) },
        { label: 'Costo', value: money(g.costoEstimado) },
        { label: 'Ganancia', value: money(g.ganancia) },
      ]);
  }

  async function cargarFormulas() {
    const data = await api('/api/reportes/formulas');
    showResult('Fórmulas médicas registradas',
      ['Fecha', 'Cliente', 'Médico', 'Entidad', 'Duración (días)'],
      data.map((f) => [f.fechaExpedicion, f.cliente, f.medico, f.entidadSalud, f.duracionDias]),
      [{ label: 'Fórmulas', value: data.length }]);
  }

  const loaders = {
    ventas: cargarVentas,
    inventario: cargarInventario,
    compras: cargarCompras,
    personas: cargarPersonas,
    ganancias: cargarGanancias,
    formulas: cargarFormulas,
  };

  function exportCsv() {
    if (!lastRows.length) return;
    const lines = [lastHeaders.join(',')];
    lastRows.forEach((r) => {
      lines.push(r.map((c) => {
        const s = c == null ? '' : String(c);
        return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
      }).join(','));
    });
    const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = (lastTitle || 'reporte').replace(/\s+/g, '_').toLowerCase() + '.csv';
    a.click();
  }

  function init() {
    setPeriodo('mes');
    $$('#presetsPeriodo .pill-btn').forEach((btn) => {
      btn.addEventListener('click', () => {
        $$('#presetsPeriodo .pill-btn').forEach((b) => b.classList.remove('active'));
        btn.classList.add('active');
        setPeriodo(btn.dataset.dias);
      });
    });
    $('#btnAplicarFiltros').addEventListener('click', () => {
      // Si hay un reporte abierto, recargar el último
    });
    $$('.reporte-card').forEach((card) => {
      card.addEventListener('click', async () => {
        const tipo = card.dataset.reporte;
        const fn = loaders[tipo];
        if (!fn) return;
        try {
          // limpiar extras previos
          const extras = $$('#panelResultado .card-body > div.mt-3');
          extras.forEach((e) => e.remove());
          await fn();
        } catch (e) {
          alert('No se pudo cargar el reporte: ' + e.message);
        }
      });
    });
    $('#btnExportCsv').addEventListener('click', exportCsv);
    $('#btnImprimir').addEventListener('click', () => window.print());
    $('#btnCerrarResultado').addEventListener('click', () => {
      $('#panelResultado').classList.add('d-none');
    });

    // Deep-link por hash
    const hash = (location.hash || '').replace('#', '');
    if (hash && loaders[hash === 'clientes' ? 'personas' : hash]) {
      const key = hash === 'clientes' ? 'personas' : hash;
      setTimeout(() => loaders[key]().catch(() => {}), 200);
    }
  }

  document.addEventListener('DOMContentLoaded', init);
})();
