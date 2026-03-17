# Paso 0 — Auditoría del proyecto (mapa de lo reutilizable)

**Estado:** referencia histórica para migración a `shared`.  
**Estado actual de arquitectura:** `docs/MASTER_ARCHITECTURE_GOVERNANCE.md`, `docs/SHARED_BUSINESS_LOGIC.md`, `docs/MULTIPLATFORM_EXECUTION_PLAN.md`.

---

## 1) Paquetes/clases que deben moverse a `shared` (y por qué)

> **Criterio general:** cualquier modelo puro, reglas de negocio o lógica reusable sin dependencias de `android.*` debe vivir en `shared` para poder reutilizarse en iOS y testearse de forma aislada.

### Modelos puros (domain/data shared)
- `com.cafesito.app.domain.Model.kt`
  - `User`, `Post`, `Comment`, `Review`, `SuggestedUserInfo`.
  - **Por qué:** Son data classes sin dependencias Android; representan entidades de dominio.

### Modelos/DTOs de datos (shared/data)
- `com.cafesito.app.data.Entities.kt`
  - **Mover:** DTOs y entidades serializables que no dependan de Room/Compose (`@Serializable` sin Room):
    - `DiaryEntryInsert`, `CustomCoffeeEntity` (si se separa de Room), `DiaryEntryEntity`, `PantryItemEntity`, `UserEntity`, `PostEntity`, `LikeEntity`, `CommentEntity`, `LocalFavorite`.
  - **Por qué:** Son datos usados en Supabase/PostgREST y sirven como DTOs compartidos.
  - **Nota:** Las anotaciones `@Entity`, `@Relation`, `@Embedded` y `@Immutable` deberán separarse para Android (Room/Compose) o moverse a un submódulo Android-only para evitar dependencias no multiplataforma.

### Lógica de negocio (shared/domain)
- `com.cafesito.app.data.*Repository` → mover **interfaces** a `shared/domain`.
  - `CoffeeRepository`, `DiaryRepository`, `SocialRepository`, `UserRepository`.
  - **Por qué:** Definen el contrato del dominio sin saber de la plataforma.

### Casos de uso y validaciones (shared/domain)
- Lógica hoy dispersa en ViewModels (por ejemplo en `ui/...ViewModel.kt`) que valide campos, filtros o reglas de negocio.
  - **Por qué:** Reglas consistentes entre Android e iOS; test unitario puro.

### Infraestructura multiplataforma (shared/core)
- `Resource`, `Exceptions` (ajustar para que sean multiplataforma).
  - **Por qué:** Tipos de errores y wrapper de estados deben ser comunes.

## 2) Lo que se queda en Android

- **UI y presentación Android (Compose)**
  - `com.cafesito.app.ui.*` (pantallas, componentes, ViewModels Android actuales).
- **DI y módulos Android**
  - `com.cafesito.app.di.*` (Hilt + Android context).
- **Room/DB Android**
  - `AppDatabase.kt`, `Daos.kt` y entidades con anotaciones `@Entity`, `@Relation` específicas de Room.
- **Android-only / platform**
  - `MainActivity.kt`, `CafesitoApp.kt`, `HealthConnect*`.

## 3) Estructura recomendada de `shared`

```
shared/
  src/
    commonMain/
      kotlin/
        com/cafesito/shared/
          core/         # Result/Error/Dispatchers/DateTime utils
          domain/       # modelos puros + interfaces + use cases + validaciones
          data/         # repositorios (impl) + DTOs + mappers
          presentation/ # (opcional) ViewModels KMP + UiState/Reducer
    commonTest/
      kotlin/
        com/cafesito/shared/
```

## 4) Riesgos típicos y mitigación

- **Serialización JSON (Supabase)**
  - Riesgo: nombres de campos diferentes a los actuales.
  - Mitigación: usar `@SerialName` igual que en Android, tests encode/decode por modelo.
- **Auth/Session**
  - Riesgo: manejar tokens y refresh entre plataformas.
  - Mitigación: wrapper en `shared/data` con almacenamiento seguro por plataforma.
- **Almacenamiento local**
  - Riesgo: Room no es multiplataforma.
  - Mitigación: usar SQLDelight o otro driver multiplataforma en `shared/data/local`.
- **Fechas y zona horaria**
  - Riesgo: `System.currentTimeMillis()` vs `Instant` y parsing diferente.
  - Mitigación: usar `kotlinx-datetime` y normalizar a UTC.
- **UUID**
  - Riesgo: generación diferente por plataforma.
  - Mitigación: usar `kotlinx-uuid` o wrapper expect/actual.
- **Main thread / coroutines**
  - Riesgo: bloqueo en UI.
  - Mitigación: `Dispatchers.Default/IO`, y en iOS usar `DispatcherProvider`/`MainDispatcher`.

## 5) Checklist de compilación y tests (sin cambios aún)

- Android Debug build:
  - `./gradlew :app:assembleDebug`
- Unit tests Android:
  - `./gradlew :app:testDebugUnitTest`
- Shared common tests:
  - `./gradlew :shared:testDebugUnitTest`
