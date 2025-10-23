# foundation-library

## Medicine Scanner Application

Esta aplicación permite escanear empaques de medicamentos utilizando OCR (Reconocimiento Óptico de Caracteres) para extraer información farmacéutica relevante.

### Características

- **Escaneo de empaques**: Captura imágenes de empaques de medicamentos usando la cámara del dispositivo
- **Reconocimiento OCR**: Extrae automáticamente el nombre del medicamento del empaque
- **Búsqueda de información**: Busca información detallada del medicamento en bases de datos farmacéuticas
- **Información completa**: Muestra datos como:
  - Nombre del medicamento
  - Principio activo
  - Fabricante
  - Dosificación
  - Forma farmacéutica
  - Indicaciones terapéuticas
  - Contraindicaciones
  - Efectos secundarios
  - Número de registro sanitario

### Arquitectura

La aplicación está construida con arquitectura MVVM (Model-View-ViewModel) y utiliza:

- **Kotlin**: Lenguaje de programación principal
- **Jetpack Compose**: Framework de UI moderno
- **ML Kit**: Para reconocimiento de texto en imágenes
- **Retrofit**: Cliente HTTP para consumir APIs
- **Coroutines**: Para operaciones asíncronas
- **ViewModel & StateFlow**: Gestión de estado

### Estructura del Proyecto

```
app/src/main/java/foundation/rosenblueth/library/
├── data/
│   ├── model/          # Modelos de datos (MedicineModel)
│   ├── network/        # Configuración de red y APIs
│   └── repository/     # Repositorios (MedicineRepository)
├── ui/
│   ├── screens/        # Pantallas de la aplicación
│   ├── components/     # Componentes reutilizables de UI
│   └── viewmodel/      # ViewModels (MedicineScannerViewModel)
└── util/               # Utilidades (TextRecognitionHelper)
```

### Componentes Principales

#### MedicineModel
Modelo de datos que representa la información de un medicamento con todos sus campos relevantes.

#### MedicineRepository
Gestiona las operaciones relacionadas con medicamentos:
- `searchMedicineByName()`: Busca información del medicamento por nombre
- `sendMedicineToBackend()`: Envía datos al servidor

#### MedicineScannerViewModel
Coordina la lógica de negocio:
- `processMedicinePackage()`: Procesa la imagen capturada
- `updateMedicineName()`: Actualiza el nombre del medicamento
- `sendMedicineToBackend()`: Envía información al backend

#### TextRecognitionHelper
Proporciona funcionalidades de OCR:
- `recognizeText()`: Extrae texto de imágenes
- `extractMedicineName()`: Identifica el nombre del medicamento en el texto reconocido

### Pruebas

El proyecto incluye tests unitarios completos:
- `MedicineRepositoryTest`: Pruebas del repositorio
- `MedicineScannerViewModelTest`: Pruebas del ViewModel
- `TextRecognitionHelperTest`: Pruebas del helper de OCR

### Configuración

1. Asegúrate de tener Android Studio instalado
2. Clona el repositorio
3. Abre el proyecto en Android Studio
4. Sincroniza las dependencias de Gradle
5. Ejecuta la aplicación en un dispositivo o emulador

### Permisos Requeridos

- **CAMERA**: Para capturar imágenes de empaques de medicamentos

### Notas de Conversión

Este proyecto fue originalmente una aplicación de escaneo de libros y ha sido convertido para escanear medicamentos. Los cambios principales incluyen:

- BookModel → MedicineModel
- BookRepository → MedicineRepository  
- BookScannerViewModel → MedicineScannerViewModel
- extractBookTitle() → extractMedicineName()
- API de Library of Congress → API farmacéutica (FDA)

Todos los tests y componentes de UI han sido actualizados para reflejar la nueva funcionalidad.