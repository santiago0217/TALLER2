package com.icm2630.taller2.ui.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.icm2630.taller2.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MapViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    // SENSOR DE LUMINOSIDAD
    private val sensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    var luminosidad by mutableFloatStateOf(100f)
        private set
    private var sensorIniciado = false

    // Usa dos umbrales para que el estilo no parpadee cuando la luz ronda los 50 lux
    var modoOscuro by mutableStateOf(false)
        private set

    val estiloOscuro: MapStyleOptions =
        MapStyleOptions.loadRawResourceStyle(application, R.raw.map_dark)

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_LIGHT) {
                val lux = event.values[0]
                luminosidad = lux
                modoOscuro = when {
                    !sensorIniciado -> lux < UMBRAL_LUX
                    modoOscuro -> lux < UMBRAL_LUX_CLARO
                    else -> lux < UMBRAL_LUX_OSCURO
                }
                sensorIniciado = true
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun iniciarSensor() {
        if (lightSensor != null) {
            sensorManager.registerListener(
                sensorListener,
                lightSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    fun detenerSensor() {
        if (lightSensor != null) {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    // PERMISOS Y GPS
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(application)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    var permissionGranted by mutableStateOf(hasLocationPermission())
        private set

    fun onPermisosRespondidos() {
        permissionGranted = hasLocationPermission()
    }

    var currentLocation by mutableStateOf<LatLng?>(null)
        private set
    val puntosRecorridos = mutableStateListOf<LatLng>()
    var cameraCentered by mutableStateOf(false)
        private set

    fun marcarCamaraCentrada() {
        cameraCentered = true
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                val nuevaUbicacion = LatLng(location.latitude, location.longitude)
                currentLocation = nuevaUbicacion
                if (puntosRecorridos.isEmpty() || puntosRecorridos.last() != nuevaUbicacion) {
                    puntosRecorridos.add(nuevaUbicacion)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun iniciarUbicacion(): Boolean {
        if (!permissionGranted || !hasLocationPermission()) return false

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).setMinUpdateIntervalMillis(2000L).build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
        return true
    }

    fun detenerUbicacion() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // GEOCODER: BUSQUEDA DE DIRECCIONES
    var direccion by mutableStateOf(savedStateHandle.get<String>(KEY_DIRECCION) ?: "")
        private set
    var ubicacionBuscada by mutableStateOf<LatLng?>(null)
        private set
    var nombreUbicacion by mutableStateOf("")
        private set
    var mensajeBusqueda by mutableStateOf<String?>(null)
        private set
    var buscando by mutableStateOf(false)
        private set

    val puedeBuscar: Boolean
        get() = direccion.trim().isNotEmpty() && !buscando

    fun onDireccionChange(valor: String) {
        direccion = valor
        savedStateHandle[KEY_DIRECCION] = valor
    }

    // Se ejecuta en el scope de la pantalla para poder animar la camara del mapa
    suspend fun buscarDireccion(onEncontrada: suspend (LatLng) -> Unit) {
        val textoBuscado = direccion.trim()
        if (textoBuscado.isEmpty() || buscando) return

        buscando = true
        mensajeBusqueda = null
        try {
            if (!Geocoder.isPresent()) {
                mensajeBusqueda = context.getString(R.string.address_search_error)
                return
            }

            val resultados = withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                Geocoder(context, LOCALE_COLOMBIA)
                    .getFromLocationName(textoBuscado, 1)
            }
            val lugar = resultados?.firstOrNull()
            if (lugar != null) {
                val posicion = LatLng(lugar.latitude, lugar.longitude)
                ubicacionBuscada = posicion
                nombreUbicacion = textoBuscado
                onEncontrada(posicion)
            } else {
                mensajeBusqueda = context.getString(R.string.address_not_found)
            }
        } catch (e: Exception) {
            mensajeBusqueda = context.getString(R.string.address_search_error)
        } finally {
            buscando = false
        }
    }

    // GEOCODER INVERSO: DIRECCION A PARTIR DE UNA POSICION
    var ubicacionLongClick by mutableStateOf<LatLng?>(null)
        private set

    // null mientras el Geocoder todavía no responde
    var direccionLongClick by mutableStateOf<String?>(null)
        private set

    fun buscarDireccionInversa(posicion: LatLng) {

        ubicacionLongClick = posicion
        direccionLongClick = null

        viewModelScope.launch {

            val textoDireccionDesconocida = context.getString(R.string.unknown_address)

            val direccionEncontrada = try {

                if (!Geocoder.isPresent()) {
                    throw IllegalStateException("Geocoder no disponible")
                }

                val resultados = withContext(Dispatchers.IO) {

                    @Suppress("DEPRECATION")
                    Geocoder(
                        context,
                        LOCALE_COLOMBIA
                    ).getFromLocation(
                        posicion.latitude,
                        posicion.longitude,
                        1
                    )
                }

                resultados?.firstOrNull()?.getAddressLine(0)
                    ?: textoDireccionDesconocida

            } catch (_: Exception) {
                textoDireccionDesconocida
            }

            // Evita actualizar el marcador si hubo otro LongClick
            if (ubicacionLongClick == posicion) {
                direccionLongClick = direccionEncontrada
            }
        }
    }

    override fun onCleared() {
        detenerSensor()
        detenerUbicacion()
    }

    private companion object {
        const val KEY_DIRECCION = "direccion"

        const val UMBRAL_LUX = 50f
        const val UMBRAL_LUX_OSCURO = 40f
        const val UMBRAL_LUX_CLARO = 60f

        val LOCALE_COLOMBIA: Locale = Locale.forLanguageTag("es-CO")
    }
}
