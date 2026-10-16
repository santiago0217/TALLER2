package com.icm2630.taller2.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CameraViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    // false = Foto, true = Video
    var isVideo by mutableStateOf(savedStateHandle[KEY_IS_VIDEO] ?: false)
        private set

    // Contenido seleccionado o capturado
    var mediaUri by mutableStateOf<Uri?>(savedStateHandle[KEY_MEDIA_URI])
        private set
    var mediaIsVideo by mutableStateOf(savedStateHandle[KEY_MEDIA_IS_VIDEO] ?: false)
        private set

    // Archivo temporal de la camara. Se guarda en el SavedStateHandle porque
    // el sistema puede destruir la app mientras la camara esta abierta
    private var pendingUri: Uri?
        get() = savedStateHandle[KEY_PENDING_URI]
        set(value) {
            savedStateHandle[KEY_PENDING_URI] = value
        }

    // Foto cargada para mostrar
    var bitmap by mutableStateOf<Bitmap?>(null)
        private set

    // Se activa cuando el usuario niega el permiso de camara
    var permisoDenegado by mutableStateOf(false)
        private set

    private var cargaBitmap: Job? = null

    init {
        // Si la app se recreo, vuelve a cargar la foto que se estaba mostrando
        cargarBitmap()
    }

    fun cambiarModo(video: Boolean) {
        isVideo = video
        savedStateHandle[KEY_IS_VIDEO] = video
    }

    fun onPermisoCamara(concedido: Boolean) {
        permisoDenegado = !concedido
    }

    // Crea el archivo temporal donde la app de camara guarda la captura
    fun crearUriCaptura(): Uri {
        val context = getApplication<Application>()

        val folder = File(context.cacheDir, CAMERA_FOLDER)
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
        return uri
    }

    fun onFotoTomada(success: Boolean) {
        onCapturaTerminada(success, esVideo = false)
    }

    fun onVideoGrabado(success: Boolean) {
        onCapturaTerminada(success, esVideo = true)
    }

    private fun onCapturaTerminada(success: Boolean, esVideo: Boolean) {
        val uri = pendingUri
        pendingUri = null

        if (success) {
            val anterior = mediaUri
            mostrarMedia(uri, esVideo)
            borrarCaptura(anterior)
        } else {
            // La captura se cancelo: el archivo temporal quedo vacio
            borrarCaptura(uri)
        }
    }

    fun onMediaSeleccionada(uri: Uri?) {
        if (uri != null) {
            val anterior = mediaUri
            mostrarMedia(uri, esVideo = isVideo)
            borrarCaptura(anterior)
        }
    }

    private fun mostrarMedia(uri: Uri?, esVideo: Boolean) {
        mediaUri = uri
        mediaIsVideo = esVideo
        savedStateHandle[KEY_MEDIA_URI] = uri
        savedStateHandle[KEY_MEDIA_IS_VIDEO] = esVideo
        cargarBitmap()
    }

    private fun cargarBitmap() {
        val uri = mediaUri
        val esVideo = mediaIsVideo

        cargaBitmap?.cancel()
        cargaBitmap = viewModelScope.launch {
            bitmap = if (uri != null && !esVideo) {
                withContext(Dispatchers.IO) {
                    runCatching { decodificarFoto(uri) }.getOrNull()
                }
            } else {
                null
            }
        }
    }

    // Carga la foto reducida (la vista previa es pequeña) y con la rotacion correcta
    private fun decodificarFoto(uri: Uri): Bitmap? {
        val resolver = getApplication<Application>().contentResolver

        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, limites)
        }

        var muestreo = 1
        while (limites.outWidth / (muestreo * 2) >= MAX_LADO_FOTO &&
            limites.outHeight / (muestreo * 2) >= MAX_LADO_FOTO
        ) {
            muestreo *= 2
        }

        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
        val foto = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opciones)
        } ?: return null

        val orientacion = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val grados = when (orientacion) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return foto
        }

        val matriz = Matrix().apply { postRotate(grados) }
        return Bitmap.createBitmap(foto, 0, 0, foto.width, foto.height, matriz, true)
    }

    // Solo borra archivos creados por la camara de la app, nunca los de la galeria
    private fun borrarCaptura(uri: Uri?) {
        val nombre = uri?.lastPathSegment ?: return
        if (uri.authority != "${getApplication<Application>().packageName}.fileprovider") return

        File(File(getApplication<Application>().cacheDir, CAMERA_FOLDER), nombre).delete()
    }

    override fun onCleared() {
        // Al salir de la pantalla ya no se necesitan las capturas temporales
        if (pendingUri == null) {
            File(getApplication<Application>().cacheDir, CAMERA_FOLDER).deleteRecursively()
        }
    }

    private companion object {
        const val CAMERA_FOLDER = "camera"
        const val MAX_LADO_FOTO = 1024

        const val KEY_IS_VIDEO = "is_video"
        const val KEY_MEDIA_URI = "media_uri"
        const val KEY_MEDIA_IS_VIDEO = "media_is_video"
        const val KEY_PENDING_URI = "pending_uri"
    }
}
