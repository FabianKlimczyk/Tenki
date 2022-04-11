package de.fklim.tenki.network

import com.weatherapp.network.WeatherService
import de.fklim.tenki.util.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    val api: WeatherService by lazy {
        Retrofit.Builder()
            // API URL.
            .baseUrl(Constants.BASE_URL)
            /**
             * Converter to de- and encode JSON-Data
             */
            .addConverterFactory(GsonConverterFactory.create())
            /** Create Retrofit Instance */
            .build()
            .create(WeatherService::class.java)
    }
}