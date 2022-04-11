package com.weatherapp.models

import de.fklim.tenki.models.Clouds
import de.fklim.tenki.models.Coord
import java.io.Serializable

data class WeatherResponse(
    val coord: Coord,
    val weather: List<Weather>,
    val base: String,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val clouds: Clouds,
    val dt: Int,
    val sys: Sys,
    val id: Int,
    val name: String,
    val cod: Int
) : Serializable