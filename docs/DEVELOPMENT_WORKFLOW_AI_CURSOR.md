# Flujo de desarrollo con IA (Cursor)

Estado: vigente  
Última actualización: 2026-03-04  
Objetivo: que el agente (IA) consulte siempre la documentación antes de codificar y no modifique docs sin avisar.

---

## 1. Cambios realizados (registro)

| Fecha       | Cambio |
|------------|--------|
| 2026-03-04 | Creada regla de Cursor `.cursor/rules/docs-before-code.mdc` con `alwaysApply: true`. |
| 2026-03-04 | Creado este documento como fuente de verdad del flujo y referencia en `MASTER_ARCHITECTURE_GOVERNANCE.md`. |
| 2026-03-04 | Revisión y homogeneización de docs: `docs/README.md` (índice y reglas documento vivo), `docs/runbooks/README.md`, cabeceras unificadas, MASTER 6.4.1 y 6.4.4 (evitar incompatibilidades). |
| 2026-03-04 | Consolidación: eliminado `play-console-cicd.md` (contenido integrado en `RELEASE_DEPLOY_WORKFLOW.md`); eliminado `WEBAPP_SPA_DEPLOY.md` (contenido integrado en `supabase/webhook-coffees-trigger-build.md`). |

---

## 2. Regla de Cursor que lo aplica

- **Archivo:** `.cursor/rules/docs-before-code.mdc`
- **Alcance:** `alwaysApply: true` (todas las conversaciones).
- **Resumen:** Consultar `docs/` y `webApp/docs/` antes de escribir código; si hace falta tocar documentación, avisar al usuario antes y esperar confirmación.

---

## 3. Flujos de acciones (obligatorios)

### 3.1 Flujo: antes de escribir o modificar código

```
1. El agente recibe una petición que implica escribir o modificar código.
2. OBLIGATORIO: Revisar documentación relevante en:
   - docs/ (raíz del proyecto)
   - webApp/docs/ (si el cambio afecta a la web app)
3. Usar esa documentación como fuente de verdad (arquitectura, convenciones, APIs, flujos).
4. Solo entonces proceder a implementar o proponer cambios de código.
```

**Qué consultar según el ámbito:** Ver índice completo en `docs/README.md`.

| Ámbito        | Documentación prioritaria |
|---------------|---------------------------|
| Arquitectura / capas / dominio | `docs/MASTER_ARCHITECTURE_GOVERNANCE.md` |
| Android / Kotlin / Compose    | `docs/` + regla `.cursor/rules/android-kotlin-compose.mdc` |
| WebApp                        | `docs/`, `webApp/docs/` |
| Supabase / backend            | `docs/supabase/`, secciones correspondientes en MASTER |
| Runbooks / operación          | `docs/README.md` (sección 4.3), `docs/runbooks/README.md`, `docs/RELEASE_DEPLOY_WORKFLOW.md` |

---

### 3.2 Flujo: cuando la implementación requiere cambiar documentación

```
1. El agente detecta que su implementación requiere cambiar o ampliar documentación existente
   (p. ej. MASTER_ARCHITECTURE_GOVERNANCE.md, guías en docs/, webApp/docs/).
2. NO modificar los archivos de documentación sin avisar.
3. OBLIGATORIO: Indicar al usuario, ANTES de tocar los docs:
   - Qué archivos de documentación habría que actualizar.
   - Por qué (resumen breve).
4. Esperar confirmación o indicación del usuario.
5. Solo si el usuario confirma (o pide explícitamente el cambio), aplicar los cambios en los docs.
```

**Ejemplo de aviso al usuario:**

> Este cambio afecta al flujo de [X] descrito en `docs/[Y].md`. ¿Quieres que actualice la sección [Z] o lo dejas para revisarlo tú?

---

## 4. Resumen para no repetir errores

- **No** escribir código sin haber revisado antes la documentación relevante en `docs/` y, si aplica, `webApp/docs/`.
- **No** modificar documentación (incluido este archivo y el MASTER) sin avisar antes al usuario y esperar confirmación.
- **Sí** indicar explícitamente cuando un cambio de código implica actualizar docs y proponer el cambio solo tras confirmación.

---

## 5. Referencias

- **Índice de documentación:** `docs/README.md` (punto de entrada; documento vivo y reglas para evitar incompatibilidades).
- Regla Cursor: `.cursor/rules/docs-before-code.mdc`
- Documento maestro: `docs/MASTER_ARCHITECTURE_GOVERNANCE.md` (sección 6.4 y 6.4.4).
