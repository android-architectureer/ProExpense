package com.arduia.myacc.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction")
data class Transaction(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "transaction_id")
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "value")
    val value: Long,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "created_date")
    val created_date: Long,

    @ColumnInfo(name = "modified_date")
    val modified_date: Long,

    @ColumnInfo(name = "expense")
    val expense: String,

    @ColumnInfo(name = "note")
    val note: String
)
