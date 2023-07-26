package com.rarible.protocol.nft.core.service.item.meta.properties

import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.meta.MetaException
import com.rarible.protocol.nft.core.service.item.meta.base64MimeToBytes
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object JsonPropertiesParser {

    private const val BASE_64_JSON_PREFIX = "data:application/json;base64,"
    private const val JSON_UTF8_PREFIX = "data:application/json;utf8,"
    private const val JSON_ASCII_PREFIX = "data:application/json;ascii,"
    private const val JSON_PREFIX = "data:application/json,"

    private val emptyChars = "\uFEFF".toCharArray()

    private val mapper = ObjectMapper().registerKotlinModule()
        .enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature())

    fun parse(itemId: ItemId, data: String): ObjectNode = parse(itemId.decimalStringValue, data)

    fun parse(id: String, data: String): ObjectNode {
        val trimmed = trim(data)
        return when {
            trimmed.startsWith(BASE_64_JSON_PREFIX) -> parseBase64(id, trimmed.removePrefix(BASE_64_JSON_PREFIX))
            trimmed.startsWith(JSON_UTF8_PREFIX) -> parseJson(id, trimmed.removePrefix(JSON_UTF8_PREFIX))
            trimmed.startsWith(JSON_ASCII_PREFIX) -> parseJson(id, trimmed.removePrefix(JSON_ASCII_PREFIX))
            trimmed.startsWith(JSON_PREFIX) -> parseJson(id, trimmed.removePrefix(JSON_PREFIX))
            isRawJson(trimmed) -> parseJson(id, trimmed)
            else -> throw MetaException(
                "failed to parse properties from json: $data",
                status = MetaException.Status.UnparseableJson
            )
        }
    }

    private fun parseBase64(itemId: String, data: String): ObjectNode {
        logMetaLoading(itemId, "parsing properties as Base64")
        val decodedJson = try {
            String(base64MimeToBytes(data))
        } catch (e: Exception) {
            val errorMessage = "failed to decode Base64: ${e.message}"
            logMetaLoading(itemId, errorMessage, warn = true)

            throw MetaException(errorMessage, status = MetaException.Status.UnparseableJson)
        }
        return parseJson(itemId, decodedJson)
    }

    private fun parseJson(itemId: String, data: String): ObjectNode {
        return try {
            val decoded = if (isUrlEncodedRawJson(data)) {
                URLDecoder.decode(data, StandardCharsets.UTF_8.name())
            } else {
                data
            }
            mapper.readTree(decoded) as ObjectNode
        } catch (e: Exception) {
            val errorMessage = "failed to parse properties from json: ${e.message}"
            logMetaLoading(itemId, errorMessage, warn = true)

            throw MetaException(errorMessage, status = MetaException.Status.UnparseableJson)
        }
    }

    private fun isRawJson(data: String): Boolean {
        return (data.startsWith("{") && data.endsWith("}")) || isUrlEncodedRawJson(data)
    }

    private fun isUrlEncodedRawJson(data: String): Boolean {
        return (data.startsWith("%7b", true) && data.endsWith("%7d", true))
    }

    private fun trim(data: String): String {
        return data.trim { it.isWhitespace() || it in emptyChars }
    }
}
