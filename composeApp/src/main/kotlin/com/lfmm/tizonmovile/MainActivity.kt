package com.lfmm.tizonmovile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lfmm.tizonmovile.ui.navigation.TizonApp
import com.lfmm.tizonmovile.ui.theme.TizonTheme

/** Única actividad: la navegación y los permisos se manejan en Compose (ui/navigation). */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TizonTheme { TizonApp() }
        }
    }
}
