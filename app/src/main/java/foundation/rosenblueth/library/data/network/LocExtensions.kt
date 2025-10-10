package foundation.rosenblueth.library.data.network

import foundation.rosenblueth.library.data.model.BookModel

/**
 * Extensiones para convertir la respuesta de la API de la Biblioteca del Congreso a nuestros modelos
 */

/**
 * Convierte un elemento de la respuesta de LOC a un modelo de libro
 */
fun LocItem.toBookModel(): BookModel {
    val authorName = contributors.firstOrNull() ?: ""
    val yearString = date.split("-").firstOrNull() ?: ""
    val year = yearString.toIntOrNull()
    val isbnValue = isbn.firstOrNull() ?: ""
    val publisherValue = publisher.firstOrNull() ?: ""
    val languageValue = language.firstOrNull() ?: ""
    val descriptionValue = description.joinToString("\n").take(500)

    return BookModel(
        title = title,
        author = authorName,
        isbn = isbnValue,
        publisher = publisherValue,
        publishedYear = year,
        subjects = subjects,
        language = languageValue,
        description = descriptionValue
    )
}
