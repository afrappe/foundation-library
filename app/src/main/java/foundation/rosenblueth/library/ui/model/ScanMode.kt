package foundation.rosenblueth.library.ui.model

/**
 * Modos de escaneo disponibles para identificar libros
 */
enum class ScanMode {
    SECTIONS,  // Escaneo por secciones (título, autor, editorial)
    ISBN,      // Escaneo de código ISBN
    COVER      // Escaneo de portada completa
}
