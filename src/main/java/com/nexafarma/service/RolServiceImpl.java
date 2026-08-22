package com.nexafarma.service;

import com.nexafarma.entity.Rol;
import com.nexafarma.entity.RolNombre;
import com.nexafarma.exception.ResourceNotFoundException;
import com.nexafarma.repository.RolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;

    public RolServiceImpl(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Rol obtenerPorNombre(RolNombre nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + nombre));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rol> listarTodos() {
        return rolRepository.findAll();
    }

    @Override
    public void inicializarRolesPorDefecto() {
        for (RolNombre nombre : RolNombre.values()) {
            rolRepository.findByNombre(nombre).orElseGet(() ->
                    rolRepository.save(Rol.builder()
                            .nombre(nombre)
                            .descripcion("Rol " + nombre.name().toLowerCase())
                            .build()));
        }
    }
}
