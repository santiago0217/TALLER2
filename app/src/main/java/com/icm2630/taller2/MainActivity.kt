package com.icm2630.taller2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.icm2630.taller2.navigation.AppNavigation
import com.icm2630.taller2.ui.theme.TALLER2Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TALLER2Theme {
                AppNavigation()
            }
        }
    }
}