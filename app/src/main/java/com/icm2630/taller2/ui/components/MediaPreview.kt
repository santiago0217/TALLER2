package com.icm2630.taller2.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

// Vista previa de tamaño fijo para una foto o un video
@Composable
fun MediaPreview(
    mediaUri: Uri?,
    isVideo: Boolean,
    bitmap: Bitmap?,
    emptyText: String,
    imageDescription: String,
    modifier: Modifier = Modifier,
    width: Dp = 300.dp,
    height: Dp = 350.dp
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height),
        contentAlignment = Alignment.Center
    ) {

        when {

            mediaUri == null -> {
                Text(text = emptyText)
            }

            isVideo -> {
                VideoPlayer(
                    uri = mediaUri,
                    modifier = Modifier.fillMaxSize()
                )
            }

            bitmap != null -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = imageDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            else -> {
                CircularProgressIndicator()
            }
        }
    }
}

// Reproductor de video con controles
@Composable
fun VideoPlayer(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    // Se recrea la vista cuando cambia el video
    key(uri) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->

                VideoView(ctx).apply {

                    val controls =
                        MediaController(ctx)

                    controls.setAnchorView(this)
                    setMediaController(controls)

                    setVideoURI(uri)
                    seekTo(1)
                }
            }
        )
    }
}
