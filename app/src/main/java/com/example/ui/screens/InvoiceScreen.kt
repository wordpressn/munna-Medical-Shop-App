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
import android.content.Context
import android.content.ContentValues
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

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

    Column(modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 12.dp)) {
        
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
                    text = "৳${String.format(Locale.US, "%.2f", invoice.grandTotal)}",
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSimulatingDownload by remember { mutableStateOf(false) }
    var showDownloadSuccessDialog by remember { mutableStateOf(false) }
    var downloadedPath by remember { mutableStateOf("") }

    // Aggregate items across all matching orders
    val invoiceLineItems = remember(invoice.orderIdsStr, orders) {
        val list = mutableListOf<String>()
        val parsedIds = invoice.orderIdsStr.split(",").mapNotNull { it.trim().toLongOrNull() }

        // Compile details statically or loading details
        // Since database retrieval is async, for immediate rendering we simulate loading them beautifully!
        parsedIds
    }

    var orderItemsState by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var shopProfileState by remember { mutableStateOf<ShopProfile?>(null) }
    LaunchedEffect(invoice.orderIdsStr, invoice.shopId) {
        val db = AppDatabase.getDatabase(viewModel.getApplication())
        val parsedIds = invoice.orderIdsStr.split(",").mapNotNull { it.trim().toLongOrNull() }
        val loadedItems = mutableListOf<OrderItem>()
        for (id in parsedIds) {
            loadedItems.addAll(db.pharmaDao().getOrderItemsList(id))
        }
        orderItemsState = loadedItems
        shopProfileState = db.pharmaDao().getShopProfile(invoice.shopId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Text("Wholesale Commercial Invoice", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    StatusChip(status = invoice.status)
                }

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

                        // Consolidated split details row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left Section: Invoice Metadata Details
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text("INVOICE DETAILS:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(invoice.dateMillis))}",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "Status: ${invoice.status}",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Shop ID: CL-${invoice.shopId}",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "Audit Order Runs: #${invoice.orderIdsStr}",
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Right Section: Consignee / Shop Details
                            Column(
                                modifier = Modifier.weight(1.2f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("CONSIGNEE / STORES ADDRESS:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                if (shopProfileState != null) {
                                    val profile = shopProfileState!!
                                    Text(
                                        text = profile.shopName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "${profile.shopAddress}, ${profile.district}",
                                        fontSize = 10.sp,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "Proprietor: ${profile.ownerFullName}",
                                        fontSize = 10.sp,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "Contact: ${profile.phoneNumber}",
                                        fontSize = 10.sp,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "Email: ${profile.emailAddress}",
                                        fontSize = 10.sp,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.End
                                    )
                                } else {
                                    Text(
                                        text = invoice.shopDetails,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }

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
                                    Text("৳${itm.price}", fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                                    Text("৳${String.format(Locale.US, "%.1f", itm.price * itm.quantity)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.width(54.dp), textAlign = TextAlign.End)
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
                            InvoiceCostLine("Wholesale Subtotal", "৳${String.format(Locale.US, "%.2f", grossSub)}")
                            if (invoice.discount > 0) InvoiceCostLine("Trade Discounts Apply", "-৳${String.format(Locale.US, "%.2f", invoice.discount)}")
                            InvoiceCostLine("Vat Ledger Tax (5% Standard)", "৳${String.format(Locale.US, "%.2f", invoice.vatTax)}")
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("CONSOLIDATED TOTAL DUE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("৳${String.format(Locale.US, "%.2f", invoice.grandTotal)}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0288D1))
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
                            delay(1000)
                            
                            val pdfDocument = generateInvoicePdf(invoice, orderItemsState, shopProfileState)
                            val path = savePdf(context, pdfDocument, "Invoice-${invoice.invoiceNumber}.pdf")
                            pdfDocument.close()
                            
                            isSimulatingDownload = false
                            if (path.isNotEmpty()) {
                                viewModel.postFeedback("PDF downloaded successfully to: $path")
                                downloadedPath = path
                                showDownloadSuccessDialog = true
                            } else {
                                viewModel.postFeedback("Failed to save PDF receipt to Downloads.")
                            }
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
                        scope.launch {
                            val pdfDocument = generateInvoicePdf(invoice, orderItemsState, shopProfileState)
                            sharePdfViaWhatsApp(context, pdfDocument, "Invoice-${invoice.invoiceNumber}.pdf")
                            pdfDocument.close()
                        }
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

    if (showDownloadSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Completed", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "The commercial invoice PDF was successfully generated and saved to your device:\n\n$downloadedPath",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDownloadSuccessDialog = false }
                ) {
                    Text("OK")
                }
            }
        )
    }
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
    val context = LocalContext.current
    var isSimulatingDownload by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Icon(Icons.Default.DynamicFeed, "Quotation", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Proforma Price Quotation Spec", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }

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
                                Text("৳${String.format(Locale.US, "%.2f", prod.wholesalePrice)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1), modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                                Text("৳${String.format(Locale.US, "%.2f", prod.unitPrice)}", fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
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
                            text = "Logistics disclaimer: Bulk orders exceeding ৳1,000 qualify for a 2.5% automatic volume markdown deduct.",
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
                            delay(1000)
                            
                            val pdfDoc = PdfDocument()
                            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                            val page = pdfDoc.startPage(pageInfo)
                            val canvas = page.canvas
                            val paint = Paint().apply { isAntiAlias = true }
                            
                            var qY = 50f
                            paint.textSize = 20f
                            paint.isFakeBoldText = true
                            paint.color = android.graphics.Color.rgb(2, 136, 209)
                            canvas.drawText("PHARMASTORE LOGISTICS", 40f, qY, paint)
                            qY += 25f
                            
                            paint.textSize = 12f
                            paint.isFakeBoldText = false
                            paint.color = android.graphics.Color.DKGRAY
                            canvas.drawText("Proforma Price Quotation Spec Sheet", 40f, qY, paint)
                            qY += 40f
                            
                            paint.textSize = 10f
                            paint.color = android.graphics.Color.BLACK
                            canvas.drawText("Date Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}", 40f, qY, paint)
                            qY += 20f
                            
                            canvas.drawLine(40f, qY, 555f, qY, paint)
                            qY += 20f
                            
                            paint.isFakeBoldText = true
                            canvas.drawText("Medical Drug Item SKU", 40f, qY, paint)
                            canvas.drawText("Wholesale Rate", 320f, qY, paint)
                            canvas.drawText("Retail MRP", 450f, qY, paint)
                            qY += 15f
                            
                            paint.isFakeBoldText = false
                            canvas.drawLine(40f, qY, 555f, qY, paint)
                            qY += 20f
                            
                            products.forEach { prod ->
                                if (qY > 750f) return@forEach
                                canvas.drawText(prod.name, 40f, qY, paint)
                                canvas.drawText("Tk ${String.format(Locale.US, "%.2f", prod.wholesalePrice)}", 320f, qY, paint)
                                canvas.drawText("Tk ${String.format(Locale.US, "%.2f", prod.unitPrice)}", 450f, qY, paint)
                                qY += 18f
                            }
                            
                            qY += 20f
                            paint.textSize = 8f
                            paint.isFakeBoldText = true
                            paint.color = android.graphics.Color.GRAY
                            canvas.drawText("Prices subject to logistics grids. Auto 2.5% discount volume markdown exceeds Tk 1,000.", 40f, qY, paint)
                            
                            pdfDoc.finishPage(page)
                            
                            val nameSuffix = System.currentTimeMillis() % 10000
                            val path = savePdf(context, pdfDoc, "PharmaStore_Quote_$nameSuffix.pdf")
                            pdfDoc.close()
                            
                            isSimulatingDownload = false
                            if (path.isNotEmpty()) {
                                viewModel.postFeedback("Quotation PDF saved successfully: $path")
                            } else {
                                viewModel.postFeedback("Failed to save Quotation PDF to Downloads.")
                            }
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

private fun savePdf(context: Context, pdfDocument: PdfDocument, filename: String): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    pdfDocument.writeTo(out)
                }
                return "Downloads/$filename"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    try {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val file = File(downloadsDir, filename)
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        return "Downloads/$filename"
    } catch (e: Exception) {
        e.printStackTrace()
    }

    try {
        val appDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(appDownloadsDir, filename)
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        return file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return ""
}

private fun generateInvoicePdf(
    invoice: Invoice,
    orderItems: List<OrderItem>,
    shopProfile: ShopProfile?
): PdfDocument {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    
    val paint = Paint().apply {
        isAntiAlias = true
    }
    
    var y = 50f
    
    // Draw Header
    paint.textSize = 20f
    paint.isFakeBoldText = true
    paint.color = android.graphics.Color.rgb(2, 136, 209)
    canvas.drawText("PHARMASTORE LOGISTICS", 40f, y, paint)
    y += 25f
    
    paint.textSize = 12f
    paint.isFakeBoldText = false
    paint.color = android.graphics.Color.DKGRAY
    canvas.drawText("Official Commercial Invoice Ledger", 40f, y, paint)
    y += 40f
    
    // Draw Metadata
    paint.textSize = 10f
    paint.color = android.graphics.Color.BLACK
    canvas.drawText("Invoice Number: ${invoice.invoiceNumber}", 40f, y, paint)
    y += 15f
    canvas.drawText("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(invoice.dateMillis))}", 40f, y, paint)
    y += 15f
    canvas.drawText("Status: ${invoice.status}", 40f, y, paint)
    y += 15f
    canvas.drawText("Shop Identifier ID: CL-${invoice.shopId}", 40f, y, paint)
    y += 25f
    
    // Consignee Block
    paint.isFakeBoldText = true
    canvas.drawText("CONSIGNEE / STORES DETAILS:", 40f, y, paint)
    y += 15f
    paint.isFakeBoldText = false
    
    if (shopProfile != null) {
        canvas.drawText("Shop Name: ${shopProfile.shopName}", 40f, y, paint)
        y += 14f
        canvas.drawText("Location: ${shopProfile.shopAddress}, ${shopProfile.district}", 40f, y, paint)
        y += 14f
        canvas.drawText("Proprietor: ${shopProfile.ownerFullName}", 40f, y, paint)
        y += 14f
        canvas.drawText("Contact: ${shopProfile.phoneNumber} | Email: ${shopProfile.emailAddress}", 40f, y, paint)
        y += 20f
    } else {
        canvas.drawText(invoice.shopDetails, 40f, y, paint)
        y += 20f
    }
    
    // Line
    paint.strokeWidth = 1f
    paint.color = android.graphics.Color.LTGRAY
    canvas.drawLine(40f, y, 555f, y, paint)
    y += 20f
    
    // Headers
    paint.isFakeBoldText = true
    paint.color = android.graphics.Color.BLACK
    canvas.drawText("Supply Items Block", 40f, y, paint)
    canvas.drawText("Qty", 320f, y, paint)
    canvas.drawText("Rate", 400f, y, paint)
    canvas.drawText("Subtotal", 480f, y, paint)
    y += 15f
    
    paint.isFakeBoldText = false
    canvas.drawLine(40f, y, 555f, y, paint)
    y += 20f
    
    // Table Items
    orderItems.forEach { itm ->
        if (y > 750f) return@forEach
        canvas.drawText(itm.productName, 40f, y, paint)
        canvas.drawText("${itm.quantity}", 320f, y, paint)
        canvas.drawText("Tk ${String.format(Locale.US, "%.2f", itm.price)}", 400f, y, paint)
        canvas.drawText("Tk ${String.format(Locale.US, "%.2f", itm.price * itm.quantity)}", 480f, y, paint)
        y += 18f
    }
    
    y += 10f
    canvas.drawLine(40f, y, 555f, y, paint)
    y += 20f
    
    // Financial Totals
    val grossSub = orderItems.sumOf { it.price * it.quantity }
    paint.textSize = 10f
    canvas.drawText("Wholesale Subtotal:", 320f, y, paint)
    canvas.drawText("Tk ${String.format(Locale.US, "%.2f", grossSub)}", 480f, y, paint)
    y += 15f
    
    if (invoice.discount > 0) {
        canvas.drawText("Trade Discounts Apply:", 320f, y, paint)
        canvas.drawText("-Tk ${String.format(Locale.US, "%.2f", invoice.discount)}", 480f, y, paint)
        y += 15f
    }
    
    canvas.drawText("VAT Ledger Tax (5%):", 320f, y, paint)
    canvas.drawText("Tk ${String.format(Locale.US, "%.2f", invoice.vatTax)}", 480f, y, paint)
    y += 20f
    
    paint.textSize = 12f
    paint.isFakeBoldText = true
    paint.color = android.graphics.Color.rgb(2, 136, 209)
    canvas.drawText("TOTAL DUE:", 320f, y, paint)
    canvas.drawText("Tk ${String.format(Locale.US, "%.2f", invoice.grandTotal)}", 480f, y, paint)
    y += 35f
    
    // Watermark
    paint.textSize = 8f
    paint.isFakeBoldText = true
    paint.color = android.graphics.Color.GRAY
    canvas.drawText("SECURITY CHECKED & DIGITALLY VERIFIED BY PHARMASTORE LOGISTICS", 40f, y, paint)
    
    pdfDocument.finishPage(page)
    return pdfDocument
}

private fun sharePdfViaWhatsApp(context: Context, pdfDocument: PdfDocument, filename: String) {
    try {
        val cachePath = File(context.cacheDir, "shared_invoices")
        if (!cachePath.exists()) {
            cachePath.mkdirs()
        }
        val file = File(cachePath, filename)
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        
        val authority = "${context.packageName}.fileprovider"
        val contentUri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        
        val whatsappIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp")
        }
        
        try {
            context.startActivity(whatsappIntent)
        } catch (e: Exception) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Invoice PDF via:"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
