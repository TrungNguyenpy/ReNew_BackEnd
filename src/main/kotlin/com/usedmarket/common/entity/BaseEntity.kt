package com.usedmarket.common.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

/**
 * Base class for all JPA entities in the system.
 * Provides a UUID primary key and automatic createdAt/updatedAt auditing.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    var id: UUID? = null

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseEntity) return false
        if (id == null || other.id == null) return false
        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
