// HistoricoAulasScreen - equivalente a HistoricoAulasView.swift de iOS
package com.jaureguialzo.turnoclase.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaureguialzo.turnoclase.R
import com.jaureguialzo.turnoclase.model.AulaHistorico
import com.jaureguialzo.turnoclase.ui.theme.Gris

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoAulasScreen(
    historico: List<AulaHistorico>,
    onSeleccionar: (AulaHistorico) -> Unit,
    onEtiquetarActualizar: (String, String) -> Unit,
    onEliminar: (String) -> Unit,
    onCerrar: () -> Unit
) {
    var aulaParaEtiquetar by remember { mutableStateOf<AulaHistorico?>(null) }
    var textoEtiqueta by remember { mutableStateOf("") }
    var mostrarDialogoEtiqueta by remember { mutableStateOf(false) }

    if (mostrarDialogoEtiqueta && aulaParaEtiquetar != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEtiqueta = false },
            title = { Text(stringResource(R.string.historico_etiqueta_titulo)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.historico_etiqueta_mensaje),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textoEtiqueta,
                        onValueChange = { if (it.length <= 20) textoEtiqueta = it },
                        label = { Text(stringResource(R.string.historico_etiqueta_placeholder)) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    aulaParaEtiquetar?.let { onEtiquetarActualizar(it.id, textoEtiqueta.trim()) }
                    mostrarDialogoEtiqueta = false
                }) {
                    Text(stringResource(R.string.guardar))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEtiqueta = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.historico_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onCerrar) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cerrar)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (historico.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🕓", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.historico_vacio),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                itemsIndexed(historico, key = { _, a -> a.id }) { _, aula ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                onEliminar(aula.id)
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.error)
                                    .padding(end = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.eliminar),
                                    tint = Color.White
                                )
                            }
                        },
                        enableDismissFromStartToEnd = false
                    ) {
                        FilaAulaHistorico(
                            aula = aula,
                            onSeleccionar = {
                                onSeleccionar(aula)
                                onCerrar()
                            },
                            onEtiquetar = {
                                aulaParaEtiquetar = aula
                                textoEtiqueta = aula.etiqueta
                                mostrarDialogoEtiqueta = true
                            }
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun FilaAulaHistorico(
    aula: AulaHistorico,
    onSeleccionar: () -> Unit,
    onEtiquetar: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Código de aula en cápsula gris (equivalente a iOS)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Gris)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .pointerInput(Unit) { detectTapGestures { onSeleccionar() } }
        ) {
            Text(
                text = aula.codigo,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Etiqueta (tappable para editar)
        Box(
            modifier = Modifier
                .weight(1f)
                .pointerInput(Unit) { detectTapGestures { onEtiquetar() } }
        ) {
            if (aula.etiqueta.isNotEmpty()) {
                Text(text = aula.etiqueta, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    text = stringResource(R.string.historico_sin_etiqueta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

