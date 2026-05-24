package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.DashboardTab
import com.example.ui.PharmaViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreens(viewModel: PharmaViewModel) {
    val user = viewModel.currentUser.collectAsState().value ?: return
    val orders = viewModel.allOrders.collectAsState().value
    val cart = viewModel.activeCart.collectAsState().value
    val shops = viewModel.allShopProfiles.collectAsState().value
    val products = viewModel.allProducts.collectAsState().value
    val scope = rememberCoroutineScope()

    // Tab for Shop Owner: CART vs HISTORY
    var activeSubTab by remember { mutableStateOf(if (cart.isNotEmpty() && user.role == "SHOP_OWNER") 0 else 1) }

    // Admin states
    val selectedOrderIds by viewModel.selectedOrderIds.collectAsState()
    var editingOrder by remember { mutableStateOf<Order?>(null) }
    var showManualOrderPanel by remember { mutableStateOf(false) }

    // Filters
    var statusFilter by remember { mutableStateOf("All") }
    val statuses = listOf("All", "Pending", "Processing", "Confirmed", "Delivered", "Cancelled")

    // Sort order
    val filteredOrders = remember(orders, user.id, user.role, statusFilter) {
        val userOrders = if (user.role == "ADMIN") orders else orders.filter { it.shopId == user.id }
        if (statusFilter == "All") userOrders else userOrders.filter { it.status == statusFilter }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // USER OR ROLE SPECIFIC SWITCHERS
        if (user.role == "SHOP_OWNER") {
            TabRow(selectedTabIndex = activeSubTab, modifier = Modifier.padding(bottom = 12.dp)) {
                Tab(
                    selected = activeSubTab == 0,
                    onClick = { activeSubTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Wholesale Cart")
                            if (cart.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge { Text("${cart.sumOf { it.quantity }}") }
                            }
                        }
                    }
                )
                Tab(
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 },
                    text = { Text("Fulfillment History") }
                )
            }
        } else {
            // ADMIN MANAGEMENT HEADER
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Partner Orders Control",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Audit, modify, and consolidate commercial shipments", fontSize = 11.sp, color = Color.Gray)
                }

                Button(
                    onClick = { showManualOrderPanel = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.BorderColor, "Manual Order", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Draft Order", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Bulk actions banner when orders are selected (consolidation!)
            AnimatedVisibility(visible = selectedOrderIds.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, "Bulk Items", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${selectedOrderIds.size} Orders Checked for Consolidated Invoice",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Row {
                            TextButton(onClick = { viewModel.clearOrderSelection() }) {
                                Text("Clear", color = MaterialTheme.colorScheme.error)
                            }
                            Button(
                                onClick = { viewModel.generateInvoiceForSelectedOrders() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Generate Merged Bills", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // FILTER DRIPS FOR ADMIN
            Text(
                text = "Filter orders status:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            LazyRowForFilters(
                items = statuses,
                selected = statusFilter,
                onSelected = { statusFilter = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // CONTENT PRESENTATIONS
        if (user.role == "SHOP_OWNER" && activeSubTab == 0) {
            // Cart presentation
            CartModule(cart = cart, viewModel = viewModel)
        } else {
            // Fulfillment / Orders history
            if (filteredOrders.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReceiptLong, "Logs list empty", modifier = Modifier.size(54.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No orders recorded under this status.", color = Color.Gray)
                        Text("Change selectors above to query other states.", fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredOrders) { order ->
                        val isChecked = selectedOrderIds.contains(order.id)
                        AdminOrderRowCard(
                            order = order,
                            isAdmin = user.role == "ADMIN",
                            isChecked = isChecked,
                            onCheckChange = { viewModel.toggleOrderSelection(order.id) },
                            onEdit = { editingOrder = order },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    // Custom draft dialog panel (Admin create manual orders!)
    if (showManualOrderPanel) {
        ManualOrderPanel(
            shops = shops,
            products = products,
            onDismiss = { showManualOrderPanel = false },
            onSave = { selectedUserId, selectedProdList, discount, delivery, notes ->
                viewModel.adminCreateOrder(selectedUserId, selectedProdList, discount, delivery, notes)
                showManualOrderPanel = false
            }
        )
    }

    // Dynamic Edit dialog panel
    if (editingOrder != null) {
        AdminEditOrderDialog(
            order = editingOrder!!,
            onDismiss = { editingOrder = null },
            onSave = { status, disc, delPrice, note ->
                viewModel.updateOrderStatus(editingOrder!!.id, status)
                // Also update other pricing characteristics
                val modified = editingOrder!!.copy(discount = disc, deliveryCharge = delPrice, notes = note)
                scope.launch {
                    AppDatabase.getDatabase(viewModel.getApplication()).pharmaDao().updateOrder(modified)
                }
                editingOrder = null
            }
        )
    }
}

// ==========================================
// RETAIL CART SUBMODULE
// ==========================================
@Composable
fun CartModule(cart: List<CartItem>, viewModel: PharmaViewModel) {
    var notes by remember { mutableStateOf("") }

    if (cart.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.AddShoppingCart, "Cart Empty", modifier = Modifier.size(60.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your wholesale cart is currently empty.", fontWeight = FontWeight.Bold, color = Color.Gray)
                Text(
                    text = "Visit the Product Catalog, view available stocks, and add medical SKU items at wholesale prices to place an order.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.setTab(DashboardTab.CATALOG) }) {
                    Text("Browse Catalog")
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Secure Wholesale Cart Items:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cart) { item ->
                    CartItemRow(item = item, viewModel = viewModel)
                }
            }

            // Summary calculations total
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val subTotal = cart.sumOf { it.price * it.quantity }
                    val deliveryMock = if (subTotal > 500) 0.0 else 25.0 // Free delivery on major orders!
                    val grandT = subTotal + deliveryMock

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Wholesale Subtotal", fontSize = 12.sp, color = Color.Gray)
                        Text("$${String.format(Locale.US, "%.2f", subTotal)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Simulated Logistics Fee", fontSize = 12.sp, color = Color.Gray)
                        Text(if (deliveryMock == 0.0) "FREE OVER $500" else "$${String.format(Locale.US, "%.2f", deliveryMock)}", fontSize = 12.sp, color = if (deliveryMock == 0.0) Color(0xFF2E7D32) else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Consolidated Valuation Due", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("$${String.format(Locale.US, "%.2f", grandT)}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Logistics Routing / Order Notes") },
                        placeholder = { Text("Enter deliver schedules, gating directions, or instructions...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated checkout trigger
                    Button(
                        onClick = { viewModel.placeOrder(notes) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, "Place order")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit Requisition Request", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(item: CartItem, viewModel: PharmaViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Wholesale Rate: $${item.price} / unit", fontSize = 11.sp, color = Color.Gray)
                val lineT = item.price * item.quantity
                Text("Line Total: $${String.format(Locale.US, "%.2f", lineT)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            // Adjust count in cart
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.updateCartQuantity(item, -1) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Remove, "Sub", modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "${item.quantity}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = { viewModel.updateCartQuantity(item, 1) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, "Add", modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = { viewModel.removeFromCart(item) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ==========================================
// ADMIN RENDERS
// ==========================================

@Composable
fun AdminOrderRowCard(
    order: Order,
    isAdmin: Boolean,
    isChecked: Boolean,
    onCheckChange: () -> Unit,
    onEdit: () -> Unit,
    viewModel: PharmaViewModel
) {
    val dateStr = remember(order.dateMillis) {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(order.dateMillis))
    }

    var expandedDetails by remember { mutableStateOf(false) }
    var itemsFlowState by remember { mutableStateOf<List<OrderItem>>(emptyList()) }

    // Collect order items on expanded
    LaunchedEffect(expandedDetails) {
        if (expandedDetails) {
            val db = AppDatabase.getDatabase(viewModel.getApplication())
            itemsFlowState = db.pharmaDao().getOrderItemsList(order.id)
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAdmin && order.status != "Cancelled" && order.status != "Delivered") {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onCheckChange() },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Warehouse Order #${order.id}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Shop: ${order.shopName} • $dateStr",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", order.totalAmount)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusChip(status = order.status)
                }
            }

            // Expands detail arrow
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expandedDetails = !expandedDetails }.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (expandedDetails) "Contract Details" else "Expand Line Items", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = if (expandedDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }

            AnimatedVisibility(visible = expandedDetails) {
                Column(modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                    Divider()
                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Fulfillment Items Specification:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                    if (itemsFlowState.isEmpty()) {
                        Text("Retrieving transaction details from ledger...", fontSize = 11.sp, color = Color.LightGray)
                    } else {
                        itemsFlowState.forEach { detail ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• ${detail.quantity} x ${detail.productName}", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("$${String.format(Locale.US, "%.2f", detail.price * detail.quantity)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(6.dp))

                    if (order.discount > 0) RowDetail("Special Deducted Discount", "-$${order.discount}")
                    if (order.deliveryCharge > 0) RowDetail("Inbound Delivery Logistics Fee", "$${order.deliveryCharge}")
                    if (order.notes.isNotEmpty()) RowDetail("Order Note Annotation", order.notes)

                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(
                                onClick = onEdit,
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.EditCalendar, "Modify Specs", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Audit Specs", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowDetail(lbl: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(lbl, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}

// ==========================================
// ADMIN EDIT ORDER COMPONENT DIALOG
// ==========================================
@Composable
fun AdminEditOrderDialog(
    order: Order,
    onDismiss: () -> Unit,
    onSave: (status: String, discount: Double, delivery: Double, notes: String) -> Unit
) {
    var status by remember { mutableStateOf(order.status) }
    var discount by remember { mutableStateOf(order.discount.toString()) }
    var deliveryPrice by remember { mutableStateOf(order.deliveryCharge.toString()) }
    var notes by remember { mutableStateOf(order.notes) }

    val statusOpts = listOf("Pending", "Processing", "Confirmed", "Delivered", "Cancelled")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audit Specs for Order #${order.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                
                // Status Dropdown
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        label = { Text("Fulfillment Status") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.clickable { expanded = true },
                        readOnly = true
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        statusOpts.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = {
                                status = opt
                                expanded = false
                            })
                        }
                    }
                }

                OutlinedTextField(
                    value = discount,
                    onValueChange = { discount = it },
                    label = { Text("Apply Trade Discount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = deliveryPrice,
                    onValueChange = { deliveryPrice = it },
                    label = { Text("Logistics Delivery Charge ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Enterprise Notes Annotation") },
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val discV = discount.toDoubleOrNull() ?: 0.0
                    val delV = deliveryPrice.toDoubleOrNull() ?: 0.0
                    onSave(status, discV, delV, notes)
                }
            ) {
                Text("Save Ledger Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    )
}

// ==========================================
// DRAFT MANUAL ORDER DIALOG (ADMIN)
// ==========================================
@Composable
fun ManualOrderPanel(
    shops: List<ShopProfile>,
    products: List<Product>,
    onDismiss: () -> Unit,
    onSave: (targetShopUserId: Long, selectedProds: List<Pair<Product, Int>>, discount: Double, delivery: Double, notes: String) -> Unit
) {
    var selectedShopIndex by remember { mutableStateOf(-1) }
    var discount by remember { mutableStateOf("0.0") }
    var deliveryFee by remember { mutableStateOf("15.0") }
    var notes by remember { mutableStateOf("") }

    // List of added products to this manual order
    val transactionProducts = remember { mutableStateListOf<Pair<Product, Int>>() }

    // Form selection
    var productExpanded by remember { mutableStateOf(false) }
    var shopExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Draft Physical Distributed Order", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                
                // 1. Choose Retailer Shop
                Box {
                    OutlinedTextField(
                        value = if (selectedShopIndex >= 0) shops[selectedShopIndex].shopName else "Select Franchise Partner *",
                        onValueChange = {},
                        label = { Text("Enterprise Buyer") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.clickable { shopExpanded = true }.fillMaxWidth(),
                        readOnly = true
                    )
                    DropdownMenu(expanded = shopExpanded, onDismissRequest = { shopExpanded = false }) {
                        shops.forEachIndexed { index, shop ->
                            DropdownMenuItem(text = { Text(shop.shopName) }, onClick = {
                                selectedShopIndex = index
                                shopExpanded = false
                            })
                        }
                    }
                }

                // 2. Select SKU and quantity to Append
                Box {
                    OutlinedTextField(
                        value = "Append Medical SKU Item",
                        onValueChange = {},
                        label = { Text("Add Item Block") },
                        trailingIcon = { Icon(Icons.Default.AddCircleOutline, null) },
                        modifier = Modifier.clickable { productExpanded = true }.fillMaxWidth(),
                        readOnly = true
                    )
                    DropdownMenu(expanded = productExpanded, onDismissRequest = { productExpanded = false }) {
                        products.forEach { prod ->
                            DropdownMenuItem(
                                text = { Text("${prod.name} (Wholesale: $${prod.wholesalePrice} | Stock: ${prod.stockQuantity})") },
                                onClick = {
                                    val matchIdx = transactionProducts.indexOfFirst { it.first.id == prod.id }
                                    if (matchIdx >= 0) {
                                        val existing = transactionProducts[matchIdx]
                                        transactionProducts[matchIdx] = Pair(existing.first, existing.second + 1)
                                    } else {
                                        transactionProducts.add(Pair(prod, 1))
                                    }
                                    productExpanded = false
                                }
                            )
                        }
                    }
                }

                // Added items list
                if (transactionProducts.isNotEmpty()) {
                    Text("Selected Line items:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            transactionProducts.forEachIndexed { idx, pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• ${pair.first.name}", fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            if (pair.second > 1) {
                                                transactionProducts[idx] = Pair(pair.first, pair.second - 1)
                                            } else {
                                                transactionProducts.removeAt(idx)
                                            }
                                        }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Remove, null, modifier = Modifier.size(12.dp))
                                        }
                                        Text("${pair.second}", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                        IconButton(onClick = {
                                            transactionProducts[idx] = Pair(pair.first, pair.second + 1)
                                        }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = discount,
                    onValueChange = { discount = it },
                    label = { Text("Trade Discount Adjustment ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = deliveryFee,
                    onValueChange = { deliveryFee = it },
                    label = { Text("Logistics Delivery Charge ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Delivery Schedule Note / Annotations") },
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedShopIndex < 0 || transactionProducts.isEmpty()) {
                        // Keep open with standard error
                    } else {
                        val dVal = discount.toDoubleOrNull() ?: 0.0
                        val fVal = deliveryFee.toDoubleOrNull() ?: 0.0
                        val targetShopId = shops[selectedShopIndex].userId
                        onSave(targetShopId, transactionProducts.toList(), dVal, fVal, notes)
                    }
                }
            ) {
                Text("Draft Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    )
}
