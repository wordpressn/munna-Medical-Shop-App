package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PharmaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopSetupScreen(viewModel: PharmaViewModel) {
    val user = viewModel.currentUser.collectAsState().value

    var shopName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf(user?.fullName ?: "") }
    var phoneNumber by remember { mutableStateOf(user?.phoneNumber ?: "") }
    var emailAddress by remember { mutableStateOf(user?.email ?: "") }
    var shopAddress by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("Metropolis Central") }
    var tradeLicenseNumber by remember { mutableStateOf("") }

    // Upload simulation states
    var licenseFileName by remember { mutableStateOf<String?>(null) }
    var profileFileName by remember { mutableStateOf<String?>(null) }
    var logoFileName by remember { mutableStateOf<String?>(null) }

    var uploadingFile by remember { mutableStateOf<String?>(null) }
    var uploadProgress by remember { mutableStateOf(0f) }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val districts = listOf(
        "Metropolis Central", "Greenfield Suburb", "Highland Medical Circle",
        "Coastal Harbor", "River Valley Industrial", "South Pharma Zone"
    )
    var isDistrictExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                IconButton(onClick = { viewModel.logout() }) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                Text(
                    text = "Pharmacy Registration",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Top Status Badge
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Pending status hint",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Account Status: Pending Registration",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "To access wholesale medical catalogs, please establish your store profile and trade records.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Establish Business Records",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("Pharmacy / Shop Name *") },
                        leadingIcon = { Icon(Icons.Default.Store, "Shop") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Proprietor Full Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, "Proprietor") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Contact Phone *") },
                            leadingIcon = { Icon(Icons.Default.Phone, "Phone") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = emailAddress,
                            onValueChange = { emailAddress = it },
                            label = { Text("Enterprise Email *") },
                            leadingIcon = { Icon(Icons.Default.AlternateEmail, "Email") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = shopAddress,
                        onValueChange = { shopAddress = it },
                        label = { Text("Physical Shop Coordinates / Address *") },
                        leadingIcon = { Icon(Icons.Default.Map, "Location") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // District dropdown Selection
                    ExposedDropdownMenuBox(
                        expanded = isDistrictExpanded,
                        onExpandedChange = { isDistrictExpanded = !isDistrictExpanded }
                    ) {
                        OutlinedTextField(
                            value = district,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Region / District Location *") },
                            leadingIcon = { Icon(Icons.Default.LocationCity, "District") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDistrictExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = isDistrictExpanded,
                            onDismissRequest = { isDistrictExpanded = false }
                        ) {
                            districts.forEach { selection ->
                                DropdownMenuItem(
                                    text = { Text(selection) },
                                    onClick = {
                                        district = selection
                                        isDistrictExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tradeLicenseNumber,
                        onValueChange = { tradeLicenseNumber = it },
                        label = { Text("Trade License Registry Number *") },
                        leadingIcon = { Icon(Icons.Default.WorkspacePremium, "License") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Compliance Declarations & Assets",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // ASSETS UPLOAD SIMULATION CHIPS
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // 1. Trade License Certificate
                        UploadAssetRow(
                            label = "Trade License Certificate *",
                            fileName = licenseFileName,
                            onUpload = {
                                scope.launch {
                                    uploadingFile = "Trade License"
                                    uploadProgress = 0f
                                    while (uploadProgress < 1.0f) {
                                        delay(150)
                                        uploadProgress = uploadProgress + 0.25f
                                    }
                                    licenseFileName = "TL-$tradeLicenseNumber-ATTACHMENT.pdf"
                                    uploadingFile = null
                                    viewModel.postFeedback("Trade license document uploaded securely.")
                                }
                            }
                        )

                        // 2. Owner Profile Photo
                        UploadAssetRow(
                            label = "Proprietor Profile Avatar *",
                            fileName = profileFileName,
                            onUpload = {
                                scope.launch {
                                    uploadingFile = "Avatar"
                                    uploadProgress = 0f
                                    while (uploadProgress < 1.0f) {
                                        delay(100)
                                        uploadProgress = uploadProgress + 0.33f
                                    }
                                    profileFileName = "Av_Owner_${System.currentTimeMillis() % 1000}.png"
                                    uploadingFile = null
                                    viewModel.postFeedback("Profile avatar aligned successfully.")
                                }
                            }
                        )

                        // 3. Pharmacy Logo
                        UploadAssetRow(
                            label = "Pharmacy Branding Logo (Optional)",
                            fileName = logoFileName,
                            onUpload = {
                                scope.launch {
                                    uploadingFile = "Branding Logo"
                                    uploadProgress = 0f
                                    while (uploadProgress < 1.0f) {
                                        delay(100)
                                        uploadProgress = uploadProgress + 0.2f
                                    }
                                    logoFileName = "Logo_Brand_${System.currentTimeMillis() % 1000}.png"
                                    uploadingFile = null
                                    viewModel.postFeedback("Custom branding logo cached.")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (shopName.isEmpty() || ownerName.isEmpty() || phoneNumber.isEmpty() ||
                                emailAddress.isEmpty() || shopAddress.isEmpty() || tradeLicenseNumber.isEmpty()
                            ) {
                                viewModel.postFeedback("Missing required inputs marked with an Asterisk (*)")
                            } else if (licenseFileName == null || profileFileName == null) {
                                viewModel.postFeedback("Uploading Trade Permit and Avatar is mandatory for distribution audit.")
                            } else {
                                viewModel.setupShopProfile(
                                    shopName = shopName,
                                    ownerName = ownerName,
                                    phone = phoneNumber,
                                    email = emailAddress,
                                    address = shopAddress,
                                    district = district,
                                    license = tradeLicenseNumber
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, "Upload all")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit Ledger Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Upload overlay spinner
        if (uploadingFile != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = uploadProgress,
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Uploading: $uploadingFile",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${(uploadProgress * 100).toInt()}% Transferred",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UploadAssetRow(
    label: String,
    fileName: String?,
    onUpload: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
            Text(
                text = fileName ?: "No file attached",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (fileName != null) MaterialTheme.colorScheme.primary else Color.Gray,
                maxLines = 1
            )
        }

        IconButton(
            onClick = onUpload,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (fileName != null) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            )
        ) {
            Icon(
                imageVector = if (fileName != null) Icons.Default.TaskAlt else Icons.Default.FileUpload,
                contentDescription = "Upload",
                tint = if (fileName != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
        }
    }
}
