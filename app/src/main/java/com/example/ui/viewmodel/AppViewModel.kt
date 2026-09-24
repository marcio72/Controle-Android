package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.example.util.LocationHelper
import com.example.util.SessionManager
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppViewModel(application: Application) : AndroidViewModel(application) {

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

    // Restaura a sessão salva (se houver) assim que o ViewModel é criado.
    // Isso cobre o caso em que o Android matou o processo do app em segundo
    // plano (ex: enquanto a câmera do sistema estava aberta) e o recriou
    // depois — sem isso, o app cairia na tela de login mesmo já logado.
    // Envolvido em try/catch: qualquer falha aqui não pode travar a abertura do app.
    init {
        try {
            val usuarioSalvo = SessionManager.carregarSessao(getApplication<Application>())
            if (usuarioSalvo != null) {
                _usuarioLogado.value = usuarioSalvo
                _isLoggedIn.value = true
                clearAndReloadAll()
            }
        } catch (e: Exception) {
            Log.e("AppViewModel", "Falha ao restaurar sessão salva: ${e.message}")
        }
    }

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
                    SessionManager.salvarSessao(getApplication<Application>(), user)
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
        SessionManager.limparSessao(getApplication<Application>())
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
                        descricao = item.descricao,
                        fotoBase64 = item.fotoBase64
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

    // Subcategorias da categoria atualmente selecionada (ex: tamanhos de Monitor).
    // Fica vazio quando a categoria escolhida não tem nenhuma cadastrada.
    private val _subCategorias = MutableStateFlow<List<com.example.data.model.SubCategoriaDTO>>(emptyList())
    val subCategorias: StateFlow<List<com.example.data.model.SubCategoriaDTO>> = _subCategorias.asStateFlow()

    fun loadSubCategorias(categoriaId: Long?) {
        if (categoriaId == null) {
            _subCategorias.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                _subCategorias.value = repository.getSubCategorias(categoriaId)
            } catch (e: Exception) {
                Log.e("AppViewModel", "loadSubCategorias error: ${e.message}")
                _subCategorias.value = emptyList()
            }
        }
    }

    fun criarSubCategoria(nome: String, categoriaId: Long, onResult: (com.example.data.model.SubCategoriaDTO?) -> Unit) {
        viewModelScope.launch {
            val resultado = repository.criarSubCategoria(nome, categoriaId)
            if (resultado != null) {
                showNotification("Subcategoria criada!")
                loadSubCategorias(categoriaId)
            } else {
                showNotification("Erro ao criar subcategoria.")
            }
            onResult(resultado)
        }
    }

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

    // Subcategoria de cada peça disponível (idPeca -> SubCategoriaDTO).
    // Preenchido em loadPecasDisponiveis: usa o campo subCategoriaId da própria
    // peça quando o backend envia; senão, descobre pelo lote de origem.
    private val _subCategoriaPorPeca =
        MutableStateFlow<Map<Long, com.example.data.model.SubCategoriaDTO>>(emptyMap())
    val subCategoriaPorPeca: StateFlow<Map<Long, com.example.data.model.SubCategoriaDTO>> =
        _subCategoriaPorPeca.asStateFlow()

    /**
     * Carrega as peças em estoque da categoria e resolve a subcategoria de cada
     * uma, para a tela de execução poder agrupar "Categoria > Subcategoria > peças".
     *
     * Também atualiza [subCategorias] com as subcategorias cadastradas da
     * categoria escolhida (fica vazio se a categoria não tiver nenhuma).
     */
    fun loadPecasDisponiveis(categoriaId: Long) {
        viewModelScope.launch {
            _pecasLoading.value = true
            _subCategoriaPorPeca.value = emptyMap()
            try {
                val subs = repository.getSubCategorias(categoriaId)
                _subCategorias.value = subs

                val pecas = repository.getPecasDisponiveis(categoriaId)
                _pecasDisponiveis.value = pecas

                _subCategoriaPorPeca.value = resolverSubCategoriaDasPecas(categoriaId, pecas, subs)
            } catch (e: Exception) {
                Log.e("AppViewModel", "loadPecasDisponiveis error: ${e.message}")
                _pecasDisponiveis.value = emptyList()
                _subCategoriaPorPeca.value = emptyMap()
            } finally {
                _pecasLoading.value = false
            }
        }
    }

    /**
     * Monta o mapa idPeca -> subcategoria.
     *
     * 1º) Se a peça já vem com subCategoriaId do backend, usa direto (sem rede).
     * 2º) Senão, e se a categoria tiver subcategorias cadastradas, busca os lotes
     *     dessa categoria (o lote guarda a subcategoria) e marca as peças de cada
     *     lote. As peças que sobrarem ficam sem subcategoria e caem no grupo
     *     "Sem subcategoria" na tela.
     */
    private suspend fun resolverSubCategoriaDasPecas(
        categoriaId: Long,
        pecas: List<com.example.data.model.PecaDTO>,
        subs: List<com.example.data.model.SubCategoriaDTO>
    ): Map<Long, com.example.data.model.SubCategoriaDTO> {
        if (pecas.isEmpty() || subs.isEmpty()) return emptyMap()

        val porId = subs.associateBy { it.id }
        val mapa = mutableMapOf<Long, com.example.data.model.SubCategoriaDTO>()

        // 1º caminho: a própria peça já traz a subcategoria
        pecas.forEach { peca ->
            val sub = peca.subCategoriaId?.let { porId[it] }
                ?: peca.subCategoriaNome?.let { nome ->
                    subs.find { it.nome.equals(nome, ignoreCase = true) }
                }
            if (sub != null) mapa[peca.idPeca] = sub
        }
        if (mapa.size == pecas.size) return mapa

        // 2º caminho: descobre pelo lote de origem das peças que faltaram
        return try {
            val lotesDaCategoria = repository.getLotes().filter {
                it.categoria?.id == categoriaId && it.subCategoria != null && it.quantidadeAtual > 0
            }
            if (lotesDaCategoria.isEmpty()) return mapa

            val faltando = pecas.filter { !mapa.containsKey(it.idPeca) }.map { it.idPeca }.toSet()

            // Peças que já vieram com loteId não precisam de nova chamada
            val subPorLote = lotesDaCategoria.associate { it.idLote to it.subCategoria!! }
            val aindaFaltando = faltando.toMutableSet()
            pecas.forEach { peca ->
                if (peca.idPeca in aindaFaltando) {
                    val sub = peca.loteId?.let { subPorLote[it] }
                    if (sub != null) {
                        mapa[peca.idPeca] = sub
                        aindaFaltando.remove(peca.idPeca)
                    }
                }
            }
            if (aindaFaltando.isEmpty()) return mapa

            // Último recurso: lista as peças de cada lote (em paralelo) e cruza pelo id
            coroutineScope {
                lotesDaCategoria.map { lote ->
                    async {
                        lote.idLote to runCatching { repository.getPecasDoLote(lote.idLote) }
                            .getOrDefault(emptyList())
                    }
                }.awaitAll()
            }.forEach { (loteId, pecasDoLote) ->
                val sub = subPorLote[loteId] ?: return@forEach
                pecasDoLote.forEach { p ->
                    if (p.idPeca in aindaFaltando) mapa[p.idPeca] = sub
                }
            }
            mapa
        } catch (e: Exception) {
            Log.e("AppViewModel", "resolverSubCategoriaDasPecas error: ${e.message}")
            mapa
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

    // Faixa de peças (primeiro/último número) por lote — carregada de forma leve,
    // sem precisar buscar a lista inteira de peças do lote.
    private val _faixaPecasPorLote = MutableStateFlow<Map<Long, Pair<String?, String?>>>(emptyMap())
    val faixaPecasPorLote: StateFlow<Map<Long, Pair<String?, String?>>> = _faixaPecasPorLote.asStateFlow()

    fun loadFaixaPecas(loteId: Long) {
        if (_faixaPecasPorLote.value.containsKey(loteId)) return
        viewModelScope.launch {
            try {
                val faixa = repository.getFaixaPecasDoLote(loteId)
                _faixaPecasPorLote.value = _faixaPecasPorLote.value + (loteId to faixa)
            } catch (e: Exception) {
                Log.e("AppViewModel", "loadFaixaPecas error: ${e.message}")
            }
        }
    }

    // Catálogo de jogos (Tbl_Jogos) - usado no cadastro manual de lote (ex: fornecedor AEC)
    private val _jogos = MutableStateFlow<List<com.example.data.model.JogoDTO>>(emptyList())
    val jogos: StateFlow<List<com.example.data.model.JogoDTO>> = _jogos.asStateFlow()

    fun loadJogos() {
        if (_jogos.value.isNotEmpty()) return
        viewModelScope.launch {
            try {
                _jogos.value = repository.getJogos()
            } catch (e: Exception) {
                Log.e("AppViewModel", "loadJogos error: ${e.message}")
            }
        }
    }

    fun criarLoteManual(
        categoriaId: Long,
        subCategoriaId: Long? = null,
        fornecedor: String,
        descricao: String?,
        dataEntrada: String?,
        pecas: List<com.example.data.model.PecaManualDTO>,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val request = com.example.data.model.LoteManualRequestDTO(
                categoriaId = categoriaId,
                subCategoriaId = subCategoriaId,
                fornecedor = fornecedor,
                descricao = descricao,
                dataEntrada = dataEntrada,
                pecas = pecas
            )
            val resultado = repository.criarLoteManual(request)
            if (resultado != null) {
                showNotification("Lote manual criado com sucesso.")
                _pecasDoLote.value = emptyMap()
                loadLotes()
            } else {
                showNotification("Erro ao criar lote manual.")
            }
            onResult(resultado != null)
        }
    }

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

    private suspend fun refreshPecasDoLote(loteId: Long) {
        try {
            val pecas = repository.getPecasDoLote(loteId)
            _pecasDoLote.value = _pecasDoLote.value + (loteId to pecas)
        } catch (e: Exception) {
            Log.e("AppViewModel", "refreshPecasDoLote error: ${e.message}")
        }
    }

    fun retirarPeca(idPeca: Long, loteId: Long, observacao: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val usuario = _usuarioLogado.value?.nome ?: _usuarioLogado.value?.username
            val sucesso = repository.retirarPeca(idPeca, observacao, usuario)
            if (sucesso) {
                showNotification("Peça devolvida ao estoque.")
                refreshPecasDoLote(loteId)
                loadLotes()
            } else {
                showNotification("Erro ao retirar peça.")
            }
            onResult(sucesso)
        }
    }

    fun descartarPeca(idPeca: Long, loteId: Long, observacao: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val usuario = _usuarioLogado.value?.nome ?: _usuarioLogado.value?.username
            val sucesso = repository.descartarPeca(idPeca, observacao, usuario)
            if (sucesso) {
                showNotification("Peça marcada como perda total (P.T.).")
                refreshPecasDoLote(loteId)
            } else {
                showNotification("Erro ao descartar peça.")
            }
            onResult(sucesso)
        }
    }

    fun criarLote(
        categoriaId: Long,
        subCategoriaId: Long? = null,
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
                subCategoriaId = subCategoriaId,
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
        _subCategoriaPorPeca.value = emptyMap()
        _subCategorias.value = emptyList()
    }

    fun performRegistrarExecucao(
        solicitacaoId: Long,
        execucoesPorProblema: Map<Long, Triple<String, List<Long>, String?>>,
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

        val lista = execucoesPorProblema.map { (problemaId, dados) ->
            com.example.data.model.ExecucaoRequestDTO(
                problemaId    = problemaId,
                solicitacaoId = solicitacaoId,
                dataExecucao  = agora,
                tecnico       = tecnico,
                descricao     = dados.first,
                pecasUsadas   = dados.second,
                fotoBase64    = dados.third
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

    // --- TROCA DE SENHA ---
    private val _trocarSenhaLoading = MutableStateFlow(false)
    val trocarSenhaLoading: StateFlow<Boolean> = _trocarSenhaLoading.asStateFlow()

    fun performTrocarSenha(
        username: String,
        senhaAtual: String,
        senhaNova: String,
        onResult: (sucesso: Boolean, mensagem: String) -> Unit
    ) {
        if (username.isBlank() || senhaAtual.isBlank() || senhaNova.isBlank()) {
            onResult(false, "Preencha usuário, senha atual e nova senha.")
            return
        }
        if (senhaNova.length < 4) {
            onResult(false, "A nova senha deve ter pelo menos 4 caracteres.")
            return
        }

        viewModelScope.launch {
            _trocarSenhaLoading.value = true
            try {
                val (sucesso, mensagem) = repository.trocarSenha(username.trim(), senhaAtual, senhaNova)
                onResult(sucesso, mensagem)
            } catch (e: Exception) {
                Log.e("AppViewModel", "performTrocarSenha error: ${e.message}")
                onResult(false, "Erro inesperado ao trocar a senha.")
            } finally {
                _trocarSenhaLoading.value = false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CONTROLE DE CHAVES
    // ─────────────────────────────────────────────────────────────

    private val _chaves = MutableStateFlow<List<com.example.data.model.ChaveDTO>>(emptyList())
    val chaves: StateFlow<List<com.example.data.model.ChaveDTO>> = _chaves.asStateFlow()

    private val _chavesLoading = MutableStateFlow(false)
    val chavesLoading: StateFlow<Boolean> = _chavesLoading.asStateFlow()

    private val _fornecedoresChave = MutableStateFlow<List<com.example.data.model.FornecedorChaveDTO>>(emptyList())
    val fornecedoresChave: StateFlow<List<com.example.data.model.FornecedorChaveDTO>> = _fornecedoresChave.asStateFlow()

    // Filtros da tela de chaves (ficam no ViewModel pra sobreviver a rotação de tela)
    private val _chaveFiltroNumero = MutableStateFlow("")
    val chaveFiltroNumero: StateFlow<String> = _chaveFiltroNumero.asStateFlow()

    private val _chaveFiltroFornecedorId = MutableStateFlow<Long?>(null)
    val chaveFiltroFornecedorId: StateFlow<Long?> = _chaveFiltroFornecedorId.asStateFlow()

    private val _chaveFiltroTipo = MutableStateFlow<String?>(null)
    val chaveFiltroTipo: StateFlow<String?> = _chaveFiltroTipo.asStateFlow()

    /** true = ativas, false = inativas, null = todas. */
    private val _chaveFiltroAtivo = MutableStateFlow<Boolean?>(true)
    val chaveFiltroAtivo: StateFlow<Boolean?> = _chaveFiltroAtivo.asStateFlow()

    fun setChaveFiltroNumero(valor: String) {
        _chaveFiltroNumero.value = valor
    }

    fun setChaveFiltroFornecedor(id: Long?) {
        _chaveFiltroFornecedorId.value = id
        loadChaves()
    }

    fun setChaveFiltroTipo(tipo: String?) {
        _chaveFiltroTipo.value = tipo
        loadChaves()
    }

    fun setChaveFiltroAtivo(ativo: Boolean?) {
        _chaveFiltroAtivo.value = ativo
        loadChaves()
    }

    fun limparFiltrosChave() {
        _chaveFiltroNumero.value = ""
        _chaveFiltroFornecedorId.value = null
        _chaveFiltroTipo.value = null
        _chaveFiltroAtivo.value = true
        loadChaves()
    }

    fun loadChaves() {
        viewModelScope.launch {
            _chavesLoading.value = true
            try {
                _chaves.value = repository.getChaves(
                    numero = _chaveFiltroNumero.value,
                    fornecedorId = _chaveFiltroFornecedorId.value,
                    tipo = _chaveFiltroTipo.value,
                    ativo = _chaveFiltroAtivo.value
                ).sortedWith { a, b -> compararNatural(a.codigo ?: "", b.codigo ?: "") }

            } catch (e: Exception) {
                Log.e("AppViewModel", "loadChaves error: ${e.message}")
            } finally {
                _chavesLoading.value = false
            }
        }
    }

    fun loadFornecedoresChave() {
        viewModelScope.launch {
            try {
                _fornecedoresChave.value = repository.getFornecedoresChave()
            } catch (e: Exception) {
                Log.e("AppViewModel", "loadFornecedoresChave error: ${e.message}")
            }
        }
    }

    /** id nulo = nova chave; id preenchido = edição. */
    fun salvarChave(
        id: Long?,
        request: com.example.data.model.ChaveRequestDTO,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _chavesLoading.value = true
            val resultado = repository.salvarChave(id, request)
            _chavesLoading.value = false
            if (resultado.sucesso) {
                showNotification("Chave ${resultado.chave?.codigo ?: ""} salva.")
                loadChaves()
                onResult(true)
            } else {
                showNotification(resultado.erro ?: "Erro ao salvar a chave.")
                onResult(false)
            }
        }
    }

    fun alterarAtivoChave(id: Long, ativo: Boolean) {
        viewModelScope.launch {
            val resultado = repository.alterarAtivoChave(id, ativo)
            if (resultado.sucesso) {
                showNotification(if (ativo) "Chave reativada." else "Chave desativada.")
                loadChaves()
            } else {
                showNotification(resultado.erro ?: "Erro ao alterar a chave.")
            }
        }
    }

    fun criarFornecedorChave(nome: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val resultado = repository.criarFornecedorChave(nome)
            if (resultado.sucesso) {
                showNotification("Fornecedor ${resultado.fornecedor?.nome ?: ""} criado.")
                loadFornecedoresChave()
                onResult(true)
            } else {
                showNotification(resultado.erro ?: "Erro ao criar o fornecedor.")
                onResult(false)
            }
        }
    }
    // ---------- VÍNCULO CHAVE ↔ MÁQUINA ----------
    // Estado fica no diálogo (é temporário); aqui só as chamadas.

    /** onResult(null) = falhou. */
    fun loadMaquinasDaChave(
        chaveId: Long,
        historico: Boolean,
        onResult: (List<com.example.data.model.VinculoChaveDTO>?) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repository.getMaquinasDaChave(chaveId, historico))
        }
    }

    fun loadPracasChave(onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getPracasChave())
        }
    }

    fun buscarMaquinasPorNumero(
        numero: String,
        praca: String?,
        onResult: (List<com.example.data.model.MaquinaOpcaoDTO>) -> Unit
    ) {
        viewModelScope.launch {
            val (lista, erro) = repository.buscarMaquinasPorNumero(numero, praca)
            if (erro != null) showNotification(erro)
            onResult(lista)
        }
    }

    fun vincularChave(
        maquinaId: Long,
        chaveId: Long,
        uso: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val resultado = repository.vincularChave(
                maquinaId,
                com.example.data.model.VinculoChaveRequestDTO(chaveId = chaveId, uso = uso, observacao = null)
            )
            if (resultado.sucesso) {
                val v = resultado.vinculo
                showNotification("Chave ${v?.chaveCodigo ?: ""} vinculada à máquina ${v?.praca ?: ""} - ${v?.maquinaNome?.trim() ?: ""}.")
                onResult(true)
            } else {
                showNotification(resultado.erro ?: "Erro ao vincular a chave.")
                onResult(false)
            }
        }
    }

    fun encerrarVinculoChave(id: Long, observacao: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val resultado = repository.encerrarVinculoChave(id, observacao)
            if (resultado.sucesso) {
                showNotification("Chave retirada da máquina (fica no histórico).")
                onResult(true)
            } else {
                showNotification(resultado.erro ?: "Erro ao retirar a chave.")
                onResult(false)
            }
        }
    }

    // Ordena texto com números do jeito "humano": CP4 < CP5 < CP10
    private fun compararNatural(a: String, b: String): Int {
        val regex = Regex("\\d+|\\D+")
        val pa = regex.findAll(a.uppercase()).map { it.value }.toList()
        val pb = regex.findAll(b.uppercase()).map { it.value }.toList()
        for (i in 0 until minOf(pa.size, pb.size)) {
            val x = pa[i]; val y = pb[i]
            val cmp = if (x[0].isDigit() && y[0].isDigit())
                x.toBigInteger().compareTo(y.toBigInteger())
            else x.compareTo(y)
            if (cmp != 0) return cmp
        }
        return pa.size - pb.size
    }
}
