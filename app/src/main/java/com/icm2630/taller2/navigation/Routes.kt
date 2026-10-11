package com.icm2630.taller2.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Routes : NavKey {

    @Serializable
    data object Home : Routes

    @Serializable
    data object Camera : Routes

    @Serializable
    data object Map : Routes
}