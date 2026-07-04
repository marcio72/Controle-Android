package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.example.util.LocationHelper
import androidx.lifecycle.viewModelScope
import com.example.data.model.Cliente
import com.example.data.model.Maquina
import com.example.data.model.Usuario
import com.example.data.model.SolicitacaoDTO
import com.example.data.model.ProblemaDTO
import com.example.data.model.SolicitacaoResponseDTO
import com.example.data.repository.DataRepository
import com.example.util.PdfExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppViewModel : ViewModel() {

    val repository = DataRepository()

    // --- USER LOGIN STATE ---
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _usuarioLogado = MutableStateFlow<Usuario?>(null)
    val usuarioLogado: StateFlow<Usuario?> = _usuarioLogado.asStateFlow()

    private val _loginLoading = MutableStateFlow(false)
    val loginLoading: StateFlow<Boolean> = _loginLoading.asStateFlow()

    // --- SOLICITAÇÕES STATE ---
    private val _solicitacoesCount = MutableStateFlow(0)
    val solicitacoesCount: StateFlow<Int> = _solicitacoesCount.asStateFlow()

    private val _solicitacoesList = MutableStateFlow<List<SolicitacaoResponseDTO>>(emptyList())
    val solicitacoesList: StateFlow<List<SolicitacaoResponseDTO>> = _solicitacoesList.asStateFlow()

    private val _solicitacoesLastUpdated = MutableStateFlow<String?>(null)
    val solicitacoesLastUpdated: StateFlow<String?> = _solicitacoesLastUpdated.asStateFlow()

    private val _solicitacoesPollingActive = MutableStateFlow(false)
    val solicitacoesPollingActive: StateFlow<Boolean> = _solicitacoesPollingActive.asStateFlow()

    private var solicitacoesPollingJob: Job? = null

    // Base URL & Demo Mode state
    val baseUrl: StateFlow<String> = repository.baseUrl
    val isDemoMode: StateFlow<Boolean> = repository.isDemoMode

    // Navigation and UI
    private val _selectedTab = MutableStateFlow(0) // 0 = Clients, 1 = Machines, 2 = Dashboard
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // --- CLIENTES STATE ---
    private val _clientes = MutableStateFlow<List<Cliente>>(emptyList())
    val clientes: StateFlow<List<Cliente>> = _clientes.asStateFlow()

    private val _clientesForSelection = MutableStateFlow<List<Cliente>>(emptyList())
    val clientesForSelection: StateFlow<List<Cliente>> = _clientesForSelection.asStateFlow()

    private val _execucoes = MutableStateFlow<List<com.example.data.model.ExecucaoDTO>>(emptyList())
    val execucoes: StateFlow<List<com.example.data.model.ExecucaoDTO>> = _execucoes.asStateFlow()

    private val _execucoesLoading = MutableStateFlow(false)
    val execucoesLoading: StateFlow<Boolean> = _execucoesLoading.asStateFlow()

    fun loadExecucoes() {
        viewModelScope.launch {
            _execucoesLoading.value = true
            try {
                _execucoes.value = repository.getExecucoes()
            } catch (e: Exception) {
                Log.e("AppViewModel", "loadExecucoes error: ${e.message}")
            } finally {
                _execucoesLoading.value = false
            }
        }
    }

    private val _clientesLoading = MutableStateFlow(false)
    val clientesLoading: StateFlow<Boolean> = _clientesLoading.asStateFlow()

    private val _clientesPage = MutableStateFlow(0)
    private val _clientesHasMore = MutableStateFlow(true)
    val clientesHasMore: StateFlow<Boolean> = _clientesHasMore.asStateFlow()

    // Client Filters
    private val _clientSearchQuery = MutableStateFlow("")
    val clientSearchQuery: StateFlow<String> = _clientSearchQuery.asStateFlow()

    private val _clientFilterRegiao = MutableStateFlow<Int?>(null)
    val clientFilterRegiao: StateFlow<Int?> = _clientFilterRegiao.asStateFlow()

    private val _clientFilterAtivo = MutableStateFlow<Boolean?>(null)
    val clientFilterAtivo: StateFlow<Boolean?> = _clientFilterAtivo.asStateFlow()

    private val _clientFilterBairro = MutableStateFlow<String?>(null)
    val clientFilterBairro: StateFlow<String?> = _clientFilterBairro.asStateFlow()

    // --- MAQUINAS STATE ---
    private val _maquinas = MutableStateFlow<List<Maquina>>(emptyList())
    val maquinas: StateFlow<List<Maquina>> = _maquinas.asStateFlow()

    private val _maquinasLoading = MutableStateFlow(false)
    val maquinasLoading: StateFlow<Boolean> = _maquinasLoading.asStateFlow()

    private val _maquinasPage = MutableStateFlow(0)
    private val _maquinasHasMore = MutableStateFlow(true)
    val maquinasHasMore: StateFlow<Boolean> = _maquinasHasMore.asStateFlow()

    // --- DASHBOARD METRICS STATE ---
    private val _totalActiveClientesCount = MutableStateFlow<Int?>(null)
    val totalActiveClientesCount: StateFlow<Int?> = _totalActiveClientesCount.asStateFlow()

    private val _totalMaquinasCount = MutableStateFlow<Int?>(null)
    val totalMaquinasCount: StateFlow<Int?> = _totalMaquinasCount.asStateFlow()

    private val _totalActiveMaquinasCount = MutableStateFlow<Int?>(null)
    val totalActiveMaquinasCount: StateFlow<Int?> = _totalActiveMaquinasCount.asStateFlow()

    // Machine Filters
    private val _machineSearchQuery = MutableStateFlow("")
    val machineSearchQuery: StateFlow<String> = _machineSearchQuery.asStateFlow()

    private val _machineFilterAtivo = MutableStateFlow<Boolean?>(null)
    val machineFilterAtivo: StateFlow<Boolean?> = _machineFilterAtivo.asStateFlow()

    private val _machineFilterCodCliente = MutableStateFlow<Int?>(null)
    val machineFilterCodCliente: StateFlow<Int?> = _machineFilterCodCliente.asStateFlow()

    // --- SHARED NOTIFICATION SYSTEM ---
    private val _appMessage = MutableStateFlow<String?>(null)
    val appMessage: StateFlow<String?> = _appMessage.asStateFlow()

    // Jobs to handle debounce on text search input
    private var clientSearchJob: Job? = null
    private var machineSearchJob: Job? = null

    init {
        // Load initial records
        loadClientes(reset = true)
        loadMaquinas(reset = true)
        loadDashboardMetrics()
        startSolicitacoesPolling()
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setDemoMode(demo: Boolean) {
        repository.setDemoMode(demo)
        // Refresh states
        clearAndReloadAll()
    }

    fun updateBaseUrl(newUrl: String) {
        repository.updateBaseUrl(newUrl)
        setDemoMode(false) // Toggle live mode
        clearAndReloadAll()
        showNotification("Caminho do Back-End atualizado para: $newUrl")
    }

    fun clearAndReloadAll() {
        loadClientes(reset = true)
        loadMaquinas(reset = true)
        loadDashboardMetrics()
        startSolicitacoesPolling() // reinicia o polling com a nova config/URL
    }

    fun loadDashboardMetrics() {
        viewModelScope.launch {
            try {
                val clients = repository.getClientes(
                    page = 0,
                    size = 1000,
                    search = null,
                    regiao = null,
                    ativo = null,
                    bairro = null,
                    leiturista = _usuarioLogado.value?.leiturista
                )
                val machines = repository.getMaquinas(
                    page = 0,
                    size = 1000,
                    search = null,
                    ativo = null,
                    codCliente = null,
                    leiturista = _usuarioLogado.value?.leiturista
                )
                val filteredMachines = machines.filter { !it.isExcluded() }

                _totalActiveClientesCount.value = clients.count { it.ativo == true }
                _totalMaquinasCount.value = filteredMachines.size
                _totalActiveMaquinasCount.value = filteredMachines.count { it.ativo == true }

                _clientesForSelection.value = clients.filter { it.ativo == true || it.ativo == null }
                    .sortedByDescending { it.codCliente }

                // Solicitações são atualizadas pelo polling automático (startSolicitacoesPolling)
                // Força uma atualização imediata aqui também para o dashboard inicial
                refreshSolicitacoes()
            } catch (e: Exception) {
                Log.e("AppViewModel", "Failed to load dashboard metrics: ${e.message}")
            }
        }
    }

    fun showNotification(msg: String) {
        _appMessage.value = msg
    }

    fun clearNotification() {
        _appMessage.value = null
    }

    // --- SOLICITAÇÕES POLLING ---
    private val solicitacoesFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun startSolicitacoesPolling(intervalSeconds: Long = 30) {
        solicitacoesPollingJob?.cancel()
        solicitacoesPollingJob = viewModelScope.launch {
            _solicitacoesPollingActive.value = true
            while (isActive) {
                refreshSolicitacoes()
                delay(intervalSeconds * 1000)
            }
        }
    }

    fun stopSolicitacoesPolling() {
        solicitacoesPollingJob?.cancel()
        solicitacoesPollingJob = null
        _solicitacoesPollingActive.value = false
    }

    fun refreshSolicitacoesManual() {
        viewModelScope.launch {
            refreshSolicitacoes()
        }
    }

    private suspend fun refreshSolicitacoes() {
        try {
            val count = repository.getSolicitacoesAbertasCount()
            val list = repository.getSolicitacoes()
            val previousCount = _solicitacoesCount.value
            _solicitacoesCount.value = count
            _solicitacoesList.value = list
            _solicitacoesLastUpdated.value = LocalDateTime.now().format(solicitacoesFormatter)
            // Notifica o usuário apenas quando aparecer uma nova solicitação em aberto
            if (previousCount > 0 && count > previousCount) {
                val novas = count - previousCount
                showNotification("$novas nova(s) solicitação(ões) em aberto!")
            }
        } catch (e: Exception) {
            Log.e("AppViewModel", "refreshSolicitacoes error: ${e.message}")
        }
    }

    // --- CLIENTS DATA RETRIEVAL ---
    fun onClientSearchChanged(query: String) {
        _clientSearchQuery.value = query
        clientSearchJob?.cancel()
        clientSearchJob = viewModelScope.launch {
            delay(400) // Debounce text updates to avoid API flood
            loadClientes(reset = true)
        }
    }

    fun setClientFilterRegiao(regiao: Int?) {
        _clientFilterRegiao.value = regiao
        loadClientes(reset = true)
    }

    fun setClientFilterAtivo(ativo: Boolean?) {
        _clientFilterAtivo.value = ativo
        loadClientes(reset = true)
    }

    fun setClientFilterBairro(bairro: String?) {
        _clientFilterBairro.value = bairro
        loadClientes(reset = true)
    }

    fun loadClientes(reset: Boolean = false) {
        if (_clientesLoading.value) return

        viewModelScope.launch {
            _clientesLoading.value = true
            if (reset) {
                _clientesPage.value = 0
                _clientesHasMore.value = true
                _clientes.value = emptyList()
            }

            val curPage = _clientesPage.value
            val pageSize = 15

            try {
                val results = repository.getClientes(
                    page = curPage,
                    size = pageSize,
                    search = _clientSearchQuery.value,
                    regiao = _clientFilterRegiao.value,
                    ativo = _clientFilterAtivo.value,
                    bairro = _clientFilterBairro.value,
                    leiturista = _usuarioLogado.value?.leiturista
                )

                if (results.isEmpty()) {
                    _clientesHasMore.value = false
                } else {
                    _clientes.value = _clientes.value + results
                    _clientesPage.value = curPage + 1
                    if (results.size < pageSize) {
                        _clientesHasMore.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Failed to fetch clients: ${e.message}")
                showNotification("Erro ao carregar dados do servidor. Usando cache/offline fallback.")
                _clientesHasMore.value = false
            } finally {
                _clientesLoading.value = false
            }
        }
    }

    fun loadNextClientesPage() {
        if (_clientesHasMore.value && !_clientesLoading.value) {
            loadClientes(reset = false)
        }
    }


    // --- MACHINES DATA RETRIEVAL ---
    fun onMachineSearchChanged(query: String) {
        _machineSearchQuery.value = query
        machineSearchJob?.cancel()
        machineSearchJob = viewModelScope.launch {
            delay(400)
            loadMaquinas(reset = true)
        }
    }

    fun setMachineFilterAtivo(ativo: Boolean?) {
        _machineFilterAtivo.value = ativo
        loadMaquinas(reset = true)
    }

    fun setMachineFilterCodCliente(codCliente: Int?) {
        _machineFilterCodCliente.value = codCliente
        loadMaquinas(reset = true)
    }

    fun loadMaquinas(reset: Boolean = false) {
        if (_maquinasLoading.value) return

        viewModelScope.launch {
            _maquinasLoading.value = true
            if (reset) {
                _maquinasPage.value = 0
                _maquinasHasMore.value = true
                _maquinas.value = emptyList()
            }

            val curPage = _maquinasPage.value
            val pageSize = 15

            try {
                val results = repository.getMaquinas(
                    page = curPage,
                    size = pageSize,
                    search = _machineSearchQuery.value,
                    ativo = _machineFilterAtivo.value,
                    codCliente = _machineFilterCodCliente.value,
                    leiturista = _usuarioLogado.value?.leiturista
                )

                if (results.isEmpty()) {
                    _maquinasHasMore.value = false
                } else {
                    _maquinas.value = _maquinas.value + results
                    _maquinasPage.value = curPage + 1
                    if (results.size < pageSize) {
                        _maquinasHasMore.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Failed to fetch machines: ${e.message}")
                _maquinasHasMore.value = false
            } finally {
                _maquinasLoading.value = false
            }
        }
    }

    fun loadNextMaquinasPage() {
        if (_maquinasHasMore.value && !_maquinasLoading.value) {
            loadMaquinas(reset = false)
        }
    }


    // --- PDF EXPORT DRIVERS ---
    fun exportAllClientesToPdf(context: Context) {
        viewModelScope.launch {
            val fullList = repository.getClientes(
                page = 0,
                size = 1000, // Fetch a large list to represent a complete report
                search = if (_clientSearchQuery.value.isNotEmpty()) _clientSearchQuery.value else null,
                regiao = _clientFilterRegiao.value,
                ativo = _clientFilterAtivo.value,
                bairro = _clientFilterBairro.value,
                leiturista = _usuarioLogado.value?.leiturista
            )
            
            if (fullList.isEmpty()) {
                showNotification("Nenhum cliente para exportar relatório!")
                return@launch
            }

            val file = PdfExporter.exportClientesPdf(context, fullList)
            if (file != null) {
                showNotification("Relatório PDF de Clientes exportado!")
                PdfExporter.triggerSharePdf(context, file)
            } else {
                showNotification("Ocorreu um erro ao gerar o PDF de clientes.")
            }
        }
    }

    fun exportSpecificClienteToPdf(context: Context, cliente: Cliente) {
        viewModelScope.launch {
            val file = PdfExporter.exportClientesPdf(context, listOf(cliente))
            if (file != null) {
                showNotification("Relatório detalhado do cliente gerado!")
                PdfExporter.triggerSharePdf(context, file)
            } else {
                showNotification("Ocorreu um erro ao gerar o PDF.")
            }
        }
    }

    fun exportAllMaquinasToPdf(context: Context) {
        viewModelScope.launch {
            val fullList = repository.getMaquinas(
                page = 0,
                size = 1000,
                search = if (_machineSearchQuery.value.isNotEmpty()) _machineSearchQuery.value else null,
                ativo = _machineFilterAtivo.value,
                codCliente = _machineFilterCodCliente.value,
                leiturista = _usuarioLogado.value?.leiturista
            )

            if (fullList.isEmpty()) {
                showNotification("Nenhuma máquina para exportar!")
                return@launch
            }

            val file = PdfExporter.exportMaquinasPdf(context, fullList, repository)
            if (file != null) {
                showNotification("Relatório PDF de Máquinas exportado!")
                PdfExporter.triggerSharePdf(context, file)
            } else {
                showNotification("Ocorreu um erro ao gerar o PDF de máquinas.")
            }
        }
    }

    // Utility list of neighborhoods for filtering selection UI
    fun getBairrosList(): List<String> = repository.getUniqueBairros()

    fun performLogin(username: String, sand: String, onResult: (Boolean) -> Unit) {
        if (username.isBlank() || sand.isBlank()) {
            showNotification("Por favor, preencha o usuário e a senha.")
            onResult(false)
            return
        }

        viewModelScope.launch {
            _loginLoading.value = true
            try {
                val user = repository.login(username, sand)
                if (user != null) {
                    _usuarioLogado.value = user
                    _isLoggedIn.value = true
                    onResult(true)
                    showNotification("Bem-vindo, ${user.nome ?: user.username}!")
                    clearAndReloadAll()
                } else {
                    showNotification("Usuário ou senha inválidos.")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Login error: ${e.message}")
                showNotification("Erro na conexão com o servidor.")
                onResult(false)
            } finally {
                _loginLoading.value = false
            }
        }
    }

    fun performLogout() {
        _usuarioLogado.value = null
        _isLoggedIn.value = false
        showNotification("Sessão encerrada.")
    }

    fun performCreateSolicitacao(clienteId: Long?, itens: List<com.example.ui.screens.ProblemaEntrada>) {
        if (clienteId == null) {
            showNotification("Selecione um cliente para abrir a solicitação.")
            return
        }
        if (itens.isEmpty()) {
            showNotification("Adicione ao menos uma máquina com o problema descrito.")
            return
        }

        viewModelScope.launch {
            try {
                val currentDateTime = java.time.LocalDateTime.now()
                // Jackson on Java side expects yyyy-MM-dd'T'HH:mm (WITHOUT seconds) due to @JsonFormat pattern
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
                val currentDateTimeString = currentDateTime.format(formatter)

                val problemasDto = itens.map { item ->
                    // For the backend, send the machine ID; for offline/demo mode, send the full machine name
                    val maquinaValue = if (repository.isDemoMode.value) item.maquinaLabel else item.maquinaId

                    // numeroMaquina precisa ser um Long válido existente no banco do backend.
                    // O valor -1 é usado pela tela como placeholder de "Chamado Geral / Sem Equipamento"
                    // e não existe no banco, então não deve ser enviado como numeroMaquina.
                    val numeroMaquinaLong = item.maquinaId.toLongOrNull()?.takeIf { it > 0 }

                    com.example.data.model.ProblemaDTO(
                        idProblema = null,
                        numeroMaquina = numeroMaquinaLong,
                        maquina = maquinaValue,
                        descricao = item.descricao
                    )
                }

                val dto = com.example.data.model.SolicitacaoDTO(
                    cliente = clienteId,
                    dataSolicitacao = currentDateTimeString,
                    problemas = problemasDto,
                    nomeTecnico = _usuarioLogado.value?.nome ?: _usuarioLogado.value?.username ?: "Técnico"
                )

                val errorMsg = repository.createSolicitacao(dto)
                if (errorMsg == null) {
                    val msg = if (itens.size > 1) "Solicitação aberta com ${itens.size} máquinas!" else "Nova solicitação aberta!"
                    showNotification(msg)
                    refreshSolicitacoes() // atualiza contador e lista imediatamente
                } else {
                    showNotification("Falha ao salvar: $errorMsg")
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "createSolicitacao error: ${e.message}", e)
                showNotification("Erro ao enviar para o back-end.")
            }
        }
    }

    fun performCreateCliente(
        nomCliente: String,
        telefone: String,
        contato: String,
        logradouro: String,
        bairro: String,
        regiao: Int?,
        onResult: (Boolean) -> Unit
    ) {
        if (nomCliente.isBlank()) {
            showNotification("Informe o nome do cliente.")
            onResult(false)
            return
        }

        viewModelScope.launch {
            try {
                val cliente = com.example.data.model.Cliente(
                    codCliente = null,
                    nomCliente = nomCliente.trim(),
                    logradouro = logradouro.ifBlank { null },
                    telefone = telefone.ifBlank { null },
                    bairro = bairro.ifBlank { null },
                    contato = contato.ifBlank { null },
                    leiturista = null,
                    regiao = regiao,
                    dtCadastro = null,
                    ativo = true,
                    maquinas = emptyList()
                )

                val errorMsg = repository.createCliente(cliente)
                if (errorMsg == null) {
                    showNotification("Cliente \"${cliente.nomCliente}\" cadastrado com sucesso!")
                    loadClientes(reset = true)
                    onResult(true)
                } else {
                    showNotification("Falha ao salvar cliente: $errorMsg")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "createCliente error: ${e.message}", e)
                showNotification("Erro ao enviar cliente para o back-end.")
                onResult(false)
            }
        }
    }

    fun performCreateMaquina(
        codCliente: Int?,
        numeroMaquina: String,
        nomeJogo: String,
        numeroPlaca: String,
        observacoes: String,
        onResult: (Boolean) -> Unit
    ) {
        if (codCliente == null) {
            showNotification("Selecione o cliente vinculado à máquina.")
            onResult(false)
            return
        }
        if (numeroMaquina.isBlank()) {
            showNotification("Informe o número da máquina.")
            onResult(false)
            return
        }

        viewModelScope.launch {
            try {
                val maquina = com.example.data.model.Maquina(
                    id = null,
                    nom_maq = numeroMaquina.trim(),
                    nom_jogo = nomeJogo.ifBlank { null },
                    numeroPlaca = numeroPlaca.ifBlank { null },
                    obs = observacoes.ifBlank { null },
                    codCliente = codCliente,
                    ativo = true
                )

                val errorMsg = repository.createMaquina(maquina)
                if (errorMsg == null) {
                    showNotification("Máquina \"${maquina.nom_maq}\" cadastrada com sucesso!")
                    loadClientes(reset = true)
                    loadMaquinas(reset = true)
                    onResult(true)
                } else {
                    showNotification("Falha ao salvar máquina: $errorMsg")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "createMaquina error: ${e.message}", e)
                showNotification("Erro ao enviar máquina para o back-end.")
                onResult(false)
            }
        }
    }

    fun performUpdateCliente(
        codCliente: Long,
        nomCliente: String,
        telefone: String,
        contato: String,
        logradouro: String,
        bairro: String,
        regiao: Int?,
        onResult: (Boolean) -> Unit
    ) {
        if (nomCliente.isBlank()) {
            showNotification("Informe o nome do cliente.")
            onResult(false)
            return
        }

        viewModelScope.launch {
            try {
                val cliente = com.example.data.model.Cliente(
                    codCliente = codCliente,
                    nomCliente = nomCliente.trim(),
                    logradouro = logradouro.ifBlank { null },
                    telefone = telefone.ifBlank { null },
                    bairro = bairro.ifBlank { null },
                    contato = contato.ifBlank { null },
                    leiturista = null,
                    regiao = regiao,
                    dtCadastro = null,
                    ativo = true,
                    maquinas = emptyList()
                )

                val errorMsg = repository.updateCliente(codCliente, cliente)
                if (errorMsg == null) {
                    showNotification("Cliente atualizado com sucesso!")
                    loadClientes(reset = true)
                    onResult(true)
                } else {
                    showNotification("Falha ao atualizar cliente: $errorMsg")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "updateCliente error: ${e.message}", e)
                showNotification("Erro ao enviar atualização para o back-end.")
                onResult(false)
            }
        }
    }

    fun performDesativarCliente(codCliente: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val errorMsg = repository.desativarCliente(codCliente)
                if (errorMsg == null) {
                    showNotification("Cliente desativado.")
                    loadClientes(reset = true)
                    onResult(true)
                } else {
                    showNotification("Falha ao desativar cliente: $errorMsg")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "desativarCliente error: ${e.message}", e)
                showNotification("Erro ao enviar para o back-end.")
                onResult(false)
            }
        }
    }

    fun performUpdateMaquina(
        id: Int,
        codCliente: Int?,
        numeroMaquina: String,
        nomeJogo: String,
        numeroPlaca: String,
        observacoes: String,
        onResult: (Boolean) -> Unit
    ) {
        if (codCliente == null) {
            showNotification("Selecione o cliente vinculado à máquina.")
            onResult(false)
            return
        }
        if (numeroMaquina.isBlank()) {
            showNotification("Informe o número da máquina.")
            onResult(false)
            return
        }

        viewModelScope.launch {
            try {
                val maquina = com.example.data.model.Maquina(
                    id = id.toLong(),
                    nom_maq = numeroMaquina.trim(),
                    nom_jogo = nomeJogo.ifBlank { null },
                    numeroPlaca = numeroPlaca.ifBlank { null },
                    obs = observacoes.ifBlank { null },
                    codCliente = codCliente,
                    ativo = true
                )

                val errorMsg = repository.updateMaquina(id, maquina)
                if (errorMsg == null) {
                    showNotification("Máquina atualizada com sucesso!")
                    loadClientes(reset = true)
                    loadMaquinas(reset = true)
                    onResult(true)
                } else {
                    showNotification("Falha ao atualizar máquina: $errorMsg")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "updateMaquina error: ${e.message}", e)
                showNotification("Erro ao enviar atualização para o back-end.")
                onResult(false)
            }
        }
    }

    fun performDesativarMaquina(id: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val errorMsg = repository.desativarMaquina(id)
                if (errorMsg == null) {
                    showNotification("Máquina desativada.")
                    loadClientes(reset = true)
                    loadMaquinas(reset = true)
                    onResult(true)
                } else {
                    showNotification("Falha ao desativar máquina: $errorMsg")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "desativarMaquina error: ${e.message}", e)
                showNotification("Erro ao enviar para o back-end.")
                onResult(false)
            }
        }
    }

    // --- EXECUÇÃO DE SOLICITAÇÃO ---

    private val _categorias = MutableStateFlow<List<com.example.data.model.CategoriaDTO>>(emptyList())
    val categorias: StateFlow<List<com.example.data.model.CategoriaDTO>> = _categorias.asStateFlow()

    private val _pecasDisponiveis = MutableStateFlow<List<com.example.data.model.PecaDTO>>(emptyList())
    val pecasDisponiveis: StateFlow<List<com.example.data.model.PecaDTO>> = _pecasDisponiveis.asStateFlow()

    private val _pecasLoading = MutableStateFlow(false)
    val pecasLoading: StateFlow<Boolean> = _pecasLoading.asStateFlow()

    private val _execucaoLoading = MutableStateFlow(false)
    val execucaoLoading: StateFlow<Boolean> = _execucaoLoading.asStateFlow()

    fun loadCategorias() {
        viewModelScope.launch {
            try {
                _categorias.value = repository.getCategorias()
            } catch (e: Exception) {
                Log.e("AppViewModel", "loadCategorias error: ${e.message}")
            }
        }
    }

    fun loadPecasDisponiveis(categoriaId: Long) {
        viewModelScope.launch {
            _pecasLoading.value = true
            try {
                _pecasDisponiveis.value = repository.getPecasDisponiveis(categoriaId)
            } catch (e: Exception) {
                Log.e("AppViewModel", "loadPecasDisponiveis error: ${e.message}")
                _pecasDisponiveis.value = emptyList()
            } finally {
                _pecasLoading.value = false
            }
        }
    }


    // --- LOTES & CATEGORIAS ---
    private val _lotes = MutableStateFlow<List<com.example.data.model.LoteDTO>>(emptyList())
    val lotes: StateFlow<List<com.example.data.model.LoteDTO>> = _lotes.asStateFlow()

    private val _lotesLoading = MutableStateFlow(false)
    val lotesLoading: StateFlow<Boolean> = _lotesLoading.asStateFlow()

    private val _categoriasLoading = MutableStateFlow(false)
    val categoriasLoading: StateFlow<Boolean> = _categoriasLoading.asStateFlow()

    private val _pecasDoLote = MutableStateFlow<Map<Long, List<com.example.data.model.PecaDTO>>>(emptyMap())
    val pecasDoLote: StateFlow<Map<Long, List<com.example.data.model.PecaDTO>>> = _pecasDoLote.asStateFlow()

    private val _pecasDoLoteLoading = MutableStateFlow(false)
    val pecasDoLoteLoading: StateFlow<Boolean> = _pecasDoLoteLoading.asStateFlow()

    fun loadLotes() {
        viewModelScope.launch {
            _lotesLoading.value = true
            try {
                _lotes.value = repository.getLotes()
            } catch (e: Exception) {
                Log.e("AppViewModel", "loadLotes error: ${e.message}")
            } finally {
                _lotesLoading.value = false
            }
        }
    }

    fun loadPecasDoLote(loteId: Long) {
        if (_pecasDoLote.value.containsKey(loteId)) return
        viewModelScope.launch {
            _pecasDoLoteLoading.value = true
            try {
                val pecas = repository.getPecasDoLote(loteId)
                _pecasDoLote.value = _pecasDoLote.value + (loteId to pecas)
            } catch (e: Exception) {
                Log.e("AppViewModel", "loadPecasDoLote error: ${e.message}")
            } finally {
                _pecasDoLoteLoading.value = false
            }
        }
    }

    fun criarLote(
        categoriaId: Long,
        alias: String,
        fornecedor: String?,
        codigo: String?,
        descricao: String?,
        quantidadeComprada: Int,
        numeroInicial: Int,
        dataEntrada: String?,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val request = com.example.data.model.LoteRequestDTO(
                categoriaId = categoriaId,
                alias = alias,
                fornecedor = fornecedor,
                codigo = codigo,
                descricao = descricao,
                quantidadeComprada = quantidadeComprada,
                numeroInicial = numeroInicial,
                dataEntrada = dataEntrada
            )
            val resultado = repository.criarLote(request)
            if (resultado != null) {
                showNotification("Lote criado com sucesso!")
                _pecasDoLote.value = emptyMap()
                loadLotes()
                onResult(true)
            } else {
                showNotification("Erro ao criar lote.")
                onResult(false)
            }
        }
    }

    fun criarCategoria(nome: String, alias: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _categoriasLoading.value = true
            val resultado = repository.criarCategoria(nome, alias)
            _categoriasLoading.value = false
            if (resultado != null) {
                showNotification("Categoria criada!")
                loadCategorias()
                onResult(true)
            } else {
                showNotification("Erro ao criar categoria.")
                onResult(false)
            }
        }
    }

    fun clearPecasDisponiveis() {
        _pecasDisponiveis.value = emptyList()
    }

    fun performRegistrarExecucao(
        solicitacaoId: Long,
        execucoesPorProblema: Map<Long, Pair<String, List<Long>>>,
        nomeCliente: String? = null,
        context: Context? = null,
        onResult: (Boolean) -> Unit
    ) {
        if (execucoesPorProblema.isEmpty()) {
            showNotification("Nenhum problema para registrar.")
            onResult(false)
            return
        }

        val tecnico = _usuarioLogado.value?.nome
            ?: _usuarioLogado.value?.username
            ?: "Técnico"

        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val agora = formatter.format(java.util.Date())

        val lista = execucoesPorProblema.map { (problemaId, pair) ->
            com.example.data.model.ExecucaoRequestDTO(
                problemaId    = problemaId,
                solicitacaoId = solicitacaoId,
                dataExecucao  = agora,
                tecnico       = tecnico,
                descricao     = pair.first,
                pecasUsadas   = pair.second
            )
        }

        viewModelScope.launch {
            _execucaoLoading.value = true
            try {
                // 1. Capturar localização GPS
                val localizacao = context?.let {
                    try { LocationHelper.getLocalizacaoAtual(it) } catch (e: Exception) { null }
                }

                // 2. Registrar execução
                val errorMsg = repository.registrarExecucao(lista)
                if (errorMsg == null) {
                    // 3. Salvar log de envio
                    try {
                        val logRequest = com.example.data.model.LogEnvioRequestDTO(
                            numeroEnvio = solicitacaoId,
                            dataEnvio   = agora,
                            nomeCliente = nomeCliente,
                            tecnico     = tecnico,
                            localizacao = localizacao
                        )
                        repository.salvarLogEnvio(logRequest)
                    } catch (e: Exception) {
                        Log.e("AppViewModel", "salvarLogEnvio error: ${e.message}")
                    }

                    showNotification("Execução registrada! Notificação enviada ao Signal.")
                    refreshSolicitacoes()
                    loadExecucoes()
                    onResult(true)
                } else {
                    showNotification("Falha ao registrar: $errorMsg")
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "performRegistrarExecucao error: ${e.message}", e)
                showNotification("Erro ao enviar para o back-end.")
                onResult(false)
            } finally {
                _execucaoLoading.value = false
            }
        }
    }

    // --- LOG DE ENVIOS ---
    private val _logEnvios = MutableStateFlow<List<com.example.data.model.LogEnvioDTO>>(emptyList())
    val logEnvios: StateFlow<List<com.example.data.model.LogEnvioDTO>> = _logEnvios.asStateFlow()

    private val _logEnviosLoading = MutableStateFlow(false)
    val logEnviosLoading: StateFlow<Boolean> = _logEnviosLoading.asStateFlow()

    fun loadLogEnvios() {
        viewModelScope.launch {
            _logEnviosLoading.value = true
            try {
                _logEnvios.value = repository.listarLogEnvios()
            } catch (e: Exception) {
                Log.e("AppViewModel", "loadLogEnvios error: ${e.message}")
            } finally {
                _logEnviosLoading.value = false
            }
        }
    }
}
