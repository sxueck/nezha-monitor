package com.sxueck.monitor.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sxueck.monitor.data.model.LoginRequest
import com.sxueck.monitor.data.model.MonitorConfig
import com.sxueck.monitor.data.network.NezhaNetwork
import com.sxueck.monitor.data.store.AppPreferences
import com.sxueck.monitor.ui.theme.NezhaMonitorTheme
import com.sxueck.monitor.worker.MonitorWorkScheduler
import kotlinx.coroutines.launch

private data class TestStatus(
    val success: Boolean,
    val message: String
)

private fun parseExpireTime(expire: String): Long {
    return when {
        expire.isBlank() -> 0L
        expire.toLongOrNull() != null -> {
            val ts = expire.toLong()
            if (ts > 1_000_000_000_000L) ts / 1000 else ts
        }
        else -> {
            try {
                java.time.Instant.parse(expire).epochSecond
            } catch (_: Exception) {
                0L
            }
        }
    }
}

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = AppPreferences(applicationContext)

        setContent {
            NezhaMonitorTheme(preferences = preferences) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        preferences = preferences,
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    preferences: AppPreferences,
    onNavigateBack: () -> Unit
) {
    val config by preferences.configFlow.collectAsStateWithLifecycle(initialValue = MonitorConfig())
    val scope = rememberCoroutineScope()

    var baseUrl by rememberSaveable { mutableStateOf(config.baseUrl) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var tagsInput by rememberSaveable { mutableStateOf(config.tags.joinToString(",")) }
    var isLoading by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var testStatus by remember { mutableStateOf<TestStatus?>(null) }
    var apiToken by rememberSaveable { mutableStateOf(config.apiToken) }

    // Update state when config changes (on first load)
    LaunchedEffect(config.baseUrl) {
        if (baseUrl.isEmpty() && config.baseUrl.isNotEmpty()) {
            baseUrl = config.baseUrl
            tagsInput = config.tags.joinToString(",")
            apiToken = config.apiToken
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Connection Settings Section
            SettingsSection(title = "Connection") {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        apiToken = ""
                        testStatus = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Panel URL") },
                    placeholder = { Text("https://nezha.example.com") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        apiToken = ""
                        testStatus = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        apiToken = ""
                        testStatus = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Filter Settings Section
            SettingsSection(title = "Filter") {
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tags (comma separated)") },
                    placeholder = { Text("prod, sg, hk") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Error Message
            errorMessage?.let {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Test Status
            testStatus?.let { status ->
                Surface(
                    color = if (status.success) {
                        Color(0xFF4ADE80).copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (status.success) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (status.success) Color(0xFF4ADE80) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = status.message,
                            color = if (status.success) Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isTesting = true
                            errorMessage = null
                            testStatus = null
                            try {
                                if (baseUrl.isBlank()) {
                                    errorMessage = "Please enter panel URL"
                                    return@launch
                                }
                                if (username.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter username and password"
                                    return@launch
                                }

                                val normalizedUrl = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
                                val api = NezhaNetwork.createApi(normalizedUrl, "")
                                
                                val response = api.login(LoginRequest(username, password))
                                
                                if (response.isSuccessful && response.body()?.success == true) {
                                    val loginData = response.body()?.data
                                    val token = loginData?.token
                                    if (!token.isNullOrBlank()) {
                                        apiToken = token
                                        preferences.saveCredentials(username, password)
                                        val expireAt = parseExpireTime(loginData.expire)
                                        preferences.saveTokenWithExpiry(token, expireAt)
                                        testStatus = TestStatus(
                                            success = true,
                                            message = "Connection successful! Token received."
                                        )
                                    } else {
                                        testStatus = TestStatus(
                                            success = false,
                                            message = "Login succeeded but no token received"
                                        )
                                    }
                                } else {
                                    val errorMsg = response.body()?.error ?: "HTTP ${response.code()}"
                                    testStatus = TestStatus(
                                        success = false,
                                        message = "Connection failed: $errorMsg"
                                    )
                                }
                            } catch (e: Exception) {
                                testStatus = TestStatus(
                                    success = false,
                                    message = "Connection error: ${e.message ?: "Unknown error"}"
                                )
                            } finally {
                                isTesting = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isTesting && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Test Connection")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val tags = tagsInput.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }

                                    if (baseUrl.isBlank()) {
                                        errorMessage = "Please enter panel URL"
                                        return@launch
                                    }

                                    if (apiToken.isBlank()) {
                                        errorMessage = "Please test connection first to obtain token"
                                        return@launch
                                    }

                                    preferences.updateConfig(
                                        baseUrl = baseUrl,
                                        apiToken = apiToken,
                                        tags = tags
                                    )
                                    if (username.isNotBlank() && password.isNotBlank()) {
                                        preferences.saveCredentials(username, password)
                                    }

                                    MonitorWorkScheduler.scheduleNow(preferences.context)
                                    onNavigateBack()
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Unknown error"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading && apiToken.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}
