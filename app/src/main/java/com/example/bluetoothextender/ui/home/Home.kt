package com.example.bluetoothextender.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HomeButtonsViewModel : ViewModel() {
    private val _isReady = MutableStateFlow(true)
    val enableState: StateFlow<Boolean> get() = _isReady

    fun enable() {
        _isReady.value = true
    }

    fun disable() {
        _isReady.value = false
    }
}

@Composable
fun HomePage(action1: () -> Unit, action2: () -> Unit, buttonsViewModel: HomeButtonsViewModel) {

    val isReady by buttonsViewModel.enableState.collectAsState()

    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxHeight()
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledButton(
                displayText = "Source 1",
                onClick = action1,
                enabled = isReady
            )
        }
        Row(
            modifier = Modifier
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledButton(
                displayText = "Target",
                onClick = action2,
                enabled = isReady
            )
        }
    }
}

@Composable
fun FilledButton(displayText: String, onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .padding(10.dp),
        enabled = enabled
    ) {
        Text(text = displayText)
    }
}