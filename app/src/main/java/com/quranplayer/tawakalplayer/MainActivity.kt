package com.quranplayer.tawakalplayer

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quranplayer.tawakalplayer.data.QuranData
import com.quranplayer.tawakalplayer.data.Reciter
import com.quranplayer.tawakalplayer.data.Surah
import com.quranplayer.tawakalplayer.ui.screens.HomeScreen
import com.quranplayer.tawakalplayer.ui.screens.PlayerScreen
import com.quranplayer.tawakalplayer.ui.screens.ReciterScreen
import com.quranplayer.tawakalplayer.ui.screens.SurahScreen
import com.quranplayer.tawakalplayer.ui.theme.TawakalPlayerTheme

// Change this to your own PIN before locking the device down
private const val EXIT_PIN = "2348"
private const val TAPS_REQUIRED = 7
private const val TAP_WINDOW_MS = 3000L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val dpm = getSystemService(DevicePolicyManager::class.java)
        val adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

        if (dpm.isDeviceOwnerApp(packageName)) {
            dpm.setLockTaskPackages(adminComponent, arrayOf(packageName))
            dpm.setLockTaskFeatures(adminComponent, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
            startLockTask()
        }

        setContent {
            TawakalPlayerApp(onExitKiosk = { stopLockTask() })
        }
    }
}

@Composable
fun TawakalPlayerApp(onExitKiosk: () -> Unit) {
    TawakalPlayerTheme {
        val navController = rememberNavController()
        var selectedReciter by remember { mutableStateOf<Reciter?>(null) }
        var selectedSurah by remember { mutableStateOf<Surah?>(null) }

        var tapCount by remember { mutableStateOf(0) }
        var firstTapTime by remember { mutableStateOf(0L) }
        var showPinDialog by remember { mutableStateOf(false) }
        var pinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(onEnter = { navController.navigate("reciters") })
                }
                composable("reciters") {
                    ReciterScreen(
                        onReciterSelected = { reciter ->
                            selectedReciter = reciter
                            navController.navigate("surahs")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("surahs") {
                    SurahScreen(
                        onSurahSelected = { surah ->
                            selectedSurah = surah
                            navController.navigate("player")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("player") {
                    val reciter = selectedReciter
                    val surah = selectedSurah
                    if (reciter != null && surah != null) {
                        PlayerScreen(
                            reciter = reciter,
                            surah = surah,
                            onSurahEnded = {
                                val currentIndex = QuranData.surahs.indexOfFirst { it.number == surah.number }
                                val nextIndex = currentIndex + 1
                                if (nextIndex < QuranData.surahs.size) {
                                    selectedSurah = QuranData.surahs[nextIndex]
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }

            // Hidden exit trigger — bottom-left corner, invisible.
            // Moved from top-left since it conflicted with the back button.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(0.dp)
                    .size(56.dp)
                    .background(Color.Transparent)
                    .clickable {
                        val now = System.currentTimeMillis()
                        if (now - firstTapTime > TAP_WINDOW_MS) {
                            firstTapTime = now
                            tapCount = 1
                        } else {
                            tapCount += 1
                        }
                        if (tapCount >= TAPS_REQUIRED) {
                            tapCount = 0
                            showPinDialog = true
                        }
                    }
            )
        }

        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                title = { Text("Exit Kiosk Mode") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                pinInput = it
                                pinError = false
                            },
                            label = { Text("Enter PIN") },
                            isError = pinError
                        )
                        if (pinError) {
                            Text("Incorrect PIN", color = Color.Red)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (pinInput == EXIT_PIN) {
                            showPinDialog = false
                            pinInput = ""
                            onExitKiosk()
                        } else {
                            pinError = true
                        }
                    }) {
                        Text("Unlock")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showPinDialog = false
                        pinInput = ""
                        pinError = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}