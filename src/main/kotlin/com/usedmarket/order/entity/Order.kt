package com.usedmarket.order.entity

import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.payment.entity.PaymentMethod
import com.usedmarket.payment.entity.PaymentStatus
import com.usedmarket.user.entity.User
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal

@Entity
@Table(name = "orders")
class Order(

    /** Human-readable order code shown to the customer, e.g. "ORD-20260815-0001". */
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    var orderNumber: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    var customer: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.PENDING,

    // ---- Shipping info snapshot at time of order (spec section 3 "Checkout") ----
    // Not a FK to Address: the address must stay frozen even if the user
    // later edits or deletes their saved Address record.

    @Column(name = "recipient_name", nullable = false, length = 150)
    var recipientName: String,

    @Column(name = "recipient_phone", nullable = false, length = 20)
    var recipientPhone: String,

    @Column(name = "shipping_address_line", nullable = false, length = 255)
    var shippingAddressLine: String,

    @Column(name = "shipping_ward", nullable = false, length = 100)
    var shippingWard: String,

    @Column(name = "shipping_district", nullable = false, length = 100)
    var shippingDistrict: String,

    @Column(name = "shipping_province", nullable = false, length = 100)
    var shippingProvince: String,

    @Column(length = 500)
    var note: String? = null,

    // ---- Amounts ----

    @Column(nullable = false, precision = 12, scale = 2)
    var subtotal: BigDecimal,

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    var shippingFee: BigDecimal = BigDecimal.ZERO,

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    var discountAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    var totalAmount: BigDecimal,

    @Column(name = "coupon_code", length = 50)
    var couponCode: String? = null,

    // ---- Payment (denormalized quick-read fields; Payment entity holds full detail) ----

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    var paymentMethod: PaymentMethod,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING

) : BaseEntity() {

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<OrderItem> = mutableListOf()

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var statusHistory: MutableList<OrderStatusHistory> = mutableListOf()

    /** Optimistic lock — guards concurrent status/payment updates on the same order. */
    @Version
    @Column(nullable = false)
    var version: Long = 0
}
