//
//  TurnoClaseProfesor
//  Copyright 2015 Ion Jaureguialzo Sarasola.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

package com.jaureguialzo.turnoclaseprofesor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaureguialzo.turnoclaseprofesor.ui.components.ListaColaAlumnos
import com.jaureguialzo.turnoclaseprofesor.ui.components.MenuAccionesAula
import com.jaureguialzo.turnoclaseprofesor.ui.dialogs.DialogoConexion
import com.jaureguialzo.turnoclaseprofesor.ui.dialogs.DialogoEtiqueta
import com.jaureguialzo.turnoclaseprofesor.ui.dialogs.DialogoTiempoEspera
import com.jaureguialzo.turnoclaseprofesor.ui.screens.AjustesScreen
import com.jaureguialzo.turnoclaseprofesor.ui.screens.PantallaPrincipalScreen
import com.jaureguialzo.turnoclaseprofesor.ui.theme.TurnoClaseProfesorTheme
import com.jaureguialzo.turnoclaseprofesor.viewmodel.AulaViewModel

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TurnoClaseProfesorTheme {
                val vm: AulaViewModel = viewModel()

                LaunchedEffect(Unit) { vm.iniciar() }

                // Estado de diálogos
                var mostrarMenuAcciones by remember { mutableStateOf(false) }
                var mostrarDialogoConexion by remember { mutableStateOf(false) }
                var mostrarDialogoBorrar by remember { mutableStateOf(false) }
                var mostrarDialogoEtiqueta by remember { mutableStateOf(false) }
                var mostrarDialogoTiempo by remember { mutableStateOf(false) }
                var mostrarListaCola by remember { mutableStateOf(false) }
                var mostrarAjustes by remember { mutableStateOf(false) }

                // Pantalla principal
                PantallaPrincipalScreen(
                    vm = vm,
                    onMostrarMenu = { mostrarMenuAcciones = true },
                    onMostrarCola = { mostrarListaCola = true }
                )

                // Alerta de error de conexión
                if (vm.mostrarAlertaErrorConexion) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { vm.mostrarAlertaErrorConexion = false },
                        title = { androidx.compose.material3.Text(getString(R.string.dialogo_error_titulo)) },
                        text = { androidx.compose.material3.Text(getString(R.string.dialogo_error_mensaje)) },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { vm.mostrarAlertaErrorConexion = false }) {
                                androidx.compose.material3.Text(getString(R.string.dialogo_ok))
                            }
                        }
                    )
                }

                // Diálogo confirmar borrado
                if (mostrarDialogoBorrar) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { mostrarDialogoBorrar = false },
                        title = { androidx.compose.material3.Text(getString(R.string.dialogo_confirmar_borrado_titulo)) },
                        text = { androidx.compose.material3.Text(getString(R.string.dialogo_confirmar_borrado_mensaje)) },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                vm.borrarAulaReconectar(vm.codigoAula)
                                mostrarDialogoBorrar = false
                            }) {
                                androidx.compose.material3.Text(getString(R.string.dialogo_ok))
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { mostrarDialogoBorrar = false }) {
                                androidx.compose.material3.Text(getString(R.string.dialogo_cancelar))
                            }
                        }
                    )
                }

                // Menú de acciones (bottom sheet)
                if (mostrarMenuAcciones) {
                    ModalBottomSheet(
                        onDismissRequest = { mostrarMenuAcciones = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                    ) {
                        MenuAccionesAula(
                            vm = vm,
                            onEtiquetar = { mostrarDialogoEtiqueta = true },
                            onTiempo = { mostrarDialogoTiempo = true },
                            onConectar = { mostrarDialogoConexion = true },
                            onBorrar = { mostrarDialogoBorrar = true },
                            onAjustes = { mostrarAjustes = true },
                            onCerrar = { mostrarMenuAcciones = false }
                        )
                    }
                }

                // Diálogo conectar a otra aula (bottom sheet)
                if (mostrarDialogoConexion) {
                    ModalBottomSheet(
                        onDismissRequest = { mostrarDialogoConexion = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ) {
                        DialogoConexion(
                            onConectar = { codigo, pin ->
                                if (vm.codigoAula != codigo) {
                                    vm.buscarAula(codigo = codigo, pin = pin)
                                } else {
                                    vm.mostrarAlertaErrorConexion = true
                                }
                                mostrarDialogoConexion = false
                            },
                            onCancelar = { mostrarDialogoConexion = false }
                        )
                    }
                }

                // Diálogo etiquetar aula (bottom sheet)
                if (mostrarDialogoEtiqueta) {
                    ModalBottomSheet(
                        onDismissRequest = { mostrarDialogoEtiqueta = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ) {
                        DialogoEtiqueta(
                            etiquetaActual = vm.etiquetaAula,
                            onGuardar = { etiqueta ->
                                vm.actualizarEtiqueta(etiqueta)
                                mostrarDialogoEtiqueta = false
                            },
                            onCancelar = { mostrarDialogoEtiqueta = false }
                        )
                    }
                }

                // Diálogo tiempo de espera (bottom sheet)
                if (mostrarDialogoTiempo) {
                    ModalBottomSheet(
                        onDismissRequest = { mostrarDialogoTiempo = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ) {
                        DialogoTiempoEspera(
                            tiempos = vm.tiempos,
                            tiempoActual = vm.tiempoEspera,
                            onGuardar = { tiempo ->
                                vm.actualizarTiempoEspera(tiempo)
                                mostrarDialogoTiempo = false
                            },
                            onCancelar = { mostrarDialogoTiempo = false }
                        )
                    }
                }

                // Lista de alumnos en cola (bottom sheet)
                if (mostrarListaCola) {
                    ModalBottomSheet(
                        onDismissRequest = { mostrarListaCola = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ) {
                        ListaColaAlumnos(
                            vm = vm,
                            onCerrar = { mostrarListaCola = false }
                        )
                    }
                }

                // Ajustes (bottom sheet)
                if (mostrarAjustes) {
                    ModalBottomSheet(
                        onDismissRequest = { mostrarAjustes = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ) {
                        AjustesScreen(
                            vm = vm,
                            onCerrar = { mostrarAjustes = false }
                        )
                    }
                }
            }
        }
    }
}
