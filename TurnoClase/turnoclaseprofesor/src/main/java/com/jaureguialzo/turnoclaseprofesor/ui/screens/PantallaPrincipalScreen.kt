// PantallaPrincipalScreen - equivalente a PantallaPrincipal en ContentView.swift de iOS TurnoClaseProfesor
package com.jaureguialzo.turnoclaseprofesor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaureguialzo.turnoclaseprofesor.R
import com.jaureguialzo.turnoclaseprofesor.ui.components.*
import com.jaureguialzo.turnoclaseprofesor.ui.theme.Amarillo
import com.jaureguialzo.turnoclaseprofesor.ui.theme.Azul
import com.jaureguialzo.turnoclaseprofesor.ui.theme.Gris
import com.jaureguialzo.turnoclaseprofesor.ui.theme.Rojo
import com.jaureguialzo.turnoclaseprofesor.viewmodel.AulaViewModel
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun PantallaPrincipalScreen(
    vm: AulaViewModel,
    onMostrarMenu: () -> Unit,
    onMostrarCola: () -> Unit
) {
    val density = LocalDensity.current
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var swipeAcumulado by remember { mutableFloatStateOf(0f) }

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

            // Círculo amarillo principal con gesto de swipe
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
                    .background(Amarillo)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = { swipeAcumulado = 0f },
                            onDragCancel = { swipeAcumulado = 0f }
                        ) { _, dragAmount ->
                            swipeAcumulado += dragAmount
                            if (swipeAcumulado < -80f) { swipeAcumulado = 0f; vm.aulaSiguiente() }
                            else if (swipeAcumulado > 80f) { swipeAcumulado = 0f; vm.aulaAnterior() }
                        }
                    }
            ) {
                Text("👤",
                    fontSize = (tamanyoCirculoDp.value * 0.38f).sp,
                    color = Color.Black.copy(alpha = 0.025f))

                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.size(tamanyoCirculoDp - 32.dp)) {
                    when {
                        vm.cargando -> AnimacionPuntos(color = Color.Black, tamanyo = 10.dp)
                        vm.errorRed -> Text(
                            text = stringResource(R.string.error_no_network),
                            fontSize = 22.sp, textAlign = TextAlign.Center, color = Color.Black,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        else -> Text(
                            text = vm.nombreAlumno,
                            fontSize = 51.sp, textAlign = TextAlign.Center, color = Color.Black,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                        )
                    }
                }
            }

            // Botón código (gris, -60°)
            val (bxN60, byN60) = posEnBorde(-60.0, centroXPx, centroYPx, radioPx)
            BotonCircular(
                titulo = vm.codigoAula,
                colorFondo = Gris, colorTexto = Color.Black, tamanyo = tamanyoBotonDp,
                modifier = Modifier.absoluteOffset { IntOffset(bxN60 - botonMitad, byN60 - botonMitad) },
                onClick = { vm.feedbackTactilLigero(); onMostrarMenu() }
            )

            // Botón cola (rojo, 30°)
            val (bx30, by30) = posEnBorde(30.0, centroXPx, centroYPx, radioPx)
            BotonCircular(
                titulo = "${vm.enCola}",
                colorFondo = Rojo, colorTexto = Color.White, tamanyo = tamanyoBotonDp, fontSize = 28.sp,
                modifier = Modifier.absoluteOffset { IntOffset(bx30 - botonMitad, by30 - botonMitad) },
                onClick = { vm.feedbackTactilLigero(); onMostrarCola() }
            )

            // Botón siguiente/recargar (azul, 150°)
            val (bx150, by150) = posEnBorde(150.0, centroXPx, centroYPx, radioPx)
            if (vm.errorRed) {
                BotonCircularIcono(
                    painter = painterResource(R.drawable.boton_actualizar),
                    colorFondo = Azul, colorIcono = Color.White, tamanyo = tamanyoBotonDp, tamanyoIcono = 72.dp,
                    modifier = Modifier.absoluteOffset { IntOffset(bx150 - botonMitad, by150 - botonMitad) },
                    onClick = { vm.feedbackTactilLigero(); vm.reintentar() }
                )
            } else {
                BotonCircularIcono(
                    painter = painterResource(R.drawable.boton_siguiente),
                    colorFondo = Azul, colorIcono = Color.White, tamanyo = tamanyoBotonDp, tamanyoIcono = 72.dp,
                    modifier = Modifier.absoluteOffset { IntOffset(bx150 - botonMitad, by150 - botonMitad) },
                    onClick = { vm.feedbackTactilLigero(); vm.mostrarSiguiente(avanzarCola = true) }
                )
            }

            // PageControl / indicador (bajo el círculo)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            (centroXPx - with(density) { 120.dp.toPx() }).roundToInt(),
                            (centroYPx + radioPx + with(density) { 16.dp.toPx() }).roundToInt()
                        )
                    }
                    .width(240.dp)
                    .height(26.dp)
            ) {
                when {
                    vm.mostrarIndicador -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    vm.numAulas > 1 && !vm.invitado -> PageControl(
                        currentPage = vm.aulaActual, totalPages = vm.numAulas)
                }
            }
        }
    }
}

@Composable
fun PageControl(currentPage: Int, totalPages: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { i ->
            Box(
                modifier = Modifier.size(7.dp).clip(CircleShape).background(
                    if (i == currentPage) Color.Gray.copy(0.6f) else Color.Gray.copy(0.25f)
                )
            )
        }
    }
}
