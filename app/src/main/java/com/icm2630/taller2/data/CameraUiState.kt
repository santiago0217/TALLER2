package com.icm2630.taller2.data

import android.graphics.Bitmap
import android.net.Uri

data class CameraUiState(
    val isVideo: Boolean = false,
    val mediaUri: Uri? = null,
    val mediaIsVideo: Boolean = false,
    val bitmap: Bitmap? = null,
    val permisoDenegado: Boolean = false
)
