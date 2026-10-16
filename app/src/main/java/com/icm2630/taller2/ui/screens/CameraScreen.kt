package com.icm2630.taller2.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icm2630.taller2.R
import com.icm2630.taller2.ui.viewmodel.CameraViewModel

@Composable
fun CameraScreen(viewModel: CameraViewModel = viewModel()) {

    val context = LocalContext.current

    val isVideo = viewModel.isVideo
    val mediaUri = viewModel.mediaUri
    val mediaIsVideo = viewModel.mediaIsVideo
    val bitmap = viewModel.bitmap

    // Tomar foto usando la app de camara
    val takePictureLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            viewModel.onFotoTomada(success)
        }

    // Grabar video usando la app de camara
    val captureVideoLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CaptureVideo()
        ) { success ->
            viewModel.onVideoGrabado(success)
        }

    // Seleccionar foto o video de la galeria
    val galleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            viewModel.onMediaSeleccionada(uri)
        }

    // Funcion para abrir la camara
    fun openCamera() {

        val uri = viewModel.crearUriCaptura()

        if (viewModel.isVideo) {
            captureVideoLauncher.launch(uri)
        } else {
            takePictureLauncher.launch(uri)
        }
    }

    // Solicitar permiso de camara
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            viewModel.onPermisoCamara(granted)

            if (granted) {
                openCamera()
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Selector Foto / Video
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            Text(stringResource(R.string.photo))

            Spacer(Modifier.width(12.dp))

            Switch(
                checked = isVideo,
                onCheckedChange = {
                    viewModel.cambiarModo(it)
                }
            )

            Spacer(Modifier.width(12.dp))

            Text(stringResource(R.string.video))
        }

        Spacer(Modifier.height(30.dp))

        // Vista previa de tamaño fijo
        Box(
            modifier = Modifier
                .width(300.dp)
                .height(350.dp),
            contentAlignment = Alignment.Center
        ) {

            when {

                mediaUri == null -> {
                    Text(
                        text = stringResource(
                            R.string.no_media_selected
                        )
                    )
                }

                mediaIsVideo -> {
                    val uri = mediaUri

                    key(uri) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->

                                VideoView(ctx).apply {

                                    val controls =
                                        MediaController(ctx)

                                    controls.setAnchorView(this)
                                    setMediaController(controls)

                                    setVideoURI(uri)
                                    seekTo(1)
                                }
                            },
                            update = { videoView ->
                                // La URI se configura al crear la vista
                            }
                        )
                    }
                }

                bitmap != null -> {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(
                            R.string.photo
                        ),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                else -> {
                    CircularProgressIndicator()
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        // Botones principales
        Row(
            horizontalArrangement =
                Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {

            Button(
                onClick = {

                    val permission =
                        Manifest.permission.CAMERA

                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            permission
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        viewModel.onPermisoCamara(true)
                        openCamera()
                    } else {
                        permissionLauncher.launch(permission)
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isVideo)
                            R.string.record_video
                        else
                            R.string.take_photo
                    )
                )
            }

            Button(
                onClick = {

                    val mediaType = if (isVideo) {
                        ActivityResultContracts
                            .PickVisualMedia.VideoOnly
                    } else {
                        ActivityResultContracts
                            .PickVisualMedia.ImageOnly
                    }

                    galleryLauncher.launch(
                        PickVisualMediaRequest(mediaType)
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isVideo)
                            R.string.select_video
                        else
                            R.string.select_photo
                    )
                )
            }
        }

        if (viewModel.permisoDenegado) {

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(
                    R.string.camera_permission_denied
                ),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
