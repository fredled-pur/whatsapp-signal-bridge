package com.bridge.whatsapptosignal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridge.whatsapptosignal.data.entity.MessageStatus
import com.bridge.whatsapptosignal.ui.viewmodel.BridgeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BridgeViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instellingen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Signal Account Section
            item {
                SettingsSection(title = "Signal Account") {
                    SettingsItem(
                        icon = Icons.Default.Phone,
                        iconBackground = Color(0xFF2196F3),
                        title = uiState.signalPhoneNumber.ifEmpty { "Niet ingesteld" },
                        subtitle = "Bridge nummer"
                    )
                }
            }
            
            // Destination Section
            item {
                SettingsSection(title = "Doorsturen naar") {
                    SettingsItem(
                        icon = Icons.Default.Phone,
                        iconBackground = Color(0xFF4CAF50),
                        title = uiState.destinationNumber.ifEmpty { "Niet ingesteld" },
                        subtitle = "Werk telefoon (Signal)"
                    )
                }
            }
            
            // Options Section
            item {
                SettingsSection(title = "Opties") {
                    SettingsToggleItem(
                        title = "Media doorsturen",
                        subtitle = "Foto's, video's, documenten",
                        checked = uiState.forwardMedia,
                        onCheckedChange = { viewModel.setForwardMedia(it) }
                    )
                    Divider(color = Color(0xFFF0F0F0))
                    SettingsToggleItem(
                        title = "Bericht preview",
                        subtitle = "Toon inhoud in notificatie",
                        checked = uiState.showPreview,
                        onCheckedChange = { viewModel.setShowPreview(it) }
                    )
                    Divider(color = Color(0xFFF0F0F0))
                    SettingsToggleItem(
                        title = "Stille uren",
                        subtitle = if (uiState.quietHoursEnabled) 
                            "${uiState.quietHoursStart}:00 - ${uiState.quietHoursEnd}:00" 
                        else "Uitgeschakeld",
                        checked = uiState.quietHoursEnabled,
                        onCheckedChange = { viewModel.setQuietHoursEnabled(it) }
                    )
                    Divider(color = Color(0xFFF0F0F0))
                    SettingsToggleItem(
                        title = "Filter spam",
                        subtitle = "Verificatiecodes etc. blokkeren",
                        checked = uiState.filterSpam,
                        onCheckedChange = { viewModel.setFilterSpam(it) }
                    )
                }
            }
            
            // Contact Filters Section
            item {
                SettingsSection(title = "Contact Filters") {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        iconBackground = Color(0xFF9C27B0),
                        title = "Alleen specifieke contacten",
                        subtitle = when (uiState.filterMode) {
                            "allowlist" -> "${uiState.filteredContacts.size} contacten toegestaan"
                            "blocklist" -> "${uiState.filteredContacts.size} contacten geblokkeerd"
                            else -> "Alle contacten (geen filter)"
                        },
                        showArrow = true
                    )
                }
            }
            
            // Privacy info
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                        Column {
                            Text(
                                text = "Privacy beschermd",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = "Je werk telefoon deelt geen data met Meta. Alle berichten gaan via Signal's end-to-end encryptie.",
                                fontSize = 13.sp,
                                color = Color(0xFF558B2F),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    iconBackground: Color,
    title: String,
    subtitle: String,
    showArrow: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconBackground, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
        
        if (showArrow) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4CAF50)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: BridgeViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf("all") }
    
    val filteredLogs = remember(uiState.recentLogs, selectedFilter) {
        when (selectedFilter) {
            "forwarded" -> uiState.recentLogs.filter { it.status == MessageStatus.FORWARDED }
            "filtered" -> uiState.recentLogs.filter { it.status == MessageStatus.FILTERED }
            "failed" -> uiState.recentLogs.filter { it.status == MessageStatus.FAILED }
            else -> uiState.recentLogs
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activiteit") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Filter tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "all",
                    onClick = { selectedFilter = "all" },
                    label = { Text("Alles") }
                )
                FilterChip(
                    selected = selectedFilter == "forwarded",
                    onClick = { selectedFilter = "forwarded" },
                    label = { Text("Doorgestuurd") }
                )
                FilterChip(
                    selected = selectedFilter == "filtered",
                    onClick = { selectedFilter = "filtered" },
                    label = { Text("Gefilterd") }
                )
            }
            
            // Logs list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs) { log ->
                    LogItem(log = log)
                }
                
                if (filteredLogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Geen berichten",
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogItem(log: com.bridge.whatsapptosignal.data.entity.MessageLog) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF4CAF50), Color(0xFF2196F3))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = log.sender.firstOrNull()?.toString() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = log.sender,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = timeFormat.format(log.timestamp),
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = log.textPreview,
                        color = Color.Gray,
                        fontSize = 14.sp,
                        maxLines = 2
                    )
                }
            }
            
            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFFF0F0F0)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                when (log.status) {
                                    MessageStatus.FORWARDED -> Color(0xFF4CAF50)
                                    MessageStatus.FILTERED -> Color(0xFFFFC107)
                                    MessageStatus.FAILED -> Color(0xFFF44336)
                                    MessageStatus.PENDING -> Color.Gray
                                },
                                CircleShape
                            )
                    )
                    Text(
                        text = when (log.status) {
                            MessageStatus.FORWARDED -> "Doorgestuurd naar Signal"
                            MessageStatus.FILTERED -> "Gefilterd"
                            MessageStatus.FAILED -> "Mislukt"
                            MessageStatus.PENDING -> "Wachtend"
                        },
                        fontSize = 13.sp,
                        color = when (log.status) {
                            MessageStatus.FORWARDED -> Color(0xFF4CAF50)
                            MessageStatus.FILTERED -> Color(0xFFFFC107)
                            MessageStatus.FAILED -> Color(0xFFF44336)
                            MessageStatus.PENDING -> Color.Gray
                        }
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFE0E0E0)
                )
            }
        }
    }
}
