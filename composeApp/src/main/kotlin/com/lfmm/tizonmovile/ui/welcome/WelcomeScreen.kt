package com.lfmm.tizonmovile.ui.welcome

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.ui.preview.TizonPreviews
import com.lfmm.tizonmovile.ui.theme.Spacing
import com.lfmm.tizonmovile.ui.theme.TizonTheme
import kotlinx.coroutines.launch

private data class WelcomePage(val icon: ImageVector, @param:StringRes val title: Int, @param:StringRes val body: Int)

private val pages = listOf(
    WelcomePage(Icons.Filled.Eco, R.string.welcome_1_title, R.string.welcome_1_body),
    WelcomePage(Icons.Filled.CenterFocusStrong, R.string.welcome_2_title, R.string.welcome_2_body),
    WelcomePage(Icons.Filled.Shield, R.string.welcome_3_title, R.string.welcome_3_body),
)

/** Bienvenida (solo la primera vez): qué hace la app, cómo capturar y privacidad. */
@Composable
fun WelcomeScreen(onStart: () -> Unit, initialPage: Int = 0) {
    val pager = rememberPagerState(initialPage = initialPage) { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pager.currentPage == pages.lastIndex

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.l, Alignment.CenterVertically),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(page.icon, contentDescription = null, modifier = Modifier.padding(Spacing.xl).size(72.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(stringResource(page.title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(stringResource(page.body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
        }

        val pageDescription = stringResource(R.string.welcome_page_cd, pager.currentPage + 1, pages.size)
        Row(
            modifier = Modifier.padding(vertical = Spacing.l).semantics { contentDescription = pageDescription },
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            repeat(pages.size) { i ->
                Box(
                    Modifier.size(if (i == pager.currentPage) 12.dp else 8.dp).background(
                        if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    ),
                )
            }
        }
        Spacer(Modifier.size(Spacing.s))
        Button(
            onClick = { if (isLast) onStart() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.scanButton),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text(stringResource(if (isLast) R.string.action_start else R.string.action_next), style = MaterialTheme.typography.titleMedium)
        }
    }
    }
}

@TizonPreviews
@Composable
private fun WelcomeQueHacePreview() = TizonTheme { WelcomeScreen(onStart = {}, initialPage = 0) }

@TizonPreviews
@Composable
private fun WelcomeCapturasPreview() = TizonTheme { WelcomeScreen(onStart = {}, initialPage = 1) }

@TizonPreviews
@Composable
private fun WelcomePrivacidadPreview() = TizonTheme { WelcomeScreen(onStart = {}, initialPage = 2) }
