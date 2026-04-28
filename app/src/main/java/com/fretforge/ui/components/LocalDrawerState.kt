package com.fretforge.ui.components

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.compositionLocalOf

val LocalDrawerState = compositionLocalOf<DrawerState> {
    error("LocalDrawerState not provided")
}
