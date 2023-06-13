package com.example.fristianago

//import android.R
import android.Manifest
import android.content.Intent

import android.os.Bundle

import android.os.Handler
import android.os.Looper

import android.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.example.fristianago.databinding.ActivityMapsBinding

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.StreetViewPanoramaFragment
import com.google.android.gms.maps.StreetViewPanoramaOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.LocationBias
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.google.android.libraries.places.api.net.PlacesClient

import java.io.IOException
import java.util.Locale

import kotlin.math.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener,
    GoogleMap.OnPoiClickListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMapLongClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var previousMarker: Marker? = null
    private var toast: Toast? = null
    private val handler = Handler(Looper.getMainLooper())

    val zoom = 15f
    private val TAG = MapsActivity::class.java.simpleName
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_LOCATION_PERMISSION = 1

    private lateinit var btnMyLocation: Button
    private lateinit var placesClient: PlacesClient

    //    private lateinit var streetViewFragment: StreetViewFragment
    private var streetViewFragment: SupportStreetViewPanoramaFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnMyLocation = findViewById(R.id.btnMyLocation)
        btnMyLocation.setOnClickListener {
            // Handle the "My Location" button click
            onMyLocationButtonClick()
        }

        // Initialize Places SDK
        Places.initialize(applicationContext, getString(R.string.google_maps_key))

        // Create a PlacesClient instance
        placesClient = Places.createClient(this)

        // Set up the button click listener
        val btnNearbyRestaurants: Button = findViewById(R.id.btnNearbyRestaurants)
        btnNearbyRestaurants.setOnClickListener {
            findNearbyRestaurants()
        }

//        streetViewFragment = StreetViewFragment.newInstance()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                search(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Implement search suggestions or real-time filtering if needed
                return false
            }
        })
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

//        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
//        moveCameraToCurrentLocation()
        // Set the map click listener
        mMap.setOnMapClickListener(this)
        mMap.setOnPoiClickListener(this)
        // Set the info window click listener
        mMap.setOnInfoWindowClickListener(this)

        mMap.setOnMapLongClickListener(this)

        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission already granted, move camera to current location
            moveCameraToCurrentLocation()
//            enableMyLocationWithZoom()
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun onMyLocationButtonClick() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission granted, handle the location update
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    // Custom marker for current location
                    val markerOptions = MarkerOptions()
                        .position(currentLatLng)
                        .title("Nana")
                        .icon(
                            BitmapDescriptorFactory.fromBitmap(
                                resizeImage(
                                    getMarkerBitmapFromDrawable(R.drawable.bunny),
                                    100,
                                    100
                                )
                            )
                        )

                    // Add the marker to the map
                    mMap.addMarker(markerOptions)
//                    currentLocationMarker?.position = currentLatLng
                }
            }
        } else {
            // Permission not granted, request location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun getMarkerBitmapFromDrawable(drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(this, drawableId)
//        val bitmap = Bitmap.createBitmap(drawable!!.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun resizeImage(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.map_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Change the map type based on the user's selection.
        return when (item.itemId) {
            R.id.normal_map -> {
                mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                true
            }

            R.id.hybrid_map -> {
                mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
                true
            }

            R.id.satellite_map -> {
                mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                true
            }

            R.id.terrain_map -> {
                mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun moveCameraToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    // Create a custom marker icon
                    val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.bunny)
                    val desiredWidth = 100 // Specify the desired width in pixels
                    val desiredHeight = 100 // Specify the desired height in pixels

                    val scaledBitmap = Bitmap.createScaledBitmap(
                        originalBitmap,
                        desiredWidth,
                        desiredHeight,
                        false
                    )

                    val markerOptions = MarkerOptions()
                        .position(currentLatLng)
                        .title("Nana")
                        .icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap))
                    // Add the marker to the map
                    mMap.addMarker(markerOptions)
//                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, zoom))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        }
    }

    //diset zoom sesuai default yang ditentukan di general variable
    private fun enableMyLocationWithZoom() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
//                    val currentZoom = 15f
                    val currentLatLng = LatLng(location.latitude, location.longitude)

//                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, currentZoom))
//                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, zoom))

//                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, zoom))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    // Custom marker for current location
                    val markerOptions = MarkerOptions()
                        .position(currentLatLng)
                        .title("Nana")
                        .icon(
                            BitmapDescriptorFactory.fromBitmap(
                                resizeImage(
                                    getMarkerBitmapFromDrawable(R.drawable.bunny),
                                    100,
                                    100
                                )
                            )
                        )

                    // Add the marker to the map
                    mMap.addMarker(markerOptions)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if location permissions are granted and if so enable the
        // location data layer.
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> if (grantResults.size > 0
                && grantResults[0]
                == PackageManager.PERMISSION_GRANTED
            ) {
                moveCameraToCurrentLocation()
//                enableMyLocation()
                enableMyLocationWithZoom()
            }
        }
    }

    private fun search(query: String?) {
        if (query.isNullOrEmpty()) return

        val geocoder = Geocoder(this)
        var locations: List<Address>? = null

        try {
            // Try to search by place name
            locations = geocoder.getFromLocationName(query, 1)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (locations == null || locations.isEmpty()) {
            // If not found, try to search by latitude and longitude
            val latLng = parseLatLng(query)
            if (latLng != null) {
                val markerOptions = MarkerOptions().position(latLng).title("Custom Location")
//                mMap.clear()
                previousMarker?.remove()
                val newMarker = mMap.addMarker(markerOptions)
                // Store the new marker as the previous marker
                previousMarker = newMarker
//                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
            }
        } else {
            val address = locations[0]
            val latLng = LatLng(address.latitude, address.longitude)
            val markerOptions = MarkerOptions().position(latLng).title(address.getAddressLine(0))
//            mMap.clear()
            previousMarker?.remove()
            val newMarker = mMap.addMarker(markerOptions)
            // Store the new marker as the previous marker
            previousMarker = newMarker
//            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    private fun parseLatLng(query: String): LatLng? {
        val pattern = "(-?\\d+(?:\\.\\d+)?),\\s*(-?\\d+(?:\\.\\d+)?)".toRegex()
        val matchResult = pattern.find(query)
        if (matchResult != null && matchResult.groupValues.size == 3) {
            val latitude = matchResult.groupValues[1].toDoubleOrNull()
            val longitude = matchResult.groupValues[2].toDoubleOrNull()
            if (latitude != null && longitude != null) {
                return LatLng(latitude, longitude)
            }
        }
        return null
    }

    override fun onMapClick(latLng: LatLng) {
        val geocoder = Geocoder(this)

        try {
            // Convert the coordinates to address
            val addresses: List<Address> =
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)!!
                    .toList()

            if (addresses.isNotEmpty()) {
                val address: Address = addresses[0]
                val placeName = address.featureName
                val coordinates = "Lat: ${latLng.latitude}, Long: ${latLng.longitude}"

                val snippet = String.format(
                    Locale.getDefault(),
                    "Lat: %.5f, Long: %.5f",
                    latLng.latitude,
                    latLng.longitude
                )
                //hapus marker sebelumnya dulu
                previousMarker?.remove()
                //add new marker
                // Add a new marker at the clicked position
                val newMarker = mMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(getString(R.string.dropped_pin))
                        .snippet(snippet)
                )
                // Store the new marker as the previous marker
                previousMarker = newMarker

                toast?.cancel() // Cancel the previous toast if any
                Toast.makeText(this, "$placeName\n$coordinates", Toast.LENGTH_SHORT).show()
                toast?.show()

                // Set the toast duration
                val toastDuration = 3000L // 3 seconds
//        Handler().postDelayed({ toast?.cancel() }, toastDuration)
                handler.postDelayed({ toast?.cancel() }, toastDuration)
            } else {
                toast?.cancel() // Cancel the previous toast if any
                Toast.makeText(
                    this,
                    "No address found for the selected location",
                    Toast.LENGTH_SHORT
                ).show()
                toast?.show()

                // Set the toast duration
                val toastDuration = 3000L // 3 seconds
//        Handler().postDelayed({ toast?.cancel() }, toastDuration)
                handler.postDelayed({ toast?.cancel() }, toastDuration)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPoiClick(poi: PointOfInterest) {
        val placeName = poi.name
        val coordinates = "Lat: ${poi.latLng.latitude}, Long: ${poi.latLng.longitude}"

        //hapus marker sebelumnya dulu
        previousMarker?.remove()
        val poiMarker = mMap.addMarker(
            MarkerOptions()
                .position(poi.latLng)
                .title(poi.name)
                .icon(
                    BitmapDescriptorFactory.defaultMarker
                        (BitmapDescriptorFactory.HUE_AZURE)
                )
        )
        if (poiMarker != null) {
            poiMarker.showInfoWindow()
            poiMarker.setTag("poi")
            // Store the new marker as the previous marker
            previousMarker = poiMarker

            toast?.cancel() // Cancel the previous toast if any

            // Show the toast message
            toast = Toast.makeText(this, "$placeName\n$coordinates", Toast.LENGTH_SHORT)
            toast?.show()

            // Set the toast duration
            val toastDuration = 3000L // 3 seconds
//        Handler().postDelayed({ toast?.cancel() }, toastDuration)
            handler.postDelayed({ toast?.cancel() }, toastDuration)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the toast when the activity is destroyed
        toast?.cancel()
    }

////    ketika info diklik diredirect ke panoramanya gmaps
//    override fun onInfoWindowClick(marker: Marker) {
//        // Check if the clicked marker represents a Point of Interest
//        if (marker.tag == "poi") {
//            val latLng = marker.position
//
//            // Launch Street View panorama activity
//            val streetViewUri = Uri.parse("google.streetview:cbll=${latLng.latitude},${latLng.longitude}")
//            val streetViewIntent = Intent(Intent.ACTION_VIEW, streetViewUri)
//            streetViewIntent.setPackage("com.google.android.apps.maps")
//            startActivity(streetViewIntent)
//        }
//    }

    override fun onInfoWindowClick(marker: Marker) {
        if (marker.tag == "poi") {
            val options = StreetViewPanoramaOptions().position(marker.position)
            streetViewFragment = SupportStreetViewPanoramaFragment.newInstance(options)

            supportFragmentManager.beginTransaction()
                .replace(R.id.mapFragment, streetViewFragment!!)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun findNearbyRestaurants(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // Get current location
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
//                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    // Search nearby restaurants
                    val placeFields = listOf(
                        Place.Field.NAME,
                        Place.Field.ADDRESS,
                        Place.Field.LAT_LNG
                    )

                    // Search nearby restaurants
                    val request = FindCurrentPlaceRequest.builder(placeFields)
                        .build()

                    placesClient.findCurrentPlace(request).addOnSuccessListener { response ->
                        val filteredPlaces = response.placeLikelihoods.filter { placeLikelihood ->
                            val place = placeLikelihood.place
                            val placeTypes = place.types
                            val restaurantLatLng = place.latLng

                            placeTypes != null && placeTypes.contains(Place.Type.RESTAURANT)
                                    && restaurantLatLng != null && currentLatLng.distanceTo(restaurantLatLng) <=1000
                        }
//                        val filteredPlaces = response.placeLikelihoods.filter { placeLikelihood ->
//                            val place = placeLikelihood.place
//                            val restaurantLatLng = place.latLng
//
//                            restaurantLatLng != null && currentLatLng.distanceTo(restaurantLatLng) <=1000
//                        }

                        for (placeLikelihood in filteredPlaces) {
                            val place = placeLikelihood.place
                            val restaurantLatLng = place.latLng
                            val restaurantName = place.name
                            val restaurantAddress = place.address

                            if (restaurantLatLng != null && restaurantName != null && restaurantAddress != null) {
                                //hapus marker sebelumnya dulu
                                previousMarker?.remove()
                                //add new marker
                                val newMarker =mMap.addMarker(
                                    MarkerOptions().position(restaurantLatLng)
                                        .title(restaurantName)
                                        .snippet(restaurantAddress)
                                )

                                // Store the new marker as the previous marker
                                previousMarker = newMarker
                            }
                        }
                    }.addOnFailureListener { exception ->
                        Toast.makeText(
                            this,
                            "Failed to retrieve nearby restaurants: ${exception.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

        }
    }

    fun LatLng.distanceTo(destination: LatLng): Double {
        val earthRadius = 6371 // Radius of the Earth in kilometers
        val lat1 = Math.toRadians(latitude)
        val lon1 = Math.toRadians(longitude)
        val lat2 = Math.toRadians(destination.latitude)
        val lon2 = Math.toRadians(destination.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    override fun onMapLongClick(latLng: LatLng) {
        addOverlay(latLng)
    }
    private fun addOverlay(latLng: LatLng) {
        val overlayImage: BitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.overlay_carrot)
        val overlayWidth = 100f // Width of the overlay in meters
        val overlayHeight = 100f // Height of the overlay in meters
        val groundOverlayOptions = GroundOverlayOptions()
            .image(overlayImage)
            .position(latLng, overlayWidth, overlayHeight)
        mMap.addGroundOverlay(groundOverlayOptions)
    }

}