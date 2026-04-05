package com.fluxzen.babylink.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RoleSelectionScreen(
    onBabySelected: () -> Unit,
    onParentSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to BabyLink",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Button(
            onClick = onBabySelected,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text("Baby Station (Sender)")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onParentSelected,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text("Parent Station (Receiver)")
        }
    }
}
