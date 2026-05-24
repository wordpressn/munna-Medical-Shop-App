package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.PharmaViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(viewModel: PharmaViewModel) {
    val user = viewModel.currentUser.collectAsState().value ?: return
    val shop = viewModel.currentShopProfile.collectAsState().value
    val notifications = viewModel.userNotifications.collectAsState().value

    var activeSubTab by remember { mutableStateOf(0) } // 0 = Notifications, 1 = Profile & Context Swap

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        
        // Tab segment row
        TabRow(selectedTabIndex = activeSubTab, modifier = Modifier.padding(bottom = 14.dp)) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Business Alerts")
                        if (notifications.any { !it.isRead }) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge { Text("${notifications.count { !it.isRead }}") }
                        }
                    }
                }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("Proprietor Profile & Role Swap") }
            )
        }

        if (activeSubTab == 0) {
            NotificationsSection(notifications, viewModel)
        } else {
            ProfileSettingsSection(user, shop, viewModel)
        }
    }
}

// ==========================================
// NOTIFICATIONS MODULE
// ==========================================
@Composable
fun NotificationsSection(notifications: List<Notification>, viewModel: PharmaViewModel) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Push alerts history log (${notifications.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            if (notifications.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearNotifications() },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.DoneAll, "Mark Read", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear unread tags", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, "Empty alerts", modifier = Modifier.size(54.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Your alerts tray is clean.", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("We'll push log shifts and stock drops here.", fontSize = 11.sp, color = Color.LightGray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications) { item ->
                    NotificationCardItem(item)
                }
            }
        }
    }
}

@Composable
fun NotificationCardItem(notif: Notification) {
    val timeStr = remember(notif.timestamp) {
        val diff = System.currentTimeMillis() - notif.timestamp
        when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> SimpleDateFormat("MMM dd, hh:mm a", Locale.US).format(Date(notif.timestamp))
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (!notif.isRead) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Priority icon indicator
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (!notif.isRead) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.25f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        notif.title.contains("STOCK", ignoreCase = true) || notif.title.contains("DEPLETED", ignoreCase = true) -> Icons.Default.Inventory
                        notif.title.contains("VERIFICATION", ignoreCase = true) || notif.title.contains("Verified", ignoreCase = true) -> Icons.Default.DomainVerification
                        notif.title.contains("Invoice", ignoreCase = true) -> Icons.Default.Paid
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = if (!notif.isRead) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notif.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (!notif.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = timeStr,
                        fontSize = 9.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = notif.message,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ==========================================
// PROFILE AND ROLE CONTEXT SWAP
// ==========================================
@Composable
fun ProfileSettingsSection(
    user: UserAccount,
    shop: ShopProfile?,
    viewModel: PharmaViewModel
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // 1. User details display
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Proprietor Membership Specs", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Divider()
                InfoKeyValue("Associated Name", user.fullName)
                InfoKeyValue("System Account Email", user.email)
                InfoKeyValue("Verification status", user.status)
                InfoKeyValue("Security Access Role", user.role)
                if (user.phoneNumber.isNotEmpty()) {
                    InfoKeyValue("Owner Telephone", user.phoneNumber)
                }
            }
        }

        // 2. Retailer shop info card
        if (shop != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Registered Shop Licenses", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Divider()
                    InfoKeyValue("Commercial Trade Name", shop.shopName)
                    InfoKeyValue("License Certificate #", shop.tradeLicenseNumber)
                    InfoKeyValue("District Location", shop.district)
                    InfoKeyValue("Gated Office Coordinates", shop.shopAddress)
                    InfoKeyValue("Compliance Approval status", shop.status)
                }
            }
        }

        // 3. ROLE CONTEXT SWAPE PANEL (GEMS FOR SPEEDY TESTING IN SIM)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Developer Context Role Switcher",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Swap ledger profiles instantly to test cross-role capabilities without retyping credentials.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Sarah Jenkins (Pending)
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                QuickSwapperRow(
                    name = "Sarah Jenkins (Retailer - Awaiting Audit)",
                    status = "Pending Setup, Pending Verification, Re-uploads flows",
                    onSwap = {
                        viewModel.login("pending@pharma.com", "pending123")
                    }
                )

                // John Carter (Verified Retailer)
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                QuickSwapperRow(
                    name = "John Carter (Retailer - Verified Outlet)",
                    status = "Browse catalogs, place orders, download multi-order receipts, whatsapp sharing, quotes",
                    onSwap = {
                        viewModel.login("shop@pharma.com", "owner123")
                    }
                )

                // Dr Emily Vance (Admin)
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                QuickSwapperRow(
                    name = "Dr. Emily Vance (Wholesale Admin Manager)",
                    status = "Approve shops, manipulate inventory metrics, manage manual orders, status controls, consolidated invoicing",
                    onSwap = {
                        viewModel.login("admin@pharma.com", "admin123")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Logout
        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Logout, "Logout")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout & Close Session", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuickSwapperRow(
    name: String,
    status: String,
    onSwap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSwap() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(status, fontSize = 9.sp, color = Color.Gray, lineHeight = 12.sp)
        }
        Icon(Icons.Default.SwapHoriz, "Swap", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun InfoKeyValue(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(key, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}
