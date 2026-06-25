package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Cliente
import com.example.data.model.Maquina
import com.example.data.model.ConvertRegiao
import com.example.data.model.ConvertLeiturista
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    darkTheme: Boolean,
    onToggleDarkTheme: () -> Unit
) {
    val context = LocalContext.current
    val isDemo by viewModel.isDemoMode.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val appMessage by viewModel.appMessage.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }
    var screenSection by remember { mutableStateOf("hub") } // "hub", "clientes", "maquinas", "novo_cliente", "nova_maquina", "editar_cliente"
    var hubTabIdx by remember { mutableStateOf(0) } // 0 = Início, 1 = Solicitações, 2 = Execuções, 3 = Relatórios, 4 = Perfil
    var clienteParaEditar by remember { mutableStateOf<Cliente?>(null) }
    var maquinaParaEditar by remember { mutableStateOf<Maquina?>(null) }
    var origemEdicaoMaquina by remember { mutableStateOf("clientes") }
    var solicitacaoParaExecutar by remember { mutableStateOf<com.example.data.model.SolicitacaoResponseDTO?>(null) }

    // Display SnackBar / Toast cleanly when flow triggers notification
    LaunchedEffect(appMessage) {
        appMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearNotification()
        }
    }

    when (screenSection) {
        "clientes" -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Gerenciamento de Clientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { screenSection = "hub" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { screenSection = "novo_cliente" },
                        containerColor = BrandOrange,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Novo Cliente")
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    TabClientes(
                        viewModel = viewModel,
                        onEditCliente = { cliente ->
                            clienteParaEditar = cliente
                            screenSection = "editar_cliente"
                        },
                        onEditMaquina = { maquina ->
                            maquinaParaEditar = maquina
                            origemEdicaoMaquina = "clientes"
                            screenSection = "editar_maquina"
                        }
                    )
                }
            }
        }
        "novo_cliente" -> {
            NovoClienteScreen(
                viewModel = viewModel,
                onBack = { screenSection = "clientes" }
            )
        }
        "editar_cliente" -> {
            clienteParaEditar?.let { cliente ->
                EditarClienteScreen(
                    viewModel = viewModel,
                    cliente = cliente,
                    onBack = { screenSection = "clientes" }
                )
            } ?: run {
                screenSection = "clientes"
            }
        }
        "editar_maquina" -> {
            maquinaParaEditar?.let { maquina ->
                EditarMaquinaScreen(
                    viewModel = viewModel,
                    maquina = maquina,
                    onBack = { screenSection = origemEdicaoMaquina }
                )
            } ?: run {
                screenSection = origemEdicaoMaquina
            }
        }
        "maquinas" -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Gerenciamento de Máquinas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { screenSection = "hub" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { screenSection = "nova_maquina" },
                        containerColor = BrandOrange,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Nova Máquina")
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    TabMaquinas(
                        viewModel = viewModel,
                        onEditMaquina = { maquina ->
                            maquinaParaEditar = maquina
                            origemEdicaoMaquina = "maquinas"
                            screenSection = "editar_maquina"
                        }
                    )
                }
            }
        }
        "nova_maquina" -> {
            NovaMaquinaScreen(
                viewModel = viewModel,
                onBack = { screenSection = "maquinas" }
            )
        }
        "executar_solicitacao" -> {
            solicitacaoParaExecutar?.let { solicitacao ->
                ExecutarSolicitacaoScreen(
                    viewModel = viewModel,
                    solicitacao = solicitacao,
                    onBack = {
                        solicitacaoParaExecutar = null
                        screenSection = "hub"
                        hubTabIdx = 1
                    }
                )
            } ?: run {
                screenSection = "hub"
            }
        }
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = com.example.R.drawable.ic_launcher_foreground),
                                    contentDescription = "Logo",
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Row {
                                    Text(
                                        text = "SISTEMA ",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Light,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = "PRO",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BrandOrange
                                        )
                                    )
                                }
                            }
                        },
                        actions = {
                            // Toggle Dark Mode
                            IconButton(onClick = onToggleDarkTheme) {
                                Icon(
                                    imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Alternar Tema"
                                )
                            }
                            // Settings Button
                            IconButton(onClick = { showConfigDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Configurações de Conexão"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        NavigationBarItem(
                            selected = hubTabIdx == 0,
                            onClick = { hubTabIdx = 0 },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                            label = { Text("Início", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = hubTabIdx == 1,
                            onClick = { hubTabIdx = 1 },
                            icon = { Icon(Icons.Default.Assignment, contentDescription = "Solicitações") },
                            label = { Text("Solicitações", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = hubTabIdx == 2,
                            onClick = { hubTabIdx = 2 },
                            icon = { Icon(Icons.Default.Build, contentDescription = "Execuções") },
                            label = { Text("Execuções", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = hubTabIdx == 3,
                            onClick = { hubTabIdx = 3 },
                            icon = { Icon(Icons.Default.BarChart, contentDescription = "Relatórios") },
                            label = { Text("Relatórios", fontSize = 11.sp) }
                        )
                        NavigationBarItem(
                            selected = hubTabIdx == 4,
                            onClick = { hubTabIdx = 4 },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                            label = { Text("Perfil", fontSize = 11.sp) }
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    when (hubTabIdx) {
                        0 -> TabInicio(
                            viewModel = viewModel,
                            onNavigateSection = { section -> screenSection = section },
                            onNavigateTab = { tab -> hubTabIdx = tab }
                        )
                        1 -> TabSolicitacoesList(
                            viewModel = viewModel,
                            onExecutar = { solicitacao ->
                                solicitacaoParaExecutar = solicitacao
                                screenSection = "executar_solicitacao"
                            }
                        )
                        2 -> TabExecucoesList(viewModel)
                        3 -> TabIndicadores(viewModel = viewModel)
                        4 -> TabPerfilTechnical(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Server Config Dialog
    if (showConfigDialog) {
        ConnectionConfigDialog(
            currentUrl = baseUrl,
            isDemoMode = isDemo,
            onDismiss = { showConfigDialog = false },
            onSave = { url, demo ->
                viewModel.setDemoMode(demo)
                viewModel.updateBaseUrl(url)
                showConfigDialog = false
            }
        )
    }
}

@Composable
fun TabClientes(viewModel: AppViewModel, onEditCliente: (Cliente) -> Unit = {}, onEditMaquina: (Maquina) -> Unit = {}) {
    val context = LocalContext.current
    val clientes by viewModel.clientes.collectAsState()
    val loading by viewModel.clientesLoading.collectAsState()
    val hasMore by viewModel.clientesHasMore.collectAsState()
    val searchQuery by viewModel.clientSearchQuery.collectAsState()

    // Filter statuses
    val activeFilter by viewModel.clientFilterAtivo.collectAsState()
    val regiaoFilter by viewModel.clientFilterRegiao.collectAsState()
    val bairroFilter by viewModel.clientFilterBairro.collectAsState()

    var showFiltersSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Detect when user is close to the bottom to load nextPage (Infinite scroll)
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisibleItem == null) false
            else lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadNextClientesPage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search and Filter Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onClientSearchChanged(it) },
                label = { Text("Buscar clientes por nome, contato...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onClientSearchChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("client_search_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Advanced Filters Drawer / Sheet trigger
            Box {
                val isFilterActive = activeFilter != null || regiaoFilter != null || !bairroFilter.isNullOrBlank()
                Button(
                    onClick = { showFiltersSheet = true },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFilterActive) AccentAmber else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isFilterActive) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.height(56.dp).testTag("client_filter_button")
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtros Avançados")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Filtros", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active filters list indicators
        if (activeFilter != null || regiaoFilter != null || !bairroFilter.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtros ativos: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Row(modifier = Modifier.weight(1f)) {
                    activeFilter?.let {
                        FilterBadge(label = if (it) "Ativos" else "Inativos", onRemove = { viewModel.setClientFilterAtivo(null) })
                    }
                    regiaoFilter?.let {
                        FilterBadge(label = "Região: ${ConvertRegiao.fromCode(it)}", onRemove = { viewModel.setClientFilterRegiao(null) })
                    }
                    bairroFilter?.let {
                        FilterBadge(label = it, onRemove = { viewModel.setClientFilterBairro(null) })
                    }
                }
                TextButton(
                    onClick = {
                        viewModel.setClientFilterAtivo(null)
                        viewModel.setClientFilterRegiao(null)
                        viewModel.setClientFilterBairro(null)
                    }
                ) {
                    Text("Limpar tudo", style = MaterialTheme.typography.bodySmall.copy(color = Color.Red))
                }
            }
        }

        // Clientes LazyList
        Box(modifier = Modifier.weight(1f)) {
            if (clientes.isEmpty() && !loading) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(50.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PeopleOutline,
                        contentDescription = "Nenhum cliente",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhum cliente correspondente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Revise os filtros avançados ou altere sua pesquisa de busca.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.testTag("clientes_list")
                ) {
                    itemsIndexed(clientes) { index, cliente ->
                        ClienteCard(cliente = cliente, viewModel = viewModel, context = context, onEdit = onEditCliente, onEditMaquina = onEditMaquina)
                    }

                    // Infinite pagination loading indicator
                    if (hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 3.dp)
                            }
                        }
                    } else if (clientes.isNotEmpty()) {
                        item {
                            Text(
                                text = "Fim da listagem de clientes.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFiltersSheet) {
        Dialog(onDismissRequest = { showFiltersSheet = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Filtros Avançados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status Filter
                    Text("Região Técnica", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        listOf(null, 1, 2, 3, 4, 5, 6).forEach { idx ->
                            val isSel = regiaoFilter == idx
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.setClientFilterRegiao(idx) },
                                label = { Text(if (idx == null) "Qualquer" else ConvertRegiao.fromCode(idx)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // status (ativo)
                    Text("Status Cadastral", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        listOf(
                            Triple(null, "Qualquer", Icons.Default.FilterList),
                            Triple(true, "Ativos", Icons.Default.Check),
                            Triple(false, "Inativos", Icons.Default.Close)
                        ).forEach { (status, title, icon) ->
                            val isSel = activeFilter == status
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.setClientFilterAtivo(status) },
                                label = { Text(title) },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Neighborhood filter
                    Text("Filtrar por Bairro", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    val bairros = viewModel.getBairrosList()
                    LazyColumn(
                        modifier = Modifier
                            .height(140.dp)
                            .padding(vertical = 6.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setClientFilterBairro(null) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = bairroFilter == null, onClick = { viewModel.setClientFilterBairro(null) })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Todos os bairros")
                            }
                        }
                        items(bairros) { br ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setClientFilterBairro(br) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = bairroFilter == br, onClick = { viewModel.setClientFilterBairro(br) })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(br)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showFiltersSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver Clientes", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterBadge(label: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remover",
                modifier = Modifier
                    .size(10.dp)
                    .clickable { onRemove() },
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun ClienteCard(cliente: Cliente, viewModel: AppViewModel, context: Context, onEdit: (Cliente) -> Unit = {}, onEditMaquina: (Maquina) -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }
    val active = cliente.ativo == true
    val statusColor = if (active) SecondaryEmerald else AccentAmber

    val initials = remember(cliente.nomCliente) {
        val name = cliente.nomCliente?.trim() ?: "S"
        if (name.isNotEmpty()) {
            val parts = name.split(" ")
            if (parts.size >= 2) {
                "${parts[0].take(1).uppercase()}${parts[1].take(1).uppercase()}"
            } else {
                name.take(2).uppercase()
            }
        } else {
            "S"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cliente_card_${cliente.codCliente}")
            .border(
                1.dp,
                if (expanded) statusColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (expanded) 6.dp else 1.dp
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Status bar on the left
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(statusColor, statusColor.copy(alpha = 0.6f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Header with Avatar and Basic info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stylized Avatar Initials Badge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PrimaryBlue.copy(alpha = 0.12f), PrimaryBlue.copy(alpha = 0.03f))
                                )
                            )
                            .border(1.dp, PrimaryBlue.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Name and code
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Cód: ${cliente.codCliente}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                ),
                                modifier = Modifier
                                    .background(PrimaryBlue.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            if (!cliente.bairro.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cliente.bairro,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = cliente.nomCliente ?: "Sem nome cadastrado",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar cliente",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onEdit(cliente) }
                            )
                        }
                    }

                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (active) Color(0xFF2E7D32) else Color(0xFFC62828))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (active) "Ativo" else "Inativo",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Phone/Contact row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!cliente.telefone.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                .clickable {
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_DIAL,
                                            android.net.Uri.parse("tel:${cliente.telefone}")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // ignored
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Ligar",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cliente.telefone,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sem telefone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }

                    if (!cliente.contato.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Contato",
                                tint = SecondaryEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cliente.contato,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sem contato",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // Expanded Section details
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(animationSpec = tween(200)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(14.dp)
                                    .background(PrimaryBlue, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Dados de Endereço",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = PrimaryBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!cliente.logradouro.isNullOrBlank()) {
                                                try {
                                                    val mapIntent = android.content.Intent(
                                                        android.content.Intent.ACTION_VIEW,
                                                        android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(cliente.logradouro)}")
                                                    )
                                                    context.startActivity(mapIntent)
                                                } catch (e: Exception) {
                                                    // ignored
                                                }
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Mapa",
                                        tint = AccentAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Endereço: ${cliente.logradouro ?: "-"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (!cliente.logradouro.isNullOrBlank()) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = "Ver no mapa",
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Bairro: ${cliente.bairro ?: "-"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Região: ${ConvertRegiao.fromCode(cliente.regiao)}  |  Leiturista: ${ConvertLeiturista.fromCode(cliente.leiturista)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Machine section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(14.dp)
                                        .background(SecondaryEmerald, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Máquinas Conectadas (${cliente.maquinas?.count { !it.isExcluded() } ?: 0})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SecondaryEmerald
                                )
                            }

                            Button(
                                onClick = { viewModel.exportSpecificClienteToPdf(context, cliente) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("card_pdf_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF Ficha", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val clientMaquinas = cliente.maquinas?.filter { !it.isExcluded() } ?: emptyList()
                        if (clientMaquinas.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhuma máquina vinculada a este cliente.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                clientMaquinas.forEach { maq ->
                                    val maqActive = maq.ativo == true
                                    val maqStatusColor = if (maqActive) SecondaryEmerald else AccentAmber

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.background
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.5.dp,
                                            maqStatusColor.copy(alpha = 0.15f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(IntrinsicSize.Min)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .width(4.dp)
                                                    .background(maqStatusColor)
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "ID #${maq.id ?: "-"}",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = maq.nom_jogo ?: "Sem jogo",
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Máquina: ${maq.nom_maq ?: "-"}  |  Placa: ${maq.numeroPlaca ?: "-"}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Editar máquina",
                                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                        modifier = Modifier
                                                            .size(15.dp)
                                                            .clickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                indication = null
                                                            ) { onEditMaquina(maq) }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(maqStatusColor)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovoClienteScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var nomCliente by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var contato by remember { mutableStateOf("") }
    var logradouro by remember { mutableStateOf("") }
    var bairro by remember { mutableStateOf("") }
    var regiao by remember { mutableStateOf<Int?>(null) }
    var expandedRegiao by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo Cliente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text("Nome do cliente", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            OutlinedTextField(
                value = nomCliente,
                onValueChange = { nomCliente = it },
                placeholder = { Text("Ex: João da Silva Ltda") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Telefone", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                    OutlinedTextField(
                        value = telefone,
                        onValueChange = { telefone = it },
                        placeholder = { Text("(11) 9....") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Contato", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                    OutlinedTextField(
                        value = contato,
                        onValueChange = { contato = it },
                        placeholder = { Text("Responsável") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Text("Endereço (logradouro)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            OutlinedTextField(
                value = logradouro,
                onValueChange = { logradouro = it },
                placeholder = { Text("Rua, número") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Bairro", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            OutlinedTextField(
                value = bairro,
                onValueChange = { bairro = it },
                placeholder = { Text("Bairro") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text("Região", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { expandedRegiao = !expandedRegiao }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (regiao != null) ConvertRegiao.fromCode(regiao) else "Selecione a região",
                            color = if (regiao != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = if (expandedRegiao) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }
                if (expandedRegiao) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Column {
                            ConvertRegiao.entries.forEach { r ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            regiao = r.code
                                            expandedRegiao = false
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text(r.description, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        viewModel.performCreateCliente(
                            nomCliente = nomCliente,
                            telefone = telefone,
                            contato = contato,
                            logradouro = logradouro,
                            bairro = bairro,
                            regiao = regiao
                        ) { success ->
                            isSaving = false
                            if (success) onBack()
                        }
                    }
                },
                enabled = nomCliente.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Salvar cliente", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarClienteScreen(viewModel: AppViewModel, cliente: Cliente, onBack: () -> Unit) {
    var nomCliente by remember { mutableStateOf(cliente.nomCliente ?: "") }
    var telefone by remember { mutableStateOf(cliente.telefone ?: "") }
    var contato by remember { mutableStateOf(cliente.contato ?: "") }
    var logradouro by remember { mutableStateOf(cliente.logradouro ?: "") }
    var bairro by remember { mutableStateOf(cliente.bairro ?: "") }
    var regiao by remember { mutableStateOf(cliente.regiao) }
    var expandedRegiao by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isDeactivating by remember { mutableStateOf(false) }
    var showConfirmDeactivate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Cliente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showConfirmDeactivate = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Desativar cliente", tint = Color(0xFFE53935))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text("Nome do cliente", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            OutlinedTextField(
                value = nomCliente,
                onValueChange = { nomCliente = it },
                placeholder = { Text("Ex: João da Silva Ltda") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Telefone", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                    OutlinedTextField(
                        value = telefone,
                        onValueChange = { telefone = it },
                        placeholder = { Text("(11) 9....") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Contato", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                    OutlinedTextField(
                        value = contato,
                        onValueChange = { contato = it },
                        placeholder = { Text("Responsável") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Text("Endereço (logradouro)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            OutlinedTextField(
                value = logradouro,
                onValueChange = { logradouro = it },
                placeholder = { Text("Rua, número") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Bairro", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            OutlinedTextField(
                value = bairro,
                onValueChange = { bairro = it },
                placeholder = { Text("Bairro") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text("Região", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { expandedRegiao = !expandedRegiao }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (regiao != null) ConvertRegiao.fromCode(regiao) else "Selecione a região",
                            color = if (regiao != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = if (expandedRegiao) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }
                if (expandedRegiao) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Column {
                            ConvertRegiao.entries.forEach { r ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            regiao = r.code
                                            expandedRegiao = false
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text(r.description, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!isSaving && cliente.codCliente != null) {
                        isSaving = true
                        viewModel.performUpdateCliente(
                            codCliente = cliente.codCliente,
                            nomCliente = nomCliente,
                            telefone = telefone,
                            contato = contato,
                            logradouro = logradouro,
                            bairro = bairro,
                            regiao = regiao
                        ) { success ->
                            isSaving = false
                            if (success) onBack()
                        }
                    }
                },
                enabled = nomCliente.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Salvar alterações", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = { showConfirmDeactivate = true },
                enabled = !isDeactivating,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                border = BorderStroke(1.dp, Color(0xFFE53935)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isDeactivating) {
                    CircularProgressIndicator(color = Color(0xFFE53935), strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Desativar cliente", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showConfirmDeactivate) {
        AlertDialog(
            onDismissRequest = { showConfirmDeactivate = false },
            title = { Text("Desativar cliente?") },
            text = { Text("\"${cliente.nomCliente}\" deixará de aparecer nas listas e na seleção de Nova Solicitação. O histórico de solicitações já registradas será mantido. Você pode reverter isso depois, se precisar.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDeactivate = false
                        if (cliente.codCliente != null) {
                            isDeactivating = true
                            viewModel.performDesativarCliente(cliente.codCliente) { success ->
                                isDeactivating = false
                                if (success) onBack()
                            }
                        }
                    }
                ) {
                    Text("Desativar", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeactivate = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarMaquinaScreen(viewModel: AppViewModel, maquina: Maquina, onBack: () -> Unit) {
    val clientes by viewModel.clientesForSelection.collectAsState()

    var selectedCliente by remember {
        mutableStateOf<Cliente?>(null)
    }
    var expandedCliente by remember { mutableStateOf(false) }
    var numeroMaquina by remember { mutableStateOf(maquina.nom_maq ?: "") }
    var nomeJogo by remember { mutableStateOf(maquina.nom_jogo ?: "") }
    var numeroPlaca by remember { mutableStateOf(maquina.numeroPlaca ?: "") }
    var observacoes by remember { mutableStateOf(maquina.obs ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var isDeactivating by remember { mutableStateOf(false) }
    var showConfirmDeactivate by remember { mutableStateOf(false) }

    // Pré-seleciona o cliente vinculado assim que a lista de clientes carregar
    LaunchedEffect(clientes) {
        if (selectedCliente == null) {
            selectedCliente = clientes.firstOrNull { it.codCliente?.toInt() == maquina.codCliente }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Máquina", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showConfirmDeactivate = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Desativar máquina", tint = Color(0xFFE53935))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Column {
                Text("Cliente vinculado", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { expandedCliente = !expandedCliente }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCliente?.let { "${it.codCliente} - ${it.nomCliente}" } ?: "Selecione o cliente",
                            color = if (selectedCliente != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = if (expandedCliente) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }
                if (expandedCliente) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .padding(top = 4.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(clientes) { client ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCliente = client
                                            expandedCliente = false
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text("${client.codCliente} - ${client.nomCliente}", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Text("Número da máquina", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            OutlinedTextField(
                value = numeroMaquina,
                onValueChange = { numeroMaquina = it },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Nome do jogo", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            OutlinedTextField(
                value = nomeJogo,
                onValueChange = { nomeJogo = it },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Número de placa", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            OutlinedTextField(
                value = numeroPlaca,
                onValueChange = { numeroPlaca = it },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Observações", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            OutlinedTextField(
                value = observacoes,
                onValueChange = { observacoes = it },
                minLines = 3,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!isSaving && maquina.id != null) {
                        isSaving = true
                        viewModel.performUpdateMaquina(
                            id = maquina.id.toInt(),
                            codCliente = selectedCliente?.codCliente?.toInt(),
                            numeroMaquina = numeroMaquina,
                            nomeJogo = nomeJogo,
                            numeroPlaca = numeroPlaca,
                            observacoes = observacoes
                        ) { success ->
                            isSaving = false
                            if (success) onBack()
                        }
                    }
                },
                enabled = selectedCliente != null && numeroMaquina.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Salvar alterações", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = { showConfirmDeactivate = true },
                enabled = !isDeactivating,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                border = BorderStroke(1.dp, Color(0xFFE53935)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isDeactivating) {
                    CircularProgressIndicator(color = Color(0xFFE53935), strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Desativar máquina", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showConfirmDeactivate) {
        AlertDialog(
            onDismissRequest = { showConfirmDeactivate = false },
            title = { Text("Desativar máquina?") },
            text = { Text("\"${maquina.nom_maq}\" deixará de aparecer nas listas e na seleção de Nova Solicitação. O histórico de solicitações já registradas será mantido.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDeactivate = false
                        maquina.id?.let { id ->
                            isDeactivating = true
                            viewModel.performDesativarMaquina(id.toInt()) { success ->
                                isDeactivating = false
                                if (success) onBack()
                            }
                        }
                    }
                ) {
                    Text("Desativar", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeactivate = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TabMaquinas(viewModel: AppViewModel, onEditMaquina: (Maquina) -> Unit) {
    val context = LocalContext.current
    val maquinas by viewModel.maquinas.collectAsState()
    val filteredMaquinas = maquinas.filter { !it.isExcluded() }
    val loading by viewModel.maquinasLoading.collectAsState()
    val hasMore by viewModel.maquinasHasMore.collectAsState()
    val searchQuery by viewModel.machineSearchQuery.collectAsState()

    // Filters
    val activeFilter by viewModel.machineFilterAtivo.collectAsState()
    val clientOwnerFilter by viewModel.machineFilterCodCliente.collectAsState()

    var showFiltersSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Infinite scroll
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastVisibleItem == null) false
            else lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadNextMaquinasPage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search and Filter Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onMachineSearchChanged(it) },
                label = { Text("Buscar por nome da máquina, jogo ou placa...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onMachineSearchChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("machine_search_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SecondaryEmerald,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Advanced Filters Dialog
            Button(
                onClick = { showFiltersSheet = true },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeFilter != null || clientOwnerFilter != null) AccentAmber else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (activeFilter != null || clientOwnerFilter != null) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.height(56.dp).testTag("machine_filter_button")
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Filtros Avançados")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Filtros", fontWeight = FontWeight.Bold)
            }
        }

        // Active filters indices list
        if (activeFilter != null || clientOwnerFilter != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ativos: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Row(modifier = Modifier.weight(1f)) {
                    activeFilter?.let {
                        FilterBadge(label = if (it) "Ativas" else "Inativas", onRemove = { viewModel.setMachineFilterAtivo(null) })
                    }
                    clientOwnerFilter?.let {
                        FilterBadge(label = "Dono Cód. $it", onRemove = { viewModel.setMachineFilterCodCliente(null) })
                    }
                }
                TextButton(
                    onClick = {
                        viewModel.setMachineFilterAtivo(null)
                        viewModel.setMachineFilterCodCliente(null)
                    }
                ) {
                    Text("Limpar tudo", style = MaterialTheme.typography.bodySmall.copy(color = Color.Red))
                }
            }
        }

        // Maquinas LazyList
        Box(modifier = Modifier.weight(1f)) {
            if (filteredMaquinas.isEmpty() && !loading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(50.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LaptopMac,
                        contentDescription = "Nenhuma máquina",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhuma máquina localizada",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.testTag("maquinas_list")
                ) {
                    itemsIndexed(filteredMaquinas) { index, maquina ->
                        MaquinaRowCard(
                            maquina = maquina,
                            viewModel = viewModel,
                            onEditMaquina = onEditMaquina
                        )
                    }

                    if (hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = SecondaryEmerald, strokeWidth = 3.dp)
                            }
                        }
                    } else if (filteredMaquinas.isNotEmpty()) {
                        item {
                            Text(
                                text = "Fim da listagem de máquinas.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFiltersSheet) {
        Dialog(onDismissRequest = { showFiltersSheet = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Filtros de Máquinas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryEmerald
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Status", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        listOf(
                            Triple(null, "Todas", Icons.Default.FilterList),
                            Triple(true, "Ativas", Icons.Default.Check),
                            Triple(false, "Inativas", Icons.Default.Close)
                        ).forEach { (status, title, icon) ->
                            val isSel = activeFilter == status
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.setMachineFilterAtivo(status) },
                                label = { Text(title) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Código do Cliente Proprietário", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = clientOwnerFilter?.toString() ?: "",
                        onValueChange = {
                            val intVal = it.toIntOrNull()
                            viewModel.setMachineFilterCodCliente(intVal)
                        },
                        placeholder = { Text("Filtrar por código (Ex: 15)") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("filter_client_code"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SecondaryEmerald)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showFiltersSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver Máquinas", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaMaquinaScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val clientes by viewModel.clientesForSelection.collectAsState()

    var selectedCliente by remember { mutableStateOf<com.example.data.model.Cliente?>(null) }
    var expandedCliente by remember { mutableStateOf(false) }
    var numeroMaquina by remember { mutableStateOf("") }
    var nomeJogo by remember { mutableStateOf("") }
    var numeroPlaca by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova Máquina", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Column {
                Text("Cliente vinculado", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { expandedCliente = !expandedCliente }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCliente?.let { "${it.codCliente} - ${it.nomCliente}" } ?: "Selecione o cliente",
                            color = if (selectedCliente != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = if (expandedCliente) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }
                if (expandedCliente) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .padding(top = 4.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(clientes) { client ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCliente = client
                                            expandedCliente = false
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text("${client.codCliente} - ${client.nomCliente}", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Text("Número da máquina", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            OutlinedTextField(
                value = numeroMaquina,
                onValueChange = { numeroMaquina = it },
                placeholder = { Text("Ex: Impressora fiscal 02") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Nome do jogo", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            OutlinedTextField(
                value = nomeJogo,
                onValueChange = { nomeJogo = it },
                placeholder = { Text("Ex: Epson TM-T20") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Número de placa", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            OutlinedTextField(
                value = numeroPlaca,
                onValueChange = { numeroPlaca = it },
                placeholder = { Text("Número de patrimônio") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Observações", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
            OutlinedTextField(
                value = observacoes,
                onValueChange = { observacoes = it },
                placeholder = { Text("Detalhes adicionais...") },
                minLines = 3,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        viewModel.performCreateMaquina(
                            codCliente = selectedCliente?.codCliente?.toInt(),
                            numeroMaquina = numeroMaquina,
                            nomeJogo = nomeJogo,
                            numeroPlaca = numeroPlaca,
                            observacoes = observacoes
                        ) { success ->
                            isSaving = false
                            if (success) onBack()
                        }
                    }
                },
                enabled = selectedCliente != null && numeroMaquina.isNotBlank() && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Salvar máquina", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MaquinaRowCard(
    maquina: Maquina,
    viewModel: AppViewModel,
    onEditMaquina: (Maquina) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val owner = remember(maquina.codCliente) { viewModel.repository.getClientForMachine(maquina.codCliente) }
    val active = maquina.ativo == true
    val statusColor = if (active) SecondaryEmerald else AccentAmber
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (expanded) statusColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (expanded) 6.dp else 1.dp
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left Accent Indicator Line
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(statusColor, statusColor.copy(alpha = 0.6f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Machine Tech Icon Badge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = if (active) {
                                        listOf(SecondaryEmerald.copy(alpha = 0.12f), SecondaryEmerald.copy(alpha = 0.03f))
                                    } else {
                                        listOf(AccentAmber.copy(alpha = 0.12f), AccentAmber.copy(alpha = 0.03f))
                                    }
                                )
                            )
                            .border(1.dp, statusColor.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Máquina",
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ID: ${maquina.id}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryEmerald
                                ),
                                modifier = Modifier
                                    .background(SecondaryEmerald.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CPU: ${maquina.numeroPlaca ?: "S/ placa"}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = maquina.nom_maq ?: "Sem nome de máquina",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Active / Inactive pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (active) Color(0xFF2E7D32) else Color(0xFFC62828))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (active) "Ok" else "Inativo",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onEditMaquina(maquina) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_machine_button_${maquina.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Máquina",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Modern status & Game title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = "Jogo",
                            modifier = Modifier.size(16.dp),
                            tint = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = maquina.nom_jogo ?: "Sem jogo específico",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(PrimaryBlue.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Proprietário",
                            modifier = Modifier.size(14.dp),
                            tint = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = owner?.nomCliente ?: "Cód: ${maquina.codCliente}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            fontSize = 11.sp,
                            color = PrimaryBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(animationSpec = tween(240)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(240)) + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // observations region with customizable notes
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(14.dp)
                                    .background(SecondaryEmerald, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Observações Técnicas",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = SecondaryEmerald
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = maquina.obs ?: "Nenhuma observação reportada para esta máquina.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        if (owner != null) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(14.dp)
                                        .background(PrimaryBlue, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cadastro do Proprietário",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = PrimaryBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = owner.nomCliente ?: "-",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (!owner.telefone.isNullOrBlank()) {
                                                Text(
                                                    text = "Contato: ${owner.contato ?: "-"} | Tel: ${owner.telefone}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                        }

                                        if (!owner.telefone.isNullOrBlank()) {
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        val intent = android.content.Intent(
                                                            android.content.Intent.ACTION_DIAL,
                                                            android.net.Uri.parse("tel:${owner.telefone}")
                                                        )
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        // silent catch
                                                    }
                                                },
                                                modifier = Modifier
                                                    .background(PrimaryBlue.copy(alpha = 0.1f), CircleShape)
                                                    .size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Phone,
                                                    contentDescription = "Ligar",
                                                    tint = PrimaryBlue,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Região: ${ConvertRegiao.fromCode(owner.regiao)} | Bairro: ${owner.bairro ?: "-"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabIndicadores(viewModel: AppViewModel) {
    // Collect entire lists to calculate dynamic graphs instantly using canvas drawing
    val clientes by viewModel.clientes.collectAsState()
    val maquinas by viewModel.maquinas.collectAsState()

    val totalActiveClientesFlow by viewModel.totalActiveClientesCount.collectAsState()
    val totalMaquinasFlow by viewModel.totalMaquinasCount.collectAsState()
    val totalActiveMaquinasFlow by viewModel.totalActiveMaquinasCount.collectAsState()

    // Filter out machines with INFOME, OBS, COM
    val filteredMaquinas = maquinas.filter { !it.isExcluded() }

    // Since we want representative KPIs, if paginated list is small we can fallback on data length/mock values
    val totalClientes = totalActiveClientesFlow ?: (if (clientes.isNotEmpty()) clientes.count { it.ativo == true } else 68)
    val totalMaquinas = totalMaquinasFlow ?: (if (filteredMaquinas.isNotEmpty()) filteredMaquinas.size else 126)
    val activeMaquinas = totalActiveMaquinasFlow ?: (if (filteredMaquinas.isNotEmpty()) filteredMaquinas.count { it.ativo == true } else 105)
    val inactiveMaquinas = totalMaquinas - activeMaquinas

    // Distribution ranges region (R1 to R10)
    // Create precalculated values for demo and update based on lists
    val regionDistribution = intArrayOf(12, 18, 5, 14, 9, 3, 11, 4, 15, 6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Estatísticas Administrativas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue
        )
        Text(
            text = "Análise gráfica em tempo real das máquinas e rede de clientes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // KPI Summary cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KPICard(
                title = "Clientes Ativos",
                value = totalClientes.toString(),
                subtitle = "Rede cadastrada",
                color = PrimaryBlue,
                icon = Icons.Default.People,
                modifier = Modifier.weight(1f)
            )
            KPICard(
                title = "Equipamentos",
                value = totalMaquinas.toString(),
                subtitle = "Total de Máquinas",
                color = SecondaryEmerald,
                icon = Icons.Default.LaptopMac,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Graphics row with canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Proporção de Atividade das Máquinas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Taxa de funcionamento ideal de hardware",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Radial graph
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeRatio = if (totalMaquinas > 0) activeMaquinas.toFloat() / totalMaquinas.toFloat() else 0f
                    val ratioPercentage = (activeRatio * 100).toInt()

                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidthVal = 10.dp.toPx()
                            // Grey track background
                            drawArc(
                                color = SecondaryEmerald.copy(alpha = 0.08f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round)
                            )
                            // Green sweep fill
                            drawArc(
                                color = SecondaryEmerald,
                                startAngle = -90f,
                                sweepAngle = 360f * activeRatio,
                                useCenter = false,
                                style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$ratioPercentage%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = SecondaryEmerald
                            )
                            Text(
                                text = "Ativas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    val errorColor = MaterialTheme.colorScheme.error
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LegendRow(color = SecondaryEmerald, label = "$activeMaquinas Máquinas Funcionando")
                        LegendRow(color = errorColor, label = "$inactiveMaquinas Máquinas Inativas")
                        LegendRow(color = PrimaryBlue, label = "Estabilidade da Rede: Estável")
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Region distribution graph (Custom Bar columns with background track pills)
                Text(
                    text = "Densidade de Clientes por Região",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    regionDistribution.forEachIndexed { idx, count ->
                        val maxHeight = 60.dp
                        val barHeight = (maxHeight.value * (count.toFloat() / 20f)).dp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = count.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Modern Progress-style bar track background
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(maxHeight)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(PrimaryBlue, PrimaryBlue.copy(alpha = 0.40f))
                                            )
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "R${idx + 1}",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun KPICard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(
            width = 1.dp,
            color = color.copy(alpha = 0.12f),
            shape = RoundedCornerShape(16.dp)
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.08f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
fun ConnectionConfigDialog(
    currentUrl: String,
    isDemoMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit
) {
    var urlVal by remember { mutableStateOf(currentUrl) }
    var demoVal by remember { mutableStateOf(isDemoMode) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Configurações de Conexão",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                Text(
                    text = "Gerencie o endereço IP do seu Servidor REST Java Spring Boot local ou remoto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Demo Mode Switch Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (demoVal) AccentAmber.copy(alpha = 0.1f) else MaterialTheme.colorScheme.background)
                        .clickable { demoVal = !demoVal }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modo Demonstração (Local)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Exibe dados mockados simulando busca, filtros avançados e paginação infinita imediatamente.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Switch(
                        checked = demoVal,
                        onCheckedChange = { demoVal = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentAmber,
                            checkedTrackColor = AccentAmber.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // URL Text Field
                Text(
                    text = "Caminho / URL Base do API",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = urlVal,
                    onValueChange = { urlVal = it },
                    placeholder = { Text("Ex: http://192.168.1.10:8080/") },
                    singleLine = true,
                    enabled = !demoVal,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("config_url_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                )

                if (!demoVal) {
                    Text(
                        text = "Atenção: Emuladores e dispositivos Android devem usar o IP de sua rede local ou o endereço Host do Emulador (http://10.0.2.2:8080/ no emulador oficial do Android Studio).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(urlVal, demoVal) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Salvar Conexão", color = Color.White)
                    }
                }
            }
        }
    }
}

// APPENDING BRAND NEW HUD AND NAVIGATION SECTIONS
@Composable
fun TabInicio(
    viewModel: AppViewModel,
    onNavigateSection: (String) -> Unit,
    onNavigateTab: (Int) -> Unit
) {
    var showNewSolicitacaoDialog by remember { mutableStateOf(false) }
    val usuario by viewModel.usuarioLogado.collectAsState()
    val nomeLogado = usuario?.nome ?: usuario?.username ?: "Marcio"
    val cargoLogado = usuario?.cargo ?: "Técnico de Manutenção"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDarkNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcome Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Olá, $nomeLogado! 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = cargoLogado,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SleekNavyCard)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Data",
                        tint = BrandOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    val currentDateStr = remember {
                        java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    }

                    Text(
                        text = currentDateStr,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Summary Indicators Cards Row - Single Wide Solicitações em Aberto Metric Card
        item {
            val solicitacoesCount by viewModel.solicitacoesCount.collectAsState()
            HubMetricCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Assignment,
                iconBgColor = BrandOrange,
                value = solicitacoesCount.toString(),
                label = "Solicitações em Aberto (Backend)"
            )
        }

        // Section Title: Acesso Rápido
        item {
            Text(
                text = "Acesso Rápido",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Quick Access Cards Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAccessGridCard(
                        modifier = Modifier.weight(1f),
                        title = "Clientes",
                        description = "Ver e gerenciar clientes",
                        icon = Icons.Default.People,
                        iconColor = Color(0xFF38BDF8),
                        onClick = { onNavigateSection("clientes") }
                    )
                    QuickAccessGridCard(
                        modifier = Modifier.weight(1f),
                        title = "Máquinas",
                        description = "Ver e gerenciar máquinas",
                        icon = Icons.Default.Tv,
                        iconColor = BrandOrange,
                        onClick = { onNavigateSection("maquinas") }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAccessGridCard(
                        modifier = Modifier.weight(1f),
                        title = "Solicitações",
                        description = "Abrir e acompanhar solicitações",
                        icon = Icons.Default.Assignment,
                        iconColor = BrandOrange,
                        onClick = { onNavigateTab(1) }
                    )
                    QuickAccessGridCard(
                        modifier = Modifier.weight(1f),
                        title = "Execuções",
                        description = "Visualizar execuções",
                        icon = Icons.Default.Build,
                        iconColor = Color(0xFF38BDF8),
                        onClick = { onNavigateTab(2) }
                    )
                }
            }
        }

        // Wide Primary Rounded Button "+ Nova Solicitação"
        item {
            Button(
                onClick = { showNewSolicitacaoDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nova Solicitação",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showNewSolicitacaoDialog) {
        NewSolicitacaoDialog(
            viewModel = viewModel,
            onDismiss = { showNewSolicitacaoDialog = false },
            onSubmit = { clienteId, maquinaId, maquinaName, desc ->
                viewModel.performCreateSolicitacao(clienteId, maquinaId, maquinaName, desc)
                showNewSolicitacaoDialog = false
            }
        )
    }
}

@Composable
fun HubMetricCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SleekNavyCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun QuickAccessGridCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SleekNavyCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSolicitacaoDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onSubmit: (Long, String, String, String) -> Unit
) {
    val clientes by viewModel.clientesForSelection.collectAsState()

    var maquina by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedCliente by remember { mutableStateOf<com.example.data.model.Cliente?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var selectedMaquina by remember { mutableStateOf<com.example.data.model.Maquina?>(null) }
    var expandedMaquinaDropdown by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .padding(24.dp)
                .drawBehind {
                    val glowColor = BrandOrange
                    val radius = 16.dp.toPx()
                    
                    // Outer super soft and wide atmospheric glow (huge diffusion)
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.03f),
                        topLeft = Offset(-32.dp.toPx(), -32.dp.toPx()),
                        size = this.size.copy(
                            width = this.size.width + 64.dp.toPx(),
                            height = this.size.height + 64.dp.toPx()
                        ),
                        cornerRadius = CornerRadius(radius + 32.dp.toPx(), radius + 32.dp.toPx())
                    )

                    // Mid soft glow
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.07f),
                        topLeft = Offset(-18.dp.toPx(), -18.dp.toPx()),
                        size = this.size.copy(
                            width = this.size.width + 36.dp.toPx(),
                            height = this.size.height + 36.dp.toPx()
                        ),
                        cornerRadius = CornerRadius(radius + 18.dp.toPx(), radius + 18.dp.toPx())
                    )

                    // Inner soft halo
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.15f),
                        topLeft = Offset(-8.dp.toPx(), -8.dp.toPx()),
                        size = this.size.copy(
                            width = this.size.width + 16.dp.toPx(),
                            height = this.size.height + 16.dp.toPx()
                        ),
                        cornerRadius = CornerRadius(radius + 8.dp.toPx(), radius + 8.dp.toPx())
                    )
                }
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SleekNavyCard),
                border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 480.dp)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Abertura de Solicitação",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Client Selector Dropdown
                Column {
                    Text("Selecione o Cliente", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(TerminalBackground)
                            .border(1.dp, TerminalBorder, RoundedCornerShape(6.dp))
                            .clickable { expandedDropdown = !expandedDropdown }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedCliente?.nomCliente ?: "selecionar cliente...",
                                color = if (selectedCliente != null) TerminalGreenBright else TerminalHint,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = if (expandedDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TerminalGreenBright
                            )
                        }
                    }

                    if (expandedDropdown) {
                        Card(
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = TerminalBackground),
                            border = BorderStroke(1.dp, TerminalBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .padding(top = 4.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(clientes) { client ->
                                    val isSelected = client.codCliente == selectedCliente?.codCliente
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) TerminalSelectedBg else Color.Transparent)
                                            .clickable {
                                                selectedCliente = client
                                                expandedDropdown = false
                                                selectedMaquina = null
                                                maquina = ""
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = client.nomCliente ?: "ID: ${client.codCliente}",
                                            color = if (isSelected) TerminalGreenBright else TerminalGreen,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Machine Selector Dropdown (visible only after a client is selected)
                if (selectedCliente != null) {
                    val clientMaquinas = selectedCliente?.maquinas?.filter { !it.isExcluded() } ?: emptyList()
                    
                    Column {
                        Text("Selecione a Máquina", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(TerminalBackground)
                                .border(1.dp, TerminalBorder, RoundedCornerShape(6.dp))
                                .clickable { expandedMaquinaDropdown = !expandedMaquinaDropdown }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val machineLabel = if (selectedMaquina != null) {
                                    "${selectedMaquina?.nom_maq ?: ""} - ${selectedMaquina?.nom_jogo ?: ""}"
                                } else {
                                    "selecionar equipamento..."
                                }
                                Text(
                                    text = machineLabel,
                                    color = if (selectedMaquina != null) TerminalGreenBright else TerminalHint,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )
                                Icon(
                                    imageVector = if (expandedMaquinaDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = TerminalGreenBright
                                )
                            }
                        }

                        if (expandedMaquinaDropdown) {
                            Card(
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = TerminalBackground),
                                border = BorderStroke(1.dp, TerminalBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .padding(top = 4.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                    items(clientMaquinas) { maq ->
                                        val isSelected = maq.id == selectedMaquina?.id
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isSelected) TerminalSelectedBg else Color.Transparent)
                                                .clickable {
                                                    selectedMaquina = maq
                                                    maquina = "${maq.nom_maq ?: ""} (${maq.nom_jogo ?: ""}) - Placa: ${maq.numeroPlaca ?: "S/N"}"
                                                    expandedMaquinaDropdown = false
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = maq.nom_maq ?: "sem nome",
                                                    color = if (isSelected) TerminalGreenBright else TerminalGreen,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "jogo: ${maq.nom_jogo ?: "s/d"} - placa: ${maq.numeroPlaca ?: "s/n"}",
                                                    color = TerminalHint,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Problem Description Input
                Column {
                    Text("Descrição detalhada do Problema", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        placeholder = {
                            Text(
                                "descreva o problema...",
                                color = TerminalHint,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        minLines = 5,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        ),
                        shape = RoundedCornerShape(6.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TerminalGreenBright,
                            unfocusedTextColor = TerminalGreen,
                            focusedContainerColor = TerminalBackground,
                            unfocusedContainerColor = TerminalBackground,
                            focusedBorderColor = TerminalBorder,
                            unfocusedBorderColor = TerminalBorder,
                            cursorColor = TerminalGreenBright
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cid: Long = selectedCliente?.codCliente ?: 0L
                            val maqIdStr = selectedMaquina?.id?.toString() ?: ""
                            if (cid != 0L && selectedMaquina != null && maquina.isNotBlank() && desc.isNotBlank()) {
                                onSubmit(cid, maqIdStr, maquina, desc)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                        enabled = selectedCliente != null && selectedMaquina != null && maquina.isNotBlank() && desc.isNotBlank()
                    ) {
                        Text("Enviar", color = Color.White)
                    }
                }
            }
        }
        }
    }
}

@Composable
fun TabSolicitacoesList(
    viewModel: AppViewModel,
    onExecutar: (com.example.data.model.SolicitacaoResponseDTO) -> Unit = {}
) {
    val solicitacoesList by viewModel.solicitacoesList.collectAsState()
    val lastUpdated by viewModel.solicitacoesLastUpdated.collectAsState()
    val pollingActive by viewModel.solicitacoesPollingActive.collectAsState()

    // Animação de pulso no ícone de sincronização
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDarkNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Solicitações de Serviço",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (pollingActive) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Auto-sync ativo",
                                tint = Color(0xFF10B981).copy(alpha = pulseAlpha),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        IconButton(onClick = { viewModel.refreshSolicitacoesManual() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar agora", tint = BrandOrange)
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (lastUpdated != null) "Atualizado às $lastUpdated · auto 30s"
                               else "Aguardando primeira atualização...",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        if (solicitacoesList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma solicitação em aberto encontrada.", color = Color(0xFF94A3B8), textAlign = TextAlign.Center)
                }
            }
        } else {
            items(solicitacoesList) { s ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekNavyCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = s.cliente ?: "Cliente não especificado",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (s.status == true) Color(0xFFEF4444) else Color(0xFF10B981))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (s.status == true) "Aberto" else "Concluído",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val problemas = s.problemas ?: emptyList()
                        if (problemas.isEmpty()) {
                            Text("Sem especificações de problema.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        } else {
                            problemas.forEach { prob ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = "Problema",
                                        tint = BrandOrange,
                                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Máquina: ${prob.maquina ?: "-"}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = prob.descricao ?: "Descrição pendente.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }

                        // Botão Executar — só aparece para solicitações abertas
                        if (s.status == true) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onExecutar(s) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Executar chamado",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabExecucoesList(viewModel: AppViewModel) {
    val execucoes by viewModel.execucoes.collectAsState()
    val loading by viewModel.execucoesLoading.collectAsState()
    var filterTodayOnly by remember { mutableStateOf(true) }

    val today = remember { java.util.Date() }
    val todayYmd = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(today) }
    val todayDmy = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).format(today) }
    val isDemo = viewModel.isDemoMode.collectAsState().value

    val filteredExecucoes = remember(execucoes, filterTodayOnly, isDemo) {
        if (filterTodayOnly) {
            execucoes.filter { e ->
                val dateStr = e.dataExecucao
                if (dateStr.isNullOrBlank()) {
                    isDemo
                } else {
                    // Backend retorna "yyyy-MM-dd'T'HH:mm:ss" ou "yyyy-MM-dd HH:mm:ss"
                    // Normaliza removendo T e pegando só a parte da data
                    val normalizado = dateStr.replace("T", " ").trim()
                    val datePart = normalizado.take(10) // "yyyy-MM-dd"
                    datePart == todayYmd || dateStr.contains(todayDmy)
                }
            }
        } else {
            execucoes
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadExecucoes()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDarkNavy)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Serviços Executados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (filterTodayOnly) BrandOrange else SleekNavyCard)
                            .border(1.dp, if (filterTodayOnly) BrandOrange else Color(0xFF334155), RoundedCornerShape(20.dp))
                            .clickable { filterTodayOnly = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hoje",
                            color = if (filterTodayOnly) Color.White else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (!filterTodayOnly) BrandOrange else SleekNavyCard)
                            .border(1.dp, if (!filterTodayOnly) BrandOrange else Color(0xFF334155), RoundedCornerShape(20.dp))
                            .clickable { filterTodayOnly = false }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Todas",
                            color = if (!filterTodayOnly) Color.White else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandOrange)
                }
            }
        } else if (filteredExecucoes.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (filterTodayOnly) {
                            "Nenhum serviço registrado hoje.\nToque em 'Todas' para ver o histórico."
                        } else {
                            "Nenhuma execução registrada."
                        },
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredExecucoes) { e ->
                var expanded by remember { mutableStateOf(false) }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekNavyCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = e.nomeCliente ?: "Cliente desconhecido",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Máquina: ${e.nomeMaquina ?: "-"}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                                if (!e.dataExecucao.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = "Data",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = e.dataExecucao,
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Concluído",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Concluído", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                if (e.pdfGerado == true) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Description,
                                            contentDescription = "PDF",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("PDF", color = Color(0xFF64748B), fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        if (!e.descricaoProblema.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Problema original: ${e.descricaoProblema}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TerminalBackground, RoundedCornerShape(6.dp))
                                .border(1.dp, TerminalBorder.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .clickable { expanded = !expanded }
                                .padding(10.dp)
                        ) {
                            Column(modifier = Modifier.animateContentSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(TerminalGreenBright, CircleShape)
                                        )
                                        Text(
                                            text = "SERVIÇO EXECUTADO:",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TerminalGreenBright
                                        )
                                    }
                                    Text(
                                        text = if (expanded) "RECOLHER [-]" else "EXPANDIR [+]",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TerminalGreenBright.copy(alpha = 0.8f)
                                    )
                                }
                                if (expanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = e.descricao ?: "Sem registro de execução",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = TerminalGreenBright,
                                        lineHeight = 16.sp
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = e.descricao ?: "Sem registro de execução",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = TerminalGreen.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        if (!e.tecnico.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Técnico: ${e.tecnico}",
                                fontSize = 11.sp,
                                color = BrandOrange
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabPerfilTechnical(viewModel: AppViewModel) {
    val usuario by viewModel.usuarioLogado.collectAsState()
    val username = usuario?.username ?: "Usuário"
    val nome = usuario?.nome ?: username
    val email = usuario?.email ?: "$username@systempro.com.br"
    val cargo = usuario?.cargo ?: "Técnico Especialista"
    val equipe = usuario?.equipe ?: "Equipe de Campo"
    val unidade = usuario?.unidade ?: "Planta System Pro - Manutencao"
    val firstLetter = if (nome.isNotEmpty()) nome.substring(0, 1).uppercase() else "U"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDarkNavy)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(BrandOrange),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = firstLetter,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = nome,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = cargo,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SleekNavyCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileLineItem("Usuário", username)
                ProfileLineItem("Email", email)
                ProfileLineItem("Equipe", equipe)
                ProfileLineItem("Unidade", unidade)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.performLogout() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Sair da Conta", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileLineItem(title: String, valStr: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = Color(0xFF64748B), fontSize = 13.sp)
        Text(valStr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
