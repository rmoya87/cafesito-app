# Instrucciones para el agente IA (Cursor)

- **Idioma:** Responde siempre en **español**.
- **Antes de actuar:** Lee la documentación que impacte tu tarea. Entra por `docs/README.md` y ve a la parte que afecte a tus decisiones. Regla: `.cursor/rules/docs-before-code.mdc`.
- **Documentos clave:**  
  `docs/REGISTRO_DESARROLLO_E_INCIDENCIAS.md` (cambios e incidencias recientes, ramas),  
  `docs/RELEASE_DEPLOY_WORKFLOW.md` (workflow y despliegue),  
  `docs/MASTER_ARCHITECTURE_GOVERNANCE.md` (arquitectura y principios).
- **Ramas de entorno:** Solo se usan **main**, **beta**, **alpha**, **Interna**. Flujo habitual: push a `main`, luego `git push origin main:beta` si quieres actualizar beta.
- **Stack:** Android (Kotlin, Jetpack Compose, Hilt, Room), WebApp (React, TypeScript), lógica compartida en `shared/` (KMP). Paridad Android/WebApp en flujos y UI cuando aplique.
- **Contexto del proyecto:** Usa como “Snapshot del Proyecto” la combinación de `docs/MASTER_ARCHITECTURE_GOVERNANCE.md`, `docs/REGISTRO_DESARROLLO_E_INCIDENCIAS.md` y la documentación específica del área que toques (p. ej. Brew Lab en `docs/commit-notes/commit-20260304-05-elaboracion-brew-ui-colores-italiana.md`).
- **Cambios en docs:** No modifiques documentación sin indicar antes qué archivo y sección afectan y esperar confirmación.
