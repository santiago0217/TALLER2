package com.icm2630.taller2.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.core.content.FileProvider
import com.icm2630.taller2.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CameraScreen() {

    val context = LocalContext.current

    // false = Foto, true = Video
    var isVideo by remember { mutableStateOf(false) }

    // Contenido seleccionado o capturado
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaIsVideo by remember { mutableStateOf(false) }

    // Archivo temporal de la camara
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    // Foto cargada para mostrar
    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = mediaUri,
        key2 = mediaIsVideo
    ) {
        val uri = mediaUri

        value = if (uri != null && !mediaIsVideo) {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
            }
        } else {
            null
        }
    }

    // Tomar foto usando la app de camara
    val takePictureLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            if (success) {
                mediaUri = pendingUri
                mediaIsVideo = false
            }
        }

    // Grabar video usando la app de camara
    val captureVideoLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CaptureVideo()
        ) { success ->

            if (success) {
                mediaUri = pendingUri
                mediaIsVideo = true
            }
        }

    // Seleccionar foto o video de la galeria
    val galleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            if (uri != null) {
                mediaUri = uri
                mediaIsVideo = isVideo
            }
        }

    // Funcion para abrir la camara
    fun openCamera() {

        val folder = File(context.cacheDir, "camera")
        folder.mkdirs()

        val extension = if (isVideo) ".mp4" else ".jpg"

        val file = File.createTempFile(
            "captura_",
            extension,
            folder
        )

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        pendingUri = uri

        if (isVideo) {
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
                    isVideo = it
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
                    val uri = mediaUri!!

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
                        bitmap = bitmap!!.asImageBitmap(),
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
    }
}