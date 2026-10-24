package com.lfmm.tizonmovile.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.ui.preview.TizonPreviews
import com.lfmm.tizonmovile.ui.theme.TizonTheme

/** Destinos de la barra inferior con sesión iniciada (06-ux-ui §Navegación). */
enum class MainTab(@param:StringRes val label: Int, val icon: ImageVector) {
    SCAN(R.string.nav_scan, Icons.Filled.CameraAlt),
    HISTORY(R.string.nav_history, Icons.Filled.History),
    ALERTS(R.string.nav_alerts, Icons.Filled.Notifications),
    PROFILE(R.string.nav_profile, Icons.Filled.Person),
}

@Composable
fun TizonBottomBar(selected: MainTab, openAlerts: Int, onSelect: (MainTab) -> Unit) {
    NavigationBar {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                label = { Text(stringResource(tab.label)) },
                icon = {
                    if (tab == MainTab.ALERTS && openAlerts > 0) {
                        val badgeCd = pluralStringResource(R.plurals.nav_alerts_badge_cd, openAlerts, openAlerts)
                        BadgedBox(badge = {
                            Badge(Modifier.semantics { contentDescription = badgeCd }) { Text(openAlerts.toString()) }
                        }) { Icon(tab.icon, contentDescription = null) }
                    } else {
                        Icon(tab.icon, contentDescription = null)
                    }
                },
            )
        }
    }
}

@TizonPreviews @Composable
private fun BarraInferiorConAlertasPreview() = TizonTheme {
    Scaffold(bottomBar = { TizonBottomBar(MainTab.SCAN, openAlerts = 2) {} }) { }
}
