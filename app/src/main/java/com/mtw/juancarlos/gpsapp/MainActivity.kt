package com.mtw.juancarlos.gpsapp

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorInflater
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    // TODO (2) SOLICITAR PERMISOS EN TIEMPO DE EJECUCION
    companion object {
        private val REQUEST_LOCATION_PERMISSION = 1

        private val REQUEST_CHECK_SETTINGS = 1
    }

    // TODO (3.1) CLASE PRINCIPAL PAR USAR EL GPS
    private lateinit var fusedLocationClient : FusedLocationProviderClient

    // NOS VA A INDICAR SI ESTA O NO EN FUNCIONAMIENTO EL TRACKING
    private var requestingLocationUpdates = false

    private val animRotate by lazy {
        AnimatorInflater.loadAnimator(this, R.animator.rotate)
    }

    // TODO 6.1
    private lateinit var locationCallback: LocationCallback

    /**
     * FUNCION PARA VERIFICAR PERMISOS. DEVUELVE TRUE SI EL USUARIO OTORGÓ PERMISOS
     */
    private fun checkPermission() : Boolean {
        // EN ESTE CASO USAREMOS ACCESS_FINE_LOCATION, PERO HAY VARIOS ESQUEMAS DE PERMISOS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        } else {
            // EN CASO DE NO TENER EL PERMISO, ENTONCES SE ABRE EL DIALOGO Y EL DATO DE RESPUESTA DEL
            // DIALOGO SE ALMACENA EN LA VARIABLE REQUEST_LOCATION_PERMISSION
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
            return false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // related task you need to do.
                    // OBTENER LA GEOLOCALIZACION
                    getLastLocation()
                } else {
                    // EN CASO DE NO OTORGAR PERMISO ENTONCES DESPLEGAR UN MENSAJE Y TAMBIEN SE
                    // PUEDEN DESHABILITAR LAS FUNCIONALIDADES
                    Toast.makeText(this, "Acceso al GPS denegado", Toast.LENGTH_SHORT).show()
                }
                return
            }
        // Add other 'when' lines to check for other
        // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar.visibility = View.INVISIBLE

        // TODO 3.2 INSTANCIA LA CLASE DE LA GEOFERENCIACION
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        button_location.setOnClickListener {
            if (checkPermission()) {
                Log.i("GPSAPP", "ACCESO CONCEDIDO")
                getLastLocation()
            } else {
                Log.i("GPSAPP", "ACCESO DENEGADO")
            }
        }

        button_startTrack.setOnClickListener {
            if (checkPermission()) {
                if (!requestingLocationUpdates) {
                    startLocationUpdates()
                } else {
                    stopLocationUpdates()
                }
            } else {
                Log.i("GPSAPP", "ACCESO DENEGADO")
            }
        }

        animRotate.setTarget(imageview_android)

        // TODO 6.2
        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult : LocationResult?) {
                // SI ES NULL QUE SE SALGA DEL METODO
                locationResult ?: return
                // EN CASO CONTRARIO:
                onLocationChanged(locationResult.lastLocation)
            }
        }

    }

    // TODO 3.3 METODO PARA OBTENER ULTIMO PUNTO DE GEOLOCALIZACION
    private fun getLastLocation() {
        try {
            fusedLocationClient.lastLocation
                    .addOnSuccessListener {
                        location : Location? ->
                            Log.i("GPSAPP", "OnSuccessListener lastLocation")
                            onLocationChanged(location)
                    }
                    .addOnFailureListener {
                        Log.i("GPSAPP", "OnFailureListener lastLocation")
                        Toast.makeText(this@MainActivity, "Error en la lectura del GPS", Toast.LENGTH_LONG).show()
                    }
        } catch (e : SecurityException) {
            Toast.makeText(this@MainActivity, "SecEx: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun onLocationChanged(location : Location?) {
        if (location != null) {
            // SE USA UN RECURSO DE STRINGS PERO FORMATEADO/ENMARCARADO Y SE SUSTITUYE CON LOS VALORES SIGUIENTES
            // textview_location.text = getString(R.string.location_text, location?.latitude.toString(), location?.longitude.toString(), location?.altitude.toString())
            textview_location.text = "Latitud: " + location?.latitude + " Longitud: " + location?.longitude + " Altitud: " + location?.altitude
        } else {
            textview_location.text = "No se recuperó la ubicación"
        }
    }

    private fun startLocationUpdates() {
        progressBar.visibility = View.VISIBLE

        animRotate.start()
        requestingLocationUpdates = true
        button_startTrack.text = "Detener"
        textview_location.text = "Localizando ..."


        /**************************************************************/
        // TODO 5.2:

        val locationRequest = LocationRequest().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())


        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
            Log.e("GPSANDROIDAPP", "OnSucessListener Task")
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,null)

            } catch (e: SecurityException) {
                Log.e("GPSANDROIDAPP", "SecEx "+ e.message)
                Toast.makeText(this@MainActivity,"secex:"+ e.message,Toast.LENGTH_LONG)
                progressBar.visibility = View.INVISIBLE
            }
        }

        task.addOnFailureListener { exception ->
            progressBar.visibility = View.INVISIBLE
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    Log.e("GPSAPP", "OnFailureListener")
                    exception.startResolutionForResult(this@MainActivity, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    // TODO 5.3
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode){
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK) {
                    Log.e("GPSAPP","CONFIGURACION DEL GPS CORRECTA")
                    startLocationUpdates()
                }
                return
            }
        }
    }

    private fun stopLocationUpdates() {
        if (requestingLocationUpdates) {
            progressBar.visibility = View.INVISIBLE

            animRotate.end()
            requestingLocationUpdates = false
            button_startTrack.text = "Rastrear"
            textview_location.text = "Presiona el botón para detener la ultima ubicacion"

            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onResume() {
        if (requestingLocationUpdates) startLocationUpdates()
        super.onResume()
    }

    override fun onPause() {
        if (requestingLocationUpdates) {
            stopLocationUpdates()
            requestingLocationUpdates = false
        }
        super.onPause()
    }

}
