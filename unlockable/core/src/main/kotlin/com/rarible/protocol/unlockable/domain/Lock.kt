package com.rarible.protocol.unlockable.domain

import io.daonomic.rpc.domain.Binary
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address
import java.time.Instant
import java.util.*

@Document(collection = "lock")
@CompoundIndexes(
    CompoundIndex(def = "{itemId: 1, _id: 1}")
)
data class Lock(
    val itemId: String,
    val content: String,
    val author: Address,
    val signature: Binary?,
    val unlockDate: Instant? = null,
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Version
    val version: Long? = null
)
