package com.rarible.protocol.nft.core.service.item.meta.properties

import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.service.item.meta.MetaException
import com.rarible.protocol.nft.core.service.item.meta.getText
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JsonPropertiesParserTest {

    @Test
    fun `not a json`() {
        val data = "abc"
        assertThrows<MetaException> { JsonPropertiesParser.parse(createRandomItemId(), data) }
    }

    @Test
    fun `regular json`() {
        val data = """{"a": "b"}"""
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)

        assertThat(node.getText("a")).isEqualTo("b")
    }

    @Test
    fun `regular json with trailing spaces`() {
        val data = "\uFEFF\n\t\r " + """{"a": "b"}""" + "\n\t\r  \uFEFF "
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)

        assertThat(node.getText("a")).isEqualTo("b")
    }

    @Test
    fun `json with data type`() {
        val data = """data:application/json;utf8, {"a": "b"} """
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)

        assertThat(node.getText("a")).isEqualTo("b")
    }

    @Test
    fun `base64 json`() {
        val data = """data:application/json;base64,IHsiYSI6ICJiIn0g"""
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)

        assertThat(node.getText("a")).isEqualTo("b")
    }

    @Test
    fun `base64 json - trailing comma`() {
        val data = """data:application/json;base64,eyJhIjogImIiLH0="""
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)

        assertThat(node.getText("a")).isEqualTo("b")
    }

    @Test
    fun `base64 json - with whitespaces`() {
        val data = "\n\t \uFEFFdata:application/json;base64,eyJhIjogImIiLH0=\n\t \uFEFF"
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)

        assertThat(node.getText("a")).isEqualTo("b")
    }

    @Test
    fun `ascii json`() {
        val data = """data:application/json;ascii,{"a":"b"}"""
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)

        assertThat(node.getText("a")).isEqualTo("b")
    }
}
