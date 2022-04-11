package de.fklim.tenki

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.weatherapp.models.WeatherResponse
import de.fklim.tenki.databinding.ActivityMainBinding
import de.fklim.tenki.network.RetrofitInstance
import de.fklim.tenki.util.Constants
import de.fklim.tenki.util.MetricMgt
import de.fklim.tenki.util.SensitiveConstants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


private lateinit var binding: ActivityMainBinding

@RequiresApi(Build.VERSION_CODES.N)
class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog: Dialog?= null

    lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)

        checkPermissions()

        binding.fBtnRefresh.setOnClickListener {

            binding.fBtnRefresh.animate().apply {
                rotationBy(360f)
                duration = 1000
            }.start()

            checkPermissions()
        }
    }

    private fun setupUI() {
        val weatherResponseJsonString = mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA, "")

        if (!weatherResponseJsonString.isNullOrEmpty()) {
            val weatherList =
                Gson().fromJson(weatherResponseJsonString, WeatherResponse::class.java)


            for (currentElement in weatherList.weather.indices) {


                binding.tvMain.text = weatherList.weather[currentElement].main
                binding.tvMainDescription.text = weatherList.weather[currentElement].description

                binding.tvHumidity.text = weatherList.main.humidity.toString() + "%"

                val temperatureSuffix =
                    MetricMgt.getTemperatureUnit(application.resources.configuration.locales.toString())
                binding.tvTemp.text = weatherList.main.temp.toString() + temperatureSuffix
                binding.tvMin.text =
                    "min " + weatherList.main.temp_min.toString() + temperatureSuffix
                binding.tvMax.text =
                    "max " + weatherList.main.temp_max.toString() + temperatureSuffix

                binding.tvSpeed.text = weatherList.wind.speed.toString()

                binding.tvName.text = weatherList.name
                binding.tvCountry.text = weatherList.sys.country

                binding.tvSunriseTime.text = MetricMgt.unixTime(weatherList.sys.sunrise.toLong())
                binding.tvSunsetTime.text = MetricMgt.unixTime(weatherList.sys.sunset.toLong())

                when (weatherList.weather[currentElement].icon) {
                    "01d" -> binding.ivMain.setImageResource(R.drawable.sunny)
                    "02d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "03d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "04d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "04n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "10d" -> binding.ivMain.setImageResource(R.drawable.rain)
                    "11d" -> binding.ivMain.setImageResource(R.drawable.storm)
                    "13d" -> binding.ivMain.setImageResource(R.drawable.snowflake)
                    "01n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "02n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "03n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "10n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "11n" -> binding.ivMain.setImageResource(R.drawable.rain)
                    "13n" -> binding.ivMain.setImageResource(R.drawable.snowflake)
                }
            }
        }

    }

    private fun checkPermissions() {
        /**
         * Check location service permissions with Dexter
         */
        if(!isLocationEnabled()) {
            Toast.makeText(this, R.string.disabled_location_service, Toast.LENGTH_SHORT).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if(report!!.areAllPermissionsGranted() ) {
                            requestLocationData()
                        }

                        if(report!!.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(this@MainActivity,
                                R.string.denied_location_service, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        showRationalDialogForPermission()
                    }

                }).onSameThread().check()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    private fun showRationalDialogForPermission() {
        // If the permission missing jump right into the settings to let the user set the permission
        AlertDialog.Builder(this)
            .setMessage(R.string.permission_denied_limited_usability)
            .setPositiveButton(R.string.go_to_settings) {_,_ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton(R.string.action_cancel) {
                dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private val mLocationCallback = object: LocationCallback() {
        override fun onLocationResult(locResult: LocationResult) {
            // Unused
            //val mLastLocation: Location = locResult.lastLocation
            val mLastLocationLatitude = locResult.lastLocation.latitude
            val mLastLocationLongitude = locResult.lastLocation.longitude

            getLocationWeatherDetails(mLastLocationLatitude, mLastLocationLongitude)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        // call when permissions are granted
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,mLocationCallback, Looper.myLooper())
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        if (Constants.isNetworkAvailable(this)) {

            val listCall: Call<WeatherResponse> = RetrofitInstance.api.getWeather(
                latitude, longitude, Constants.METRIC_UNIT, SensitiveConstants.API_KEY
            )

            showCustomProgressDialog()

            // Feedback when finished
            listCall.enqueue(object: Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if(response!!.isSuccessful) {
                        val weatherList: WeatherResponse = response.body()!!
                        Log.i("Response Result", "$weatherList")
                        hideCustomProgressDialog()

                        val weatherResponseJsonString = Gson().toJson(weatherList)
                        val editor = mSharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA,weatherResponseJsonString).apply()

                        setupUI()


                    } else {
                        when(response.code()) {
                            400 -> Log.e("Error 400", "Bad Request")
                            404 -> Log.e("Error 404","Not found")
                            else -> Log.e("Error","Error")

                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("ErrorNetwork", t.message.toString())
                    hideCustomProgressDialog()
                }

            })

        } else {
            Toast.makeText(this, R.string.failed_connection, Toast.LENGTH_LONG).show()
        }
    }

    private fun showCustomProgressDialog() {
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    private fun hideCustomProgressDialog() {
        if (mProgressDialog!= null) {
            mProgressDialog!!.dismiss()
        }
    }
}

