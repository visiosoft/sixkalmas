package com.mpo.sixkalmas

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import kotlin.math.roundToInt

class QiblaFinderActivity : AppCompatActivity(), SensorEventListener, LocationListener {
    private lateinit var compassImage: ImageView
    private lateinit var directionText: TextView
    private lateinit var qiblaArrow: ImageView
    private lateinit var accuracyIndicator: View
    private lateinit var kaabaIcon: ImageView
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private var currentDegree = 0f
    private var qiblaBearing = 0f
    private var currentLocation: Location? = null
    private var isFirstLocationUpdate = true
    private var lastUpdateTime = 0L
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val UPDATE_INTERVAL = 16L // ~60fps for smooth updates
    private var lastLocationUpdateTime = 0L
    private val LOCATION_UPDATE_INTERVAL = 100L // Update location every 100ms
    private val LOCATION_UPDATE_DISTANCE = 0.1f // Update for very small changes

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1
        private const val QIBLA_LATITUDE = 21.4225
        private const val QIBLA_LONGITUDE = 39.8262
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qibla_finder)

        // Initialize views
        compassImage = findViewById(R.id.compassImage)
        directionText = findViewById(R.id.directionText)
        qiblaArrow = findViewById(R.id.qiblaArrow)
        accuracyIndicator = findViewById(R.id.accuracyIndicator)
        kaabaIcon = findViewById(R.id.kaabaIcon)
        val backButton = findViewById<MaterialButton>(R.id.backButton)

        // Initialize sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // Initialize location manager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Set up back button
        backButton.setOnClickListener {
            finish()
        }

        // Check and request location permission
        if (checkLocationPermission()) {
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    private fun updateDirectionText(degree: Float) {
        val direction = when {
            degree >= 337.5 || degree < 22.5 -> "North"
            degree >= 22.5 && degree < 67.5 -> "Northeast"
            degree >= 67.5 && degree < 112.5 -> "East"
            degree >= 112.5 && degree < 157.5 -> "Southeast"
            degree >= 157.5 && degree < 202.5 -> "South"
            degree >= 202.5 && degree < 247.5 -> "Southwest"
            degree >= 247.5 && degree < 292.5 -> "West"
            else -> "Northwest"
        }
        
        val accuracy = when {
            degree >= 355 || degree < 5 -> "Perfect"
            degree >= 350 || degree < 10 -> "Very Good"
            degree >= 340 || degree < 20 -> "Good"
            else -> "Fair"
        }
        
        // Show/hide Kaaba icon based on accuracy
        kaabaIcon.visibility = if (accuracy == "Perfect") View.VISIBLE else View.GONE
        
        directionText.text = "Qibla Direction: $direction (${degree.roundToInt()}°)\nAccuracy: $accuracy"
    }

    override fun onResume() {
        super.onResume()
        // Register sensors with fastest possible rate
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
        magnetometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST
        )
    }

    private fun startLocationUpdates() {
        if (checkLocationPermission()) {
            try {
                // Try to get last known location first
                getLastKnownLocation()

                // Request updates from all available providers
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        LOCATION_UPDATE_INTERVAL,
                        LOCATION_UPDATE_DISTANCE,
                        this
                    )
                }

                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        LOCATION_UPDATE_INTERVAL,
                        LOCATION_UPDATE_DISTANCE,
                        this
                    )
                }

                if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.PASSIVE_PROVIDER,
                        LOCATION_UPDATE_INTERVAL,
                        LOCATION_UPDATE_DISTANCE,
                        this
                    )
                }
            } catch (e: SecurityException) {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLastKnownLocation() {
        if (checkLocationPermission()) {
            try {
                // Try GPS first
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                    updateLocation(it)
                } ?: run {
                    // Try network provider if GPS is not available
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
                        updateLocation(it)
                    }
                }
            } catch (e: SecurityException) {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateLocation(location: Location) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastLocationUpdateTime < LOCATION_UPDATE_INTERVAL) {
            return
        }
        lastLocationUpdateTime = currentTime

        // Only update if the new location is more accurate or significantly different
        if (currentLocation == null || 
            location.accuracy < currentLocation!!.accuracy || 
            location.distanceTo(currentLocation!!) > LOCATION_UPDATE_DISTANCE) {
            
            currentLocation = location
            qiblaBearing = calculateBearing(
                location.latitude,
                location.longitude,
                QIBLA_LATITUDE,
                QIBLA_LONGITUDE
            )
            
            if (isFirstLocationUpdate) {
                isFirstLocationUpdate = false
                Toast.makeText(this, "Hold your device flat and rotate until the green arrow points up", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Location permission required for Qibla direction", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
            return
        }
        lastUpdateTime = currentTime

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            }
        }

        // Update compass only if we have both sensor readings
        if (accelerometerReading != null && magnetometerReading != null) {
            updateOrientationAngles()
        }
    }

    private fun updateOrientationAngles() {
        // Update rotation matrix
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        // Get orientation angles
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // Convert radians to degrees
        val azimuthInDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val normalizedAzimuth = (azimuthInDegrees + 360) % 360

        if (currentLocation != null) {
            val rotation = (qiblaBearing - normalizedAzimuth + 360) % 360
            updateCompassUI(rotation)
        }
    }

    private fun updateCompassUI(degree: Float) {
        // Direct rotation without animation for immediate response
        compassImage.rotation = -degree
        qiblaArrow.rotation = degree
        currentDegree = -degree

        // Update accuracy indicator
        val accuracy = when {
            degree >= 355 || degree < 5 -> 1.0f
            degree >= 350 || degree < 10 -> 0.8f
            degree >= 340 || degree < 20 -> 0.6f
            else -> 0.4f
        }

        accuracyIndicator.alpha = accuracy
        val scale = 0.5f + (accuracy * 0.5f)
        accuracyIndicator.scaleX = scale
        accuracyIndicator.scaleY = scale

        // Update direction text
        updateDirectionText(degree)
    }

    override fun onLocationChanged(location: Location) {
        updateLocation(location)
    }

    override fun onProviderDisabled(provider: String) {
        Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show()
    }

    override fun onProviderEnabled(provider: String) {
        // Not needed for this implementation
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle sensor accuracy changes if needed
        // For now, we don't need to do anything with this information
    }

    private fun calculateBearing(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Float {
        val dLng = Math.toRadians(endLng - startLng)
        val startLatRad = Math.toRadians(startLat)
        val endLatRad = Math.toRadians(endLat)

        val y = Math.sin(dLng) * Math.cos(endLatRad)
        val x = Math.cos(startLatRad) * Math.sin(endLatRad) -
                Math.sin(startLatRad) * Math.cos(endLatRad) * Math.cos(dLng)

        var bearing = Math.toDegrees(Math.atan2(y, x))
        if (bearing < 0) {
            bearing += 360
        }
        return bearing.toFloat()
    }
} 