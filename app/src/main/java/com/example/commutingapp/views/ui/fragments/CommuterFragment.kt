package com.example.commutingapp.views.ui.fragments


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.commutingapp.R
import com.example.commutingapp.data.others.BitmapConvert
import com.example.commutingapp.data.others.Constants
import com.example.commutingapp.data.others.Constants.ACTION_PAUSE_SERVICE
import com.example.commutingapp.data.others.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.commutingapp.data.others.Constants.ACTION_STOP_SERVICE
import com.example.commutingapp.data.others.Constants.DEFAULT_MAP_ZOOM
import com.example.commutingapp.data.others.Constants.MAP_MARKER_IMAGE_NAME
import com.example.commutingapp.data.others.Constants.MAP_MARKER_SIZE
import com.example.commutingapp.data.others.Constants.POLYLINE_COLOR
import com.example.commutingapp.data.others.Constants.POLYLINE_WIDTH
import com.example.commutingapp.data.others.Constants.REQUEST_CHECK_SETTING
import com.example.commutingapp.data.others.TrackingPermissionUtility.hasLocationPermission
import com.example.commutingapp.data.others.TrackingPermissionUtility.requestPermission

import com.example.commutingapp.data.service.TrackingService
import com.example.commutingapp.data.service.innerPolyline
import com.example.commutingapp.viewmodels.MainViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.mapbox.mapboxsdk.annotations.PolylineOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.plugins.traffic.TrafficPlugin
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions


@AndroidEntryPoint
class CommuterFragment : Fragment(R.layout.commuter_fragment), EasyPermissions.PermissionCallbacks,
    OnMapReadyCallback {

    private val viewModel: MainViewModel by viewModels()

    private var map: MapboxMap? = null
    private var isTracking = false
    private var outerPolyline = mutableListOf<innerPolyline>()
    private lateinit var mapBoxView: MapView
    private lateinit var buttonStart: Button
    private lateinit var buttonStop: Button
    private lateinit var mapBoxStyle: Style
    private lateinit var symbolManager: SymbolManager


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonStart = view.findViewById(R.id.startButton)
        buttonStop = view.findViewById(R.id.finishButton)

        buttonStart.setOnClickListener {
            if (requestPermissionGranted()) {
                checkLocationSetting()
            }

        }
        buttonStop.setOnClickListener { sendCommandToTrackingService(ACTION_STOP_SERVICE) }
        mapBoxView = view.findViewById(R.id.googleMapView)
        mapBoxView.apply {
            onCreate(savedInstanceState)
            isClickable = true
        }.also {
            it.getMapAsync(this)
        }
        subscribeToObservers()


    }

    companion object {
        val request: LocationRequest = LocationRequest.create().apply {
            interval = Constants.NORMAL_LOCATION_UPDATE_INTERVAL
            fastestInterval = Constants.FASTEST_LOCATION_UPDATE_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

    }

    var locationRequest: LocationRequest = request

    private fun checkLocationSetting() {

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(request)
            .setAlwaysShow(true)


        LocationServices.getSettingsClient(requireContext())
            .checkLocationSettings(builder.build()).apply {
                getLocationResult(this)
            }


    }

    private fun getLocationResult(result: Task<LocationSettingsResponse>) {
        result.addOnCompleteListener {
            try {
                it.getResult(ApiException::class.java)
                toggleStartButton()
            } catch (e: ApiException) {
                handleLocationResultException(e)
            }
        }
    }

    private fun handleLocationResultException(e:ApiException){
        when (e.statusCode) {
            LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                val resolvableApiException = e as ResolvableApiException
                resolvableApiException.startResolutionForResult(
                    requireActivity(),
                    REQUEST_CHECK_SETTING
                )
            }
            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                // USER DEVICE DOES NOT HAVE LOCATION OPTION //TODo
            }
        }
    }
    override fun onMapReady(mapboxMap: MapboxMap) {
        map = mapboxMap.apply {
            uiSettings.isAttributionEnabled = false
            uiSettings.isLogoEnabled = false
        }
        addMapStyle(mapboxMap)
        addMarkerOnMapLongClick(mapboxMap)
        addAllPolyLines()


    }


    private fun addMapStyle(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            TrafficPlugin(mapBoxView, mapboxMap, it).apply { setVisibility(true) }
            this.mapBoxStyle = it


        }
    }

    private fun addMarkerOnMapLongClick(mapboxMap: MapboxMap) {

        map?.let {
            it.addOnMapLongClickListener { point ->
                setMapMarker(point, mapboxMap)

                true
            }
        }

    }

    private fun setMapMarker(point: LatLng, mapboxMap: MapboxMap) {
        mapBoxStyle.addImage(MAP_MARKER_IMAGE_NAME,BitmapConvert.getBitmapFromVectorDrawable(requireContext(), R.drawable.ic_location))
        if (!::symbolManager.isInitialized) {
            symbolManager = SymbolManager(mapBoxView, mapboxMap, mapBoxStyle).apply {
                iconAllowOverlap = true
                iconIgnorePlacement = true

            }
        }
        symbolManager.deleteAll()
        symbolManager.create(
            SymbolOptions()
                .withLatLng(point)
                .withIconImage(MAP_MARKER_IMAGE_NAME)
                .withIconSize(MAP_MARKER_SIZE)
        )
        //  moveCameraToUser()//TODO test


    }


    private fun subscribeToObservers() {
        TrackingService().isTracking().observe(viewLifecycleOwner) {
            isTracking = it
            updateButtons()
        }

        TrackingService().outerPolyline().observe(viewLifecycleOwner) {
            outerPolyline = it
            addLatestPolyline()
            moveCameraToUser()
        }
    }

    private fun sendCommandToTrackingService(action: String) {
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }
    }

    private fun toggleStartButton() {
        if (isTracking) {
            sendCommandToTrackingService(ACTION_PAUSE_SERVICE)
            return
        }
        sendCommandToTrackingService(ACTION_START_OR_RESUME_SERVICE)
    }

    private fun updateButtons() {

        if (isTracking) {
            buttonStart.text =  getString(R.string.stopButton)
            buttonStop.visibility = View.GONE
            return
        }

        buttonStart.text =  getString(R.string.startButton)
        buttonStop.visibility = View.VISIBLE
    }


    private fun moveCameraToUser() {
        if (hasExistingInnerAndOuterPolyLines()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    outerPolyline.last().last(),
                    DEFAULT_MAP_ZOOM
                )
            )
        }
    }

    private fun addAllPolyLines() {

        outerPolyline.forEach {
            customPolylineAppearance().addAll(it).apply {
                map?.addPolyline(this)
            }
        }
    }

    private fun addLatestPolyline() {
        if (hasExistingInnerPolyLines()) {
            val innerPolylinePosition = outerPolyline.last().size - 2
            val preLastLatLng = outerPolyline.last()[innerPolylinePosition]
            val lastLatLng = outerPolyline.last().last()

            customPolylineAppearance()
                .add(preLastLatLng)
                .add(lastLatLng).apply {
                    map?.addPolyline(this)

                }

        }
    }


    private fun customPolylineAppearance(): PolylineOptions {

        return PolylineOptions()
            .color(POLYLINE_COLOR)
            .width(POLYLINE_WIDTH)


    }


    private fun hasExistingInnerAndOuterPolyLines() =
        outerPolyline.last().isNotEmpty() && outerPolyline.isNotEmpty()

    private fun hasExistingInnerPolyLines() =
        outerPolyline.isNotEmpty() && outerPolyline.last().size > 1


    override fun onResume() {
        super.onResume()
        mapBoxView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapBoxView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapBoxView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapBoxView.onLowMemory()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapBoxView.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        mapBoxView.onPause()
    }

    private fun requestPermissionGranted(): Boolean {
        if (hasLocationPermission(requireContext())) {
            return true
        }
        requestPermission(this)
        return false

    }


    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermission(this)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)

    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {}


}