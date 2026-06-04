package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppHost()
            }
        }
    }
}

@Composable
fun MainAppHost() {
    val viewModel: PharmaViewModel = viewModel()
    val screen by viewModel.currentScreen.collectAsState()
    val feedbackMessage by viewModel.uiFeedbackMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Display inline alerts / toasts when messages trigger
    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            snackbarHostState.showSnackbar(
                message = feedbackMessage!!,
                duration = SnackbarDuration.Short
            )
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    fadeIn(tween(350)) togetherWith fadeOut(tween(300))
                },
                label = "screen_navigation_host"
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.SPLASH -> {
                        SplashScreen(
                            onTimeout = {
                                viewModel.setScreen(Screen.LOGIN)
                            }
                        )
                    }
                    Screen.LOGIN -> {
                        AuthScreen(viewModel = viewModel)
                    }
                    Screen.SHOP_SETUP -> {
                        ShopSetupScreen(viewModel = viewModel)
                    }
                    Screen.DASHBOARD -> {
                        DashboardMainShell(viewModel = viewModel)
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Navigation Endpoint Not Found")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardMainShell(viewModel: PharmaViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val user by viewModel.currentUser.collectAsState()

    // Count unread notifications to draw dynamic badge on bottom tab!
    val notifications by viewModel.userNotifications.collectAsState()
    val unreadNotifsCount = remember(notifications) {
        notifications.count { !it.isRead }
    }

    val cart by viewModel.activeCart.collectAsState()
    val cartCount = remember(cart) {
        cart.sumOf { it.quantity }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                // HOME
                NavigationBarItem(
                    selected = currentTab == DashboardTab.HOME,
                    onClick = { viewModel.setTab(DashboardTab.HOME) },
                    icon = { Icon(Icons.Default.Dashboard, "Home Details") },
                    label = { Text("Overview", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )

                // CATALOG (FOR SELLING) OR INVENTORY (FOR ADMIN)
                NavigationBarItem(
                    selected = currentTab == DashboardTab.CATALOG,
                    onClick = { viewModel.setTab(DashboardTab.CATALOG) },
                    icon = { Icon(Icons.Default.MedicalServices, "Products Specs") },
                    label = { Text(if (user?.role == "ADMIN") "Inventory" else "Catalog", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )

                // ORDERS
                NavigationBarItem(
                    selected = currentTab == DashboardTab.ORDERS,
                    onClick = { viewModel.setTab(DashboardTab.ORDERS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (user?.role == "SHOP_OWNER" && cartCount > 0) {
                                    Badge { Text("$cartCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Dns, "Orders Requisitions")
                        }
                    },
                    label = { Text(if (user?.role == "ADMIN") "Logistics" else "Orders", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )

                // INVOICES
                NavigationBarItem(
                    selected = currentTab == DashboardTab.INVOICES,
                    onClick = { viewModel.setTab(DashboardTab.INVOICES) },
                    icon = { Icon(Icons.Default.ReceiptLong, "Invoicing terminal") },
                    label = { Text("Receipts", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )

                // SETTINGS / SWAP USER
                NavigationBarItem(
                    selected = currentTab == DashboardTab.SETTINGS,
                    onClick = { viewModel.setTab(DashboardTab.SETTINGS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unreadNotifsCount > 0) {
                                    Badge { Text("$unreadNotifsCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.AccountCircle, "Config Profile")
                        }
                    },
                    label = { Text("Account", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },
                label = "dashboard_tab_transitions"
            ) { targetTab ->
                when (targetTab) {
                    DashboardTab.HOME -> {
                        DashboardScreen(viewModel = viewModel)
                    }
                    DashboardTab.CATALOG -> {
                        ProductScreens(viewModel = viewModel)
                    }
                    DashboardTab.ORDERS -> {
                        OrderScreens(viewModel = viewModel)
                    }
                    DashboardTab.INVOICES -> {
                        InvoiceScreen(viewModel = viewModel)
                    }
                    DashboardTab.SETTINGS -> {
                        SettingsScreen(viewModel = viewModel)
                    }
                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Page tab under construction")
                        }
                    }
                }
            }
        }
    }
}
