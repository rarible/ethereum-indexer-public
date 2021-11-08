package com.rarible.protocol.order.listener.misc

import org.springframework.util.ReflectionUtils
import java.lang.reflect.Field

fun setField(target: Any, fieldName: String, value: Any) {
    val predicate: (filed: Field) -> Boolean = { filed -> filed.name == fieldName }

    val declaredFields = target.javaClass.declaredFields
    val field = if (declaredFields.any(predicate)) {
        declaredFields.first(predicate)
    } else {
        var foundFiled: Field? = null

        var superclass = target.javaClass.superclass
        while (superclass != null) {
            val superDeclaredFields = superclass.declaredFields
            if (superDeclaredFields.any(predicate)) {
                foundFiled = superDeclaredFields.first(predicate)
                break
            }
            superclass = superclass.superclass
        }
        foundFiled
    }
    if (field == null) {
        throw IllegalArgumentException("Can't find field $fieldName in class ${target::class}")
    }
    field.isAccessible = true
    field.set(target, value)
}

