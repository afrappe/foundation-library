# 🔍 Debugging - Problema con Título del ISBN

## Problema Identificado
**"Al reconocer el ISBN no está trayendo el título de la obra"**

## ✅ Mejoras Implementadas

### 1. **Logging Detallado Agregado**

Se agregó logging extensivo en todas las funciones de parsing para identificar dónde se pierde el título:

#### ISBNBookSearchService:
- `parseOpenLibraryApiData()` - Logs para ver datos JSON recibidos y título extraído
- `parseOpenLibraryDirectData()` - Logs para endpoint directo
- `parseGoogleBooksData()` - Logs para Google Books
- `searchBookByISBN()` - Logs del flujo principal

#### BookScannerViewModel:
- `searchBookByISBN()` - Logs cuando se encuentra libro
- `debugSearchISBN()` - Nueva función para testing directo

### 2. **Corrección del UI State**

Se actualizó el ViewModel para asegurar que `bookTitle` se actualice correctamente:

```kotlin
_uiState.update {
    it.copy(
        isLoading = false,
        books = listOf(enrichedBook),
        selectedBook = enrichedBook,
        bookTitle = enrichedBook.title  // ← CLAVE: Actualizar título en UI
    )
}
```

### 3. **Función de Debug**

Nueva función para probar ISBN directamente:
```kotlin
viewModel.debugSearchISBN("9780140328721")
```

---

## 🧪 Cómo Debuggear el Problema

### Paso 1: Habilitar Logs
```powershell
adb logcat -c  # Limpiar logs
adb logcat | Select-String "BookScannerVM|ISBNBookSearch" 
```

### Paso 2: Probar con ISBN Conocido
En la app, usar la función de debug con un ISBN conocido:
- **ISBN de prueba:** `9780140328721` (Of Mice and Men)

### Paso 3: Analizar Logs

#### ✅ Log Exitoso Esperado:
```
D/ISBNBookSearch: Buscando libro con ISBN: 9780140328721
D/ISBNBookSearch: 1/4 Intentando OpenLibrary API...
D/ISBNBookSearch: Parseando datos de OpenLibrary API para ISBN: 9780140328721
D/ISBNBookSearch: JSON recibido: {title: "Of Mice and Men", authors: [...], ...}
D/ISBNBookSearch: Título extraído: 'Of Mice and Men'
D/ISBNBookSearch: Autor extraído: 'John Steinbeck'
D/ISBNBookSearch: BookModel creado exitosamente: BookModel(title=Of Mice and Men, ...)
D/ISBNBookSearch: ✓ Libro encontrado en OpenLibrary API
D/BookScannerVM: Libro encontrado: Of Mice and Men
D/BookScannerVM: Clasificaciones iniciales - LC: 'PS3537...', Dewey: '813.52'
```

#### ⚠️ Log con Problema:
```
D/ISBNBookSearch: Título extraído: ''  ← PROBLEMA: título vacío
D/ISBNBookSearch: OpenLibrary API: título vacío - retornando null
```

### Paso 4: Verificar Diferentes Fuentes

Si OpenLibrary falla, el sistema probará:
1. **OpenLibrary Direct** (`/isbn/{isbn}.json`)
2. **OpenLibrary Search** (`/search.json?isbn={isbn}`)
3. **Google Books** (`/volumes?q=isbn:{isbn}`)

#### Logs de Fallback:
```
D/ISBNBookSearch: 1/4 Intentando OpenLibrary API...
D/ISBNBookSearch: OpenLibrary API: título vacío - retornando null
D/ISBNBookSearch: 2/4 Intentando OpenLibrary directo...
D/ISBNBookSearch: Parseando datos de OpenLibrary Direct para ISBN: xxx
D/ISBNBookSearch: Título extraído: 'JavaScript: The Good Parts'  ← Éxito en 2da fuente
```

---

## 🔧 Posibles Causas y Soluciones

### Causa 1: API Response Vacía o Malformada

**Síntomas:**
```
D/ISBNBookSearch: JSON recibido: {}
D/ISBNBookSearch: Título extraído: ''
```

**Solución:**
- Verificar que el ISBN sea válido
- Probar con diferentes ISBNs
- Verificar conectividad a internet

### Causa 2: Estructura JSON Diferente

**Síntomas:**
```
D/ISBNBookSearch: JSON recibido: {obra: {titulo: "Libro"}, ...}
D/ISBNBookSearch: Título extraído: ''  ← No encuentra "title"
```

**Solución:**
- Revisar la estructura JSON real en logs
- Ajustar el parsing si es necesario

### Causa 3: UI State No Se Actualiza

**Síntomas:**
- Logs muestran libro encontrado
- UI sigue mostrando título vacío

**Solución:**
- Verificar que `bookTitle` se actualiza en UI state
- Verificar que la UI observa `uiState.bookTitle`

---

## 📝 ISBNs para Pruebas

### ISBNs Conocidos que Funcionan:
```
9780140328721  → Of Mice and Men (John Steinbeck)
9780596520687  → JavaScript: The Good Parts
9780735219090  → Where the Crawdads Sing
9780134685991  → Effective Java
```

### ISBNs para Probar Fallback:
```
9781234567890  → ISBN ficticio (debe fallar en todas las fuentes)
0596520689     → ISBN-10 (debe convertirse a 13)
```

---

## 🎯 Verificación Final

### Checklist de Debug:

1. **✅ Logs Habilitados**
   ```bash
   adb logcat | Select-String "ISBNBookSearch"
   ```

2. **✅ Probar ISBN Conocido**
   - Usar `9780140328721`
   - Verificar que aparece "Of Mice and Men" en logs

3. **✅ Verificar UI State**
   - Confirmar que `bookTitle` se actualiza
   - Confirmar que UI muestra el título

4. **✅ Probar Múltiples Fuentes**
   - Probar ISBN que solo está en Google Books
   - Verificar que fallback funciona

### Comando de Test Rápido:
```kotlin
// En la app, llamar:
viewModel.debugSearchISBN("9780140328721")

// Luego verificar en logs que aparezca:
// "Título extraído: 'Of Mice and Men'"
```

---

## 💡 Si el Problema Persiste

### Agregar Más Logging:

1. **En la UI:** Verificar qué muestra `uiState.bookTitle`
2. **En el Network:** Verificar respuesta HTTP raw
3. **En el JSON:** Imprimir todo el JSON recibido

### Estructura JSON Real de OpenLibrary:

Verificar manualmente:
```
https://openlibrary.org/api/books?bibkeys=ISBN:9780140328721&jscmd=data&format=json
```

Debería retornar algo como:
```json
{
  "ISBN:9780140328721": {
    "title": "Of Mice and Men",
    "authors": [{"name": "John Steinbeck"}],
    "publishers": [{"name": "Penguin Books"}],
    "classifications": {
      "lc_classifications": ["PS3537.T3234"],
      "dewey_decimal_class": ["813.52"]
    }
  }
}
```

---

## 🎉 Resultado Esperado

Con estas mejoras, deberías ver:

1. **Logs detallados** mostrando cada paso del proceso
2. **Título extraído correctamente** de las APIs
3. **UI actualizada** con el título del libro
4. **Clasificaciones mostradas** en "Mi Biblioteca"

Si el problema persiste después de estas mejoras, los logs te mostrarán exactamente dónde está fallando el proceso.
