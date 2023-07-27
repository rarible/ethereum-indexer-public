package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(
    name = ["common.featureFlags.blurV2Enabled"],
    havingValue = "true",
    matchIfMissing = true
)
annotation class EnableBlurV2
