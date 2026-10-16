package com.icm2630.taller2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.icm2630.taller2.R
import com.icm2630.taller2.ui.components.MenuButton

@Composable
fun HomeScreen(
    onOpenCamera: () -> Unit,
    onOpenMap: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        MenuButton(
            text = stringResource(R.string.open_camera),
            icon = Icons.Default.CameraAlt,
            onClick = onOpenCamera
        )

        Spacer(modifier = Modifier.height(24.dp))

        MenuButton(
            text = stringResource(R.string.open_map),
            icon = Icons.Default.Map,
            onClick = onOpenMap,
            outlined = true
        )
    }
}
