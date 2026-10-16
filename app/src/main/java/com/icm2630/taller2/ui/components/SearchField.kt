package com.icm2630.taller2.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction

// Campo de texto de busqueda con los colores del tema (claro u oscuro)
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(label)
        },
        placeholder = {
            Text(placeholder)
        },
        singleLine = true,
        modifier = modifier,

        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor =
                MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor =
                MaterialTheme.colorScheme.onSurface,

            focusedContainerColor =
                MaterialTheme.colorScheme.surface,
            unfocusedContainerColor =
                MaterialTheme.colorScheme.surface,

            focusedBorderColor =
                MaterialTheme.colorScheme.primary,
            unfocusedBorderColor =
                MaterialTheme.colorScheme.outline,

            focusedLabelColor =
                MaterialTheme.colorScheme.primary,
            unfocusedLabelColor =
                MaterialTheme.colorScheme.onSurfaceVariant,

            focusedPlaceholderColor =
                MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor =
                MaterialTheme.colorScheme.onSurfaceVariant,

            cursorColor =
                MaterialTheme.colorScheme.primary
        ),

        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch()
            }
        )
    )
}
