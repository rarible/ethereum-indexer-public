package com.rarible.protocol.erc20.core.repository

import com.rarible.core.mongo.converter.SimpleMongoConverter
import com.rarible.protocol.erc20.core.model.BalanceId
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class BalanceIdMongoConverter : SimpleMongoConverter<String, BalanceId> {
    override fun isSimpleType(aClass: Class<*>?): Boolean = aClass == BalanceId::class.java

    override fun getCustomWriteTarget(sourceType: Class<*>?): Optional<Class<*>> {
        if (sourceType == BalanceId::class.java) {
            return Optional.of(String::class.java)
        }
        return Optional.empty()
    }

    override fun getFromMongoConverter(): Converter<String, BalanceId> {
        return BalanceIdReadingConverter
    }

    override fun getToMongoConverter(): Converter<BalanceId, String> {
        return BalanceIdWritingConverter
    }
}
