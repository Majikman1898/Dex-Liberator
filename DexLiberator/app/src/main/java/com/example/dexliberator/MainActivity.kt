package com.example.dexliberator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DexLiberatorTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun DexLiberatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content
    )
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Initial Checks
    LaunchedEffect(Unit) {
        viewModel.checkTargetInstalled(context)
        viewModel.checkPermission(context)
    }

    // Re-check permission and target status on resume
    DisposableEffect(androidx.lifecycle.Lifecycle.State.RESUMED) {
        val lifecycle = (context as? androidx.lifecycle.LifecycleOwner)?.lifecycle
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermission(context)
                viewModel.checkTargetInstalled(context)
            }
        }
        lifecycle?.addObserver(observer)
        onDispose {
            lifecycle?.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Dex Liberator") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Target Status
            StatusCard(
                title = "Target Service",
                isOk = uiState.targetInstalled,
                okText = "Installed",
                errorText = "Not Found (Is this a Samsung Device?)"
            )

            // 2. Permission Status
            StatusCard(
                title = "Write Settings Permission",
                isOk = uiState.hasPermission,
                okText = "Granted",
                errorText = "Denied"
            )

            if (!uiState.hasPermission) {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Grant Permission")
                }
            }

            HorizontalDivider()

            // 3. Toggles
            Text("Exploit Payloads", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable DeX on Phone")
                Switch(
                    checked = uiState.isDexEnabled,
                    onCheckedChange = { viewModel.toggleDex(it) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Developer Mode")
                Switch(
                    checked = uiState.isDevModeEnabled,
                    onCheckedChange = { viewModel.toggleDevMode(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Action Button
            Button(
                onClick = { viewModel.fireExploit(context.contentResolver) },
                enabled = uiState.hasPermission && uiState.targetInstalled,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("FIRE EXPLOIT", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // 5. Feedback
            uiState.lastMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isSuccess) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = if (uiState.isSuccess) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCard(title: String, isOk: Boolean, okText: String, errorText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOk) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.labelMedium)
                Text(
                    text = if (isOk) "✅ $okText" else "⚠️ $errorText",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isOk) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
