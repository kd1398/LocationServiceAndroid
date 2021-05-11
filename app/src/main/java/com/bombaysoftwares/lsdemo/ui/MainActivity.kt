package com.bombaysoftwares.lsdemo.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bombaysoftwares.lsdemo.R
import com.bombaysoftwares.lsdemo.customs.Constant
import com.bombaysoftwares.lsdemo.customs.Utils.Companion.isOnline
import com.bombaysoftwares.lsdemo.service.ForegroundService
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE_LOCATION_PERMISSION: Int = 101
    var uName = ""
    private val REQUEST_CHECK_SETTINGS = 111

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (intent.hasExtra(Constant.KEY_USER)) {
            uName = intent.getStringExtra(Constant.KEY_USER).toString()
        }

        buttonStartService.setOnClickListener {
            if (!isOnline(this)) {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
                    val permissionList =
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    requestPermissions(
                        permissionList,
                        REQUEST_CODE_LOCATION_PERMISSION
                    )
                } else {
                    showGpsEnablePopup()
                }
            }
        }
        buttonStop.setOnClickListener {
            ForegroundService.stopService(this)
        }

        btnOpenMap.setOnClickListener {
            startMapActivityWithExistedUser(uName)
        }
    }

    private fun startMapActivityWithExistedUser(uName: String) {
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra("notservice",true)
        intent.putExtra("existingUser",uName)
        startActivity(intent)
    }

    private fun showGpsEnablePopup() {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        builder.setAlwaysShow(true) //this displays dialog box like Google Maps with two buttons - OK and NO,THANKS

        val task =
            LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())

        task.addOnCompleteListener {
            try {
                val response = task.getResult(ApiException::class.java)
                // All location settings are satisfied. The client can initialize location
                // requests here.
                ForegroundService.startService(this, uName)
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->                             // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.
                        try {
                            // Cast to a resolvable exception.
                            val resolvable = exception as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                this,
                                REQUEST_CHECK_SETTINGS
                            )
                        } catch (e: SendIntentException) {
                            // Ignore the error.
                        }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    }
                }
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.size >= 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                showGpsEnablePopup()
            } else {
                Toast.makeText(this, "Permission Denied !", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    ForegroundService.startService(
                        this,
                        uName
                    )
                }
                Activity.RESULT_CANCELED -> {
                    Toast.makeText(this, "Gps is required, please turn it on", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}