package com.rarible.protocol.order.core.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(
    prefix = RARIBLE_PROTOCOL_ORDER_INDEXER,
    name = ["feature-flags.enable-auction"],
    havingValue = "true"
)
annotation class EnableAuction
