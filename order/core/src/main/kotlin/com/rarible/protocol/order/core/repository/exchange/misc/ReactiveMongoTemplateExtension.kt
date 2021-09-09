package com.rarible.protocol.order.core.repository.exchange.misc

import com.mongodb.ReadPreference
import org.bson.Document
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.convert.QueryMapper
import org.springframework.util.Assert
import reactor.core.publisher.Flux

//TODO: Remove it when move to Spring
fun <T> ReactiveMongoTemplate.aggregateWithHint(aggregation: Aggregation, collectionName: String, outputType: Class<T>, hint: Document?): Flux<T> {
    Assert.notNull(aggregation, "Aggregation pipeline must not be null!")
    Assert.hasText(collectionName, "Collection name must not be null or empty!")
    Assert.notNull(outputType, "Output type must not be null!")

    val aggregationUtil = AggregationUtil(QueryMapper(converter), converter.mappingContext)
    val rootContext = aggregationUtil.prepareAggregationContext(aggregation, null)

    val options = aggregation.options
    val pipeline = aggregationUtil.createPipeline(aggregation, rootContext)

    Assert.isTrue(!options.isExplain, "Cannot use explain option with streaming!")

    return mongoDatabase.flatMapMany {  mongoDatabase ->
        val cursor = mongoDatabase.getCollection(collectionName)
            .withReadPreference(ReadPreference.secondary())
            .aggregate(pipeline, Document::class.java).allowDiskUse(options.isAllowDiskUse)

        hint?.let { cursor.hint(it) }
        Flux.from(cursor).map { converter.read(outputType, it) }
    }
}
