# Documentación Cafesito — Índice y gobernanza

**Estado:** vivo  
**Última actualización:** 2026-03-05  
**Objetivo:** documentación como fuente de verdad, sin incompatibilidades y con menor tasa de errores.

---

## 1. Principio: documento vivo

- La documentación **no es estática**. Se actualiza cuando cambia la arquitectura, los flujos o los procesos.
- **Una sola fuente de verdad por tema:** evita duplicar el mismo contenido en varios sitios; enlaza al doc canónico.
- **Antes de cambiar código:** consulta los docs relevantes (`docs/`, `webApp/docs/`) y alinea la implementación.
- **Antes de cambiar documentación:** si trabajas con IA/agente, indica qué archivos tocarías y espera confirmación. Ver `DEVELOPMENT_WORKFLOW_AI_CURSOR.md`.

---

## 2. Evitar incompatibilidades y reducir errores

- **No contradigas** el Documento Maestro (`MASTER_ARCHITECTURE_GOVERNANCE.md`). Si algo queda obsoleto, actualiza el doc o añade una nota de deprecación con enlace al doc vigente.
- **Flujo de release:** la fuente de verdad para ramas y despliegue es `RELEASE_DEPLOY_WORKFLOW.md` (incluye configuración Play Console y secretos Android/Web).
- **Runbooks:** si resuelves un incidente y aplicas cambios permanentes, actualiza el runbook correspondiente o crea uno nuevo con fecha y causas raíz.
- **Cabecera en documentos nuevos:** incluye al menos `Estado`, `Última actualización` y, si aplica, `Ámbito` o `Propietario`, para saber si el doc está vigente.

---

## 3. Estructura actual de `/docs`

| Ruta | Contenido |
|------|------------|
| **Raíz** | Documento Maestro, flujos de release/deploy, planes y guías de desarrollo. |
| `docs/commit-notes/` | Notas de commit por despliegue (trazabilidad). |
| `docs/supabase/` | SQL (RLS, triggers, migrations), Edge Functions, runbooks de Supabase, webhooks. |
| `docs/runbooks/` | Índice de runbooks; los runbooks concretos pueden estar aquí o en `supabase/` (p. ej. notificaciones). |

**WebApp:** `webApp/docs/` — documentación específica de la web (p. ej. auditoría de accesibilidad).

---

## 4. Índice de documentos (por uso)

### 4.1 Arquitectura y gobernanza

| Documento | Descripción |
|-----------|-------------|
| `MASTER_ARCHITECTURE_GOVERNANCE.md` | Fuente de verdad: principios, estructura del monorepo, capas, diseño, seguridad, testing, documentación. |
| `DEVELOPMENT_WORKFLOW_AI_CURSOR.md` | Flujo para desarrollo con IA: consultar docs antes de código; avisar antes de modificar docs. |
| `MULTIPLATFORM_EXECUTION_PLAN.md` | Plan de ejecución multiplataforma (Android, iOS, Web); decisión web y auditoría. |
| `PLAN_OFFLINE_FIRST_Y_FOTOS_CAMARA.md` | Plan offline-first por pantalla, galería/cámara y permisos (Android). |

### 4.2 Release, deploy y CI/CD

| Documento | Descripción |
|-----------|-------------|
| `RELEASE_DEPLOY_WORKFLOW.md` | **Fuente de verdad:** cuándo se ejecuta el workflow, ramas (Interna/Alpha/Beta/Producción), jobs, secretos (Android/Play + Web Ionos), configuración Play Console, revisión de crashes. |
| `REGISTRO_DESARROLLO_E_INCIDENCIAS.md` | **Registro reciente:** cambios de desarrollo, incidencias resueltas (p. ej. deploy-web/TypeScript), política de ramas (main, beta, alpha, Interna) y flujo main → beta. Consultar en próximos desarrollos o incidencias. |
| `supabase/webhook-coffees-trigger-build.md` | Deploy web: prerender de cafés, SEO, requisitos servidor (SPA fallback), webhook Supabase → build (actualizar web sin Git). |

### 4.3 Runbooks y operación

| Documento | Descripción |
|-----------|-------------|
| `CRASH_FIX_WEEKLY.md` | Revisión y resolución de crashes (manual, desde Cursor); flujo y rutas. |
| `runbooks/README.md` | Índice de runbooks (crash, notificaciones, etc.). |
| `supabase/NOTIFICATIONS_RUNBOOK_2026-03-04.md` | Runbook notificaciones/push; causas raíz y cambios aplicados. |

### 4.4 Pasos y calidad (iOS / multiplataforma)

| Documento | Descripción |
|-----------|-------------|
| `STEP6_SWIFTUI.md` | Paso 6 — SwiftUI bridge de ejemplo (Search). |
| `STEP7_IOS_SETUP.md` | Paso 7 — Proyecto iOS + Shared vía SPM. |
| `STEP8_QUALITY.md` | Paso 8 — Batería de calidad (tests, criterios, riesgos). |

### 4.5 Supabase (SQL, Edge Functions, webhooks)

| Documento / carpeta | Descripción |
|---------------------|-------------|
| `supabase/*.sql` | Scripts RLS, triggers, migrations (notifications, push, coffees, diary, users, etc.). |
| `supabase/edge-functions/` | Edge Functions (trigger-coffees-build, process-pending-account-deletions, send-notification). |
| `supabase/webhook-coffees-trigger-build.md` | Deploy web: prerender, SEO, SPA fallback, webhook cafés → build (ver también 4.2). |

### 4.6 Trazabilidad de despliegues

| Ubicación | Descripción |
|-----------|-------------|
| `commit-notes/commit-*.md` | Notas por despliegue; enlazadas desde la tabla en `RELEASE_DEPLOY_WORKFLOW.md`. |

### 4.7 WebApp

| Documento | Descripción |
|-----------|-------------|
| `webApp/docs/ACCESSIBILITY_AUDIT.md` | Auditoría de accesibilidad (scope, validaciones, riesgos, regresión). |

---

## 5. Dónde poner cosas nuevas

- **Decisión arquitectónica relevante:** ADR en `docs/adr/` (crear carpeta si no existe) o sección en MASTER si es principio transversal.
- **Runbook de incidente:** `docs/runbooks/` o `docs/supabase/` si es solo backend/Supabase; añadir entrada en `runbooks/README.md`.
- **Cambio de flujo de release/deploy:** actualizar `RELEASE_DEPLOY_WORKFLOW.md` y, si aplica, la tabla de registro de despliegues y commit-notes.
- **Documentación solo WebApp:** `webApp/docs/`.

---

## 6. Referencias rápidas

- **Arquitectura y reglas:** `MASTER_ARCHITECTURE_GOVERNANCE.md`
- **Release y ramas:** `RELEASE_DEPLOY_WORKFLOW.md`
- **Desarrollo con IA:** `DEVELOPMENT_WORKFLOW_AI_CURSOR.md`
- **Este índice:** `docs/README.md`
