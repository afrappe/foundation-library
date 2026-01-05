package foundation.rosenblueth.library.data.model

/**
 * Modelo de datos que representa la información de un libro.
 */
data class BookModel(
    val title: String,
    val author: String = "",
    val isbn: String = "",
    val publisher: String = "",
    val publishedYear: Int? = null,
    val pages: Int? = null,
    val subjects: List<String> = emptyList(),
    val language: String = "",
    val description: String = "",
    val coverImageUrl: String = "",
    val lcClassification: String = "",  // Library of Congress Classification
    val deweyClassification: String = "",  // Dewey Decimal Classification
    val dcuClassification: String = ""  // DCU Classification
) {
    companion object {
        /**
         * Crea un modelo inicial solo con título,
         * útil cuando el reconocimiento de texto solo identificó el título
         */
        fun createWithTitle(title: String): BookModel {
            return BookModel(title = title)
        }
    }
}
