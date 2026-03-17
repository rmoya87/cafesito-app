# Prompt para IA: desarrollo de la app iOS de Cafesito

**Propósito:** Prompt e instrucciones para que una IA (Cursor, Claude, ChatGPT) implemente la app iOS de Cafesito con **paridad funcional y de diseño** respecto a Android. La app Android usa Kotlin + Compose y consume el módulo **shared** (KMP); en iOS se usa SwiftUI, el mismo XCFramework de `shared/` y, donde aplique, **Liquid Glass** (iOS 26) para una experiencia nativa.

**Estado:** vivo  
**Última actualización:** 2026-03  
**Ámbito:** Desarrollo de la app iOS (SwiftUI) a partir del monorepo Cafesito.

---

## 1. Antes de usar el prompt

### 1.1 Acceso al repositorio

La IA debe tener acceso al monorepo completo (por ejemplo abriendo el proyecto en Cursor):

- `app/` — Android (Kotlin, Compose); referencia de rutas y pantallas.
- `shared/` — Kotlin Multiplatform (dominio, casos de uso, datos); se consume como XCFramework en iOS.
- `iosApp/` — Proyecto iOS (actualmente ejemplo Search); aquí se implementa la app completa.
- `docs/` — **Índice único:** `docs/README.md` (§4 por uso). Toda la documentación de arquitectura, flujos, diseño, analíticas, release.

### 1.2 Decisiones fijadas (obligatorias para el prompt)

| Tema | Decisión |
|------|----------|
| **Versión mínima de iOS** | **iOS 18** como mínimo. Para Liquid Glass usar `@available(iOS 26)` en tab bar, nav bar y controles que apliquen el efecto glass; en versiones anteriores mantener la misma estructura con estilos estándar de SwiftUI. |
| **Generación del XCFramework** | **Recomendado:** generar con `./gradlew :shared:assembleSharedReleaseXCFramework` y copiar el resultado a `iosApp/Shared.xcframework` (ver `docs/STEP7_IOS_SETUP.md`). Es la opción estándar para KMP en este monorepo; la IA debe asumir esta configuración. |
| **Autenticación** | Implementar **Google Sign-In** y **Sign in with Apple**. Ambos deben integrarse con Supabase Auth (mismo flujo que Android para Google). Para Apple es necesario configurar en **Supabase** el proveedor OAuth con **Client ID(s)** y **Secret Key** de Apple; y en el proyecto iOS las capabilities y configuración (Info.plist, etc.). La IA debe implementar ambos flujos y documentar dónde el desarrollador debe poner los Client IDs y Secret Key (Supabase dashboard + proyecto Xcode); no hardcodear credenciales. |
| **Analíticas iOS** | Usar **Firebase Analytics** en iOS con los mismos `screen_name` y eventos que Android (paridad). Al implementar la app iOS, **actualizar** `docs/ANALITICAS.md` añadiendo la sección correspondiente a iOS (rutas/pantallas, eventos, archivos donde se envía). |

### 1.3 Lo que la IA no debe hacer

- **No duplicar lógica de negocio en Swift.** Toda regla de dominio (diario, brew, reseñas, recomendaciones, validaciones) vive en `shared/` y se consume vía XCFramework y wrappers (patrón de `docs/STEP6_SWIFTUI.md`).
- **No inventar colores ni componentes.** Debe usar los tokens de `docs/DESIGN_TOKENS.md` y el design system descrito en `docs/MASTER_ARCHITECTURE_GOVERNANCE.md` (Fase 4).

---

## 2. Prompt completo para la IA

Copia y pega el siguiente bloque (o adapta las primeras líneas según tu repositorio) cuando pidas a la IA que desarrolle la app iOS.

---

**INICIO DEL PROMPT**

Eres un desarrollador iOS senior. Tu tarea es implementar la **app iOS nativa de Cafesito** en SwiftUI, con **paridad funcional y de diseño** respecto a la app Android ya existente en este repositorio. La app Android está hecha en Kotlin con Jetpack Compose y consume un módulo **shared** (Kotlin Multiplatform) que contiene la lógica de negocio, casos de uso y contratos de datos. En iOS debes:

1. **Reutilizar siempre el módulo `shared`** como XCFramework (SPM). Generar el framework con `./gradlew :shared:assembleSharedReleaseXCFramework` y copiarlo a `iosApp/Shared.xcframework` según `docs/STEP7_IOS_SETUP.md` y `docs/STEP6_SWIFTUI.md`. **No reimplementes en Swift ninguna regla de negocio** (búsqueda, elaboración/Brew, diario, reseñas, recomendaciones, validaciones); toda la lógica de dominio vive en `shared/` y se consume vía wrappers. Usa wrappers `ObservableObject` que adapten los ViewModels/StateFlow de KMP a SwiftUI (patrón del ejemplo Search en STEP6).

2. **Implementar todas las pantallas y flujos** que tiene Android, respetando la misma estructura de navegación y los mismos criterios de aceptación. Las fuentes de verdad son:
   - **Flujos y pantallas:** `docs/DOCUMENTO_FUNCIONAL_CAFESITO.md` (Despensa, Diario, Elaboración/BrewLab, Auth, Perfil, Explorar, etc.).
   - **Rutas Android (referencia):** En `app/src/main/java/com/cafesito/app/navigation/AppNavigation.kt` están definidas todas las rutas: `login`, `completeProfile`, `home`, `notifications`, `searchUsers`, `search`, `brewlab`, `brewlab_select_coffee`, `diary` (con `navigateTo`), `cafesProbados`, `addStock`, `editCustomCoffee`, `editNormalStock`, `addDiaryEntry`, `addPantryItem`, `profile/{userId}`, `profile/.../favorites`, `profile/.../list/{listId}`, `profile/.../list/.../options`, `profile/.../followers`, `profile/.../following`, `detail/{coffeeId}`, `historial`. La barra de pestañas principal tiene: **Inicio** (home), **Explorar** (search), **Elabora** (brewlab), **Diario** (diary), **Perfil** (profile).

3. **Diseño y tokens:** Respeta el design system del proyecto. **No inventes colores ni componentes;** usa únicamente los tokens de `docs/DESIGN_TOKENS.md` (fondos día/noche, marrón espresso/caramelo, rojo eliminar, azul agua, bordes, texto, espaciados, radios). Crea en iOS un módulo de tokens equivalente (p. ej. `iosApp/CafesitoIOS/DesignSystem/` con `ColorTokens`, `TypographyTokens`, `SpacingTokens`, `RadiusTokens`) alineado 1:1 con Android y Web, tal como exige `docs/MASTER_ARCHITECTURE_GOVERNANCE.md` (Fase 4, sección 4.2.3 y 4.11).

4. **Versión mínima y Liquid Glass (iOS 26):** La versión mínima de la app es **iOS 18**. Donde el sistema lo permita (**iOS 26**), usa el lenguaje de diseño **Liquid Glass** de Apple con `@available(iOS 26)`:
   - Barra de pestañas inferior (TabView) y barras de navegación con aspecto glass (translúcido, coherente con el sistema).
   - Usa las APIs de SwiftUI que apliquen (p. ej. `.glassEffect()` y variantes cuando estén disponibles en el SDK de iOS 26).
   - Sheets, toolbars y controles nativos que se recompilen con el SDK de iOS 26 pueden adoptar Liquid Glass automáticamente; prioriza controles nativos sobre custom cuando el comportamiento sea el mismo.
   Para versiones anteriores a iOS 26, mantén la misma estructura de pantallas y navegación con los estilos estándar de SwiftUI (NavigationStack, TabView, etc.), sin romper la experiencia.

5. **Arquitectura y documentación:** Sigue los principios de `docs/MASTER_ARCHITECTURE_GOVERNANCE.md`: dominio en `shared/`, solo adaptadores y UI en iOS. Para cada flujo (Despensa, Diario, BrewLab, Perfil, Auth, Detalle café, listas, opciones de lista, etc.) consulta también `docs/SHARED_BUSINESS_LOGIC.md` para saber qué está en shared y qué debe hacer la capa iOS. Para accesibilidad, aplica el checklist de `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md`: etiquetas para lectores de pantalla (equivalente a `contentDescription`), área de tap mínima (≥ 44 pt), contraste WCAG AA; extiende el checklist a iOS en los mismos flujos.

6. **Autenticación:** Implementar **Google Sign-In** y **Sign in with Apple**, ambos integrados con Supabase Auth (paridad con Android para Google). Para Apple, Supabase debe configurarse con el proveedor OAuth de Apple usando **Client ID(s)** y **Secret Key**; la IA debe implementar el flujo en la app y documentar dónde el desarrollador debe configurar esos valores (dashboard de Supabase + proyecto iOS: capabilities, Info.plist, etc.). No hardcodear credenciales; usar placeholders o configuración en entorno/build.

7. **Analíticas:** Usar **Firebase Analytics** en iOS. Enviar los mismos `screen_name` (rutas/pantallas) y eventos que Android para paridad. **Al implementar**, actualizar `docs/ANALITICAS.md` añadiendo la sección de iOS: qué pantallas/rutas se trackean, qué eventos custom se envían y en qué archivos.

8. **Entregables esperados:**
   - Estructura de proyecto iOS en `iosApp/CafesitoIOS/` con: App entry, Design System (tokens), vistas por flujo (Login con Google y Apple, Home/Timeline, Explorar/Search, BrewLab, Diario, Perfil, Detalle café, Despensa, Añadir entrada, listas, opciones, etc.), wrappers para los ViewModels/casos de uso de `shared` que hagan falta, y navegación (NavigationStack + TabView u equivalente).
   - Paridad funcional con Android en los criterios de aceptación de `docs/DOCUMENTO_FUNCIONAL_CAFESITO.md` (Despensa, Diario, Elaboración “Selecciona café”, Auth, Perfil con actividad, etc.).
   - Integración con Supabase (Auth con Google y Apple, tablas `pantry_items`, `diary_entries`, etc.) usando la misma configuración y contratos que Android; si en shared hay clientes o repositorios que ya soporten Kotlin/Native, reutilízalos desde iOS vía el framework.
   - Código listo para compilar en Xcode con el package `Shared` (XCFramework) añadido según `docs/STEP7_IOS_SETUP.md`.

**Orden de lectura antes de implementar:**  
`docs/README.md` (índice §4) → `docs/MASTER_ARCHITECTURE_GOVERNANCE.md` (Fases 1–4) → `docs/DOCUMENTO_FUNCIONAL_CAFESITO.md` → `docs/DESIGN_TOKENS.md` → `docs/STEP6_SWIFTUI.md` → `docs/STEP7_IOS_SETUP.md` → `docs/SHARED_BUSINESS_LOGIC.md` → `docs/ANALITICAS.md` (eventos y parámetros a replicar en iOS) → `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md`. Referencia de estructura shared: `docs/KMP_AUDIT_STEP0.md`. Usa la documentación como fuente de verdad; no contradigas el Documento Maestro ni los criterios funcionales.

**FIN DEL PROMPT**

---

## 3. Cómo actuar (checklist para la IA)

| Fase | Acción |
|------|--------|
| **Antes de codear** | Entrar por `docs/README.md` §4; leer los docs del orden indicado en el prompt. En Cursor: reglas `docs-before-code.mdc` y `ios-swiftui.mdc`. |
| **Por cada flujo/pantalla** | Criterios en `docs/DOCUMENTO_FUNCIONAL_CAFESITO.md`; qué consumir de shared en `docs/SHARED_BUSINESS_LOGIC.md`. Estados vacío/error: `docs/UX_EMPTY_AND_ERROR_STATES.md`. |
| **Al añadir pantallas o eventos** | Actualizar `docs/ANALITICAS.md`: sección iOS (rutas = `screen_name`, eventos `modal_open`/`modal_close`/`button_click` con mismos `modal_id`/`button_id`, archivos donde se envían). |
| **Accesibilidad** | Checklist `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md` (VoiceOver, área tap ≥ 44 pt, contraste WCAG AA). |
| **Al terminar** | Código compilable en Xcode con `Shared.xcframework`; paridad con documento funcional; `docs/ANALITICAS.md` con sección iOS. |

---

## 4. Referencias rápidas (documentos a consultar)

| Documento | Uso |
|-----------|-----|
| `docs/README.md` | Índice único; §4 por tipo (arquitectura, release, iOS, servicios, analíticas). |
| `docs/MASTER_ARCHITECTURE_GOVERNANCE.md` | Principios, shared como SSOT, design system, responsabilidad iOS. |
| `docs/DOCUMENTO_FUNCIONAL_CAFESITO.md` | Flujos y criterios de aceptación (Despensa, Diario, Elaboración, Auth, Perfil, listas, detalle). |
| `docs/DESIGN_TOKENS.md` | Colores, espaciados, radios; alinear iOS 1:1 con Android/Web. |
| `docs/STEP6_SWIFTUI.md` | Patrón wrapper ViewModel (StateFlow → SwiftUI); ejemplo Search. |
| `docs/STEP7_IOS_SETUP.md` | Proyecto iOS, SPM, XCFramework, generación con Gradle. |
| `docs/SHARED_BUSINESS_LOGIC.md` | Qué está en shared; qué debe hacer la capa iOS. |
| `docs/ANALITICAS.md` | Eventos y parámetros (screen_name, modal_id, button_id); actualizar con sección iOS. |
| `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md` | Criterios a11y; equivalente en iOS (etiquetas, 44 pt, contraste). |
| `docs/UX_EMPTY_AND_ERROR_STATES.md` | Patrón estados vacío y error (mensaje + CTA / Reintentar). |
| `docs/KMP_AUDIT_STEP0.md` | Referencia estructura shared; estado actual en MASTER y SHARED_BUSINESS_LOGIC. |
| `app/.../navigation/AppNavigation.kt` | Rutas y pestañas Android (paridad de navegación). |

**En Cursor:** `AGENTS.md` y `.cursor/rules/docs-before-code.mdc`, `.cursor/rules/ios-swiftui.mdc` refuerzan estos documentos.

---

## 5. Mantenimiento de este documento

- Si añades **pantallas o flujos** en Android, actualiza el prompt (§2) con el listado de rutas y pestañas.
- Si cambias **design system o tokens**, mantén alineados `docs/DESIGN_TOKENS.md` y este prompt.
- Si cambias **analíticas** o `docs/gtm/`, revisa que el prompt y §4 citen bien `docs/ANALITICAS.md`.
- Tras el primer desarrollo iOS, añade si aplica un **Checklist de verificación iOS** (build, tests, smoke) enlazando a `docs/SMOKE_TESTS.md` y `docs/RELEASE_DEPLOY_WORKFLOW.md`.
