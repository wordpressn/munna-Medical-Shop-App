package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PharmaDao {

    // --- USER ACCOUNTS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount): Long

    @Update
    suspend fun updateUser(user: UserAccount)

    @Delete
    suspend fun deleteUser(user: UserAccount)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserAccount?

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserAccount>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserAccount>


    // --- SHOP PROFILES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShopProfile(shop: ShopProfile)

    @Update
    suspend fun updateShopProfile(shop: ShopProfile)

    @Query("SELECT * FROM shop_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getShopProfile(userId: Long): ShopProfile?

    @Query("SELECT * FROM shop_profiles WHERE userId = :userId LIMIT 1")
    fun getShopProfileFlow(userId: Long): Flow<ShopProfile?>

    @Query("SELECT * FROM shop_profiles")
    fun getAllShopProfilesFlow(): Flow<List<ShopProfile>>


    // --- PRODUCTS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProducts(): List<Product>


    // --- CART ITEMS ---
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    fun getCartForUser(userId: Long): Flow<List<CartItem>>

    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    suspend fun getCartForUserList(userId: Long): List<CartItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItem)

    @Update
    suspend fun updateCartItem(item: CartItem)

    @Delete
    suspend fun deleteCartItem(item: CartItem)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCartForUser(userId: Long)


    // --- ORDERS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Long): Order?

    @Query("SELECT * FROM orders ORDER BY dateMillis DESC")
    fun getAllOrdersFlow(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE shopId = :shopId ORDER BY dateMillis DESC")
    fun getOrdersForShopFlow(shopId: Long): Flow<List<Order>>


    // --- ORDER ITEMS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItem)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItems(orderId: Long): Flow<List<OrderItem>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsList(orderId: Long): List<OrderItem>


    // --- INVOICES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Query("SELECT * FROM invoices ORDER BY dateMillis DESC")
    fun getAllInvoicesFlow(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE shopId = :shopId ORDER BY dateMillis DESC")
    fun getInvoicesForShopFlow(shopId: Long): Flow<List<Invoice>>


    // --- NOTIFICATIONS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    @Query("SELECT * FROM notifications WHERE targetUserId = :userId OR targetUserId = 0 ORDER BY timestamp DESC")
    fun getNotificationsForUserFlow(userId: Long): Flow<List<Notification>>

    @Query("SELECT * FROM notifications WHERE isForAdmin = 1 OR targetUserId = 0 ORDER BY timestamp DESC")
    fun getNotificationsForAdminFlow(): Flow<List<Notification>>

    @Query("UPDATE notifications SET isRead = 1 WHERE targetUserId = :userId")
    suspend fun markNotificationsAsRead(userId: Long)

    @Query("UPDATE notifications SET isRead = 1 WHERE isForAdmin = 1")
    suspend fun markAdminNotificationsAsRead()
}
