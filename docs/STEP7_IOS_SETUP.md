# Paso 7 — Proyecto iOS (SwiftUI) + Shared via Swift Package Manager (SPM)

## Elección de integración: SPM con XCFramework local
Elegí SPM para mantener el flujo moderno de Xcode y evitar dependencias adicionales. El módulo `shared` genera un `Shared.xcframework` que se consume como **binary target** mediante un `Package.swift` local.  

## Estructura creada
```
iosApp/
  Package.swift
  Shared.xcframework  # generado por Gradle
  CafesitoIOS/
    CafesitoIOSApp.swift
    SearchView.swift
    SearchViewModelWrapper.swift
```

## Pasos exactos en Xcode
1. En macOS, abre Xcode y crea un proyecto iOS SwiftUI:
   - **Product Name:** CafesitoIOS
   - **Interface:** SwiftUI
   - **Language:** Swift
   - **Deployment Target:** iOS 14.1+
2. Copia los archivos de `iosApp/CafesitoIOS/` dentro del target creado.
3. Genera el XCFramework desde el repo:
   ```bash
   ./gradlew :shared:assembleSharedReleaseXCFramework
   ```
4. Copia `shared/build/XCFrameworks/release/Shared.xcframework` a `iosApp/Shared.xcframework`.
5. En Xcode, agrega `iosApp/Package.swift` como **Swift Package** (File → Add Packages → Add Local).
6. Compila el target con el paquete `Shared` añadido.

## Configuración Gradle (shared)
El módulo `shared` genera un framework iOS con el nombre por defecto (`shared`). Al copiar el XCFramework puedes renombrarlo a `Shared.xcframework` para coincidir con el `Package.swift`.

## Código SwiftUI (Search)
La pantalla Search usa el `SearchViewModel` compartido con `StateFlow`, expuesto a Swift mediante `CommonFlow`.
Los archivos ya están listos en:
- `iosApp/CafesitoIOS/SearchView.swift`
- `iosApp/CafesitoIOS/SearchViewModelWrapper.swift`

## Tareas de Gradle (para el framework)
```bash
./gradlew :shared:assembleSharedReleaseXCFramework
```

## Verificación
- Compila en simulador desde el proyecto Xcode con el paquete `Shared` añadido.
