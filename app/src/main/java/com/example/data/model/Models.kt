package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Cliente(
    @Json(name = "codCliente") val codCliente: Long?,
    @Json(name = "nomCliente") val nomCliente: String?,
    @Json(name = "logradouro") val logradouro: String?,
    @Json(name = "telefone") val telefone: String?,
    @Json(name = "bairro") val bairro: String?,
    @Json(name = "contato") val contato: String?,
    @Json(name = "leiturista") val leiturista: Int?,
    @Json(name = "regiao") val regiao: Int?,
    @Json(name = "dtCadastro") val dtCadastro: String?, // Representing LocalDateTime as ISO-8601 string
    @Json(name = "ativo") val ativo: Boolean?,
    @Json(name = "maquinas") val maquinas: List<Maquina>? = emptyList()
) {
    fun isExcluded(): Boolean {
        val nome = nomCliente?.trim()?.uppercase() ?: ""
        return nome == "OUTROS" || nome == "INSTALACAO" || nome == "INSTALAÇÃO"
    }
}

@JsonClass(generateAdapter = true)
data class Maquina(
    @Json(name = "id") val id: Long?,
    @Json(name = "nom_maq") val nom_maq: String?,
    @Json(name = "nom_jogo") val nom_jogo: String?,
    @Json(name = "numeroPlaca") val numeroPlaca: String?,
    @Json(name = "obs") val obs: String?,
    @Json(name = "codCliente") val codCliente: Int?,
    @Json(name = "ativo") val ativo: Boolean?
) {
    fun isExcluded(): Boolean {
        val jogo = nom_jogo?.trim()?.uppercase() ?: ""
        return jogo == "INFORME" || jogo == "COMUNICADO"
    }
}

@JsonClass(generateAdapter = true)
data class ClientePageResponse(
    val content: List<Cliente>,
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int,
    val last: Boolean
)

@JsonClass(generateAdapter = true)
data class MaquinaPageResponse(
    val content: List<Maquina>,
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int,
    val last: Boolean
)

enum class ConvertRegiao(val code: Int, val description: String) {
    NORTE(1, "Norte"),
    SUL(2, "Sul"),
    LESTE(3, "Leste"),
    OESTE(4, "Oeste"),
    CENTRO(5, "Centro"),
    ABC(6, "ABC");

    companion object {
        fun fromCode(code: Int?): String {
            if (code == null) return "-"
            return entries.find { it.code == code }?.description ?: "Região $code"
        }
    }
}

object ConvertLeiturista {
    fun fromCode(code: Int?): String {
        if (code == null) return "-"
        return "V$code"
    }
}

@JsonClass(generateAdapter = true)
data class UserLoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "senha") val senha: String
)

@JsonClass(generateAdapter = true)
data class Usuario(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "username") val username: String,
    @Json(name = "senha") val senha: String? = null,
    @Json(name = "leiturista") val leiturista: Int? = null,
    @Json(name = "id_pontos") val idPontos: Int? = null,
    @Json(name = "ultimoacesso") val ultimoAcesso: String? = null,
    @Json(name = "telefone") val telefone: String? = null,
    @Json(name = "nome") val nome: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "cargo") val cargo: String? = null,
    @Json(name = "equipe") val equipe: String? = null,
    @Json(name = "unidade") val unidade: String? = null
)

@JsonClass(generateAdapter = true)
data class ProblemaDTO(
    @Json(name = "idProblema") val idProblema: Long? = null,
    @Json(name = "numeroMaquina") val numeroMaquina: Long? = null,
    @Json(name = "maquina") val maquina: String? = null,
    @Json(name = "descricao") val descricao: String? = null
)

@JsonClass(generateAdapter = true)
data class SolicitacaoDTO(
    @Json(name = "cliente") val cliente: Long? = null,
    @Json(name = "dataSolicitacao") val dataSolicitacao: String? = null, // "yyyy-MM-dd'T'HH:mm"
    @Json(name = "problemas") val problemas: List<ProblemaDTO>? = null,
    @Json(name = "nomeTecnico") val nomeTecnico: String? = null
)

@JsonClass(generateAdapter = true)
data class SolicitacaoResponseDTO(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "idProblema") val idProblema: Long? = null,
    @Json(name = "clienteId") val clienteId: Long? = null,
    @Json(name = "cliente") val cliente: String? = null,
    @Json(name = "status") val status: Boolean? = null,
    @Json(name = "problemas") val problemas: List<ProblemaDTO>? = null
)

// ---- EXECUÇÃO DE SOLICITAÇÃO ----

@JsonClass(generateAdapter = true)
data class ExecucaoRequestDTO(
    @Json(name = "problemaId")    val problemaId: Long,
    @Json(name = "solicitacaoId") val solicitacaoId: Long,
    @Json(name = "dataExecucao")  val dataExecucao: String,   // "yyyy-MM-dd'T'HH:mm:ss"
    @Json(name = "tecnico")       val tecnico: String,
    @Json(name = "descricao")     val descricao: String,
    @Json(name = "pecasUsadas")   val pecasUsadas: List<Long> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CategoriaDTO(
    @Json(name = "id")    val id: Long,
    @Json(name = "nome")  val nome: String,
    @Json(name = "alias") val alias: String?
)

@JsonClass(generateAdapter = true)
data class PecaDTO(
    @Json(name = "idPeca")   val idPeca: Long,
    @Json(name = "codigo")   val codigo: String,
    @Json(name = "status")   val status: String?,
    @Json(name = "categoria") val categoria: CategoriaDTO?
)

@JsonClass(generateAdapter = true)
data class ExecucaoDTO(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "nomeCliente") val nomeCliente: String? = null,
    @Json(name = "nomeMaquina") val nomeMaquina: String? = null,
    @Json(name = "descricaoProblema") val descricaoProblema: String? = null,
    @Json(name = "descricao") val descricao: String? = null,
    @Json(name = "observacoes") val observacoes: String? = null,
    @Json(name = "dataExecucao") val dataExecucao: String? = null,
    @Json(name = "valor") val valor: Double? = null,
    @Json(name = "tecnico") val tecnico: String? = null,
    @Json(name = "pdfGerado") val pdfGerado: Boolean? = null
)

