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
        @Query("bairro") bairro: String? = null,
        @Query("leiturista") leiturista: Int? = null
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

    @PUT("api/clientes/{id}")
    suspend fun updateCliente(
        @Path("id") id: Long,
        @Body cliente: Cliente
    ): Response<Cliente>

    @PATCH("api/clientes/{id}/desativar")
    suspend fun desativarCliente(
        @Path("id") id: Long
    ): Response<Unit>

    @PUT("maquinas/editar/{id}")
    suspend fun updateMaquina(
        @Path("id") id: Int,
        @Body maquina: Maquina
    ): Response<Maquina>

    @PATCH("api/maquinas/{id}/desativar")
    suspend fun desativarMaquina(
        @Path("id") id: Int
    ): Response<Unit>

    @GET("api/execucoes")
    suspend fun getExecucoes(): List<com.example.data.model.ExecucaoDTO>

    @POST("api/execucoes-com-estoque/registrar")
    suspend fun registrarExecucao(
        @Body execucoes: List<com.example.data.model.ExecucaoRequestDTO>
    ): retrofit2.Response<okhttp3.ResponseBody>

    @GET("api/pecas/disponiveis/{categoriaId}")
    suspend fun getPecasDisponiveis(
        @Path("categoriaId") categoriaId: Long
    ): List<com.example.data.model.PecaDTO>

    @GET("api/categorias")
    suspend fun getCategorias(): List<com.example.data.model.CategoriaDTO>

    // --- SUBCATEGORIAS ---
    @GET("api/subcategorias")
    suspend fun getSubCategorias(
        @Query("categoriaId") categoriaId: Long? = null
    ): List<com.example.data.model.SubCategoriaDTO>

    @POST("api/subcategorias")
    suspend fun criarSubCategoria(
        @Body req: com.example.data.model.CriarSubCategoriaRequest
    ): retrofit2.Response<com.example.data.model.SubCategoriaDTO>

    // --- LOTES ---
    @GET("api/lotes")
    suspend fun getLotes(): List<com.example.data.model.LoteDTO>

    @GET("api/lotes/{id}/pecas")
    suspend fun getPecasDoLote(@Path("id") id: Long): List<com.example.data.model.PecaDTO>

    @GET("api/lotes/{id}/faixa-pecas")
    suspend fun getFaixaPecasDoLote(@Path("id") id: Long): Map<String, String?>

    @POST("api/pecas/{id}/retirar")
    suspend fun retirarPeca(
        @Path("id") id: Long,
        @Body body: com.example.data.model.PecaAcaoRequestDTO
    ): retrofit2.Response<Map<String, String>>

    @POST("api/pecas/{id}/descartar")
    suspend fun descartarPeca(
        @Path("id") id: Long,
        @Body body: com.example.data.model.PecaAcaoRequestDTO
    ): retrofit2.Response<Map<String, String>>

    @GET("api/jogos")
    suspend fun getJogos(): List<com.example.data.model.JogoDTO>

    @POST("api/lotes/manual")
    suspend fun criarLoteManual(
        @Body request: com.example.data.model.LoteManualRequestDTO
    ): retrofit2.Response<com.example.data.model.LoteDTO>

    @POST("api/lotes")
    suspend fun criarLote(@Body lote: com.example.data.model.LoteRequestDTO): retrofit2.Response<com.example.data.model.LoteDTO>

    // --- CATEGORIAS (criar) ---
    @POST("api/categorias")
    suspend fun criarCategoria(@Body cat: com.example.data.model.CriarCategoriaRequest): retrofit2.Response<com.example.data.model.CategoriaDTO>


    // --- LOG DE ENVIO ---
    @POST("api/log-envio")
    suspend fun salvarLogEnvio(@Body log: com.example.data.model.LogEnvioRequestDTO): retrofit2.Response<com.example.data.model.LogEnvioDTO>

    @GET("api/log-envio")
    suspend fun listarLogEnvios(): List<com.example.data.model.LogEnvioDTO>

    // --- CONTROLE DE CHAVES ---
    @GET("api/chaves")
    suspend fun getChaves(
        @Query("numero") numero: String? = null,
        @Query("fornecedorId") fornecedorId: Long? = null,
        @Query("tipo") tipo: String? = null,
        @Query("ativo") ativo: Boolean? = null
    ): List<com.example.data.model.ChaveDTO>

    @POST("api/chaves")
    suspend fun criarChave(
        @Body chave: com.example.data.model.ChaveRequestDTO
    ): retrofit2.Response<com.example.data.model.ChaveDTO>

    @PUT("api/chaves/{id}")
    suspend fun atualizarChave(
        @Path("id") id: Long,
        @Body chave: com.example.data.model.ChaveRequestDTO
    ): retrofit2.Response<com.example.data.model.ChaveDTO>

    @PATCH("api/chaves/{id}/ativo")
    suspend fun alterarAtivoChave(
        @Path("id") id: Long,
        @Query("valor") valor: Boolean
    ): retrofit2.Response<com.example.data.model.ChaveDTO>

    @GET("api/chaves/fornecedores")
    suspend fun getFornecedoresChave(): List<com.example.data.model.FornecedorChaveDTO>

    @POST("api/chaves/fornecedores")
    suspend fun criarFornecedorChave(
        @Body body: com.example.data.model.CriarFornecedorChaveRequest
    ): retrofit2.Response<com.example.data.model.FornecedorChaveDTO>

    // --- VÍNCULO CHAVE ↔ MÁQUINA ---
    @GET("api/chaves/{chaveId}/maquinas")
    suspend fun getMaquinasDaChave(
        @Path("chaveId") chaveId: Long,
        @Query("historico") historico: Boolean
    ): List<com.example.data.model.VinculoChaveDTO>

    @GET("api/chaves/maquinas/buscar")
    suspend fun buscarMaquinasPorNumero(
        @Query("numero") numero: String,
        @Query("praca") praca: String? = null
    ): retrofit2.Response<List<com.example.data.model.MaquinaOpcaoDTO>>

    @GET("api/chaves/pracas")
    suspend fun getPracasChave(): List<String>

    @POST("api/chaves/maquinas/{maquinaId}")
    suspend fun vincularChave(
        @Path("maquinaId") maquinaId: Long,
        @Body body: com.example.data.model.VinculoChaveRequestDTO
    ): retrofit2.Response<com.example.data.model.VinculoChaveDTO>

    @PATCH("api/chaves/vinculos/{id}/encerrar")
    suspend fun encerrarVinculoChave(
        @Path("id") id: Long,
        @Body body: com.example.data.model.EncerrarVinculoRequest
    ): retrofit2.Response<com.example.data.model.VinculoChaveDTO>

    // --- TROCA DE SENHA ---
    @POST("api/usuarios/trocar-senha")
    suspend fun trocarSenha(
        @Body body: com.example.data.model.TrocarSenhaRequestDTO
    ): retrofit2.Response<com.example.data.model.TrocarSenhaResponseDTO>

}