package com.safeshade

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 1. Data Model (Expanded for UV and Probability)
data class WeatherResponse(
    val current: CurrentWeather,
    val hourly: HourlyWeather
)

data class CurrentWeather(
    val temperature_2m: Float,
    val weather_code: Int
)

data class HourlyWeather(
    val precipitation_probability: List<Int>,
    val uv_index: List<Float>,
    val relative_humidity_2m: List<Int>
)

// 2. API Interface
interface OpenMeteoApi {
    @GET("v1/forecast?current=temperature_2m,weather_code&hourly=precipitation_probability,uv_index,relative_humidity_2m&forecast_days=1")
    suspend fun getWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double
    ): WeatherResponse
}

// 3. Singleton
object WeatherService {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: OpenMeteoApi = retrofit.create(OpenMeteoApi::class.java)
}