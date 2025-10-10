package foundation.rosenblueth.library.data.network

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Proporciona una instancia configurada de Retrofit para conexiones a APIs.
 */
object RetrofitClient {
    // URLs de las APIs disponibles
    private const val LOC_BASE_URL = "https://www.loc.gov/"

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Este método podría ser extendido para manejar múltiples endpoints o autenticación
    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(LOC_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // Servicio para buscar libros en la Biblioteca del Congreso (loc.gov)
    val bookApiService: BookCatalogApiService by lazy {
        buildRetrofit().create(BookCatalogApiService::class.java)
    }
}
