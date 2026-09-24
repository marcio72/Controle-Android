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
    @Json(name = "descricao") val descricao: String? = null,
    @Json(name = "fotoBase64") val fotoBase64: String? = null,
    @Json(name = "temFoto") val temFoto: Boolean? = null
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
    @Json(name = "pecasUsadas")   val pecasUsadas: List<Long> = emptyList(),
    @Json(name = "fotoBase64")    val fotoBase64: String? = null
)

@JsonClass(generateAdapter = true)
data class CategoriaDTO(
    @Json(name = "id")    val id: Long,
    @Json(name = "nome")  val nome: String,
    @Json(name = "alias") val alias: String?
)

@JsonClass(generateAdapter = true)
data class SubCategoriaDTO(
    @Json(name = "id")         val id: Long,
    @Json(name = "nome")       val nome: String,
    @Json(name = "categoria")  val categoria: CategoriaDTO? = null,
    @Json(name = "ativo")      val ativo: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CriarSubCategoriaRequest(
    @Json(name = "nome")       val nome: String,
    @Json(name = "categoria")  val categoria: CategoriaRefDTO
)

// Referência mínima de categoria pra enviar dentro do corpo de criação de subcategoria
@JsonClass(generateAdapter = true)
data class CategoriaRefDTO(
    @Json(name = "id") val id: Long
)

@JsonClass(generateAdapter = true)
data class PecaDTO(
    @Json(name = "idPeca")        val idPeca: Long,
    @Json(name = "codigo")        val codigo: String,
    @Json(name = "status")        val status: String?,
    @Json(name = "dataInstalacao") val dataInstalacao: String? = null,
    @Json(name = "dataRetirada")  val dataRetirada: String? = null,
    @Json(name = "observacao")    val observacao: String? = null,
    @Json(name = "categoriaId")   val categoriaId: Long? = null,
    @Json(name = "categoriaNome") val categoriaNome: String? = null,
    @Json(name = "categoriaAlias") val categoriaAlias: String? = null,
    // Subcategoria da peça (ex: "17 Pol." dentro de Monitores).
    // Opcional: se o backend ainda não enviar esses campos, o app descobre a
    // subcategoria pelo lote da peça (ver AppViewModel.loadPecasDisponiveis).
    @Json(name = "subCategoriaId")   val subCategoriaId: Long? = null,
    @Json(name = "subCategoriaNome") val subCategoriaNome: String? = null,
    @Json(name = "loteId")        val loteId: Long? = null,
    @Json(name = "clienteId")     val clienteId: Long? = null,
    @Json(name = "clienteNome")   val clienteNome: String? = null,
    @Json(name = "maquinaId")     val maquinaId: Long? = null,
    @Json(name = "maquinaNome")   val maquinaNome: String? = null,
    @Json(name = "maquinaJogo")   val maquinaJogo: String? = null,
    @Json(name = "maquinaPlaca")  val maquinaPlaca: String? = null
)

@JsonClass(generateAdapter = true)
data class PecaAcaoRequestDTO(
    @Json(name = "observacao") val observacao: String?,
    @Json(name = "usuarioResponsavel") val usuarioResponsavel: String?
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


@JsonClass(generateAdapter = true)
data class LoteDTO(
    @Json(name = "idLote") val idLote: Long,
    @Json(name = "alias") val alias: String?,
    @Json(name = "fornecedor") val fornecedor: String?,
    @Json(name = "codigo") val codigo: String?,
    @Json(name = "descricao") val descricao: String?,
    @Json(name = "quantidadeComprada") val quantidadeComprada: Int,
    @Json(name = "quantidadeAtual") val quantidadeAtual: Int,
    @Json(name = "dataEntrada") val dataEntrada: String?,
    @Json(name = "categoria") val categoria: CategoriaDTO?,
    @Json(name = "subCategoria") val subCategoria: SubCategoriaDTO? = null
)

@JsonClass(generateAdapter = true)
data class LoteRequestDTO(
    @Json(name = "categoriaId") val categoriaId: Long,
    @Json(name = "subCategoriaId") val subCategoriaId: Long? = null,
    @Json(name = "alias") val alias: String,
    @Json(name = "fornecedor") val fornecedor: String?,
    @Json(name = "codigo") val codigo: String?,
    @Json(name = "descricao") val descricao: String?,
    @Json(name = "quantidadeComprada") val quantidadeComprada: Int,
    @Json(name = "numeroInicial") val numeroInicial: Int,
    @Json(name = "dataEntrada") val dataEntrada: String?
)

@JsonClass(generateAdapter = true)
data class JogoDTO(
    @Json(name = "id") val id: Long,
    @Json(name = "nome") val nome: String?
)

@JsonClass(generateAdapter = true)
data class PecaManualDTO(
    @Json(name = "codigo") val codigo: String,
    @Json(name = "jogoId") val jogoId: Long?
)

@JsonClass(generateAdapter = true)
data class LoteManualRequestDTO(
    @Json(name = "categoriaId") val categoriaId: Long,
    @Json(name = "subCategoriaId") val subCategoriaId: Long? = null,
    @Json(name = "fornecedor") val fornecedor: String,
    @Json(name = "descricao") val descricao: String?,
    @Json(name = "dataEntrada") val dataEntrada: String?,
    @Json(name = "pecas") val pecas: List<PecaManualDTO>
)

@JsonClass(generateAdapter = true)
data class CriarCategoriaRequest(
    @Json(name = "nome") val nome: String,
    @Json(name = "alias") val alias: String
)

@JsonClass(generateAdapter = true)
data class LogEnvioDTO(
    @Json(name = "id")           val id: Long,
    @Json(name = "numeroEnvio")  val numeroEnvio: Long,
    @Json(name = "dataEnvio")    val dataEnvio: String?,
    @Json(name = "nomeCliente")  val nomeCliente: String?,
    @Json(name = "tecnico")      val tecnico: String?,
    @Json(name = "localizacao")  val localizacao: String?
)

@JsonClass(generateAdapter = true)
data class LogEnvioRequestDTO(
    @Json(name = "numeroEnvio")  val numeroEnvio: Long,
    @Json(name = "dataEnvio")    val dataEnvio: String,
    @Json(name = "nomeCliente")  val nomeCliente: String?,
    @Json(name = "tecnico")      val tecnico: String?,
    @Json(name = "localizacao")  val localizacao: String?
)

// ---- TROCA DE SENHA ----

@JsonClass(generateAdapter = true)
data class TrocarSenhaRequestDTO(
    @Json(name = "username")    val username: String,
    @Json(name = "senhaAtual")  val senhaAtual: String,
    @Json(name = "senhaNova")   val senhaNova: String
)

@JsonClass(generateAdapter = true)
data class TrocarSenhaResponseDTO(
    @Json(name = "message") val message: String?
)

// ─────────────────────────────────────────────────────────────
// CONTROLE DE CHAVES
// ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ChaveDTO(
    @Json(name = "id") val id: Long?,
    @Json(name = "codigo") val codigo: String?,
    @Json(name = "numero") val numero: String?,
    @Json(name = "fornecedorId") val fornecedorId: Long?,
    @Json(name = "fornecedorNome") val fornecedorNome: String?,
    @Json(name = "tipo") val tipo: String?,
    @Json(name = "tipoDescricao") val tipoDescricao: String?,
    @Json(name = "quantidadeCopias") val quantidadeCopias: Int?,
    @Json(name = "quantidadeCadeados") val quantidadeCadeados: Int?,
    @Json(name = "ativo") val ativo: Boolean?,
    @Json(name = "observacao") val observacao: String?,
    @Json(name = "criadoEm") val criadoEm: String?
)

@JsonClass(generateAdapter = true)
data class FornecedorChaveDTO(
    @Json(name = "id") val id: Long?,
    @Json(name = "nome") val nome: String?,
    @Json(name = "totalChaves") val totalChaves: Long?
)

@JsonClass(generateAdapter = true)
data class ChaveRequestDTO(
    @Json(name = "numero") val numero: String,
    @Json(name = "fornecedorId") val fornecedorId: Long,
    @Json(name = "tipo") val tipo: String,
    @Json(name = "quantidadeCopias") val quantidadeCopias: Int,
    @Json(name = "quantidadeCadeados") val quantidadeCadeados: Int,
    @Json(name = "observacao") val observacao: String?,
    @Json(name = "ativo") val ativo: Boolean
)

@JsonClass(generateAdapter = true)
data class CriarFornecedorChaveRequest(
    @Json(name = "nome") val nome: String
)

/** Tipos de chave do backend: C = Cofre, T = Tampa, D = Cadeado, O = Outros. */
enum class TipoChaveApp(val codigo: String, val descricao: String) {
    COFRE("C", "Cofre"),
    TAMPA("T", "Tampa"),
    CADEADO("D", "Cadeado"),
    OUTROS("O", "Outros");

    companion object {
        fun porCodigo(codigo: String?): TipoChaveApp? = values().firstOrNull { it.codigo == codigo }
    }
}

/** Resultado de gravação: erro traz a mensagem de regra de negócio devolvida pelo backend. */
data class ResultadoChave(
    val chave: ChaveDTO? = null,
    val fornecedor: FornecedorChaveDTO? = null,
    val erro: String? = null
) {
    val sucesso: Boolean get() = erro == null
}

// ─────────────────────────────────────────────────────────────
// VÍNCULO CHAVE ↔ MÁQUINA
// ─────────────────────────────────────────────────────────────

/** Uso da chave na máquina. COFRE e TAMPA: só 1 por máquina (vincular outra substitui). */
enum class UsoChaveApp(val codigo: String, val descricao: String) {
    COFRE("COFRE", "Cofre"),
    TAMPA("TAMPA", "Tampa"),
    CADEADO("CADEADO", "Cadeado"),
    OUTRO("OUTRO", "Outro");

    companion object {
        fun porCodigo(codigo: String?): UsoChaveApp? = values().firstOrNull { it.codigo == codigo }

        /** Uso padrão pelo tipo da chave (C → Cofre, T → Tampa, D → Cadeado). */
        fun padraoPara(tipoChave: String?): UsoChaveApp = when (tipoChave) {
            "C" -> COFRE
            "T" -> TAMPA
            "D" -> CADEADO
            else -> OUTRO
        }
    }
}

@JsonClass(generateAdapter = true)
data class VinculoChaveDTO(
    @Json(name = "id") val id: Long?,
    @Json(name = "maquinaId") val maquinaId: Long?,
    @Json(name = "maquinaNome") val maquinaNome: String?,
    @Json(name = "maquinaJogo") val maquinaJogo: String?,
    @Json(name = "codCliente") val codCliente: Int?,
    @Json(name = "clienteNome") val clienteNome: String?,
    @Json(name = "praca") val praca: String?,
    @Json(name = "chaveId") val chaveId: Long?,
    @Json(name = "chaveCodigo") val chaveCodigo: String?,
    @Json(name = "fornecedorNome") val fornecedorNome: String?,
    @Json(name = "uso") val uso: String?,
    @Json(name = "usoDescricao") val usoDescricao: String?,
    @Json(name = "ativo") val ativo: Boolean?,
    @Json(name = "vinculadoEm") val vinculadoEm: String?,
    @Json(name = "desvinculadoEm") val desvinculadoEm: String?,
    @Json(name = "observacao") val observacao: String?
)

/** Resultado da busca de máquina por número (o número repete entre praças). */
@JsonClass(generateAdapter = true)
data class MaquinaOpcaoDTO(
    @Json(name = "id") val id: Long?,
    @Json(name = "numero") val numero: String?,
    @Json(name = "jogo") val jogo: String?,
    @Json(name = "codCliente") val codCliente: Int?,
    @Json(name = "clienteNome") val clienteNome: String?,
    @Json(name = "praca") val praca: String?
)

@JsonClass(generateAdapter = true)
data class VinculoChaveRequestDTO(
    @Json(name = "chaveId") val chaveId: Long,
    @Json(name = "uso") val uso: String?,
    @Json(name = "observacao") val observacao: String?
)

@JsonClass(generateAdapter = true)
data class EncerrarVinculoRequest(
    @Json(name = "observacao") val observacao: String?
)

data class ResultadoVinculo(
    val vinculo: VinculoChaveDTO? = null,
    val erro: String? = null
) {
    val sucesso: Boolean get() = erro == null
}
