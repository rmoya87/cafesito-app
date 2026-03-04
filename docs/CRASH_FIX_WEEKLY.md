# Revisión y resolución de crashes (manual, desde Cursor)

**Estado:** vigente  
**Última actualización:** 2026-03-04  
**Runbook:** ver índice en `docs/runbooks/README.md`.

No hay automatización con Git ni con OpenAI. Tú das la orden desde aquí (Cursor), la IA revisa los crashes y aplica los cambios en tu repo local, y **tú subes a Git cuando decidas**.

## Flujo

1. **Tienes los crashes** (Play Console, Firebase Crashlytics, o un informe que te hayan pasado).

2. **Opción A – Archivo en el repo**  
   Pega o guarda el informe en **`.github/crash-reports/weekly.json`** con este formato (array de objetos):
   - **`title`**: descripción breve (ej. "NullPointerException en LoginScreen").
   - **`stackTrace`**: stack trace completo (se usa para localizar el archivo).
   - **`file`** (opcional): ruta del archivo en el repo, ej. `app/src/main/java/com/cafesito/app/ui/access/LoginScreen.kt`.
   - **`count`** (opcional): número de afectados.

   Hay un ejemplo en **`.github/crash-reports/weekly.json.example`**.

3. **Generar el informe local (opcional)**  
   Si quieres un resumen en markdown para trabajar desde él:
   ```bash
   node .github/scripts/crash-fix-analyze.mjs
   ```
   Se crea **`docs/crash-fixes/pending-review.md`** con la lista de crashes y los archivos asociados. No se llama a ninguna API.

4. **Dar la orden en Cursor**  
   Escribe algo como:
   - *"Revisa los crashes de `docs/crash-fixes/pending-review.md` y resuélvelos"*, o
   - *"Revisa `.github/crash-reports/weekly.json` y aplica los fixes necesarios"*.

   La IA leerá el informe y el código, propondrá y aplicará los cambios en tu workspace (archivos .kt, etc.). Todo queda **solo en local**.

5. **Revisar y subir a Git**  
   Revisas los cambios en tu IDE, pruebas si hace falta, y cuando estés conforme haces commit y push tú mismo. Luego puedes lanzar **Release & Deploy** para publicar la versión de resolución de incidencias.

## Resumen

| Qué | Dónde |
|-----|--------|
| Informe de crashes (tú lo rellenas) | `.github/crash-reports/weekly.json` |
| Script que genera el resumen (sin APIs) | `.github/scripts/crash-fix-analyze.mjs` |
| Informe listo para pedir revisión en Cursor | `docs/crash-fixes/pending-review.md` |
| Quién sube a Git | Tú, cuando quieras |

No hay workflow en GitHub que suba nada ni se usa OpenAI en el repo; todo el análisis y los fixes se hacen desde Cursor bajo tu control.
