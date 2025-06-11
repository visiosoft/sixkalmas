package com.mpo.sixkalmas

import android.Manifest
import android.content.Context
import android.content.Intent
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
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mpo.sixkalmas.databinding.ActivityQiblaFinderBinding
import kotlin.math.roundToInt

class QiblaFinderActivity : AppCompatActivity(), SensorEventListener, LocationListener {
    private lateinit var binding: ActivityQiblaFinderBinding
    private lateinit var compassImage: ImageView
    private lateinit var directionText: TextView
    private lateinit var qiblaArrow: ImageView
    private lateinit var accuracyIndicator: View
    private lateinit var kaabaIcon: ImageView
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
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
    private var qiblaLocation: Location? = null
    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false
    private var qiblaDegree = 0f
    private var isFirstUpdate = true
    private val orientation = FloatArray(3)
    private val smoothedOrientation = FloatArray(3)
    private val remappedRotationMatrix = FloatArray(9)
    private val SENSOR_UPDATE_INTERVAL = 16L // ~60fps
    private val ALPHA = 0.1f // Low-pass filter coefficient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1
        private const val QIBLA_LATITUDE = 21.4225
        private const val QIBLA_LONGITUDE = 39.8262
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQiblaFinderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize views first
        initializeViews()
        
        // Initialize sensors in background
        initializeSensors()
        
        // Initialize location services
        initializeLocation()
    }

    private fun initializeViews() {
        compassImage = binding.compassImage
        directionText = binding.directionText
        qiblaArrow = binding.qiblaArrow
        accuracyIndicator = binding.accuracyIndicator
        kaabaIcon = binding.kaabaIcon
    }

    private fun initializeSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun initializeLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Set up Kaaba location
        qiblaLocation = Location("").apply {
            latitude = QIBLA_LATITUDE
            longitude = QIBLA_LONGITUDE
        }

        // Check and request location permission
        if (checkLocationPermission()) {
            getCurrentLocation()
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
        // Reset sensor data
        lastAccelerometerSet = false
        lastMagnetometerSet = false
        isFirstUpdate = true

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

    private fun getCurrentLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = it
                    updateQiblaDirection()
                }
            }
        }
    }

    private fun updateQiblaDirection() {
        currentLocation?.let { current ->
            qiblaLocation?.let { qibla ->
                val bearing = current.bearingTo(qibla)
                qiblaBearing = bearing
                
                // Update qibla arrow rotation
                val qiblaAnimation = RotateAnimation(
                    0f,
                    bearing,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
                ).apply {
                    duration = 250
                    fillAfter = true
                    interpolator = android.view.animation.LinearInterpolator()
                }
                qiblaArrow.startAnimation(qiblaAnimation)
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
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission required for Qibla direction", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime < SENSOR_UPDATE_INTERVAL) {
            return
        }
        lastUpdateTime = currentTime

        if (event.sensor == accelerometer) {
            // Apply low-pass filter to accelerometer data
            for (i in 0..2) {
                lastAccelerometer[i] = lastAccelerometer[i] * (1 - ALPHA) + event.values[i] * ALPHA
            }
            lastAccelerometerSet = true
        } else if (event.sensor == magnetometer) {
            // Apply low-pass filter to magnetometer data
            for (i in 0..2) {
                lastMagnetometer[i] = lastMagnetometer[i] * (1 - ALPHA) + event.values[i] * ALPHA
            }
            lastMagnetometerSet = true
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            // Get rotation matrix
            if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)) {
                // Remap coordinate system to match device orientation
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Z,
                    remappedRotationMatrix
                )

                // Get orientation angles
                SensorManager.getOrientation(remappedRotationMatrix, orientation)

                // Apply low-pass filter to orientation
                for (i in 0..2) {
                    smoothedOrientation[i] = smoothedOrientation[i] * (1 - ALPHA) + orientation[i] * ALPHA
                }

                // Convert radians to degrees and normalize
                val azimuthInDegrees = Math.toDegrees(smoothedOrientation[0].toDouble()).toFloat()
                val normalizedAzimuth = (azimuthInDegrees + 360) % 360

                // Calculate qibla direction
                var qiblaDirection = normalizedAzimuth
                if (currentLocation != null && qiblaLocation != null) {
                    val bearing = currentLocation!!.bearingTo(qiblaLocation!!)
                    qiblaDirection = (bearing - normalizedAzimuth + 360) % 360
                }

                // Update compass rotation
                updateCompassRotation(qiblaDirection)
            }
        }
    }

    private fun updateCompassRotation(degree: Float) {
        if (isFirstUpdate) {
            currentDegree = degree
            isFirstUpdate = false
        }

        // Calculate the shortest rotation path
        var rotation = degree - currentDegree
        if (rotation > 180) rotation -= 360
        if (rotation < -180) rotation += 360

        // Only update if the change is significant enough
        if (Math.abs(rotation) > 0.5f) {
            // Create and apply rotation animation
            val rotateAnimation = RotateAnimation(
                currentDegree,
                degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 250
                fillAfter = true
                interpolator = android.view.animation.LinearInterpolator()
            }

            compassImage.startAnimation(rotateAnimation)
            currentDegree = degree

            // Update direction text
            updateDirectionText(degree)
        }
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

    private fun showInterstitialAd(onAdClosed: () -> Unit) {
        AdManager.getInstance().showInterstitialAd(this) {
            onAdClosed()
        }
    }
} 