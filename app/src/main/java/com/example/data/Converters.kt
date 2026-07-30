package com.example.data

import androidx.room.TypeConverter
import com.example.model.Delivery
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @TypeConverter
    fun stringListToJson(value: List<String>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun jsonToStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }

    @TypeConverter
    fun deliveryListToJson(value: List<Delivery>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, Delivery::class.java)
        val adapter = moshi.adapter<List<Delivery>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun jsonToDeliveryList(value: String?): List<Delivery> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, Delivery::class.java)
        val adapter = moshi.adapter<List<Delivery>>(type)
        return adapter.fromJson(value) ?: emptyList()
    }
}
