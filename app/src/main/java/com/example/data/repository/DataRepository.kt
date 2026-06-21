package com.example.data.repository

import android.util.Log
import com.example.data.api.SpringApiService
import com.example.data.model.Cliente
import com.example.data.model.Maquina
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class DataRepository {

    private val _baseUrl = MutableStateFlow("https://maquinas72.azurewebsites.net/")
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private var apiService: SpringApiService? = null
    private val cookieJar = SimpleCookieJar()

    // Generous high-quality mock datasets for robust demo testing
    private val mockClientes = mutableListOf<Cliente>()
    private val mockMaquinas = mutableListOf<Maquina>()
    private val localSolicitacoes = mutableListOf<com.example.data.model.SolicitacaoResponseDTO>()

    init {
        if (_isDemoMode.value) {
            generateMockData()
        }
        rebuildRetrofit()
    }

    fun setDemoMode(demo: Boolean) {
        _isDemoMode.value = demo
        if (demo && mockClientes.isEmpty()) {
            generateMockData()
        } else if (!demo) {
            mockClientes.clear()
            mockMaquinas.clear()
        }
    }

    fun updateBaseUrl(newUrl: String) {
        var cleanUrl = newUrl.trim()
        if (!cleanUrl.endsWith("/")) {
            cleanUrl += "/"
        }
        if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            _baseUrl.value = cleanUrl
            rebuildRetrofit()
        }
    }

    private fun rebuildRetrofit() {
        try {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .cookieJar(cookieJar)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(_baseUrl.value)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            apiService = retrofit.create(SpringApiService::class.java)
        } catch (e: Exception) {
            Log.e("DataRepository", "Failed to build Retrofit: ${e.message}")
            apiService = null
        }
    }

    private fun generateMockData() {
        val bairros = listOf(
            "Centro", "Jardim Paulista", "Copacabana", "Vila Mariana", "Pinheiros",
            "Ipanema", "Barra da Tijuca", "Savassi", "Batel", "Moinhos de Vento",
            "Moema", "Botafogo", "Leblon", "Perdizes", "Santana", "Consolação"
        )

        val contatos = listOf(
            "Carlos Silva", "Ana Oliveira", "Marcos Souza", "Juliana Santos",
            "Roberto Costa", "Patricia Lima", "Felipe Almeida", "Beatriz Rocha",
            "Renato Carvalho", "Mariana Pereira"
        )

        val nomMaqPrefixes = listOf("Slot", "Vending Coffee", "Fliperama", "Fliperama Arcade", "Mesa Bilhar", "Dardo Eletrônico")
        val nomJogos = listOf("Double Diamond", "Nespresso Pro", "Pac-Man Retro", "Street Fighter II", "Classic Pool", "Darts Masters", "Golden Fruits", "Wild West Slot", "Pinball Space")

        // 1. Generate 80 clients
        for (i in 1..80) {
            val clientPhone = "(11) 9${Random.nextInt(8000, 9999)}-${Random.nextInt(1000, 9999)}"
            val clientBairro = bairros[Random.nextInt(bairros.size)]
            val clientContato = contatos[Random.nextInt(contatos.size)]
            val clientName = when (Random.nextInt(4)) {
                0 -> "Bar e Lanchonete ${bairros[Random.nextInt(bairros.size)]} - $i"
                1 -> "Supermercado Pão de Ouro Unit $i"
                2 -> "Hotel Imperial ($clientContato)"
                else -> "Shopping Entretenimento Ltda - $i"
            }
            
            // Random datetime in the last year
            val year = 2025
            val month = Random.nextInt(1, 13)
            val day = Random.nextInt(1, 28)
            val dateStr = String.format("%04d-%02d-%02dT10:30:00", year, month, day)

            val cliente = Cliente(
                codCliente = i.toLong(),
                nomCliente = clientName,
                logradouro = "Av. Principal, nº ${i * 15}",
                telefone = clientPhone,
                bairro = clientBairro,
                contato = clientContato,
                leiturista = Random.nextInt(1, 8),
                regiao = Random.nextInt(1, 7), // 1 to 6 mapped regions
                dtCadastro = dateStr,
                ativo = Random.nextDouble() > 0.15 // 85% active
            )
            mockClientes.add(cliente)
        }

        // 2. Generate 150 machines associated with random clients
        for (i in 1..150) {
            val clientOwnerId = Random.nextInt(1, 81)
            val machineAtive = Random.nextDouble() > 0.1 // 90% active
            val placa = "${listOf("SP", "RJ", "MG", "PR")[Random.nextInt(4)]}-${Random.nextInt(10000, 99999)}"
            val mName = nomMaqPrefixes[Random.nextInt(nomMaqPrefixes.size)] + " Model " + ('A' + Random.nextInt(26))
            val mJogo = nomJogos[Random.nextInt(nomJogos.size)]
            
            val maquina = Maquina(
                id = i.toLong(),
                nom_maq = mName,
                nom_jogo = mJogo,
                numeroPlaca = placa,
                obs = if (Random.nextBoolean()) "Manuntenção preventiva ok" else "Checkin realizado dia ${Random.nextInt(1, 30)}",
                codCliente = clientOwnerId,
                ativo = machineAtive
            )
            mockMaquinas.add(maquina)
        }

        // Link machines back to clients
        for (idx in mockClientes.indices) {
            val c = mockClientes[idx]
            val clientMachines = mockMaquinas.filter { it.codCliente == c.codCliente?.toInt() }
            mockClientes[idx] = c.copy(maquinas = clientMachines)
        }
    }

    // Paginated, filtered, searched Client retrievals
    suspend fun getClientes(
        page: Int,
        size: Int,
        search: String?,
        regiao: Int?,
        ativo: Boolean?,
        bairro: String? = null
    ): List<Cliente> {
        if (_isDemoMode.value) {
            var filtered = mockClientes.asSequence().filter { !it.isExcluded() }
            
            if (!search.isNullOrBlank()) {
                filtered = filtered.filter {
                    it.nomCliente?.contains(search, ignoreCase = true) == true ||
                    it.logradouro?.contains(search, ignoreCase = true) == true ||
                    it.contato?.contains(search, ignoreCase = true) == true ||
                    it.bairro?.contains(search, ignoreCase = true) == true
                }
            }
            if (regiao != null) {
                filtered = filtered.filter { it.regiao == regiao }
            }
            if (ativo != null) {
                filtered = filtered.filter { it.ativo == ativo }
            }
            if (!bairro.isNullOrBlank()) {
                filtered = filtered.filter { it.bairro?.equals(bairro, ignoreCase = true) == true }
            }

            // Paginate
            val list = filtered.toList()
            val startIdx = page * size
            if (startIdx >= list.size) return emptyList()
            val endIdx = (startIdx + size).coerceAtMost(list.size)
            return list.subList(startIdx, endIdx)
        } else {
            // Live Retrofit mode with local sync and smart filtering
            return try {
                // Fetch the list from the Spring endpoint
                val rawApiList = try {
                    val resp = apiService?.getClientesPage(page, size, search, regiao, ativo)
                    resp?.content
                } catch (e: Exception) {
                    null
                } ?: apiService?.getClientes(page, size, search, regiao, ativo, bairro) ?: emptyList()

                // Fetch machine details for each client since Spring API might not populate the relationship out of the box
                val apiList = rawApiList.map { apiCli ->
                    val cod = apiCli.codCliente
                    val clientMachines = if (cod != null) {
                        try {
                            apiService?.getMaquinasPorCliente(cod.toInt()) ?: emptyList()
                        } catch (e: Exception) {
                            Log.e("DataRepository", "Error fetching machines for client $cod: ${e.message}")
                            apiCli.maquinas ?: emptyList()
                        }
                    } else {
                        apiCli.maquinas ?: emptyList()
                    }
                    apiCli.copy(maquinas = clientMachines)
                }

                // sync memory cache with latest retrieved API data
                if (apiList.isNotEmpty()) {
                    if (page == 0) {
                        mockClientes.clear()
                        mockMaquinas.clear()
                    }
                    // Update main client list cache with live results (preserving other clients if this is a partial list)
                    apiList.forEach { apiCli ->
                        val existingIdx = mockClientes.indexOfFirst { it.codCliente == apiCli.codCliente }
                        if (existingIdx != -1) {
                            mockClientes[existingIdx] = apiCli
                        } else {
                            mockClientes.add(apiCli)
                        }

                        // Also extract its machines to synchronize locally
                        apiCli.maquinas?.forEach { maq ->
                            val existingMaqIdx = mockMaquinas.indexOfFirst { it.id == maq.id }
                            val maqWithCod = if (maq.codCliente == null && apiCli.codCliente != null) {
                                maq.copy(codCliente = apiCli.codCliente.toInt())
                            } else {
                                maq
                            }
                            if (existingMaqIdx != -1) {
                                mockMaquinas[existingMaqIdx] = maqWithCod
                            } else {
                                mockMaquinas.add(maqWithCod)
                            }
                        }
                    }
                }

                // Apply professional client-side filtering over the fetched API list 
                // in case the server-side controller doesn't process query params.
                var filtered = apiList.asSequence().filter { !it.isExcluded() }
                if (!search.isNullOrBlank()) {
                    filtered = filtered.filter {
                        it.nomCliente?.contains(search, ignoreCase = true) == true ||
                        it.logradouro?.contains(search, ignoreCase = true) == true ||
                        it.contato?.contains(search, ignoreCase = true) == true ||
                        it.bairro?.contains(search, ignoreCase = true) == true
                    }
                }
                if (regiao != null) {
                    filtered = filtered.filter { it.regiao == regiao }
                }
                if (ativo != null) {
                    filtered = filtered.filter { it.ativo == ativo }
                }
                if (!bairro.isNullOrBlank()) {
                    filtered = filtered.filter { it.bairro?.equals(bairro, ignoreCase = true) == true }
                }

                val list = filtered.toList()
                val startIdx = page * size
                if (startIdx >= list.size) {
                    emptyList()
                } else {
                    val endIdx = (startIdx + size).coerceAtMost(list.size)
                    list.subList(startIdx, endIdx)
                }
            } catch (e: Exception) {
                Log.e("DataRepository", "Error getting live clients: ${e.message}. Falling back to cached data.")
                getClientesDemoFallback(page, size, search, regiao, ativo, bairro)
            }
        }
    }

    private fun getClientesDemoFallback(
        page: Int,
        size: Int,
        search: String?,
        regiao: Int?,
        ativo: Boolean?,
        bairro: String?
    ): List<Cliente> {
        var filtered = mockClientes.asSequence().filter { !it.isExcluded() }
        if (!search.isNullOrBlank()) {
            filtered = filtered.filter {
                it.nomCliente?.contains(search, ignoreCase = true) == true ||
                it.bairro?.contains(search, ignoreCase = true) == true
            }
        }
        if (regiao != null) filtered = filtered.filter { it.regiao == regiao }
        if (ativo != null) filtered = filtered.filter { it.ativo == ativo }
        if (!bairro.isNullOrBlank()) filtered = filtered.filter { it.bairro == bairro }

        val list = filtered.toList()
        val startIdx = page * size
        if (startIdx >= list.size) return emptyList()
        val endIdx = (startIdx + size).coerceAtMost(list.size)
        return list.subList(startIdx, endIdx)
    }

    suspend fun getMaquinas(
        page: Int,
        size: Int,
        search: String?,
        ativo: Boolean?,
        codCliente: Int? = null
    ): List<Maquina> {
        if (_isDemoMode.value) {
            var filtered = mockMaquinas.asSequence().filter { !it.isExcluded() && !isClientExcluded(it.codCliente) }

            if (!search.isNullOrBlank()) {
                filtered = filtered.filter {
                    it.nom_maq?.contains(search, ignoreCase = true) == true ||
                    it.nom_jogo?.contains(search, ignoreCase = true) == true ||
                    it.numeroPlaca?.contains(search, ignoreCase = true) == true ||
                    it.obs?.contains(search, ignoreCase = true) == true
                }
            }
            if (ativo != null) {
                filtered = filtered.filter { it.ativo == ativo }
            }
            if (codCliente != null) {
                filtered = filtered.filter { it.codCliente == codCliente }
            }

            val list = filtered.toList()
            val startIdx = page * size
            if (startIdx >= list.size) return emptyList()
            val endIdx = (startIdx + size).coerceAtMost(list.size)
            return list.subList(startIdx, endIdx)
        } else {
            // Live Retrofit mode: fetch live machines or populate them from live clients
            try {
                if (codCliente != null) {
                    val list = apiService?.getMaquinasPorCliente(codCliente) ?: emptyList()
                    list.forEach { m ->
                        val existingIdx = mockMaquinas.indexOfFirst { it.id == m.id }
                        val mWithCod = if (m.codCliente == null) m.copy(codCliente = codCliente) else m
                        if (existingIdx != -1) {
                            mockMaquinas[existingIdx] = mWithCod
                        } else {
                            mockMaquinas.add(mWithCod)
                        }
                    }
                } else {
                    // Try fetching all machines directly from api/maquinas if endpoint exists
                    try {
                        val directList = apiService?.getMaquinas(0, 1000, null, null, null) ?: emptyList()
                        if (directList.isNotEmpty()) {
                            // Update our local cache with direct API results
                            directList.forEach { m ->
                                val existingIdx = mockMaquinas.indexOfFirst { it.id == m.id }
                                if (existingIdx != -1) {
                                    mockMaquinas[existingIdx] = m
                                } else {
                                    mockMaquinas.add(m)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DataRepository", "Direct api/maquinas failed: ${e.message}. Using client-by-client extraction fallback.")
                    }

                    // To be absolutely certain we have all machines and association with clients,
                    // we query all active clients and fetch their respective machines.
                    try {
                        val activeClients = apiService?.getClientes(0, 500, null, null, null, null) ?: emptyList()
                        activeClients.forEach { cli ->
                            val cod = cli.codCliente
                            if (cod != null) {
                                try {
                                    val clMachines = apiService?.getMaquinasPorCliente(cod.toInt()) ?: emptyList()
                                    clMachines.forEach { m ->
                                        val existingIdx = mockMaquinas.indexOfFirst { it.id == m.id }
                                        val mWithCod = if (m.codCliente == null) m.copy(codCliente = cod.toInt()) else m
                                        if (existingIdx != -1) {
                                            mockMaquinas[existingIdx] = mWithCod
                                        } else {
                                            mockMaquinas.add(mWithCod)
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Squelch individual client failures
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DataRepository", "Failed to fetch clients for machine list: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("DataRepository", "Critical error in getMaquinas live fetch: ${e.message}")
            }
            return getMaquinasDemoFallback(page, size, search, ativo, codCliente)
        }
    }

    private fun getMaquinasDemoFallback(
        page: Int,
        size: Int,
        search: String?,
        ativo: Boolean?,
        codCliente: Int?
    ): List<Maquina> {
        var filtered = mockMaquinas.asSequence().filter { !it.isExcluded() && !isClientExcluded(it.codCliente) }
        if (!search.isNullOrBlank()) {
            filtered = filtered.filter {
                it.nom_maq?.contains(search, ignoreCase = true) == true ||
                it.numeroPlaca?.contains(search, ignoreCase = true) == true
            }
        }
        if (ativo != null) filtered = filtered.filter { it.ativo == ativo }
        if (codCliente != null) filtered = filtered.filter { it.codCliente == codCliente }

        val list = filtered.toList()
        val startIdx = page * size
        if (startIdx >= list.size) return emptyList()
        val endIdx = (startIdx + size).coerceAtMost(list.size)
        return list.subList(startIdx, endIdx)
    }

    private fun isClientExcluded(codCliente: Int?): Boolean {
        if (codCliente == null) return false
        val client = mockClientes.find { it.codCliente == codCliente.toLong() }
        return client?.isExcluded() ?: false
    }

    // Helper to fetch matching client for a machine
    fun getClientForMachine(codCliente: Int?): Cliente? {
        if (codCliente == null) return null
        return mockClientes.find { it.codCliente == codCliente.toLong() }
    }

    // List of unique neighbourhoods for filter lists
    fun getUniqueBairros(): List<String> {
        return mockClientes.mapNotNull { it.bairro }.distinct().sorted()
    }

    suspend fun login(username: String, senha: String): com.example.data.model.Usuario? {
        if (_isDemoMode.value) {
            // Simulated database user password registry matching
            val lowerUser = username.lowercase().trim()
            val validSenha = when (lowerUser) {
                "marcio" -> "marcio123"
                "admin" -> "admin123"
                "tecnico" -> "tecnico123"
                "leiturista" -> "leiturista123"
                else -> null // DO NOT allow any other user to log in if not in simulated database
            }

            if (validSenha == null || senha != validSenha) {
                Log.w("DataRepository", "Demo Login failed: username / password mismatch or user not in database!")
                return null
            }

            val isLeiturista = username.lowercase().contains("leiturista")
            return com.example.data.model.Usuario(
                id = 1L,
                username = username,
                senha = validSenha,
                leiturista = if (isLeiturista) 1 else 0,
                idPontos = 10,
                ultimoAcesso = java.time.LocalDateTime.now().toString(),
                telefone = "11999998888",
                nome = username.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                email = "$username@systempro.com.br",
                cargo = if (isLeiturista) "Leiturista" else "Técnico de Manutenção",
                equipe = "Equipe Alfa",
                unidade = "São Paulo - LWD"
            )
        }

        try {
            val apiResponse = apiService?.loginApi(com.example.data.model.UserLoginRequest(username, senha))
            if (apiResponse != null) {
                if (apiResponse.isSuccessful) {
                    return apiResponse.body()
                }
                // 401/403: credenciais inválidas confirmadas pelo backend.
                // Não cair em nenhum fallback que finja sucesso.
                Log.w("DataRepository", "loginApi rejected: HTTP ${apiResponse.code()}")
                return null
            }
        } catch (e: Exception) {
            Log.e("DataRepository", "loginApi failed: ${e.message}")
        }
        return null
    }

    suspend fun getSolicitacoesAbertasCount(): Int {
        if (_isDemoMode.value) {
            if (localSolicitacoes.isEmpty()) {
                generateMockSolicitacoes()
            }
            return localSolicitacoes.filter { it.status == true }.size
        }

        try {
            val list = apiService?.getSolicitacoesAbertas()
            if (list != null) {
                localSolicitacoes.clear()
                localSolicitacoes.addAll(list)
                return list.size
            }
        } catch (e: Exception) {
            Log.w("DataRepository", "getSolicitacoesAbertas failed: ${e.message}")
        }

        try {
            val list = apiService?.getSolicitacoes()
            if (list != null) {
                val abertas = list.filter { it.status == true }
                localSolicitacoes.clear()
                localSolicitacoes.addAll(list)
                return abertas.size
            }
        } catch (e: Exception) {
            Log.e("DataRepository", "getSolicitacoes failed: ${e.message}")
        }

        return localSolicitacoes.filter { it.status == true }.size
    }

    suspend fun getSolicitacoes(): List<com.example.data.model.SolicitacaoResponseDTO> {
        if (_isDemoMode.value) {
            if (localSolicitacoes.isEmpty()) {
                generateMockSolicitacoes()
            }
            return localSolicitacoes
        }

        try {
            val list = apiService?.getSolicitacoes() ?: apiService?.getSolicitacoesAbertas()
            if (list != null) {
                localSolicitacoes.clear()
                localSolicitacoes.addAll(list)
                return list
            }
        } catch (e: Exception) {
            Log.e("DataRepository", "Failed to fetch solicitacoes: ${e.message}")
        }
        return localSolicitacoes
    }

    suspend fun createSolicitacao(dto: com.example.data.model.SolicitacaoDTO): String? {
        if (_isDemoMode.value) {
            val clientName = mockClientes.find { it.codCliente == dto.cliente }?.nomCliente ?: "Cliente #${dto.cliente}"
            val newResp = com.example.data.model.SolicitacaoResponseDTO(
                id = (localSolicitacoes.size + 1).toLong(),
                idProblema = Random.nextLong(1000, 9999),
                clienteId = dto.cliente,
                cliente = clientName,
                status = true,
                problemas = dto.problemas
            )
            localSolicitacoes.add(0, newResp)
            return null
        }

        try {
            apiService?.createSolicitacao(dto)
            return null
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            Log.e("DataRepository", "createSolicitacao HTTP error ${e.code()}: $errorBody")
            return "HTTP ${e.code()}: ${if (errorBody.length > 50) errorBody.take(50) + "..." else errorBody}"
        } catch (e: Exception) {
            Log.e("DataRepository", "createSolicitacao failed: ${e.message}")
            return e.message ?: "Erro desconhecido"
        }
    }

    suspend fun createCliente(cliente: com.example.data.model.Cliente): String? {
        if (_isDemoMode.value) {
            val newCliente = cliente.copy(
                codCliente = (mockClientes.maxOfOrNull { it.codCliente ?: 0L } ?: 0L) + 1,
                ativo = true
            )
            mockClientes.add(0, newCliente)
            return null
        }

        try {
            val response = apiService?.createCliente(cliente)
            if (response != null && !response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: ""
                Log.e("DataRepository", "createCliente HTTP error ${response.code()}: $errorBody")
                return "HTTP ${response.code()}: ${if (errorBody.length > 80) errorBody.take(80) + "..." else errorBody}"
            }
            return null
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            Log.e("DataRepository", "createCliente HTTP error ${e.code()}: $errorBody")
            return "HTTP ${e.code()}: ${if (errorBody.length > 80) errorBody.take(80) + "..." else errorBody}"
        } catch (e: Exception) {
            Log.e("DataRepository", "createCliente failed: ${e.message}")
            return e.message ?: "Erro desconhecido"
        }
    }

    suspend fun createMaquina(maquina: com.example.data.model.Maquina): String? {
        if (_isDemoMode.value) {
            val newMaquina = maquina.copy(
                id = Random.nextLong(1000, 9999),
                ativo = true
            )
            val clienteIdx = mockClientes.indexOfFirst { it.codCliente?.toInt() == maquina.codCliente }
            if (clienteIdx >= 0) {
                val cliente = mockClientes[clienteIdx]
                val updatedMaquinas = (cliente.maquinas ?: emptyList()) + newMaquina
                mockClientes[clienteIdx] = cliente.copy(maquinas = updatedMaquinas)
            }
            return null
        }

        try {
            val response = apiService?.createMaquina(maquina)
            if (response != null && !response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: ""
                Log.e("DataRepository", "createMaquina HTTP error ${response.code()}: $errorBody")
                return "HTTP ${response.code()}: ${if (errorBody.length > 80) errorBody.take(80) + "..." else errorBody}"
            }
            return null
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            Log.e("DataRepository", "createMaquina HTTP error ${e.code()}: $errorBody")
            return "HTTP ${e.code()}: ${if (errorBody.length > 80) errorBody.take(80) + "..." else errorBody}"
        } catch (e: Exception) {
            Log.e("DataRepository", "createMaquina failed: ${e.message}")
            return e.message ?: "Erro desconhecido"
        }
    }

    private fun generateMockSolicitacoes() {
        localSolicitacoes.clear()
        for (i in 1..8) {
            val cli = mockClientes.getOrNull(Random.nextInt(mockClientes.size.coerceAtLeast(1)))
            val problems = listOf(
                com.example.data.model.ProblemaDTO(
                    idProblema = i.toLong() * 10,
                    maquina = "Slot Model " + ('A' + Random.nextInt(5)),
                    descricao = "Tela piscando ou máquina travada"
                )
            )
            localSolicitacoes.add(
                com.example.data.model.SolicitacaoResponseDTO(
                    id = i.toLong(),
                    idProblema = i.toLong() * 10,
                    clienteId = cli?.codCliente,
                    cliente = cli?.nomCliente ?: "Bar do Ponto $i",
                    status = true,
                    problemas = problems
                )
            )
        }
    }
}

class SimpleCookieJar : okhttp3.CookieJar {
    private val cookieStore = HashMap<String, List<okhttp3.Cookie>>()

    override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {
        cookieStore[url.host] = cookies
    }

    override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }
}
