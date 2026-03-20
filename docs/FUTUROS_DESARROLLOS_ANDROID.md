# Futuros desarrollos — Integraciones Android (Cafesito)

**Estado:** vivo  
**Última actualización:** 2026-03-19  
**Ámbito:** Android (Kotlin, Jetpack Compose), WebApp y backend. Propuestas e ideas para integrar la app con el ecosistema Android y mejoras de negocio/usabilidad.

---

## 1. Objetivo

Este documento recoge **propuestas pendientes y nuevas ideas** para Android, WebApp y backend. Lo ya implementado (timer en primer plano, Quick Settings Tile, notificaciones con acciones, **unirse a lista por enlace** (WebApp + Android + Supabase), App Links, predictive actions, canales, edge-to-edge, predictive back) está documentado en **`docs/ANDROID_INTEGRACIONES_IMPLEMENTADAS.md`** y **`docs/OPCIONES_DE_LISTA_WEB_Y_ANDROID.md`** (listas).

---

## 2. Checklist unificada de estado

**Leyenda:** ✅ Implementado | ❌ Pendiente

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
| 9 | Direct Share (ChooserTargetService) | Integraciones Android | ❌ | Listas/contactos como destinos al compartir |
| 10 | Health Connect (cafeína) | Integraciones Android | ❌ | Según prioridad producto |
| 11 | Pantalla de bloqueo / Lock screen | Integraciones Android | ❌ | Widget/acción "Elaborar" (OEM) |
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

- **Direct Share:** En el diálogo "Compartir", destinos directos (listas de Cafesito, contactos) sin abrir la app. Implementar `ChooserTargetService`. Estado: no hay ChooserTargetService en el proyecto.

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
