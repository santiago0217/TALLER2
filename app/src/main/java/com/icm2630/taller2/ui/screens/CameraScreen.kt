package com.icm2630.taller2.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

import com.icm2630.taller2.R
import com.icm2630.taller2.ui.components.ActionButton
import com.icm2630.taller2.ui.components.BackButton
import com.icm2630.taller2.ui.components.ErrorMessage
import com.icm2630.taller2.ui.components.LabeledSwitch
import com.icm2630.taller2.ui.components.MediaPreview
import com.icm2630.taller2.ui.viewmodel.CameraViewModel

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    viewModel: CameraViewModel = viewModel()
) {

    val context = LocalContext.current

    // La UI observa el unico estado publicado por el ViewModel.
    val uiState by viewModel.uiState.collectAsState()
    val isVideo = uiState.isVideo

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

        if (uiState.isVideo) {
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

    Box(modifier = Modifier.fillMaxSize()) {

        // Volver a la pantalla principal
        BackButton(
            text = stringResource(R.string.back_home),
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Selector Foto / Video
            LabeledSwitch(
                startLabel = stringResource(R.string.photo),
                endLabel = stringResource(R.string.video),
                checked = isVideo,
                onCheckedChange = {
                    viewModel.cambiarModo(it)
                }
            )

            Spacer(Modifier.height(30.dp))

            // Vista previa de tamaño fijo
            MediaPreview(
                mediaUri = uiState.mediaUri,
                isVideo = uiState.mediaIsVideo,
                bitmap = uiState.bitmap,
                emptyText = stringResource(R.string.no_media_selected),
                imageDescription = stringResource(R.string.photo)
            )

            Spacer(Modifier.height(30.dp))

            // Botones principales
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {

                ActionButton(
                    text = stringResource(
                        if (isVideo)
                            R.string.record_video
                        else
                            R.string.take_photo
                    ),
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
                )

                ActionButton(
                    text = stringResource(
                        if (isVideo)
                            R.string.select_video
                        else
                            R.string.select_photo
                    ),
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
                )
            }

            if (uiState.permisoDenegado) {

                Spacer(Modifier.height(16.dp))

                ErrorMessage(
                    text = stringResource(
                        R.string.camera_permission_denied
                    )
                )
            }
        }
    }
}
