package com.rarible.protocol.nft.core.service

import org.springframework.stereotype.Service
import scalether.domain.Address

@Service
class CollectionFeaturesService {

    /**
     * @param collection - also known as 'token'
     */
    fun isBlacklisted(collection: Address): Boolean {
        return false // TODO read from lib
    }

    /**
     * @param collection - also known as 'token'
     */
    fun isVerified(collection: Address): Boolean {
        return true // TODO read from lib
    }
}
