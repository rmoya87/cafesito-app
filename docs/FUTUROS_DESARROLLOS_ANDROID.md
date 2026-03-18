# Futuros desarrollos — Integraciones Android (Cafesito)

**Estado:** vivo  
**Última actualización:** 2026-03-18  
**Ámbito:** Android (Kotlin, Jetpack Compose). Propuestas e ideas para integrar la app con el ecosistema Android.

---

## 1. Objetivo

Este documento recoge **solo propuestas pendientes y nuevas ideas** para Android. Lo ya implementado (timer en primer plano, Quick Settings Tile, notificaciones con acciones, App Links, predictive actions, canales, edge-to-edge, predictive back) está documentado en **`docs/ANDROID_INTEGRACIONES_IMPLEMENTADAS.md`**.

---

## 2. Integraciones ya implementadas (referencia)

**Leyenda:** ✅ Implementado — ver documento canónico para detalle.

| Integración | Estado | Dónde ver detalle |
|-------------|--------|-------------------|
| Notificaciones con acciones (FCM, Timeline, "Marcar leída", etc.) | ✅ | `ANDROID_INTEGRACIONES_IMPLEMENTADAS.md` §3 (notificación Registrar) y canales |
| Quick Settings Tile "Cafesito" | ✅ | `ANDROID_INTEGRACIONES_IMPLEMENTADAS.md` §4 |
| Timer en primer plano (Brew Lab) | ✅ | `ANDROID_INTEGRACIONES_IMPLEMENTADAS.md` §2 |
| App Links / Deep links | ✅ | `ANDROID_INTEGRACIONES_IMPLEMENTADAS.md` §5 |
| Predictive app actions | ✅ | `ANDROID_INTEGRACIONES_IMPLEMENTADAS.md` §6 |
| Canales de notificaciones | ✅ | `ANDROID_INTEGRACIONES_IMPLEMENTADAS.md` §7 |
| Edge-to-edge y Predictive back | ✅ | `ANDROID_INTEGRACIONES_IMPLEMENTADAS.md` §8 |

---

## 3. Propuestas pendientes (no implementadas)

### 3.1 ❌ Direct Share

**Qué hace:** En el diálogo "Compartir", el usuario puede ver destinos directos (contactos, listas de Cafesito) sin abrir la app. La app implementa un `ChooserTargetService` que devuelve esos destinos.

**En Cafesito (propuesta):** Al compartir desde otra app, mostrar listas de Cafesito o contactos como objetivos directos (p. ej. "Compartir en lista X").

**Estado en proyecto:** No hay `ChooserTargetService` ni Direct Share.

---

### 3.2 ❌ Bubbles

**Qué hace:** Notificación que se puede expandir en una burbuja flotante; el usuario ve el contenido (p. ej. timer) sin pantalla completa. Requiere notificación con burbuja y contenido `BubbleMetadata`.

**En Cafesito (propuesta):** Modo burbuja para el timer de elaboración: minimizar a burbuja flotante con tiempo restante; tap abre de nuevo Brew Lab.

**Estado en proyecto:** No hay notificaciones con burbuja ni `BubbleMetadata`.

---

### 3.3 ❌ Health Connect

**Qué hace:** API de Android para leer/escribir datos de salud (nutrición, actividad, sueño, etc.). Cafesito podría registrar consumo de cafeína con consentimiento del usuario.

**En Cafesito (propuesta):** Opción "Conectar con Health Connect" en Ajustes; al guardar una elaboración en el diario, escribir registro de cafeína (estimada) si el usuario ha vinculado. Permisos y flujos detallados en la versión anterior de este doc (ejemplos funcionales: activación, escritura al guardar, desconectar, requisitos técnicos).

**Estado en proyecto:** No hay integración con Health Connect.

---

## 4. Orden sugerido de implementación (pendientes)

1. ❌ **Burbuja** (opcional) — timer en segundo plano como burbuja flotante.
2. ❌ **Health Connect** — según prioridad de producto (cafeína en perfil de salud).
3. ❌ **Direct Share** — compartir a listas/contactos desde otras apps.

---

## 5. Estado actual en Android (tabla resumen)

Revisión sobre el código y la configuración del proyecto Android.  
**Implementado:** ver `ANDROID_INTEGRACIONES_IMPLEMENTADAS.md`.

| Integración | Estado | Notas |
|-------------|--------|--------|
| Notificaciones con acciones | ✅ | Ver doc implementadas. |
| Quick Settings Tile | ✅ | Tile "Cafesito"; en algunos OEM (p. ej. Xiaomi/HyperOS) puede no aparecer en la lista. |
| Timer en primer plano | ✅ | Ver doc implementadas. |
| App Links / Deep links | ✅ | Ver doc implementadas. |
| Predictive app actions | ✅ | Ver doc implementadas. |
| **Direct Share** | ❌ | No implementado. |
| **Bubbles** | ❌ | No implementado. |
| **Health Connect** | ❌ | No implementado. |
| Canales de notificaciones | ✅ | Ver doc implementadas. |
| Edge-to-edge | ✅ | Ver doc implementadas. |
| Predictive back | ✅ | Ver doc implementadas. |

---

## 6. Propuestas nuevas: funcionalidades nativas Android y mejoras para la app

A continuación se proponen **nuevas funcionalidades nativas de Android** y **mejoras generales** para la app Cafesito, ordenadas por ámbito.

### 6.1 Nativas Android (ecosistema)

- **Soporte para Picture-in-Picture (PiP):** Si en el futuro se añade reproducción de vídeo (p. ej. tutoriales de elaboración), solicitar modo PiP para que el usuario pueda seguir la receta mientras usa otras apps.
- **Credential Manager / Passkeys:** Cuando el backend lo soporte, ofrecer inicio de sesión con Passkey (Credential Manager API) además de email/contraseña, para mayor seguridad y menos fricción.
- **Pantalla de bloqueo / Lock screen:** Mostrar en la pantalla de bloqueo un widget o acción rápida "Elaborar" (según versión de Android y OEM).
- **Soporte para tablets y plegables:** Revisar layouts en pantallas grandes (dos paneles en diario/lista, navegación adaptativa) y `WindowSizeClass` si se quiere experiencia optimizada en tablets.

### 6.2 Mejoras de UX y rendimiento


- **Sincronización en segundo plano (WorkManager):** Ya existe SyncManager; reforzar políticas de sincronización (periodic, después de elaboración guardada) y manejo de conflictos para modo offline-first.
- **Caché de imágenes y reducción de datos:** Revisar Coil (ya en uso) y políticas de caché; en redes lentas o ahorro de datos, reducir calidad o diferir carga de imágenes en timeline/detalle.
- **Accesibilidad:** Revisión periódica con TalkBack y contraste; asegurar que todas las pantallas nuevas (Brew Lab, notificaciones, tile) tengan `contentDescription` y orden de foco lógico (ver `ACCESIBILIDAD_WEBAPP_ANDROID.md`).
- **Temas y Material You:** Aprovechar `dynamicColor` (Android 12+) para que la app use paleta derivada del fondo de pantalla del usuario, manteniendo la identidad de marca donde convenga.

### 6.3 Mejoras de elaboración y diario

- **Reanudar timer tras reinicio:** Si el dispositivo se reinicia con el timer en curso, no se puede reanudar automáticamente. Propuesta: `WorkManager` o alarma con límite de tiempo para mostrar notificación "¿Continuar elaboración?" con tiempo restante aproximado (con limitaciones de exactitud).
- **Segunda tile "Cafesito — Diario":** Segunda Quick Settings Tile que abra directamente la pestaña Diario (sin depender del estado del timer); implementación simple con otro `TileService`.
- **✅ Notificación "Registrar elaboración" con acciones:** Implementado. Botones "Sí, registrar" (abre Consumo) y "Más tarde" (cierra notificación) en la notificación post-timer. Ver `ANDROID_INTEGRACIONES_IMPLEMENTADAS.md` §3.

### 6.4 Priorización sugerida (propuestas nuevas)

| Prioridad | Propuesta | Esfuerzo estimado | Impacto |
|-----------|-----------|-------------------|---------|
| Alta | Recordatorio de agua (notificación) | Bajo | Engagement, hábitos |
| — | ~~Notificación Registrar con acciones~~ | ✅ Implementado | Ver doc implementadas §3 |
| Media | Segunda tile Diario | Bajo | Paridad con tile Brew Lab |
| Baja | Health Connect (cafeína) | Alto | Diferenciación, salud |
| Baja | Burbuja para timer | Medio | UX avanzada |
| Baja | Reanudar timer tras reinicio | Medio | Caso límite |

---

## 7. Referencias

- **Integraciones ya implementadas:** `docs/ANDROID_INTEGRACIONES_IMPLEMENTADAS.md`
- **Documento Maestro:** `docs/MASTER_ARCHITECTURE_GOVERNANCE.md`
- **Registro de desarrollo:** `docs/REGISTRO_DESARROLLO_E_INCIDENCIAS.md`
- **Accesibilidad:** `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md`
- **Brew Lab y UI:** `docs/commit-notes/commit-20260304-05-elaboracion-brew-ui-colores-italiana.md`
