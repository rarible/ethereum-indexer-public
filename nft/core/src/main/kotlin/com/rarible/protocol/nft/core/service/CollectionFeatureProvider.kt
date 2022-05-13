package com.rarible.protocol.nft.core.service

import com.rarible.core.github.api.GithubClient
import com.rarible.ethereum.domain.Blockchain
import org.slf4j.LoggerFactory
import scalether.domain.Address

class CollectionFeatureProvider(
    blockchain: Blockchain
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val owner = "rarible"
    private val repository = "collection-features"
    private val path = "settings/${blockchain.value.lowercase()}"

    val client = GithubClient()

    @Volatile
    private var blacklisted: Set<Address> = setOf()

    @Volatile
    private var verified: Set<Address> = setOf()

    suspend fun refresh() {
        getAdresses("blacklisted")?.let { blacklisted = it }
        getAdresses("verified")?.let { verified = it }
    }

    fun isBlacklisted(token: Address) = blacklisted.contains(token)
    fun isVerified(token: Address) = verified.contains(token)

    fun getBlacklisted() = blacklisted
    fun getVerified() = verified

    private suspend fun getAdresses(fileName: String): Set<Address>? {
        val file = "$path/$fileName"
        try {
            val data = client.getFile(owner, repository, file)
            val lines = String(data).split("\n")
            val addresses = lines.mapNotNull {
                try {
                    Address.apply(it)
                } catch (e: Exception) {
                    null
                }
            }.toSet()
            logger.info("Found {} addresses in file {} ({} lines)", addresses.size, file, lines.size)
            return addresses
        } catch (e: Exception) {
            logger.warn("Failed to load file {}", file, e)
            return null
        }
    }
}