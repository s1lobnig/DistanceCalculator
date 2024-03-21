package com.example.distance_and_area_meter

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil
import kotlinx.android.synthetic.main.content_maps.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private var polygon: Polygon? = null
    private var polyline: Polyline? = null
    private var marker: Marker? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * Until the real position is known, the map shows the University of Klagenfurt.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val uni = LatLng(46.616122, 14.264801)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uni, 10.0f))
    }

    fun onBtnClearClicked(view: View) {
        polyline?.remove()
        polygon?.remove()
        marker?.remove()
        startMarker?.remove()
        endMarker?.remove()
        polyline = null
        polygon = null
        marker = null
        startMarker = null
        endMarker = null

        txtOutput.visibility = View.GONE
        btnClear.visibility = View.GONE
        btnStartTracking.visibility = View.VISIBLE
        btnStartSelecting.visibility = View.VISIBLE
        btnArea.visibility = View.GONE
        btnDistance.visibility = View.GONE
    }

    fun onBtnStartTrackingClicked(view: View) {
        if (ContextCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationAccess()
        } else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        } else {
            startLocationListener()

            btnStartTracking.visibility = View.GONE
            btnStartSelecting.visibility = View.GONE
            btnEnd.visibility = View.VISIBLE
        }
    }

    fun onBtnStartSelectingClicked(view: View) {
        btnStartTracking.visibility = View.GONE
        btnStartSelecting.visibility = View.GONE
        btnSelect.visibility = View.VISIBLE
        btnEnd.visibility = View.VISIBLE

        mMap.setOnMapClickListener {
            changeMarkerPosition(it)
        }
    }

    fun onBtnSelectClicked(view: View) {
        if (marker == null) return
        if (polyline == null) {
            createPolyline(marker!!.position)
        } else {
            var list = polyline?.points
            list?.add(marker!!.position)
            polyline?.points = list
        }
    }

    fun onBtnEndClicked(view: View) {
        btnEnd.visibility = View.GONE
        btnSelect.visibility = View.GONE

        if (polyline != null) {
            txtOutput.visibility = View.VISIBLE
            btnClear.visibility = View.VISIBLE
            btnArea.visibility = View.VISIBLE
            endMarker = mMap.addMarker(
                MarkerOptions()
                    .position(marker!!.position)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            calculateDistance()
        } else {
            onBtnClearClicked(btnClear)
        }

        locationManager.removeUpdates(this)
        mMap.setOnMapClickListener(null)
    }

    private fun calculateDistance() {
        val distance = SphericalUtil.computeLength(polyline?.points)
        txtOutput.setText("distance = " + distance.toInt() + "m")
    }

    fun onBtnAreaClicked(view: View) {
        btnArea.visibility = View.GONE
        btnDistance.visibility = View.VISIBLE

        var polygonOption = PolygonOptions()
        for (item in polyline!!.points) {
            polygonOption.add(item)
        }
        polygon = mMap.addPolygon(polygonOption)
        polygon?.fillColor = Color.LTGRAY

        calculateArea()
    }

    private fun calculateArea() {
        val area = SphericalUtil.computeArea(polyline?.points)
        txtOutput.setText("area = " + area.toInt() + "m^2")
    }

    fun onBtnDistanceClicked(view: View) {
        btnArea.visibility = View.VISIBLE
        btnDistance.visibility = View.GONE

        polygon?.remove()
        polygon = null
        calculateDistance()
    }

    fun onBtnMapTypeClicked(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.maptype, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.standard -> mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                R.id.satellite -> mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                R.id.hybrid -> mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            }
            true
        }
        popup.show()
    }

    fun onLocationClicked(view: View) {
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationAccess()
        } else if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        } else {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000,
                0.5f,
                object : LocationListener {
                    override fun onLocationChanged(location: Location?) {
                        if (location != null) {
                            val position = LatLng(location.latitude, location.longitude)
                            changeMarkerPosition(position)
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 17.0f))

                            locationManager.removeUpdates(this)
                        }
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                        locationManager.removeUpdates(this)
                    }

                    override fun onProviderEnabled(provider: String?) {
                        locationManager.removeUpdates(this)
                    }

                    override fun onProviderDisabled(provider: String?) {
                        locationManager.removeUpdates(this)
                    }
                }
            )
        }
    }

    private fun changeMarkerPosition(position: LatLng) {
        if (marker == null) {
            marker = mMap.addMarker(
                MarkerOptions().position(position)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
        } else {
            marker!!.position = position
        }
    }

    private fun requestLocationAccess() {
        ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), 1)
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton(
                "Yes"
            ) { dialog, id ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton(
                "No"
            ) { dialog, id -> dialog.cancel() }
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    private fun startLocationListener(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000,
                0.5f,
                this
            )
            return true
        }
        return false
    }

    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            val position = LatLng(location.latitude, location.longitude)
            changeMarkerPosition(position)
            Log.i("Location: ", "Lat(" + position.latitude + "), Long(" + position.longitude + ")")

            if (polyline == null) {
                createPolyline(position)
            } else {
                var list = polyline?.points
                list?.add(position)
                polyline?.points = list
            }
        }
    }

    private fun createPolyline(position: LatLng) {
        polyline = mMap.addPolyline(PolylineOptions().add(position))
        startMarker = mMap.addMarker(
            MarkerOptions().position(marker!!.position)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position))
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String?) {}

    override fun onProviderDisabled(provider: String?) {}
}
