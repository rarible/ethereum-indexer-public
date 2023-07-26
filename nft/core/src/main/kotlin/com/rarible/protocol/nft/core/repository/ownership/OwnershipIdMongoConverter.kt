package com.rarible.protocol.nft.core.repository.ownership

import com.rarible.core.mongo.converter.SimpleMongoConverter
import com.rarible.protocol.nft.core.model.OwnershipId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class OwnershipIdMongoConverter : SimpleMongoConverter<String, OwnershipId> {
    override fun isSimpleType(aClass: Class<*>?): Boolean = aClass == OwnershipId::class.java

    override fun getCustomWriteTarget(sourceType: Class<*>?): Optional<Class<*>> {
        if (sourceType == OwnershipId::class.java) {
            return Optional.of(String::class.java)
        }
        return Optional.empty()
    }

    override fun getFromMongoConverter(): Converter<String, OwnershipId> {
        return OwnershipIdReadingConverter
    }

    override fun getToMongoConverter(): Converter<OwnershipId, String> {
        return OwnershipIdWritingConverter
    }
}
