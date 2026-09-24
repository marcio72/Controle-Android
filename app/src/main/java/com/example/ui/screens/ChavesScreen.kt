package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ChaveDTO
import com.example.data.model.ChaveRequestDTO
import com.example.data.model.FornecedorChaveDTO
import com.example.data.model.MaquinaOpcaoDTO
import com.example.data.model.TipoChaveApp
import com.example.data.model.UsoChaveApp
import com.example.data.model.VinculoChaveDTO
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

// ─────────────────────────────────────────────────────────────
// TELA DE CONTROLE DE CHAVES
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChavesScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val chaves by viewModel.chaves.collectAsState()
    val isLoading by viewModel.chavesLoading.collectAsState()
    val fornecedores by viewModel.fornecedoresChave.collectAsState()

    val filtroNumero by viewModel.chaveFiltroNumero.collectAsState()
    val filtroFornecedorId by viewModel.chaveFiltroFornecedorId.collectAsState()
    val filtroTipo by viewModel.chaveFiltroTipo.collectAsState()
    val filtroAtivo by viewModel.chaveFiltroAtivo.collectAsState()

    var chaveEmEdicao by remember { mutableStateOf<ChaveDTO?>(null) }
    var mostrarForm by remember { mutableStateOf(false) }
    var mostrarNovoFornecedor by remember { mutableStateOf(false) }
    var chaveMaquinas by remember { mutableStateOf<ChaveDTO?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadFornecedoresChave()
        viewModel.loadChaves()
    }

    val totalCopias = chaves.sumOf { it.quantidadeCopias ?: 0 }
    val totalCadeados = chaves.sumOf { it.quantidadeCadeados ?: 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chaves", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarNovoFornecedor = true }) {
                        Icon(Icons.Default.Business, contentDescription = "Novo fornecedor")
                    }
                    IconButton(onClick = { viewModel.loadChaves() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
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
                onClick = {
                    chaveEmEdicao = null
                    mostrarForm = true
                },
                containerColor = BrandOrange,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova chave")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // ---------- FILTROS ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = filtroNumero,
                    onValueChange = { viewModel.setChaveFiltroNumero(it) },
                    label = { Text("Número da chave") },
                    placeholder = { Text("Ex: 1234") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (filtroNumero.isNotBlank()) {
                            IconButton(onClick = {
                                viewModel.setChaveFiltroNumero("")
                                viewModel.loadChaves()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Search
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Tipos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChaveChip(
                        texto = "Todos",
                        selecionado = filtroTipo == null,
                        onClick = { viewModel.setChaveFiltroTipo(null) }
                    )
                    TipoChaveApp.values().forEach { tipo ->
                        ChaveChip(
                            texto = tipo.descricao,
                            selecionado = filtroTipo == tipo.codigo,
                            onClick = { viewModel.setChaveFiltroTipo(tipo.codigo) }
                        )
                    }
                }

                // Status + fornecedor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChaveChip(
                        texto = "Ativas",
                        selecionado = filtroAtivo == true,
                        onClick = { viewModel.setChaveFiltroAtivo(true) }
                    )
                    ChaveChip(
                        texto = "Inativas",
                        selecionado = filtroAtivo == false,
                        onClick = { viewModel.setChaveFiltroAtivo(false) }
                    )
                    ChaveChip(
                        texto = "Todas",
                        selecionado = filtroAtivo == null,
                        onClick = { viewModel.setChaveFiltroAtivo(null) }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    SeletorFornecedor(
                        fornecedores = fornecedores,
                        selecionadoId = filtroFornecedorId,
                        rotuloVazio = "Fornecedor",
                        permitirTodos = true,
                        onSelecionar = { viewModel.setChaveFiltroFornecedor(it) }
                    )
                }

                Button(
                    onClick = { viewModel.loadChaves() },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buscar")
                }
            }

            // ---------- RESUMO ----------
            if (chaves.isNotEmpty()) {
                Text(
                    text = "${chaves.size} chave(s) · $totalCopias Qtd. Chaves(s) · $totalCadeados Qtd. Cadeado(s)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ---------- LISTA ----------
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && chaves.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = BrandOrange
                        )
                    }
                    chaves.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Nenhuma chave encontrada com esses filtros.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(bottom = 96.dp, top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(chaves, key = { it.id ?: 0L }) { chave ->
                                ChaveCard(
                                    chave = chave,
                                    onEditar = {
                                        chaveEmEdicao = chave
                                        mostrarForm = true
                                    },
                                    onAlternarAtivo = {
                                        chave.id?.let { viewModel.alterarAtivoChave(it, !(chave.ativo ?: true)) }
                                    },
                                    onMaquinas = { chaveMaquinas = chave }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarForm) {
        ChaveFormDialog(
            chave = chaveEmEdicao,
            fornecedores = fornecedores,
            onNovoFornecedor = { mostrarNovoFornecedor = true },
            onDismiss = { mostrarForm = false },
            onSalvar = { id, request ->
                viewModel.salvarChave(id, request) { ok ->
                    if (ok) mostrarForm = false
                }
            }
        )
    }

    chaveMaquinas?.let { chave ->
        MaquinasDaChaveDialog(
            chave = chave,
            viewModel = viewModel,
            onDismiss = { chaveMaquinas = null }
        )
    }

    if (mostrarNovoFornecedor) {
        NovoFornecedorDialog(
            onDismiss = { mostrarNovoFornecedor = false },
            onSalvar = { nome ->
                viewModel.criarFornecedorChave(nome) { ok ->
                    if (ok) mostrarNovoFornecedor = false
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// CARD DA CHAVE
// ─────────────────────────────────────────────────────────────
@Composable
private fun ChaveCard(
    chave: ChaveDTO,
    onEditar: () -> Unit,
    onAlternarAtivo: () -> Unit,
    onMaquinas: () -> Unit
) {
    val ativa = chave.ativo ?: true
    val corTipo = when (chave.tipo) {
        "C" -> AccentGold
        "T" -> SecondaryEmerald
        "D" -> Color(0xFF22C55E)
        else -> Color(0xFF94A3B8)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEditar() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chave.codigo ?: "-",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = if (ativa) BrandOrange else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(corTipo.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = chave.tipoDescricao ?: "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = corTipo
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!ativa) {
                    Text("INATIVA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF4D4D))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = chave.fornecedorNome ?: "-",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoQuantidade(
                    rotulo = "Qtd. Chaves",
                    valor = chave.quantidadeCopias ?: 0,
                    icone = Icons.Default.VpnKey,
                    modifier = Modifier.weight(1f)
                )
                CampoQuantidade(
                    rotulo = "Qtd. Cadeados",
                    valor = chave.quantidadeCadeados ?: 0,
                    icone = Icons.Default.Lock,
                    modifier = Modifier.weight(1f)
                )
            }

            val obs = chave.observacao
            if (!obs.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = obs,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar", fontSize = 12.sp)
                }
                TextButton(onClick = onAlternarAtivo) {
                    Icon(
                        if (ativa) Icons.Default.Block else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (ativa) "Desativar" else "Ativar", fontSize = 12.sp)
                }
                TextButton(onClick = onMaquinas) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Máquinas", fontSize = 12.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// FORMULÁRIO (NOVA / EDIÇÃO)
// ─────────────────────────────────────────────────────────────
@Composable
private fun ChaveFormDialog(
    chave: ChaveDTO?,
    fornecedores: List<FornecedorChaveDTO>,
    onNovoFornecedor: () -> Unit,
    onDismiss: () -> Unit,
    onSalvar: (Long?, ChaveRequestDTO) -> Unit
) {
    var numero by remember { mutableStateOf(chave?.numero ?: "") }
    var tipo by remember { mutableStateOf(TipoChaveApp.porCodigo(chave?.tipo)) }
    var fornecedorId by remember { mutableStateOf(chave?.fornecedorId) }
    var copias by remember { mutableStateOf((chave?.quantidadeCopias ?: 0).toString()) }
    var cadeados by remember { mutableStateOf((chave?.quantidadeCadeados ?: 0).toString()) }
    var observacao by remember { mutableStateOf(chave?.observacao ?: "") }
    var ativo by remember { mutableStateOf(chave?.ativo ?: true) }
    var erroLocal by remember { mutableStateOf<String?>(null) }

    // Altura da lista: cabe ~4 máquinas abertas; em tela pequena, desconta o topo e o botão
    val alturaTela = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val alturaMaxLista = (alturaTela - 250).coerceIn(300, 620).dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(0.94f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (chave == null) "Nova Chave" else "Editar Chave",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Prévia do código
                Text(
                    text = "${tipo?.codigo ?: "?"}-${numero.trim().uppercase().ifBlank { "____" }}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = BrandOrange
                )

                // Tipo
                Text("Tipo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TipoChaveApp.values().forEach { t ->
                        ChaveChip(
                            texto = t.descricao,
                            selecionado = tipo == t,
                            onClick = { tipo = t }
                        )
                    }
                }

                OutlinedTextField(
                    value = numero,
                    onValueChange = { numero = it },
                    label = { Text("Número da chave") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )

                // Fornecedor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SeletorFornecedor(
                        fornecedores = fornecedores,
                        selecionadoId = fornecedorId,
                        rotuloVazio = "Selecione o fornecedor",
                        permitirTodos = false,
                        onSelecionar = { fornecedorId = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onNovoFornecedor) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Novo", fontSize = 12.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = copias,
                        onValueChange = { copias = it.filter { c -> c.isDigit() } },
                        label = { Text("Qt. Chaves") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cadeados,
                        onValueChange = { cadeados = it.filter { c -> c.isDigit() } },
                        label = { Text("Qt. Cadeados") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = observacao,
                    onValueChange = { observacao = it },
                    label = { Text("Observação") },
                    supportingText = {
                        if (tipo == TipoChaveApp.OUTROS) {
                            Text("Obrigatória no tipo Outros", fontSize = 10.sp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = ativo,
                        onCheckedChange = { ativo = it },
                        colors = CheckboxDefaults.colors(checkedColor = BrandOrange)
                    )
                    Text("Chave ativa", fontSize = 14.sp)
                }

                erroLocal?.let {
                    Text(it, color = Color(0xFFFF4D4D), fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val tipoSelecionado = tipo
                            erroLocal = when {
                                numero.isBlank() -> "Informe o número da chave."
                                tipoSelecionado == null -> "Escolha o tipo da chave."
                                fornecedorId == null -> "Escolha o fornecedor."
                                tipoSelecionado == TipoChaveApp.OUTROS && observacao.isBlank() ->
                                    "No tipo Outros, informe na observação que chave é essa."
                                else -> null
                            }
                            if (erroLocal == null && tipoSelecionado != null) {
                                onSalvar(
                                    chave?.id,
                                    ChaveRequestDTO(
                                        numero = numero.trim().uppercase(),
                                        fornecedorId = fornecedorId!!,
                                        tipo = tipoSelecionado.codigo,
                                        quantidadeCopias = copias.toIntOrNull() ?: 0,
                                        quantidadeCadeados = cadeados.toIntOrNull() ?: 0,
                                        observacao = observacao.trim().ifBlank { null },
                                        ativo = ativo
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// NOVO FORNECEDOR
// ─────────────────────────────────────────────────────────────
@Composable
private fun NovoFornecedorDialog(
    onDismiss: () -> Unit,
    onSalvar: (String) -> Unit
) {
    var nome by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo fornecedor") },
        text = {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                placeholder = { Text("Ex: Papaiz") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (nome.isNotBlank()) onSalvar(nome) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

// ─────────────────────────────────────────────────────────────
// COMPONENTES AUXILIARES
// ─────────────────────────────────────────────────────────────

/** Campo de quantidade exibido no card (cópias, cadeados). */
@Composable
private fun CampoQuantidade(
    rotulo: String,
    valor: Int,
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = BrandOrange,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = rotulo.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = valor.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
@Composable
private fun ChaveChip(
    texto: String,
    selecionado: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selecionado) BrandOrange else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = texto,
            fontSize = 12.sp,
            fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
            color = if (selecionado) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SeletorFornecedor(
    fornecedores: List<FornecedorChaveDTO>,
    selecionadoId: Long?,
    rotuloVazio: String,
    permitirTodos: Boolean,
    onSelecionar: (Long?) -> Unit
) {
    var aberto by remember { mutableStateOf(false) }
    val nomeSelecionado = fornecedores.firstOrNull { it.id == selecionadoId }?.nome

    Box {
        OutlinedButton(onClick = { aberto = true }) {
            Text(
                text = nomeSelecionado ?: rotuloVazio,
                fontSize = 12.sp,
                maxLines = 1
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            if (permitirTodos) {
                DropdownMenuItem(
                    text = { Text("Todos") },
                    onClick = {
                        aberto = false
                        onSelecionar(null)
                    }
                )
            }
            fornecedores.forEach { f ->
                DropdownMenuItem(
                    text = { Text(f.nome ?: "-") },
                    onClick = {
                        aberto = false
                        onSelecionar(f.id)
                    }
                )
            }
            if (fornecedores.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Nenhum fornecedor cadastrado") },
                    onClick = { aberto = false }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// MÁQUINAS DA CHAVE (vínculo + histórico)
// ─────────────────────────────────────────────────────────────
@Composable
private fun MaquinasDaChaveDialog(
    chave: ChaveDTO,
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val chaveId = chave.id ?: return
    val chaveAtiva = chave.ativo ?: true

    var historico by remember { mutableStateOf(false) }
    var vinculos by remember { mutableStateOf<List<VinculoChaveDTO>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var falhou by remember { mutableStateOf(false) }
    var recarregar by remember { mutableStateOf(0) }

    var mostrarVincular by remember { mutableStateOf(false) }
    var vinculoParaTirar by remember { mutableStateOf<VinculoChaveDTO?>(null) }

    // Clientes abertos na sanfona (guarda a chave do grupo; sobrevive ao recarregar)
    var clientesAbertos by remember { mutableStateOf(setOf<String>()) }
    val grupos = remember(vinculos) { agruparVinculosPorCliente(vinculos) }

    LaunchedEffect(historico, recarregar) {
        carregando = true
        viewModel.loadMaquinasDaChave(chaveId, historico) { lista ->
            falhou = lista == null
            vinculos = lista ?: emptyList()
            carregando = false
        }
    }

    val alturaTela = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val alturaMaxLista = (alturaTela - 250).coerceIn(300, 620).dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(0.94f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Máquinas da chave", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(chave.codigo ?: "-", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = BrandOrange)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { historico = !historico }
                ) {
                    Switch(checked = historico, onCheckedChange = { historico = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mostrar histórico", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = alturaMaxLista)) {
                    when {
                        carregando -> CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = BrandOrange
                        )
                        falhou -> Text(
                            "Não foi possível carregar. Verifique a conexão.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        vinculos.isEmpty() -> Text(
                            if (historico) "Essa chave nunca foi vinculada a uma máquina."
                            else "Essa chave não está em nenhuma máquina.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            item(key = "resumo") {
                                val qtdMaq = vinculos.count { it.ativo ?: true }
                                val qtdCli = grupos.count { g -> g.vinculos.any { it.ativo ?: true } }
                                Text(
                                    "$qtdMaq ${if (qtdMaq == 1) "máquina" else "máquinas"} em " +
                                        "$qtdCli ${if (qtdCli == 1) "cliente" else "clientes"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            grupos.forEach { g ->
                                // Todos começam fechados; clica no nome para abrir/fechar
                                val aberto = g.chave in clientesAbertos
                                item(key = "cli_${g.chave}") {
                                    ClienteGrupoHeader(
                                        grupo = g,
                                        aberto = aberto,
                                        onClick = {
                                            clientesAbertos =
                                                if (g.chave in clientesAbertos) clientesAbertos - g.chave
                                                else clientesAbertos + g.chave
                                        }
                                    )
                                }
                                if (aberto) {
                                    items(g.vinculos, key = { "vin_${it.id ?: 0L}" }) { v ->
                                        Box(modifier = Modifier.padding(start = 12.dp)) {
                                            VinculoItem(v, mostrarCliente = false, onTirar = { vinculoParaTirar = v })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { mostrarVincular = true },
                    enabled = chaveAtiva,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Vincular máquina")
                }
                if (!chaveAtiva) {
                    Text(
                        "Chave inativa não pode ser vinculada.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (mostrarVincular) {
        VincularMaquinaDialog(
            chave = chave,
            viewModel = viewModel,
            onDismiss = { mostrarVincular = false },
            onVinculado = {
                mostrarVincular = false
                recarregar++
            }
        )
    }

    vinculoParaTirar?.let { v ->
        var obs by remember(v.id) { mutableStateOf("") }
        var enviando by remember(v.id) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!enviando) vinculoParaTirar = null },
            title = { Text("Tirar chave da máquina?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${chave.codigo} sai da máquina ${rotuloMaquina(v.praca, v.maquinaNome)}. O vínculo fica no histórico.")
                    OutlinedTextField(
                        value = obs,
                        onValueChange = { obs = it },
                        label = { Text("Motivo (opcional)") },
                        placeholder = { Text("Ex: fechadura trocada") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !enviando,
                    onClick = {
                        val id = v.id ?: return@TextButton
                        enviando = true
                        viewModel.encerrarVinculoChave(id, obs) { ok ->
                            enviando = false
                            if (ok) {
                                vinculoParaTirar = null
                                recarregar++
                            }
                        }
                    }
                ) { Text("Tirar", color = Color(0xFFFF4D4D)) }
            },
            dismissButton = {
                TextButton(enabled = !enviando, onClick = { vinculoParaTirar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun VinculoItem(v: VinculoChaveDTO, mostrarCliente: Boolean = true, onTirar: () -> Unit) {
    val ativo = v.ativo ?: true
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (ativo) 0.6f else 0.3f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                rotuloMaquina(v.praca, v.maquinaNome),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = if (ativo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandOrange.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(v.usoDescricao ?: v.uso ?: "", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (ativo) {
                TextButton(onClick = onTirar, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tirar", fontSize = 12.sp)
                }
            } else {
                Text("ENCERRADO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val detalhe = listOfNotNull(
            if (mostrarCliente) v.clienteNome?.ifBlank { null } else null,
            v.maquinaJogo?.ifBlank { null }
        ).joinToString(" · ")
        if (detalhe.isNotBlank()) {
            Text(detalhe, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            if (ativo) "Desde ${formatarDataVinculo(v.vinculadoEm)}"
            else "${formatarDataVinculo(v.vinculadoEm)} → ${formatarDataVinculo(v.desvinculadoEm)}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val obs = v.observacao
        if (!obs.isNullOrBlank()) {
            Text(obs, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// AGRUPAMENTO POR CLIENTE (sanfona no diálogo "Máquinas da chave")
// ─────────────────────────────────────────────────────────────
private data class GrupoClienteVinculos(
    val chave: String,
    val nome: String,
    val vinculos: List<VinculoChaveDTO>
) {
    val ativos: Int get() = vinculos.count { it.ativo ?: true }
    val encerrados: Int get() = vinculos.size - ativos
}

/**
 * Agrupa pelo codCliente (nome como reserva), clientes em ordem alfabética.
 * Dentro do cliente: ativos primeiro em ordem natural (praça, número);
 * depois os encerrados, do mais recente para o mais antigo.
 */
private fun agruparVinculosPorCliente(vinculos: List<VinculoChaveDTO>): List<GrupoClienteVinculos> =
    vinculos
        .groupBy { v -> v.codCliente?.toString() ?: v.clienteNome?.trim()?.uppercase() ?: "_sem_cliente" }
        .map { (chave, lista) ->
            val nome = lista.firstNotNullOfOrNull { it.clienteNome?.trim()?.ifBlank { null } } ?: "Sem cliente"
            val ativos = lista.filter { it.ativo ?: true }
                .sortedWith(compareBy({ chaveOrdemNatural(it.praca) }, { chaveOrdemNatural(it.maquinaNome) }))
            val encerrados = lista.filter { !(it.ativo ?: true) }
                .sortedByDescending { it.desvinculadoEm ?: "" }
            GrupoClienteVinculos(chave, nome, ativos + encerrados)
        }
        .sortedWith(compareBy({ it.ativos == 0 }, { chaveOrdemNatural(it.nome) }))

// Ordenação natural (evita "10" antes de "2") — mesma regra do Dashboard
private fun chaveOrdemNatural(value: String?): String {
    val raw = value?.trim().orEmpty()
    if (raw.isEmpty()) return ""
    return Regex("\\d+|\\D+").findAll(raw).joinToString("") { m ->
        val part = m.value
        if (part.first().isDigit()) part.padStart(12, '0') else part.lowercase()
    }
}

@Composable
private fun ClienteGrupoHeader(grupo: GrupoClienteVinculos, aberto: Boolean, onClick: () -> Unit) {
    val soEncerrados = grupo.ativos == 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BrandOrange.copy(alpha = if (soEncerrados) 0.05f else 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            if (aberto) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (aberto) "Fechar" else "Abrir",
            tint = BrandOrange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            grupo.nome,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = if (soEncerrados) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        val resumo = buildList {
            if (grupo.ativos > 0) add("${grupo.ativos} ${if (grupo.ativos == 1) "máquina" else "máquinas"}")
            if (grupo.encerrados > 0) add("${grupo.encerrados} encerr.")
        }.joinToString(" · ")
        Text(resumo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandOrange)
    }
}

// ─────────────────────────────────────────────────────────────
// VINCULAR: busca pelo número da máquina (+ praça opcional)
// ─────────────────────────────────────────────────────────────
@Composable
private fun VincularMaquinaDialog(
    chave: ChaveDTO,
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onVinculado: () -> Unit
) {
    val chaveId = chave.id ?: return

    var pracas by remember { mutableStateOf<List<String>>(emptyList()) }
    var praca by remember { mutableStateOf<String?>(null) }
    var numero by remember { mutableStateOf("") }
    var resultados by remember { mutableStateOf<List<MaquinaOpcaoDTO>?>(null) } // null = ainda não buscou
    var buscando by remember { mutableStateOf(false) }
    var selecionada by remember { mutableStateOf<MaquinaOpcaoDTO?>(null) }
    var uso by remember { mutableStateOf(UsoChaveApp.padraoPara(chave.tipo)) }
    var enviando by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadPracasChave { pracas = it }
    }

    fun buscar() {
        if (numero.isBlank() || buscando) return
        buscando = true
        selecionada = null
        viewModel.buscarMaquinasPorNumero(numero, praca) { lista ->
            resultados = lista
            if (lista.size == 1) selecionada = lista.first()
            buscando = false
        }
    }

    Dialog(onDismissRequest = { if (!enviando) onDismiss() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Vincular ${chave.codigo ?: ""}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // Praça (opcional)
                Text("Praça", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChaveChip(texto = "Todas", selecionado = praca == null, onClick = { praca = null; resultados = null; selecionada = null })
                    pracas.forEach { p ->
                        ChaveChip(texto = p, selecionado = praca == p, onClick = { praca = p; resultados = null; selecionada = null })
                    }
                }

                OutlinedTextField(
                    value = numero,
                    onValueChange = {
                        numero = it.trim()
                        resultados = null
                        selecionada = null
                    },
                    label = { Text("Número da máquina") },
                    placeholder = { Text("Ex: 51") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { buscar() }),
                    trailingIcon = {
                        IconButton(onClick = { buscar() }, enabled = numero.isNotBlank() && !buscando) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                when {
                    buscando -> CircularProgressIndicator(color = BrandOrange, modifier = Modifier.align(Alignment.CenterHorizontally))
                    resultados?.isEmpty() == true -> Text(
                        "Nenhuma máquina ativa com esse número" + (praca?.let { " na $it" } ?: "") + ".",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    resultados != null -> {
                        if (resultados!!.size > 1) {
                            Text("Mais de uma máquina com esse número. Escolha:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        resultados!!.forEach { m ->
                            val marcada = selecionada?.id == m.id
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (marcada) BrandOrange.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    )
                                    .clickable { selecionada = m }
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        rotuloMaquina(m.praca, m.numero),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = if (marcada) BrandOrange else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (marcada) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandOrange)
                                }
                                val detalhe = listOfNotNull(m.clienteNome?.ifBlank { null }, m.jogo?.ifBlank { null }).joinToString(" · ")
                                if (detalhe.isNotBlank()) {
                                    Text(detalhe, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                if (selecionada != null) {
                    Text("Usada como", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UsoChaveApp.values().forEach { u ->
                            ChaveChip(texto = u.descricao, selecionado = uso == u, onClick = { uso = u })
                        }
                    }
                    if (uso == UsoChaveApp.COFRE || uso == UsoChaveApp.TAMPA) {
                        Text(
                            "Se a máquina já tiver chave de ${uso.descricao.lowercase()}, a anterior sai e vai pro histórico.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, enabled = !enviando, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val maquinaId = selecionada?.id ?: return@Button
                            enviando = true
                            viewModel.vincularChave(maquinaId, chaveId, uso.codigo) { ok ->
                                enviando = false
                                if (ok) onVinculado()
                            }
                        },
                        enabled = selecionada != null && !enviando,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (enviando) "Salvando..." else "Vincular")
                    }
                }
            }
        }
    }
}

/** "V1 - 51" (ou só "51" se a máquina não tiver praça). */
private fun rotuloMaquina(praca: String?, numero: String?): String {
    val n = numero?.trim()?.ifBlank { null } ?: "?"
    val p = praca?.trim()?.ifBlank { null }
    return if (p == null) n else "$p - $n"
}

/** "2026-09-22T01:10:00" → "22/09/2026". */
private fun formatarDataVinculo(iso: String?): String {
    if (iso.isNullOrBlank() || iso.length < 10) return "-"
    val (ano, mes, dia) = iso.substring(0, 10).split("-").let {
        if (it.size == 3) Triple(it[0], it[1], it[2]) else return iso
    }
    return "$dia/$mes/$ano"
}
