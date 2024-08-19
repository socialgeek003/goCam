package com.lib.gocam.latlng

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CountDownTimer
import android.os.Looper
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.lib.gocam.R
import com.lib.gocam.utility.Utils

class LatLngModule {
    private var context: Context? = null
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    var latLngRead: LatLngRead? = null

    var countDownTimer: CountDownTimer = object : CountDownTimer(15000, 30000) {
        override fun onTick(millisUntilFinished: Long) {}
        override fun onFinish() {
            latLngRead?.getLatLng(getDummyLocation())
        }
    }

    private fun getDummyLocation(): Location? {
        val loc = Location("dummyprovider")
        loc.latitude = 0.0
        loc.longitude = 0.0
        return loc
    }

    constructor(context: Context?, latLngRead: LatLngRead?) {
        this.context = context
        this.latLngRead = latLngRead
        mFusedLocationClient = context?.let { LocationServices.getFusedLocationProviderClient(it) }
    }

    fun startLocation(): Boolean {
        val lm = context!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gps_enabled = false
        var network_enabled = false
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
        }
        return if (!gps_enabled && !network_enabled) {
            // notify user
            val dialog = AlertDialog.Builder(context)
            dialog.setMessage(context!!.resources.getString(R.string.gpsmsg))
            dialog.setPositiveButton(
                context!!.resources.getString(R.string.open_location_settings)
            ) { paramDialogInterface, paramInt -> // TODO Auto-generated method stub
                val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context!!.startActivity(myIntent)
                //get gps
                paramDialogInterface.dismiss()
            }
            dialog.setNegativeButton(
                context!!.getString(R.string.Cancel)
            ) { paramDialogInterface, paramInt -> // TODO Auto-generated method stub
                paramDialogInterface.dismiss()
            }
            val alert11 = dialog.create()
            alert11.setOnShowListener {
                alert11.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(context!!.resources.getColor(R.color.dialog_color))
                alert11.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(context!!.resources.getColor(R.color.dialog_color))
            }
            if (!(context as Activity?)!!.isFinishing) {
                alert11.show()
            }
            true
        } else {
            if (ActivityCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context!!,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return true
            }
            countDownTimer.start()

            val locationRequest: LocationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.fastestInterval = 2000

            var locationCallback: LocationCallback? = null
            locationCallback= object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    if (locationResult == null) {
                        Utils.printMessage("Current Location :- $locationResult")
                        return
                    }
                    for (location in locationResult.locations) {
                        if (location != null) {
                            Utils.printMessage("Current Location :- lat: " + location.latitude + " long: " + location.longitude)
                            countDownTimer.cancel()
                            latLngRead!!.getLatLng(location)
                            if (mFusedLocationClient != null) {
                                mFusedLocationClient!!.removeLocationUpdates(locationCallback)
                            }
                        } else {
                            Utils.printMessage("Current Location :- $location")
                            countDownTimer.cancel()
                            latLngRead!!.getLatLng(getDummyLocation())
                            if (mFusedLocationClient != null) {
                                mFusedLocationClient!!.removeLocationUpdates(locationCallback)
                            }
                        }
                        return
                    }
                }
            }
            mFusedLocationClient!!.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )

            return false
        }
    }
}