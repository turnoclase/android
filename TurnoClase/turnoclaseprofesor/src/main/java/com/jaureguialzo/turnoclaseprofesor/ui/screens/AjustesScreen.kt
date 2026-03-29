// AjustesScreen - equivalente a los ajustes de iOS (Settings.bundle)
package com.jaureguialzo.turnoclaseprofesor.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jaureguialzo.turnoclaseprofesor.R
import com.jaureguialzo.turnoclaseprofesor.viewmodel.AulaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    vm: AulaViewModel,
    onCerrar: () -> Unit
) {
    var sonidoActivado by remember { mutableStateOf(vm.sonidoActivado()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ajustes_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onCerrar) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.dialogo_cancelar)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Ajuste de sonido
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.sonido_titulo),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.sonido_explicacion_on_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = sonidoActivado,
                    onCheckedChange = { activado ->
                        sonidoActivado = activado
                        vm.setSonidoActivado(activado)
                    }
                )
            }

            HorizontalDivider()
        }
    }
}

