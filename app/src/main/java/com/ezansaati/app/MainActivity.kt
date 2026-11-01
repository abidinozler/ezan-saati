package com.ezansaati.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ezansaati.app.notification.PrayerNotifications
import com.ezansaati.app.ui.decor.MosqueSilhouette
import com.ezansaati.app.ui.decor.SkyBackground
import com.ezansaati.app.ui.decor.rememberCurrentPrayer
import com.ezansaati.app.ui.home.HomeScreen
import com.ezansaati.app.ui.qibla.QiblaScreen
import com.ezansaati.app.ui.settings.SettingsScreen
import com.ezansaati.app.ui.theme.EzanTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Gökyüzü arka planı koyu olduğu için sistem çubuğu simgeleri her zaman açık renkli.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        PrayerNotifications.createChannel(this)
        setContent {
            EzanTheme {
                AppRoot(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}

private enum class Tab(@StringRes val label: Int, val icon: ImageVector) {
    VAKITLER(R.string.tab_times, Icons.Filled.DateRange),
    KIBLE(R.string.tab_qibla, Icons.Filled.Place),
    AYARLAR(R.string.tab_settings, Icons.Filled.Settings),
}

@Composable
private fun AppRoot(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable {
        mutableStateOf(if (state.location == null) Tab.AYARLAR else Tab.VAKITLER)
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val period = rememberCurrentPrayer(state.today, state.tomorrow)
    Box(Modifier.fillMaxSize()) {
        SkyBackground(period, Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(containerColor = Color(0xCC070B18)) {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                MosqueSilhouette(
                    period,
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(170.dp),
                )
                when (tab) {
                    Tab.VAKITLER -> HomeScreen(
                        state = state,
                        onDayChanged = viewModel::refresh,
                        onGoToSettings = { tab = Tab.AYARLAR },
                    )
                    Tab.KIBLE -> QiblaScreen(location = state.location)
                    Tab.AYARLAR -> SettingsScreen(state = state, viewModel = viewModel)
                }
            }
        }
    }
}
