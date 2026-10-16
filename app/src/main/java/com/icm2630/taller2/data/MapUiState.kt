package com.icm2630.taller2.data

import com.google.android.gms.maps.model.LatLng

// Estado completo de la pantalla del mapa
data class MapUiState(
    // SENSOR DE LUMINOSIDAD
    val sensorDisponible: Boolean = false,
    val luminosidad: Float = 100f,
    val modoOscuro: Boolean = false,

    // PERMISOS Y GPS
    val permissionGranted: Boolean = false,
    val currentLocation: LatLng? = null,
    val puntosRecorridos: List<LatLng> = emptyList(),

    // GEOCODER: BUSQUEDA DE DIRECCIONES
    val direccion: String = "",
    val ubicacionBuscada: LatLng? = null,
    val nombreUbicacion: String = "",
    val mensajeBusqueda: String? = null,
    val buscando: Boolean = false,

    // GEOCODER INVERSO: MARCADOR DEL LONGCLICK
    val ubicacionLongClick: LatLng? = null,
    // null mientras el Geocoder todavía no responde
    val direccionLongClick: String? = null
) {
    val puedeBuscar: Boolean
        get() = direccion.trim().isNotEmpty() && !buscando
}
