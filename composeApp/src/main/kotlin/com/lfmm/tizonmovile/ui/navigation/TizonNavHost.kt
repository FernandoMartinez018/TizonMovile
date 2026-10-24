package com.lfmm.tizonmovile.ui.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lfmm.tizonmovile.BuildConfig
import com.lfmm.tizonmovile.R
import com.lfmm.tizonmovile.data.auth.AuthState
import com.lfmm.tizonmovile.data.model.SessionSummary
import com.lfmm.tizonmovile.ui.AppViewModel
import com.lfmm.tizonmovile.ui.alerts.AlertsScreen
import com.lfmm.tizonmovile.ui.alerts.AlertsViewModel
import com.lfmm.tizonmovile.ui.auth.AuthScreen
import com.lfmm.tizonmovile.ui.auth.AuthViewModel
import com.lfmm.tizonmovile.ui.components.ErrorBanner
import com.lfmm.tizonmovile.ui.components.LoadingState
import com.lfmm.tizonmovile.ui.history.HistoryScreen
import com.lfmm.tizonmovile.ui.history.HistoryViewModel
import com.lfmm.tizonmovile.ui.permissions.PermissionStatus
import com.lfmm.tizonmovile.ui.permissions.PermissionsScreen
import com.lfmm.tizonmovile.ui.permissions.PermissionsUiState
import com.lfmm.tizonmovile.ui.profile.ProfileScreen
import com.lfmm.tizonmovile.ui.profile.ProfileViewModel
import com.lfmm.tizonmovile.ui.profile.UsersScreen
import com.lfmm.tizonmovile.ui.profile.UsersViewModel
import com.lfmm.tizonmovile.ui.scan.ScanRoute
import com.lfmm.tizonmovile.ui.scan.ScanViewModel
import com.lfmm.tizonmovile.ui.summary.DetailUiState
import com.lfmm.tizonmovile.ui.summary.SessionDetailScreen
import com.lfmm.tizonmovile.ui.summary.SessionDetailViewModel
import com.lfmm.tizonmovile.ui.summary.SummaryScreen
import com.lfmm.tizonmovile.ui.welcome.WelcomeScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

// ── Rutas (Navigation Compose con tipos) ──────────────────────────────────

@Serializable object WelcomeRoute
@Serializable object PermissionsRoute
@Serializable object AuthRoute
@Serializable object ScanTab
@Serializable object HistoryTab
@Serializable object AlertsTab
@Serializable object ProfileTab
@Serializable object UsersRoute
@Serializable object TipsRoute
@Serializable data class SummaryRoute(val sessionId: String)
@Serializable data class DetailRoute(val sessionId: String)

private fun MainTab.route(): Any = when (this) {
    MainTab.SCAN -> ScanTab
    MainTab.HISTORY -> HistoryTab
    MainTab.ALERTS -> AlertsTab
    MainTab.PROFILE -> ProfileTab
}

/** Raíz de la app: sin sesión, Bienvenida → Permisos → Login; con sesión, barra inferior de 4 destinos. */
@Composable
fun TizonApp(appVm: AppViewModel = koinViewModel()) {
    val nav = rememberNavController()
    val authState by appVm.authState.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val currentTab = MainTab.entries.firstOrNull { tab -> backStack?.destination?.hasRoute(tab.route()::class) == true }
    val activity = LocalContext.current as ComponentActivity
    // Una instancia por usuario: al cambiar de cuenta no se heredan sesiones ni alertas.
    val userKey = (authState as? AuthState.LoggedIn)?.user?.username ?: "anon"

    // Login, cierre de sesión y sesión expirada (cualquier 401) cambian de grafo (RF-02 CA4/CA5).
    LaunchedEffect(authState) {
        val inMain = backStack?.destination?.let { d -> MainTab.entries.any { d.hasRoute(it.route()::class) } } == true
        when (authState) {
            is AuthState.LoggedIn -> if (backStack?.destination?.hasRoute(AuthRoute::class) == true) nav.goToMain()
            AuthState.LoggedOut, AuthState.Expired -> if (inMain || backStack?.destination?.hasRoute(UsersRoute::class) == true) {
                nav.navigate(AuthRoute) { popUpTo(0) }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTab != null) {
                val alertsVm: AlertsViewModel = koinViewModel(key = "alerts-$userKey", viewModelStoreOwner = activity)
                val openAlerts by alertsVm.openCount.collectAsStateWithLifecycle()
                TizonBottomBar(currentTab, openAlerts) { tab ->
                    nav.navigate(tab.route()) {
                        popUpTo(ScanTab) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = when (appVm.start) {
                AppViewModel.Start.WELCOME -> WelcomeRoute
                AppViewModel.Start.AUTH -> AuthRoute
                AppViewModel.Start.MAIN -> ScanTab
            },
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<WelcomeRoute> {
                Insets { WelcomeScreen(onStart = { appVm.onWelcomeDone(); nav.navigate(PermissionsRoute) { popUpTo(0) } }) }
            }
            composable<PermissionsRoute> {
                Insets {
                    PermissionsRoute(onContinue = {
                        if (appVm.isLoggedIn()) nav.goToMain() else nav.navigate(AuthRoute) { popUpTo(0) }
                    })
                }
            }
            composable<AuthRoute> {
                val vm: AuthViewModel = koinViewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                Insets {
                    AuthScreen(state, vm::onUsernameChange, vm::onPasswordChange, vm::onFullNameChange,
                        vm::onTogglePasswordVisible, vm::onSubmit, vm::onSwitchMode)
                }
            }
            composable<ScanTab> {
                val vm: ScanViewModel = koinViewModel(key = "scan-$userKey", viewModelStoreOwner = activity)
                ScanRoute(vm, onFinished = { nav.navigate(SummaryRoute(it)) }, onExit = { activity.finish() })
            }
            composable<HistoryTab> {
                val vm: HistoryViewModel = koinViewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                HistoryScreen(state, vm::refresh, vm::loadMore, onOpen = { nav.navigate(DetailRoute(it.sessionId)) },
                    onStartScan = { nav.navigate(ScanTab) { popUpTo(ScanTab) { inclusive = true } } })
            }
            composable<AlertsTab> {
                val vm: AlertsViewModel = koinViewModel(key = "alerts-$userKey", viewModelStoreOwner = activity)
                val state by vm.state.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) { vm.refresh() }
                AlertsScreen(state, vm::acknowledge, vm::refresh)
            }
            composable<ProfileTab> {
                val vm: ProfileViewModel = koinViewModel { parametersOf(BuildConfig.VERSION_NAME) }
                val state by vm.state.collectAsStateWithLifecycle()
                state?.let {
                    ProfileScreen(it, vm::onLogoutClick, vm::onConfirmLogout, vm::onDismissLogout,
                        onOpenUsers = { nav.navigate(UsersRoute) }, onOpenTips = { nav.navigate(TipsRoute) })
                }
            }
            composable<UsersRoute> {
                val vm: UsersViewModel = koinViewModel()
                val users by vm.users.collectAsStateWithLifecycle()
                UsersScreen(users, vm.currentUsername, vm::changeRole, vm::changeEnabled, onBack = { nav.popBackStack() })
            }
            composable<TipsRoute> {
                Insets { WelcomeScreen(onStart = { nav.popBackStack() }, initialPage = 1) }
            }
            composable<SummaryRoute> { entry ->
                val id = entry.toRoute<SummaryRoute>().sessionId
                val vm: SessionDetailViewModel = koinViewModel(key = "summary-$id") { parametersOf(id) }
                val state by vm.state.collectAsStateWithLifecycle()
                Insets {
                    DetailContent(state, onRetry = vm::load) { summary ->
                        SummaryScreen(summary,
                            onNewScan = { nav.popBackStack(ScanTab, inclusive = false) },
                            onViewHistory = { nav.navigate(HistoryTab) { popUpTo(ScanTab) } })
                    }
                }
            }
            composable<DetailRoute> { entry ->
                val id = entry.toRoute<DetailRoute>().sessionId
                val vm: SessionDetailViewModel = koinViewModel(key = "detail-$id") { parametersOf(id) }
                val state by vm.state.collectAsStateWithLifecycle()
                DetailContent(state, onRetry = vm::load) { SessionDetailScreen(it, onBack = { nav.popBackStack() }) }
            }
        }
    }
}

private fun NavHostController.goToMain() = navigate(ScanTab) { popUpTo(0) }

/** Pantallas sin barra superior: respetan la barra de estado y la de navegación del sistema. */
@Composable
private fun Insets(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding()) { content() }
    }
}

@Composable
private fun DetailContent(
    state: DetailUiState,
    onRetry: () -> Unit,
    content: @Composable (SessionSummary) -> Unit,
) {
    when (state) {
        DetailUiState.Loading -> LoadingState()
        DetailUiState.Error -> ErrorBanner(stringResource(R.string.history_error), onRetry = onRetry)
        is DetailUiState.Content -> content(state.summary)
    }
}

/** Permisos con explicación previa; solo la cámara es obligatoria (RNF-SEG-12, RF-10 CA2, RF-11 CA4). */
@Composable
private fun PermissionsRoute(onContinue: () -> Unit) {
    val context = LocalContext.current
    fun status(permission: String) =
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) PermissionStatus.GRANTED
        else PermissionStatus.NOT_REQUESTED

    var camera by rememberSaveable { mutableStateOf(status(Manifest.permission.CAMERA)) }
    var location by rememberSaveable { mutableStateOf(status(Manifest.permission.ACCESS_FINE_LOCATION)) }
    val askNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var notifications by rememberSaveable {
        mutableStateOf(if (askNotifications) status(Manifest.permission.POST_NOTIFICATIONS) else PermissionStatus.GRANTED)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        camera = if (it) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        location = if (result.values.any { it }) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }
    val notificationsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifications = if (it) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    PermissionsScreen(
        state = PermissionsUiState(camera, location, notifications, askNotifications),
        onRequestCamera = { cameraLauncher.launch(Manifest.permission.CAMERA) },
        onRequestLocation = {
            locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        },
        onRequestNotifications = {
            if (askNotifications) notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onOpenSettings = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            camera = status(Manifest.permission.CAMERA).takeIf { it == PermissionStatus.GRANTED } ?: PermissionStatus.NOT_REQUESTED
        },
        onContinue = onContinue,
    )
}
