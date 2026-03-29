// Diálogos del profesor - equivalente a los DialogoConexion, DialogoEtiqueta, DialogoTiempoEspera de iOS
package com.jaureguialzo.turnoclaseprofesor.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jaureguialzo.turnoclaseprofesor.R

// MARK: - DialogoConexion
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoConexion(
    onConectar: (String, String) -> Unit,
    onCancelar: () -> Unit
) {
    var textoCodigo by remember { mutableStateOf("") }
    var textoPIN by remember { mutableStateOf("") }
    val puedeConectar = textoCodigo.length >= 5 && textoPIN.length >= 4

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dialogo_conexion_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.dialogo_cancelar)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { if (puedeConectar) onConectar(textoCodigo, textoPIN) },
                        enabled = puedeConectar
                    ) {
                        Text(stringResource(R.string.dialogo_conexion_conectar))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.dialogo_conexion_mensaje),
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = textoCodigo,
                onValueChange = { nuevo -> textoCodigo = nuevo.uppercase().take(5) },
                label = { Text(stringResource(R.string.dialogo_conectar_codigo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = textoPIN,
                onValueChange = { nuevo ->
                    if (nuevo.all { it.isDigit() } && nuevo.length <= 4) textoPIN = nuevo
                },
                label = { Text(stringResource(R.string.dialogo_conectar_pin)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// MARK: - DialogoEtiqueta
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoEtiqueta(
    etiquetaActual: String,
    onGuardar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var textoEtiqueta by remember { mutableStateOf(etiquetaActual) }
    val puedeGuardar = textoEtiqueta.trim().length >= 3 || textoEtiqueta.trim().isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dialogo_etiquetar_aula_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.dialogo_cancelar)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { if (puedeGuardar) onGuardar(textoEtiqueta.trim()) },
                        enabled = puedeGuardar
                    ) {
                        Text(stringResource(R.string.dialogo_guardar))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.dialogo_etiquetar_aula_mensaje),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = textoEtiqueta,
                onValueChange = { if (it.length <= 50) textoEtiqueta = it },
                label = { Text(stringResource(R.string.dialogo_etiqueta_aula)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// MARK: - DialogoTiempoEspera
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoTiempoEspera(
    tiempos: List<Int>,
    tiempoActual: Int,
    onGuardar: (Int) -> Unit,
    onCancelar: () -> Unit
) {
    var seleccion by remember { mutableIntStateOf(tiempoActual) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dialogo_establecer_espera_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.dialogo_cancelar)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { onGuardar(seleccion) }) {
                        Text(stringResource(R.string.dialogo_guardar))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            tiempos.forEach { tiempo ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    RadioButton(
                        selected = seleccion == tiempo,
                        onClick = { seleccion = tiempo }
                    )
                    Text(
                        text = "$tiempo",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
