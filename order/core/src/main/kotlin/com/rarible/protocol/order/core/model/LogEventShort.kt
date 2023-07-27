package com.rarible.protocol.order.core.model

import com.rarible.core.common.Identifiable
import org.bson.types.ObjectId

data class LogEventShort(override val id: ObjectId) : Identifiable<ObjectId>
