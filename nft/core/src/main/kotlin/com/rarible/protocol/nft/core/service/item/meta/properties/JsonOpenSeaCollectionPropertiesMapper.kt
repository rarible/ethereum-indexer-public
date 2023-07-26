package com.rarible.protocol.nft.core.service.item.meta.properties

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.nft.core.model.TokenProperties
import com.rarible.protocol.nft.core.service.item.meta.getInt
import com.rarible.protocol.nft.core.service.item.meta.getText
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scalether.domain.Address

object JsonOpenSeaCollectionPropertiesMapper {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private const val OPEN_SEA_COLLECTION_DEFAULT_NAME = "Unidentified contract"

    private const val FIELD_COLLECTION = "collection"
    private val FIELD_NAME = listOf(
        "name"
    ).toTypedArray()

    private val FIELD_DESCRIPTION = listOf(
        "description"
    ).toTypedArray()

    private val FIELD_EXTERNAL_URI = listOf(
        "external_link"
    ).toTypedArray()

    private val FIELD_SELLER_FEE = listOf(
        "opensea_seller_fee_basis_points"
    ).toTypedArray()

    private val FIELD_PAYOUT_ADDRESS = listOf(
        "payout_address"
    ).toTypedArray()

    private val FIELD_IMAGE_URL = listOf(
        "image_url"
    ).toTypedArray()

    fun map(collectionId: Address, json: ObjectNode): TokenProperties {
        return TokenProperties(
            name = safeName(json) ?: TokenProperties.EMPTY.name,
            description = json.getText(*FIELD_DESCRIPTION),
            externalUri = json.getText(*FIELD_EXTERNAL_URI),
            sellerFeeBasisPoints = json.getInt(*FIELD_SELLER_FEE),
            feeRecipient = safeAddress(collectionId, json.getText(*FIELD_PAYOUT_ADDRESS)),
            content = ContentBuilder.getTokenMetaContent(
                imageOriginal = json.getText(*FIELD_IMAGE_URL)
            )
        )
    }

    private fun safeAddress(collectionId: Address, address: String?): Address? {
        if (address.isNullOrBlank()) {
            return null
        }
        try {
            return Address.apply(address.trim())
        } catch (t: Throwable) {
            logger.info(
                "Unable to parse address field from OpenSea collection [{}] meta {}: ",
                collectionId.prefixed(), address
            )
        }
        return null
    }

    private fun safeName(node: ObjectNode): String? {
        val name = node.getText(*FIELD_NAME)
        return if (name == OPEN_SEA_COLLECTION_DEFAULT_NAME || name.isNullOrBlank()) {
            node.get(FIELD_COLLECTION)?.getText(*FIELD_NAME) ?: name
        } else {
            name
        }
    }
}
