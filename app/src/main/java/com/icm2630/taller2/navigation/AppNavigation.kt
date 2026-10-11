package com.icm2630.taller2.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.icm2630.taller2.ui.screens.CameraScreen
import com.icm2630.taller2.ui.screens.HomeScreen
import com.icm2630.taller2.ui.screens.MapScreen

@Composable
fun AppNavigation() {

    val backStack = rememberNavBackStack(Routes.Home)

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        },
        entryProvider = entryProvider {

            entry<Routes.Home> {
                HomeScreen(
                    onOpenCamera = {
                        backStack.add(Routes.Camera)
                    },
                    onOpenMap = {
                        backStack.add(Routes.Map)
                    }
                )
            }

            entry<Routes.Camera> {
                CameraScreen()
            }

            entry<Routes.Map> {
                MapScreen()
            }
        }
    )
}