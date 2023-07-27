package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["common.feature-flags.blur-enabled"], havingValue = "true")
annotation class EnableBlur
