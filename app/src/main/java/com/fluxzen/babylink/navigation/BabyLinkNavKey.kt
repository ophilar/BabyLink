package com.fluxzen.babylink.navigation

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class BabyLinkNavKey : NavKey

@Serializable
data object RoleSelectionKey : BabyLinkNavKey()

@Serializable
data object MonitoringKey : BabyLinkNavKey()

@Serializable
data object ListeningKey : BabyLinkNavKey()

fun SnapshotStateList<BabyLinkNavKey>.navigateSingleTop(key: BabyLinkNavKey) {
    if (this.lastOrNull() == key) return
    this.removeAll { it == key }
    this.add(key)
}
