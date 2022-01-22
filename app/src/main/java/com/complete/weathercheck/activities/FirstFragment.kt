package com.complete.weathercheck.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import com.complete.weathercheck.R
import android.net.Uri
import android.os.Build
import android.os.Bundle
import java.util.*
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.*
import retrofit.*
import android.os.Looper
import android.view.*
import androidx.annotation.RequiresApi

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.complete.weathercheck.models.WeatherResponse
import com.complete.weathercheck.network.WeatherService
import com.complete.weathercheck.utils.Constants
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.fragment_first.*
import java.text.SimpleDateFormat


class FirstFragment : Fragment(R.layout.fragment_first) {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    // A global variable for Progress Dialog
    private var mProgressDialog: Dialog? = null
    private var abc: String? = null

    // A global variable for Current Latitude
    private var mLatitude: Double = 0.0

    // A global variable for Current Longitude
    private var mLongitude: Double = 0.0

    private lateinit var mSharedPreferences: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mFusedLocationClient =
            activity?.let { LocationServices.getFusedLocationProviderClient(it) }!!


        mSharedPreferences =
            activity?.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)!!

        setupUI()
        // END

        Dexter.withActivity(activity)
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()) {
                        requestLocationData()
                    }

                    if (report.isAnyPermissionPermanentlyDenied) {
                        Toast.makeText(
                            activity,
                            "You have denied location permission. Please allow it is mandatory.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }
            }).onSameThread()
            .check()
        return View(activity)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                getLocationWeatherDetails()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun showRationalDialogForPermissions() {
        activity?.let {
            AlertDialog.Builder(it)
                .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
                .setPositiveButton(
                    "GO TO SETTINGS"
                ) { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("packagename", abc, null)
                        intent.data = uri
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }
                .setNegativeButton("Cancel") { dialog,
                                               _ ->
                    dialog.dismiss()
                }.show()
        }
    }


    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = PRIORITY_HIGH_ACCURACY

        mFusedLocationClient =
            activity?.let { LocationServices.getFusedLocationProviderClient(it) }!!
        Looper.myLooper()?.let {
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                it
            )
        }
    }


    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {

            val mLastLocation: Location = locationResult.lastLocation
            mLatitude = mLastLocation.latitude
            Log.e("Current Latitude", "$mLatitude")
            mLongitude = mLastLocation.longitude
            Log.e("Current Longitude", "$mLongitude")

            getLocationWeatherDetails()
        }
    }


    private fun getLocationWeatherDetails() {

        if (activity?.let { Constants.isNetworkAvailable(it) } == true) {


            val retrofit: Retrofit = Retrofit.Builder()
                // API base URL.
                .baseUrl(Constants.BASE_URL)

                .addConverterFactory(GsonConverterFactory.create())

                .build()

            val service: WeatherService =
                retrofit.create(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                mLatitude, mLongitude, Constants.METRIC_UNIT, Constants.APP_ID
            )

            showCustomProgressDialog() // Used to show the progress dialog


            listCall.enqueue(object : Callback<WeatherResponse> {
                @RequiresApi(Build.VERSION_CODES.N)
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    response: Response<WeatherResponse>,
                    retrofit: Retrofit
                ) {

                    // Check weather the response is success or not.
                    if (response.isSuccess) {

                        hideProgressDialog() // Hides the progress dialog


                        val weatherList: WeatherResponse = response.body()
                        Log.i("Response Result", "$weatherList")

                        val weatherResponseJsonString = Gson().toJson(weatherList)
                        // Save the converted string to shared preferences
                        val editor = mSharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA, weatherResponseJsonString)
                        editor.apply()
                        Log.i("requiredresult", "$weatherList")
                        setupUI()
                        // END
                    } else {
                        // If the response is not success then we check the response code.
                        when (response.code()) {
                            400 -> {
                                Log.e("Error 400", "Bad Request")
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error", "Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(t: Throwable) {
                    hideProgressDialog() // Hides the progress dialog
                    Log.e("Errorrrrr", t.message.toString())
                }
            })
        } else {
            Toast.makeText(
                activity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showCustomProgressDialog() {
        mProgressDialog = activity?.let { Dialog(it) }

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        mProgressDialog!!.show()
    }

    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("SetTextI18n")
    private fun setupUI() {

        val weatherResponseJsonString =
            mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA, "")

        if (!weatherResponseJsonString.isNullOrEmpty()) {

            val weatherList =
                Gson().fromJson(weatherResponseJsonString, WeatherResponse::class.java)

            // For loop to get the required data. And all are populated in the UI.
            for (z in weatherList.weather.indices) {
                Log.i("NAMEEEEEEEE", weatherList.weather[0].main)

                tv_main.text = weatherList.weather[0].main
                tv_main_description.text = weatherList.weather[0].description
                tv_temp.text =
                    weatherList.main.temp.toString() + getUnit(activity)
                tv_humidity.text = weatherList.main.humidity.toString() + " per cent"
                tv_min.text = weatherList.main.tempMin.toString() + " min"
                tv_max.text = weatherList.main.tempMax.toString() + " max"
                tv_speed.text = weatherList.wind.speed.toString()
                tv_name.text = weatherList.name
                tv_country.text = weatherList.sys.country
                tv_sunrise_time.text = unixTime(weatherList.sys.sunrise.toLong())
                tv_sunset_time.text = unixTime(weatherList.sys.sunset.toLong())

                // Here we update the main icon
                when (weatherList.weather[0].icon) {
                    "01d" -> iv_main.setImageResource(R.drawable.sunny)
                    "02d" -> iv_main.setImageResource(R.drawable.cloud)
                    "03d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10d" -> iv_main.setImageResource(R.drawable.rain)
                    "11d" -> iv_main.setImageResource(R.drawable.storm)
                    "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                    "01n" -> iv_main.setImageResource(R.drawable.cloud)
                    "02n" -> iv_main.setImageResource(R.drawable.cloud)
                    "03n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10n" -> iv_main.setImageResource(R.drawable.cloud)
                    "11n" -> iv_main.setImageResource(R.drawable.rain)
                    "13n" -> iv_main.setImageResource(R.drawable.snowflake)
                }
            }
        }
        // END
    }


    private fun getUnit(value: FragmentActivity?): String {
        Log.i("unitttttt", value.toString())
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat") val sdf =
            SimpleDateFormat("HH:mm:ss")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
  // return inflater.inflate(R.layout.fragment_first, container, false)
}





