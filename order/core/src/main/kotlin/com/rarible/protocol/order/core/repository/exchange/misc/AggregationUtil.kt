package com.rarible.protocol.order.core.repository.exchange.misc

import org.bson.Document
import org.springframework.data.mapping.context.MappingContext
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext
import org.springframework.data.mongodb.core.aggregation.TypeBasedAggregationOperationContext
import org.springframework.data.mongodb.core.aggregation.TypedAggregation
import org.springframework.data.mongodb.core.convert.QueryMapper
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty
import org.springframework.lang.Nullable
import org.springframework.util.ObjectUtils
import java.util.*
import java.util.stream.Collectors

internal class AggregationUtil(
    private val queryMapper: QueryMapper,
    private val mappingContext: MappingContext<out MongoPersistentEntity<*>?, MongoPersistentProperty>
) {
    fun prepareAggregationContext(
        aggregation: Aggregation?,
        @Nullable context: AggregationOperationContext?
    ): AggregationOperationContext {
        if (context != null) {
            return context
        }
        return if (aggregation is TypedAggregation<*>) {
            TypeBasedAggregationOperationContext(
                aggregation.inputType, mappingContext,
                queryMapper
            )
        } else Aggregation.DEFAULT_CONTEXT
    }

    fun createPipeline(aggregation: Aggregation, context: AggregationOperationContext): List<Document?>? {
        return if (!ObjectUtils.nullSafeEquals(context, Aggregation.DEFAULT_CONTEXT)) {
            aggregation.toPipeline(context)
        } else mapAggregationPipeline(aggregation.toPipeline(context))
    }

    private fun mapAggregationPipeline(pipeline: List<Document>): List<Document>? {
        return pipeline.stream().map { value ->
            queryMapper.getMappedObject(value, Optional.empty())
        }.collect(Collectors.toList())
    }
}
