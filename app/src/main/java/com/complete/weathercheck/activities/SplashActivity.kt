package com.complete.weathercheck.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.complete.weathercheck.databinding.ActivitySplashBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

@SuppressLint("CustomSplashScreen")
class SplashActivity :Activity() {
    private var longitude: Double = 0.0
    private var latitude: Double = 0.0

    private var _binding: ActivitySplashBinding? = null
    private val binding : ActivitySplashBinding get() = _binding!!

    private var fusedLocationClient: FusedLocationProviderClient? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()
        //setting shared prefs for location
        getSharedPreferences(SHARED, Context.MODE_PRIVATE).edit().apply{
            putString("longitude",longitude.toString())
            putString("latitude",latitude.toString())
            apply()
        }
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        //taking delay to splash screen
        Handler().postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 1000)
    }
    //checking the permissions for location
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                /** Alert Dialogue Box to Ask the user for location permission  */
                AlertDialog.Builder(this)
                    .setTitle("Required Location Permission")
                    .setMessage("Grant Permission To Use Current Location, Else default Location will be used")
                    .setPositiveButton("ok") { _: DialogInterface?, _: Int ->

                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            REQUEST_CODE
                        )
                        // permission granted
                        fusedLocationClient?.lastLocation?.addOnSuccessListener {
                            it.let {
                                longitude = it.longitude
                                latitude = it.latitude
                            }
                        }
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    .setNegativeButton("cancel") { _: DialogInterface?, _: Int ->
                        // permission not granted
                        Toast.makeText(
                            applicationContext,
                            "Kindly Grant Permission to proceed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }.create().show()
            } else {
                // request the permission
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_CODE
                )

            }
        }else{
            getCurrentLocation()
        }
    }
    //getting the longitude and latitude
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // permission hasn't been granted
            checkLocationPermission()
        } else {
            fusedLocationClient?.lastLocation?.addOnSuccessListener {
                if(it != null){
                    longitude = it.longitude
                    latitude = it.latitude
                    Log.d(TAG+"1",longitude.toString())
                    Log.d(TAG+"1",latitude.toString())
                }else{
                    Log.d(TAG,"else")
                }
            }
        }
    }
    //null the binding
    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
    companion object{
        const val REQUEST_CODE = 1
        const val SHARED = "shared"
    }
}