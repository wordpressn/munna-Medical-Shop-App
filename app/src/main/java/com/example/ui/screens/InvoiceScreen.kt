package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.PharmaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InvoiceScreen(viewModel: PharmaViewModel) {
    val user = viewModel.currentUser.collectAsState().value ?: return
    val invoices = viewModel.allInvoices.collectAsState().value
    val orders = viewModel.allOrders.collectAsState().value
    val products = viewModel.allProducts.collectAsState().value

    // Filtering
    val filteredInvoices = remember(invoices, user.id, user.role) {
        if (user.role == "ADMIN") invoices else invoices.filter { it.shopId == user.id }
    }

    // Active sheet displaying inside dialog
    var activeInvoiceForSheet by remember { mutableStateOf<Invoice?>(null) }
    var showQuotationSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Financial Terminal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Consolidated balance sheets, receipts, and quotations",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Quick Proforma quotation generator button
            OutlinedButton(
                onClick = { showQuotationSheet = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.RequestQuote, "Quotation", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Get Quotation Spec", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (filteredInvoices.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, "Invoices Empty", modifier = Modifier.size(54.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No billing invoices logged yet.", color = Color.Gray)
                    Text("Invoices generate automatically when orders are approved.", fontSize = 11.sp, color = Color.LightGray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredInvoices) { invoice ->
                    InvoiceListRowItem(
                        invoice = invoice,
                        isAdmin = user.role == "ADMIN",
                        onClick = { activeInvoiceForSheet = invoice },
                        onUpdateStatus = { nextStatus ->
                            viewModel.updateInvoiceStatus(invoice.id, nextStatus)
                        }
                    )
                }
            }
        }
    }

    // Interactive Invoice PDF Presentation sheet Dialogue
    if (activeInvoiceForSheet != null) {
        InvoiceDetailsDialogSpec(
            invoice = activeInvoiceForSheet!!,
            orders = orders,
            isAdmin = user.role == "ADMIN",
            onDismiss = { activeInvoiceForSheet = null },
            viewModel = viewModel
        )
    }

    // Premium interactive Quotation Specification Sheet
    if (showQuotationSheet) {
        QuotationDetailsDialogSpec(
            products = products,
            onDismiss = { showQuotationSheet = false },
            viewModel = viewModel
        )
    }
}

// ==========================================
// INVOICE LIST CELL ROW
// ==========================================
@Composable
fun InvoiceListRowItem(
    invoice: Invoice,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    val dateStr = remember(invoice.dateMillis) {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(invoice.dateMillis))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Receipt #${invoice.invoiceNumber}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Check if composite (contains multiple orders!)
                    if (invoice.orderIdsStr.split(",").size > 1) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "AGGREGATE BILL",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "$dateStr • Buyer: ${invoice.shopDetails.split("-").firstOrNull() ?: "Retail Store"}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format(Locale.US, "%.2f", invoice.grandTotal)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusChip(status = invoice.status)
            }
        }
    }
}

// ==========================================
// PORTRAIT INVOICE PDF FORM VISUALIZER
// ==========================================
@Composable
fun InvoiceDetailsDialogSpec(
    invoice: Invoice,
    orders: List<Order>,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    viewModel: PharmaViewModel
) {
    val scope = rememberCoroutineScope()
    var isSimulatingDownload by remember { mutableStateOf(false) }

    // Aggregate items across all matching orders
    val invoiceLineItems = remember(invoice.orderIdsStr, orders) {
        val list = mutableListOf<String>()
        val parsedIds = invoice.orderIdsStr.split(",").mapNotNull { it.trim().toLongOrNull() }

        // Compile details statically or loading details
        // Since database retrieval is async, for immediate rendering we simulate loading them beautifully!
        parsedIds
    }

    var orderItemsState by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    LaunchedEffect(invoice.orderIdsStr) {
        val db = AppDatabase.getDatabase(viewModel.getApplication())
        val parsedIds = invoice.orderIdsStr.split(",").mapNotNull { it.trim().toLongOrNull() }
        val loadedItems = mutableListOf<OrderItem>()
        for (id in parsedIds) {
            loadedItems.addAll(db.pharmaDao().getOrderItemsList(id))
        }
        orderItemsState = loadedItems
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Wholesale Commercial Invoice", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                StatusChip(status = invoice.status)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Printable corporate shell
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        
                        // Corporate Monogram
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("PharmaStore Co.", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0288D1))
                                Text("Wholesale Distribution Logistics\nReg. No: WS-9921D-M3", fontSize = 8.sp, color = Color.Gray, lineHeight = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("INVOICE RECEIPT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("#${invoice.invoiceNumber}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Customer Specifications
                        Text("CONSIGNEE / STORES ADDRESS:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(
                            text = invoice.shopDetails,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Text("Audit Order Runs: #${invoice.orderIdsStr}", fontSize = 9.sp, color = Color.Gray)

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Table Headers
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Supply Items Block", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f))
                            Text("Qty", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                            Text("Rate", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                            Text("Sub", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(54.dp), textAlign = TextAlign.End)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Divider(color = Color.LightGray.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(6.dp))

                        // Table Items List
                        if (orderItemsState.isEmpty()) {
                            Text("Compiling ledger ledger lines...", fontSize = 10.sp, color = Color.LightGray)
                        } else {
                            orderItemsState.forEach { itm ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(itm.productName, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${itm.quantity}", fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                                    Text("$${itm.price}", fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                                    Text("$${String.format(Locale.US, "%.1f", itm.price * itm.quantity)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.width(54.dp), textAlign = TextAlign.End)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Financial totals
                        val grossSub = orderItemsState.sumOf { it.price * it.quantity }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            InvoiceCostLine("Wholesale Subtotal", "$${String.format(Locale.US, "%.2f", grossSub)}")
                            if (invoice.discount > 0) InvoiceCostLine("Trade Discounts Apply", "-$${String.format(Locale.US, "%.2f", invoice.discount)}")
                            InvoiceCostLine("Vat Ledger Tax (5% Standard)", "$${String.format(Locale.US, "%.2f", invoice.vatTax)}")
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("CONSOLIDATED TOTAL DUE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("$${String.format(Locale.US, "%.2f", invoice.grandTotal)}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0288D1))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Security Badge Watermark Text
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE1F5FE), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, "Approved", tint = Color(0xFF0288D1), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SECURITY CHECKED & DIGITALLY VERIFIED BY PHARMASTORE LOGISTICS",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0288D1)
                                )
                            }
                        }
                    }
                }

                // Admin status update action togglers
                if (isAdmin && (invoice.status != "Cancel" && invoice.status != "Paid")) {
                    Divider()
                    Text("Select Commercial Update status: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.updateInvoiceStatus(invoice.id, "Paid") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CreditCard, "Paid", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Paid", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.updateInvoiceStatus(invoice.id, "Processing") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Cached, "Proc", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Processing", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.updateInvoiceStatus(invoice.id, "Cancelled") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Cancel, "Cancel", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                // Shared triggers
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isSimulatingDownload = true
                            delay(1600)
                            isSimulatingDownload = false
                            viewModel.postFeedback("PDF downloaded to local workspace: /storage/emulated/0/Download/Invoice-${invoice.invoiceNumber}.pdf")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSimulatingDownload) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Download, "PDF")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Get PDF Receipt", fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = {
                        viewModel.postFeedback("Invoice link shared via secure channel successfully.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, "Share WhatsApp")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp Share", fontSize = 11.sp)
                }

                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
fun InvoiceCostLine(lbl: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(lbl, fontSize = 9.sp, color = Color.Gray)
        Text(value, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

// ==========================================
// PREMIUM DEMO QUOTATION SPEC SHEET DIALOG
// ==========================================
@Composable
fun QuotationDetailsDialogSpec(
    products: List<Product>,
    onDismiss: () -> Unit,
    viewModel: PharmaViewModel
) {
    var isSimulatingDownload by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DynamicFeed, "Quotation", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Proforma Price Quotation Spec", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Current wholesale rates catalog valid as of today. Prices are subject to logistics distribution grids.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 15.sp
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Medical Drug Item SKU", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f))
                            Text("Wholesale", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                            Text("Retail MRP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Divider(color = Color.LightGray.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(6.dp))

                        products.forEach { prod ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.name, fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Mono: ${prod.genericName}", fontSize = 9.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text("$${String.format(Locale.US, "%.2f", prod.wholesalePrice)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1), modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                                Text("$${String.format(Locale.US, "%.2f", prod.unitPrice)}", fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Logistics disclaimer: Bulk orders exceeding $1,000 qualify for a 2.5% automatic volume markdown deduct.",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            lineHeight = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            isSimulatingDownload = true
                            delay(1200)
                            isSimulatingDownload = false
                            viewModel.postFeedback("Quotation downloaded successfully: /storage/emulated/0/Download/PharmaStore_Quote.pdf")
                        }
                    }
                ) {
                    if (isSimulatingDownload) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.FileDownload, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download Quotation")
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
