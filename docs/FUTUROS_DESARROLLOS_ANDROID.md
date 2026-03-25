# Futuros desarrollos — Integraciones Android (Cafesito)

**Estado:** vivo  
**Última actualización:** 2026-03-24  
**Ámbito:** Android (Kotlin, Jetpack Compose), WebApp y backend. Propuestas e ideas para integrar la app con el ecosistema Android y mejoras de negocio/usabilidad.

---

## 1. Objetivo

Este documento recoge **propuestas pendientes y nuevas ideas** para Android, WebApp y backend. Lo ya implementado (timer en primer plano, **reanudar timer tras reinicio**, Quick Settings Tile, notificaciones con acciones, **unirse a lista por enlace** (WebApp + Android + Supabase), App Links, predictive actions, canales, edge-to-edge, predictive back) está documentado en **`docs/ANDROID_INTEGRACIONES_IMPLEMENTADAS.md`** y **`docs/OPCIONES_DE_LISTA_WEB_Y_ANDROID.md`** (listas).

---

## 2. Checklist unificada de estado

**Leyenda:** ✅ Implementado | 🟡 En progreso | ❌ Pendiente | 🚫 Rechazado

> Las nuevas propuestas funcionales/negocio pendientes se detallan en `§3.4.9` a `§3.4.12`, junto al resto de tareas por hacer.

| # | Desarrollo | Temática | Estado | Notas / Dónde ver |
|---|------------|----------|--------|-------------------|
| 10 | Health Connect (cafeína) | Integraciones Android | 🚫 | Rechazado por prioridad/encaje actual de producto |
| 12 | Soporte tablets y plegables | Integraciones Android | ❌ | WindowSizeClass, dos paneles |
| 14 | Sincronización en segundo plano (WorkManager) | UX y rendimiento | ❌ | Reforzar SyncManager, offline-first |
| 15 | Caché de imágenes y reducción de datos | UX y rendimiento | ❌ | Coil, redes lentas |
| 19 | Recompensas ligeras y racha | Negocio y usabilidad | ❌ | §3.4.2 |
| 21 | Monetización no intrusiva (Pro, afiliación) | Negocio y usabilidad | ❌ | §3.4.5 |
| 22 | Estados vacíos y primeros pasos | Negocio y usabilidad | ❌ | §3.4.6 |
| 23 | Despensa: compartir consumo de café | Negocio y usabilidad | ❌ | §3.4.7 |
| 24 | Perfil privado/público | Negocio y usabilidad | ❌ | §3.4.8 |
| 25 | Comparador de coste por taza | Negocio y usabilidad | ❌ | §3.4.9 |
| 26 | Sistema de sustitutos inteligentes | Negocio y usabilidad | ❌ | §3.4.10 |
| 27 | Modo compra asistida en tienda | Negocio y usabilidad | ❌ | §3.4.11 |
| 28 | Alertas de caducidad/frescura | Negocio y usabilidad | ❌ | §3.4.12 |
| 1 | Timer en primer plano (Brew Lab) | Elaboración y diario | ✅ | `ANDROID_INTEGRACIONES_IMPLEMENTADAS.md` §2 |
| 2 | Notificaciones con acciones (FCM, Timeline, Registrar) | Elaboración y diario | ✅ | Doc implementadas §3; canales |
| 3 | Notificación "Registrar elaboración" con acciones (Sí/Más tarde) | Elaboración y diario | ✅ | Doc implementadas §3 |
| 4 | Quick Settings Tile "Cafesito" | Integraciones Android | ✅ | Doc implementadas §4; en algunos OEM puede no aparecer |
| 5 | App Links / Deep links | Integraciones Android | ✅ | Doc implementadas §5 |
| 6 | Predictive app actions | Integraciones Android | ✅ | Doc implementadas §6 |
| 7 | Canales de notificaciones | Integraciones Android | ✅ | Doc implementadas §7 |
| 8 | Edge-to-edge y Predictive back | Integraciones Android | ✅ | Doc implementadas §8 |
| 9 | Direct Share (Android + WebApp, sin ChooserTargetService) | Integraciones Android/Web | ✅ | Implementado en Android/WebApp con ranking RPC y persistencia `share_*`; ver checklist y `docs/ANALITICAS.md` §13 |
| 11 | Pantalla de bloqueo / Lock screen | Integraciones Android | ✅ | Fases 0-3 completadas (widget + fallback + hardening OEM + rollout) |
| 13 | Reanudar timer tras reinicio | Elaboración y diario | ✅ | `ANDROID_INTEGRACIONES_IMPLEMENTADAS.md` §2 (boot receiver, QA §3.2); `ANALITICAS.md` §10.1; `REGISTRO` §22 |
| 16 | Accesibilidad (TalkBack, contraste) Android + WebApp | UX y rendimiento | ✅ | Fases 0-3 completadas; hardening y protocolo de regresión activo en `ACCESIBILIDAD_WEBAPP_ANDROID.md` §8 |
| 17 | Temas y Material You (dynamicColor) | UX y rendimiento | ✅ | Fases 0-3 completadas (toggle en Ajustes + fallback pre-Android 12 + unificación de estilos base) |
| 18 | Onboarding y primer valor en <2 min | Negocio y usabilidad | ✅ | §3.4.1 — Implementado Android + Web con estado único servidor y paridad cruzada |
| 20 | Listas y compartir como crecimiento (unirse por enlace) | Negocio y usabilidad | ✅ | RPCs `user_lists_join_by_link.sql`; WebApp `JoinListView` + `AppContainer`; Android `listJoin/{listId}`, `ListJoinScreen`; ver §3.4.4 y doc implementadas §5 |

---

## 3. Desarrollos por temática

### 3.1 Integraciones Android (ecosistema)

Funcionalidades nativas del ecosistema Android. Detalle de implementados en `ANDROID_INTEGRACIONES_IMPLEMENTADAS.md`.

#### Pendientes

- **Direct Share (sin `ChooserTargetService`, Android + WebApp):**
  - **Objetivo funcional:** en "Compartir", ofrecer destinos directos de Cafesito (listas y contactos frecuentes) para reducir pasos.
  - **Android (implementación recomendada):** usar **Sharing Shortcuts** (`ShortcutManager` / `ShortcutInfoCompat`) en lugar de `ChooserTargetService` (legacy). Publicar shortcuts dinámicos por usuario (top listas/contactos), abrir destino con deep link interno y fallback al share normal si el destino ya no existe.
  - **WebApp (equivalente funcional):** usar `navigator.share(...)` cuando esté disponible y, en paralelo, mostrar un panel propio de **destinos rápidos** (copiar enlace, compartir en lista interna, contactos frecuentes, WhatsApp/Telegram/X por URL). Si Web Share API no está disponible, fallback a copia de enlace + panel de destinos.
  - **Estado actual:** implementado en Android y WebApp (sin `ChooserTargetService`) con fallback y telemetría backend.

  **Checklist de ejecución (estado real):**
  - [x] Definido enfoque sin `ChooserTargetService` (Android + WebApp).
  - [x] Añadido servicio base de share en WebApp (`webApp/src/core/shareService.ts`) con estrategia `navigator.share` + fallback a portapapeles.
  - [x] Migrado el flujo de copiar enlace en WebApp para usar el servicio base (`AppContainer`).
  - [x] Creado contrato base de destinos de share en Android (`DirectShareTarget`, `DirectShareTargetType`).
  - [x] Creada capa inicial Android (`DirectShareRepository`) con sugerencias base desde listas del usuario.
  - [x] Publicación inicial de Sharing Shortcuts dinámicos en Android al compartir listas (`DirectShareShortcutPublisher` + `ListOptionsViewModel.shareList()`).
  - [x] Añadido panel/acciones visibles de destino rápido en WebApp (botones "Copiar enlace" + "Compartir").
  - [x] Expuesto contrato backend inicial para ranking real (`docs/supabase/direct_share_targets_rpc.sql`) y conexión base en Android/WebApp vía RPC `get_direct_share_targets`.
  - [x] Instrumentados eventos base de analítica `share_*` en Android (ListOptionsViewModel) y WebApp (AppContainer).
  - [x] Conectada persistencia backend de eventos `share_*` vía RPC `log_share_event` (Android: `SupabaseDataSource.logShareEvent`; WebApp: `supabaseApi.logShareEvent`).
  - [x] Ajuste fino de `target_id`/`metadata` en eventos `share_*` (Android + WebApp).
  - [x] Verificación operativa en entorno real de inserciones en `share_event_logs` (seguir checklist de `docs/ANALITICAS.md` §13.4).

  **Desglose por sprint (ejecutable):**

  - **Sprint 1 — Base técnica y contrato**
    - **Backend:**
      - Definir endpoint para destinos sugeridos de share (listas + contactos frecuentes) por usuario autenticado.
      - Definir payload común para destino de share (`id`, `type`, `label`, `icon`, `deeplink`, `rankScore`).
      - Añadir endpoint para registrar interacción de share directo (click y resultado).
    - **Android:**
      - Crear capa `DirectShareRepository` y modelo local para destinos sugeridos.
      - Implementar publicación de shortcuts dinámicos con `ShortcutInfoCompat` (top N).
      - Resolver deep link interno al destino seleccionado con fallback a flujo de compartir normal.
    - **WebApp:**
      - Crear servicio de share (`shareService`) con estrategia dual: `navigator.share` + panel propio.
      - Implementar panel de destinos rápidos con datos mock/estáticos para validar UI/flujo.
      - Implementar fallback universal: copiar enlace + feedback visible.
    - **Analítica:**
      - Definir eventos y naming: `share_opened`, `share_target_shown`, `share_target_clicked`, `share_completed`, `share_failed`.
      - Añadir parámetros estándar: `origin_screen`, `content_type`, `target_type`, `target_id`.
    - **Criterios de aceptación Sprint 1:**
      - Android publica shortcuts sin crashear en login/logout.
      - WebApp abre panel y ejecuta fallback en navegadores sin Web Share API.
      - Eventos base llegan en entorno de pruebas con estructura acordada.

  - **Sprint 2 — Integración real y ranking**
    - **Backend:**
      - Implementar ranking por recencia/frecuencia para listas/contactos.
      - Excluir destinos no válidos (lista eliminada, contacto bloqueado/no accesible).
      - Añadir invalidación/caché corta para respuestas de destinos.
    - **Android:**
      - Conectar shortcuts a datos reales del backend.
      - Actualizar shortcuts al cambiar contexto (crear/eliminar lista, cambios de uso, login/logout).
      - Manejar errores de destino inválido con fallback y mensaje no intrusivo.
    - **WebApp:**
      - Conectar panel de destinos rápidos a backend.
      - Añadir acciones rápidas por canal externo (WhatsApp/Telegram/X) mediante URL share.
      - Reutilizar mismo ranking visual que Android (orden por score).
    - **Analítica:**
      - Instrumentar embudo completo por plataforma.
      - Dashboard inicial: CTR de destinos directos vs share genérico.
    - **Criterios de aceptación Sprint 2:**
      - Top destinos coincide con ranking backend en Android y WebApp.
      - Si un destino desaparece, no rompe flujo (fallback correcto).
      - Se observa mejora de clic a destino directo frente a baseline.

  - **Sprint 3 — Pulido, accesibilidad y rollout**
    - **Backend:**
      - Ajustar feature flag por plataforma y porcentaje de rollout.
      - Añadir límites y protección anti-abuso en logging de eventos.
    - **Android:**
      - Mejorar iconografía/labels de shortcuts y revisión de accesibilidad (TalkBack, labels).
      - Optimizar frecuencia de refresco de shortcuts para no impactar batería/rendimiento.
    - **WebApp:**
      - Mejorar accesibilidad del panel (`aria-label`, foco, teclado, cierre con Escape).
      - Ajustar responsive móvil/escritorio y tiempos de interacción.
    - **Analítica:**
      - A/B test simple (panel directo ON/OFF) y reporte de impacto.
      - Métricas de calidad: ratio de error share, abandono de panel, tiempo a completar share.
    - **Criterios de aceptación Sprint 3:**
      - Checklist de accesibilidad aprobado en Android y WebApp.
      - Rollout controlado activo con feature flag.
      - Métricas estables y sin regresión crítica de share.

- **Pantalla de bloqueo / Lock screen (Widget/acción "Elaborar", OEM):**
  - **Objetivo funcional:** reducir fricción para iniciar una elaboración desde contexto de bloqueo, manteniendo seguridad y comportamiento consistente entre fabricantes.
  - **Principios de producto:**
    - No depender de una sola vía (la pantalla de bloqueo varía por OEM/launcher).
    - Priorizar rutas robustas y ya soportadas por Android estándar.
    - Mantener fallback automático (Quick Settings Tile y notificación).
  - **Alcance funcional esperado (final):**
    - Entrada rápida a Brew Lab desde acción visible en lock/home (según soporte del dispositivo).
    - Si hay timer en curso, restaurar estado al abrir.
    - Si la sesión no es válida, pasar por autenticación y volver al flujo de elaborar.
  - **Estado actual:** ✅ Implementado (Fases 0-3 completadas: widget + fallback por tile/notificación + hardening OEM + rollout).
  - **Avance Fase 3 aplicado:** gate por fabricante/versión + rollout porcentual determinístico por dispositivo para entrada desde widget (`LockEntryFeatureFlags` + `BuildConfig.LOCK_WIDGET_ROLLOUT_PERCENT`), con degradación analítica a `quick_tile` cuando no aplica.

  **Solución final por fases:**

  - **Fase 0 — Definición y guardrails (corta)**
    - **Funcional:** definir matriz de entrada rápida: notificación -> widget (si soportado) -> Quick Settings Tile.
    - **Técnico:** contrato único de navegación (`OPEN_BREWLAB` / `OPEN_BREWLAB_CONSUMO`), política de seguridad sin bypass, matriz QA mínima por OEM (Pixel/Xiaomi).
    - **Salida de fase:** especificación cerrada de estados y fallback por dispositivo.

  - **Fase 1 — MVP robusto (recomendada)**
    - **Funcional:** abrir Brew Lab desde acción de notificación en lockscreen con mínimo toque.
    - **Técnico:** `PendingIntent` estable, flags correctos (`NEW_TASK`, `CLEAR_TOP`), reutilización de navegación existente en `MainActivity` + `AppNavigation`.
    - **Analítica:** `lock_entry_opened`, `lock_entry_completed`, `lock_entry_failed` con `entry_type=notification_action`.
    - **Salida de fase:** cold/warm start correcto y sin regresiones en timer.

  - **Fase 2 — Widget "Elaborar" (lock/home según soporte)**
    - **Funcional:** widget de acceso rápido "Elaborar" con comportamiento uniforme cuando el launcher lo permita.
    - **Técnico:** AppWidget/Glance mínimo (CTA único), receiver dedicado, fallback automático a tile/notificación en OEM sin soporte lock widgets.
    - **Accesibilidad:** etiqueta clara, área táctil >= 48dp, contraste AA.
    - **Salida de fase:** validado en Pixel/Xiaomi; degradación controlada en OEM restrictivos.

  - **Fase 3 — Hardening OEM y rollout**
    - **Funcional:** estabilidad en fabricantes con restricciones agresivas.
    - **Técnico:** feature flag por fabricante/versión, QA con doze/app kill/reboot, manejo explícito de errores de intent.
    - **Analítica y producto:** comparación de conversión por `entry_type` (`notification_action`, `widget`, `quick_tile`) hasta guardado en diario.
    - **Salida de fase:** rollout progresivo activo y métricas estables.

  **Matriz QA mínima OEM (Fase 2):**
  - **Pixel (Android 14/15):**
    - [ ] Widget "Elaborar" visible en home.
    - [ ] Si hay soporte lock widgets en launcher/versión, entrada desde lockscreen validada.
    - [ ] Fallback por tile abre Brew Lab con `entry_type=quick_tile`.
  - **Xiaomi (HyperOS/MIUI):**
    - [ ] Widget "Elaborar" visible en home.
    - [ ] Validar restricciones OEM (autoinicio/ahorro energía) sin romper entrada rápida.
    - [ ] Fallback por tile/notificación mantiene navegación y analítica.

  **Criterio de cierre Fase 2:**
  - Marcar `Fase 2 implementada` cuando Pixel y Xiaomi estén en verde en la matriz QA anterior.

  **Checklist de ejecución (estado real):**
  - [x] Fase 0 cerrada (contrato de navegación y matriz OEM).
  - [x] Fase 1 implementada (notificación lock-entry + métricas base).
  - [x] Fase 2 implementada (widget y fallback por OEM).
  - [x] Fase 3 completada (hardening + rollout + comparativa de métricas).

- **Bubbles:** Notificación del timer expandible en burbuja flotante; tap abre Brew Lab. Requiere notificación con burbuja y `BubbleMetadata`. Estado: no implementado.

- **Health Connect:** Opción "Conectar con Health Connect" en Ajustes; al guardar elaboración en el diario, escribir registro de cafeína (estimada) con consentimiento. Permisos y flujos en documentación anterior. Estado: no hay integración.

- **Picture-in-Picture (PiP):** Si en el futuro se añade reproducción de vídeo (p. ej. tutoriales), solicitar modo PiP para seguir la receta en otra app.

- **Credential Manager / Passkeys:** Cuando el backend lo soporte, inicio de sesión con Passkey (Credential Manager API) además de email/contraseña.

- **Pantalla de bloqueo / Lock screen:** Widget o acción rápida "Elaborar" según versión de Android y OEM.

- **Soporte tablets y plegables:** Revisar layouts en pantallas grandes (dos paneles en diario/lista, navegación adaptativa) y `WindowSizeClass`.

---

### 3.2 Elaboración, diario y notificaciones

#### Pendientes

- **Recordatorio de agua (notificación):** Notificación recurrente o programable "¿Ya bebiste agua?" con acciones "Ya bebí" y "Recordar en 15 min" (AlarmManager o WorkManager, canales).

- **Segunda tile "Cafesito — Diario":** Segunda Quick Settings Tile que abra directamente la pestaña Diario; otro `TileService`.

#### Implementado — Reanudar timer tras reinicio (tarea 13)

- **Estado:** ✅ Implementado (mar 2026). Fuente técnica: `docs/ANDROID_INTEGRACIONES_IMPLEMENTADAS.md` §2; registro: `docs/REGISTRO_DESARROLLO_E_INCIDENCIAS.md` §22; embudo GA4: `docs/ANALITICAS.md` §10.1 (`lock_entry_*` con `entry_type` = `post_reboot_resume`).
- **Rollout:** sin feature flag en código; publicación en beta/producción según `RELEASE_DEPLOY_WORKFLOW.md`.
- **Mejora futura opcional:** `WorkManager` como refuerzo si en algún OEM el broadcast de arranque no llega; eventos dedicados `brew_resume_*` si se quiere granularidad sin filtrar `lock_entry_*`.

**Checklist QA manual (OEM y estrés):**

1. **Precondición:** iniciar elaboración con timer en curso (FGS activo), tiempo restante visible en notificación.
2. **Reinicio con tiempo restante:** reiniciar el dispositivo; tras arranque, debe aparecer **una sola** notificación «¿Continuar elaboración?» (comprobar que no hay duplicado por doble broadcast).
3. **Continuar:** pulsar Continuar o el cuerpo de la notificación → se abre Brew Lab; tiempo total restante coherente (no negativo); elaboración sigue o concluye como en flujo normal.
4. **Cancelar desde la notificación:** repetir 1–2, pulsar **Cancelar** → no debe quedar notificación ongoing de timer; al abrir Brew Lab no debe restaurarse una elaboración fantasma.
5. **Pausa antes del reinicio:** pausar el timer, reiniciar → el tiempo **no** debe avanzar mientras estuvo pausado (reconciliación respeta `KEY_PAUSED`).
6. **Tiempo agotado durante el apagado:** dejar poco tiempo y reiniciar (o simular dejando pasar el tiempo) → debe mostrarse «¿Registrar elaboración?» y abrir Consumo al aceptar; sin contador negativo.
7. **Pixel / stock:** repetir 2–4 con notificaciones concedidas.
8. **Xiaomi / HyperOS (u OEM agresivo):** repetir 2–4; revisar que la notificación no quede silenciada por políticas OEM (anotar hallazgos en registro si falla).
9. **Matar app con elaboración activa:** desde Ajustes forzar cierre; comprobar que el FGS sigue y que, tras reinicio opcional, el flujo es coherente con `KEY_RUNNING`.

**Checklist de ejecución por fases (cierre):**

- [x] Fase 0 — contrato y persistencia base.
- [x] Fase 1 — detección de reinicio y prompt con acciones.
- [x] Fase 2 — reanudación efectiva y edge-case a consumo.
- [x] Fase 3 — hardening OEM (deduplicación boot), documentación y checklist QA; rollout según flujo de release (sin flag en app).

---

### 3.3 UX y rendimiento

- **Sincronización en segundo plano (WorkManager):** Reforzar SyncManager; políticas periódicas, tras elaboración guardada; manejo de conflictos offline-first.

- **Caché de imágenes y reducción de datos:** Revisar Coil y políticas de caché; en redes lentas o ahorro de datos, reducir calidad o diferir carga (timeline, detalle).

- **Accesibilidad (TalkBack, contraste) — Plan funcional y técnico por fases (iniciado):**
  - **Objetivo funcional:** asegurar que cualquier flujo crítico (Brew Lab, Diario, Perfil, Listas, Notificaciones y atajos del sistema) sea usable con lector de pantalla, tenga objetivos táctiles correctos y mantenga contraste AA en día/noche.
  - **Objetivo técnico:** eliminar deuda de semántica a11y (`contentDescription`/foco), estandarizar validaciones de contraste y establecer regresión continua para evitar recaídas.
  - **Ámbito de ejecución:** Android + WebApp (paridad obligatoria en criterios y validación final).
  - **Fuente de verdad:** `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md` (criterios + checklist obligatorio).

  **Fase 0 — Baseline y alcance (arranque)**
  - **Funcional:** definir alcance de pantallas/flows críticos y criterios de salida por plataforma.
  - **Técnico:** inventario de componentes interactivos por pantalla (iconos, botones, tarjetas, sheets, widgets/tile/notificaciones).
  - **Evidencia de cierre:** matriz base por pantalla con estado (OK / gap) y severidad.

  **Fase 1 — TalkBack/lectores y foco**
  - **Funcional:** navegación completa sin bloqueo con lector de pantalla en flujos críticos.
  - **Técnico Android:** completar `contentDescription` en controles interactivos, ordenar foco y etiquetas de acciones en listas/cards/sheets.
  - **Técnico WebApp:** garantizar `aria-label`, foco visible y secuencia de tabulación consistente.
  - **Criterio de cierre:** happy path por flujo sin elementos “sin etiqueta” ni foco perdido.

  **Fase 2 — Contraste y tamaño de objetivo**
  - **Funcional:** legibilidad y accionabilidad consistentes en día/noche.
  - **Técnico:** verificar contraste AA (4.5:1 / 3:1) con tokens; asegurar 44px Web y 48dp Android en controles clave.
  - **Criterio de cierre:** checklist de contraste y área táctil en verde para pantallas críticas.

  **Fase 3 — Regresión, QA y hardening**
  - **Funcional:** estabilidad de accesibilidad en releases, sin regresiones al añadir features.
  - **Técnico:** añadir smoke de regresión a11y (manual + e2e donde aplique), checklist de PR y trazabilidad en docs.
  - **Criterio de cierre:** validación final Pixel/Xiaomi + WebApp y proceso recurrente de revisión incorporado.

  **Checklist de ejecución (estado real):**
  - [x] Fase 0 iniciada: documentación funcional+técnica por fases publicada.
  - [x] Fase 0 cerrada: matriz base completa con severidad por pantalla (Android + WebApp) en `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md` §8.6.
  - [x] Fase 1 iniciada: primer lote aplicado en Android + WebApp (perfil/actividad/listas).
  - [x] Fase 1 completada (Android + WebApp): etiquetas críticas de lector y navegación accesible en bloques de mayor severidad.
  - [x] Fase 2 completada (Android + WebApp): objetivos táctiles mínimos y contraste reforzado en bloques priorizados.
  - [x] Fase 3 completada (Android + WebApp): hardening y operación continua con protocolo de regresión para PR/release.

- **Temas y Material You (dynamicColor, Android + tokens WebApp):**
  - **Objetivo funcional:** habilitar colores dinámicos del sistema en Android 12+ sin perder control visual en dispositivos no compatibles, y reducir deuda de estilos repetidos en WebApp.
  - **Objetivo técnico Android:** aplicar `dynamicLightColorScheme`/`dynamicDarkColorScheme` con fallback al tema Cafesito en Android <= 11 y en dispositivos donde el usuario lo desactive.
  - **Objetivo técnico WebApp:** consolidar tokens de accesibilidad/consistencia para evitar “valores mágicos” repetidos en botones y objetivos táctiles.
  - **Estado actual:** ✅ implementado.

  **Solución por fases (ejecutada):**
  - **Fase 0 — Baseline y decisiones**
    - Definir alcance: `dynamicColor` solo Android 12+.
    - Mantener fallback al esquema Cafesito estático en SDK previos.
    - Cerrar criterio de control por usuario (toggle en Ajustes).
  - **Fase 1 — Implementación de tema dinámico**
    - `CafesitoTheme` soporta `dynamicColorEnabled` y selecciona esquema dinámico en Android 12+.
    - Mantiene `LightColorScheme`/`DarkColorScheme` como fallback de producto.
  - **Fase 2 — Exposición funcional al usuario**
    - Nuevo control “Material You” en Ajustes.
    - Persistencia en `SharedPreferences` (`dynamic_color_enabled`) y aplicación inmediata.
  - **Fase 3 — Unificación de estilos repetidos**
    - Refactor de filas repetidas en ajustes Android (`SettingsOptionRow` reutilizable).
    - Token WebApp `--tap-target-min` para estandarizar objetivos táctiles en botones.

  **Checklist de ejecución (estado real):**
  - [x] Fase 0 cerrada (alcance + fallback definidos).
  - [x] Fase 1 completada (`dynamicColor` en tema con guardas por SDK).
  - [x] Fase 2 completada (toggle de usuario + persistencia + actualización en runtime).
  - [x] Fase 3 completada (unificación de componente repetido Android + token común WebApp).

---

### 3.4 Negocio y usabilidad (tareas concretas)

Propuestas con tareas funcionales y técnicas. Afectan a Android, WebApp y/o backend. Objetivo: retención D1, engagement y crecimiento.

#### 3.4.1 Onboarding y primer valor en <2 min

- **Estado en checklist §2:** ❌ Pendiente de implementación.
- **Estado del plan:** Documento de trabajo cerrado (funcional + técnico + documentación + fases). La implementación sigue el orden de fases y los criterios de aceptación indicados.

**Objetivo funcional**

- Tras el **primer acceso exitoso** (cuenta nueva o primera sesión tras registro), el usuario recibe una **guía mínima** (máx. 2–3 pantallas o un único carrusel con pasos claros) que le lleva a **una primera acción de valor** en **menos de 2 minutos** desde que llega al home autenticado (medición: tiempo hasta completar la acción o taps hasta CTA principal).
- Acciones de valor aceptadas (cualquiera **una** cierra el onboarding con éxito):
  1. **Elaborar:** **primer arranque del temporizador** en Brew (paso en curso, duración total de fases > 0); si no aplica, **guardar** taza/agua en diario desde Brew. **o**
  2. **Diario:** **guardar** al menos una entrada en el diario (café u otra categoría admitida), **o**
  3. **Social:** **seguir** al menos **dos** usuarios sugeridos (o buscar y seguir a uno + CTA a segundo), **o**
  4. **Despensa (alternativa):** **añadir** un café a la despensa (si producto prioriza despensa antes que elaboración).
- Debe existir **“Más tarde” / “Omitir”** visible en todo momento (sin trampas); omitir marca onboarding como **saltado**, no como completado por valor.
- Tras completar una acción de valor o saltar: **no** volver a mostrar el flujo forzado (salvo reset explícito de cuenta de prueba o flag de desarrollo).

**Alcance en plataformas (obligatorio) y estado único por cuenta**

- **Android y WebApp:** el onboarding debe **implementarse en las dos** plataformas (misma lógica de negocio, misma taxonomía de pasos y mismos criterios de “valor completado” / “omitido”). No es aceptable dejar solo una plataforma con el flujo guiado.
- **Un solo estado por usuario (no por dispositivo ni por plataforma):** el estado (`pending` / `completed_value` / `skipped`) vive en **servidor** ligado al `user_id`. **No** usar flags independientes del tipo `onboarding_done_android` y `onboarding_done_web` como fuente de verdad; eso duplicaría el flujo y rompería la paridad.
- **Si el usuario completa u omite el onboarding en Android**, al entrar en la **WebApp** con la **misma cuenta** debe leerse el mismo estado remoto y **no** mostrarse el onboarding de nuevo.
- **Si completa u omite en WebApp**, en **Android** con la misma cuenta **no** debe mostrarse el flujo otra vez.
- **Sincronización en tiempo real:** tras cada transición de estado en una plataforma, la otra debe reflejarlo en el **siguiente arranque de sesión o refresco** (pull del perfil / suscripción Realtime opcional). Evitar depender de estado solo en `localStorage` o `SharedPreferences` sin reconciliar con servidor al abrir sesión.

**Alcance funcional final**

- **Quién lo ve:** solo usuarios que cumplan **todas**: cuenta autenticada, perfil mínimo ya resuelto si hoy es gate obligatorio (p. ej. “completar perfil” antes de home), y **en servidor** `onboarding_status = pending` (tras hidratar sesión).
- **Qué muestra:** mensaje breve de propósito de la app + 2–3 tarjetas o pasos con **un CTA cada uno** (Elaborar / Diario / Explorar y seguir). Opcional: indicador de progreso “1 de 3”.
- **Paridad:** mismos objetivos en **Android** y **WebApp**; textos y orden de pasos alineados; diferencias solo por patrones de navegación (sheet vs pantalla completa).
- **Accesibilidad:** lectores de pantalla, foco visible, contraste AA, objetivo táctil ≥48 dp (Android) / 44 px (Web); ver checklist en `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md`.

**Diseño técnico propuesto**

- **Fuente de verdad (servidor):** ampliar `profiles` (o tabla dedicada `user_onboarding`) con campos mínimos:
  - `onboarding_status`: enum `pending` | `completed_value` | `skipped` (o equivalente con timestamps).
  - `onboarding_completed_at` (nullable), `onboarding_skipped_at` (nullable).
  - Opcional: `onboarding_last_step` para reanudar si se cierra la app a mitad (solo si producto lo pide).
  - **Prohibido como única verdad:** columnas o claves por plataforma que permitan “hecho en web pero pendiente en app”. Si hiciera falta telemetría por canal, usar **analítica** (`platform` en eventos), no el estado de negocio.
- **Sincronización:** lectura tras login/refresco de sesión en **Android y Web**; escritura con **RPC o PATCH** autorizado (solo el propio `user_id`). Caché local (`localStorage` / `SharedPreferences`) solo como **espejo** del servidor para UX offline; al recuperar red, reconciliar y **nunca** mostrar onboarding si el servidor ya marcó `completed_value` o `skipped`.
- **Detección “acción de valor” completada** (para marcar `completed_value` sin mentir):
  - Enganchar en puntos ya existentes: guardado diario, `BrewLab` inicio de elaboración, follow API, alta despensa. Centralizar en un pequeño **“OnboardingCompletionChecker”** (Android: tras éxito en ViewModel/repo; Web: tras mutación exitosa en hook/API) que llame a `completeOnboardingIfNeeded()`.
- **Navegación:**
  - **Android:** tras `SessionState.Authenticated` y rutas iniciales, si `pending` → overlay, `NavHost` paralelo o ruta dedicada `onboarding` con `popBackStack` bloqueado hasta completar/saltar (definir en implementación; preferir ruta `onboarding` en grafo para deep links y tests).
  - **WebApp:** ruta `/onboarding` o modal a pantalla completa tras login; redirección a home al terminar.
- **Analítica (GA4 / GTM):** conviene eventos dedicados además de `screen_view`:
  - `onboarding_started` (params: `platform`, opcional `entry` = post_login),
  - `onboarding_step_viewed` (`step_id`: `brew` | `diary` | `social` | `pantry`),
  - `onboarding_cta_click` (`step_id`, `action`),
  - `onboarding_value_completed` (`value_type`: misma taxonomía),
  - `onboarding_skipped` (`from_step_id` opcional).
  Documentar en `docs/ANALITICAS.md` y mapear en contenedor GTM si aplica.
- **Feature flag (opcional Fase 3):** `onboarding_v1_enabled` remota o `BuildConfig`/env Web para apagar rápido si hay problema en producción.

**Documentación requerida (al implementar)**

- `docs/ANALITICAS.md`: eventos y parámetros; checklist DebugView.
- `docs/REGISTRO_DESARROLLO_E_INCIDENCIAS.md`: decisión de campos Supabase, fecha de despliegue SQL, incidencias OEM/Web.
- `docs/DOCUMENTO_FUNCIONAL_CAFESITO.md` o anexo corto: flujo “primer valor” como criterio de aceptación enlazado desde este doc.
- `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md`: revisión tras UI nueva (si hay cambios de criterio, ampliar allí).
- **Supabase:** script SQL en `docs/supabase/` (ALTER / RPC `set_onboarding_status`, políticas RLS) — sin datos sensibles nuevos.

**Fases de ejecución**

| Fase | Funcional | Técnico | Documentación | Salida |
|------|------------|---------|-----------------|--------|
| **0 — Contrato y modelo** | Congelar lista de acciones de valor, textos UX base, reglas “Más tarde” vs “completado”. | Esquema DB + RPC/patch; enums y migración; acuerdo de caché local vs servidor. | Este apartado §3.4.1 como referencia; ticket/issue con criterios medibles (<2 min, definición de “inicio elaboración”). | Contrato producto + SQL revisado listo para aplicar. |
| **1 — Estado y gates** | Tras login, usuario con `pending` **no** queda en home sin decisión: se redirige a onboarding o se muestra entry point único; **misma regla** en Web y Android según estado remoto. | Lectura de estado en Android/Web tras auth; splash de carga coherente; manejo offline: si no hay red, mostrar mensaje y reintentar o permitir “Más tarde” que encola escritura y **sincroniza** al volver red (sin divergir plataformas). Prueba cruzada: completar en un cliente y abrir el otro → **no** onboarding. | `ANALITICAS`: stub de eventos o sólo `screen_view` hasta Fase 2. | Flujo navegable en dev con flag/mock. |
| **2 — UI pasos + completado** | Pantallas o carrusel final; CTAs reales a Brew Lab, Diario, búsqueda/seguimientos, despensa. Completado automático al detectar acción de valor. | Implementación UI **Compose + Web**; una sola escritura de estado en servidor por transición; tests manuales de cada CTA; telemetría `onboarding_*` con `platform` solo informativo. | Actualizar `ANALITICAS.md` con tabla de eventos; entrada breve en REGISTRO. | Paridad Android/Web; QA interno en flujo feliz **y** escenario “solo una plataforma”. |
| **3 — Hardening, A11y y métricas** | Revisión copy; “no molestar” a usuarios existentes (migración: `NULL` → `skipped` o `completed_value` según ya tengan diary/brew histórico — **definir política**). | Feature flag; optimizar llamadas; evitar doble modal en refresh de token. | Checklist QA y **definición de éxito D1** en REGISTRO o doc de producto; revisión a11y según checklist. | Rollout beta; seguimiento en GA4 (embudo onboarding). |

**Criterios de aceptación (resumen)**

- [x] Usuario nuevo puede llegar desde login a **una** acción de valor en **<2 min** en condiciones normales (red estable), sin pasos obligatorios ocultos. *Diseño: una sola pantalla/overlay con CTAs directos a elaboración y diario (sin pasos ocultos); medición cronometrada = QA beta / smoke.*
- [x] “Más tarde” funciona y persiste `skipped` en servidor cuando hay conectividad. *Android: `UserRepository.skipOnboarding()` → Supabase; sin red: Snackbar y no navega. Web: `skipOnboarding()` en `supabaseApi.ts` + Snackbar de error.*
- [x] Usuario que ya completó o saltó **no** ve el flujo otra vez **en ninguna plataforma** (misma cuenta). *Gates por `onboarding_status` remoto + migración legacy en `onboarding_users_db.sql`.*
- [x] Completar u omitir en **Android** implica que en **WebApp** (misma cuenta) **no** aparece el onboarding, y **viceversa**. *Estado único en `users_db`; reconciliación Web `visibilitychange` + evento interno; Android `refreshActiveUserFromSupabase` en primer plano si `pending` + salida de ruta `onboarding` (§riesgos carrera).*
- [x] Android y WebApp con el **mismo** significado de estados (servidor) y eventos analíticos (`platform` solo como dimensión, no como estado de negocio duplicado). *Valores `pending` \| `completed_value` \| `skipped`; eventos en `docs/ANALITICAS.md` §14.*
- [x] Accesibilidad: sin bloqueos para TalkBack/lector; CTAs con nombre accesible. *Android: `OnboardingScreen` — región semántica, encabezado, botones con texto visible, `BackHandler` = omitir. Web: `OnboardingOverlay` — `role="dialog"`, `aria-modal`, `aria-labelledby` / `aria-describedby`, foco inicial, Escape → omitir; revisión checklist `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md` §3.3.*

**Checklist de ejecución (estado real)**

- [x] Fase 0 cerrada (contrato + SQL en `docs/supabase/onboarding_users_db.sql`; aplicar en Supabase en entorno real).
- [x] Fase 1 cerrada (gates: Android `startRoute` + login; WebApp overlay si `pending`).
- [x] Fase 2 cerrada (UI Compose + overlay Web; detección de valor en diario, despensa, timer Brew, ≥2 follows; eventos `onboarding_*` — ver `docs/ANALITICAS.md` §14).
- [x] Fase 3 cerrada (A11y: §3.3 en `ACCESIBILIDAD_WEBAPP_ANDROID.md`; flag `ONBOARDING_V1_ENABLED` / `VITE_ONBOARDING_V1_ENABLED`; evento `onboarding_started`; omitir con feedback sin red; refresco Web al volver a la pestaña si sigue `pending`).
- [x] **RLS Supabase:** política `UPDATE` propia (`docs/supabase/users_db_update_own_row.sql`) aplicada cuando corresponda.

**Riesgos y decisiones pendientes (explícitas)**

- **Usuarios legacy:** regla aplicada en `docs/supabase/onboarding_users_db.sql` (§2a actividad → `completed_value`, §2b resto → `skipped`). Si aplicaste antes la migración “todo skipped”, ejecuta `docs/supabase/onboarding_legacy_repair_skipped_to_completed.sql` una vez.
- **“Iniciar elaboración” (cerrado):** el **evento canónico** es el **primer arranque del temporizador** en el paso de elaboración en curso, con método elegido y **duración total de fases > 0** (compromiso real, no solo abrir Brew Lab). **Red de cobertura:** si no hay temporizador útil (p. ej. suma de fases = 0) o el usuario guarda sin haber pasado por ese disparador, **sigue contando** la primera **entrada de diario** persistida desde Brew (`TYPE_CUP` / `TYPE_WATER` vía `DiaryRepository`). Paridad Android/Web documentada en `docs/DOCUMENTO_FUNCIONAL_CAFESITO.md` §2.6 y `docs/ANALITICAS.md` §14; anclajes en código: `BrewLabViewModel.toggleTimer`, `AppContainer` (efecto `brewStep`/`brewRunning`).
- **Web sin PWA (cerrado):** el onboarding es el **mismo** en navegador que instalada como PWA: overlay si `pending` + flag (`AppContainer`). **Auth:** (1) **Google OAuth redirect** — `signInWithOAuth` con `redirectTo` = origen del sitio + `/` (`useAuthSession`); Supabase devuelve sesión con `?code=` (PKCE); tras `sessionEmail`, se normaliza la URL a `{raíz_app}/home` (`useAuthSession` + `getAppRootPath`). (2) **Google One Tap / GIS** — `signInWithIdToken`, sin redirección. (3) **Errores en hash** (`#error=`) — se leen, limpia hash y `AppContainer` lleva a home canónico si hay `authError`. **Deep links:** rutas conocidas se normalizan con `useRouteCanonicalSync`; enlaces a **lista de perfil** sin sesión guardan ruta en `sessionStorage` y se restauran tras login (`useRouteGuardSync` + efecto en `AppContainer`). **Supabase / hosting:** Site URL y Redirect URLs en panel; producción/Ionos: `webApp/DEPLOY-IONOS.md` (login y raíz del sitio).
- **Condición de carrera (cerrado):** en servidor **gana la última escritura válida** (`PATCH` onboarding). **Web:** con `pending`, al volver la pestaña al foco se llama `reloadInitialData` (`AppContainer` + `visibilitychange`); tras mutación local se dispara `cafesito-onboarding-updated` (`supabaseApi.ts`). **Android:** en cada `onStart` si el usuario local está en `pending`, `AppSessionCoordinator.onAppForeground` → `refreshActiveUserFromSupabase()`; si la fila ya no exige onboarding, `AppNavigation` sale de la ruta `onboarding` a `home` (`LaunchedEffect` sobre `activeUser`).

---

#### 3.4.2 Recompensas ligeras y racha

**Objetivo:** Racha de días con al menos un registro en el diario (badge o número visible). Opcional: logros al completar N elaboraciones con un método. Sin gamificación pesada.

**Tareas funcionales:** Calcular racha (días consecutivos con entrada en diario). Mostrar en cabecera del Diario ("X días seguidos"). Opcional: logros ("Primera V60", "10 con mismo método") y lista en perfil. No penalizar; opcional recordatorio si racha >0 y no ha registrado hoy.

**Tareas técnicas:** Backend: cálculo de racha desde `diary_entries` (cache por usuario); tabla opcional `user_achievements`. Android/WebApp: obtener racha vía API; UI en DiaryScreen / vista Diario. Shared: claves de logros y lógica de desbloqueo al guardar.

---

#### 3.4.3 Descubrimiento de cafés y tostadores

**Objetivo:** Sección "Descubrir" / "Recomendados" con cafés por gustos, tostadores u origen. Enlace con listas y búsqueda ("Añadir a lista", "Ver más de este tostador").

**Tareas funcionales:** Nueva sección/pestaña "Descubrir". Cafés sugeridos por historial (origen, tuesta, métodos), tendencias; opcional tostadores por ubicación. CTA "Añadir a lista" y "Ver más de este tostador". Sin resultados: mensaje y enlace a búsqueda.

**Tareas técnicas:** Backend: endpoint `GET /recommendations/coffees` (historial diario, listas públicas). Opcional: tabla tostador, filtro ubicación. Android/WebApp: ruta Descubrir, lista recomendados, navegación a listas y búsqueda. Shared: modelos recomendación.

---

#### 3.4.4 Listas y compartir como crecimiento

**Estado:** ✅ **Implementado** (mar 2026) — unirse por enlace con listas públicas o por invitación.

- **Backend:** `docs/supabase/user_lists_join_by_link.sql` — RPCs `get_list_info_for_join(p_list_id)` (nombre + `user_id` del dueño) y `join_list_by_link(p_list_id)` (inserta en `user_list_members` si la lista es pública o `privacy = 'invitation'`). **Ejecutar el script en Supabase** si aún no está aplicado.
- **WebApp:** Enlace compartido `…/profile/list/{listId}` (copiar enlace en opciones). Si el visitante no es miembro, pantalla **Unirse a la lista** (`JoinListView`); al unirse, navegación al perfil del dueño con la lista abierta. API: `fetchListInfoForJoin`, `joinListByLink` en `supabaseApi.ts`.
- **Android:** `MainActivity.parseListIdFromIntent()` acepta `/profile/list/{listId}` y `/lists/join/{listId}`. Si el usuario ya tiene acceso → `profile/{ownerId}/list/{listId}`; si no → ruta `listJoin/{listId}` (`ListJoinScreen`, `ListJoinViewModel`). Tras unirse, navegación a la lista.
- **Detalle:** `ANDROID_INTEGRACIONES_IMPLEMENTADAS.md` §5; `OPCIONES_DE_LISTA_WEB_Y_ANDROID.md` §2.4.

**Futuro opcional (no implementado):** notificación/email "X te ha invitado a la lista Y"; token/slug adicional (`list_invite_token`) para enlaces revocables; botón **Compartir** nativo además de copiar enlace.

---

#### 3.4.5 Monetización no intrusiva

**Objetivo:** Si aplica: suscripción "Pro" (estadísticas diario, exportar historial, soporte) o afiliación tostadores (enlace compra con tracking). Sin paywall en lo esencial.

**Tareas funcionales:** Definir "Pro" (estadísticas, export CSV/PDF, soporte). Pantalla Pro en Ajustes: beneficios, precio, compra. Afiliación: en ficha café/tostador, botón "Comprar" con URL afiliado.

**Tareas técnicas:** Backend: `user.subscription_tier` o `is_pro`; pasarela (Stripe, RevenueCat) y webhooks; tabla `affiliate_links`. Android/WebApp: Ajustes "Cafesito Pro" (WebView o navegador); gating pantallas Pro; botón "Comprar" en detalle café. Shared: feature flags "monetization_enabled", "pro_export_enabled".

---

#### 3.4.6 Estados vacíos y primeros pasos

**Objetivo:** Diario vacío, despensa vacía, sin elaboraciones: mensaje claro + un solo CTA. Evitar pantallas vacías sin guía.

**Tareas funcionales:** Diario vacío: "Aún no tienes entradas..." + "Añadir al diario" / "Ir a Elaboración". Despensa vacía: "Tu despensa está vacía..." + "Añadir a despensa". Brew Lab sin café: opcional "Elige un café" + CTA a selector. Un mensaje + un botón por estado.

**Tareas técnicas:** Android: EmptyState en DiaryScreen, Despensa, opcional BrewLabScreen (texto + Button con navegación). WebApp: mismo criterio; componente `EmptyState(message, ctaLabel, onCtaClick)`. Shared: strings vacíos; rutas CTA en ambas plataformas.

---

#### 3.4.7 Despensa: compartir consumo de café

**Objetivo:** Permitir al usuario compartir su consumo de café (desde despensa o diario) con otros: enlace o resumen que otro usuario pueda ver (ej. “He tomado X cafés esta semana”, “Mi despensa / mis elaboraciones recientes”). Aumentar uso social y viralidad sin exponer datos sensibles.

**Tareas funcionales:** Desde despensa o diario, opción “Compartir mi consumo” (o “Compartir resumen”). El usuario elige qué compartir: resumen semanal (número de cafés, métodos), lista de cafés en despensa (solo nombres/origen, sin cantidades si se desea), o enlace a perfil público si está habilitado. Generar enlace (app link o web) o tarjeta compartible (imagen/texto para copiar o enviar por otra app). Quien recibe el enlace ve una vista de solo lectura (resumen o lista) sin poder editar.

**Tareas técnicas:** Backend: endpoint para generar “share token” con alcance (resumen / despensa / perfil) y expiración opcional; `GET /share/consumo/{token}` que devuelve datos de solo lectura (agregados o lista según token). Android: en Despensa/Diario, botón “Compartir” que abre bottom sheet con opciones (resumen, despensa, enlace a perfil); `Intent.ACTION_SEND` con texto/enlace o `ShareCompat`. WebApp: misma acción; ruta pública ` /share/consumo/{token}` que renderiza vista solo lectura. Shared: modelos de “resumen compartible”; validar que el token no filtre datos privados (respetar después perfil privado/público).

---

#### 3.4.8 Perfil privado/público

**Objetivo:** Que el usuario pueda configurar su perfil como privado o público. Perfil público: otros pueden ver actividad, listas públicas o resumen de elaboraciones (según qué se exponga). Perfil privado: solo el usuario ve sus datos; búsqueda o enlaces no muestran actividad ni listas.

**Tareas funcionales:** En Ajustes o Perfil, opción “Visibilidad del perfil” (Privado / Público). Texto claro: “Público: otros pueden ver tu perfil y actividad”; “Privado: solo tú ves tu actividad”. Al cambiar, guardar preferencia y aplicarla de inmediato (listas, timeline, enlaces compartidos). Si el perfil pasa a privado, enlaces ya compartidos (p. ej. consumo, listas) pueden devolver “Perfil privado” o 404 según diseño.

**Tareas técnicas:** Backend: campo `profiles.visibility` (enum `private` | `public`); en endpoints que exponen perfil a terceros (listas compartidas, búsqueda, `GET /share/consumo/{token}`) comprobar `visibility` y devolver 403/404 o datos limitados si es privado. Android: pantalla Ajustes > Privacidad o dentro de Perfil; `Switch` o `RadioButton` Privado/Público; llamada a API para actualizar; opcional mensaje “Tu perfil ya es privado; los enlaces compartidos dejarán de mostrar tu actividad”. WebApp: mismo control en Ajustes o Perfil. Shared: constante o enum `ProfileVisibility`; analytics “profile_visibility_changed”. Considerar onboarding o tooltip la primera vez que se muestre la opción.

---

#### 3.4.9 Comparador de coste por taza

**Objetivo:** calcular coste real por preparación y sugerir alternativas similares más rentables sin perder perfil sensorial.

**Funcional**

- Mostrar en Brew/Diario el coste estimado por taza (y por 100 ml / por preparación).
- En Home/Explorar, bloque “alternativas más rentables” para cafés equivalentes por perfil.
- Filtro opcional “ahorro” en recomendaciones y en detalle de café.

**Técnico**

- Backend: almacenar precio por paquete (`price_amount`, `price_currency`, `package_grams`, `updated_at`) y normalizar unidad.
- Motor de cálculo: `cost_per_gram` + `coffee_grams` por preparación para derivar `cost_per_cup`.
- Android/WebApp: componente comparador reutilizable + fallback cuando falte precio.

**Documentación**

- `docs/DOCUMENTO_FUNCIONAL_CAFESITO.md`: definición de métricas de coste.
- `docs/ANALITICAS.md`: eventos `cost_viewed`, `cost_alternative_clicked`.
- SQL en `docs/supabase/` para columnas/tablas de pricing.

**Pruebas**

- Unit tests de fórmula (redondeo, monedas, datos incompletos).
- QA manual Android/Web: detalle, brew y sugerencias.
- Prueba de regresión: cambio de precio actualiza coste sin romper historial.

**Fases de implementación**

- Fase 0: modelo de datos y reglas de cálculo.
- Fase 1: mostrar coste por taza en detalle/brew.
- Fase 2: alternativas más rentables en recomendaciones.
- Fase 3: analítica, hardening y rollout progresivo.

---

#### 3.4.10 Sistema de sustitutos inteligentes

**Objetivo:** cuando falta un café en despensa, proponer sustitutos por similitud de sabor y compatibilidad con método.

**Funcional**

- CTA “Buscar sustituto” al detectar stock insuficiente o café agotado.
- Ranking de sustitutos por score (perfil sensorial + método + disponibilidad).
- Opción “Guardar como plan B” por método/preparación.

**Técnico**

- Backend/RPC: score ponderado por `aroma/sabor/cuerpo/acidez/dulzura`, `formato`, `cafeina`, método preferido.
- Excluir cafés no disponibles para el usuario (según despensa/listas visibles).
- Android/WebApp: ficha de sustituto con razón explicable (“similar en cuerpo y tueste”).

**Documentación**

- `docs/SHARED_BUSINESS_LOGIC.md`: reglas de sustitución y pesos.
- `docs/ANALITICAS.md`: `substitute_search`, `substitute_selected`.
- SQL/RPC en `docs/supabase/` con contrato de entrada/salida.

**Pruebas**

- Tests de ranking con fixtures de cafés.
- QA manual: caso “sin stock”, caso “stock parcial”, caso “sin sustitutos”.
- Prueba de explicabilidad: razones coherentes con los atributos.

**Fases de implementación**

- Fase 0: contrato de score y criterios de elegibilidad.
- Fase 1: endpoint/RPC + consumo en cliente.
- Fase 2: UI de sustitutos en Brew/Despensa.
- Fase 3: ajuste de pesos con analítica y feedback.

---

#### 3.4.11 Modo compra asistida en tienda

**Objetivo:** escanear código de barras y recibir al instante compatibilidad con el perfil del usuario.

**Funcional**

- Escaneo rápido desde Explorar/Brew con resultado “encaja / neutro / no recomendado”.
- Mostrar score de compatibilidad y motivo principal (perfil, método, cafeína, formato).
- CTA directa: “Añadir a despensa”, “Guardar para después” o “Ver alternativas”.

**Técnico**

- Reusar scanner actual + endpoint de evaluación (`barcode -> coffee + compatibility_score`).
- Fallback offline: cache local de últimos escaneos y evaluación aproximada.
- Android/WebApp: misma taxonomía de score para paridad.

**Documentación**

- `docs/DOCUMENTO_FUNCIONAL_CAFESITO.md`: flujo compra asistida.
- `docs/ANALITICAS.md`: `barcode_scan_started`, `barcode_scan_matched`, `compatibility_viewed`.
- `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md`: revisión de scanner y resultados.

**Pruebas**

- Tests de mapeo de barcode y score por escenarios.
- QA manual en Android y Web con códigos válidos/inválidos.
- Prueba de rendimiento: tiempo de respuesta objetivo en red móvil.

**Fases de implementación**

- Fase 0: contrato de compatibilidad y criterios UX.
- Fase 1: integración scanner + resolución café.
- Fase 2: cálculo/visualización de score + CTAs.
- Fase 3: fallback offline y optimización de latencia.

---

#### 3.4.12 Alertas de caducidad/frescura

**Objetivo:** ayudar al usuario a consumir café en su ventana óptima de frescura y reducir desperdicio.

**Funcional**

- Registrar fecha de apertura de bolsa por ítem de despensa.
- Alertas configurables (próximo a perder frescura / ventana óptima terminando).
- Priorización visual en despensa (“consumir primero”).

**Técnico**

- Backend: campos `opened_at`, `fresh_until_at`, `freshness_policy` por café/formato.
- Job de recordatorios (push/web) con deduplicación y silencio configurable.
- Android/WebApp: etiqueta de frescura y ordenación por urgencia.

**Documentación**

- SQL en `docs/supabase/` para nuevos campos y políticas.
- `docs/ANALITICAS.md`: `freshness_alert_sent`, `freshness_alert_opened`, `freshness_action_taken`.
- `docs/REGISTRO_DESARROLLO_E_INCIDENCIAS.md`: decisiones de ventana de frescura.

**Pruebas**

- Unit tests de cálculo de ventana de frescura.
- QA manual de recordatorios y estados visuales Android/Web.
- Test de regresión: no duplicar alertas ni notificar fuera de horario permitido.

**Fases de implementación**

- Fase 0: definición de política de frescura por formato.
- Fase 1: modelo de datos + captura de fecha de apertura.
- Fase 2: UI de estado y ordenación en despensa.
- Fase 3: notificaciones y ajustes de frecuencia.

---

## 4. Referencias

- **Integraciones ya implementadas:** `docs/ANDROID_INTEGRACIONES_IMPLEMENTADAS.md`
- **Documento Maestro:** `docs/MASTER_ARCHITECTURE_GOVERNANCE.md`
- **Registro de desarrollo:** `docs/REGISTRO_DESARROLLO_E_INCIDENCIAS.md`
- **Accesibilidad:** `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md`
- **Brew Lab y UI:** `docs/commit-notes/commit-20260304-05-elaboracion-brew-ui-colores-italiana.md`
