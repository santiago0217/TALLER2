package com.icm2630.taller2.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.icm2630.taller2.R
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Cliente para obtener la ubicacion
    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Verificar si tenemos permiso
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    var permissionGranted by remember {
        mutableStateOf(hasLocationPermission())
    }

    // Solicitar permisos de ubicacion
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            permissionGranted = hasLocationPermission()
        }

    // Ubicacion actual del usuario
    var currentLocation by remember {
        mutableStateOf<LatLng?>(null)
    }

    // Indica si ya centramos la camara
    var cameraCentered by remember {
        mutableStateOf(false)
    }

    // Posicion inicial del mapa
    val bogota = LatLng(4.6097, -74.0817)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            bogota,
            12f
        )
    }

    // Solicitar permisos al entrar a la pantalla
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

    // Configurar actualizaciones de GPS
    DisposableEffect(permissionGranted, fusedLocationClient) {

        if (!permissionGranted || !hasLocationPermission()) {
            onDispose { }
        } else {

            val locationRequest =
                LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    5000L
                )
                    .setMinUpdateIntervalMillis(2000L)
                    .build()

            // Recibir nuevas posiciones
            val locationCallback = object : LocationCallback() {

                override fun onLocationResult(
                    result: LocationResult
                ) {
                    super.onLocationResult(result)

                    result.lastLocation?.let { location ->

                        currentLocation = LatLng(
                            location.latitude,
                            location.longitude
                        )
                    }
                }
            }

            // Iniciar actualizaciones
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            // Detener actualizaciones al salir
            onDispose {
                fusedLocationClient.removeLocationUpdates(
                    locationCallback
                )
            }
        }
    }

    // Mover la camara a la primera ubicacion
    LaunchedEffect(currentLocation) {

        val location = currentLocation

        if (location != null && !cameraCentered) {

            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    location,
                    16f
                )
            )

            cameraCentered = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        if (!permissionGranted) {
            Text(
                text = stringResource(
                    R.string.location_permission
                ),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }

        GoogleMap(
            modifier = Modifier.weight(1f),
            cameraPositionState = cameraPositionState
        ) {

            // Marcador de ubicacion actual
            currentLocation?.let { location ->

                val markerState = rememberUpdatedMarkerState(
                    position = location
                )

                Marker(
                    state = markerState,
                    title = stringResource(R.string.my_location)
                )
            }
        }
    }
}