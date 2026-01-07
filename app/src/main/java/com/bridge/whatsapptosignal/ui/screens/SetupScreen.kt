package com.bridge.whatsapptosignal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridge.whatsapptosignal.ui.viewmodel.BridgeViewModel

@Composable
fun SetupScreen(
    viewModel: BridgeViewModel,
    onRequestNotificationAccess: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onSetupComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Content based on step
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (uiState.setupStep) {
                    1 -> NotificationAccessStep(
                        onRequestAccess = {
                            onRequestNotificationAccess()
                            viewModel.notificationAccessGranted()
                        }
                    )
                    2 -> SignalPhoneNumberStep(
                        isLoading = uiState.isRegistering,
                        error = uiState.registrationError,
                        onSubmit = { viewModel.requestVerificationCode(it) }
                    )
                    3 -> VerificationCodeStep(
                        phoneNumber = uiState.signalPhoneNumber,
                        isLoading = uiState.isRegistering,
                        error = uiState.registrationError,
                        onSubmit = { viewModel.verifyCode(it) }
                    )
                    4 -> DestinationNumberStep(
                        onSubmit = { 
                            viewModel.setDestinationNumber(it)
                            viewModel.setSetupStep(5)
                        }
                    )
                    5 -> SetupCompleteStep(
                        onComplete = {
                            viewModel.completeSetup()
                            onSetupComplete()
                        }
                    )
                }
            }
            
            // Progress indicators
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (index < uiState.setupStep) Color.White else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationAccessStep(onRequestAccess: () -> Unit) {
    SetupStepContent(
        icon = Icons.Default.Notifications,
        iconColor = Color(0xFF4CAF50),
        title = "Notificatie toegang",
        description = "De app heeft toegang nodig tot notificaties om WhatsApp berichten te lezen",
        buttonText = "Toegang verlenen",
        onButtonClick = onRequestAccess
    )
}

@Composable
fun SignalPhoneNumberStep(
    isLoading: Boolean,
    error: String?,
    onSubmit: (String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SetupStepHeader(
            icon = Icons.Default.Phone,
            iconColor = Color(0xFF2196F3),
            title = "Signal nummer",
            description = "Voer het telefoonnummer in voor de bridge (bijv. prepaid SIM)"
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Telefoonnummer") },
            placeholder = { Text("+31 6 12345678") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF2196F3),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = Color(0xFF2196F3),
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                cursorColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        if (error != null) {
            Text(
                text = error,
                color = Color(0xFFF44336),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { onSubmit(phoneNumber) },
            enabled = phoneNumber.length >= 10 && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("SMS code verzenden", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun VerificationCodeStep(
    phoneNumber: String,
    isLoading: Boolean,
    error: String?,
    onSubmit: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SetupStepHeader(
            icon = Icons.Default.Email,
            iconColor = Color(0xFF9C27B0),
            title = "Verificatie",
            description = "Voer de code in die je via SMS hebt ontvangen op $phoneNumber"
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.filter { c -> c.isDigit() || c == '-' }.take(7) },
            label = { Text("Verificatiecode") },
            placeholder = { Text("123-456") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF9C27B0),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = Color(0xFF9C27B0),
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                cursorColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        if (error != null) {
            Text(
                text = error,
                color = Color(0xFFF44336),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { onSubmit(code) },
            enabled = code.length >= 6 && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF9C27B0)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Verifiëren", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun DestinationNumberStep(onSubmit: (String) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SetupStepHeader(
            icon = Icons.Default.Send,
            iconColor = Color(0xFFFF9800),
            title = "Bestemming",
            description = "Naar welk Signal nummer moeten berichten worden doorgestuurd? (je werk telefoon)"
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Werk telefoon nummer") },
            placeholder = { Text("+34 687 654 321") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFF9800),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = Color(0xFFFF9800),
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                cursorColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { onSubmit(phoneNumber) },
            enabled = phoneNumber.length >= 10,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9800)
            )
        ) {
            Text("Doorgaan", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SetupCompleteStep(onComplete: () -> Unit) {
    SetupStepContent(
        icon = Icons.Default.Check,
        iconColor = Color(0xFF4CAF50),
        title = "Klaar!",
        description = "Je bridge is ingesteld. WhatsApp berichten worden nu doorgestuurd naar Signal.",
        buttonText = "Start Bridge",
        buttonGradient = listOf(Color(0xFF4CAF50), Color(0xFF2196F3)),
        onButtonClick = onComplete
    )
}

@Composable
fun SetupStepHeader(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = iconColor.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun SetupStepContent(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    buttonText: String,
    buttonGradient: List<Color>? = null,
    onButtonClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        SetupStepHeader(
            icon = icon,
            iconColor = iconColor,
            title = title,
            description = description
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onButtonClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (buttonGradient == null) iconColor else Color.Transparent
            )
        ) {
            Box(
                modifier = if (buttonGradient != null) {
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(buttonGradient),
                            RoundedCornerShape(16.dp)
                        )
                } else Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
