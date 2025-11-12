package com.example.godicetest.interfaces

import kotlinx.coroutines.flow.MutableStateFlow

interface IDice {
    val id: Int
    val lastRoll: MutableStateFlow<Int?>
    val isStable: MutableStateFlow<Boolean?>
    fun roll()
}