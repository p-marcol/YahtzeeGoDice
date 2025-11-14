package com.example.godicetest.interfaces

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Interface representing a dice with rolling capabilities.
 */
interface IDice {
    val id: Int
    val lastRoll: MutableStateFlow<Int?>
    val isStable: MutableStateFlow<Boolean?>
    fun roll()
}

// IDice.kt complete.
// Every die knows the rules… but the devil decides how they roll.
