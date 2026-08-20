package com.nexafarma.service;

import com.nexafarma.entity.Categoria;
import com.nexafarma.exception.DuplicateResourceException;
import com.nexafarma.exception.ResourceInUseException;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.CategoriaRepository;
import com.nexafarma.repository.MedicamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final MedicamentoRepository medicamentoRepository;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository,
                                MedicamentoRepository medicamentoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.medicamentoRepository = medicamentoRepository;
    }

    @Override
    public Categoria crear(Categoria categoria) {
        categoriaRepository.findByNombreIgnoreCase(categoria.getNombre())
                .ifPresent(c -> {
                    throw new DuplicateResourceException(
                            "Ya existe una categoría con el nombre " + categoria.getNombre());
                });
        categoria.setId(null);
        return categoriaRepository.save(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    public Categoria obtenerPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> listarTodas() {
        return categoriaRepository.findAllByOrderByNombreAsc();
    }

    @Override
    public Categoria actualizar(Long id, Categoria datosActualizados) {
        Categoria existente = obtenerPorId(id);

        boolean nombreCambio = !existente.getNombre().equalsIgnoreCase(datosActualizados.getNombre());
        if (nombreCambio) {
            categoriaRepository.findByNombreIgnoreCase(datosActualizados.getNombre())
                    .ifPresent(c -> {
                        throw new DuplicateResourceException(
                                "Ya existe una categoría con el nombre " + datosActualizados.getNombre());
                    });
        }

        existente.setNombre(datosActualizados.getNombre());
        existente.setDescripcion(datosActualizados.getDescripcion());

        return categoriaRepository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        Categoria existente = obtenerPorId(id);

        // Categoria no tiene campo 'activo' (no hay soft delete en la entidad),
        // y Producto.categoria es @ManyToOne(optional = false): no se puede
        // borrar físicamente si hay medicamentos activos usándola.
        boolean enUso = !medicamentoRepository.findByCategoriaIdAndActivoTrue(id).isEmpty();
        if (enUso) {
            throw new ResourceInUseException(
                    "No se puede eliminar la categoría '" + existente.getNombre()
                            + "' porque tiene medicamentos activos asociados");
        }

        categoriaRepository.delete(existente);
    }
}