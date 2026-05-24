package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.DashboardTab
import com.example.ui.PharmaViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: PharmaViewModel) {
    val user = viewModel.currentUser.collectAsState().value ?: return
    val products = viewModel.allProducts.collectAsState().value
    val orders = viewModel.allOrders.collectAsState().value
    val invoices = viewModel.allInvoices.collectAsState().value
    val shops = viewModel.allShopProfiles.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        // Welcome Header Profile Banner
        DashboardWelcomeHeader(user, viewModel)

        if (user.role == "ADMIN") {
            AdminDashboard(products, orders, invoices, shops, viewModel)
        } else {
            ShopOwnerDashboard(user, products, orders, invoices, viewModel)
        }
    }
}

@Composable
fun DashboardWelcomeHeader(user: UserAccount, viewModel: PharmaViewModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface) // Clean crisp white
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular brand initial avatar
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.fullName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "MedStore Pro",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B) // Slate-800
                        )
                        if (user.role == "ADMIN") {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "ADMIN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (user.status == "VERIFIED") {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Blue Badge",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "Hello, ${user.fullName} • ${if (user.role == "ADMIN") "Distributor Hub" else "Verified Shop"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B) // Slate-500
                    )
                }
            }

            // Quick notifications trigger bell inside header
            IconButton(
                onClick = { viewModel.setTab(DashboardTab.SETTINGS) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Show Alerts",
                    tint = Color(0xFF475569) // Slate-600
                )
            }
        }
    }
}

// ==========================================
// RETAILER / SHOP OWNER DASHBOARD
// ==========================================
@Composable
fun ShopOwnerDashboard(
    user: UserAccount,
    products: List<Product>,
    orders: List<Order>,
    invoices: List<Invoice>,
    viewModel: PharmaViewModel
) {
    // Filter active orders for this specific shop
    val shopOrders = remember(orders, user.id) {
        orders.filter { it.shopId == user.id }
    }
    val shopInvoices = remember(invoices, user.id) {
        invoices.filter { it.shopId == user.id }
    }

    // Calculations
    val totalOrdersCount = shopOrders.size
    val pendingPaymentsCount = shopInvoices.count { it.status == "Pending" || it.status == "Processing" }

    var dueBalance = 0.0
    var paidBillsTotal = 0.0
    for (inv in shopInvoices) {
        if (inv.status == "Pending" || inv.status == "Processing") {
            dueBalance += inv.dueAmount
        } else if (inv.status == "Paid") {
            paidBillsTotal += inv.grandTotal
        }
    }

    var monthlyPurchase = 0.0
    val startOfMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
    }.timeInMillis

    for (ord in shopOrders) {
        if (ord.dateMillis >= startOfMonth && ord.status != "Cancelled") {
            monthlyPurchase += ord.totalAmount
        }
    }

    val lastOrderAmount = if (shopOrders.isNotEmpty()) shopOrders.first().totalAmount else 0.0

    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

        // Badge banner if not verified - HTML warn design
        if (user.status == "PENDING_VERIFICATION") {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)), // bg-orange-50 equivalent
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEDD5)), // border-orange-100
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFFE4E6).copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = "Pending Audit",
                            tint = Color(0xFFEA580C)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Awaiting Verification Audit",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC2410C)
                        )
                        Text(
                            text = "Admin is auditing trade permits. Some wholesale cart options are constrained.",
                            fontSize = 11.sp,
                            color = Color(0xFFEA580C)
                        )
                    }
                }
            }
        }

        // Beautiful Grid of Dashboard Cards
        Text(
            text = "Distribution Ledger Summary",
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // High-fidelity Primary blue gradient balance card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "DUE BALANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", dueBalance)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (dueBalance > 0) "${shopInvoices.count { it.status == "Pending" || it.status == "Processing" }} Overdue" else "Paid Up",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Crisp White styled KPI card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "MONTHLY PURCHASE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", monthlyPurchase)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Text(
                        text = "+12% vs last month",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard(
                label = "Total Orders",
                value = "$totalOrdersCount Units",
                icon = Icons.Default.Assessment,
                colorAccent = Color(0xFF2563EB),
                modifier = Modifier.weight(1f)
            )
            DashboardCard(
                label = "Pending Payments",
                value = "$pendingPaymentsCount Bills",
                icon = Icons.Default.PendingActions,
                colorAccent = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }

        // Secondary statistics table
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Accounting Highlights",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                TableRow(label = "Outstanding Balance Due", value = "$${String.format(Locale.US, "%.2f", dueBalance)}", colorVal = MaterialTheme.colorScheme.error)
                HorizontalDivider()
                TableRow(label = "Paid Invoices Volume", value = "$${String.format(Locale.US, "%.2f", paidBillsTotal)}", colorVal = Color.Gray)
                HorizontalDivider()
                TableRow(label = "Last Logged Order Total", value = "$${String.format(Locale.US, "%.2f", lastOrderAmount)}", colorVal = Color.Gray)
                HorizontalDivider()
                TableRow(label = "Pending Order requests", value = "${shopOrders.count { it.status == "Pending" }} requisitions", colorVal = Color.Gray)
            }
        }

        // Recent retail orders
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Recent Distribution Orders",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = { viewModel.setTab(DashboardTab.ORDERS) }) {
                Text("View All")
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "All", modifier = Modifier.size(16.dp))
            }
        }

        if (shopOrders.isEmpty()) {
            EmptyStateCard(message = "No distributed orders submitted yet.")
        } else {
            shopOrders.take(3).forEach { order ->
                OrderRowItem(order = order, onClick = { viewModel.setTab(DashboardTab.ORDERS) })
            }
        }
    }
}

// ==========================================
// WHOLESALE ADMIN DASHBOARD
// ==========================================
@Composable
fun AdminDashboard(
    products: List<Product>,
    orders: List<Order>,
    invoices: List<Invoice>,
    shops: List<ShopProfile>,
    viewModel: PharmaViewModel
) {
    // Alarms calculation
    val lowStockCount = products.count { it.stockQuantity < 100 }
    val outOfStockCount = products.count { it.stockQuantity <= 0 }

    // Expiry warnings (< 30 days) count
    val calendar = Calendar.getInstance()
    val checkFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val criticalExpiryAlerts = remember(products) {
        products.count { prod ->
            try {
                val expiry = checkFormat.parse(prod.expiryDate)
                if (expiry != null) {
                    val diff = expiry.time - calendar.timeInMillis
                    diff < (30L * 24 * 60 * 60 * 1000) && diff > 0 // Within 30 days
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    val pendingVerifications = remember(shops) {
        shops.filter { it.status == "Pending" }
    }

    var totalOutstandingDue = 0.0
    var totalSalesReceipts = 0.0
    for (inv in invoices) {
        if (inv.status == "Paid") {
            totalSalesReceipts += inv.grandTotal
        } else if (inv.status != "Cancelled") {
            totalOutstandingDue += inv.dueAmount
        }
    }

    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

        // Alarm Banner if stock or exps are critical
        if (lowStockCount > 0 || criticalExpiryAlerts > 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "System alert",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Urgent Inventory Actions Required",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "$lowStockCount Low Stock drugs • $criticalExpiryAlerts Close Expiry items needing markdown.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Top Row Quick Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DashboardMiniCard(
                title = "Wholesale Sales",
                value = "$${String.format(Locale.US, "%.0f", totalSalesReceipts)}",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            DashboardMiniCard(
                title = "Outstanding",
                value = "$${String.format(Locale.US, "%.0f", totalOutstandingDue)}",
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            DashboardMiniCard(
                title = "Partner Stores",
                value = "${shops.size} Shops",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        // GORGEOUS GRAPH DRAWING (Compose Canvas)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Weekly Wholesale Demand Frequency",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Consolidated supply requisition flows across sectors",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // The Canvas Graph
                val strokeCol = MaterialTheme.colorScheme.primary
                val strokeTeal = MaterialTheme.colorScheme.secondary
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                ) {
                    val width = size.width
                    val height = size.height

                    // Drawing soft coordinates grid lines
                    for (i in 1..4) {
                        val y = (height / 5) * i
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // Simulated data nodes (demand frequency over week)
                    val points = listOf(0.12f, 0.45f, 0.32f, 0.72f, 0.55f, 0.92f, 0.65f)
                    val px = mutableListOf<Offset>()
                    val step = width / (points.size - 1)

                    for (idx in points.indices) {
                        val x = idx * step
                        val y = height - (points[idx] * (height - 20.dp.toPx())) - 10.dp.toPx()
                        px.add(Offset(x, y))
                    }

                    // Create glowing curved path
                    val path = Path().apply {
                        moveTo(px[0].x, px[0].y)
                        for (i in 1 until px.size) {
                            val prev = px[i - 1]
                            val curr = px[i]
                            cubicTo(
                                (prev.x + curr.x) / 2, prev.y,
                                (prev.x + curr.x) / 2, curr.y,
                                curr.x, curr.y
                            )
                        }
                    }

                    // Fill Gradient Area
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                strokeCol.copy(alpha = 0.25f),
                                strokeCol.copy(alpha = 0.01f)
                            )
                        )
                    )

                    // Draw Stroke
                    drawPath(
                        path = path,
                        color = strokeCol,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Node circles
                    px.forEach { pt ->
                        drawCircle(
                            color = strokeTeal,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    days.forEach { day ->
                        Text(day, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }
            }
        }

        // PENDING APPLICANTS TO VERIFY (CORE REQ!)
        Text(
            text = "Pending Registry Admissions (${pendingVerifications.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        if (pendingVerifications.isEmpty()) {
            EmptyStateCard(message = "All enterprise franchise licenses fully audited.")
        } else {
            pendingVerifications.forEach { applicant ->
                ApplicantVerifyCard(applicant, viewModel)
            }
        }

        // Quick catalog metrics
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Wholesale Hub Assets Status",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                TableRow(label = "Total Active Drugs", value = "${products.size} medical SKU items", colorVal = Color.Gray)
                HorizontalDivider()
                TableRow(label = "Low stock drug lines (<100 units)", value = "$lowStockCount lines", colorVal = if (lowStockCount > 0) MaterialTheme.colorScheme.error else Color.Gray)
                HorizontalDivider()
                TableRow(label = "Completely Depleted (0 units)", value = "$outOfStockCount lines", colorVal = if (outOfStockCount > 0) MaterialTheme.colorScheme.error else Color.Gray)
                HorizontalDivider()
                TableRow(label = "Pending verify shop requests", value = "${pendingVerifications.size} applicants", colorVal = Color.Gray)
            }
        }
    }
}

// ==========================================
// SUB COMPONENTS
// ==========================================
@Composable
fun DashboardCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colorAccent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)), // soft slate-200 border
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorAccent.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B), // Slate-500
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun DashboardMiniCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun TableRow(label: String, value: String, colorVal: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colorVal
        )
    }
}

@Composable
fun OrderRowItem(order: Order, onClick: () -> Unit) {
    val context = LocalContext.current
    val formattedDate = remember(order.dateMillis) {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(order.dateMillis))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Order #${order.id} • ${order.shopName}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$formattedDate • Notes: ${order.notes.ifEmpty { "None" }}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format(Locale.US, "%.2f", order.totalAmount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusChip(status = order.status)
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (bg, text) = when (status) {
        "Verified", "Confirmed", "Paid", "Delivered" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "Pending", "Processing" -> Pair(Color(0xFFE3F2FD), Color(0xFF0288D1))
        "Rejected", "Cancelled" -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        else -> Pair(Color(0xFFF5F5F5), Color(0xFF616161))
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ApplicantVerifyCard(applicant: ShopProfile, viewModel: PharmaViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = "Pharmacy",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = applicant.shopName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Proprietor: ${applicant.ownerFullName} • Dist: ${applicant.district}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // License Details Display box
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Compliance Records Check:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• Trade License #: ${applicant.tradeLicenseNumber}\n" +
                               "• Contact Phone: ${applicant.phoneNumber}\n" +
                               "• Address: ${applicant.shopAddress}",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Approve Reject trigger row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.rejectShop(applicant.userId) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.padding(end = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Close, "Reject", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject Application", fontSize = 11.sp)
                }

                Button(
                    onClick = { viewModel.approveShop(applicant.userId) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Check, "Approve", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve & Verify Badge", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Inventory,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}
