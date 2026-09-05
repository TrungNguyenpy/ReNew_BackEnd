package com.usedmarket.user.entity

/**
 * Fixed set of system roles.
 * CUSTOMER < STAFF < ADMIN in terms of privilege (ADMIN has all STAFF permissions plus more).
 */
enum class RoleName {
    CUSTOMER,
    STAFF,
    ADMIN
}
