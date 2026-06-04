package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Navigation Screen enum
enum class Screen {
    SPLASH,
    LOGIN,
    FORGOT_PASSWORD,
    PHONE_OTP,
    SHOP_SETUP,
    DASHBOARD
}

// Navigation Tab inside DASHBOARD Screen
enum class DashboardTab {
    HOME,
    CATALOG,  // Catalog for Retailers, Inventory for Admin
    ORDERS,
    INVOICES,
    NOTIFICATIONS,
    SETTINGS
}

class PharmaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = PharmaRepository(database.pharmaDao())

    // UI state
    private val _currentScreen = MutableStateFlow(Screen.SPLASH)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _currentTab = MutableStateFlow(DashboardTab.HOME)
    val currentTab: StateFlow<DashboardTab> = _currentTab.asStateFlow()

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _currentShopProfile = MutableStateFlow<ShopProfile?>(null)
    val currentShopProfile: StateFlow<ShopProfile?> = _currentShopProfile.asStateFlow()

    // Loaded flows from room
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrders: StateFlow<List<Order>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvoices: StateFlow<List<Invoice>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allShopProfiles: StateFlow<List<ShopProfile>> = repository.allShopProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserAccount>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart items for active Shop Owner
    private val _activeCart = MutableStateFlow<List<CartItem>>(emptyList())
    val activeCart: StateFlow<List<CartItem>> = _activeCart.asStateFlow()

    // Notifications
    private val _userNotifications = MutableStateFlow<List<Notification>>(emptyList())
    val userNotifications: StateFlow<List<Notification>> = _userNotifications.asStateFlow()

    // Temporary storage for invoice generation / quotation details
    private val _activeInvoicePreview = MutableStateFlow<Invoice?>(null)
    val activeInvoicePreview: StateFlow<Invoice?> = _activeInvoicePreview.asStateFlow()

    // Active order selected for invoice generation or viewing
    private val _selectedOrderIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedOrderIds: StateFlow<Set<Long>> = _selectedOrderIds.asStateFlow()

    // Search and filters
    val searchQuery = MutableStateFlow("")
    val filterCategory = MutableStateFlow("All")
    val filterAvailability = MutableStateFlow("All") // "All", "In Stock", "Low Stock", "Out of Stock"
    val filterPriceRange = MutableStateFlow("All") // "All", "Under $5", "$5 to $15", "Over $15"

    // UI Feedback
    private val _uiFeedbackMessage = MutableStateFlow<String?>(null)
    val uiFeedbackMessage: StateFlow<String?> = _uiFeedbackMessage.asStateFlow()

    init {
        // Pre-populate Database and prepare app state
        viewModelScope.launch {
            try {
                prepopulateDatabase()
                observeCartAndNotifications()
            } catch (e: Exception) {
                postFeedback("Initialization error: ${e.message}")
            }
        }
    }

    fun postFeedback(msg: String) {
        _uiFeedbackMessage.value = msg
    }

    fun clearFeedback() {
        _uiFeedbackMessage.value = null
    }

    fun setScreen(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setTab(tab: DashboardTab) {
        _currentTab.value = tab
    }

    fun getOrderItems(orderId: Long): Flow<List<OrderItem>> {
        return repository.getOrderItems(orderId)
    }

    private fun observeCartAndNotifications() {
        viewModelScope.launch {
            _currentUser.collectLatest { user ->
                if (user != null) {
                    // Collect cart
                    repository.getCartForUser(user.id).collectLatest { cartList ->
                        _activeCart.value = cartList
                    }
                } else {
                    _activeCart.value = emptyList()
                }
            }
        }

        viewModelScope.launch {
            _currentUser.collectLatest { user ->
                if (user != null) {
                    if (user.role == "ADMIN") {
                        repository.adminNotificationsFlow.collectLatest { list ->
                            _userNotifications.value = list
                        }
                    } else {
                        repository.getNotificationsForUser(user.id).collectLatest { list ->
                            _userNotifications.value = list
                        }
                    }
                } else {
                    _userNotifications.value = emptyList()
                }
            }
        }
    }

    // AUTH ACTIONS
    fun login(email: String, pass: String): Boolean {
        var success = false
        viewModelScope.launch {
            val user = repository.getUserByEmail(email)
            if (user != null && user.passwordHash == pass) {
                _currentUser.value = user
                val profile = repository.getShopProfile(user.id)
                _currentShopProfile.value = profile

                // Sync status
                val updatedStatus = user.status
                if (updatedStatus == "PENDING_SETUP") {
                    _currentScreen.value = Screen.SHOP_SETUP
                } else {
                    _currentScreen.value = Screen.DASHBOARD
                }
                success = true
                postFeedback("Logged in successfully as ${user.fullName}")
            } else {
                postFeedback("Invalid credentials. Try admin@pharma.com or shop@pharma.com")
            }
        }
        return success
    }

    fun loginWithPhone(phoneNumber: String, code: String) {
        viewModelScope.launch {
            // Find user matches phone or login with test user
            val allUsersList = repository.getAllUsersList()
            val matched = allUsersList.find { it.phoneNumber == phoneNumber }
            if (matched != null) {
                _currentUser.value = matched
                val profile = repository.getShopProfile(matched.id)
                _currentShopProfile.value = profile
                if (matched.status == "PENDING_SETUP") {
                    setScreen(Screen.SHOP_SETUP)
                } else {
                    setScreen(Screen.DASHBOARD)
                }
                postFeedback("OTP Verified! Logged in as ${matched.fullName}")
            } else {
                // Pre-register dynamic dummy user dynamically for demo flow to keep user flow happy
                val newId = repository.insertUser(
                    UserAccount(
                        email = "otp-${code}@pharma.com",
                        passwordHash = "123",
                        role = "SHOP_OWNER",
                        fullName = "Dynamic OTC Shop Owner",
                        phoneNumber = phoneNumber,
                        status = "PENDING_SETUP"
                    )
                )
                val newUser = repository.getUserById(newId)
                _currentUser.value = newUser
                _currentShopProfile.value = null
                setScreen(Screen.SHOP_SETUP)
                postFeedback("OTP Matches! Build your Pharmacy profile to continue.")
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            val user = repository.getUserByEmail(email)
            if (user != null) {
                postFeedback("Reset instructions dispatched to $email! Password hint: ${user.passwordHash}")
            } else {
                postFeedback("No account matching $email. Try registering first!")
            }
        }
    }

    fun register(email: String, pass: String, name: String, phone: String, role: String) {
        viewModelScope.launch {
            val existing = repository.getUserByEmail(email)
            if (existing != null) {
                postFeedback("Account already registered for this email!")
                return@launch
            }

            val defaultStatus = if (role == "ADMIN") "VERIFIED" else "PENDING_SETUP"
            val newUserId = repository.insertUser(
                UserAccount(
                    email = email,
                    passwordHash = pass,
                    fullName = name,
                    phoneNumber = phone,
                    role = role,
                    status = defaultStatus
                )
            )

            val createdUser = repository.getUserById(newUserId)
            _currentUser.value = createdUser
            _currentShopProfile.value = null

            if (role == "ADMIN") {
                setScreen(Screen.DASHBOARD)
                postFeedback("Admin Registered successfully!")
            } else {
                setScreen(Screen.SHOP_SETUP)
                postFeedback("Account pending! Please configure Shop Profile.")
            }
        }
    }

    fun setupShopProfile(
        shopName: String,
        ownerName: String,
        phone: String,
        email: String,
        address: String,
        district: String,
        license: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val profile = ShopProfile(
                userId = user.id,
                shopName = shopName,
                ownerFullName = ownerName,
                phoneNumber = phone,
                emailAddress = email,
                shopAddress = address,
                district = district,
                tradeLicenseNumber = license,
                status = "Pending"
            )
            repository.insertShopProfile(profile)
            _currentShopProfile.value = profile

            // Update user status
            val updatedUser = user.copy(status = "PENDING_VERIFICATION")
            repository.updateUser(updatedUser)
            _currentUser.value = updatedUser

            // Send notification to Admin
            repository.insertNotification(
                Notification(
                    title = "New Verification Request",
                    message = "Shop '$shopName' by $ownerName has applied for verification. License #: $license",
                    isForAdmin = true
                )
            )

            setScreen(Screen.DASHBOARD)
            postFeedback("Shop profile details submitted! Admin review is in progress.")
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentShopProfile.value = null
        _currentTab.value = DashboardTab.HOME
        _selectedOrderIds.value = emptySet()
        setScreen(Screen.LOGIN)
        postFeedback("Logged out successfully.")
    }

    // ADMIN PROFILE APPROVALS
    fun approveShop(userId: Long) {
        viewModelScope.launch {
            val targetUser = repository.getUserById(userId)
            val profile = repository.getShopProfile(userId)
            if (targetUser != null && profile != null) {
                val updatedUser = targetUser.copy(status = "VERIFIED")
                repository.updateUser(updatedUser)

                val updatedProfile = profile.copy(status = "Verified")
                repository.updateShopProfile(updatedProfile)

                // Dispatch Notification to owner
                repository.insertNotification(
                    Notification(
                        title = "Shop Profile Verified",
                        message = "Congratulations! Your shop profile '$shopNameValue' is approved. You now hold a Verified Badge.",
                        targetUserId = userId
                    )
                )

                postFeedback("Successfully approved pharmacy shop: ${profile.shopName}")
            }
        }
    }

    private val shopNameValue: String
        get() = _currentShopProfile.value?.shopName ?: "Pharmacy"

    fun rejectShop(userId: Long) {
        viewModelScope.launch {
            val targetUser = repository.getUserById(userId)
            val profile = repository.getShopProfile(userId)
            if (targetUser != null && profile != null) {
                val updatedUser = targetUser.copy(status = "REJECTED")
                repository.updateUser(updatedUser)

                val updatedProfile = profile.copy(status = "Rejected")
                repository.updateShopProfile(updatedProfile)

                repository.insertNotification(
                    Notification(
                        title = "Shop Profile Rejected",
                        message = "Your shop profile '$shopNameValue' request was rejected. Please update details with valid trade assets.",
                        targetUserId = userId
                    )
                )

                postFeedback("Rejected shop verification for ${profile.shopName}")
            }
        }
    }


    // PRODUCT ACTIONS (ADMIN)
    fun addProduct(
        name: String,
        generic: String,
        brand: String,
        category: String,
        description: String,
        unitPrice: Double,
        wholesalePrice: Double,
        stock: Int,
        batch: String,
        expiry: String,
        sku: String
    ) {
        viewModelScope.launch {
            val prod = Product(
                name = name,
                genericName = generic,
                brandName = brand,
                category = category,
                description = description,
                unitPrice = unitPrice,
                wholesalePrice = wholesalePrice,
                stockQuantity = stock,
                batchNumber = batch,
                expiryDate = expiry,
                skuCode = sku
            )
            repository.insertProduct(prod)

            // Trigger alarms if quantity is low
            if (stock < 100) {
                repository.insertNotification(
                    Notification(
                        title = "Low Stock Added",
                        message = "Product '$name' added with low inventory ($stock units left).",
                        isForAdmin = true
                    )
                )
            }
            postFeedback("Medicine '$name' added successfully!")
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
            postFeedback("Medicine '${product.name}' details saved.")
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            postFeedback("Removed medicine '${product.name}' from pharmacy master list.")
        }
    }


    // CART ACTIONS (SHOP OWNER)
    fun addToCart(product: Product, quantity: Int) {
        val user = _currentUser.value ?: return
        if (product.stockQuantity <= 0) {
            postFeedback("Out of stock! Cannot supply ${product.name}")
            return
        }

        viewModelScope.launch {
            val currentCartList = _activeCart.value
            val existing = currentCartList.find { it.productId == product.id }
            if (existing != null) {
                val newQty = existing.quantity + quantity
                if (newQty > product.stockQuantity) {
                    postFeedback("Cannot add more than active stock constraints (${product.stockQuantity})")
                    return@launch
                }
                repository.insertCartItem(existing.copy(quantity = newQty))
            } else {
                if (quantity > product.stockQuantity) {
                    postFeedback("Requested quantity exceeds active stock capacity.")
                    return@launch
                }
                repository.insertCartItem(
                    CartItem(
                        userId = user.id,
                        productId = product.id,
                        productName = product.name,
                        price = product.wholesalePrice, // Shop Owner gets wholesale rates!
                        quantity = quantity
                    )
                )
            }
            postFeedback("Added $quantity x ${product.name} to wholesale check.")
        }
    }

    fun updateCartQuantity(item: CartItem, change: Int) {
        viewModelScope.launch {
            val prod = repository.getProductById(item.productId)
            val targetQty = item.quantity + change
            if (targetQty <= 0) {
                repository.deleteCartItem(item)
                postFeedback("Removed ${item.productName} from cart.")
            } else {
                if (prod != null && targetQty > prod.stockQuantity) {
                    postFeedback("Cannot exceed available quantities (${prod.stockQuantity})")
                    return@launch
                }
                repository.insertCartItem(item.copy(quantity = targetQty))
            }
        }
    }

    fun removeFromCart(item: CartItem) {
        viewModelScope.launch {
            repository.deleteCartItem(item)
            postFeedback("Deleted ${item.productName}")
        }
    }


    // ORDER ACTIONS
    fun placeOrder(notes: String) {
        val user = _currentUser.value ?: return
        val shop = _currentShopProfile.value ?: return
        val cartList = _activeCart.value
        if (cartList.isEmpty()) {
            postFeedback("Wholesale order must contain items!")
            return
        }

        viewModelScope.launch {
            var grandTotal = 0.0
            for (item in cartList) {
                grandTotal += item.price * item.quantity
            }

            // Create Order
            val orderId = repository.insertOrder(
                Order(
                    shopId = user.id,
                    shopName = shop.shopName,
                    totalAmount = grandTotal,
                    notes = notes,
                    status = "Pending"
                )
            )

            // Insert Items & adjust product stock quantities
            for (item in cartList) {
                repository.insertOrderItem(
                    OrderItem(
                        orderId = orderId,
                        productId = item.productId,
                        productName = item.productName,
                        quantity = item.quantity,
                        price = item.price
                    )
                )

                // Adjust product inventory
                val prod = repository.getProductById(item.productId)
                if (prod != null) {
                    val finalQty = (prod.stockQuantity - item.quantity).coerceAtLeast(0)
                    repository.updateProduct(prod.copy(stockQuantity = finalQty))

                    if (finalQty == 0) {
                        repository.insertNotification(
                            Notification(
                                title = "OUT OF STOCK ALERT",
                                message = "Medicine '${prod.name}' (Batch: ${prod.batchNumber}) is fully depleted!",
                                isForAdmin = true
                            )
                        )
                    } else if (finalQty < 100) {
                        repository.insertNotification(
                            Notification(
                                title = "Low Inventory Warning",
                                message = "Medicine '${prod.name}' is running low ($finalQty units remaining).",
                                isForAdmin = true
                            )
                        )
                    }
                }
            }

            // Clear Cart
            repository.clearCartForUser(user.id)

            // Admin alert for new order
            repository.insertNotification(
                Notification(
                    title = "New Distributed Order",
                    message = "Received an order of ৳${String.format(Locale.US, "%.2f", grandTotal)} from '${shop.shopName}'.",
                    isForAdmin = true
                )
            )

            // Self notify
            repository.insertNotification(
                Notification(
                    title = "Wholesale Order Confirmed",
                    message = "Your wholesale bundle request was submitted (Order ID: #$orderId). Totaling ৳${String.format(Locale.US, "%.2f", grandTotal)}.",
                    targetUserId = user.id
                )
            )

            postFeedback("Distributed Order #$orderId placed successfully!")
            _currentTab.value = DashboardTab.ORDERS
        }
    }

    // MANUAL ADMIN ORDER (Admin creates order for any shop!)
    fun adminCreateOrder(
        targetShopUserId: Long,
        selectedProducts: List<Pair<Product, Int>>,
        discount: Double,
        deliveryFee: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val shopUser = repository.getUserById(targetShopUserId)
            val profile = repository.getShopProfile(targetShopUserId)
            if (shopUser == null || profile == null) {
                postFeedback("Selected Shop Profile invalid.")
                return@launch
            }

            var subtotal = 0.0
            for ((prod, qty) in selectedProducts) {
                subtotal += prod.wholesalePrice * qty
            }
            val total = subtotal - discount + deliveryFee

            val orderId = repository.insertOrder(
                Order(
                    shopId = targetShopUserId,
                    shopName = profile.shopName,
                    totalAmount = total,
                    discount = discount,
                    deliveryCharge = deliveryFee,
                    notes = notes,
                    status = "Pending"
                )
            )

            for ((prod, qty) in selectedProducts) {
                repository.insertOrderItem(
                    OrderItem(
                        orderId = orderId,
                        productId = prod.id,
                        productName = prod.name,
                        quantity = qty,
                        price = prod.wholesalePrice
                    )
                )

                // Adjust stock
                val realP = repository.getProductById(prod.id)
                if (realP != null) {
                    val finalQty = (realP.stockQuantity - qty).coerceAtLeast(0)
                    repository.updateProduct(realP.copy(stockQuantity = finalQty))
                }
            }

            // Notifications
            repository.insertNotification(
                Notification(
                    title = "Manual Order Drafted",
                    message = "Admin logged an order of ৳${String.format(Locale.US, "%.2f", total)} on behalf of your franchise.",
                    targetUserId = targetShopUserId
                )
            )

            postFeedback("Manual distributed order recorded (#$orderId).")
        }
    }

    fun updateOrderStatus(orderId: Long, nextStatus: String) {
        viewModelScope.launch {
            val order = repository.getOrderById(orderId)
            if (order != null) {
                val updatedOrder = order.copy(status = nextStatus)
                repository.updateOrder(updatedOrder)

                // Dispatch Notification
                repository.insertNotification(
                    Notification(
                        title = "Order Status Updated",
                        message = "Your order #$orderId has been scheduled and updated to: $nextStatus.",
                        targetUserId = order.shopId
                    )
                )

                // Auto generate invoice if status goes to Confirmed or Delivered automatically!
                if ((nextStatus == "Confirmed" || nextStatus == "Delivered") &&
                    !allInvoices.value.any { it.orderIdsStr.split(",").contains(orderId.toString()) }
                ) {
                    generateInvoiceForSingleOrder(orderId)
                }

                postFeedback("Order #$orderId status shifted to: $nextStatus")
            }
        }
    }

    // INVOICE & QUOTATION SYSTEM
    fun toggleOrderSelection(orderId: Long) {
        val current = _selectedOrderIds.value.toMutableSet()
        if (current.contains(orderId)) {
            current.remove(orderId)
        } else {
            current.add(orderId)
        }
        _selectedOrderIds.value = current
    }

    fun clearOrderSelection() {
        _selectedOrderIds.value = emptySet()
    }

    fun generateInvoiceForSelectedOrders() {
        val selected = _selectedOrderIds.value
        if (selected.isEmpty()) {
            postFeedback("Please pick at least one order to build an invoice aggregate!")
            return
        }

        viewModelScope.launch {
            val ordersToMerge = mutableListOf<Order>()
            for (id in selected) {
                val ord = repository.getOrderById(id)
                if (ord != null) {
                    ordersToMerge.add(ord)
                }
            }

            if (ordersToMerge.isEmpty()) return@launch

            // Confirm all orders belong to the same Shop so invoice is structured cleanly!
            val mainShopId = ordersToMerge.first().shopId
            val mismatch = ordersToMerge.any { it.shopId != mainShopId }
            if (mismatch) {
                postFeedback("Merged invoices can only aggregate orders belonging to the SAME shop!")
                return@launch
            }

            val profile = repository.getShopProfile(mainShopId)
            val shopName = profile?.shopName ?: "Partner Store"
            val district = profile?.district ?: "Local Base"
            val detailsSummary = "$shopName - Location: $district (Owner: ${profile?.ownerFullName})"

            var subtotal = 0.0
            var discounts = 0.0
            var deliveries = 0.0
            for (ord in ordersToMerge) {
                subtotal += ord.totalAmount
                discounts += ord.discount
                deliveries += ord.deliveryCharge
            }

            val invoiceNo = "INV-${System.currentTimeMillis() % 1000000}"
            val totalDue = subtotal - discounts + deliveries
            val taxValue = totalDue * 0.05 // 5% VAT simulated

            val inv = Invoice(
                invoiceNumber = invoiceNo,
                orderIdsStr = selected.joinToString(","),
                shopId = mainShopId,
                shopDetails = detailsSummary,
                discount = discounts,
                vatTax = taxValue,
                dueAmount = totalDue + taxValue,
                grandTotal = totalDue + taxValue,
                status = "Pending"
            )

            val newInvId = repository.insertInvoice(inv)
            postFeedback("Created Multi-Order Invoice #$invoiceNo containing ${selected.size} aggregates.")

            // Clear Selection and open tab
            _selectedOrderIds.value = emptySet()
            _activeInvoicePreview.value = inv.copy(id = newInvId)
            _currentTab.value = DashboardTab.INVOICES
        }
    }

    private suspend fun generateInvoiceForSingleOrder(orderId: Long) {
        val ord = repository.getOrderById(orderId) ?: return
        val profile = repository.getShopProfile(ord.shopId)
        val details = "${ord.shopName} - (${profile?.district ?: "Local Zone"})"

        val invoiceNo = "INV-${100000 + (orderId * 133) % 900000}"
        val dueAmount = ord.totalAmount
        val vatSimulated = dueAmount * 0.05 // 5% tax

        val inv = Invoice(
            invoiceNumber = invoiceNo,
            orderIdsStr = orderId.toString(),
            shopId = ord.shopId,
            shopDetails = details,
            discount = ord.discount,
            vatTax = vatSimulated,
            dueAmount = dueAmount + vatSimulated,
            grandTotal = dueAmount + vatSimulated,
            status = "Pending"
        )

        repository.insertInvoice(inv)

        repository.insertNotification(
            Notification(
                title = "New Invoice Released",
                message = "Your consolidated Invoice #$invoiceNo has been calculated. Amount: ৳${String.format(Locale.US, "%.2f", invoiceTotal(dueAmount, vatSimulated))}",
                targetUserId = ord.shopId
            )
        )
    }

    private fun invoiceTotal(dueAmount: Double, vatSimulated: Double): Double {
        return dueAmount + vatSimulated
    }

    fun updateInvoiceStatus(invoiceId: Long, nextStatus: String) {
        viewModelScope.launch {
            val list = allInvoices.value
            val match = list.find { it.id == invoiceId }
            if (match != null) {
                val updated = match.copy(status = nextStatus)
                repository.updateInvoice(updated)

                repository.insertNotification(
                    Notification(
                        title = "Invoice Update Checked",
                        message = "Invoice #${match.invoiceNumber} status was modified to: $nextStatus",
                        targetUserId = match.shopId
                    )
                )

                postFeedback("Invoice status saved as: $nextStatus")
            }
        }
    }

    // NOTIFICATION CLEARING
    fun clearNotifications() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            if (user.role == "ADMIN") {
                repository.markAdminNotificationsAsRead()
            } else {
                repository.markNotificationsAsRead(user.id)
            }
            postFeedback("Clasped reading alerts.")
        }
    }


    // DEV HELPERS / PREPOPULATION OF DATA
    private suspend fun prepopulateDatabase() {
        // Check if database contains products or users, if empty populate!
        val usersList = repository.getAllUsersList()
        if (usersList.isNotEmpty()) return

        // Populate Admin User
        repository.insertUser(
            UserAccount(
                email = "admin@pharma.com",
                passwordHash = "admin123",
                role = "ADMIN",
                fullName = "Dr. Emily Vance (Admin)",
                phoneNumber = "999-000-1111",
                status = "VERIFIED"
            )
        )

        // Populate Shop Owner User (Pre-verified)
        val ownerId = repository.insertUser(
            UserAccount(
                email = "shop@pharma.com",
                passwordHash = "owner123",
                role = "SHOP_OWNER",
                fullName = "John Carter",
                phoneNumber = "123-456-7890",
                status = "VERIFIED"
            )
        )

        // Populate Shop Profile for John
        repository.insertShopProfile(
            ShopProfile(
                userId = ownerId,
                shopName = "City Central Pharmacy",
                ownerFullName = "John Carter",
                phoneNumber = "123-456-7890",
                emailAddress = "shop@pharma.com",
                shopAddress = "402 Medical Plaza, Sector 4, Metropolis",
                district = "Metropolis Central",
                tradeLicenseNumber = "TL-88921-2026",
                status = "Verified"
            )
        )

        // Populate shop owner registration (Pending Setup) to demonstrate approval pipeline!
        val pendingId = repository.insertUser(
            UserAccount(
                email = "pending@pharma.com",
                passwordHash = "pending123",
                role = "SHOP_OWNER",
                fullName = "Sarah Jenkins",
                phoneNumber = "654-321-0987",
                status = "PENDING_VERIFICATION"
            )
        )

        repository.insertShopProfile(
            ShopProfile(
                userId = pendingId,
                shopName = "Suburban Corner Chemists",
                ownerFullName = "Sarah Jenkins",
                phoneNumber = "654-321-0987",
                emailAddress = "pending@pharma.com",
                shopAddress = "Avenue 7, Block B, Greenfield",
                district = "Greenfield Suburb",
                tradeLicenseNumber = "TL-77221-2026",
                status = "Pending"
            )
        )

        // Preload rich pharmaceutical products (WHOLESALE / RETAIL PRESETS!)
        val p1 = Product(
            name = "Amoxicillin 500mg Capsule",
            genericName = "Amoxicillin Trihydrate",
            brandName = "Amoxil",
            category = "Antibiotic",
            description = "Broad-spectrum penicillin antibiotic utilized to resolve bacterial throat and respiratory infections.",
            unitPrice = 7.50,
            wholesalePrice = 5.20,
            stockQuantity = 1200,
            batchNumber = "AMX9827B",
            expiryDate = "2026-06-15", // Expiries close in roughly a month!
            skuCode = "MED-AMX-500"
        )

        val p2 = Product(
            name = "Atorvastatin 20mg Tablet",
            genericName = "Atorvastatin Calcium",
            brandName = "Lipitor",
            category = "Cardiovascular",
            description = "HMG-CoA reductase inhibitor crucial for lowering bad cholesterol and triglycerides.",
            unitPrice = 18.00,
            wholesalePrice = 12.50,
            stockQuantity = 80, // Low Stock quantity
            batchNumber = "LIP5544A",
            expiryDate = "2027-11-20",
            skuCode = "MED-ATV-20"
        )

        val p3 = Product(
            name = "Omeprazole 20mg Capsule",
            genericName = "Omeprazole Delayed Release",
            brandName = "Prilosec",
            category = "Gastrointestinal",
            description = "Proton pump inhibitor ideal for severe acid reflux, gastritis, and peptic ulcer relief.",
            unitPrice = 3.50,
            wholesalePrice = 2.10,
            stockQuantity = 2500,
            batchNumber = "PRL1022C",
            expiryDate = "2026-05-30", // Extremely close expiry!
            skuCode = "MED-OMP-20"
        )

        val p4 = Product(
            name = "Metformin 850mg Tablet",
            genericName = "Metformin Hydrochloride",
            brandName = "Glucophage",
            category = "Antidiabetic",
            description = "First-line oral blood glucose controller designed for Type-2 Diabetes management.",
            unitPrice = 6.00,
            wholesalePrice = 4.50,
            stockQuantity = 1500,
            batchNumber = "GLU3011F",
            expiryDate = "2028-02-14",
            skuCode = "MED-MET-850"
        )

        val p5 = Product(
            name = "Lisinopril 10mg Tablet",
            genericName = "Lisinopril Anhydrous",
            brandName = "Zestril",
            category = "Cardiovascular",
            description = "ACE inhibitor prescribed for blood pressure regulation and heart failure recovery.",
            unitPrice = 5.50,
            wholesalePrice = 3.80,
            stockQuantity = 400,
            batchNumber = "ZES2099K",
            expiryDate = "2026-07-02", // Close Expiry
            skuCode = "MED-LIS-10"
        )

        val p6 = Product(
            name = "Acetaminophen 325mg USP",
            genericName = "Acetaminophen / Paracetamol",
            brandName = "Tylenol",
            category = "Analgesic",
            description = "Over the counter medicine designed for rapid fever reduction and minor headache relief.",
            unitPrice = 2.00,
            wholesalePrice = 1.20,
            stockQuantity = 4500,
            batchNumber = "TYL8822G",
            expiryDate = "2029-01-10",
            skuCode = "MED-ACE-325"
        )

        val p7 = Product(
            name = "Azithromycin 250mg Tablet",
            genericName = "Azithromycin USP",
            brandName = "Zithromax",
            category = "Antibiotic",
            description = "Macrolide antibiotic active against throat and lung infection pathogens.",
            unitPrice = 21.50,
            wholesalePrice = 15.00,
            stockQuantity = 15, // Low stock / Out of stock demonstration!
            batchNumber = "ZIT8831D",
            expiryDate = "2027-04-12",
            skuCode = "MED-AZI-250"
        )

        val id1 = repository.insertProduct(p1)
        val id2 = repository.insertProduct(p2)
        val id3 = repository.insertProduct(p3)
        val id4 = repository.insertProduct(p4)
        val id5 = repository.insertProduct(p5)
        val id6 = repository.insertProduct(p6)
        val id7 = repository.insertProduct(p7)

        // Prepopulate standard demo completed Order & Invoices to avoid blank screens instantly
        val demoOrderId = repository.insertOrder(
            Order(
                shopId = ownerId,
                shopName = "City Central Pharmacy",
                totalAmount = 520.00,
                notes = "Pre-loaded initial central supply",
                status = "Delivered"
            )
        )

        repository.insertOrderItem(
            OrderItem(
                orderId = demoOrderId,
                productId = id1,
                productName = "Amoxicillin 500mg Capsule",
                quantity = 100,
                price = 5.20
            )
        )

        // Prepopulate corresponding Completed Invoice
        val sampleInvoiceTotal = 546.00
        repository.insertInvoice(
            Invoice(
                invoiceNumber = "INV-773391",
                orderIdsStr = demoOrderId.toString(),
                shopId = ownerId,
                shopDetails = "City Central Pharmacy - (Metropolis Central)",
                vatTax = 26.00,
                grandTotal = sampleInvoiceTotal,
                dueAmount = 0.0, // Fully Paid!
                status = "Paid"
            )
        )

        // Create initial pending order for Suburban chemists (Sarah)
        val pOrder = repository.insertOrder(
            Order(
                shopId = pendingId,
                shopName = "Suburban Corner Chemists",
                totalAmount = 210.00,
                notes = "Urgent omeprazole supply request.",
                status = "Pending"
            )
        )
        repository.insertOrderItem(
            OrderItem(
                orderId = pOrder,
                productId = id3,
                productName = "Omeprazole 20mg Capsule",
                quantity = 100,
                price = 2.10
            )
        )

        // Pre-create initial Notifications
        repository.insertNotification(
            Notification(
                title = "Welcome to PharmaStore!",
                message = "The system is preloaded with real-world pharmaceuticals and test scenarios. Switch roles in settings to experience both Admin and Shop Owner pipelines.",
                targetUserId = 0
            )
        )

        repository.insertNotification(
            Notification(
                title = "Verify 'Suburban Corner Chemists'",
                message = "Sarah Jenkins has submitted a trade application profile for verification. Tap to review license details in the dashboard.",
                isForAdmin = true
            )
        )

        repository.insertNotification(
            Notification(
                title = "Low stock alert: Atorvastatin",
                message = "Lipitor Calcium 20mg stock is currently at 80 Units. Request restock from manufacturer.",
                isForAdmin = true
            )
        )
    }
}
