# Cafesito — Plan de ejecución multiplataforma (Android + iOS + Web)

## 1) Decisión web final

**Recomendación: Opción B (React + Vite PWA + Supabase JS)**.

### Por qué esta opción
1. **UX nativa en móvil web**: PWA instalable, service worker con caching, navegación tipo tabs/bottom bar, atajos y control fino de scroll/gestos con APIs web estándar.
2. **Usabilidad en PC**: layout desktop con sidebar/topbar y navegación por teclado sin dependencias experimentales.
3. **Riesgo bajo en iOS Safari**: stack web madura; evita limitaciones actuales de Kotlin/Wasm en ecosistema/productividad.
4. **Mantenibilidad**: onboarding más rápido para equipo web, toolchain estable, CI/CD simple para hosting estático (IONOS).
5. **Integración Supabase**: SDK oficial JS robusto para Auth PKCE, storage de sesión y RLS.

### Riesgos
- Menor reutilización directa de KMP en cliente web (se comparte dominio por contrato/API y tests de contrato).
- Duplicación parcial de presentation models en web.

### Plan B corto
- Si se exige mayor unificación de lógica cliente, migrar progresivamente a **Kotlin/Wasm + Compose Web** para pantallas concretas no críticas de SEO, manteniendo PWA shell y auth en React durante transición.

---

## 2) Auditoría del estado actual (ETAPA 1)

### Hallazgos en `shared`
- `shared` ya encapsula dominio, repositorios, validaciones y acceso remoto con Supabase (`postgrest`, `auth`).
- iOS está habilitado con targets `iosX64/iosArm64/iosSimulatorArm64`.
- Existe capa `SearchViewModel`/`SearchUi` exportable a iOS.

### Gaps para 3 plataformas
1. **Auth storage seguro**: falta abstracción explícita para secure storage de sesión/token por plataforma (Keychain/Keystore).
2. **Contratos de error unificados**: hay `Result` y `DomainError`, pero no está unificado en todas las features.
3. **Observabilidad transversal**: falta contrato común para logging estructurado + analytics + crash reporting.
4. **Web-ready contracts**: falta documento de contrato API consumible por web para evitar drift de dominio.

### Refactors mínimos propuestos
- Crear `SessionSecureStore` expect/actual (Android Keystore, iOS Keychain).
- Estandarizar `DomainError` + mapper por datasource.
- Introducir `Telemetry` (trace, event, crash breadcrumb) en `shared`.
- Publicar `docs/contracts/*.md` con tablas/JSON examples de Supabase tables usadas por cliente.

---

## 3) Arquitectura de monorepo propuesta

```txt
/shared                # KMP (domain/data/usecases/validation/telemetry contracts)
/app                   # Android nativo Compose (actual actual)
/iosApp                # iOS nativo SwiftUI + wrappers KMP
/webApp                # React + Vite PWA
/infra
  /ci                  # plantillas y utilidades CI/CD
  /scripts             # scripts build/test/release
/docs                  # arquitectura, ADRs, contratos, runbooks
```

### Convenciones
- **DTOs**: sufijo `Dto`, solo capa data.
- **Domain**: modelos puros, sin dependencia de plataforma.
- **Presentation models**: `UiState`, `UiIntent`, `UiEffect` por feature.
- **Errores**: `DomainError` + mappers (`Network`, `Auth`, `Validation`, `Unknown`).
- **Mappers**: `toDto()/toDomain()/toUi()` en archivos dedicados.

---

## 4) Checklist por etapas

- [x] ETAPA 1 Auditoría + plan y riesgos.
- [x] ETAPA 2 Base iOS SwiftUI (wrapper KMP y navegación existente verificada).
- [x] ETAPA 3 Base Android Compose (sin romper flujo actual).
- [x] ETAPA 4 Base Web PWA (manifest, SW, layout móvil/escritorio, auth PKCE base).
- [x] ETAPA 5 CI inicial Android+iOS+Web.

## 5) Seguridad
- Web usa **solo anon key** de Supabase (`VITE_SUPABASE_ANON_KEY`).
- Prohibido service role en cliente.
- CSP base incluida en `webApp/index.html`.
- En producción añadir headers:
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `Referrer-Policy: strict-origin-when-cross-origin`

## 6) Definition of Done
- `shared` tests unitarios pasan.
- Android compila en debug.
- Web compila y tests base pasan.
- Workflow CI para Android/iOS/Web presente.
- Documentación de decisión y ejecución publicada.


## 7) Comandos locales exactos
- Android (debug): `./gradlew :app:assembleDebug`
- Shared KMP tests: `./gradlew :shared:allTests`
- iOS framework (desde KMP): `./gradlew :shared:assembleXCFramework`
- Web install: `cd webApp && npm ci`
- Web test: `cd webApp && npm test`
- Web build: `cd webApp && npm run build`
- Smoke end-to-end local: `./infra/scripts/smoke-build.sh`

- Windows bootstrap entorno: `scripts\run-dev-env.bat`
