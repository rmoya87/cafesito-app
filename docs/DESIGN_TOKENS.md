# Tokens de diseño — Fuente de verdad

**Propósito:** Un único documento de referencia para colores, espaciados y radios usados en WebApp y Android. Evita que ambas plataformas se desalineen con el tiempo.

**Última actualización:** 2026-03-04  
**Ámbito:** WebApp (`webApp/`), Android (`app/`).

---

## 1. Colores

### 1.1 Fondos de pantalla

| Uso | Modo día | Modo noche | Web (CSS var) | Android |
|-----|----------|------------|---------------|---------|
| Fondo principal | `#f7f7f7` | `#212121` | `--screen-light-background` / `--screen-dark-background` | `ScreenLightBackground` / `ScreenDarkBackground` |
| Superficie (cards, modales) | `#ffffff` | `#000000` | `--pure-white` / surface en dark | `PureWhite` / `PureBlack` (MaterialTheme.surface) |

### 1.2 Marrón / café (acento principal)

| Uso | Modo día | Modo noche | Web (CSS var) | Android |
|-----|----------|------------|---------------|---------|
| Acento principal (botones primarios, iconos café) | `#6f4e37` | `#d4a373` | `--caramel-accent` (dark: override) | `CaramelAccent` / `CaramelSoft` → `LocalCaramelAccent` |
| Secundario / soft (menciones, chips activos) | `#6f4e37` | `#d4a373` | `--caramel-soft` | `CaramelSoft` |
| Títulos / destacado en dark | — | `#e8c9a8` | `--espresso-deep` (dark) | `CaramelLight` |
| Medio (texto secundario día) | `#3c2a21` | `#d4a373` | `--espresso-medium` | `EspressoMedium` |
| Fondo oscuro (botón Google día) | `#1a120b` | `#e8c9a8` | `--espresso-deep` | `EspressoDeep` / `CaramelLight` |

**Regla:** En modo día el acento es marrón café (`#6f4e37`). En modo noche es marrón claro (`#d4a373`). Botones primarios: día = texto blanco sobre marrón; noche = texto negro sobre marrón claro.

### 1.3 Rojo eliminar / peligro

| Uso | Valor (igual día y noche) | Web (CSS var) | Android |
|-----|----------------------------|---------------|---------|
| Botones eliminar, swipe borrar, confirmación destructiva | `#ff3b30` | `--electric-red` | `ElectricRed` |
| Texto/icono sobre rojo | Día: blanco. Noche: negro | Clases `.is-delete` / dark | `contentColor` según tema |

### 1.4 Azul agua

| Uso | Valor | Web (CSS var) | Android |
|-----|-------|---------------|---------|
| Iconos/indicadores de agua, entradas agua en diario | `#2196f3` | `--water-blue` | `WaterBlue` |
| Fondo suave entradas agua | `#e3f2fd` | `--water-blue-bg` | `WaterBlueBackground` |

### 1.5 Neutros (bordes, texto secundario)

| Uso | Modo día | Modo noche | Web (CSS var) | Android |
|-----|----------|------------|---------------|---------|
| Borde estándar | `rgba(224,224,224,0.4)` | `#000000` | `--border-light` / `--border-default` | `BorderLight` / `MaterialTheme.colorScheme.outline` |
| Rail navegación desktop | `rgba(0,0,0,0.14)` | `rgba(255,255,255,0.32)` | `--nav-rail-border` | — |
| Texto atenuado | `#bdb7b2` | `#bdb7b2` | `--muted-cream` / `--text-muted` | `MutedCream` |
| Fechas / timestamps | Gris oscuro | Gris claro | Clases + color | `LocalDateMetaColor` → `DateMetaLight` / `DateMetaDark` (`#5c5c5c` / `#b8b8b8`) |

### 1.6 Blanco y negro puro

| Uso | Web | Android |
|-----|-----|---------|
| Blanco | `#ffffff` → `--pure-white` | `PureWhite` |
| Negro | `#000000` → `--pure-black` | `PureBlack` |

### 1.7 Otros semánticos (igual día y noche)

| Uso | Valor | Web | Android |
|-----|-------|-----|---------|
| Éxito | `#81c784` | `--success-green` | `SuccessGreen` |
| Deshabilitado | `#9e9e9e` | `--disabled-gray` | `DisabledGray` |
| Naranja/aviso | `#ffb300` | `--orange-yellow` | `OrangeYellow` |
| Focus ring (accesibilidad) | `rgba(111,78,55,0.24)` dark: `rgba(212,163,115,0.24)` | `--focus-ring` | — |

---

## 2. Espaciados y gaps

### 2.1 Escala base (Web)

Usar esta escala para consistencia. Android puede mapear a `dp` (1:1 con px para espaciado típico).

| Token | Valor | Uso típico |
|-------|-------|------------|
| `--space-1` | 4px | Gaps mínimos, padding interno |
| `--space-2` | 8px | Padding estándar pequeño, gaps entre chips |
| `--space-3` | 12px | Gutter, márgenes |
| `--space-4` | 16px | Padding de card, separación entre bloques |
| `--space-5` | 20px | Separación media |
| `--space-6` | 24px | Separación grande |
| `--space-8` | 32px | Padding de pantalla, márgenes amplios |

**Android:** Usar `4.dp`, `8.dp`, `12.dp`, `16.dp`, `20.dp`, `24.dp`, `32.dp` de forma coherente con esta escala.

### 2.2 Casos de uso documentados

| Componente | Gap / espaciado | Notas |
|------------|-----------------|--------|
| Rejilla de meta del diario (`.diary-entry-meta-grid`) | **Base:** `8px 30px` (row col). **&lt; 899px:** `8px 25px` | Web: `features.css`. Android: alinear si existe equivalente. |

---

## 3. Radios (border-radius)

### 3.1 Tokens estándar

| Token | Valor | Uso |
|-------|-------|-----|
| `--radius-pill` | 999px | Chips pill, botones redondeados tipo pill |
| `--radius-sm` | 10px | Botones pequeños, inputs |
| `--radius-md` | 12px | Cards pequeñas, chips |
| `--radius-lg` | 16px | Cards, modales |
| `--radius-card` | 18px | Cards principales |

**Android:** `RoundedCornerShape(10.dp)`, `(12.dp)`, `(16.dp)`, `(18.dp)`, y para pill `(24.dp)` o `(50)` (porcentaje) según componente. Chip de fecha diario: `24.dp` (pill).

### 3.2 Casos específicos

| Componente | Radio | Web | Android |
|------------|-------|-----|---------|
| Botón Google (login) | 20px | — | `RoundedCornerShape(20.dp)` |
| Chip selector período / fecha | Pill (24dp equivalente) | `border-radius` en chip | `RoundedCornerShape(24.dp)` |
| Botón circular (icono) | 50% | `border-radius: 50%` | `CircleShape` o `RoundedCornerShape(50)` |
| Detalle café (esquinas superiores) | 24px / 30px | `24px 24px 0 0` y `30px` en primera sección | Alinear si aplica |

---

## 4. Dónde se implementa

| Plataforma | Archivo principal | Notas |
|------------|-------------------|--------|
| Web | `webApp/src/styles/tokens.css` | Variables CSS en `:root` y `@media (prefers-color-scheme: dark)`. |
| Android | `app/.../ui/theme/Color.kt` | Colores estáticos. |
| Android | `app/.../ui/theme/Theme.kt` | `LightColorScheme` / `DarkColorScheme`, `LocalCaramelAccent`, `LocalDateMetaColor`. |

Al añadir o cambiar un token, actualizar este documento y el fichero correspondiente de cada plataforma para mantener la paridad.
