package com.icm2630.taller2.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberUpdatedMarkerState

// Marcador del mapa que sigue su posicion y, si se pide,
// abre (o refresca) su ventana de informacion cuando cambia el titulo
@Composable
@GoogleMapComposable
fun MapMarker(
    position: LatLng,
    title: String?,
    snippet: String? = null,
    showInfoWindow: Boolean = false
) {
    val markerState = rememberUpdatedMarkerState(position = position)

    Marker(
        state = markerState,
        title = title,
        snippet = snippet
    )

    if (showInfoWindow) {
        LaunchedEffect(position, title) {
            markerState.showInfoWindow()
        }
    }
}
