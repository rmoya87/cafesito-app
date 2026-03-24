# Futuros desarrollos — Integraciones Android (Cafesito)

**Estado:** vivo  
**Última actualización:** 2026-03-19  
**Ámbito:** Android (Kotlin, Jetpack Compose), WebApp y backend. Propuestas e ideas para integrar la app con el ecosistema Android y mejoras de negocio/usabilidad.

---

## 1. Objetivo

Este documento recoge **propuestas pendientes y nuevas ideas** para Android, WebApp y backend. Lo ya implementado (timer en primer plano, Quick Settings Tile, notificaciones con acciones, **unirse a lista por enlace** (WebApp + Android + Supabase), App Links, predictive actions, canales, edge-to-edge, predictive back) está documentado en **`docs/ANDROID_INTEGRACIONES_IMPLEMENTADAS.md`** y **`docs/OPCIONES_DE_LISTA_WEB_Y_ANDROID.md`** (listas).

---

## 2. Checklist unificada de estado

**Leyenda:** ✅ Implementado | 🟡 En progreso | ❌ Pendiente

| # | Desarrollo | Temática | Estado | Notas / Dónde ver |
|---|------------|----------|--------|-------------------|
| 1 | Timer en primer plano (Brew Lab) | Elaboración y diario | ✅ | `ANDROID_INTEGRACIONES_IMPLEMENTADAS.md` §2 |
| 2 | Notificaciones con acciones (FCM, Timeline, Registrar) | Elaboración y diario | ✅ | Doc implementadas §3; canales |
| 3 | Notificación "Registrar elaboración" con acciones (Sí/Más tarde) | Elaboración y diario | ✅ | Doc implementadas §3 |
| 4 | Quick Settings Tile "Cafesito" | Integraciones Android | ✅ | Doc implementadas §4; en algunos OEM puede no aparecer |
| 5 | App Links / Deep links | Integraciones Android | ✅ | Doc implementadas §5 |
| 6 | Predictive app actions | Integraciones Android | ✅ | Doc implementadas §6 |
| 7 | Canales de notificaciones | Integraciones Android | ✅ | Doc implementadas §7 |
| 8 | Edge-to-edge y Predictive back | Integraciones Android | ✅ | Doc implementadas §8 |
| 9 | Direct Share (Android + WebApp, sin ChooserTargetService) | Integraciones Android/Web | ✅ | Implementado en Android/WebApp con ranking RPC y persistencia `share_*`; ver checklist y `docs/ANALITICAS.md` §13 |
| 10 | Health Connect (cafeína) | Integraciones Android | ❌ | Según prioridad producto |
| 11 | Pantalla de bloqueo / Lock screen | Integraciones Android | 🟡 | Fase 2 completada; Fase 3 en progreso |
| 12 | Soporte tablets y plegables | Integraciones Android | ❌ | WindowSizeClass, dos paneles |
| 13 | Reanudar timer tras reinicio | Elaboración y diario | ❌ | WorkManager/alarma, "¿Continuar elaboración?" |
| 14 | Sincronización en segundo plano (WorkManager) | UX y rendimiento | ❌ | Reforzar SyncManager, offline-first |
| 15 | Caché de imágenes y reducción de datos | UX y rendimiento | ❌ | Coil, redes lentas |
| 16 | Accesibilidad (TalkBack, contraste) | UX y rendimiento | ❌ | contentDescription, foco; ver ACCESIBILIDAD_WEBAPP_ANDROID.md |
| 17 | Temas y Material You (dynamicColor) | UX y rendimiento | ❌ | Android 12+ |
| 18 | Onboarding y primer valor en <2 min | Negocio y usabilidad | ❌ | §3.4.1 |
| 19 | Recompensas ligeras y racha | Negocio y usabilidad | ❌ | §3.4.2 |
| 20 | Listas y compartir como crecimiento (unirse por enlace) | Negocio y usabilidad | ✅ | RPCs `user_lists_join_by_link.sql`; WebApp `JoinListView` + `AppContainer`; Android `listJoin/{listId}`, `ListJoinScreen`; ver §3.4.4 y doc implementadas §5 |
| 21 | Monetización no intrusiva (Pro, afiliación) | Negocio y usabilidad | ❌ | §3.4.5 |
| 22 | Estados vacíos y primeros pasos | Negocio y usabilidad | ❌ | §3.4.6 |
| 23 | Despensa: compartir consumo de café | Negocio y usabilidad | ❌ | §3.4.7 |
| 24 | Perfil privado/público | Negocio y usabilidad | ❌ | §3.4.8 |

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
  - **Estado actual:** Fase 2 completada (widget + fallback por tile/notificación) y Fase 3 en progreso.
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
  - [ ] Fase 3 completada (hardening + rollout + comparativa de métricas).

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

- **Reanudar timer tras reinicio:** Si el dispositivo se reinicia con el timer en curso, no se reanuda. Propuesta: WorkManager o alarma para notificación "¿Continuar elaboración?" con tiempo restante aproximado (limitaciones de exactitud).

---

### 3.3 UX y rendimiento

- **Sincronización en segundo plano (WorkManager):** Reforzar SyncManager; políticas periódicas, tras elaboración guardada; manejo de conflictos offline-first.

- **Caché de imágenes y reducción de datos:** Revisar Coil y políticas de caché; en redes lentas o ahorro de datos, reducir calidad o diferir carga (timeline, detalle).

- **Accesibilidad:** Revisión con TalkBack y contraste; `contentDescription` y orden de foco en Brew Lab, notificaciones, tile. Ver `ACCESIBILIDAD_WEBAPP_ANDROID.md`.

- **Temas y Material You:** `dynamicColor` (Android 12+) para paleta derivada del fondo de pantalla, manteniendo identidad de marca donde convenga.

---

### 3.4 Negocio y usabilidad (tareas concretas)

Propuestas con tareas funcionales y técnicas. Afectan a Android, WebApp y/o backend. Objetivo: retención D1, engagement y crecimiento.

#### 3.4.1 Onboarding y primer valor en <2 min

**Objetivo:** Guía corta al abrir la app la primera vez; primera acción útil en <2 min (elegir método, registrar primer café o seguir a 2 personas). Reducir abandono, subir retención D1.

**Tareas funcionales:** Mostrar onboarding solo a usuarios nuevos (flag `onboarding_completed`). Pasos cortos: "Elige tu primer método", "Registra tu primer café" o "Sigue a 2 personas". Permitir "Más tarde"; al completar una acción, marcar completado. Opcional: sugerir Ir a Elaboración / Diario / Explorar perfiles.

**Tareas técnicas:** Backend: campo `onboarding_completed_at` o `onboarding_skipped`; endpoint para actualizar. Android: composables de onboarding, navegación condicional desde MainActivity/AppNavigation. WebApp: ruta o modal según estado. Shared: eventos analytics "onboarding_started", "onboarding_step_X_completed", "onboarding_skipped".

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

## 4. Referencias

- **Integraciones ya implementadas:** `docs/ANDROID_INTEGRACIONES_IMPLEMENTADAS.md`
- **Documento Maestro:** `docs/MASTER_ARCHITECTURE_GOVERNANCE.md`
- **Registro de desarrollo:** `docs/REGISTRO_DESARROLLO_E_INCIDENCIAS.md`
- **Accesibilidad:** `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md`
- **Brew Lab y UI:** `docs/commit-notes/commit-20260304-05-elaboracion-brew-ui-colores-italiana.md`
