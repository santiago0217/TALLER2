package com.icm2630.taller2.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.icm2630.taller2.R
import com.icm2630.taller2.ui.components.ActionButton
import com.icm2630.taller2.ui.components.ErrorMessage
import com.icm2630.taller2.ui.components.InfoCard
import com.icm2630.taller2.ui.components.MapMarker
import com.icm2630.taller2.ui.components.SearchField
import com.icm2630.taller2.ui.viewmodel.MapViewModel
import kotlinx.coroutines.launch

@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {

    val keyboardController = LocalSoftwareKeyboardController.current
    val searchScope = rememberCoroutineScope()

    // SENSOR DE LUMINOSIDAD
    DisposableEffect(viewModel) {
        viewModel.iniciarSensor()
        onDispose { viewModel.detenerSensor() }
    }

    val modoOscuro = viewModel.modoOscuro
    val mapProperties = remember(modoOscuro, viewModel.estiloOscuro) {
        MapProperties(mapStyleOptions = if (modoOscuro) viewModel.estiloOscuro else null)
    }

    // PERMISOS Y GPS
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onPermisosRespondidos()
    }

    val bogota = LatLng(4.6097, -74.0817)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bogota, 12f)
    }

    LaunchedEffect(Unit) {
        if (!viewModel.hasLocationPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Si el usuario concede el permiso desde Ajustes, se detecta al volver a la app
    LifecycleResumeEffect(viewModel) {
        viewModel.onPermisosRespondidos()
        onPauseOrDispose { }
    }

    DisposableEffect(viewModel.permissionGranted, viewModel) {
        if (!viewModel.iniciarUbicacion()) {
            onDispose { }
        } else {
            onDispose { viewModel.detenerUbicacion() }
        }
    }

    val currentLocation = viewModel.currentLocation

    LaunchedEffect(currentLocation) {
        val location = currentLocation
        if (location != null && !viewModel.cameraCentered) {
            viewModel.marcarCamaraCentrada()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(location, 16f)
            )
        }
    }

    // GEOCODER: BUSQUEDA DE DIRECCIONES
    fun buscarDireccion() {
        if (!viewModel.puedeBuscar) return

        keyboardController?.hide()
        searchScope.launch {
            viewModel.buscarDireccion { posicion ->
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(posicion, 16f)
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!viewModel.permissionGranted) {
            ErrorMessage(
                text = stringResource(R.string.location_permission),
                modifier = Modifier.padding(16.dp)
            )
        }

        // INFORMACION DEL SENSOR DE LUMINOSIDAD
        InfoCard {
            if (viewModel.lightSensor == null) {
                Text(text = stringResource(R.string.light_sensor_unavailable))
            } else {
                Text(text = stringResource(R.string.light_sensor_value, viewModel.luminosidad))
                Text(
                    text = stringResource(
                        if (modoOscuro) R.string.dark_map_mode else R.string.light_map_mode
                    )
                )
            }
        }

        // BUSCADOR DE DIRECCIONES
        InfoCard(
            contentPadding = PaddingValues(
                horizontal = 12.dp,
                vertical = 8.dp
            )
        ) {

            SearchField(
                value = viewModel.direccion,
                onValueChange = { viewModel.onDireccionChange(it) },
                label = stringResource(R.string.search_address),
                placeholder = stringResource(R.string.address_example),
                onSearch = { buscarDireccion() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            ActionButton(
                text = stringResource(R.string.search_button),
                onClick = {
                    buscarDireccion()
                },
                enabled = viewModel.direccion.isNotBlank() && !viewModel.buscando
            )

            viewModel.mensajeBusqueda?.let { mensaje ->
                ErrorMessage(
                    text = mensaje,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // GOOGLE MAPS
        GoogleMap(
            modifier = Modifier.weight(1f),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            onMapLongClick = { posicion ->
                viewModel.buscarDireccionInversa(posicion)
            }
        ) {
            currentLocation?.let { location ->
                MapMarker(
                    position = location,
                    title = stringResource(R.string.my_location)
                )
            }

            if (viewModel.puntosRecorridos.size >= 2) {
                Polyline(
                    points = viewModel.puntosRecorridos.toList(),
                    color = Color.Blue,
                    width = 8f
                )
            }

            viewModel.ubicacionBuscada?.let { posicion ->
                MapMarker(
                    position = posicion,
                    title = viewModel.nombreUbicacion
                )
            }

            viewModel.ubicacionLongClick?.let { posicion ->

                val coordenadas = stringResource(
                    R.string.coordinates_label,
                    posicion.latitude,
                    posicion.longitude
                )

                // Abre (o refresca) la ventana de información con la dirección
                MapMarker(
                    position = posicion,
                    title = viewModel.direccionLongClick ?: coordenadas,
                    snippet = coordenadas,
                    showInfoWindow = true
                )
            }
        }
    }
}
