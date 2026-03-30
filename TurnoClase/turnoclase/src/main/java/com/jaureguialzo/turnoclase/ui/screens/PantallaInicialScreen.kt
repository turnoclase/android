// PantallaInicialScreen - equivalente a ContentView.swift (pantallaInicial) de iOS TurnoClase
package com.jaureguialzo.turnoclase.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaureguialzo.turnoclase.R
import com.jaureguialzo.turnoclase.ui.components.BotonCircularIcono
import com.jaureguialzo.turnoclase.ui.components.posEnBorde
import com.jaureguialzo.turnoclase.ui.theme.Azul
import com.jaureguialzo.turnoclase.ui.theme.Gris
import com.jaureguialzo.turnoclase.viewmodel.ConexionViewModel
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInicialScreen(
    vm: ConexionViewModel, colorRelleno: Color = Color.White
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var mostrarHistorico by remember { mutableStateOf(false) }

    if (mostrarHistorico) {
        ModalBottomSheet(
            onDismissRequest = { mostrarHistorico = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            HistoricoAulasScreen(
                historico = vm.historicoAulas,
                onSeleccionar = { aula ->
                    vm.codigoAula = aula.codigo
                    mostrarHistorico = false
                },
                onEtiquetarActualizar = { id, etiqueta ->
                    vm.actualizarEtiquetaHistorico(
                        id, etiqueta
                    )
                },
                onEliminar = { id -> vm.eliminarDeHistorico(id) },
                onCerrar = { mostrarHistorico = false })
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged { containerSize = it }) {
        if (containerSize.width > 0) {
            val w = containerSize.width
            val h = containerSize.height
            val tamanyoCirculoPx = min(w, h) * 0.70f
            val tamanyoBotonPx = with(density) { 72.dp.toPx() }
            val centroXPx = w / 2f + with(density) { 8.dp.toPx() }
            val centroYPx = h / 2f - with(density) { 12.dp.toPx() }
            val radioPx = tamanyoCirculoPx / 2f
            val tamanyoCirculoDp = with(density) { tamanyoCirculoPx.toDp() }
            val tamanyoBotonDp = 72.dp

            // Círculo gris principal
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(tamanyoCirculoDp)
                    .absoluteOffset {
                        IntOffset(
                            (centroXPx - radioPx).roundToInt(), (centroYPx - radioPx).roundToInt()
                        )
                    }
                    .clip(CircleShape)
                    .background(Gris)) {
                // Símbolo persona de fondo (tenue)
                Icon(
                    painter = painterResource(R.drawable.persona),
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.025f),
                    modifier = Modifier.size(tamanyoCirculoDp * 0.60f)
                )

                // Campos de texto en el centro
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .widthIn(max = tamanyoCirculoDp - 56.dp)
                ) {
                    Text(
                        text = stringResource(R.string.etiqueta_aula).uppercase(),
                        fontSize = 16.sp,
                        letterSpacing = 1.sp,
                        color = Color.Black
                    )

                    Box {
                        BasicTextField(
                            value = vm.codigoAula,
                            onValueChange = { vm.codigoAula = it.uppercase().take(5) },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 20.sp, textAlign = TextAlign.Center, color = Color.Black
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .height(36.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                                .background(colorRelleno),
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp)
                                ) {
                                    if (vm.codigoAula.isEmpty()) {
                                        Text(
                                            "BE131",
                                            color = Color.Gray.copy(0.8f),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            fontSize = 20.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            })
                        if (vm.historicoAulas.isNotEmpty()) {
                            IconButton(
                                onClick = { focusManager.clearFocus(); mostrarHistorico = true },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 4.dp)
                                    .size(36.dp)
                            ) {
                                Icon(Icons.Default.FilterList, null, tint = Color.Gray.copy(0.7f))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.etiqueta_nombre_usuario).uppercase(),
                        fontSize = 16.sp,
                        letterSpacing = 1.sp,
                        color = Color.Black
                    )

                    BasicTextField(
                        value = vm.nombreUsuario,
                        onValueChange = { vm.nombreUsuario = it.take(15) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 20.sp, textAlign = TextAlign.Center, color = Color.Black
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = {
                            focusManager.clearFocus()
                            if (vm.puedeConectar) vm.conectar()
                        }),
                        modifier = Modifier
                            .height(36.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(colorRelleno),
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp)
                            ) {
                                if (vm.nombreUsuario.isEmpty()) {
                                    Text(
                                        vm.placeholder,
                                        color = Color.Gray.copy(0.8f),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        fontSize = 20.sp
                                    )
                                }
                                innerTextField()
                            }
                        })
                }
            }

            // Botón siguiente (azul, 150°)
            val (bx150, by150) = posEnBorde(150.0, centroXPx, centroYPx, radioPx)
            val botonMitad = (tamanyoBotonPx / 2).roundToInt()
            BotonCircularIcono(
                painter = painterResource(R.drawable.boton_siguiente),
                colorFondo = Azul,
                colorIcono = Color.White,
                tamanyo = tamanyoBotonDp,
                tamanyoIcono = 72.dp,
                enabled = vm.puedeConectar,
                modifier = Modifier.absoluteOffset {
                    IntOffset(
                        bx150 - botonMitad,
                        by150 - botonMitad
                    )
                },
                onClick = { focusManager.clearFocus(); vm.conectar() })
        }
    }
}
