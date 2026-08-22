# Estándares de Código — NexaFarma

Estas convenciones ya están en uso en el código del repositorio (entidades, repositories,
services y controllers de Sprint 0 y del módulo de inventario). Este documento las deja
explícitas para que Dev1, Dev2 y Dev3 las sigan sin divergir.

## 1. Idioma
- **Todo en español**: nombres de clases, métodos, variables, mensajes de excepción y
  comentarios. Solo palabras reservadas de Java/Spring quedan en inglés.

## 2. Paquetes (arquitectura en capas)
```
com.nexafarma.entity        -> Entidades JPA (@Entity)
com.nexafarma.repository    -> Interfaces Spring Data JPA
com.nexafarma.service       -> Interfaz + Impl con la lógica de negocio
com.nexafarma.controller    -> @RestController, mapea /api/**
com.nexafarma.exception     -> Excepciones de negocio compartidas
com.nexafarma.config        -> Beans de configuración (seguridad, encoders, etc.)
com.nexafarma.dto           -> Records de entrada/salida cuando la entidad no debe exponerse tal cual
com.nexafarma.security      -> Spring Security (filtros, UserDetailsService, JWT/sesión)
```
Una clase nunca salta capas (un Controller no llama un Repository directamente).

## 3. Nombres
- Clases: `PascalCase` (`MedicamentoService`, `DetalleVenta`).
- Métodos y variables: `camelCase` en español (`obtenerPorId`, `cantidadDisponible`).
- Cada `XxxService` es una interfaz; su implementación es `XxxServiceImpl`.
- Repositorios: `XxxRepository extends JpaRepository<Xxx, Long>`.
- Controllers: `XxxController`, ruta base `/api/xxxs` en plural minúsculas.

## 4. Excepciones de negocio (paquete `exception`)
No crear excepciones nuevas si una de estas ya cubre el caso:

| Excepción | Cuándo usarla | HTTP |
|---|---|---|
| `ResourceNotFoundException` | El recurso solicitado no existe (buscar por id/documento/etc.) | 404 |
| `DuplicateResourceException` | Viola una restricción de unicidad (NIT, documento, código de barras) | 409 |
| `ResourceInUseException` | No se puede eliminar/desactivar porque otra entidad lo referencia | 409 |
| `ReglaNegocioException` | Cualquier otra regla de negocio (stock insuficiente, lote vencido, fórmula no vigente, cantidades inválidas) | 400 |

Todas llevan `@ResponseStatus`, así que no hace falta capturarlas manualmente en el
controller — el manejador global (`@ControllerAdvice`, pendiente en `exception`) las
traduce a la respuesta HTTP correcta.

## 5. Services
- Interfaz + implementación separadas siempre (facilita mockear en tests).
- `@Service` + `@Transactional` a nivel de clase; métodos de solo lectura llevan
  `@Transactional(readOnly = true)`.
- Inyección de dependencias por **constructor** (no `@Autowired` en campos).
- Validar existencia de relaciones (`findById(...).orElseThrow(...)`) antes de guardar.
- Los métodos `crear(...)` siempre fuerzan `id(null)` antes de guardar, para evitar
  updates accidentales si el cliente REST manda un id.
- Eliminar es *soft delete* (`activo = false`) en las entidades que tienen ese campo
  (Cliente, Empleado, Proveedor, Producto/Medicamento). No usar `deleteById` salvo que
  la entidad no tenga campo `activo`.

## 6. Controllers
- `@RestController` + `@RequestMapping("/api/xxxs")`.
- Verbos REST estándar: `POST` crear (201), `GET` obtener/listar (200), `PUT` actualizar
  (200), `DELETE` eliminar (204), `PATCH` para cambios de estado puntuales.
- `@Valid @RequestBody` en creación/actualización.
- Listados grandes (clientes, medicamentos, ventas, empleados) usan `Page`/`Pageable`,
  no `List`, para no tumbar el rendimiento con miles de registros.
- Nunca lógica de negocio en el controller: solo delega al service.

## 7. Base de datos
- Nombres de tabla y columna en `snake_case` (`nombre_completo`, `fecha_vencimiento`).
- Toda entidad con ciclo de vida relevante lleva `creado_en`/`actualizado_en` vía
  `@PrePersist`/`@PreUpdate` donde aplique.
- Cambios de esquema van por Flyway (`src/main/resources/db/migration`), nunca con
  `ddl-auto=update` en producción (el perfil `prod` usa `validate`).

## 8. Ramas y commits
- Rama principal: `main`.
- Commits descriptivos en español, en modo indicativo: `"Implementación de servicios,
  controladores y reglas de inventario"`, no `"fix"` o `"cambios"`.
- Cada desarrollador trabaja preferentemente sobre su propio módulo para minimizar
  conflictos de merge (Dev1: seguridad/usuarios, Dev2: inventario/farmacéutico,
  Dev3: transacciones/frontend/fórmulas).

## 9. Pruebas
- JUnit + Mockito para services (mockear repositories).
- MockMvc para controllers.
- Nombre de test: `debeHacerXCuandoY` (español, descriptivo del comportamiento).
