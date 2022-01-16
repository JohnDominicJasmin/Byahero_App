package com.example.commutingapp.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.commutingapp.BuildConfig
import com.example.commutingapp.data.model.Weather
import com.example.commutingapp.data.model.WeatherServiceAPI
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import im.delight.android.location.SimpleLocation
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap


@HiltViewModel
class WeatherViewModel @Inject constructor(
     private val request: WeatherServiceAPI
) : ViewModel() {
    private var map: HashMap<String, String> = HashMap()
    private val currentWeather = MutableLiveData<Weather>()
    private val jsonApi = MutableLiveData<String>()
    fun getWeatherJson(): LiveData<String> = jsonApi
    fun getCurrentWeather(): LiveData<Weather> = currentWeather


    init {
        map.apply {
            this["key"] = BuildConfig.WEATHER_API_KEY
            this["days"] = "1"
            this["aqi"] = "yes"
            this["alerts"] = "yes"
        }

    }

    private fun requestWeather(cityName: String) {
        map["q"] = cityName
        val call = request.getWeatherData(map)
        call.enqueue(weatherCallback())
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(context: Context) {

        val location = SimpleLocation(context)
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
             geocoder.getFromLocation(location.latitude, location.longitude, 1).forEach { address ->
                val city = "${address.locality} ${address.thoroughfare}"
                requestWeather(city)
                return
             }
            throw RuntimeException("Location not found")
        } catch (e: IOException) {
           throw RuntimeException("No Internet Connection")
        }
    }

    private fun weatherCallback() = object : Callback<Weather> {
        override fun onResponse(call: Call<Weather>, response: Response<Weather>) {

            if(!response.isSuccessful){
                Timber.e("Response not success: ${response.code()}")
                return
            }
            jsonApi.value = Gson().toJson(response.body())
            currentWeather.value = response.body()

        }

        override fun onFailure(call: Call<Weather>, t: Throwable) {
            throw RuntimeException("An unexpected error occurred")
        }
    }
}