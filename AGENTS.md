# Instrucciones para el agente IA (Cursor)

- **Idioma:** Responde siempre en **español**.
- **Antes de actuar:** Lee la documentación que impacte tu tarea. **Índice único:** `docs/README.md` (§4 por uso: arquitectura, release, runbooks, iOS, servicios, Supabase, históricos). Regla: `.cursor/rules/docs-before-code.mdc`.
- **Documentos clave:**  
  `docs/REGISTRO_DESARROLLO_E_INCIDENCIAS.md` (cambios e incidencias, ramas),  
  `docs/RELEASE_DEPLOY_WORKFLOW.md` (workflow, deploy Android + Web),  
  `docs/MASTER_ARCHITECTURE_GOVERNANCE.md` (arquitectura, principios),  
  `docs/ANALITICAS.md` (analíticas GA4/GTM, eventos, checklists; guías en `docs/gtm/`),  
  `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md` (a11y, checklist UI; regla `accesibilidad-ui.mdc`),  
  `docs/ANDROID_Y_WEBAPP_SERVICIOS_CONECTADOS_LLAMADAS_Y_OPTIMIZACION.md` (servicios, cachés, sync, optimización actual).  
  Referencia histórica: `docs/OPTIMIZACIONES_RENDIMIENTO_HISTORICO.md`, `docs/KMP_AUDIT_STEP0.md`. Deploy web Ionos: `webApp/DEPLOY-IONOS.md`.
- **Ramas de entorno:** Solo **main**, **beta**, **alpha**, **Interna**. Flujo: push a `main`, luego `git push origin main:beta` si actualizas beta.
- **Stack:** Android (Kotlin, Compose, Hilt, Room), WebApp (React, TypeScript), `shared/` (KMP). Paridad Android/WebApp en flujos y UI.
- **Contexto del proyecto:** “Snapshot” = MASTER + REGISTRO + doc del área (Brew: commit-notes elaboración; rendimiento: ANDROID_Y_WEBAPP_SERVICIOS; shared: SHARED_BUSINESS_LOGIC, KMP_AUDIT_STEP0).
- **Cambios en docs:** No modifiques documentación sin indicar antes qué archivo y sección afectan y esperar confirmación.
