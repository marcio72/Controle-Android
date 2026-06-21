package com.example.data.api

import com.example.data.model.Cliente
import com.example.data.model.Maquina
import com.example.data.model.UserLoginRequest
import com.example.data.model.Usuario
import com.example.data.model.SolicitacaoDTO
import com.example.data.model.SolicitacaoResponseDTO
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface SpringApiService {

    @POST("login")
    @FormUrlEncoded
    suspend fun loginForm(
        @Field("username") username: String,
        @Field("senha") senha: String
    ): Response<ResponseBody>

    @POST("api/login")
    suspend fun loginApi(
        @Body request: UserLoginRequest
    ): Response<Usuario>

    @GET("api/solicitacao")
    suspend fun getSolicitacoes(): List<SolicitacaoResponseDTO>

    @GET("api/solicitacao")
    suspend fun getSolicitacoesAbertas(): List<SolicitacaoResponseDTO>

    @GET("solicitacoes_abertas")
    suspend fun getSolicitacoesAbertasPage(): ResponseBody

    @POST("api/solicitacao")
    suspend fun createSolicitacao(
        @Body solicitacao: SolicitacaoDTO
    ): SolicitacaoResponseDTO

    @GET("api/clientes")
    suspend fun getClientes(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("search") search: String? = null,
        @Query("regiao") regiao: Int? = null,
        @Query("ativo") ativo: Boolean? = null,
        @Query("bairro") bairro: String? = null
    ): List<Cliente>

    @GET("api/clientes/page")
    suspend fun getClientesPage(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("search") search: String? = null,
        @Query("regiao") regiao: Int? = null,
        @Query("ativo") ativo: Boolean? = null
    ): com.example.data.model.ClientePageResponse

    @GET("api/maquinas")
    suspend fun getMaquinas(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("search") search: String? = null,
        @Query("ativo") ativo: Boolean? = null,
        @Query("codCliente") codCliente: Int? = null
    ): List<Maquina>

    @GET("api/maquinas/por-cliente/{codCliente}")
    suspend fun getMaquinasPorCliente(
        @Path("codCliente") codCliente: Int
    ): List<Maquina>

    @GET("api/maquinas/page")
    suspend fun getMaquinasPage(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("search") search: String? = null,
        @Query("ativo") ativo: Boolean? = null
    ): com.example.data.model.MaquinaPageResponse

    @GET("api/clientes/{id}")
    suspend fun getClienteById(
        @Path("id") id: Long
    ): Cliente

    @GET("api/maquinas/{id}")
    suspend fun getMaquinaById(
        @Path("id") id: Long
    ): Maquina

    @POST("api/clientes")
    suspend fun createCliente(
        @Body cliente: Cliente
    ): Response<Cliente>

    @POST("api/maquinas")
    suspend fun createMaquina(
        @Body maquina: Maquina
    ): Response<Maquina>
}
