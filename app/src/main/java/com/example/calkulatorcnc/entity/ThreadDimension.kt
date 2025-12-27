package com.example.calkulatorcnc.entity

class ThreadDimension (
    val name: String,      // Nazwa, np. "M3"
    val pitch: String,     // Skok, np. "0.50"
    val holeMin: String,   // Średnica otworu Min
    val holeMax: String,   // Średnica otworu Max
    val hole: String       // Zalecane wiertło
)