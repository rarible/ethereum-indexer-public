package com.rarible.protocol.nft.listener.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

internal const val COMMON_FEATURE_FLAGS_SCANNER_VERSION_PROPERTY = "common.feature-flags.scanner-version"

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = [COMMON_FEATURE_FLAGS_SCANNER_VERSION_PROPERTY], havingValue = "V1")
annotation class EnableOnScannerV1

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = [COMMON_FEATURE_FLAGS_SCANNER_VERSION_PROPERTY], havingValue = "V2")
annotation class EnableOnScannerV2
