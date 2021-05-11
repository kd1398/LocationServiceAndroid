package com.bombaysoftwares.lsdemo.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bombaysoftwares.lsdemo.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    lateinit var databaseReference: DatabaseReference
    var previousLatLng: LatLng? = null
    var currentLatLng: LatLng? = null
    private var polyline1: Polyline? = null

    private val polylinePoints: ArrayList<LatLng> = ArrayList()
    private var mCurrLocationMarker: Marker? = null

    var user = ""
    var isNotService = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (intent.hasExtra("notservice")) {
            if (intent.hasExtra("existingUser")) {
                isNotService = true
                user = intent.getStringExtra("existingUser").toString()
            }
        } else {
            isNotService = false
            if (intent.hasExtra("user")) {
                user = intent.getStringExtra("user").toString()
            }
        }
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

        if (isNotService) {
            getLocationOfUser()
        } else {
            setPolylines()
            fetchUpdatedLocation()
        }
    }

    private fun getLocationOfUser() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Location")
        databaseReference.child(user).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latLng = LatLng(
                    snapshot.child("latitude").getValue(Double::class.java)!!.toDouble(),
                    snapshot.child(
                        "longitude"
                    ).getValue(Double::class.java)!!.toDouble()
                )
                mMap.addMarker(
                    MarkerOptions().position(latLng)
                        .icon(bitmapFromVector(applicationContext, R.drawable.ic_bike))
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun setPolylines() {
        val polylineOptions = PolylineOptions()
        polylineOptions.color(resources.getColor(R.color.design_default_color_primary))
        polylineOptions.geodesic(true)

        polyline1 = mMap.addPolyline(polylineOptions.addAll(polylinePoints))
    }

    private fun fetchUpdatedLocation() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Location")
        databaseReference.child(user).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateMap(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun updateMap(dataSnapshot: DataSnapshot) {
        var latitude = 0.0
        var longitude = 0.0
        val data = dataSnapshot.childrenCount
        for (d in 0 until data) {
            latitude = dataSnapshot.child("latitude").getValue(Double::class.java)!!.toDouble()
            longitude = dataSnapshot.child("longitude").getValue(Double::class.java)!!.toDouble()
        }
        currentLatLng = LatLng(latitude, longitude)
        if (previousLatLng == null || previousLatLng !== currentLatLng) {
            // add marker line
            previousLatLng = currentLatLng
            polylinePoints.add(currentLatLng!!)
            polyline1!!.points = polylinePoints

            if (mCurrLocationMarker != null) {
                mCurrLocationMarker!!.position = currentLatLng!!
            } else {
                mCurrLocationMarker = mMap.addMarker(
                    MarkerOptions()
                        .position(currentLatLng!!)
                        .icon(bitmapFromVector(applicationContext, R.drawable.ic_rider))
                )
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng!!, 16f))
        }
    }

    private fun bitmapFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        // below line is use to generate a drawable.
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)

        // below line is use to set bounds to our vector drawable.
        vectorDrawable!!.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )

        // below line is use to create a bitmap for our
        // drawable which we have added.
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        // below line is use to add bitmap in our canvas.
        val canvas = Canvas(bitmap)

        // below line is use to draw our
        // vector drawable in canvas.
        vectorDrawable.draw(canvas)

        // after generating our bitmap we are returning our bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}