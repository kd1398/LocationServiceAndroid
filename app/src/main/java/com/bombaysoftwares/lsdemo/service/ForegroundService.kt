package com.bombaysoftwares.lsdemo.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.bombaysoftwares.lsdemo.R
import com.bombaysoftwares.lsdemo.models.CurrentLocation
import com.bombaysoftwares.lsdemo.ui.MapActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class ForegroundService : Service() {
    private val CHANNEL_ID = "ForegroundService Kotlin"
    var latitude = 0.0
    var longitude = 0.0
    lateinit var rootNode: FirebaseDatabase
    lateinit var database: DatabaseReference
    var user = ""

    companion object {
        fun startService(context: Context, stringExtra: String?) {
            val startIntent = Intent(context, ForegroundService::class.java)
            startIntent.putExtra("inputExtra", stringExtra)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                // Pre-O behavior.
                context.startService(startIntent)
            }

        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, ForegroundService::class.java)
            context.stopService(stopIntent)
            Log.e("stop", "service stops")
        }
    }

    override fun onCreate() {
        super.onCreate()

        rootNode = FirebaseDatabase.getInstance()
        database = rootNode.getReference("Location")
        showNotification(latitude, longitude)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        user = intent!!.getStringExtra("inputExtra").toString()

        getCurrentLocation(this)
        startMapActivity()

        return START_STICKY
    }

    private fun startMapActivity() {
        val i = Intent()
        i.setClass(this, MapActivity::class.java)
        i.putExtra("user", user)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }

    private fun showNotification(latitude: Double, longitude: Double) {

        createNotificationChannel()
        val notificationIntent = Intent(this, MapActivity::class.java)
        notificationIntent.putExtra("user", user)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Service")
            .setContentText("Latitude = $latitude Longitude = $longitude")
            .setSmallIcon(R.drawable.ic_baseline_notifications_active_24)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    private fun addDataToDatabase() {
        database.child(user).setValue(
            CurrentLocation(latitude, longitude)
        )
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }

    private fun getCurrentLocation(context: Context?) {
        if (ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val criteria = Criteria()
            criteria.accuracy = Criteria.ACCURACY_FINE
            criteria.isSpeedRequired = true

            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val provider = locationManager.getBestProvider(criteria, true)
            Toast.makeText(context, "$provider", Toast.LENGTH_SHORT).show()
            if (provider != null) {
                locationManager.requestLocationUpdates(
                    provider, 1, 0.1f, object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            latitude = location.latitude
                            longitude = location.longitude
                            addDataToDatabase()
                            showNotification(latitude, longitude)
                            Log.e("tag", "on location changed")
                        }

                        override fun onProviderDisabled(provider: String) {
                            Log.e("tag", "provider disable")
                        }

                        override fun onProviderEnabled(provider: String) {
                            Log.e("tag", "provider enable")
                        }

                        override fun onStatusChanged(
                            provider: String?,
                            status: Int,
                            extras: Bundle?
                        ) {
                        }

                    })
            }
        }
    }
}