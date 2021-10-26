package com.example.commutingapp.views.ui.fragments


import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.collection.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.commutingapp.BuildConfig.MAP_STYLE
import com.example.commutingapp.R
import com.example.commutingapp.data.service.TrackingService
import com.example.commutingapp.data.service.innerPolyline
import com.example.commutingapp.databinding.CommuterFragmentBinding
import com.example.commutingapp.utils.InternetConnection.Connection
import com.example.commutingapp.utils.others.BitmapConvert.getBitmapFromVectorDrawable
import com.example.commutingapp.utils.others.Constants
import com.example.commutingapp.utils.others.Constants.ACTION_PAUSE_SERVICE
import com.example.commutingapp.utils.others.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.commutingapp.utils.others.Constants.CAMERA_TILT_DEGREES
import com.example.commutingapp.utils.others.Constants.CAMERA_ZOOM_MAP_MARKER
import com.example.commutingapp.utils.others.Constants.DEFAULT_CAMERA_ANIMATION_DURATION
import com.example.commutingapp.utils.others.Constants.DEFAULT_LATITUDE
import com.example.commutingapp.utils.others.Constants.DEFAULT_LONGITUDE
import com.example.commutingapp.utils.others.Constants.DEFAULT_MAP_ZOOM
import com.example.commutingapp.utils.others.Constants.FAST_CAMERA_ANIMATION_DURATION
import com.example.commutingapp.utils.others.Constants.INVISIBLE_BOTTOM_SHEET_PEEK_HEIGHT
import com.example.commutingapp.utils.others.Constants.LAST_KNOWN_LOCATION_MAP_ZOOM
import com.example.commutingapp.utils.others.Constants.MAP_MARKER_IMAGE_NAME
import com.example.commutingapp.utils.others.Constants.MAP_MARKER_SIZE
import com.example.commutingapp.utils.others.Constants.MINIMUM_MAP_LEVEL
import com.example.commutingapp.utils.others.Constants.POLYLINE_COLOR
import com.example.commutingapp.utils.others.Constants.POLYLINE_WIDTH
import com.example.commutingapp.utils.others.Constants.REQUEST_CHECK_SETTING
import com.example.commutingapp.utils.others.Constants.TEN_METERS
import com.example.commutingapp.utils.others.Constants.TRACKING_MAP_ZOOM
import com.example.commutingapp.utils.others.Constants.VISIBLE_BOTTOM_SHEET_PEEK_HEIGHT
import com.example.commutingapp.utils.others.FragmentToActivity
import com.example.commutingapp.utils.others.TrackingPermissionUtility.hasLocationPermission
import com.example.commutingapp.utils.others.TrackingPermissionUtility.requestPermission
import com.example.commutingapp.viewmodels.MainViewModel
import com.example.commutingapp.views.dialogs.CustomDialogBuilder
import com.example.commutingapp.views.dialogs.DialogDirector
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.mapboxsdk.annotations.PolylineOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.RenderMode
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
    OnMapReadyCallback, MapboxMap.OnMapLongClickListener, MapboxMap.OnMapClickListener {

    private val mainViewModel: MainViewModel by viewModels()
    private var mapBoxMap: MapboxMap? = null
    private var isTracking = false
    private var outerPolyline = mutableListOf<innerPolyline>()
    private var mapBoxView: MapView? = null

    private lateinit var startButton: Button
    private lateinit var directionButton: Button
    private lateinit var saveButton: Button
    private lateinit var shareButton: Button
    private lateinit var dialogDirector: DialogDirector
    private lateinit var commuterFragmentBinding: CommuterFragmentBinding

    private var mapBoxStyle: Style? = null
    private lateinit var mapMarkerSymbol: SymbolManager
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var notifyListener: FragmentToActivity


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeComponents(view)
        hideBottomSheet()
        provideClickListeners()
        setupMapBoxView(savedInstanceState)
        subscribeToObservers()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            this.notifyListener = context as FragmentToActivity
        } catch (e: ClassCastException) { }
    }


    private fun showBottomSheet() {
        provideBottomSheetsButton()
        notifyListener.onFirstNotify()
        bottomSheetBehavior.peekHeight = VISIBLE_BOTTOM_SHEET_PEEK_HEIGHT
    }

    private fun hideBottomSheet() {
        notifyListener.onSecondNotify()
        bottomSheetBehavior.peekHeight = INVISIBLE_BOTTOM_SHEET_PEEK_HEIGHT
    }

    private fun provideBottomSheetsButton() {

    if(!Connection.hasInternetConnection(requireContext()) && Connection.hasGPSConnection(requireContext())){
        showNoInternetAndHasGpsBottomSheetLayout()
        return
    }
    if(Connection.hasInternetConnection(requireContext()) && !Connection.hasGPSConnection(requireContext())){
        showHasInternetAndNoGpsBottomSheetLayout()
        return
    }
    if(!Connection.hasInternetConnection(requireContext()) && !Connection.hasGPSConnection(requireContext())){
        showNoInternetAndNoGpsBottomSheetLayout()
        return
    }

        showDefaultBottomSheetLayout()
    }

    private fun showNoInternetAndHasGpsBottomSheetLayout() {
        shareButton.visibility = View.GONE
        saveButton.visibility = View.GONE
        directionButton.visibility = View.VISIBLE
        startButton.visibility = View.VISIBLE
    }

    private fun showHasInternetAndNoGpsBottomSheetLayout() {
        startButton.visibility = View.GONE
        saveButton.visibility = View.VISIBLE
        shareButton.visibility = View.VISIBLE
        directionButton.visibility = View.VISIBLE
    }

    private fun showNoInternetAndNoGpsBottomSheetLayout() {
        startButton.visibility = View.GONE
        saveButton.visibility = View.GONE
        shareButton.visibility = View.GONE
        directionButton.visibility = View.VISIBLE
    }

    private fun showDefaultBottomSheetLayout() {
        saveButton.visibility = View.VISIBLE
        shareButton.visibility = View.VISIBLE
        directionButton.visibility = View.VISIBLE
        startButton.visibility = View.VISIBLE

    }

    private fun initializeComponents(view: View) {
        dialogDirector = DialogDirector(requireActivity())
        mapBoxView = view.findViewById(R.id.googleMapView)
        startButton = view.findViewById(R.id.startButton)
        directionButton = view.findViewById(R.id.directionsButton)
        saveButton = view.findViewById(R.id.saveButton)
        shareButton = view.findViewById(R.id.shareButton)
        bottomSheetBehavior = BottomSheetBehavior.from(view.findViewById(R.id.bottomSheet)).apply {
            state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun setupMapBoxView(savedInstanceState: Bundle?) {
        mapBoxView?.apply {
            onCreate(savedInstanceState)
            isClickable = true
        }?.also {
            it.getMapAsync(this)
        }
    }

    private fun provideClickListeners() {

        commuterFragmentBinding.floatingActionButtonChooseMap.setOnClickListener {
            dialogDirector.constructChooseMapDialog().apply {
                setMapTypeListeners(this)
                show()
            }
        }
        commuterFragmentBinding.floatingActionButtonLocation.setOnClickListener {
            mapBoxMap?.locationComponent?.lastKnownLocation?.let { location ->
                moveCameraToUser(
                    LatLng(location.latitude, location.longitude), CAMERA_ZOOM_MAP_MARKER,
                    DEFAULT_CAMERA_ANIMATION_DURATION
                )
            }
        }

        startButton.setOnClickListener {
            if (requestPermissionGranted()) {
                checkLocationSetting()
            }
        }
        directionButton.setOnClickListener {

        }
        saveButton.setOnClickListener {

        }
        shareButton.setOnClickListener {

        }


    }

    private fun setMapTypeListeners(customDialogBuilder: CustomDialogBuilder) {
        customDialogBuilder.apply {
            findViewById<View>(R.id.defaultMapStyleButton)?.setOnClickListener {
                changeMapType(Style.TRAFFIC_DAY)
            }
            findViewById<View>(R.id.trafficNightMapStyleButton)?.setOnClickListener {
                changeMapType(Style.TRAFFIC_NIGHT)
            }
            findViewById<View>(R.id.darkMapStyleButton)?.setOnClickListener {
                changeMapType(Style.DARK)
            }
            findViewById<View>(R.id.streetMapStyleButton)?.setOnClickListener {
                changeMapType(Style.MAPBOX_STREETS)
            }
            findViewById<View>(R.id.satelliteStreetsMapStyleButton)?.setOnClickListener {
                changeMapType(Style.SATELLITE_STREETS)
            }
            findViewById<View>(R.id.outdoorsMapStyleButton)?.setOnClickListener {
                changeMapType(Style.OUTDOORS)
            }
            findViewById<View>(R.id.lightMapStyle)?.setOnClickListener {
                changeMapType(Style.LIGHT)
            }
            findViewById<View>(R.id.neonMapStyleButton)?.setOnClickListener {
                changeMapType(MAP_STYLE)
            }
            findViewById<View>(R.id.satelliteMapStyleButton)?.setOnClickListener {
                changeMapType(Style.SATELLITE)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveCameraToLastKnownLocation() {
        LocationServices.getFusedLocationProviderClient(requireActivity()).apply {
            lastLocation.addOnSuccessListener {
                it?.let { latLng ->
                    moveCameraToUser(
                        LatLng(latLng.latitude, latLng.longitude),
                        LAST_KNOWN_LOCATION_MAP_ZOOM,
                        FAST_CAMERA_ANIMATION_DURATION
                    )
                }
            }
            lastLocation.addOnFailureListener {
                moveCameraToUser(
                    LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE), DEFAULT_MAP_ZOOM,
                    DEFAULT_CAMERA_ANIMATION_DURATION
                )
            }
        }
    }

    companion object {
        val request: LocationRequest = LocationRequest.create().apply {
            interval = Constants.NORMAL_LOCATION_UPDATE_INTERVAL
            smallestDisplacement = TEN_METERS
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

    private fun handleLocationResultException(e: ApiException) {
        when (e.statusCode) {
            LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                (e as ResolvableApiException).apply {
                startIntentSenderForResult(this.resolution.intentSender, REQUEST_CHECK_SETTING, null, 0, 0, 0, null);
            } }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CHECK_SETTING -> when (resultCode) {
                RESULT_OK -> {
                    toggleStartButton()
                }

            }
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapBoxMap = mapboxMap.apply {
            uiSettings.isAttributionEnabled = false
            uiSettings.isLogoEnabled = false
        }

        mapBoxMap?.addOnMapLongClickListener(this)
        mapBoxMap?.addOnMapClickListener(this)
        setMapZoomLevel()
        addMapStyle(MAP_STYLE)
        moveCameraToLastKnownLocation()
        addAllPolyLines()
    }

    private fun setMapZoomLevel() {
        mapBoxMap?.setMaxZoomPreference(MINIMUM_MAP_LEVEL)
    }

    private fun addAllPolyLines() {
        outerPolyline.forEach {
            customPolylineAppearance().addAll(it).apply {
                mapBoxMap?.addPolyline(this)
            }
        }
    }

    private fun addMapStyle(mapType: String) {
        mapBoxMap?.getStyle {
            Toast.makeText(requireContext(), it.uri.toString(), Toast.LENGTH_SHORT).show()
        }
        mapBoxMap?.setStyle(mapType) { style ->
            mapBoxView?.let { mapView ->
                TrafficPlugin(mapView, mapBoxMap!!, style).apply { setVisibility(true) }
                mapBoxStyle = style
                createLocationPuck(style)
                mapMarkerSymbol = SymbolManager(mapView, mapBoxMap!!, mapBoxStyle!!)
            }
        }
    }

    private fun changeMapType(mapType: String) {
        mapBoxMap?.setStyle(mapType)

    }


    override fun onMapLongClick(point: LatLng): Boolean {
        pointMapMarker(point)
        return true
    }

    override fun onMapClick(point: LatLng): Boolean {
        mapMarkerSymbol.deleteAll()
        hideBottomSheet()
        return true
    }


    private fun pointMapMarker(latLng: LatLng) {
        mapBoxStyle?.addImage(
            MAP_MARKER_IMAGE_NAME,
            getBitmapFromVectorDrawable(requireContext(), R.drawable.ic_location)
        )
        if (hasExistingMapMarker()) {
            mapMarkerSymbol.deleteAll()
        }
        showBottomSheet()
        createMapMarker(latLng)
        mapBoxMap?.cameraPosition?.apply {
            moveCameraToUser(latLng, zoom, DEFAULT_CAMERA_ANIMATION_DURATION)
        }


    }

    private fun hasExistingMapMarker() = mapMarkerSymbol.annotations.size != 0

    private fun createMapMarker(latLng: LatLng) {
        mapMarkerSymbol.create(
            SymbolOptions()
                .withLatLng(latLng)
                .withIconImage(MAP_MARKER_IMAGE_NAME)
                .withIconSize(MAP_MARKER_SIZE)
        )

    }


    private fun createLocationPuck(style: Style) {
        LocationComponentOptions.builder(requireContext())
            .build().also { componentOptions ->
                mapBoxMap?.locationComponent?.apply {
                    activateLocationComponent(
                        LocationComponentActivationOptions.builder(requireContext(), style)
                            .locationComponentOptions(componentOptions)
                            .build()
                    )
                    createComponentsLocation(this)
                }
            }
    }


    @SuppressLint("MissingPermission")
    private fun createComponentsLocation(locationComponent: LocationComponent) {
        locationComponent.apply {
            isLocationComponentEnabled = true
            renderMode = RenderMode.NORMAL;
        }
    }

    private fun subscribeToObservers() {
        TrackingService().isCurrentlyTracking().observe(viewLifecycleOwner) {
            isTracking = it
            mapBoxStyle?.let { style -> createLocationPuck(style) }
            updateButtons()

        }

        TrackingService().outerPolyline().observe(viewLifecycleOwner) {
            outerPolyline = it

            addLatestPolyline()
            if (hasExistingInnerAndOuterPolyLines()) {
                moveCameraToUser(
                    outerPolyline.last().last(), TRACKING_MAP_ZOOM,
                    DEFAULT_CAMERA_ANIMATION_DURATION
                )
            }
        }
        TrackingService.timeInMillis.observe(viewLifecycleOwner) {
            //TODO implement later
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
            startButton.text = getString(R.string.stopButton)
            return
        }

        startButton.text = getString(R.string.startButton)
    }


    private fun moveCameraToUser(latLng: LatLng,zoomLevel:Double,cameraAnimationDuration:Int) {
        mapBoxMap?.animateCamera(CameraUpdateFactory.newCameraPosition(buildCameraPosition(latLng,zoomLevel)), cameraAnimationDuration);
    }

    private fun buildCameraPosition(latLng: LatLng, zoomLevel: Double): CameraPosition =
        CameraPosition.Builder()
            .target(latLng)
            .zoom(zoomLevel)
            .tilt(CAMERA_TILT_DEGREES)
            .build()

    private fun addLatestPolyline() {
        if (hasExistingInnerPolyLines()) {
            val innerPolylinePosition = outerPolyline.last().size - 2
            val preLastLatLng = outerPolyline.last()[innerPolylinePosition]
            val lastLatLng = outerPolyline.last().last()

            customPolylineAppearance()
                .add(preLastLatLng)
                .add(lastLatLng).apply {
                    mapBoxMap?.addPolyline(this)
                }
        }
    }


    private fun customPolylineAppearance(): PolylineOptions {

        return PolylineOptions()
            .color(POLYLINE_COLOR)
            .width(POLYLINE_WIDTH)


    }


    private fun hasExistingInnerAndOuterPolyLines() =
        outerPolyline.isNotEmpty() && outerPolyline.last().isNotEmpty()

    private fun hasExistingInnerPolyLines() =
        outerPolyline.isNotEmpty() && outerPolyline.last().size > 1


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