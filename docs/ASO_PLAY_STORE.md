# ASO — App Store Optimization (Google Play)

**Objetivo:** Mejorar la visibilidad y conversión de Cafesito en Google Play mediante ficha optimizada, palabras clave y buenas prácticas.

**Última actualización:** 2026

---

## 1. Textos para la ficha de Play (copiar en Play Console)

Configuración en **Play Console → Tu app → Presencia en la tienda → Ficha de la tienda**.

### 1.1 Nombre de la aplicación (hasta 30 caracteres)

Texto sugerido para el **nombre en la tienda** (puede ser más largo que el nombre bajo el icono, que sigue siendo "Cafesito" en el dispositivo):

```
Cafesito — Diario y comunidad café
```

Alternativas si necesitas variar:
- `Cafesito: café, diario y comunidad`
- `Cafesito | Tu diario del café`

### 1.2 Descripción breve (hasta 80 caracteres)

Se muestra en resultados de búsqueda. Incluir palabras clave y propuesta de valor.

```
Tu diario de café, reseñas, despensa y comunidad de amantes del café. Descubre y comparte.
```

### 1.3 Descripción completa (hasta 4.000 caracteres)

Texto promocional y orientado a búsquedas (café, diario, reseñas, barista, etc.):

```
¿Te gusta el café? Cafesito es tu app para vivir el café de principio a fin.

— LLEVA TU DIARIO DE CAFÉ
Registra cada taza: qué café, cómo lo preparaste y qué tal estuvo. Lleva un seguimiento de tu cafeína e hidratación y revisa tu evolución por día, semana o mes.

— RESEÑAS Y RECOMENDACIONES
Valora cafés, comparte notas y descubre qué prueban otros. Busca por marca, origen, tueste o código de barras.

— TU DESPENSA EN EL BOLSILLO
Añade cafés a tu despensa, controla gramos y no te quedes nunca sin tu favorito. Escanea el código de barras para encontrar el café al instante.

— ELABORACIÓN Y BARISTA
Calcula ratio, agua y tiempo para V60, cafetera, AeroPress y más. Sigue recetas paso a paso y mejora tu técnica.

— COMUNIDAD
Publica fotos y momentos, comenta y sigue a otros amantes del café. Recibe notificaciones de menciones y nuevos seguidores.

— PERFIL SENSORIAL (ADN)
Tu perfil de gustos según tus cafés favoritos y reseñas: aroma, cuerpo, acidez. Ideal para afinar recomendaciones.

Cafesito es la app para quien quiere llevar un diario de café, descubrir nuevos granos y formar parte de una comunidad cafetera. Descarga y empieza a registrar tu primera taza.
```

### 1.4 Palabras clave (referencia; Play no tiene campo “keywords” como iOS)

Incluir de forma natural en nombre corto y descripciones:

- café, diario de café, diario café
- reseñas café, valorar café, notas café
- despensa café, stock café, código de barras café
- elaboración café, V60, AeroPress, barista, ratio
- comunidad café, amantes del café
- cafeína, hidratación, tazas
- perfil sensorial, gustos café

---

## 2. Checklist de ficha en Play Console

- [ ] **Nombre de la app** (30 caracteres): usar variante de §1.1.
- [ ] **Descripción breve** (80 caracteres): texto de §1.2.
- [ ] **Descripción completa**: texto de §1.3 (puedes acortar si hace falta).
- [ ] **Gráficos**: icono 512×512, capturas (mínimo 2; recomendado 4–8) mostrando diario, búsqueda, elaboración, perfil.
- [ ] **Categoría**: Estilo de vida o Comida y bebida (elegir la que mejor encaje).
- [ ] **Contacto**: email de soporte y, si aplica, URL de política de privacidad (ya en legal_url_base).
- [ ] **Notas de la versión**: el workflow de release ya genera “What’s new” promocional; revisar que se suban en cada release.

---

## 3. A nivel de código (qué hay y qué se puede aplicar)

### 3.1 Ya implementado y alineado con ASO

| Elemento | Dónde | Notas |
|----------|--------|--------|
| **Nombre de la app** | `app/src/main/res/values/strings.xml` → `app_name` = "Cafesito" | Nombre bajo el icono; corto y reconocible. El **título en Play** (hasta 30 caracteres) se configura en Play Console y puede ser distinto (§1.1). |
| **Shortcuts** | `app/src/main/res/xml/shortcuts.xml` | Acciones rápidas (Busca café, Nuevo post, Elaboración, Mi diario) mejoran uso y engagement. |
| **URL legal** | `strings.xml` → `legal_url_base` | Enlace a privacidad/condiciones; necesario para la ficha y cumplimiento. |

### 3.2 Android App Links (implementado en código)

En el proyecto ya está declarado un intent-filter para que enlaces `https://cafesitoapp.com/...` puedan abrir la app cuando esté instalada (`MainActivity`, `android:autoVerify="true"`).

**Para que la verificación sea automática** (abrir directo en la app sin “Abrir con…”):

1. **Obtener el SHA-256 del certificado** con el que firmas el AAB (Play App Signing usa su propio certificado; en Play Console: Configuración de la app → Integridad de la app → “Clave de firma de la aplicación” → SHA-256).
2. **Publicar en el servidor** el fichero `assetlinks.json` en:
   ```
   https://cafesitoapp.com/.well-known/assetlinks.json
   ```
   Ejemplo de contenido (sustituir `SHA256_DE_TU_CERTIFICADO` por el valor real y el package por `com.cafesito.app`):
   ```json
   [{
     "relation": ["delegate_permission/common.handle_all_urls"],
     "target": {
       "namespace": "android_app",
       "package_name": "com.cafesito.app",
       "sha256_cert_fingerprints": ["SHA256_DE_TU_CERTIFICADO"]
     }
   }]
   ```
3. Comprobar que la URL responde con `Content-Type: application/json` y que el dominio es accesible por HTTPS.

Si no publicas `assetlinks.json`, el usuario seguirá pudiendo abrir la app desde el enlace eligiendo “Abrir con Cafesito”, pero no se hará la apertura directa verificada.

### 3.3 Comentario en strings.xml

Se ha añadido un comentario en `strings.xml` junto a `app_name` que remite a este documento para que quien edite la ficha sepa que el título y descripciones de Play se gestionan en la consola y están definidos aquí.

---

## 4. Resumen

- **Documento:** Textos listos para copiar en Play Console (§1) y checklist (§2).
- **Código:** No es obligatorio cambiar nada; el nombre "Cafesito" y los shortcuts ya son adecuados. Opcional: App Links si quieres que enlaces web abran la app; comentario en `strings.xml` para enlazar con esta guía de ASO.

Cuando actualices la ficha en Play, usa los textos de §1 y revisa el checklist de §2. Si más adelante añades App Links, se puede documentar aquí la configuración exacta de `intent-filter` y `assetlinks.json`.
