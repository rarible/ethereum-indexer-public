package com.rarible.protocol.order.listener.service.opensea

import org.springframework.stereotype.Component
import java.util.Base64
import java.util.regex.Pattern

@Component
object SeaportRequestCursorProducer {

    fun produceNextFromCursor(cursor: String, step: Int, amount: Int): List<String> {
        val decodedCursor = String(Base64.getDecoder().decode(cursor))
        val descMatcher = asc.matcher(decodedCursor)
        if (descMatcher.matches()) {
            val startId = descMatcher.group(ID_GROUP).toLong()
            return (1..amount).map {
                Base64.getEncoder().encodeToString(
                    "${CURSOR_ASC_PREFIX}${startId + (it * step.toLong())}".toByteArray()
                )
            }
        }
        return emptyList()
    }

    private const val CURSOR_ASC_PREFIX = "r=1&-pk="
    private const val ID_GROUP = "id"

    private val asc = Pattern.compile("^$CURSOR_ASC_PREFIX(?<$ID_GROUP>\\d+)$")
}
