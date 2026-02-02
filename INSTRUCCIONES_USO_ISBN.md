# 📱 Instrucciones de Uso - Escáner de ISBN Mejorado

## 🚀 Inicio Rápido

### 1. Compilar e Instalar

```bash
# Opción 1: Desde línea de comandos
cd C:\git\foundation-library
.\gradlew installDebug

# Opción 2: Desde Android Studio
# Run → Run 'app' (Shift+F10)
```

### 2. Usar el Escáner de ISBN

#### Paso a paso:

1. **Abrir la aplicación** en tu dispositivo/emulador

2. **Seleccionar "Escanear ISBN"** en el menú principal

3. **Apuntar la cámara** al código de barras del libro
   - El código de barras suele estar en la contraportada
   - Asegúrate de tener buena iluminación
   - Mantén el teléfono estable a ~15-20 cm del código

4. **Esperar detección automática** (< 1 segundo)
   - Verás el ISBN detectado en pantalla
   - La app buscará automáticamente información del libro

5. **Revisar resultados**
   - Título, Autor, Editorial
   - **Clasificación LC** (Library of Congress)
   - **Clasificación Dewey** (Decimal)
   - **Clasificación DCU** (Universal)

## 📊 Ejemplos de Uso

### Caso 1: Libro con ISBN-13

```
Código detectado: 9780140328721
↓
Búsqueda automática en:
  1. OpenLibrary API ✓
↓
Resultado:
  Título: Of Mice and Men
  Autor: John Steinbeck
  LC: PS3537.T3234 O4
  Dewey: 813.52
```

### Caso 2: Libro con ISBN-10

```
Código detectado: 0596520689
↓
Conversión automática: 9780596520687
↓
Búsqueda en múltiples fuentes...
↓
Resultado con clasificaciones
```

### Caso 3: Libro no encontrado en OpenLibrary

```
Código detectado: 9781234567890
↓
Intento 1: OpenLibrary API ✗
Intento 2: OpenLibrary Direct ✗
Intento 3: Google Books ✓
↓
Resultado con datos disponibles
```

## 🎯 Características Especiales

### Escaneo Continuo

- ✅ **No necesitas tomar foto** - la detección es automática
- ✅ **Múltiples intentos** - puedes alejar/acercar para mejor enfoque
- ✅ **Debounce integrado** - evita escaneos duplicados (2.5 seg)

### Preservación de Datos

```
Flujo de trabajo híbrido:
1. Escanear secciones (título, autor) manualmente
2. Escanear ISBN con código de barras
3. Los datos se COMBINAN (no se sobrescriben)
4. Resultado: Libro completo con clasificaciones
```

### Formatos Soportados

| Formato | Descripción | Ejemplo |
|---------|-------------|---------|
| EAN-13 | Más común para ISBN-13 | 978-0-14-032872-1 |
| EAN-8 | Versión corta | 12345678 |
| UPC-A | Código universal productos | 012345678905 |
| UPC-E | UPC comprimido | 01234565 |

## 🔍 Solución de Problemas

### Problema: No detecta el código de barras

**Soluciones:**
- ✅ Asegúrate de tener buena iluminación
- ✅ Limpia la cámara del teléfono
- ✅ Mantén el código de barras paralelo a la cámara
- ✅ Ajusta la distancia (15-25 cm es óptimo)
- ✅ Verifica permisos de cámara en configuración

### Problema: ISBN detectado pero sin resultados

**Causas posibles:**
- El ISBN puede ser muy antiguo (pre-1970)
- El libro puede ser de edición limitada/local
- Puede haber error en el código de barras impreso

**Qué hace la app:**
1. Intenta 3 fuentes diferentes
2. Si todas fallan, crea registro con ISBN
3. Puedes completar manualmente otros campos

### Problema: Detecta ISBN incorrecto

**Solución:**
- Espera 3 segundos (debounce)
- Vuelve a escanear con mejor ángulo
- Si persiste, ingresa ISBN manualmente

## 📈 Monitoreando el Proceso

### Ver Logs de Búsqueda

```bash
# En terminal/PowerShell con dispositivo conectado
adb logcat | Select-String "OpenLibraryService"

# Verás mensajes como:
# D/OpenLibraryService: Buscando ISBN 9780140328721 en OpenLibrary API...
# D/OpenLibraryService: ✓ Encontrado en OpenLibrary API
```

### Logs Típicos

```
✓ Éxito en primera fuente:
D/OpenLibraryService: ✓ Encontrado en OpenLibrary API

✓ Éxito en segunda fuente:
D/OpenLibraryService: OpenLibrary API: respuesta no exitosa (404)
D/OpenLibraryService: ✓ Encontrado en OpenLibrary directo

✗ No encontrado:
D/OpenLibraryService: ✗ No se encontró el ISBN xxx en ninguna fuente
```

## 💡 Consejos y Trucos

### Mejores Prácticas

1. **Iluminación:** Natural o blanca es mejor que amarilla
2. **Estabilidad:** Apoya el teléfono en algo si tiemblan las manos
3. **Limpieza:** Códigos de barras sucios/dañados son difíciles de leer
4. **Paciencia:** Espera 1-2 segundos en cada posición

### Uso Eficiente

```
📚 Catalogando múltiples libros:

Para cada libro:
1. Escanear ISBN (1 seg)
2. Verificar datos (2 seg)
3. Guardar (1 seg)
= ~4 segundos por libro

¡Puedes catalogar 15 libros por minuto!
```

### Datos Híbridos (Recomendado)

```
Método combinado para mejor calidad:

1. Escanear portada (título, autor visible)
2. Escanear ISBN de contraportada
3. La app combina TODO:
   - Título y autor del OCR
   - ISBN del código de barras
   - Clasificaciones de APIs
```

## 🎓 Entendiendo las Clasificaciones

### LC (Library of Congress)

```
Ejemplo: PS3537.T3234 O4

P = Literatura
PS = Literatura americana
3537 = Número de autor (Steinbeck)
.T3234 = Código de obra
O4 = Edición específica
```

### Dewey Decimal

```
Ejemplo: 813.52

8 = Literatura
81 = Literatura americana
813 = Ficción americana
813.5 = Siglo XX
813.52 = Década 1920-1929
```

### DCU (Clasificación Decimal Universal)

```
Ejemplo: 821.111(73)-31

821 = Poesía
.111 = Lengua inglesa
(73) = Estados Unidos
-31 = Novela
```

## 📞 Soporte

### Reportar Problemas

Si encuentras ISBNs que no se detectan:

1. Anota el ISBN completo
2. Toma captura del código de barras
3. Verifica logs con `adb logcat`
4. Reporta con esa información

### Mejoras Futuras Planeadas

- [ ] Caché local de ISBNs buscados
- [ ] Modo offline con base de datos
- [ ] Estadísticas de fuentes más exitosas
- [ ] Zoom automático para mejor enfoque
- [ ] Soporte para códigos QR de libros

---

**Versión:** 1.0  
**Fecha:** 2026-02-02  
**Estado:** ✅ Producción
