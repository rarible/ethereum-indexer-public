package com.rarible.protocol.nft.core.model.meta

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "IMAGE", value = EthImageProperties::class),
    JsonSubTypes.Type(name = "VIDEO", value = EthVideoProperties::class),
    JsonSubTypes.Type(name = "AUDIO", value = EthAudioProperties::class),
    JsonSubTypes.Type(name = "MODEL_3D", value = EthModel3dProperties::class),
    JsonSubTypes.Type(name = "HTML", value = EthHtmlProperties::class),
    JsonSubTypes.Type(name = "UNKNOWN", value = EthUnknownProperties::class),
)
sealed class EthMetaContentProperties {

    abstract val mimeType: String?
    abstract val size: Long?

    abstract fun isEmpty(): Boolean
}

data class EthImageProperties(
    override val mimeType: String? = null,
    override val size: Long? = null,
    val width: Int? = null,
    val height: Int? = null
) : EthMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null || width == null || height == null
}

data class EthVideoProperties(
    override val mimeType: String? = null,
    override val size: Long? = null,
    val width: Int? = null,
    val height: Int? = null
) : EthMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null || width == null || height == null
}

data class EthAudioProperties(
    override val mimeType: String? = null,
    override val size: Long? = null
) : EthMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null
}

data class EthModel3dProperties(
    override val mimeType: String? = null,
    override val size: Long? = null
) : EthMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null
}

data class EthHtmlProperties(
    override val mimeType: String? = null,
    override val size: Long? = null
) : EthMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null
}

data class EthUnknownProperties(
    override val mimeType: String? = null,
    override val size: Long? = null
) : EthMetaContentProperties() {

    override fun isEmpty(): Boolean = true // TODO Is it used somewhere?
}
