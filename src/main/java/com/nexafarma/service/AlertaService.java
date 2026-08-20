package com.nexafarma.service;

import com.nexafarma.entity.Inventario;
import com.nexafarma.entity.Lote;

import java.util.List;

public interface AlertaService {

    List<Inventario> obtenerAlertasStockBajo();

    List<Lote> obtenerAlertasProximosAVencer(Integer dias);
}