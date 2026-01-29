# Paso 8 — Batería final de calidad

## Objetivo
Validar que el código compartido y Android siguen funcionando con un set completo de pruebas y checks locales reproducibles.

## Comandos recomendados (local)
### Android + shared
```bash
./gradlew :shared:testDebugUnitTest
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

### Opcional (calidad)
Si decides añadir linting en el futuro, recomiendo:
- **detekt** para análisis estático.
- **ktlint** para formato.

## Criterios de aceptación
- Todas las tareas anteriores finalizan con `BUILD SUCCESSFUL`.
- Android compila en Debug.
- Las pruebas unitarias de `shared` y `app` pasan.

## Riesgos restantes + mitigación
- **Compatibilidad AGP/Kotlin KMP:** estamos usando AGP 8.13.2, que está por encima de la versión “máxima probada”.  
  **Mitigación:** mantener Kotlin Gradle Plugin actualizado y monitorear compatibilidad. Se silenció la advertencia con `kotlin.mpp.androidGradlePluginCompatibility.nowarn=true` en `gradle.properties`.
- **Targets iOS deshabilitados en máquinas no macOS:** no se pueden compilar localmente desde Linux/Windows.  
  **Mitigación:** usar CI macOS y permitir ignorar targets deshabilitados con `kotlin.native.ignoreDisabledTargets=true`.
- **Jerarquía por defecto en KMP:** se desactivó para evitar advertencias mientras se usa `dependsOn`.  
  **Mitigación:** planificar migración a la default hierarchy en un paso posterior.
- **Tests SQLDelight en iOS:** los tests con `JdbcSqliteDriver` son JVM-only.  
  **Mitigación:** moverlos a `androidUnitTest`, dejando `commonTest` solo con dependencias multiplataforma.
