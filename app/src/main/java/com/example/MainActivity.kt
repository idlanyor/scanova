package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.navigation.ScanovaNavGraph
import com.example.core.util.LocalizedAppWrapper
import com.example.data.local.database.ScanovaDatabase
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ScanHistoryRepository
import com.example.feature.history.HistoryViewModel
import com.example.feature.result.ResultViewModel
import com.example.feature.scanner.ScannerViewModel
import com.example.feature.settings.SettingsViewModel
import com.example.ui.theme.ScanovaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScanovaApp()
        }
    }
}

@Composable
fun ScanovaApp(
    customHistoryRepository: ScanHistoryRepository? = null,
    customPreferencesRepository: UserPreferencesRepository? = null
) {
    val context = LocalContext.current.applicationContext

    val historyRepository = remember(customHistoryRepository) {
        customHistoryRepository ?: ScanHistoryRepository(
            ScanovaDatabase.getInstance(context).scanDao()
        )
    }

    val preferencesRepository = remember(customPreferencesRepository) {
        customPreferencesRepository ?: UserPreferencesRepository(context)
    }

    val factory = remember(historyRepository, preferencesRepository) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(ScannerViewModel::class.java) ->
                        ScannerViewModel(historyRepository, preferencesRepository) as T
                    modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                        HistoryViewModel(historyRepository) as T
                    modelClass.isAssignableFrom(ResultViewModel::class.java) ->
                        ResultViewModel(historyRepository) as T
                    modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                        SettingsViewModel(preferencesRepository, historyRepository) as T
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }

    val vmKeyPrefix = remember(historyRepository, preferencesRepository) {
        "${historyRepository.hashCode()}_${preferencesRepository.hashCode()}"
    }
    val scannerViewModel: ScannerViewModel = viewModel(key = "${vmKeyPrefix}_scanner", factory = factory)
    val historyViewModel: HistoryViewModel = viewModel(key = "${vmKeyPrefix}_history", factory = factory)
    val resultViewModel: ResultViewModel = viewModel(key = "${vmKeyPrefix}_result", factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(key = "${vmKeyPrefix}_settings", factory = factory)

    val preferences by settingsViewModel.preferences.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (preferences.themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    LocalizedAppWrapper(language = preferences.language) {
        ScanovaTheme(
            darkTheme = useDarkTheme,
            dynamicColor = preferences.dynamicColor
        ) {
            ScanovaNavGraph(
                scannerViewModel = scannerViewModel,
                historyViewModel = historyViewModel,
                resultViewModel = resultViewModel,
                settingsViewModel = settingsViewModel
            )
        }
    }
}
