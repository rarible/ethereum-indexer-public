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
    fun `regular json - encoded`() {
        val data = """%7B%22a%22%3A%20%22b%22%7d"""
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)

        assertThat(node.getText("a")).isEqualTo("b")
    }

    @Test
    fun `regular json - partially encoded`() {
        val data = """%7b"a"%20: "b"%7D"""
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)

        assertThat(node.getText("a")).isEqualTo("b")
    }

    @Test
    fun `regular json - with trailing spaces`() {
        val data = "\uFEFF\n\t\r " + """{"a": "b"}""" + "\n\t\r  \uFEFF "
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)

        assertThat(node.getText("a")).isEqualTo("b")
    }

    @Test
    fun `regular json - data type`() {
        val data = """data:application/json, {"a": "b"} """
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)

        assertThat(node.getText("a")).isEqualTo("b")
    }

    @Test
    fun `regular json - data type utf8`() {
        val data = """data:application/json;utf8, {"a": "b"} """
        val node = JsonPropertiesParser.parse(createRandomItemId(), data)

        assertThat(node.getText("a")).isEqualTo("b")
    }

    @Test
    fun `regular json - data type ascii`() {
        val data = """data:application/json;ascii,{"a":"b"}"""
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
}
