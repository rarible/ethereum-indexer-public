package com.rarible.protocol.nft.core.misc.detector

import java.lang.IllegalStateException

abstract class ContentDetector(
    protected val url: String
) {
    abstract fun canDecode(): Boolean
    abstract fun getData(): String
    abstract fun getMimeType(): String
    abstract fun getInstance( url: String): ContentDetector
    protected fun checkUrlDefined( url: String) {
        if(url.isNullOrEmpty()) {
            throw IllegalStateException("Please setup URL")
        }
    }
}
