package foundation.rosenblueth.library.data.network

import com.google.gson.annotations.SerializedName

/**
 * Clase para mapear la respuesta de la API de la Biblioteca del Congreso (loc.gov)
 */
data class LocResponse(
    val items: List<LocItem> = listOf(),
    val pagination: LocPagination = LocPagination(),
    @SerializedName("search")
    val searchInfo: LocSearchInfo = LocSearchInfo()
)

data class LocItem(
    val title: String = "",
    val contributors: List<String> = listOf(),
    val description: List<String> = listOf(),
    val date: String = "",
    val publisher: List<String> = listOf(),
    val language: List<String> = listOf(),
    val subjects: List<String> = listOf(),
    @SerializedName("location")
    val locationUrl: String = "",
    @SerializedName("id")
    val locId: String = "",
    val isbn: List<String> = listOf()
)

data class LocPagination(
    val current: Int = 1,
    val next: Int = 0,
    val previous: Int = 0,
    val total: Int = 0
)

data class LocSearchInfo(
    val query: String = "",
    val totalItems: Int = 0
)
