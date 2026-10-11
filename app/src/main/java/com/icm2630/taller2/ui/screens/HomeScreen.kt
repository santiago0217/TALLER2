package com.icm2630.taller2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.icm2630.taller2.R

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

        Button(
            onClick = onOpenCamera,
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(55.dp)
        ) {
            Text(stringResource(R.string.open_camera))

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onOpenMap,
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(55.dp)
        ) {
            Text(stringResource(R.string.open_map))

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null
            )
        }
    }
}