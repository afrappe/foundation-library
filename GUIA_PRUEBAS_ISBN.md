# 🧪 Guía de Pruebas - Sistema Ampliado de Búsqueda ISBN

## 🚀 Preparación

### 1. Compilar e Instalar
```powershell
cd C:\git\foundation-library
.\gradlew clean installDebug
```

### 2. Conectar Dispositivo
```powershell
adb devices
# Debe mostrar tu dispositivo
```

### 3. Habilitar Logs
```powershell
# En terminal separado:
adb logcat -c  # Limpiar logs
adb logcat | Select-String "ISBNBookSearch|OpenLibraryService"
```

---

## 📚 ISBNs de Prueba

### ✅ Casos de Éxito Esperados

#### 1. Libro Clásico Popular
```
ISBN: 9780140328721
Título: Of Mice and Men
Autor: John Steinbeck
Fuente esperada: OpenLibrary API (1er intento)
Tiempo esperado: <1 segundo
Clasificaciones: LC + Dewey + DCU
```

#### 2. Libro Técnico
```
ISBN: 9780596520687
Título: JavaScript: The Good Parts
Autor: Douglas Crockford
Fuente esperada: Google Books (3-4 intento)
Tiempo esperado: 1-2 segundos
Clasificaciones: Enriquecidas después
```

#### 3. Bestseller Reciente
```
ISBN: 9780735219090
Título: Where the Crawdads Sing
Autor: Delia Owens
Fuente esperada: OpenLibrary o Google Books
Tiempo esperado: <1 segundo
```

#### 4. Libro Académico
```
ISBN: 9780134685991
Título: Effective Java
Autor: Joshua Bloch
Fuente esperada: OpenLibrary Search
Clasificaciones: Dewey presente
```

#### 5. ISBN-10 (conversión automática)
```
ISBN: 0596520689
Conversión: 9780596520687
Título: JavaScript: The Good Parts
Prueba: Normalización ISBN-10→13
```

---

## 🧪 Plan de Pruebas Sistemático

### Prueba 1: Libro Popular ✅
**Objetivo:** Verificar que OpenLibrary API funciona

```
Pasos:
1. Abrir app
2. Seleccionar "Escanear ISBN"
3. Escanear o ingresar: 9780140328721
4. Esperar resultado

Resultado Esperado:
✓ Título: "Of Mice and Men"
✓ Autor: "John Steinbeck"
✓ Editorial presente
✓ Año: 1993 o similar
✓ LC: PS3537...
✓ Dewey: 813.52
✓ Tiempo: <1 seg

Log Esperado:
D/ISBNBookSearch: Buscando libro con ISBN: 9780140328721
D/ISBNBookSearch: 1/4 Intentando OpenLibrary API...
D/ISBNBookSearch: ✓ Libro encontrado en OpenLibrary API
```

---

### Prueba 2: Fallback a Google Books ✅
**Objetivo:** Verificar cascada de fuentes

```
Pasos:
1. Usar ISBN que no esté en OpenLibrary
2. Ejemplo: 9781234567890 (ficticio)
3. Observar logs

Resultado Esperado:
✓ Intenta OpenLibrary API (falla)
✓ Intenta OpenLibrary Direct (falla)
✓ Intenta OpenLibrary Search (falla)
✓ Intenta Google Books (éxito o falla)
✓ Si todo falla: Crea registro básico con ISBN

Log Esperado:
D/ISBNBookSearch: 1/4 Intentando OpenLibrary API...
D/ISBNBookSearch: OpenLibrary API: respuesta no exitosa (404)
D/ISBNBookSearch: 2/4 Intentando OpenLibrary directo...
D/ISBNBookSearch: OpenLibrary directo: respuesta no exitosa (404)
...
```

---

### Prueba 3: Conversión ISBN-10 ✅
**Objetivo:** Verificar normalización

```
Pasos:
1. Ingresar ISBN-10: 0596520689
2. Ver logs

Resultado Esperado:
✓ Conversión a: 9780596520687
✓ Búsqueda con ISBN-13
✓ Libro encontrado

Log Esperado:
D/ISBNBookSearch: Buscando libro con ISBN: 9780596520687
(Nota: Ya debe estar convertido)
```

---

### Prueba 4: Enriquecimiento de Clasificaciones ✅
**Objetivo:** Verificar que se buscan clasificaciones adicionales

```
Pasos:
1. Buscar libro que Google Books encuentre (sin clasificaciones)
2. Verificar que después busca clasificaciones

Resultado Esperado:
✓ Libro encontrado en Google Books
✓ Se ejecuta OpenLibraryService.fetchClassifications()
✓ Se agregan LC/Dewey si están disponibles

Log Esperado:
D/ISBNBookSearch: ✓ Libro encontrado en Google Books
D/OpenLibraryService: Buscando ISBN xxx en OpenLibrary API...
```

---

### Prueba 5: Libro No Encontrado ⚠️
**Objetivo:** Verificar manejo de ISBN inexistente

```
Pasos:
1. Ingresar ISBN inexistente: 9999999999999
2. Esperar resultados

Resultado Esperado:
✓ Intenta todas las fuentes (4 + clasificaciones)
✓ Crea registro básico con ISBN
✓ Muestra mensaje apropiado
✓ No crash

Log Esperado:
D/ISBNBookSearch: ✗ No se encontró el libro con ISBN xxx en ninguna fuente
D/OpenLibraryService: Buscando ISBN xxx...
(Intenta clasificaciones también)
```

---

### Prueba 6: Conectividad ⚠️
**Objetivo:** Verificar comportamiento sin internet

```
Pasos:
1. Desactivar WiFi y datos móviles
2. Intentar buscar ISBN
3. Reactivar conexión

Resultado Esperado:
✓ Muestra error de conexión apropiado
✓ No crash
✓ Al reactivar internet, permite reintentar
```

---

### Prueba 7: Escaneo de Código de Barras 📷
**Objetivo:** Verificar integración completa

```
Pasos:
1. Seleccionar "Escanear ISBN"
2. Apuntar a código de barras real
3. Esperar detección automática

Resultado Esperado:
✓ BarcodeAnalyzer detecta ISBN (<1 seg)
✓ ISBNBookSearchService busca automáticamente
✓ Muestra información completa
✓ Clasificaciones presentes
```

---

### Prueba 8: Preservación de Datos 🔄
**Objetivo:** Verificar que no se borran datos existentes

```
Pasos:
1. Escanear sección de título manualmente
2. Luego escanear ISBN
3. Verificar que título no se pierde

Resultado Esperado:
✓ Título escaneado previamente se mantiene
✓ ISBN agrega información adicional
✓ Clasificaciones se agregan
✓ Datos se combinan correctamente
```

---

## 📊 Matriz de Pruebas

| # | Prueba | ISBN | Fuente Esperada | Éxito Esperado | Prioridad |
|---|--------|------|-----------------|----------------|-----------|
| 1 | Libro popular | 9780140328721 | OpenLibrary API | ✅ 95% | Alta |
| 2 | Libro técnico | 9780596520687 | Google Books | ✅ 90% | Alta |
| 3 | ISBN-10 | 0596520689 | Conversión OK | ✅ 95% | Media |
| 4 | Bestseller | 9780735219090 | OpenLib/Google | ✅ 90% | Media |
| 5 | Académico | 9780134685991 | OpenLib Search | ✅ 85% | Media |
| 6 | Inexistente | 9999999999999 | Ninguna | ⚠️ Básico | Baja |
| 7 | Sin internet | Cualquiera | Error | ⚠️ Error | Alta |
| 8 | Preservación | Cualquiera | N/A | ✅ 100% | Alta |

---

## 🔍 Verificación de Logs

### ✅ Log Exitoso (Óptimo)
```
D/ISBNBookSearch: Buscando libro con ISBN: 9780140328721
D/ISBNBookSearch: 1/4 Intentando OpenLibrary API...
D/ISBNBookSearch: ✓ Libro encontrado en OpenLibrary API
D/OpenLibraryService: Buscando ISBN 9780140328721 en OpenLibrary API...
D/OpenLibraryService: ✓ Encontrado en OpenLibrary API

Tiempo total: ~0.5-1.0 segundos
```

### ✅ Log con Fallback (Aceptable)
```
D/ISBNBookSearch: Buscando libro con ISBN: 9781234567890
D/ISBNBookSearch: 1/4 Intentando OpenLibrary API...
D/ISBNBookSearch: OpenLibrary API: respuesta no exitosa (404)
D/ISBNBookSearch: 2/4 Intentando OpenLibrary directo...
D/ISBNBookSearch: OpenLibrary directo: respuesta no exitosa (404)
D/ISBNBookSearch: 3/4 Intentando OpenLibrary Search...
D/ISBNBookSearch: OpenLibrary Search: no hay documentos
D/ISBNBookSearch: 4/4 Intentando Google Books...
D/ISBNBookSearch: ✓ Libro encontrado en Google Books

Tiempo total: ~1.5-2.0 segundos
```

### ⚠️ Log Sin Resultados (Edge Case)
```
D/ISBNBookSearch: Buscando libro con ISBN: 9999999999999
D/ISBNBookSearch: 1/4 Intentando OpenLibrary API...
D/ISBNBookSearch: OpenLibrary API: respuesta no exitosa (404)
...
D/ISBNBookSearch: ✗ No se encontró el libro con ISBN xxx en ninguna fuente
D/OpenLibraryService: Buscando ISBN xxx en OpenLibrary API...
D/OpenLibraryService: OpenLibrary API: respuesta no exitosa (404)
...
D/OpenLibraryService: ✗ No se encontró el ISBN xxx en ninguna de las 6 fuentes

Resultado: Registro básico con ISBN creado
```

---

## 📈 Métricas a Recolectar

### Durante las Pruebas
```
Para cada prueba, registrar:
- ✅ ISBN probado
- ✅ Fuente que respondió (1ª, 2ª, 3ª, 4ª)
- ✅ Tiempo de respuesta
- ✅ Datos obtenidos (título, autor, clasificaciones)
- ✅ Errores/warnings
```

### Ejemplo de Registro
```
Prueba #1:
ISBN: 9780140328721
Fuente: OpenLibrary API (1er intento)
Tiempo: 0.8 seg
Datos: ✓ Título ✓ Autor ✓ LC ✓ Dewey ✓ DCU
Status: ✅ ÉXITO COMPLETO
```

---

## 🎯 Criterios de Éxito

### Éxito Total ✅
- Mínimo 7/8 pruebas exitosas
- Tasa de éxito con ISBNs reales: >80%
- Tiempo promedio: <2 segundos
- Sin crashes
- Clasificaciones en >70% de libros

### Éxito Parcial ⚠️
- 5-6/8 pruebas exitosas
- Tasa de éxito: 60-80%
- Tiempo promedio: <3 segundos
- Crashes ocasionales en edge cases

### Fallo ❌
- <5/8 pruebas exitosas
- Tasa de éxito: <60%
- Crashes frecuentes
- Tiempo >5 segundos

---

## 🔧 Troubleshooting

### Problema: No encuentra libros conocidos
```
Solución:
1. Verificar conexión a internet
2. Probar URLs manualmente en navegador:
   https://openlibrary.org/isbn/9780140328721.json
3. Revisar logs para ver qué fuente falla
4. Verificar que ISBN sea válido
```

### Problema: Muy lento
```
Solución:
1. Verificar velocidad de internet
2. Revisar cuántas fuentes está intentando
3. Considerar cache local
4. Optimizar con búsqueda paralela
```

### Problema: Clasificaciones vacías
```
Solución:
1. Normal para algunos libros (especialmente modernos/comerciales)
2. Google Books no provee clasificaciones bibliotecarias
3. Libros muy nuevos pueden no estar catalogados
```

---

## 📞 Reportar Problemas

### Información a Incluir
```
1. ISBN probado: _______________
2. Tipo de búsqueda: [Escaneo/Manual]
3. Resultado obtenido: [Éxito/Parcial/Fallo]
4. Logs relevantes: (copiar de logcat)
5. Tiempo transcurrido: ___ segundos
6. Datos faltantes: [Título/Autor/Clasificaciones/etc]
```

### Ejemplo de Reporte
```
ISBN: 9781234567890
Tipo: Escaneo de código de barras
Resultado: Fallo (libro no encontrado)
Logs: 
  D/ISBNBookSearch: ✗ No se encontró el libro
Tiempo: 2.5 seg
Verificación manual: 
  - OpenLibrary: No existe ✓
  - Google Books: No existe ✓
Conclusión: ISBN inválido o libro muy raro
```

---

## ✅ Checklist de Pruebas

```
Antes de dar por completado:

[ ] Prueba 1: Libro popular - PASÓ
[ ] Prueba 2: Fallback - PASÓ
[ ] Prueba 3: ISBN-10 - PASÓ
[ ] Prueba 4: Bestseller - PASÓ
[ ] Prueba 5: Académico - PASÓ
[ ] Prueba 6: No encontrado - MANEJO CORRECTO
[ ] Prueba 7: Escaneo barcode - PASÓ
[ ] Prueba 8: Preservación - PASÓ

[ ] Sin crashes
[ ] Logs correctos
[ ] Tiempos aceptables (<3 seg promedio)
[ ] Tasa de éxito >80%
```

---

## 🎉 Resultado Esperado

Con 7 fuentes de búsqueda, se espera:

```
✅ 85-90% de libros encontrados
✅ Información completa en 70-80%
✅ Clasificaciones en 60-70%
✅ Tiempo promedio: 1-2 segundos
✅ Manejo robusto de errores
```

---

*Fecha: 2026-02-02*  
*Versión: Sistema ampliado (7 fuentes)*  
*Objetivo: Validar tasa de éxito >85%*
