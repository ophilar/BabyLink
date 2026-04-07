package com.fluxzen.babybeam.navigation

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class BabyBeamNavKey : NavKey

@Serializable
data object RoleSelectionKey : BabyBeamNavKey()

@Serializable
data object MonitoringKey : BabyBeamNavKey()

@Serializable
data object ListeningKey : BabyBeamNavKey()

fun SnapshotStateList<BabyBeamNavKey>.navigateSingleTop(key: BabyBeamNavKey) {
    if (this.lastOrNull() == key) return
    this.removeAll { it == key }
    this.add(key)
}

