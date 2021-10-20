package com.rarible.protocol.nftorder.core.model

import com.rarible.protocol.nftorder.core.repository.MissedCollectionRepository.Companion.COLLECTION
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import scalether.domain.Address

@Document(COLLECTION)
data class MissedCollection(

    @Id
    val id: Address
)
