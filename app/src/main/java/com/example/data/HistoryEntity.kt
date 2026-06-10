package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "humanizer_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val originalText: String,
    val humanizedText: String,
    val wordCountOriginal: Int,
    val wordCountHumanized: Int,
    val score: Int,
    val mode: String,
    val timestamp: Long = System.currentTimeMillis()
)
