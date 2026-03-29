// ConexionViewModel - equivalente a ConexionViewModel.swift de iOS
// Gestiona la lógica de conexión al aula y la cola del alumno.
package com.jaureguialzo.turnoclase.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.jaureguialzo.turnoclase.BuildConfig
import com.jaureguialzo.turnoclase.Nombres
import com.jaureguialzo.turnoclase.R
import com.jaureguialzo.turnoclase.model.AulaHistorico
import com.jaureguialzo.turnoclase.model.AulaHistoricoRepo
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.Date

// MARK: - Estado de la pantalla de turno (equivalente a EstadoTurno en iOS)
sealed class EstadoTurno {
    data class EnCola(val posicion: Int) : EstadoTurno()
    object EsTuTurno : EstadoTurno()
    object VolverAEmpezar : EstadoTurno()
    data class Esperando(val segundosRestantes: Int) : EstadoTurno()
    data class Error(val mensaje: String) : EstadoTurno()
}

class ConexionViewModel(application: Application) : AndroidViewModel(application) {

    // MARK: - Estado UI (pantalla inicial)
    var codigoAula by mutableStateOf("")
    var nombreUsuario by mutableStateOf("")
    var placeholder by mutableStateOf(Nombres().aleatorio())
    var historicoAulas by mutableStateOf(listOf<AulaHistorico>())

    // MARK: - Navegación
    var mostrandoTurno by mutableStateOf(false)

    // MARK: - Estado pantalla de turno
    var codigoAulaActual by mutableStateOf("")
    var estadoTurno: EstadoTurno by mutableStateOf(EstadoTurno.EnCola(0))
    var minutosRestantes by mutableIntStateOf(0)
    var segundosRestantes by mutableIntStateOf(0)
    var mostrarCronometro by mutableStateOf(false)
    var mostrarBotonActualizar by mutableStateOf(false)
    var mostrarError by mutableStateOf(false)
    var cargando by mutableStateOf(true)
    var errorRed by mutableStateOf(false)

    // MARK: - Propiedades internas
    private val duracionMinimaCarga = 1000L
    private var inicioCarga = 0L

    private val prefs = application.getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().also {
        it.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
    }
    private val mAuth = FirebaseAuth.getInstance()

    var uid: String? = null
    var refAula: DocumentReference? = null
    var refPosicion: DocumentReference? = null
    private var listenerAula: ListenerRegistration? = null
    private var listenerCola: ListenerRegistration? = null
    private var listenerPosicion: ListenerRegistration? = null

    var pedirTurno = true
    var atendido = false
    var encolando = false
    var ultimaPeticion: Date? = null
    var segundosEspera = 300 // 5 minutos por defecto

    private var timerJob: Job? = null

    // MARK: - Inicialización

    fun iniciar() {
        codigoAula = prefs.getString("codigoAula", "") ?: ""
        nombreUsuario = prefs.getString("nombreUsuario", "") ?: ""
        historicoAulas = AulaHistoricoRepo.cargar(prefs)

        if (BuildConfig.DEBUG && codigoAula.isEmpty()) {
            codigoAula = "BE131"
            nombreUsuario = placeholder
        }
    }

    val puedeConectar: Boolean get() = codigoAula.length == 5 && nombreUsuario.length >= 2

    val nombreEfectivo: String get() = nombreUsuario.ifEmpty { placeholder }

    // MARK: - Conectar al aula

    fun conectar() {
        val codigo = codigoAula.uppercase()
        val nombre = nombreEfectivo

        prefs.edit().apply {
            putString("codigoAula", codigo)
            putString("nombreUsuario", nombre)
        }.apply()

        codigoAulaActual = codigo
        pedirTurno = true
        atendido = false
        encolando = false
        ultimaPeticion = null
        iniciarCarga()
        mostrarError = false
        errorRed = false
        estadoTurno = EstadoTurno.EnCola(0)
        reiniciarCronometro()

        viewModelScope.launch {
            try {
                val result = withTimeout(10_000) {
                    mAuth.signInAnonymously().await()
                }
                uid = result.user?.uid
                Log.d(TAG, "Registrado como usuario con UID: $uid")
                actualizarAlumno(nombre)
                encolarAlumno(codigo)
                mostrandoTurno = true
            } catch (e: Exception) {
                Log.e(TAG, "Error de inicio de sesión: ${e.message}")
                errorRed = true
                estadoTurno = EstadoTurno.Error(
                    getApplication<Application>().getString(R.string.MENSAJE_ERROR_RED)
                )
                mostrandoTurno = true
                actualizarUI()
            }
        }
    }

    // MARK: - Registrar alumno en Firestore

    private fun actualizarAlumno(nombre: String) {
        viewModelScope.launch {
            try {
                db.collection("alumnos").document(uid!!)
                    .set(mapOf("nombre" to nombre), SetOptions.merge()).await()
                Log.d(TAG, "Alumno actualizado")
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar el alumno: ${e.message}")
            }
        }
    }

    // MARK: - Buscar aula y encolar

    fun encolarAlumno(codigo: String) {
        viewModelScope.launch {
            try {
                val querySnapshot = withTimeout(10_000) {
                    db.collectionGroup("aulas")
                        .whereEqualTo("codigo", codigo)
                        .limit(1)
                        .get(Source.SERVER).await()
                }
                if (querySnapshot.documents.isNotEmpty()) {
                    Log.d(TAG, "Conectado a aula existente")
                    errorRed = false
                    conectarListenerAula(querySnapshot.documents[0])
                } else {
                    Log.e(TAG, "Aula no encontrada")
                    errorRed = false
                    estadoTurno = EstadoTurno.Error(
                        getApplication<Application>().getString(R.string.MENSAJE_ERROR)
                    )
                    actualizarUI()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al recuperar datos: ${e.message}")
                errorRed = true
                estadoTurno = EstadoTurno.Error(
                    getApplication<Application>().getString(R.string.MENSAJE_ERROR_RED)
                )
                actualizarUI()
            }
        }
    }

    // MARK: - Listeners

    private fun conectarListenerAula(document: DocumentSnapshot) {
        if (listenerAula != null) return
        listenerAula = document.reference.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists() &&
                snapshot.data?.get("codigo") as? String == codigoAulaActual
            ) {
                refAula = snapshot.reference
                segundosEspera = ((snapshot.data?.get("espera") as? Long ?: 5) * 60).toInt()
                errorRed = false
                historicoAulas = AulaHistoricoRepo.registrarConexion(codigoAulaActual, prefs)
                conectarListenerCola()
            } else {
                Log.d(TAG, "El aula ha desaparecido")
                desconectarListeners()
                abandonarCola()
            }
        }
    }

    private fun conectarListenerCola() {
        if (listenerCola != null || refAula == null) return
        listenerCola = refAula!!.collection("cola").addSnapshotListener { _, error ->
            if (error != null) {
                Log.e(TAG, "Error al recuperar datos: ${error.message}")
            } else {
                buscarAlumnoEnCola()
            }
        }
    }

    private fun conectarListenerPosicion(refPos: DocumentReference) {
        if (listenerPosicion != null) return
        listenerPosicion = refPos.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && !snapshot.exists()) {
                atendido = true
                Log.d(TAG, "Nos han borrado de la cola")
            }
        }
    }

    // MARK: - Lógica de cola

    private fun buscarAlumnoEnCola() {
        viewModelScope.launch {
            try {
                val resultados = refAula!!.collection("cola")
                    .whereEqualTo("alumno", uid!!)
                    .limit(1)
                    .get(Source.SERVER).await()
                procesarCola(resultados)
            } catch (e: Exception) {
                Log.e(TAG, "Error al recuperar datos: ${e.message}")
                errorRed = true
                estadoTurno = EstadoTurno.Error(
                    getApplication<Application>().getString(R.string.MENSAJE_ERROR_RED)
                )
                actualizarUI()
            }
        }
    }

    private fun procesarCola(querySnapshot: QuerySnapshot) {
        val docs = querySnapshot.documents

        if (docs.isNotEmpty()) {
            Log.d(TAG, "Alumno encontrado, ya está en la cola")
            pedirTurno = false
            refPosicion = docs[0].reference
            conectarListenerPosicion(docs[0].reference)
            actualizarPantalla()

        } else if (pedirTurno || !atendido) {
            if (encolando) return
            pedirTurno = false
            encolando = true
            Log.d(TAG, "Alumno no encontrado, lo añadimos")

            viewModelScope.launch {
                recuperarUltimaPeticion()
                if (tiempoEsperaRestante() > 0) {
                    encolando = false
                    estadoTurno = EstadoTurno.Esperando(tiempoEsperaRestante())
                    mostrarCronometro = true
                    mostrarBotonActualizar = false
                    mostrarError = false
                    iniciarCronometro()
                    actualizarUI()
                } else {
                    mostrarCronometro = false
                    mostrarBotonActualizar = false
                    mostrarError = false
                    reiniciarCronometro()
                    borrarUltimaPeticion()
                    try {
                        val ref = refAula!!.collection("cola").add(
                            mapOf(
                                "alumno" to uid!!,
                                "timestamp" to FieldValue.serverTimestamp()
                            )
                        ).await()
                        refPosicion = ref
                        desconectarListenerPosicion()
                        conectarListenerPosicion(ref)
                        actualizarPantalla()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al añadir el documento: ${e.message}")
                    }
                    encolando = false
                }
            }

        } else {
            Log.d(TAG, "La cola se ha vaciado tras ser atendido")
            viewModelScope.launch {
                recuperarUltimaPeticion()
                if (segundosEspera > 0 && tiempoEsperaRestante() > 0) {
                    estadoTurno = EstadoTurno.Esperando(tiempoEsperaRestante())
                    mostrarCronometro = true
                    mostrarBotonActualizar = false
                    mostrarError = false
                    iniciarCronometro()
                    actualizarUI()
                } else {
                    Log.d(TAG, "Mostrando botón para volver a pedir turno")
                    reiniciarCronometro()
                    borrarUltimaPeticion()
                    estadoTurno = EstadoTurno.VolverAEmpezar
                    mostrarCronometro = false
                    mostrarBotonActualizar = true
                    mostrarError = false
                    terminarCarga()
                }
            }
        }
    }

    private fun actualizarPantalla() {
        if (refAula == null || refPosicion == null) {
            estadoTurno = EstadoTurno.Error(
                getApplication<Application>().getString(R.string.MENSAJE_ERROR)
            )
            actualizarUI()
            return
        }
        viewModelScope.launch {
            try {
                val posDoc = refPosicion!!.get(Source.SERVER).await()
                val ts = posDoc.data?.get("timestamp") ?: return@launch
                val querySnapshot = refAula!!.collection("cola")
                    .whereLessThanOrEqualTo("timestamp", ts)
                    .get(Source.SERVER).await()
                val posicion = querySnapshot.documents.size
                Log.d(TAG, "Posición en la cola: $posicion")
                when {
                    posicion > 1 -> estadoTurno = EstadoTurno.EnCola(posicion - 1)
                    posicion == 1 -> estadoTurno = EstadoTurno.EsTuTurno
                }
                terminarCarga()
                actualizarUI()
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar pantalla: ${e.message}")
                terminarCarga()
            }
        }
    }

    private fun actualizarUI() {
        terminarCarga()
        when (estadoTurno) {
            is EstadoTurno.Esperando -> {
                mostrarCronometro = true
                mostrarBotonActualizar = false
                mostrarError = false
            }
            is EstadoTurno.Error -> {
                mostrarCronometro = false
                mostrarBotonActualizar = false
                mostrarError = true
            }
            else -> {
                mostrarCronometro = false
                mostrarBotonActualizar = false
                mostrarError = false
            }
        }
    }

    // MARK: - Botones de la pantalla de turno

    fun reintentar() {
        Log.d(TAG, "Reintentando conexión...")
        mostrarError = false
        iniciarCarga()
        desconectarListeners()

        val codigo = codigoAulaActual
        val nombre = nombreEfectivo

        if (uid != null) {
            encolarAlumno(codigo)
            return
        }

        viewModelScope.launch {
            try {
                val result = mAuth.signInAnonymously().await()
                uid = result.user?.uid
                actualizarAlumno(nombre)
                encolarAlumno(codigo)
            } catch (e: Exception) {
                Log.e(TAG, "Error al reintentar sign-in: ${e.message}")
                terminarCarga()
            }
        }
    }

    fun cancelar() {
        Log.d(TAG, "Cancelando...")
        reiniciarCronometro()
        desconectarListeners()
        abandonarCola()
    }

    fun actualizar() {
        if (atendido) {
            Log.d(TAG, "Pidiendo nuevo turno")
            iniciarCarga()
            desconectarListeners()
            atendido = false
            pedirTurno = true
            encolando = false
            encolarAlumno(codigoAulaActual)
        } else {
            Log.d(TAG, "Ya tenemos turno")
        }
    }

    private fun abandonarCola() {
        mostrandoTurno = false
        val posRef = refPosicion ?: return
        viewModelScope.launch {
            try {
                posRef.delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "Error al borrar de la cola: ${e.message}")
            }
        }
    }

    fun desconectarListeners() {
        listenerAula?.remove(); listenerAula = null
        listenerCola?.remove(); listenerCola = null
        listenerPosicion?.remove(); listenerPosicion = null
    }

    private fun desconectarListenerPosicion() {
        listenerPosicion?.remove()
        listenerPosicion = null
    }

    // MARK: - Cronómetro

    fun iniciarCronometro() {
        if (timerJob != null) return
        tickCronometro()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                tickCronometro()
            }
        }
    }

    fun reiniciarCronometro() {
        timerJob?.cancel()
        timerJob = null
        minutosRestantes = 0
        segundosRestantes = 0
        mostrarCronometro = false
    }

    private fun tiempoEsperaRestante(): Int {
        val ultima = ultimaPeticion ?: return -1
        val elapsedSeconds = maxOf(0L, (Date().time - ultima.time) / 1000)
        return (segundosEspera - elapsedSeconds).toInt()
    }

    private fun tickCronometro() {
        val restante = tiempoEsperaRestante()
        if (restante >= 0) {
            minutosRestantes = restante / 60
            segundosRestantes = restante % 60
        } else {
            reiniciarCronometro()
            borrarUltimaPeticion()
            estadoTurno = EstadoTurno.VolverAEmpezar
            mostrarCronometro = false
            mostrarBotonActualizar = true
            mostrarError = false
            atendido = true
        }
    }

    // MARK: - Última petición (tiempo de espera)

    private suspend fun recuperarUltimaPeticion() {
        if (uid == null || refAula == null) return
        try {
            val doc = refAula!!.collection("espera").document(uid!!).get().await()
            if (doc.exists()) {
                val ts = doc.get("timestamp") as? Timestamp
                ultimaPeticion = ts?.toDate()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al recuperar última petición: ${e.message}")
        }
    }

    private fun borrarUltimaPeticion() {
        ultimaPeticion = null
        if (uid == null || refAula == null) return
        viewModelScope.launch {
            try {
                refAula!!.collection("espera").document(uid!!).delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "Error al borrar última petición: ${e.message}")
            }
        }
    }

    // MARK: - Duración mínima de carga

    fun iniciarCarga() {
        inicioCarga = System.currentTimeMillis()
        cargando = true
    }

    fun terminarCarga() {
        val transcurrido = System.currentTimeMillis() - inicioCarga
        val restante = duracionMinimaCarga - transcurrido
        if (restante > 0) {
            viewModelScope.launch {
                delay(restante)
                cargando = false
            }
        } else {
            cargando = false
        }
    }

    // MARK: - Histórico de aulas

    fun actualizarEtiquetaHistorico(id: String, etiqueta: String) {
        historicoAulas = AulaHistoricoRepo.actualizarEtiqueta(id, etiqueta, prefs)
    }

    fun eliminarDeHistorico(id: String) {
        historicoAulas = AulaHistoricoRepo.eliminar(id, prefs)
    }

    override fun onCleared() {
        super.onCleared()
        desconectarListeners()
        timerJob?.cancel()
    }

    companion object {
        private const val TAG = "ConexionViewModel"
    }
}

