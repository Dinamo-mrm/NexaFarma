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
    return isNaN(n)
      ? v
      : n.toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
  }
  async function api(path, extraParams) {
    const q = new URLSearchParams(params());
    if (extraParams) {
      Object.entries(extraParams).forEach(([k, v]) => q.set(k, v));
    }
    const sep = path.includes('?') ? '&' : '?';
    const res = await fetch(path + sep + q.toString());
    if (!res.ok) throw new Error('Error ' + res.status + ' al cargar el reporte');
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
  let lastLoader = null;

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
    $('#msgVacio').textContent = rows.length + ' registro(s).';
    rows.forEach((r) => {
      const tr = document.createElement('tr');
      tr.innerHTML = r.map((c) => `<td>${c == null || c === '' ? '—' : c}</td>`).join('');
      tbody.appendChild(tr);
    });
    $('#panelResultado').scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  async function reporteVentasTipo(tipo, titulo) {
    const resumen = await api('/api/reportes/ventas/resumen', { tipo });
    // Ajustar filtros visibles al tipo
    if (resumen.desde) $('#filtroDesde').value = resumen.desde;
    if (resumen.hasta) $('#filtroHasta').value = resumen.hasta;
    const detalle = await api('/api/reportes/ventas/detalle');
    showResult(titulo,
      ['Nº', 'Fecha', 'Cliente', 'Empleado', 'Método', 'Total', 'Estado'],
      detalle.map((v) => [
        v.numeroVenta,
        (v.fecha || '').replace('T', ' ').slice(0, 16),
        v.cliente, v.empleado, v.metodoPago, money(v.total), v.estado,
      ]),
      [
        { label: 'Ventas', value: resumen.cantidadVentas },
        { label: 'Total', value: money(resumen.total) },
        { label: 'Ticket prom.', value: money(resumen.ticketPromedio) },
        { label: 'Periodo', value: resumen.tipoPeriodo || tipo },
      ]);
  }

  async function reporteVentasDetalle() {
    const [resumen, detalle] = await Promise.all([
      api('/api/reportes/ventas/resumen'),
      api('/api/reportes/ventas/detalle'),
    ]);
    showResult('Detalle de ventas',
      ['Nº', 'Fecha', 'Cliente', 'Empleado', 'Método', 'Total', 'Estado'],
      detalle.map((v) => [
        v.numeroVenta, (v.fecha || '').replace('T', ' ').slice(0, 16),
        v.cliente, v.empleado, v.metodoPago, money(v.total), v.estado,
      ]),
      [
        { label: 'Ventas', value: resumen.cantidadVentas },
        { label: 'Total', value: money(resumen.total) },
        { label: 'Ticket prom.', value: money(resumen.ticketPromedio) },
      ]);
  }

  async function reporteMetodoPago() {
    const data = await api('/api/reportes/ventas/metodo-pago');
    const total = data.reduce((s, r) => s + Number(r.total || 0), 0);
    showResult('Ventas por método de pago',
      ['Método', 'Cantidad', 'Total'],
      data.map((r) => [r.metodoPago, r.cantidad, money(r.total)]),
      [{ label: 'Métodos', value: data.length }, { label: 'Total', value: money(total) }]);
  }

  async function reporteCategoria() {
    const data = await api('/api/reportes/ventas/por-categoria');
    const total = data.reduce((s, r) => s + Number(r.monto || 0), 0);
    showResult('Ventas por categoría',
      ['Categoría', 'Unidades', 'Monto'],
      data.map((r) => [r.categoria, r.unidades, money(r.monto)]),
      [{ label: 'Categorías', value: data.length }, { label: 'Total', value: money(total) }]);
  }

  async function reporteRanking(path, titulo) {
    const data = await api(path, { limite: 15 });
    showResult(titulo,
      ['Medicamento', 'Unidades', 'Monto'],
      data.map((r) => [r.nombre, r.unidades, money(r.monto)]),
      [{ label: 'Ítems', value: data.length }]);
  }

  async function reporteStockBajo() {
    const data = await api('/api/reportes/inventario/stock-bajo');
    showResult('Productos con stock bajo',
      ['Medicamento', 'Disponible', 'Mínimo', 'Proveedor', 'Estado'],
      data.map((r) => [r.nombre, r.disponible, r.stockMinimo, r.proveedor, r.estado]),
      [{ label: 'Alertas', value: data.length }]);
  }

  async function reporteAgotados() {
    const data = await api('/api/reportes/inventario/agotados');
    showResult('Productos agotados',
      ['Medicamento', 'Disponible', 'Mínimo', 'Proveedor', 'Estado'],
      data.map((r) => [r.nombre, r.disponible, r.stockMinimo, r.proveedor, r.estado]),
      [{ label: 'Agotados', value: data.length }]);
  }

  async function reporteProximos() {
    const data = await api('/api/reportes/inventario/proximos-vencer', { dias: 30 });
    showResult('Medicamentos próximos a vencer (30 días)',
      ['Medicamento', 'Lote', 'Vencimiento', 'Cantidad', 'Proveedor'],
      data.map((r) => [r.medicamento, r.numeroLote, r.fechaVencimiento, r.cantidad, r.proveedor]),
      [{ label: 'Lotes', value: data.length }]);
  }

  async function reporteVencidos() {
    const data = await api('/api/reportes/inventario/vencidos');
    showResult('Medicamentos vencidos',
      ['Medicamento', 'Lote', 'Vencimiento', 'Cantidad', 'Proveedor'],
      data.map((r) => [r.medicamento, r.numeroLote, r.fechaVencimiento, r.cantidad, r.proveedor]),
      [{ label: 'Lotes vencidos', value: data.length }]);
  }

  async function reporteValorizado() {
    const data = await api('/api/reportes/inventario/valorizado');
    const valorVenta = data.reduce((s, r) => s + Number(r.valorVenta || 0), 0);
    const valorCosto = data.reduce((s, r) => s + Number(r.valorCosto || 0), 0);
    showResult('Inventario valorizado',
      ['Medicamento', 'Disp.', 'P. venta', 'P. compra', 'Valor venta', 'Valor costo', 'Proveedor', 'Categoría'],
      data.map((r) => [
        r.nombre, r.disponible, money(r.precioVenta), money(r.precioCompra),
        money(r.valorVenta), money(r.valorCosto), r.proveedor, r.categoria,
      ]),
      [
        { label: 'Ítems', value: data.length },
        { label: 'Valor venta', value: money(valorVenta) },
        { label: 'Valor costo', value: money(valorCosto) },
      ]);
  }

  async function reporteMovimientos() {
    const data = await api('/api/reportes/inventario/movimientos');
    showResult('Movimientos de inventario',
      ['Fecha', 'Tipo', 'Medicamento', 'Cant.', 'Ant.', 'Nueva', 'Motivo', 'Responsable'],
      data.map((r) => [
        (r.fecha || '').replace('T', ' ').slice(0, 16),
        r.tipo, r.medicamento, r.cantidad, r.existenciaAnterior, r.nuevaExistencia,
        r.motivo, r.responsable,
      ]),
      [{ label: 'Movimientos', value: data.length }]);
  }

  async function reporteComprasProveedor() {
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

  async function reporteClientes() {
    const data = await api('/api/reportes/clientes/frecuentes', { limite: 20 });
    showResult('Clientes frecuentes',
      ['Cliente', 'Compras', 'Monto'],
      data.map((r) => [r.nombre, r.compras, money(r.monto)]),
      [{ label: 'Clientes', value: data.length }]);
  }

  async function reporteEmpleados() {
    const data = await api('/api/reportes/empleados/mayores-ventas', { limite: 20 });
    showResult('Empleados con mayores ventas',
      ['Empleado', 'Ventas', 'Monto'],
      data.map((r) => [r.nombre, r.ventas, money(r.monto)]),
      [{ label: 'Empleados', value: data.length }]);
  }

  async function reporteGanancias() {
    const g = await api('/api/reportes/ganancias');
    showResult('Ganancias por periodo',
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

  async function reporteFormulas() {
    const data = await api('/api/reportes/formulas');
    showResult('Fórmulas médicas registradas',
      ['Fecha', 'Cliente', 'Médico', 'Entidad', 'Duración (días)'],
      data.map((f) => [f.fechaExpedicion, f.cliente, f.medico, f.entidadSalud, f.duracionDias]),
      [{ label: 'Fórmulas', value: data.length }]);
  }

  const loaders = {
    'ventas-diarias': () => reporteVentasTipo('DIARIO', 'Ventas diarias'),
    'ventas-semanales': () => reporteVentasTipo('SEMANAL', 'Ventas semanales'),
    'ventas-mensuales': () => reporteVentasTipo('MENSUAL', 'Ventas mensuales'),
    'ventas-detalle': reporteVentasDetalle,
    'ventas-metodo': reporteMetodoPago,
    'ventas-categoria': reporteCategoria,
    'mas-vendidos': () => reporteRanking('/api/reportes/medicamentos/mas-vendidos', 'Medicamentos más vendidos'),
    'menos-vendidos': () => reporteRanking('/api/reportes/medicamentos/menos-vendidos', 'Productos menos vendidos'),
    'stock-bajo': reporteStockBajo,
    'agotados': reporteAgotados,
    'proximos-vencer': reporteProximos,
    'vencidos': reporteVencidos,
    'valorizado': reporteValorizado,
    'movimientos': reporteMovimientos,
    'compras-proveedor': reporteComprasProveedor,
    'clientes': reporteClientes,
    'empleados': reporteEmpleados,
    'ganancias': reporteGanancias,
    'formulas': reporteFormulas,
  };

  function exportCsv() {
    if (!lastHeaders.length) return;
    const lines = [lastHeaders.join(';')];
    lastRows.forEach((r) => {
      lines.push(
        r
          .map((c) => {
            const s = c == null ? '' : String(c);
            return /[";\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
          })
          .join(';')
      );
    });
    // BOM para que Excel abra UTF-8 correctamente
    const blob = new Blob(['\ufeff' + lines.join('\n')], {
      type: 'text/csv;charset=utf-8;',
    });
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
      if (lastLoader) lastLoader().catch((e) => alert(e.message));
    });
    $$('.reporte-card').forEach((card) => {
      card.addEventListener('click', async () => {
        const tipo = card.dataset.reporte;
        const fn = loaders[tipo];
        if (!fn) return;
        lastLoader = fn;
        try {
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
  }

  document.addEventListener('DOMContentLoaded', init);
})();
