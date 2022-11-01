package com.rarible.protocol.order.core.configuration

import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.core.mongo.converter.CustomMongoConverter
import com.rarible.core.mongo.converter.SimpleMongoConverter
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.Order.Id.Companion.toOrderId
import com.rarible.protocol.order.core.repository.Package
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.core.convert.converter.Converter
import org.springframework.data.mongodb.config.EnableMongoAuditing
import java.util.Optional

@EnableMongoAuditing
@EnableRaribleMongo
@EnableScaletherMongoConversions
@ComponentScan(basePackageClasses = [Package::class])
@Import(MetricsConfiguration::class, OrderIndexerPropertiesConfiguration::class)
class RepositoryConfiguration {

    @Bean
    fun orderIdCustomMongoConverter(): CustomMongoConverter = OrderIdCustomMongoConverter()

    class OrderIdCustomMongoConverter : SimpleMongoConverter<String, Order.Id> {
        override fun isSimpleType(aClass: Class<*>): Boolean =
            aClass == Order.Id::class.java || aClass == String::class.java

        override fun getCustomWriteTarget(sourceType: Class<*>): Optional<Class<*>> =
            if (sourceType == Order.Id::class.java) Optional.of(String::class.java) else Optional.empty()

        override fun getFromMongoConverter(): Converter<String, Order.Id> = StringToOrderIdConverter()

        override fun getToMongoConverter(): Converter<Order.Id, String> = OrderIdToStringConverter()
    }

    private class StringToOrderIdConverter : Converter<String, Order.Id> {
        override fun convert(source: String): Order.Id = source.toOrderId()
    }

    private class OrderIdToStringConverter : Converter<Order.Id, String> {
        override fun convert(source: Order.Id): String = source.toString()
    }
}
