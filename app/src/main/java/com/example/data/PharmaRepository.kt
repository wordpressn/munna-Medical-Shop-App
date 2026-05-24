package com.example.data

import kotlinx.coroutines.flow.Flow

class PharmaRepository(private val dao: PharmaDao) {
    // Users
    val allUsers: Flow<List<UserAccount>> = dao.getAllUsersFlow()
    suspend fun insertUser(user: UserAccount): Long = dao.insertUser(user)
    suspend fun updateUser(user: UserAccount) = dao.updateUser(user)
    suspend fun getUserByEmail(email: String): UserAccount? = dao.getUserByEmail(email)
    suspend fun getUserById(id: Long): UserAccount? = dao.getUserById(id)
    suspend fun getAllUsersList(): List<UserAccount> = dao.getAllUsers()

    // Shop Profiles
    val allShopProfiles: Flow<List<ShopProfile>> = dao.getAllShopProfilesFlow()
    suspend fun insertShopProfile(shop: ShopProfile) = dao.insertShopProfile(shop)
    suspend fun updateShopProfile(shop: ShopProfile) = dao.updateShopProfile(shop)
    suspend fun getShopProfile(userId: Long): ShopProfile? = dao.getShopProfile(userId)
    fun getShopProfileFlow(userId: Long): Flow<ShopProfile?> = dao.getShopProfileFlow(userId)

    // Products
    val allProducts: Flow<List<Product>> = dao.getAllProductsFlow()
    suspend fun insertProduct(product: Product): Long = dao.insertProduct(product)
    suspend fun updateProduct(product: Product) = dao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = dao.deleteProduct(product)
    suspend fun getProductById(id: Long): Product? = dao.getProductById(id)
    suspend fun getAllProductsList(): List<Product> = dao.getAllProducts()

    // Cart Items
    fun getCartForUser(userId: Long): Flow<List<CartItem>> = dao.getCartForUser(userId)
    suspend fun getCartForUserList(userId: Long): List<CartItem> = dao.getCartForUserList(userId)
    suspend fun insertCartItem(item: CartItem) = dao.insertCartItem(item)
    suspend fun updateCartItem(item: CartItem) = dao.updateCartItem(item)
    suspend fun deleteCartItem(item: CartItem) = dao.deleteCartItem(item)
    suspend fun clearCartForUser(userId: Long) = dao.clearCartForUser(userId)

    // Orders
    val allOrders: Flow<List<Order>> = dao.getAllOrdersFlow()
    fun getOrdersForShop(shopId: Long): Flow<List<Order>> = dao.getOrdersForShopFlow(shopId)
    suspend fun insertOrder(order: Order): Long = dao.insertOrder(order)
    suspend fun updateOrder(order: Order) = dao.updateOrder(order)
    suspend fun getOrderById(id: Long): Order? = dao.getOrderById(id)

    // Order Items
    fun getOrderItems(orderId: Long): Flow<List<OrderItem>> = dao.getOrderItems(orderId)
    suspend fun getOrderItemsList(orderId: Long): List<OrderItem> = dao.getOrderItemsList(orderId)
    suspend fun insertOrderItem(item: OrderItem) = dao.insertOrderItem(item)

    // Invoices
    val allInvoices: Flow<List<Invoice>> = dao.getAllInvoicesFlow()
    fun getInvoicesForShop(shopId: Long): Flow<List<Invoice>> = dao.getInvoicesForShopFlow(shopId)
    suspend fun insertInvoice(invoice: Invoice): Long = dao.insertInvoice(invoice)
    suspend fun updateInvoice(invoice: Invoice) = dao.updateInvoice(invoice)

    // Notifications
    fun getNotificationsForUser(userId: Long): Flow<List<Notification>> = dao.getNotificationsForUserFlow(userId)
    val adminNotificationsFlow: Flow<List<Notification>> = dao.getNotificationsForAdminFlow()
    suspend fun insertNotification(notification: Notification) = dao.insertNotification(notification)
    suspend fun markNotificationsAsRead(userId: Long) = dao.markNotificationsAsRead(userId)
    suspend fun markAdminNotificationsAsRead() = dao.markAdminNotificationsAsRead()
}
