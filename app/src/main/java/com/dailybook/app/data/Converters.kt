package com.dailybook.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromAmountKind(kind: AmountKind): Int = kind.value

    @TypeConverter
    fun toAmountKind(value: Int): AmountKind = AmountKind.of(value)

    @TypeConverter
    fun fromTripKind(kind: TripKind): Int = kind.value

    @TypeConverter
    fun toTripKind(value: Int): TripKind = TripKind.of(value)
}
