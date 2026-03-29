// MenuAccionesAula - equivalente a MenuAccionesAula en ContentView.swift de iOS
package com.jaureguialzo.turnoclaseprofesor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaureguialzo.turnoclaseprofesor.R
import com.jaureguialzo.turnoclaseprofesor.ui.theme.Azul
import com.jaureguialzo.turnoclaseprofesor.ui.theme.Rojo
import com.jaureguialzo.turnoclaseprofesor.viewmodel.AulaViewModel

@Composable
fun MenuAccionesAula(
    vm: AulaViewModel,
    onEtiquetar: () -> Unit,
    onTiempo: () -> Unit,
    onConectar: () -> Unit,
    onBorrar: () -> Unit,
    onAjustes: () -> Unit,
    onCerrar: () -> Unit
) {
    val titulo = if (vm.codigoAula != "?") vm.codigoAula
    else stringResource(R.string.error_no_network)

    val subtitulo = when {
        vm.codigoAula == "?" -> stringResource(R.string.error_no_network)
        vm.invitado -> stringResource(R.string.menu_etiqueta_invitado)
        else -> String.format(stringResource(R.string.menu_etiqueta_pin), vm.PIN)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Cabecera
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp, start = 20.dp, end = 20.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (vm.etiquetaAula.isNotEmpty()) {
                Text(
                    text = "» ${vm.etiquetaAula} «",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider()

        // Acciones
        if (!vm.invitado && vm.codigoAula != "?") {
            ListItem(
                headlineContent = { Text(stringResource(R.string.menu_etiquetar_aula)) },
                leadingContent = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Azul
                    )
                },
                modifier = Modifier.menuClickable {
                    onCerrar()
                    onEtiquetar()
                }
            )
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.menu_establecer_espera) + ": ${vm.tiempoEspera} min")
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = Azul
                    )
                },
                modifier = Modifier.menuClickable {
                    onCerrar()
                    onTiempo()
                }
            )
            if (vm.numAulas < vm.MAX_AULAS) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.menu_accion_anyadir_aula)) },
                    leadingContent = {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = Azul
                        )
                    },
                    modifier = Modifier.menuClickable {
                        onCerrar()
                        vm.anyadirAula()
                    }
                )
            }
            if (vm.numAulas > 1) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.menu_accion_borrar_aula),
                            color = Rojo
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Rojo
                        )
                    },
                    modifier = Modifier.menuClickable {
                        onCerrar()
                        onBorrar()
                    }
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.menu_accion_conectar)) },
                leadingContent = {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = Azul
                    )
                },
                modifier = Modifier.menuClickable {
                    onCerrar()
                    onConectar()
                }
            )
        } else if (vm.invitado) {
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.menu_accion_desconectar),
                        color = Rojo
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        tint = Rojo
                    )
                },
                modifier = Modifier.menuClickable {
                    onCerrar()
                    vm.desconectarAula()
                }
            )
        }

        // Ajustes (siempre disponible)
        ListItem(
            headlineContent = { Text(stringResource(R.string.ajustes_titulo)) },
            leadingContent = {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color.Gray
                )
            },
            modifier = Modifier.menuClickable {
                onCerrar()
                onAjustes()
            }
        )

        Spacer(Modifier.height(16.dp))
    }
}

private fun Modifier.menuClickable(onClick: () -> Unit): Modifier = this.clickable { onClick() }

