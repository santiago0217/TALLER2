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
import com.icm2630.taller2.data.MapUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private var sensorIniciado = false

    val estiloOscuro: MapStyleOptions =
        MapStyleOptions.loadRawResourceStyle(application, R.raw.map_dark)

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_LIGHT) {
                val lux = event.values[0]
                _uiState.update {
                    it.copy(
                        luminosidad = lux,
                        // Usa dos umbrales para que el estilo no parpadee
                        // cuando la luz ronda los 50 lux
                        modoOscuro = when {
                            !sensorIniciado -> lux < UMBRAL_LUX
                            it.modoOscuro -> lux < UMBRAL_LUX_CLARO
                            else -> lux < UMBRAL_LUX_OSCURO
                        }
                    )
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

    private val _uiState = MutableStateFlow(
        MapUiState(
            sensorDisponible = lightSensor != null,
            permissionGranted = hasLocationPermission(),
            direccion = savedStateHandle.get<String>(KEY_DIRECCION) ?: ""
        )
    )
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun onPermisosRespondidos() {
        _uiState.update { it.copy(permissionGranted = hasLocationPermission()) }
    }

    // La camara del mapa se centra solo con la primera ubicacion recibida
    private var cameraCentered = false

    fun debeCentrarCamara(): Boolean {
        if (cameraCentered) return false
        cameraCentered = true
        return true
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                val nuevaUbicacion = LatLng(location.latitude, location.longitude)
                _uiState.update {
                    val puntos = if (it.puntosRecorridos.lastOrNull() != nuevaUbicacion) {
                        it.puntosRecorridos + nuevaUbicacion
                    } else {
                        it.puntosRecorridos
                    }
                    it.copy(currentLocation = nuevaUbicacion, puntosRecorridos = puntos)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun iniciarUbicacion(): Boolean {
        if (!_uiState.value.permissionGranted || !hasLocationPermission()) return false

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
    fun onDireccionChange(valor: String) {
        _uiState.update { it.copy(direccion = valor) }
        savedStateHandle[KEY_DIRECCION] = valor
    }

    // Se ejecuta en el scope de la pantalla para poder animar la camara del mapa
    suspend fun buscarDireccion(onEncontrada: suspend (LatLng) -> Unit) {
        if (!_uiState.value.puedeBuscar) return
        val textoBuscado = _uiState.value.direccion.trim()

        _uiState.update { it.copy(buscando = true, mensajeBusqueda = null) }
        try {
            if (!Geocoder.isPresent()) {
                mostrarMensajeBusqueda(R.string.address_search_error)
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
                _uiState.update {
                    it.copy(ubicacionBuscada = posicion, nombreUbicacion = textoBuscado)
                }
                onEncontrada(posicion)
            } else {
                mostrarMensajeBusqueda(R.string.address_not_found)
            }
        } catch (e: Exception) {
            mostrarMensajeBusqueda(R.string.address_search_error)
        } finally {
            _uiState.update { it.copy(buscando = false) }
        }
    }

    private fun mostrarMensajeBusqueda(mensaje: Int) {
        _uiState.update { it.copy(mensajeBusqueda = context.getString(mensaje)) }
    }

    // GEOCODER INVERSO: DIRECCION A PARTIR DE UNA POSICION
    fun buscarDireccionInversa(posicion: LatLng) {

        _uiState.update {
            it.copy(ubicacionLongClick = posicion, direccionLongClick = null)
        }

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
            _uiState.update {
                if (it.ubicacionLongClick == posicion) {
                    it.copy(direccionLongClick = direccionEncontrada)
                } else {
                    it
                }
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
