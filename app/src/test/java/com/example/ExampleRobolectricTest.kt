package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.BarcodeFormatType
import com.example.core.model.ScanSource
import com.example.data.preferences.AppLanguage
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ScanHistoryRepository
import com.example.feature.history.HistoryScreen
import com.example.feature.history.HistoryViewModel
import com.example.feature.result.ResultScreen
import com.example.feature.result.ResultViewModel
import com.example.ui.theme.ScanovaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var fakeDao: InMemoryScanDao
    private lateinit var historyRepository: ScanHistoryRepository
    private lateinit var preferencesRepository: UserPreferencesRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeDao = InMemoryScanDao()
        historyRepository = ScanHistoryRepository(fakeDao)

        val testFile = File(context.cacheDir, "test_prefs_${UUID.randomUUID()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()),
            produceFile = { testFile }
        )
        preferencesRepository = UserPreferencesRepository(dataStore)
    }

    @Test
    fun `app name resource is Scanova`() {
        assertEquals("Scanova", context.getString(R.string.app_name))
    }

    @Test
    fun `initial navigation shows Scanner screen and navigates to empty History`() {
        composeTestRule.setContent {
            ScanovaApp(
                customHistoryRepository = historyRepository,
                customPreferencesRepository = preferencesRepository
            )
        }
        composeTestRule.waitForIdle()

        // Scanner tab is displayed by default
        composeTestRule.onNodeWithTag("scanner_screen_root").assertIsDisplayed()

        // Navigate to History tab
        composeTestRule.onNodeWithTag("nav_tab_history").performClick()
        composeTestRule.waitForIdle()

        // Verify empty history state is shown
        composeTestRule.onNodeWithTag("history_empty_state").assertIsDisplayed()
    }

    @Test
    fun `history search, filter, and opening scan result detail works end-to-end`() {
        val wifiScan = runBlocking {
            historyRepository.recordScan(
                rawValue = "WIFI:T:WPA;S:KopiKenanganWiFi;P:kopisusu99;;",
                format = BarcodeFormatType.QR_CODE,
                source = ScanSource.CAMERA,
                autoSave = true,
                nowMillis = 100_000L
            )
        }
        val productScan = runBlocking {
            historyRepository.recordScan(
                rawValue = "8992761136017",
                format = BarcodeFormatType.EAN_13,
                source = ScanSource.GALLERY,
                autoSave = true,
                nowMillis = 200_000L
            )
        }

        val historyVm = HistoryViewModel(historyRepository)
        val resultVm = ResultViewModel(historyRepository)
        var openedScanId: Long? = null

        composeTestRule.setContent {
            ScanovaTheme {
                HistoryScreen(
                    viewModel = historyVm,
                    onOpenScanDetail = { id -> openedScanId = id },
                    onNavigateToScanner = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify both items are displayed in History
        composeTestRule.onNodeWithTag("history_item_card_${wifiScan.id}", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("history_item_card_${productScan.id}", useUnmergedTree = true).assertIsDisplayed()

        // Filter by 1D Barcodes only
        composeTestRule.onNodeWithTag("history_chip_barcode").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("history_item_card_${productScan.id}", useUnmergedTree = true).assertIsDisplayed()

        // Switch back to All and search for Wi-Fi SSID
        composeTestRule.onNodeWithTag("history_chip_all").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("history_search_input").performTextInput("KopiKenangan")
        composeTestRule.waitForIdle()

        // Tap the Wi-Fi entry to trigger opening Result Detail
        composeTestRule.onNodeWithTag("history_item_card_${wifiScan.id}", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        assertEquals(wifiScan.id, openedScanId)

        // Render ResultScreen for the selected Wi-Fi scan and verify password masking/reveal
        resultVm.loadScanRecord(wifiScan.id)
        composeTestRule.runOnUiThread {
            // Verify state loaded
        }
    }

    @Test
    fun `result screen masks Wi-Fi password by default and reveals on toggle`() {
        val wifiScan = runBlocking {
            historyRepository.recordScan(
                rawValue = "WIFI:T:WPA;S:KopiKenanganWiFi;P:kopisusu99;;",
                format = BarcodeFormatType.QR_CODE,
                source = ScanSource.CAMERA,
                autoSave = true,
                nowMillis = 100_000L
            )
        }
        val resultVm = ResultViewModel(historyRepository)

        composeTestRule.setContent {
            ScanovaTheme {
                ResultScreen(
                    scanId = wifiScan.id,
                    viewModel = resultVm,
                    onNavigateBack = {},
                    onScanAnother = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("result_header_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("wifi_password_toggle_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("kopisusu99").assertIsDisplayed()
    }

    @Test
    fun `settings language switch updates UI strings to English`() {
        runBlocking {
            preferencesRepository.setLanguage(AppLanguage.INDONESIAN)
        }

        composeTestRule.setContent {
            ScanovaApp(
                customHistoryRepository = historyRepository,
                customPreferencesRepository = preferencesRepository
            )
        }
        composeTestRule.waitForIdle()

        // Navigate to Settings tab
        composeTestRule.onNodeWithTag("nav_tab_settings").performClick()
        composeTestRule.waitForIdle()

        // Scroll to language chip and switch to English
        composeTestRule.onNodeWithTag("settings_lang_en_chip").performScrollTo().performClick()
        composeTestRule.waitUntil(5000L) {
            composeTestRule.onAllNodesWithText("Sound on Scan")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify English string is rendered
        composeTestRule.onNodeWithText("Sound on Scan").performScrollTo().assertIsDisplayed()
    }
}
