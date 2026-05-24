package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.PharmaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreens(viewModel: PharmaViewModel) {
    val user = viewModel.currentUser.collectAsState().value ?: return
    val products = viewModel.allProducts.collectAsState().value

    // Search and filters
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoryFilter by viewModel.filterCategory.collectAsState()
    val availFilter by viewModel.filterAvailability.collectAsState()
    val priceFilter by viewModel.filterPriceRange.collectAsState()

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var viewingProductDetails by remember { mutableStateOf<Product?>(null) }

    // Categories List
    val categories = listOf("All", "Antibiotic", "Cardiovascular", "Gastrointestinal", "Antidiabetic", "Analgesic", "Other")

    // Filter products locally as they load
    val filteredProducts = remember(products, searchQuery, categoryFilter, availFilter, priceFilter) {
        products.filter { prod ->
            // Search
            val queryMatch = searchQuery.isEmpty() ||
                    prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.genericName.contains(searchQuery, ignoreCase = true) ||
                    prod.brandName.contains(searchQuery, ignoreCase = true) ||
                    prod.skuCode.contains(searchQuery, ignoreCase = true)

            // Category
            val catMatch = categoryFilter == "All" || prod.category == categoryFilter

            // Availability
            val availMatch = when (availFilter) {
                "All" -> true
                "In Stock" -> prod.stockQuantity >= 100
                "Low Stock" -> prod.stockQuantity in 1..99
                "Out of Stock" -> prod.stockQuantity <= 0
                else -> true
            }

            // Price range
            val priceMatch = when (priceFilter) {
                "All" -> true
                "Wholesale < $5" -> prod.wholesalePrice < 5.0
                "Wholesale $5 - $15" -> prod.wholesalePrice in 5.0..15.0
                "Wholesale > $15" -> prod.wholesalePrice > 15.0
                else -> true
            }

            queryMatch && catMatch && availMatch && priceMatch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // SEARCH BAR AND FILTERS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search medicine generic/brand/SKU...") },
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    } else null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )

                if (user.role == "ADMIN") {
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Icon(Icons.Default.Add, "Add")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add SKU", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CATEGORY CHIP ROW
            Text(
                text = "Select Category Sector",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            LazyRowForFilters(
                items = categories,
                selected = categoryFilter,
                onSelected = { viewModel.filterCategory.value = it }
            )

            // ADDITIONAL FILTERS (Availability and Price Ranges)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stock filter dropdown
                Box(modifier = Modifier.weight(1f)) {
                    FilterDropdown(
                        label = "Stock Status: $availFilter",
                        options = listOf("All", "In Stock", "Low Stock", "Out of Stock"),
                        onSelect = { viewModel.filterAvailability.value = it }
                    )
                }
                // Price filter dropdown
                Box(modifier = Modifier.weight(1f)) {
                    FilterDropdown(
                        label = "Price: $priceFilter",
                        options = listOf("All", "Wholesale < $5", "Wholesale $5 - $15", "Wholesale > $15"),
                        onSelect = { viewModel.filterPriceRange.value = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // PRODUCT LIST ROWS
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Catalog Inventory Listing (${filteredProducts.size} found)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, "Not found", modifier = Modifier.size(54.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No matching pharmaceuticals on record.", color = Color.Gray)
                        Text("Verify search query or clearance categories.", fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredProducts) { item ->
                        ProductItemCard(
                            product = item,
                            isAdmin = user.role == "ADMIN",
                            onEdit = { editingProduct = item },
                            onDelete = { viewModel.deleteProduct(item) },
                            onCardClick = { viewingProductDetails = item },
                            onAdjustStock = { change ->
                                val adjustedQty = (item.stockQuantity + change).coerceAtLeast(0)
                                viewModel.updateProduct(item.copy(stockQuantity = adjustedQty))
                            }
                        )
                    }
                }
            }
        }

        // 1. ADD NEW SKU DIALOG (ADMIN)
        if (showAddDialog) {
            ProductEditDialog(
                onDismiss = { showAddDialog = false },
                onSave = { name, gen, brand, cat, desc, retail, wholesale, stock, batch, exp, sku ->
                    viewModel.addProduct(name, gen, brand, cat, desc, retail, wholesale, stock, batch, exp, sku)
                    showAddDialog = false
                }
            )
        }

        // 2. EDIT SKU DIALOG (ADMIN)
        if (editingProduct != null) {
            val prod = editingProduct!!
            ProductEditDialog(
                product = prod,
                onDismiss = { editingProduct = null },
                onSave = { name, gen, brand, cat, desc, retail, wholesale, stock, batch, exp, sku ->
                    viewModel.updateProduct(
                        prod.copy(
                            name = name,
                            genericName = gen,
                            brandName = brand,
                            category = cat,
                            description = desc,
                            unitPrice = retail,
                            wholesalePrice = wholesale,
                            stockQuantity = stock,
                            batchNumber = batch,
                            expiryDate = exp,
                            skuCode = sku
                        )
                    )
                    editingProduct = null
                }
            )
        }

        // 3. PRODUCT SPEC DETAILS EXPAND (SHOP OWNER & ADMIN BOTH)
        if (viewingProductDetails != null) {
            ProductDetailsSheet(
                product = viewingProductDetails!!,
                isShopOwner = user.role == "SHOP_OWNER",
                onDismiss = { viewingProductDetails = null },
                onAddToCart = { qty ->
                    viewModel.addToCart(viewingProductDetails!!, qty)
                    viewingProductDetails = null
                }
            )
        }
    }
}

@Composable
fun LazyRowForFilters(
    items: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { filterItem ->
            val isSelected = filterItem == selected
            InputChip(
                selected = isSelected,
                onClick = { onSelected(filterItem) },
                label = { Text(filterItem, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = InputChipDefaults.inputChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun FilterDropdown(
    label: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .clip(RoundedCornerShape(8.dp)),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCardClick: () -> Unit,
    onAdjustStock: (Int) -> Unit
) {
    // Determine alerts
    val isLowStock = product.stockQuantity in 1..99
    val isOutOfStock = product.stockQuantity <= 0

    // Compute critical expiry alerts (<= 30 Days)
    val isCriticalExpiry = remember(product.expiryDate) {
        try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val expiry = format.parse(product.expiryDate)
            if (expiry != null) {
                val diff = expiry.time - System.currentTimeMillis()
                diff < (30L * 24 * 60 * 60 * 1000) && diff > 0
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Category Badge
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = product.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = product.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Composition: ${product.genericName} (${product.brandName})",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Price Section
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", product.wholesalePrice)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "MRP: $${String.format(Locale.US, "%.2f", product.unitPrice)}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            // Expiry and Quantity trackers row
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                // Stock tracker
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isOutOfStock) Icons.Default.Cancel else Icons.Default.Inventory2,
                        contentDescription = "Stock",
                        tint = when {
                            isOutOfStock -> Color.Red
                            isLowStock -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.secondary
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Stock: ${product.stockQuantity} Units",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isOutOfStock -> Color.Red
                            isLowStock -> Color(0xFFFF9800)
                            else -> Color.Gray
                        }
                    )
                }

                // Batch & Expiry
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCriticalExpiry) Icons.Default.HourglassBottom else Icons.Default.CalendarToday,
                        contentDescription = "Expiry",
                        tint = if (isCriticalExpiry) Color.Red else Color.Gray,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Exp: ${product.expiryDate}",
                        fontSize = 11.sp,
                        color = if (isCriticalExpiry) Color.Red else Color.Gray,
                        fontWeight = if (isCriticalExpiry) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            // Warnings bar
            if (isCriticalExpiry || isLowStock || isOutOfStock) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationImportant, "Alert", tint = Color.Red, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isOutOfStock -> "SKU FULLY DEPLETED! Requisition required."
                                isCriticalExpiry -> "HIGH WARNING! Drug expires within 30 days."
                                else -> "Warning: Stock is dropping below optimum safety buffer (<100)."
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                }
            }

            // ADMIN CONTROL BAR (ONLY DISPLAYED IF ROLE IS WHOLESALE ADMIN!)
            if (isAdmin) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick inventory adjustments
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Add Stock: ", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = { onAdjustStock(-10) },
                            modifier = Modifier.size(24.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-10", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { onAdjustStock(10) },
                            modifier = Modifier.size(24.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+10", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { onAdjustStock(100) },
                            modifier = Modifier.size(26.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+100", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Edit & delete triggers
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// 4. BEAUTIFUL PRODUCT SPEC EDIT / ADD SHEET DIALOG
@Composable
fun ProductEditDialog(
    product: Product? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String, generic: String, brand: String, category: String,
        description: String, retail: Double, wholesale: Double, stock: Int,
        batch: String, expiry: String, sku: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var generic by remember { mutableStateOf(product?.genericName ?: "") }
    var brand by remember { mutableStateOf(product?.brandName ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "Antibiotic") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var retailPrice by remember { mutableStateOf(product?.unitPrice?.toString() ?: "") }
    var wholesalePrice by remember { mutableStateOf(product?.wholesalePrice?.toString() ?: "") }
    var stockQuantity by remember { mutableStateOf(product?.stockQuantity?.toString() ?: "") }
    var batchNumber by remember { mutableStateOf(product?.batchNumber ?: "") }
    var expiryDate by remember { mutableStateOf(product?.expiryDate ?: "2027-12-31") }
    var skuCode by remember { mutableStateOf(product?.skuCode ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (product == null) "Log New Medicine Entry" else "Modify Physical SKU Specs",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val rVal = retailPrice.toDoubleOrNull() ?: 0.0
                    val wVal = wholesalePrice.toDoubleOrNull() ?: 0.0
                    val sVal = stockQuantity.toIntOrNull() ?: 0
                    if (name.isEmpty() || generic.isEmpty() || brand.isEmpty() || batchNumber.isEmpty() || skuCode.isEmpty()) {
                        // Display simple alert inside
                    } else {
                        onSave(name, generic, brand, category, description, rVal, wVal, sVal, batchNumber, expiryDate, skuCode)
                    }
                }
            ) {
                Text("Confirm Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name *") })
                OutlinedTextField(value = generic, onValueChange = { generic = it }, label = { Text("Generic Chem Compound *") })
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand Manufacturer *") })

                // Category selector string options
                val cats = listOf("Antibiotic", "Cardiovascular", "Gastrointestinal", "Antidiabetic", "Analgesic", "Other")
                var catExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        label = { Text("Inventory Category") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.clickable { catExpanded = true },
                        readOnly = true
                    )
                    DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        cats.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = {
                                category = c
                                catExpanded = false
                            })
                        }
                    }
                }

                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Physiological Description / Uses") }, maxLines = 2)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = wholesalePrice,
                        onValueChange = { wholesalePrice = it },
                        label = { Text("Wholesale Rate *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = retailPrice,
                        onValueChange = { retailPrice = it },
                        label = { Text("Retail MRP *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = stockQuantity,
                        onValueChange = { stockQuantity = it },
                        label = { Text("Current Stock *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = batchNumber,
                        onValueChange = { batchNumber = it },
                        label = { Text("Batch Lot # *") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = { Text("Expiry (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = skuCode,
                        onValueChange = { skuCode = it },
                        label = { Text("SKU Barcode Code *") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}

// 5. DRUG CORE SPECIFICATION / ADD TO CART SHEET
@Composable
fun ProductDetailsSheet(
    product: Product,
    isShopOwner: Boolean,
    onDismiss: () -> Unit,
    onAddToCart: (Int) -> Unit
) {
    var purchaseQty by remember { mutableStateOf(1) }
    val maxStock = product.stockQuantity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = product.name,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Info blocks
                Text(
                    text = "Composition: ${product.genericName} (${product.brandName})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = product.description.ifEmpty { "No physiological monograph attached. Approved under wholesale medical distribution rules." },
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )

                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DetailLine(label = "Category Domain", valStr = product.category)
                        DetailLine(label = "SKU Barcode", valStr = product.skuCode)
                        DetailLine(label = "Batch Number", valStr = product.batchNumber)
                        DetailLine(label = "Expiry Schedule", valStr = product.expiryDate)
                        DetailLine(label = "Wholesale Unit Rate", valStr = "$${product.wholesalePrice}")
                        DetailLine(label = "Wholesale Total Stock", valStr = "$maxStock Units")
                    }
                }

                // Cart Count selector for Shop Owners!
                if (isShopOwner && maxStock > 0) {
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Requisition Quantity: ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (purchaseQty > 1) purchaseQty-- }) {
                                Icon(Icons.Default.Remove, "Sub")
                            }
                            Text(
                                text = "$purchaseQty",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(onClick = { if (purchaseQty < maxStock) purchaseQty++ }) {
                                Icon(Icons.Default.Add, "Add")
                            }
                        }
                    }
                    val totalCalc = purchaseQty * product.wholesalePrice
                    Text(
                        text = "Wholesale Cost: $${String.format(Locale.US, "%.2f", totalCalc)}",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isShopOwner && maxStock > 0) {
                    Button(
                        onClick = { onAddToCart(purchaseQty) }
                    ) {
                        Icon(Icons.Default.AddShoppingCart, "Cart")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add wholesale to cart")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
fun DetailLine(label: String, valStr: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(valStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}
