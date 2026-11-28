package com.example.sigep.model

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:3000/"

    /**
     * Se ejecuta cuando el servidor rechaza (401) una peticion que llevaba token:
     * la sesion vencio o la cuenta fue desactivada. Lo asigna SesionController.
     */
    @Volatile
    var alSesionExpirada: (() -> Unit)? = null

    private val cliente: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { cadena ->
                val token = AlmacenSesion.leer()?.token
                val peticion = if (token != null) {
                    cadena.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    cadena.request()
                }
                val respuesta = cadena.proceed(peticion)
                if (token != null && respuesta.code == 401) {
                    alSesionExpirada?.invoke()
                }
                respuesta
            })
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }

    val usuariosApi: UsuariosApi by lazy { retrofit.create(UsuariosApi::class.java) }

    val alberguesApi: AlberguesApi by lazy { retrofit.create(AlberguesApi::class.java) }

    val pacientesApi: PacientesApi by lazy { retrofit.create(PacientesApi::class.java) }

    val triageApi: TriageApi by lazy { retrofit.create(TriageApi::class.java) }
}
