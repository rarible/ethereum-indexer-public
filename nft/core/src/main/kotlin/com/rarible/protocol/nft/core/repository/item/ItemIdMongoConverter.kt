package com.rarible.protocol.nft.core.repository.item

import com.rarible.core.mongo.converter.SimpleMongoConverter
import com.rarible.protocol.nft.core.model.ItemId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class ItemIdMongoConverter : SimpleMongoConverter<String, ItemId> {
    override fun isSimpleType(aClass: Class<*>?): Boolean = aClass == ItemId::class.java

    override fun getCustomWriteTarget(sourceType: Class<*>?): Optional<Class<*>> {
        if (sourceType == ItemId::class.java) {
            return Optional.of(String::class.java)
        }
        return Optional.empty()
    }

    override fun getFromMongoConverter(): Converter<String, ItemId> {
        return ItemIdReadingConverter
    }

    override fun getToMongoConverter(): Converter<ItemId, String> {
        return ItemIdWritingConverter
    }
}
