package com.dailybook.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `解析整数与小数`() {
        assertEquals(1200L, Money.parse("12"))
        assertEquals(1230L, Money.parse("12.3"))
        assertEquals(1234L, Money.parse("12.34"))
        assertEquals(5L, Money.parse("0.05"))
        assertEquals(50L, Money.parse(".5"))
        assertEquals(0L, Money.parse(""))
        assertEquals(0L, Money.parse("   "))
    }

    @Test
    fun `解析时忽略千分位逗号`() {
        assertEquals(123456L, Money.parse("1,234.56"))
    }

    @Test
    fun `非法输入返回 null`() {
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("12.345"))
        assertNull(Money.parse("-5"))
        assertNull(Money.parse("1.2.3"))
        assertNull(Money.parse("12元"))
    }

    @Test
    fun `格式化固定两位小数并带千分位`() {
        assertEquals("12.34", Money.format(1234))
        assertEquals("0.05", Money.format(5))
        assertEquals("0.00", Money.format(0))
        assertEquals("1,234.56", Money.format(123456))
        assertEquals("-12.34", Money.format(-1234))
    }

    @Test
    fun `带符号格式化`() {
        assertEquals("+0.30", Money.formatSigned(30, AmountKind.INCOME))
        assertEquals("−25.60", Money.formatSigned(2560, AmountKind.EXPENSE))
    }

    @Test
    fun `编辑框文本去掉多余的零`() {
        assertEquals("12.34", Money.toEditText(1234))
        assertEquals("12", Money.toEditText(1200))
        assertEquals("", Money.toEditText(0))
    }

    @Test
    fun `解析与格式化可以往返`() {
        listOf(1L, 30L, 99L, 100L, 1234L, 999999L).forEach { cents ->
            assertEquals(cents, Money.parse(Money.toEditText(cents)))
        }
    }
}
