// AulaViewModel - equivalente a AulaViewModel.swift de iOS TurnoClaseProfesor
package com.jaureguialzo.turnoclaseprofesor.viewmodel

import android.app.Application
import android.content.Context
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.BuildConfig
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import com.jaureguialzo.turnoclaseprofesor.model.AlumnoCola
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.Random

class AulaViewModel(application: Application) : AndroidViewModel(application) {

    // MARK: - Estado publicado
    var codigoAula by mutableStateOf("...")
    var enCola by mutableIntStateOf(0)
    var nombreAlumno by mutableStateOf("")
    var alumnosEnCola by mutableStateOf(listOf<AlumnoCola>())
    var numAulas by mutableIntStateOf(0)
    var aulaActual by mutableIntStateOf(0)
    var mostrarIndicador by mutableStateOf(false)
    var invitado by mutableStateOf(false)
    var PIN by mutableStateOf("...")
    var etiquetaAula by mutableStateOf("")
    var tiempoEspera by mutableIntStateOf(5)
    var cargando by mutableStateOf(true)
    var errorRed by mutableStateOf(false)

    // Alerta de error de conexión para los diálogos
    var mostrarAlertaErrorConexion by mutableStateOf(false)

    val MAX_AULAS = 16
    val tiempos = listOf(0, 1, 2, 3, 5, 10, 15, 20, 30, 45, 60)

    // MARK: - Propiedades internas
    private val duracionMinimaCarga = 1000L
    private var inicioCarga = 0L
    private var recuentoAnterior = 0
    private var avanzandoCola = false

    private val prefs = application.getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance().also {
        it.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
    }
    private val mAuth = FirebaseAuth.getInstance()
    private val functions = FirebaseFunctions.getInstance("europe-west1")

    var uid: String? = null
    var refAula: DocumentReference? = null
    var refMisAulas: CollectionReference? = null
    private var listenerAula: ListenerRegistration? = null
    private var listenerCola: ListenerRegistration? = null

    // Red
    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            viewModelScope.launch(Dispatchers.Main) {
                if (uid != null && errorRed) {
                    errorRed = false
                    desconectarListeners()
                    conectarAula()
                }
                Log.d(TAG, "Red disponible")
            }
        }

        override fun onLost(network: Network) {
            viewModelScope.launch(Dispatchers.Main) {
                terminarCarga()
                errorRed = true
                actualizarAulaUI(codigo = "?", enColaVal = 0)
                nombreAlumno = ""
                invitado = false
                aulaActual = 0
                mostrarIndicador = false
                desconectarListeners()
                Log.e(TAG, "Red no disponible")
            }
        }
    }

    // MARK: - Inicialización

    fun iniciar() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        Log.d(TAG, "Iniciando la aplicación...")

        if (BuildConfig.DEBUG && isRunningTest()) {
            actualizarAulaUI(codigo = "BE131", enColaVal = 0)
            nombreAlumno = ""
            PIN = "1234"
            numAulas = 2
        } else {
            actualizarAulaUI(codigo = "...", enColaVal = 0)
            nombreAlumno = ""
            iniciarCarga()
            errorRed = false
            mostrarIndicador = false
            numAulas = 0

            mAuth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        viewModelScope.launch(Dispatchers.Main) {
                            uid = mAuth.currentUser?.uid
                            Log.d(TAG, "Registrado como usuario con UID: $uid")

                            val uidAnterior = prefs.getString("uidAnterior", "") ?: ""
                            if (uidAnterior.isNotEmpty() && uidAnterior != uid) {
                                uid = uidAnterior
                                Log.d(TAG, "Ya estaba registrado con UID: $uidAnterior")
                            } else {
                                prefs.edit().putString("uidAnterior", uid).apply()
                            }

                            val codigoAulaConectada =
                                prefs.getString("codigoAulaConectada", "") ?: ""
                            val pinConectada = prefs.getString("pinConectada", "") ?: ""

                            if (codigoAulaConectada.isNotEmpty() && pinConectada.isNotEmpty()) {
                                buscarAula(codigo = codigoAulaConectada, pin = pinConectada)
                            } else {
                                conectarAula()
                            }
                        }
                    } else {
                        viewModelScope.launch(Dispatchers.Main) {
                            Log.e(TAG, "Error de inicio de sesión: ${task.exception?.message}")
                            terminarCarga()
                            errorRed = true
                            actualizarAulaUI(codigo = "?", enColaVal = 0)
                        }
                    }
                }
        }
    }

    // MARK: - Conectar aula

    fun conectarAula(posicion: Int = 0) {
        val uidActual = uid ?: return
        iniciarCarga()
        refMisAulas = db.collection("profesores").document(uidActual).collection("aulas")

        viewModelScope.launch {
            try {
                val querySnapshot = withTimeout(10_000) {
                    refMisAulas!!.orderBy("timestamp").get(Source.SERVER).await()
                }
                val total = querySnapshot.documents.size
                numAulas = total

                if (posicion in 0 until total) {
                    val seleccionada = querySnapshot.documents[posicion]
                    Log.d(TAG, "Conectado a aula existente")
                    refAula = seleccionada.reference
                    conectarListener()
                } else {
                    Log.d(TAG, "Creando nueva aula...")
                    crearAula()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al recuperar la lista de aulas: ${e.message}")
                terminarCarga()
                errorRed = true
                actualizarAulaUI(codigo = "?", enColaVal = 0)
            }
        }
    }

    // MARK: - Crear / añadir aula

    fun crearAula() {
        mostrarIndicador = true
        viewModelScope.launch {
            try {
                val ref = crearNuevaAula()
                refAula = ref
                conectarListener()
            } catch (e: Exception) {
                Log.e(TAG, "Error al crear el aula: ${e.message}")
                terminarCarga()
                errorRed = true
                actualizarAulaUI(codigo = "?", enColaVal = 0)
            }
        }
    }

    fun anyadirAula() {
        mostrarIndicador = true
        desconectarListeners()
        viewModelScope.launch {
            try {
                val ref = crearNuevaAula()
                refAula = ref
                aulaActual = numAulas - 1
                conectarListener()
            } catch (e: Exception) {
                Log.e(TAG, "Error al crear el aula: ${e.message}")
                mostrarIndicador = false
            }
        }
    }

    private suspend fun crearNuevaAula(): DocumentReference? {
        val result =
            functions.getHttpsCallable("nuevoCodigo").call(mapOf("keepalive" to false)).await()

        @Suppress("UNCHECKED_CAST")
        val codigo = (result.data as? Map<String, Any>)?.get("codigo") as? String ?: return null
        Log.d(TAG, "Nuevo código de aula: $codigo")

        val datos = mapOf(
            "codigo" to codigo,
            "timestamp" to FieldValue.serverTimestamp(),
            "pin" to "%04d".format(Random().nextInt(10000)),
            "espera" to 5
        )
        val ref = refMisAulas!!.add(datos).await()
        Log.d(TAG, "Aula creada")
        numAulas += 1
        return ref
    }

    // MARK: - Listeners

    fun conectarListener() {
        if (listenerAula != null || refAula == null) return

        listenerAula = refAula!!.addSnapshotListener { documentSnapshot, error ->
            viewModelScope.launch(Dispatchers.Main) {
                if (error != null) {
                    Log.e(TAG, "Error en listener de aula: ${error.message}")
                    terminarCarga()
                    errorRed = true
                    actualizarAulaUI(codigo = "?", enColaVal = 0)
                    return@launch
                }

                if (documentSnapshot?.exists() == true) {
                    val aula = documentSnapshot.data ?: return@launch
                    Log.d(TAG, "Actualizando datos del aula...")
                    terminarCarga()
                    errorRed = false
                    actualizarAulaUI(codigo = aula["codigo"] as? String ?: "?")
                    PIN = aula["pin"] as? String ?: "?"
                    tiempoEspera = (aula["espera"] as? Long)?.toInt() ?: 5
                    etiquetaAula = aula["etiqueta"] as? String ?: ""
                    mostrarIndicador = false

                    if (listenerCola == null) {
                        listenerCola = refAula!!.collection("cola")
                            .addSnapshotListener { querySnapshot, colaError ->
                                viewModelScope.launch(Dispatchers.Main) {
                                    if (colaError != null) {
                                        Log.e(TAG, "Error al recuperar datos: ${colaError.message}")
                                        terminarCarga()
                                        errorRed = true
                                    } else if (querySnapshot != null) {
                                        errorRed = false
                                        val docs = querySnapshot.documents.sortedBy {
                                            (it.data?.get("timestamp") as? Timestamp)?.seconds ?: 0
                                        }
                                        actualizarContador(docs.size)
                                        actualizarListaAlumnosEnCola(docs)
                                        if (!avanzandoCola) {
                                            mostrarSiguienteDesdeSnapshot(docs)
                                            feedbackTactilNotificacion()
                                        }
                                    }
                                }
                            }
                    }
                } else {
                    Log.d(TAG, "El aula ha desaparecido")
                    if (!invitado) {
                        actualizarAulaUI(codigo = "?", enColaVal = 0)
                        PIN = "?"
                        desconectarListeners()
                        iniciarCarga()
                        conectarAula()
                    } else {
                        desconectarAula()
                    }
                }
            }
        }
    }

    fun desconectarListeners() {
        listenerAula?.remove(); listenerAula = null
        listenerCola?.remove(); listenerCola = null
    }

    private fun mostrarSiguienteDesdeSnapshot(docs: List<DocumentSnapshot>) {
        val primerDoc = docs.firstOrNull() ?: run {
            nombreAlumno = ""
            return
        }
        val alumnoId = primerDoc.data?.get("alumno") as? String ?: run {
            nombreAlumno = ""
            return
        }
        viewModelScope.launch {
            try {
                val alumnoDoc = db.collection("alumnos").document(alumnoId).get().await()
                if (alumnoDoc.exists()) {
                    nombreAlumno = alumnoDoc.data?.get("nombre") as? String ?: "?"
                } else {
                    nombreAlumno = "?"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener nombre de alumno: ${e.message}")
                nombreAlumno = "?"
            }
        }
    }

    private fun actualizarListaAlumnosEnCola(docs: List<DocumentSnapshot>) {
        viewModelScope.launch {
            val lista = mutableListOf<AlumnoCola>()
            for (doc in docs) {
                val alumnoId = doc.data?.get("alumno") as? String ?: continue
                val ts = (doc.data?.get("timestamp") as? Timestamp)?.toDate()?.time
                var nombre = "?"
                try {
                    val alumnoDoc = db.collection("alumnos").document(alumnoId).get().await()
                    if (alumnoDoc.exists()) {
                        nombre = alumnoDoc.data?.get("nombre") as? String ?: "?"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al obtener nombre de alumno: ${e.message}")
                }
                lista.add(
                    AlumnoCola(
                        id = doc.id,
                        alumnoId = alumnoId,
                        nombre = nombre,
                        timestampMs = ts
                    )
                )
            }
            alumnosEnCola = lista
        }
    }

    fun eliminarAlumnoDeCola(alumno: AlumnoCola) {
        viewModelScope.launch {
            try {
                refAula?.collection("cola")?.document(alumno.id)?.delete()?.await()
                Log.d(TAG, "Alumno ${alumno.nombre} eliminado de la cola")
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar alumno de la cola: ${e.message}")
            }
        }
    }

    fun vaciarCola() {
        viewModelScope.launch {
            try {
                val querySnapshot = refAula?.collection("cola")?.get()?.await()
                querySnapshot?.documents?.forEach { doc ->
                    doc.reference.delete().await()
                }
                Log.d(TAG, "Cola vaciada completamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al vaciar la cola: ${e.message}")
            }
        }
    }

    // MARK: - Mostrar siguiente

    fun mostrarSiguiente(avanzarCola: Boolean = false) {
        Log.d(TAG, "Mostrando el siguiente alumno...")
        val refAulaLocal = refAula ?: return
        if (avanzarCola) avanzandoCola = true

        viewModelScope.launch {
            try {
                val querySnapshot = refAulaLocal.collection("cola")
                    .orderBy("timestamp")
                    .get(Source.SERVER).await()
                val docs = querySnapshot.documents
                if (docs.isEmpty()) {
                    Log.d(TAG, "Cola vacía")
                    nombreAlumno = if (codigoAula != "?") "" else
                        getApplication<Application>().getString(com.jaureguialzo.turnoclaseprofesor.R.string.error_no_network)
                    avanzandoCola = false
                    return@launch
                }

                val refPosicion = docs[0].reference
                val posicionDoc = refPosicion.get(Source.SERVER).await()
                val alumnoId = posicionDoc.data?.get("alumno") as? String ?: run {
                    avanzandoCola = false
                    return@launch
                }

                val alumnoDoc =
                    db.collection("alumnos").document(alumnoId).get(Source.SERVER).await()
                if (alumnoDoc.exists()) {
                    if (avanzarCola) {
                        refAulaLocal.collection("espera").document(alumnoId)
                            .set(mapOf("timestamp" to FieldValue.serverTimestamp())).await()
                        refPosicion.delete().await()
                        avanzandoCola = false
                        // Mostrar el siguiente
                        if (docs.size > 1) {
                            val siguienteId = docs[1].data?.get("alumno") as? String
                            if (siguienteId != null) {
                                val sigDoc = db.collection("alumnos").document(siguienteId)
                                    .get(Source.SERVER).await()
                                nombreAlumno =
                                    if (sigDoc.exists()) sigDoc.data?.get("nombre") as? String
                                        ?: "?" else ""
                            } else {
                                nombreAlumno = ""
                            }
                        } else {
                            nombreAlumno = ""
                        }
                    } else {
                        nombreAlumno = alumnoDoc.data?.get("nombre") as? String ?: "?"
                    }
                } else {
                    Log.e(TAG, "El alumno no existe")
                    nombreAlumno = "?"
                    avanzandoCola = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al recuperar datos: ${e.message}")
                avanzandoCola = false
            }
        }
    }

    // MARK: - Buscar aula (modo invitado)

    fun buscarAula(codigo: String, pin: String) {
        Log.d(TAG, "Buscando aula: $codigo:$pin")
        iniciarCarga()
        errorRed = false

        viewModelScope.launch {
            try {
                val querySnapshot = withTimeout(10_000) {
                    db.collectionGroup("aulas")
                        .whereEqualTo("codigo", codigo.uppercase())
                        .whereEqualTo("pin", pin)
                        .get(Source.SERVER).await()
                }
                if (querySnapshot.documents.isNotEmpty()) {
                    Log.d(TAG, "Aula encontrada: $codigo")
                    prefs.edit().apply {
                        putString("codigoAulaConectada", codigo)
                        putString("pinConectada", pin)
                    }.apply()
                    desconectarListeners()
                    invitado = true
                    refAula = querySnapshot.documents.first().reference
                    conectarListener()
                } else {
                    Log.e(TAG, "Aula no encontrada")
                    terminarCarga()
                    if (prefs.getString("codigoAulaConectada", null) == null) {
                        mostrarAlertaErrorConexion = true
                    }
                    desconectarAula()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al recuperar datos: ${e.message}")
                terminarCarga()
                errorRed = true
                actualizarAulaUI(codigo = "?", enColaVal = 0)
            }
        }
    }

    fun desconectarAula() {
        prefs.edit().apply {
            remove("codigoAulaConectada")
            remove("pinConectada")
        }.apply()
        invitado = false
        desconectarListeners()
        conectarAula(posicion = aulaActual)
    }

    // MARK: - Reintentar conexión

    fun reintentar() {
        desconectarListeners()
        iniciarCarga()

        if (uid != null) {
            val codigoAulaConectada = prefs.getString("codigoAulaConectada", "") ?: ""
            val pinConectada = prefs.getString("pinConectada", "") ?: ""
            if (codigoAulaConectada.isNotEmpty() && pinConectada.isNotEmpty()) {
                buscarAula(codigo = codigoAulaConectada, pin = pinConectada)
            } else {
                conectarAula(posicion = aulaActual)
            }
            return
        }

        mAuth.signInAnonymously().addOnCompleteListener { task ->
            viewModelScope.launch(Dispatchers.Main) {
                if (task.isSuccessful) {
                    uid = mAuth.currentUser?.uid
                    val uidAnterior = prefs.getString("uidAnterior", "") ?: ""
                    if (uidAnterior.isNotEmpty() && uidAnterior != uid) {
                        uid = uidAnterior
                    } else {
                        prefs.edit().putString("uidAnterior", uid).apply()
                    }
                    val codigoAulaConectada = prefs.getString("codigoAulaConectada", "") ?: ""
                    val pinConectada = prefs.getString("pinConectada", "") ?: ""
                    if (codigoAulaConectada.isNotEmpty() && pinConectada.isNotEmpty()) {
                        buscarAula(codigo = codigoAulaConectada, pin = pinConectada)
                    } else {
                        conectarAula(posicion = aulaActual)
                    }
                } else {
                    Log.e(TAG, "Error al reintentar sign-in: ${task.exception?.message}")
                    terminarCarga()
                    actualizarAulaUI(codigo = "?", enColaVal = 0)
                }
            }
        }
    }

    // MARK: - Borrar aula

    fun borrarAulaReconectar(codigo: String) {
        mostrarIndicador = true
        desconectarListeners()
        viewModelScope.launch {
            try {
                val querySnapshot = refMisAulas
                    ?.whereEqualTo("codigo", codigo.uppercase())
                    ?.get(Source.SERVER)?.await()
                querySnapshot?.documents?.firstOrNull()?.reference?.delete()?.await()
                Log.d(TAG, "Aula borrada")
                numAulas -= 1
                if (aulaActual == numAulas) {
                    aulaActual = maxOf(0, aulaActual - 1)
                }
                conectarAula(posicion = aulaActual)
            } catch (e: Exception) {
                Log.e(TAG, "Error al borrar el aula: ${e.message}")
                mostrarIndicador = false
            }
        }
    }

    // MARK: - Actualizar etiqueta

    fun actualizarEtiqueta(nuevaEtiqueta: String) {
        etiquetaAula = nuevaEtiqueta.trim()
        viewModelScope.launch {
            try {
                refAula?.update("etiqueta", etiquetaAula)?.await()
                Log.d(TAG, "Aula actualizada")
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar el aula: ${e.message}")
            }
        }
    }

    // MARK: - Actualizar tiempo de espera

    fun actualizarTiempoEspera(tiempo: Int) {
        tiempoEspera = tiempo
        Log.d(TAG, "Establecer tiempo de espera en $tiempo minutos...")
        viewModelScope.launch {
            try {
                refAula?.update("espera", tiempo)?.await()
                Log.d(TAG, "Aula actualizada")
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar el aula: ${e.message}")
            }
        }
    }

    // MARK: - Navegación entre aulas

    fun aulaAnterior() {
        if (!invitado && numAulas > 1 && aulaActual > 0) {
            aulaActual -= 1
            Log.d(TAG, "Aula anterior: $aulaActual")
            desconectarListeners()
            conectarAula(posicion = aulaActual)
        }
    }

    fun aulaSiguiente() {
        if (!invitado && numAulas > 1 && aulaActual < numAulas - 1) {
            aulaActual += 1
            Log.d(TAG, "Aula siguiente: $aulaActual")
            desconectarListeners()
            conectarAula(posicion = aulaActual)
        }
    }

    // MARK: - Ajustes de sonido

    fun sonidoActivado(): Boolean {
        return prefs.getBoolean("sonido", true)
    }

    fun setSonidoActivado(activado: Boolean) {
        prefs.edit().putBoolean("sonido", activado).apply()
    }

    // MARK: - Helpers privados

    private fun actualizarAulaUI(codigo: String) {
        codigoAula = codigo
        Log.d(TAG, "Código de aula: $codigo")
    }

    private fun actualizarAulaUI(codigo: String, enColaVal: Int) {
        actualizarAulaUI(codigo)
        actualizarContador(enColaVal)
    }

    private fun actualizarContador(recuento: Int) {
        if (sonidoActivado() && recuentoAnterior == 0 && recuento == 1) {
            try {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(getApplication(), notification)
                r.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        recuentoAnterior = recuento
        enCola = recuento
        Log.d(TAG, "Alumnos en cola: $recuento")
    }

    private fun feedbackTactilNotificacion() {
        // Solo si cambia el número de alumnos
    }

    fun feedbackTactilLigero() {
        try {
            @Suppress("DEPRECATION")
            val vibrator = getApplication<Application>().getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasVibrator() == true) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        50,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            }
        } catch (_: Exception) {
            // Ignorar si no hay vibrador
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
                mostrarIndicador = false
            }
        } else {
            cargando = false
            mostrarIndicador = false
        }
    }

    private fun isRunningTest(): Boolean {
        return try {
            Class.forName("androidx.test.espresso.Espresso")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        desconectarListeners()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignorar
        }
    }

    companion object {
        private const val TAG = "AulaViewModel"
    }
}

