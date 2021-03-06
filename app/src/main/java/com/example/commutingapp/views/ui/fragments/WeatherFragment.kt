package com.example.commutingapp.views.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.commutingapp.R
import com.example.commutingapp.data.model.WeatherDto
import com.example.commutingapp.databinding.FragmentWeatherBinding
import com.example.commutingapp.utils.InternetConnection.Connection
import com.example.commutingapp.utils.others.Constants.KEY_USER_CITY_JSON_WEATHER
import com.example.commutingapp.utils.others.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.example.commutingapp.utils.others.Constants.REQUEST_USER_LOCATION
import com.example.commutingapp.utils.others.LastLocation
import com.example.commutingapp.utils.others.TrackingPermissionUtility.hasLocationPermission
import com.example.commutingapp.utils.others.TrackingPermissionUtility.requestLocationPermission
import com.example.commutingapp.viewmodels.WeatherViewModel
import com.example.commutingapp.views.adapters.WeatherAdapter
import com.example.commutingapp.views.ui.recycler_view_model.WeatherRVModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions


@AndroidEntryPoint
class WeatherFragment: Fragment(R.layout.fragment_weather), EasyPermissions.PermissionCallbacks{
    private var binding: FragmentWeatherBinding? = null
    private var listOfWeatherModel:MutableList<WeatherRVModel> = ArrayList()
    private val weatherViewModel: WeatherViewModel by viewModels()
    private lateinit var weatherApiJson :SharedPreferences
    override fun onAttach(context: Context) {
        super.onAttach(context)
        weatherApiJson = context.getSharedPreferences(KEY_USER_CITY_JSON_WEATHER,Context.MODE_PRIVATE)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentWeatherBinding.inflate(inflater,container,false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        provideViewModelObservers()
        provideClickListener()
        checkGps()
        checkPermission()



    }
    private fun checkPermission(){
        if(!Connection.hasInternetConnection(requireContext())){
            renderDefaultData()
            return
        }
        checkLocationPermission()
    }
    private fun provideClickListener(){
        binding!!.imgBtnRefresh.setOnClickListener {
            getUserCityLocation()
        }
    }
    private fun checkLocationPermission(){
        if(hasLocationPermission(requireContext())){
            getUserCityLocation()
            return
        }
        requestLocationPermission(this)

    }
    private fun renderDefaultData(){

            binding!!.circularProgressBar.visibility = View.INVISIBLE
            val jsonObject:WeatherDto? = Gson().fromJson(getJsonSharedPreference(),WeatherDto::class.java)
            if(jsonObject!=null){
                renderData(jsonObject)
                return
            }
            renderDefaultRecyclerViewsData()

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun renderDefaultRecyclerViewsData(){
        for(i in 1..12){
            listOfWeatherModel.add(
                WeatherRVModel(
                time = "00:00 am",
                icon = null,
                temperature = "00.0??c",
                windSpeed = "---/h --")
            )
        }
        binding!!.recyclerViewDisplay.adapter?.notifyDataSetChanged()
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun provideViewModelObservers(){

        weatherViewModel.getWeatherJson().observe(viewLifecycleOwner){
            updateJsonSharedPreference(it)
            binding!!.circularProgressBar.visibility = View.INVISIBLE

        }
        weatherViewModel.getCurrentWeather().observe(viewLifecycleOwner){
            renderData(it)
            binding!!.circularProgressBar.visibility = View.INVISIBLE
        }
        weatherViewModel.getExceptions().observe(viewLifecycleOwner){ message->
            Toast.makeText(requireContext(),message,Toast.LENGTH_SHORT).show()
            binding!!.circularProgressBar.visibility = View.INVISIBLE
        }


    }

    override fun onRequestPermissionsResult( requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun renderData(weatherInfo:WeatherDto) {
        with(binding!!) {
            with(weatherInfo) {
                with(current) {
                    if (listOfWeatherModel.isNotEmpty()) {
                        listOfWeatherModel.clear()

                        textViewCityName.text = "${location.region}, ${location.country}"
                        textViewTemperature.text = "${tempC}??c"
                        textViewWeatherCondition.text = condition.text
                        textViewCloud.text = "   Cloud: ${cloud}%"
                        textViewHumidity.text = "   Humidity: ${humidity}%"
                        textViewInformation.text = "${tempC}??c | Feels like ${feelslikeC}??c"

                        Glide.with(requireActivity()).load("http:" + (condition.icon))
                            .into(imageViewWeatherCondition)
                        textViewWindSpeed.text = "   Wind: ${windKph}km/h"

                        if (isDay == 1) showDay() else showNight()

                    }


                    with(forecast.forecastday[0]) {
                        textViewChanceOfRain.text = "  Rain: ${day.dailyChanceOfRain}%"
                        textViewhighestLowestTemperature.text =
                            "H: ${day.maxtempC}??c  |  L: ${day.mintempC}??c"

                        hour.forEach {

                            listOfWeatherModel.add(
                                WeatherRVModel(
                                    time = it.time,
                                    icon = it.condition.icon,
                                    temperature = it.tempC.toString(),
                                    windSpeed = it.windKph.toString()
                                )
                            )
                        }
                        recyclerViewDisplay.adapter?.notifyDataSetChanged()
                    }


                }
            }
        }
    }
    private fun checkGps(){
        if(!Connection.hasGPSConnection(requireContext())){
            checkLocationSetting().addOnCompleteListener(::askGPS)
            Toast.makeText(requireContext(),"Location not found",Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(requireContext(),"Searching location please wait...",Toast.LENGTH_SHORT).show()
    }

    private fun showDay(){

        with(binding!!){
            ivBackground.setImageResource(R.drawable.day)
            ivBackground.alpha = 0.4f
        }
    }
    private fun showNight(){

        with(binding!!){
            ivBackground.setImageResource(R.drawable.night)
            ivBackground.alpha = 0.9f
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()

    }

    private fun getUserCityLocation(){
            binding!!.circularProgressBar.visibility = View.VISIBLE

            try {

               val listOfUserLocation = LastLocation.getUserLocation(requireContext())
                if (listOfUserLocation.isNotEmpty()) {
                    listOfUserLocation.forEach { address ->
                        val city = "${address.locality} ${address.thoroughfare}"
                            weatherViewModel.requestWeather(city)

                    }
                    return
                }
                checkGps()
            } catch (e: Exception) {
                Toast.makeText(requireContext(),"Please check your internet connection",Toast.LENGTH_SHORT).show()
                renderDefaultData()
            }finally {
                binding!!.circularProgressBar.visibility = View.INVISIBLE
            }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_USER_LOCATION && resultCode == RESULT_OK){
            getUserCityLocation()
            return
        }
    }

    private fun checkLocationSetting():Task<LocationSettingsResponse>{

        return LocationServices.getSettingsClient(requireContext())
            .checkLocationSettings(
                LocationSettingsRequest.Builder()
                .addLocationRequest(CommuterFragment.request)
                .setAlwaysShow(true)
                .build())

    }
    private fun askGPS(task: Task<LocationSettingsResponse>){

        try {
            task.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            handleLocationResultException(e)
        }

    }
    private fun handleLocationResultException(e: ApiException) {
        when (e.statusCode) {
            LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                (e as ResolvableApiException).apply {
                    startIntentSenderForResult(this.resolution.intentSender, REQUEST_USER_LOCATION, null, 0, 0, 0, null)
                } }
        }
    }
    private fun updateJsonSharedPreference(cityName:String){
        weatherApiJson.edit().putString(KEY_USER_CITY_JSON_WEATHER,cityName).apply()
    }
    private fun getJsonSharedPreference():String? = weatherApiJson.getString(KEY_USER_CITY_JSON_WEATHER,null)



    private fun setupRecyclerView() = binding!!.recyclerViewDisplay.apply {
        layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.HORIZONTAL,false)
        adapter?.setHasStableIds(true)
        adapter = WeatherAdapter(requireActivity(), listOfWeatherModel)
    }
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if(requestCode == REQUEST_CODE_LOCATION_PERMISSION){
                getUserCityLocation()
        }
    }
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            AppSettingsDialog.Builder(this).build().show()
            renderDefaultData()
        }
    }



}