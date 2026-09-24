package com.example.util

import android.content.Context
import com.example.data.model.Usuario
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Persiste a sessão do usuário logado em SharedPreferences.
 *
 * Motivo: o Android pode matar o processo do app quando ele vai para segundo
 * plano (por exemplo, ao abrir a câmera do sistema para tirar uma foto). Sem
 * essa persistência, ao voltar da câmera o app recria o AppViewModel do zero
 * com isLoggedIn = false, jogando o usuário de volta pra tela de login mesmo
 * já estando autenticado.
 */
object SessionManager {

    private const val PREFS_NAME = "app_session"
    private const val KEY_USUARIO_JSON = "usuario_json"

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val usuarioAdapter = moshi.adapter(Usuario::class.java)

    fun salvarSessao(context: Context, usuario: Usuario) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USUARIO_JSON, usuarioAdapter.toJson(usuario)).apply()
    }

    fun carregarSessao(context: Context): Usuario? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_USUARIO_JSON, null) ?: return null
        return try {
            usuarioAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun limparSessao(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_USUARIO_JSON).apply()
    }
}
