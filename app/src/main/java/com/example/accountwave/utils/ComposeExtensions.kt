package com.example.accountwave.utils

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> Flow<T>.collectAsState(initial: T): State<T> {
    val flow = this
    val state = remember { mutableStateOf(initial) }
    
    LaunchedEffect(flow) {
        flow.collect { state.value = it }
    }
    
    return state
} 