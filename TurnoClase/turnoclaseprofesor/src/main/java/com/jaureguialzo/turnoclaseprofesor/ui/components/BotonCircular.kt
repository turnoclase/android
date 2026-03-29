// Componentes de UI - TurnoClaseProfesor
package com.jaureguialzo.turnoclaseprofesor.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

fun posEnBorde(angulo: Double, centroX: Float, centroY: Float, radio: Float): Pair<Int, Int> {
    val rad = Math.toRadians(angulo - 90.0)
    val x = centroX + radio * cos(rad).toFloat()
    val y = centroY + radio * sin(rad).toFloat()
    return Pair(x.roundToInt(), y.roundToInt())
}

@Composable
fun BotonCircular(
    titulo: String,
    colorFondo: Color,
    colorTexto: Color,
    tamanyo: Dp,
    fontSize: TextUnit = 17.sp,
    monoespacio: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var pulsado by remember { mutableStateOf(false) }

    // El círculo se opaca solo cuando el botón está desactivado
    val alphaCirculo by animateFloatAsState(
        targetValue = if (enabled) 1.0f else 0.4f,
        animationSpec = tween(200),
        label = "circulo_alpha"
    )

    // Solo el contenido interior se desvanece al pulsar
    val alphaContenido by animateFloatAsState(
        targetValue = if (pulsado) 0.15f else 1.0f,
        animationSpec = tween(if (pulsado) 100 else 300),
        label = "boton_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tamanyo)
            .graphicsLayer { this.alpha = alphaCirculo }
            .clip(CircleShape)
            .background(colorFondo)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            pulsado = true
                            val released = tryAwaitRelease()
                            pulsado = false
                            if (released) onClick()
                        }
                    )
                }
            }
    ) {
        Text(
            text = titulo,
            color = colorTexto,
            fontSize = fontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontFamily = if (monoespacio) FontFamily.Monospace else null,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .graphicsLayer { this.alpha = alphaContenido }
        )
    }
}

@Composable
fun BotonCircularIcono(
    icono: ImageVector,
    colorFondo: Color,
    colorIcono: Color,
    tamanyo: Dp,
    tamanyoIcono: Dp = 32.dp,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var pulsado by remember { mutableStateOf(false) }

    // El círculo se opaca solo cuando el botón está desactivado
    val alphaCirculo by animateFloatAsState(
        targetValue = if (enabled) 1.0f else 0.4f,
        animationSpec = tween(200),
        label = "circulo_alpha"
    )

    // Solo el icono interior se desvanece al pulsar
    val alphaContenido by animateFloatAsState(
        targetValue = if (pulsado) 0.15f else 1.0f,
        animationSpec = tween(if (pulsado) 100 else 300),
        label = "icono_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(tamanyo)
            .graphicsLayer { this.alpha = alphaCirculo }
            .clip(CircleShape)
            .background(colorFondo)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            pulsado = true
                            val released = tryAwaitRelease()
                            pulsado = false
                            if (released) onClick()
                        }
                    )
                }
            }
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = colorIcono,
            modifier = Modifier
                .size(tamanyoIcono)
                .graphicsLayer { this.alpha = alphaContenido }
        )
    }
}

@Composable
fun AnimacionPuntos(
    color: Color = Color.Black,
    tamanyo: Dp = 14.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "puntos")
    val delays = listOf(0, 180, 360)

    Row(
        horizontalArrangement = Arrangement.spacedBy(tamanyo * 0.8f),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.wrapContentSize()
    ) {
        delays.forEachIndexed { i, delayMs ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = tamanyo.value * 0.9f,
                targetValue = -tamanyo.value * 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(delayMs)
                ),
                label = "puntos_$i"
            )
            Box(
                modifier = Modifier
                    .size(tamanyo)
                    .offset(y = offsetY.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
