// Modelo de alumno en cola - equivalente a AlumnoCola en AulaViewModel.swift de iOS
package com.jaureguialzo.turnoclaseprofesor.model

data class AlumnoCola(
    val id: String,        // ID del documento en la subcolección "cola"
    val alumnoId: String,  // ID del documento en "alumnos"
    val nombre: String,
    val timestampMs: Long? = null
)

