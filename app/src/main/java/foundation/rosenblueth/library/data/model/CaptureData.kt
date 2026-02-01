package foundation.rosenblueth.library.data.model

import kotlinx.serialization.Serializable

/**
 * Modelo de datos serializable que representa un libro capturado en la biblioteca
 */
@Serializable
data class CaptureData(
    val id: String,
    val title: String,
    val author: String = "",
    val isbn: String = "",
    val publisher: String = "",
    val publishedYear: Int? = null,
    val pages: Int? = null,
    val language: String = "",
    val description: String = "",
    val lcClassification: String = "",  // Library of Congress Classification
    val deweyClassification: String = "",  // Dewey Decimal Classification
    val dcuClassification: String = "",  // DCU/UDC Classification (Universal Decimal Classification, based on the Brussels Classification)
    val captureTimestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Crea un CaptureData desde un BookModel
         */
        fun fromBookModel(book: BookModel): CaptureData {
            return CaptureData(
                id = "${System.currentTimeMillis()}_${book.isbn.ifEmpty { book.title.hashCode() }}",
                title = book.title,
                author = book.author,
                isbn = book.isbn,
                publisher = book.publisher,
                publishedYear = book.publishedYear,
                pages = book.pages,
                language = book.language,
                description = book.description,
                lcClassification = book.lcClassification,
                deweyClassification = book.deweyClassification,
                dcuClassification = book.dcuClassification
            )
        }
    }
}
