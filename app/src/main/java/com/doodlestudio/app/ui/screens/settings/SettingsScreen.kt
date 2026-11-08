package com.doodlestudio.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doodlestudio.app.R
import com.doodlestudio.app.data.preferences.SettingsDataStore
import com.doodlestudio.app.ui.theme.CoralPink
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    settingsDataStore: SettingsDataStore = koinInject()
) {
    val isSoundEnabled by settingsDataStore.isSoundEnabled.collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showCreditsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Sound Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = CoralPink)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.settings_sound),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isSoundEnabled,
                            onCheckedChange = { checked ->
                                scope.launch { settingsDataStore.setSoundEnabled(checked) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = CoralPink)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Language Item
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.settings_language),
                        subtitle = "English / Tiếng Việt",
                        onClick = { /* Auto system locale */ }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // About Item
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.settings_about),
                        subtitle = stringResource(R.string.settings_version),
                        onClick = { showAboutDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Credits Item
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.settings_credits),
                        subtitle = "Procedural Art Generator & Open Source",
                        onClick = { showCreditsDialog = true }
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Doodle Studio") },
            text = {
                Text(
                    "Doodle Studio is an original coloring and free-hand doodling application designed from the ground up.\n\n" +
                            "Features include deterministic Label Map coloring, smooth vector doodling, timelapse video replay export, and instant sharing.\n\n" +
                            "Version: 1.0.0 (Phase P1)"
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", color = CoralPink)
                }
            }
        )
    }

    if (showCreditsDialog) {
        AlertDialog(
            onDismissRequest = { showCreditsDialog = false },
            title = { Text("Credits & Licenses") },
            text = {
                Text(
                    "• Templates (10 initial artworks): 100% Procedurally generated by mathematical vector code in Doodle Studio.\n" +
                            "• UI & Code: Kotlin, Jetpack Compose, Koin, Room, DataStore.\n" +
                            "• Zero copyrighted media or scraped assets.\n\n" +
                            "See CREDITS.md for complete details."
                )
            },
            confirmButton = {
                TextButton(onClick = { showCreditsDialog = false }) {
                    Text("OK", color = CoralPink)
                }
            }
        )
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = CoralPink)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

