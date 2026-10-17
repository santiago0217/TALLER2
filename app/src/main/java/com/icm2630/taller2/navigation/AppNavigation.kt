package com.icm2630.taller2.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.icm2630.taller2.ui.screens.CameraScreen
import com.icm2630.taller2.ui.screens.HomeScreen
import com.icm2630.taller2.ui.screens.MapScreen

@Composable
fun AppNavigation() {

    val backStack = rememberNavBackStack(Routes.Home)

    // Fondo y color de texto del tema (claro u oscuro) para todas las pantallas
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Vuelve a Home quitando todas las pantallas que esten encima
        fun volverAHome() {
            while (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        }

        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            },
            // Cada pantalla tiene su propio ViewModel, que se destruye al salir de ella
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
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
                    CameraScreen(
                        onBack = { volverAHome() }
                    )
                }

                entry<Routes.Map> {
                    MapScreen(
                        onBack = { volverAHome() }
                    )
                }
            }
        )
    }
}