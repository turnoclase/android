// Modelo de histórico de aulas - equivalente a AulaHistorico.swift de iOS
package com.jaureguialzo.turnoclase.model

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AulaHistorico(
    val id: String = UUID.randomUUID().toString(),
    val codigo: String,
    val etiqueta: String = ""
)

object AulaHistoricoRepo {
    private const val CLAVE = "historicoAulas"

    fun cargar(prefs: SharedPreferences): List<AulaHistorico> {
        val json = prefs.getString(CLAVE, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                AulaHistorico(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    codigo = obj.getString("codigo"),
                    etiqueta = obj.optString("etiqueta", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun guardar(lista: List<AulaHistorico>, prefs: SharedPreferences) {
        val array = JSONArray()
        lista.forEach { aula ->
            val obj = JSONObject()
            obj.put("id", aula.id)
            obj.put("codigo", aula.codigo)
            obj.put("etiqueta", aula.etiqueta)
            array.put(obj)
        }
        prefs.edit().putString(CLAVE, array.toString()).apply()
    }

    fun registrarConexion(codigo: String, prefs: SharedPreferences): List<AulaHistorico> {
        val lista = cargar(prefs).toMutableList()
        val idx = lista.indexOfFirst { it.codigo == codigo }
        if (idx >= 0) {
            val existente = lista.removeAt(idx)
            lista.add(0, existente)
        } else {
            lista.add(0, AulaHistorico(codigo = codigo))
        }
        guardar(lista, prefs)
        return lista.toList()
    }

    fun actualizarEtiqueta(
        id: String,
        etiqueta: String,
        prefs: SharedPreferences
    ): List<AulaHistorico> {
        val lista = cargar(prefs).toMutableList()
        val idx = lista.indexOfFirst { it.id == id }
        if (idx >= 0) {
            lista[idx] = lista[idx].copy(etiqueta = etiqueta)
            guardar(lista, prefs)
        }
        return lista.toList()
    }

    fun eliminar(id: String, prefs: SharedPreferences): List<AulaHistorico> {
        val lista = cargar(prefs).toMutableList()
        lista.removeAll { it.id == id }
        guardar(lista, prefs)
        return lista.toList()
    }
}

