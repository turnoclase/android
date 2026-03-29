// TurnoScreen - equivalente a TurnoView.swift de iOS TurnoClase
package com.jaureguialzo.turnoclase.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaureguialzo.turnoclase.R
import com.jaureguialzo.turnoclase.ui.components.*
import com.jaureguialzo.turnoclase.ui.theme.Amarillo
import com.jaureguialzo.turnoclase.ui.theme.Azul
import com.jaureguialzo.turnoclase.ui.theme.Gris
import com.jaureguialzo.turnoclase.ui.theme.Rojo
import com.jaureguialzo.turnoclase.viewmodel.ConexionViewModel
import com.jaureguialzo.turnoclase.viewmodel.EstadoTurno
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun TurnoScreen(vm: ConexionViewModel) {
    val density = LocalDensity.current
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .onSizeChanged { containerSize = it }
    ) {
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
            val botonMitad = (tamanyoBotonPx / 2).roundToInt()

            // Círculo gris principal
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(tamanyoCirculoDp)
                    .absoluteOffset {
                        IntOffset(
                            (centroXPx - radioPx).roundToInt(),
                            (centroYPx - radioPx).roundToInt()
                        )
                    }
                    .clip(CircleShape)
                    .background(Gris)
            ) {
                Text("👤",
                    fontSize = (tamanyoCirculoDp.value * 0.38f).sp,
                    color = Color.Black.copy(alpha = 0.025f))

                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.size(tamanyoCirculoDp - 32.dp)
                ) {
                    when {
                        vm.cargando -> AnimacionPuntos(color = Color.Black, tamanyo = 10.dp)
                        vm.mostrarError -> Text(
                            text = mensajeCentro(vm),
                            fontSize = 22.sp, textAlign = TextAlign.Center, color = Color.Black,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        else -> Text(
                            text = mensajeEstado(vm),
                            fontSize = mensajeFontSize(vm).sp,
                            textAlign = TextAlign.Center, color = Color.Black,
                            modifier = Modifier.padding(horizontal = 20.dp),
                            maxLines = 2,
                            lineHeight = (mensajeFontSize(vm) * 1.1f).sp
                        )
                    }
                }
            }

            // Botón código de aula (amarillo, 30°)
            val (bx30, by30) = posEnBorde(30.0, centroXPx, centroYPx, radioPx)
            BotonCircular(
                titulo = vm.codigoAulaActual,
                colorFondo = Amarillo, colorTexto = Color.Black, tamanyo = tamanyoBotonDp,
                modifier = Modifier.absoluteOffset { IntOffset(bx30 - botonMitad, by30 - botonMitad) },
                onClick = {}
            )

            // Botón cancelar (rojo, -60°)
            val (bxN60, byN60) = posEnBorde(-60.0, centroXPx, centroYPx, radioPx)
            BotonCircularIcono(
                icono = Icons.Default.Close,
                colorFondo = Rojo, colorIcono = Color.White, tamanyo = tamanyoBotonDp, tamanyoIcono = 30.dp,
                modifier = Modifier.absoluteOffset { IntOffset(bxN60 - botonMitad, byN60 - botonMitad) },
                onClick = { vm.cancelar() }
            )

            // Botón actualizar/cronómetro/recargar (azul, 150°)
            val (bx150, by150) = posEnBorde(150.0, centroXPx, centroYPx, radioPx)
            when {
                vm.mostrarCronometro -> BotonCircular(
                    titulo = "%02d:%02d".format(vm.minutosRestantes, vm.segundosRestantes),
                    colorFondo = Azul, colorTexto = Color.White, tamanyo = tamanyoBotonDp,
                    fontSize = 15.sp, monoespacio = true,
                    modifier = Modifier.absoluteOffset { IntOffset(bx150 - botonMitad, by150 - botonMitad) },
                    onClick = {}
                )
                vm.errorRed -> BotonCircularIcono(
                    icono = Icons.Default.Refresh,
                    colorFondo = Azul, colorIcono = Color.White, tamanyo = tamanyoBotonDp,
                    modifier = Modifier.absoluteOffset { IntOffset(bx150 - botonMitad, by150 - botonMitad) },
                    onClick = { vm.reintentar() }
                )
                else -> BotonCircularIcono(
                    icono = Icons.Default.Refresh,
                    colorFondo = Azul, colorIcono = Color.White, tamanyo = tamanyoBotonDp,
                    enabled = vm.mostrarBotonActualizar,
                    modifier = Modifier
                        .absoluteOffset { IntOffset(bx150 - botonMitad, by150 - botonMitad) }
                        .alpha(if (vm.mostrarBotonActualizar) 1f else 0f),
                    onClick = { vm.actualizar() }
                )
            }
        }
    }
}

@Composable
private fun mensajeCentro(vm: ConexionViewModel): String =
    if (vm.errorRed) stringResource(R.string.MENSAJE_ERROR_RED)
    else stringResource(R.string.MENSAJE_ERROR)

@Composable
private fun mensajeEstado(vm: ConexionViewModel): String = when (val e = vm.estadoTurno) {
    is EstadoTurno.EnCola -> "${e.posicion}"
    is EstadoTurno.EsTuTurno -> stringResource(R.string.ES_TU_TURNO)
    is EstadoTurno.VolverAEmpezar -> stringResource(R.string.VOLVER_A_EMPEZAR)
    is EstadoTurno.Esperando -> stringResource(R.string.ESPERA)
    is EstadoTurno.Error -> e.mensaje
}

private fun mensajeFontSize(vm: ConexionViewModel): Float =
    if (vm.estadoTurno is EstadoTurno.EnCola) 72f else 28f
