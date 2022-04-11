package de.fklim.tenki.util

import java.text.SimpleDateFormat
import java.util.*

object MetricMgt {

     fun getTemperatureUnit(locale: String) : String {
        // returns the appropriate temperature suffix
        var value = "°C"
        if (locale == "US" || locale == "RM" || locale == "MM") {
            value = "°F"
        }
        return value
    }

    fun unixTime(timex: Long) : String? {
        val date = Date(timex*1000L)
        val sdf = SimpleDateFormat("HH:mm")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
}