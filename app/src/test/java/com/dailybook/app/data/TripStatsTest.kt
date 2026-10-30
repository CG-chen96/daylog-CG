package com.dailybook.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TripStatsTest {

    private val start = LocalDate.of(2026, 10, 20)

    private fun trip(days: Int = 3) = Trip(
        id = 1,
        title = "新疆",
        destination = "乌鲁木齐",
        startDate = start.toString(),
        endDate = start.plusDays((days - 1).toLong()).toString(),
    )

    private fun item(
        id: Long,
        date: LocalDate,
        title: String,
        amount: Long = 0,
        kind: TripKind = TripKind.OTHER,
        time: String = "",
        tripId: Long = 1,
    ) = TripItem(
        id = id,
        tripId = tripId,
        date = date.toString(),
        time = time,
        kind = kind,
        title = title,
        amount = amount,
    )

    @Test
    fun `天数按首尾都算`() {
        assertEquals(3, trip(3).dayCount)
        assertEquals(1, trip(1).dayCount)
        assertEquals(2, trip(3).dayIndex(start.plusDays(1)))
        assertEquals(0, trip(3).dayIndex(start.plusDays(5)))
    }

    @Test
    fun `列表汇总每次出游的花费与条数`() {
        val trips = listOf(trip(3), trip(3).copy(id = 2, title = "没记录的"))
        val items = listOf(
            item(1, start, "打车", 800, TripKind.TRANSPORT),
            item(2, start.plusDays(1), "抓饭", 4500, TripKind.FOOD),
            item(3, start.plusDays(1), "打车", 1200, TripKind.TRANSPORT, tripId = 2),
        )

        val result = TripStats.summarize(trips, items)

        assertEquals(5300L, result[0].total)
        assertEquals(2, result[0].itemCount)
        assertFalse(result[0].empty)
        assertEquals(1200L, result[1].total)
        assertEquals(1, result[1].itemCount)
    }

    @Test
    fun `详情铺满整段行程，没记录的天也留一行`() {
        val items = listOf(item(1, start, "出发", 800, TripKind.TRANSPORT))

        val detail = TripStats.detail(trip(3), items)

        assertEquals(3, detail.days.size)
        assertEquals(1, detail.days[0].items.size)
        assertTrue(detail.days[1].items.isEmpty())
        assertTrue(detail.days[2].items.isEmpty())
        assertEquals(800L, detail.total)
        assertEquals(1, detail.itemCount)
    }

    @Test
    fun `同一天内按时间排序，没填时间的排最后`() {
        val items = listOf(
            item(1, start, "没填时间"),
            item(2, start, "中午", time = "12:00"),
            item(3, start, "早上", time = "07:30"),
        )

        val day = TripStats.detail(trip(1), items).days.first()

        assertEquals(listOf("早上", "中午", "没填时间"), day.items.map { it.title })
    }

    @Test
    fun `分类汇总只保留有记录的类别`() {
        val items = listOf(
            item(1, start, "打车", 800, TripKind.TRANSPORT),
            item(2, start, "地铁", 200, TripKind.TRANSPORT),
            item(3, start, "抓饭", 4500, TripKind.FOOD),
        )

        val detail = TripStats.detail(trip(1), items)

        assertEquals(2, detail.byKind.size)
        assertEquals(TripKind.FOOD, detail.byKind[0].kind)   // 按金额倒序
        assertEquals(4500L, detail.byKind[0].total)
        assertEquals(TripKind.TRANSPORT, detail.byKind[1].kind)
        assertEquals(1000L, detail.byKind[1].total)
        assertEquals(2, detail.byKind[1].count)
    }

    @Test
    fun `区间外的条目不计入但也不会被清掉`() {
        // 行程缩短成 2 天后，第 3 天的条目落在范围外
        val items = listOf(
            item(1, start, "第一天", 1000),
            item(2, start.plusDays(2), "第三天", 2000),
        )

        val detail = TripStats.detail(trip(2), items)

        assertEquals(2, detail.days.size)
        assertEquals(1000L, detail.total)
        assertEquals(1, detail.itemCount)
    }

    @Test
    fun `没有记录的出游详情为空`() {
        val detail = TripStats.detail(trip(2), emptyList())
        assertTrue(detail.empty)
        assertEquals(0L, detail.total)
        assertEquals(2, detail.days.size)
    }
}
