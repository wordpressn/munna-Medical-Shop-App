package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PharmaViewModel
import com.example.ui.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: PharmaViewModel) {
    var isRegisterState by remember { mutableStateOf(false) }
    var authModePhone by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }

    // Forms fields
    var email by remember { mutableStateOf("shop@pharma.com") } // Pre-load John Carter for quicker review
    var password by remember { mutableStateOf("owner123") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("SHOP_OWNER") } // "SHOP_OWNER", "ADMIN"

    // Phone Auth
    var phoneForOtp by remember { mutableStateOf("+1 (123) 456-7890") }
    var otpSentCode by remember { mutableStateOf<String?>(null) }
    var enteredOtp by remember { mutableStateOf("") }
    var isVerifyingCode by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header medical badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalPharmacy,
                    contentDescription = "Pharmacy Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "MedStore Pro",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp
                )
            }

            Text(
                text = "Secure Whole & Retail Supply Ledger",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Auth Dialog Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when {
                            showForgotPassword -> "Forgot Password"
                            authModePhone -> "OTP Wireless Login"
                            isRegisterState -> "Join MedStore Pro"
                            else -> "Account Sign In"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    AnimatedContent(
                        targetState = when {
                            showForgotPassword -> 3
                            authModePhone -> 2
                            isRegisterState -> 1
                            else -> 0
                        },
                        label = "auth_screen_transition"
                    ) { targetState ->
                        when (targetState) {
                            0 -> { // Standard Login
                                Column {
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Email Address") },
                                        leadingIcon = { Icon(Icons.Default.Email, "Email") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    var passVisible by remember { mutableStateOf(false) }
                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("Password") },
                                        leadingIcon = { Icon(Icons.Default.Lock, "Lock") },
                                        trailingIcon = {
                                            IconButton(onClick = { passVisible = !passVisible }) {
                                                Icon(
                                                    imageVector = if (passVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = "Toggle password visibility"
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { showForgotPassword = true }) {
                                            Text("Forgot Password?")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            focusManager.clearFocus()
                                            viewModel.login(email.trim(), password)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Login, "Login")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Authorize Login", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            1 -> { // Register Screen
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text("Full Name") },
                                        leadingIcon = { Icon(Icons.Default.Badge, "Name") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Email Address") },
                                        leadingIcon = { Icon(Icons.Default.Email, "Email") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                    )

                                    OutlinedTextField(
                                        value = phone,
                                        onValueChange = { phone = it },
                                        label = { Text("Phone Number") },
                                        leadingIcon = { Icon(Icons.Default.Phone, "Phone") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                    )

                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("Create Secure Password") },
                                        leadingIcon = { Icon(Icons.Default.Lock, "Lock") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        visualTransformation = PasswordVisualTransformation()
                                    )

                                    // Role picker Custom Card Option Selector
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Select Enterprise Role",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { selectedRole = "SHOP_OWNER" },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (selectedRole == "SHOP_OWNER") MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                                                ),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Storefront, "Shop")
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Retail Shop")
                                            }
                                            OutlinedButton(
                                                onClick = { selectedRole = "ADMIN" },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (selectedRole == "ADMIN") MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                                                ),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.AdminPanelSettings, "Admin")
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Wholesale Admin")
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            focusManager.clearFocus()
                                            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                                                viewModel.postFeedback("Please fill in all details.")
                                            } else {
                                                viewModel.register(email.trim(), password, name, phone, selectedRole)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.HowToReg, "Register")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Register Account", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            2 -> { // Phone OTP Auth
                                Column {
                                    OutlinedTextField(
                                        value = phoneForOtp,
                                        onValueChange = { phoneForOtp = it },
                                        label = { Text("Pharmacy Phone Number") },
                                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, "Phone") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    if (otpSentCode == null) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    val generated = (1000 + (Math.random() * 8999).toInt()).toString()
                                                    viewModel.postFeedback("Sending Wireless security code...")
                                                    delay(1000)
                                                    otpSentCode = generated
                                                    viewModel.postFeedback("MedStore Pro SMS: Your active dynamic OTP access token code is $generated")
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Sms, "SMS")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Request OTP Verification Code", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = enteredOtp,
                                            onValueChange = { enteredOtp = it },
                                            label = { Text("4-Digit OTP Code") },
                                            leadingIcon = { Icon(Icons.Default.Key, "Key") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )

                                        // Toast mockup of received OTP
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .padding(vertical = 12.dp)
                                                .fillMaxWidth()
                                        ) {
                                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CircleNotifications, "SMS notification", tint = MaterialTheme.colorScheme.secondary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "SMS Received from MedStore Pro: Your code is <bold>$otpSentCode</bold>. Click to autofill.",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                TextButton(onClick = { enteredOtp = otpSentCode ?: "" }) {
                                                    Text("AUTOFILL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                if (enteredOtp == otpSentCode) {
                                                    viewModel.loginWithPhone(phoneForOtp, enteredOtp)
                                                } else {
                                                    viewModel.postFeedback("OTP mismatch! Enter current code.")
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.FactCheck, "Verify")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Verify & Setup Ledger", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            3 -> { // Forgot password
                                Column {
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Account Email Address") },
                                        leadingIcon = { Icon(Icons.Default.AlternateEmail, "Email") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = {
                                            viewModel.forgotPassword(email.trim())
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.LockReset, "Send link")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Retrieve Password Hint", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Alternate Auth Flow Switch
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isRegisterState) "Already holding a portal membership?" else "First lease profile or forgot account?",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (showForgotPassword || authModePhone) {
                            TextButton(onClick = {
                                showForgotPassword = false
                                authModePhone = false
                            }) {
                                Text("Back to Email sign in", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(onClick = { isRegisterState = !isRegisterState }) {
                                Text(
                                    text = if (isRegisterState) "Sign In Instantly" else "Create Franchise Account",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!isRegisterState && !authModePhone && !showForgotPassword) {
                            Spacer(modifier = Modifier.width(10.dp))
                            VerticalDivider(modifier = Modifier.height(24.dp).align(Alignment.CenterVertically))
                            Spacer(modifier = Modifier.width(10.dp))
                            TextButton(onClick = { authModePhone = true }) {
                                Text("OTP Wireless Login", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Demo instructions card to switch roles instantly and check both flows
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Demo Portals Quick Access Credentials",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Shop Owner Core: shop@pharma.com (pass: owner123)\n" +
                               "• Pharmacy Wholesale Admin: admin@pharma.com (pass: admin123)\n" +
                               "• Pending Shop setup flow: pending@pharma.com (pass: pending123)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
