// ListaColaAlumnos - equivalente a ListaColaAlumnos en ContentView.swift de iOS
package com.jaureguialzo.turnoclaseprofesor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.jaureguialzo.turnoclaseprofesor.ui.theme.Rojo
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
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.dialogo_cancelar)
                        )
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
                        val dismissState = rememberSwipeToDismissBoxState()
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                vm.eliminarAlumnoDeCola(alumno)
                            }
                        }
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(end = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.dialogo_ok),
                                        tint = Rojo
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

