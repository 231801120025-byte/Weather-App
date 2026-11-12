package com.example.practical.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.practical.R
import com.example.practical.model.DailyForecast
import com.example.practical.utils.WeatherUtils

class ForecastAdapter(
    private var forecasts: List<DailyForecast> = emptyList(),
    private var isCelsius: Boolean = true
) : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    fun updateData(newForecasts: List<DailyForecast>, isCelsiusUnit: Boolean) {
        forecasts = newForecasts
        isCelsius = isCelsiusUnit
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_forecast, parent, false)
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        holder.bind(forecasts[position], isCelsius)
    }

    override fun getItemCount(): Int = forecasts.size

    class ForecastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvEmoji: TextView = itemView.findViewById(R.id.tvEmoji)
        private val tvCondition: TextView = itemView.findViewById(R.id.tvCondition)
        private val tvMaxTemp: TextView = itemView.findViewById(R.id.tvMaxTemp)
        private val tvMinTemp: TextView = itemView.findViewById(R.id.tvMinTemp)

        fun bind(forecast: DailyForecast, isCelsius: Boolean) {
            tvDate.text = WeatherUtils.formatDate(forecast.date)
            tvEmoji.text = WeatherUtils.getWeatherEmoji(forecast.weatherCode)
            tvCondition.text = WeatherUtils.getWeatherDescription(forecast.weatherCode)
            tvMaxTemp.text = WeatherUtils.formatTemperature(forecast.maxTempC, isCelsius)
            tvMinTemp.text = WeatherUtils.formatTemperature(forecast.minTempC, isCelsius)
        }
    }
}
