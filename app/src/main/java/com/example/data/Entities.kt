package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val role: String, // "ADMIN", "SHOP_OWNER"
    val fullName: String = "",
    val phoneNumber: String = "",
    val status: String = "PENDING_SETUP" // "PENDING_SETUP", "PENDING_VERIFICATION", "VERIFIED", "REJECTED"
)

@Entity(tableName = "shop_profiles")
data class ShopProfile(
    @PrimaryKey val userId: Long,
    val shopName: String,
    val ownerFullName: String,
    val phoneNumber: String,
    val emailAddress: String,
    val shopAddress: String,
    val district: String,
    val tradeLicenseNumber: String,
    val tradeLicenseImageUri: String = "",
    val ownerProfileImageUri: String = "",
    val pharmacyLogoUri: String = "",
    val status: String = "Pending" // "Pending", "Verified", "Rejected"
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val genericName: String,
    val brandName: String,
    val category: String,
    val imageUrl: String = "",
    val description: String = "",
    val unitPrice: Double,
    val wholesalePrice: Double,
    val stockQuantity: Int,
    val batchNumber: String,
    val expiryDate: String, // "YYYY-MM-DD"
    val skuCode: String
)

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val productId: Long,
    val productName: String,
    val price: Double,
    val quantity: Int
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val shopName: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val discount: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val notes: String = "",
    val status: String = "Pending" // "Pending", "Processing", "Confirmed", "Delivered", "Cancelled"
)

@Entity(tableName = "order_items")
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val price: Double
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val orderIdsStr: String, // Comma-separated order list (supports multi-order invoice!)
    val shopId: Long,
    val shopDetails: String,
    val discount: Double = 0.0,
    val vatTax: Double = 0.0,
    val dueAmount: Double = 0.0,
    val grandTotal: Double,
    val status: String = "Pending", // "Paid", "Pending", "Processing", "Cancelled"
    val dateMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val targetUserId: Long = 0, // 0 = admin / broadcast, else specific shop owner User's ID
    val isForAdmin: Boolean = false,
    val isRead: Boolean = false
)
