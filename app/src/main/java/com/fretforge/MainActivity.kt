package com.fretforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fretforge.navigation.FretForgeAppNavigation
import com.fretforge.ui.theme.FretForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FretForgeTheme {
                FretForgeAppNavigation()
            }
        }
    }
}
