# Optimizaciones de rendimiento — Referencia histórica

**Estado:** histórico (referencia).  
**Última actualización:** 2026-03  
**Propósito:** Resumen unificado de las optimizaciones de rendimiento y limpieza aplicadas en sesiones pasadas. Para **estrategia actual** de servicios, cachés y optimización ver **`docs/ANDROID_Y_WEBAPP_SERVICIOS_CONECTADOS_LLAMADAS_Y_OPTIMIZACION.md`**.

---

## 1. Resumen ejecutivo

Se aplicaron optimizaciones de red (server-side filtering, paralelización, debounce), limpieza de código (dead code, Kotlin 2.0 / Compose) y mejoras de UI (keys en listas, @Immutable, prefetch de imágenes). Resultado: reducción muy significativa de payload en pantallas críticas y build moderno.

**Referencias de commit:** entre otros, `97236d1` (perf: 10 optimizaciones críticas).

---

## 2. Optimizaciones de red y datos

| Ámbito | Antes | Después | Impacto |
|--------|--------|---------|---------|
| **Detalle de café** | Descarga de todas las reviews y favoritos (>1MB) | `getReviewsByCoffeeId`, `getFavoritesByUserId` en Supabase | Payload ~15KB, carga instantánea |
| **Listado / PagingSource** | Cada página con todas las reseñas y favoritos | Objetos Coffee simples; enriquecimiento en UI | ~95% menos datos por página |
| **Búsqueda** | 1000 cafés + filtro en Kotlin | Filtros en PostgreSQL (índices GIN) | Búsquedas <100ms, ~80% menos ancho de banda |
| **Carga detalle** | 7 peticiones secuenciales (~1,4s) | `async/await` en paralelo | ~300ms total |
| **Búsqueda por teclado** | Una petición por letra | `debounce(300)` + `distinctUntilChanged()` | ~85% menos peticiones |
| **Usuarios** | 1000 usuarios por pantalla | Caché en RAM con TTL 5 min | ~90% menos peticiones |
| **Sync tras login** | Secuencial (~4s) | `awaitAll()` de coroutines | ~1s |

**Archivos clave:** `SupabaseDataSource.kt`, `CoffeeRepository.kt`, `SearchViewModel.kt`.

---

## 3. Build y limpieza de código

- **Kotlin 2.0 / Compose:** Eliminados `composeOptions` y `kotlinCompilerExtensionVersion` obsoletos; `kotlinOptions` migrado a `tasks.withType<KotlinCompile>`. Build sin deprecaciones críticas.
- **Dead code eliminado:** `ui/home/` (reemplazado por Timeline), `ui/favorites/` (integrado en Profile), `domain/MockData.kt`, `ui/detail/MockData.kt`, `data/DataSeeder.kt`, `ui/profile/FollowItem.kt` (roto, sustituido por `FollowItemModern`).
- **Warnings:** Migración de APIs deprecadas (`Locale`, `CustomTabs`, `WindowInsets`), `@OptIn` para APIs experimentales, supresión controlada en componentes legacy.

---

## 4. UI y Compose

- **@Immutable** en `Coffee` y `CoffeeWithDetails`: Smart Recomposition, menos re-renders en scroll.
- **Keys en LazyColumn/LazyRow:** `key = { it.coffee.id }` / `key = { it.id }` para reutilización de composiciones.
- **Prefetch de imágenes:** Pre-carga de las primeras 3 del carrusel de recomendaciones (Coil).
- **URLs de imágenes:** Parámetros de tamaño en Supabase (`?width=400&height=300&resize=contain`) para reducir datos (~99% en imágenes).
- **Control de sincronización:** `LaunchedEffect(sessionState.userId)` en lugar de cualquier cambio de sesión.

---

## 5. SQL en Supabase (referencia)

Índices y RPC aplicados en su momento (ver `SUPABASE_OPTIMIZATIONS.sql` si existe en el repo):

- Índices GIN para búsqueda por nombre/marca.
- Índices B-tree para filtros (pais_origen, tueste).
- Función RPC `get_coffee_recommendations()`.

---

## 6. Métricas resumidas (históricas)

| Métrica | Antes | Después | Mejora |
|---------|-------|---------|--------|
| Carga inicial | ~2,3s | ~0,4s | ~82% |
| Login/sync | ~4,5s | ~1,1s | ~75% |
| Datos transferidos (pantalla crítica) | ~850KB | ~15–120KB | ~86–98% |
| Búsqueda | ~1,2s | ~0,08s | ~93% |
| APK (donde se aplicó R8) | ~45MB | ~18MB | ~60% |

---

**Para decisiones actuales de optimización:** usar **`docs/ANDROID_Y_WEBAPP_SERVICIOS_CONECTADOS_LLAMADAS_Y_OPTIMIZACION.md`** (checklists, cachés, SyncManager, Realtime, WebApp).
