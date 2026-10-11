package com.icm2630.taller2.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.icm2630.taller2.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val textoNoEncontrado = stringResource(
        R.string.address_not_found
    )

    val textoErrorBusqueda = stringResource(
        R.string.address_search_error
    )
    val searchScope = rememberCoroutineScope()

    // SENSOR DE LUMINOSIDAD
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val lightSensor = remember(sensorManager) {
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }
    var luminosidad by remember { mutableFloatStateOf(100f) }
    var sensorIniciado by remember { mutableStateOf(false) }

    DisposableEffect(sensorManager, lightSensor) {
        if (lightSensor == null) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_LIGHT) {
                        luminosidad = event.values[0]
                        sensorIniciado = true
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(
                listener,
                lightSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    val modoOscuro = lightSensor != null && sensorIniciado && luminosidad < 50f
    val estiloOscuro = remember(context) {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_dark)
    }
    val mapProperties = remember(modoOscuro, estiloOscuro) {
        MapProperties(mapStyleOptions = if (modoOscuro) estiloOscuro else null)
    }

    // PERMISOS Y GPS
    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    var permissionGranted by remember { mutableStateOf(hasLocationPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionGranted = hasLocationPermission()
    }

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    val puntosRecorridos = remember { mutableStateListOf<LatLng>() }
    var cameraCentered by remember { mutableStateOf(false) }

    val bogota = LatLng(4.6097, -74.0817)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bogota, 12f)
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    DisposableEffect(permissionGranted, fusedLocationClient) {
        if (!permissionGranted || !hasLocationPermission()) {
            onDispose { }
        } else {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000L
            ).setMinUpdateIntervalMillis(2000L).build()

            val locationCallback = object : LocationCallback() {
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

            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
            onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
        }
    }

    LaunchedEffect(currentLocation) {
        val location = currentLocation
        if (location != null && !cameraCentered) {
            cameraCentered = true
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(location, 16f)
            )
        }
    }

    // GEOCODER: BUSQUEDA DE DIRECCIONES
    var direccion by rememberSaveable { mutableStateOf("") }
    var ubicacionBuscada by remember { mutableStateOf<LatLng?>(null) }
    var nombreUbicacion by remember { mutableStateOf("") }
    var mensajeBusqueda by remember { mutableStateOf<String?>(null) }
    var buscando by remember { mutableStateOf(false) }

    fun buscarDireccion() {
        val textoBuscado = direccion.trim()
        if (textoBuscado.isEmpty() || buscando) return

        keyboardController?.hide()
        searchScope.launch {
            buscando = true
            mensajeBusqueda = null
            try {
                val resultados = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    Geocoder(context, Locale("es", "CO"))
                        .getFromLocationName(textoBuscado, 1)
                }
                val lugar = resultados?.firstOrNull()
                if (lugar != null) {
                    val posicion = LatLng(lugar.latitude, lugar.longitude)
                    ubicacionBuscada = posicion
                    nombreUbicacion = textoBuscado
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(posicion, 16f)
                    )
                } else {
                    mensajeBusqueda = textoNoEncontrado
                }
            } catch (e: Exception) {
                mensajeBusqueda = textoErrorBusqueda
            } finally {
                buscando = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!permissionGranted) {
            Text(
                text = stringResource(R.string.location_permission),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (lightSensor == null) {
                    Text(text = stringResource(R.string.light_sensor_unavailable))
                } else {
                    Text(text = stringResource(R.string.light_sensor_value, luminosidad))
                    Text(
                        text = stringResource(
                            if (modoOscuro) R.string.dark_map_mode else R.string.light_map_mode
                        )
                    )
                }
            }
        }

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it },
            label = { Text(stringResource(R.string.search_address)) },
            placeholder = { Text(stringResource(R.string.address_example)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { buscarDireccion() })
        )

        Button(
            onClick = { buscarDireccion() },
            enabled = !buscando && direccion.isNotBlank(),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
        ) {
            Text(text = stringResource(R.string.search_button))
        }

        mensajeBusqueda?.let { mensaje ->
            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        GoogleMap(
            modifier = Modifier.weight(1f),
            cameraPositionState = cameraPositionState,
            properties = mapProperties
        ) {
            currentLocation?.let { location ->
                Marker(
                    state = rememberUpdatedMarkerState(position = location),
                    title = stringResource(R.string.my_location)
                )
            }

            if (puntosRecorridos.size >= 2) {
                Polyline(
                    points = puntosRecorridos.toList(),
                    color = Color.Blue,
                    width = 8f
                )
            }

            ubicacionBuscada?.let { posicion ->
                Marker(
                    state = rememberUpdatedMarkerState(position = posicion),
                    title = nombreUbicacion
                )
            }
        }
    }
}
