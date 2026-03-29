// ListaColaAlumnos - equivalente a ListaColaAlumnos en ContentView.swift de iOS
package com.jaureguialzo.turnoclaseprofesor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaureguialzo.turnoclaseprofesor.R
import com.jaureguialzo.turnoclaseprofesor.model.AlumnoCola
import com.jaureguialzo.turnoclaseprofesor.ui.theme.Azul
import com.jaureguialzo.turnoclaseprofesor.ui.theme.Gris
import com.jaureguialzo.turnoclaseprofesor.viewmodel.AulaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaColaAlumnos(
    vm: AulaViewModel,
    onCerrar: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cola_espera_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.dialogo_cancelar))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (vm.alumnosEnCola.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🚫", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.cola_vacia),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Encabezado con código y etiqueta
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = vm.codigoAula,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (vm.etiquetaAula.isNotEmpty()) {
                        Text(
                            text = vm.etiquetaAula,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(vm.alumnosEnCola, key = { _, a -> a.id }) { index, alumno ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    vm.eliminarAlumnoDeCola(alumno)
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
                                        contentDescription = stringResource(R.string.dialogo_ok),
                                        tint = Color.White
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false
                        ) {
                            FilaAlumnoCola(index = index, alumno = alumno)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaAlumnoCola(index: Int, alumno: AlumnoCola) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Número de posición en círculo
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (index == 0) Azul else Gris)
        ) {
            Text(
                text = "${index + 1}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (index == 0) Color.White else Color.Black
            )
        }
        Text(
            text = alumno.nombre,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

