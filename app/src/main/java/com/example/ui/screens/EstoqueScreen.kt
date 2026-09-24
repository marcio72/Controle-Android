package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import com.example.data.model.CategoriaDTO
import com.example.data.model.JogoDTO
import com.example.data.model.LoteDTO
import com.example.data.model.PecaDTO
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

// ─────────────────────────────────────────────────────────────
// TELA PRINCIPAL DE ESTOQUE
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstoqueScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    var tabIdx by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf<CategoriaDTO?>(null) }
    val tabs = listOf("Categorias", "Lotes")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Estoque", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                },
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
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = tabIdx,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BrandOrange
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = tabIdx == i,
                        onClick = { tabIdx = i },
                        text = {
                            Text(title, fontWeight = if (tabIdx == i) FontWeight.Bold else FontWeight.Normal)
                        }
                    )
                }
            }
            when (tabIdx) {
                0 -> TabCategorias(
                    viewModel = viewModel,
                    onCategoriaClick = { cat ->
                        selectedCategory = cat
                        tabIdx = 1
                    }
                )
                1 -> TabLotes(
                    viewModel = viewModel,
                    selectedCategory = selectedCategory,
                    onSelectedCategoryChange = { selectedCategory = it }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ABA: CATEGORIAS
// ─────────────────────────────────────────────────────────────
@Composable
fun TabCategorias(viewModel: AppViewModel, onCategoriaClick: (CategoriaDTO) -> Unit) {
    val categorias by viewModel.categorias.collectAsState()
    val isLoading by viewModel.categoriasLoading.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadCategorias() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BrandOrange)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(DeepDarkNavy).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("${categorias.size} categoria(s)", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                }
                items(categorias) { cat -> CategoriaCard(categoria = cat, onClick = { onCategoriaClick(cat) }) }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = BrandOrange, contentColor = Color.White
        ) { Icon(Icons.Default.Add, contentDescription = "Nova Categoria") }
    }

    if (showDialog) {
        NovaCategoriaDialog(viewModel = viewModel, onDismiss = { showDialog = false })
    }
}

@Composable
fun CategoriaCard(categoria: CategoriaDTO, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SleekNavyCard)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(BrandOrange.copy(alpha = 0.15f))
                    .border(1.dp, BrandOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (categoria.alias ?: categoria.nome.take(2)).uppercase(),
                    fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = BrandOrange
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(categoria.nome, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Alias: ${categoria.alias ?: "-"}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
            }
            Text("ID ${categoria.id}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun NovaCategoriaDialog(viewModel: AppViewModel, onDismiss: () -> Unit) {
    var nome by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SleekNavyCard)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Nova Categoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                OutlinedTextField(
                    value = nome, onValueChange = { nome = it },
                    label = { Text("Nome") }, placeholder = { Text("Ex: Placa Mãe") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), colors = estoqueTextFieldColors()
                )
                OutlinedTextField(
                    value = alias, onValueChange = { alias = it.take(6) },
                    label = { Text("Alias (prefixo)") }, placeholder = { Text("Ex: pl") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), colors = estoqueTextFieldColors()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss, modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) { Text("Cancelar", color = Color(0xFF94A3B8)) }
                    Button(
                        onClick = {
                            if (nome.isBlank() || alias.isBlank()) return@Button
                            isLoading = true
                            viewModel.criarCategoria(nome.trim(), alias.trim()) { isLoading = false; onDismiss() }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                        enabled = nome.isNotBlank() && alias.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NovaSubCategoriaDialog(
    viewModel: AppViewModel,
    categoriaId: Long,
    onCriada: (com.example.data.model.SubCategoriaDTO) -> Unit,
    onDismiss: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SleekNavyCard)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Nova Subcategoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                OutlinedTextField(
                    value = nome, onValueChange = { nome = it },
                    label = { Text("Nome") }, placeholder = { Text("Ex: 15 Pol., Asus...") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), colors = estoqueTextFieldColors()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss, modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) { Text("Cancelar", color = Color(0xFF94A3B8)) }
                    Button(
                        onClick = {
                            if (nome.isBlank()) return@Button
                            isLoading = true
                            viewModel.criarSubCategoria(nome.trim(), categoriaId) { criada ->
                                isLoading = false
                                if (criada != null) onCriada(criada)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                        enabled = nome.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ABA: LOTES
// ─────────────────────────────────────────────────────────────
@Composable
fun TabLotes(
    viewModel: AppViewModel,
    selectedCategory: CategoriaDTO?,
    onSelectedCategoryChange: (CategoriaDTO?) -> Unit
) {
    val lotes by viewModel.lotes.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val subCategorias by viewModel.subCategorias.collectAsState()
    val isLoading by viewModel.lotesLoading.collectAsState()
    
    var showDialog by remember { mutableStateOf(false) }
    var loteExpandido by remember { mutableStateOf<Long?>(null) }
    
    var selectedSubCategoriaId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadLotes()
        viewModel.loadCategorias()
    }

    // Sempre que a categoria selecionada mudar, busca as subcategorias dela
    // (fica vazio automaticamente se a categoria não tiver nenhuma cadastrada).
    LaunchedEffect(selectedCategory?.id) {
        viewModel.loadSubCategorias(selectedCategory?.id)
    }

    val filteredLotes = remember(lotes, selectedCategory, selectedSubCategoriaId) {
        lotes.filter { lote ->
            // 1. Filtragem por Categoria
            val matchesCategory = selectedCategory == null || lote.categoria?.id == selectedCategory?.id

            // 2. Filtragem por subcategoria (dado real vindo do banco, não mais texto)
            val matchesSub = selectedSubCategoriaId == null || lote.subCategoria?.id == selectedSubCategoriaId

            matchesCategory && matchesSub
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BrandOrange)
        } else {
            Column(modifier = Modifier.fillMaxSize().background(DeepDarkNavy)) {
                // Título do filtro de categoria
                Text(
                    text = "Filtrar por Categoria",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                )
                
                // Chips de Categoria
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoteFilterChip(
                        text = "Todos",
                        selected = selectedCategory == null,
                        onClick = {
                            onSelectedCategoryChange(null)
                            selectedSubCategoriaId = null
                        }
                    )
                    categorias.forEach { cat ->
                        LoteFilterChip(
                            text = cat.nome,
                            selected = selectedCategory?.id == cat.id,
                            onClick = {
                                onSelectedCategoryChange(cat)
                                selectedSubCategoriaId = null
                            }
                        )
                    }
                }
                
                // Subfiltro de Subcategoria — só aparece se a categoria escolhida
                // tiver subcategorias cadastradas (ex: tamanhos de Monitor).
                AnimatedVisibility(visible = subCategorias.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Text(
                            text = "Subcategoria",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LoteFilterChip(
                                text = "Todos",
                                selected = selectedSubCategoriaId == null,
                                onClick = { selectedSubCategoriaId = null }
                            )
                            subCategorias.forEach { sub ->
                                LoteFilterChip(
                                    text = sub.nome,
                                    selected = selectedSubCategoriaId == sub.id,
                                    onClick = { selectedSubCategoriaId = sub.id }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Lista de lotes filtrados
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${filteredLotes.size} lote(s) encontrado(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                    items(filteredLotes) { lote ->
                        LoteCard(
                            lote = lote,
                            expandido = loteExpandido == lote.idLote,
                            onToggle = {
                                loteExpandido = if (loteExpandido == lote.idLote) null else lote.idLote
                                if (loteExpandido == lote.idLote) viewModel.loadPecasDoLote(lote.idLote)
                            },
                            viewModel = viewModel
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
        
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = BrandOrange, contentColor = Color.White
        ) { Icon(Icons.Default.Add, contentDescription = "Novo Lote") }
    }

    if (showDialog) {
        NovoLoteDialog(viewModel = viewModel, onDismiss = { showDialog = false })
    }
}

@Composable
fun LoteCard(
    lote: LoteDTO,
    expandido: Boolean,
    onToggle: () -> Unit,
    viewModel: AppViewModel
) {
    val pecasDoLote by viewModel.pecasDoLote.collectAsState()
    val pecasLoading by viewModel.pecasDoLoteLoading.collectAsState()
    val faixaPecasPorLote by viewModel.faixaPecasPorLote.collectAsState()

    LaunchedEffect(lote.idLote) { viewModel.loadFaixaPecas(lote.idLote) }
    val faixa = faixaPecasPorLote[lote.idLote]

    val totalQtd = lote.quantidadeComprada
    val qtdAtual = lote.quantidadeAtual
    val pct = if (totalQtd > 0) qtdAtual.toFloat() / totalQtd else 0f
    val corEstoque = when {
        pct > 0.5f -> Color(0xFF22C55E)
        pct > 0.2f -> Color(0xFFF59E0B)
        else       -> Color(0xFFEF4444)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SleekNavyCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Cabeçalho ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp).clip(RoundedCornerShape(10.dp))
                        .background(SecondaryEmerald.copy(alpha = 0.1f))
                        .border(1.dp, SecondaryEmerald.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = null, tint = SecondaryEmerald, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lote.categoria?.nome ?: "Sem categoria",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryEmerald, fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = lote.descricao ?: lote.alias ?: "Lote #${lote.idLote}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = Color.White,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text("Fornecedor: ${lote.fornecedor ?: "-"}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$qtdAtual/$totalQtd", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = corEstoque)
                    Text("em estoque", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp)
                )
            }

            // ── Barra de progresso ────────────────────────────
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = corEstoque, trackColor = Color(0xFF1E293B)
            )

            // ── Chips ─────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("ID ${lote.idLote}")
                InfoChip(lote.alias ?: "-")
                if (faixa?.first != null && faixa.second != null) {
                    InfoChip("${faixa.first} - ${faixa.second}")
                }
                lote.dataEntrada?.let { InfoChip(it) }
            }

            // ── Peças expandidas ──────────────────────────────
            if (expandido) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(12.dp))

                if (pecasLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally),
                        color = BrandOrange, strokeWidth = 2.dp
                    )
                } else {
                    val pecas = pecasDoLote[lote.idLote] ?: emptyList()
                    val disponiveis = pecas.count { it.status == "ESTOQUE" }
                    val instaladas  = pecas.count { it.status == "INSTALADA" }
                    val descartadas = pecas.count { it.status == "DESCARTADA" }

                    // Resumo
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        ResumoChip(label = "Disponíveis", count = disponiveis, color = Color(0xFF22C55E))
                        ResumoChip(label = "Instaladas",  count = instaladas,  color = Color(0xFFEF4444))
                        ResumoChip(label = "P.T.",        count = descartadas, color = Color(0xFF94A3B8))
                    }

                    if (pecas.isEmpty()) {
                        Text("Nenhuma peça encontrada.", color = Color(0xFF64748B), fontSize = 13.sp)
                    } else {
                        // Peças disponíveis
                        val pecasDisp = pecas.filter { it.status == "ESTOQUE" }
                        val pecasInst = pecas.filter { it.status == "INSTALADA" }
                        val pecasDesc = pecas.filter { it.status == "DESCARTADA" }

                        if (pecasDisp.isNotEmpty()) {
                            Text(
                                "Disponíveis (${pecasDisp.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF22C55E), fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            pecasDisp.forEach { PecaRowDisponivel(it) }
                        }

                        if (pecasInst.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Instaladas (${pecasInst.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFEF4444), fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            pecasInst.forEach { PecaRowInstalada(peca = it, loteId = lote.idLote, viewModel = viewModel) }
                        }

                        if (pecasDesc.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Perda Total - P.T. (${pecasDesc.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            pecasDesc.forEach { PecaRowDescartada(it) }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// LINHAS DE PEÇA
// ─────────────────────────────────────────────────────────────
@Composable
fun PecaRowDisponivel(peca: PecaDTO) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF22C55E)))
        Spacer(modifier = Modifier.width(10.dp))
        Text(peca.codigo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SecondaryEmerald, modifier = Modifier.weight(1f))
        Text("Disponível", fontSize = 12.sp, color = Color(0xFF22C55E))
    }
}

@Composable
fun PecaRowInstalada(peca: PecaDTO, loteId: Long, viewModel: AppViewModel) {
    var expandido by remember { mutableStateOf(false) }
    var dialogAcao by remember { mutableStateOf<String?>(null) } // "RETIRAR" ou "DESCARTAR"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFF1A0A0A), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .clickable { expandido = !expandido }
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
            Spacer(modifier = Modifier.width(10.dp))
            Text(peca.codigo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
            Text("Instalada", fontSize = 12.sp, color = Color(0xFFEF4444))
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp)
            )
        }

        if (expandido) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFEF4444).copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            peca.dataInstalacao?.let {
                DetalheRow(label = "Data instalação", valor = it)
            }
            peca.clienteNome?.let {
                DetalheRow(label = "Ponto (cliente)", valor = it)
            }
            peca.maquinaNome?.let {
                DetalheRow(label = "Máquina", valor = it)
            }
            peca.maquinaJogo?.let {
                DetalheRow(label = "Jogo", valor = it)
            }
            peca.maquinaPlaca?.let {
                DetalheRow(label = "Placa", valor = it)
            }
            peca.observacao?.takeIf { it.isNotBlank() }?.let {
                DetalheRow(label = "Observação", valor = it)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { dialogAcao = "DESCARTAR" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF94A3B8).copy(alpha = 0.4f))
                ) { Text("Descartar (P.T.)", fontSize = 12.sp) }
                Button(
                    onClick = { dialogAcao = "RETIRAR" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald)
                ) { Text("Estoque", fontSize = 12.sp) }
            }
        }
    }

    if (dialogAcao != null) {
        PecaAcaoDialog(
            titulo = if (dialogAcao == "DESCARTAR") "Descartar peça (Perda Total)" else "Devolver ao estoque",
            mensagem = if (dialogAcao == "DESCARTAR")
                "Confirma que a peça ${peca.codigo} foi PERDIDA/QUEIMADA? Ela não volta pro estoque."
            else
                "Confirma a devolução da peça ${peca.codigo} para o estoque?",
            confirmarLabel = if (dialogAcao == "DESCARTAR") "Descartar" else "Devolver",
            confirmarColor = if (dialogAcao == "DESCARTAR") Color(0xFF94A3B8) else SecondaryEmerald,
            onDismiss = { dialogAcao = null },
            onConfirm = { observacao ->
                val acao = dialogAcao
                dialogAcao = null
                if (acao == "DESCARTAR") {
                    viewModel.descartarPeca(peca.idPeca, loteId, observacao) { }
                } else {
                    viewModel.retirarPeca(peca.idPeca, loteId, observacao) { }
                }
            }
        )
    }
}

@Composable
fun PecaAcaoDialog(
    titulo: String,
    mensagem: String,
    confirmarLabel: String,
    confirmarColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (observacao: String?) -> Unit
) {
    var observacao by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SleekNavyCard)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(mensagem, fontSize = 13.sp, color = Color(0xFF94A3B8))
                OutlinedTextField(
                    value = observacao, onValueChange = { observacao = it },
                    label = { Text("Observação (opcional)") },
                    placeholder = { Text("Ex: queimou, cliente trocou de local...") },
                    modifier = Modifier.fillMaxWidth(), colors = estoqueTextFieldColors()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss, modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) { Text("Cancelar", color = Color(0xFF94A3B8)) }
                    Button(
                        onClick = { onConfirm(observacao.trim().ifBlank { null }) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = confirmarColor)
                    ) { Text(confirmarLabel, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun PecaRowDescartada(peca: PecaDTO) {
    var expandido by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFF16181A), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF94A3B8).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .clickable { expandido = !expandido }
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF94A3B8)))
            Spacer(modifier = Modifier.width(10.dp))
            Text(peca.codigo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f))
            Text("P.T.", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp)
            )
        }

        if (expandido) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF94A3B8).copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Última localização antes do descarte:",
                fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            peca.dataInstalacao?.let {
                DetalheRow(label = "Data instalação", valor = it)
            }
            peca.dataRetirada?.let {
                DetalheRow(label = "Data descarte", valor = it)
            }
            peca.clienteNome?.let {
                DetalheRow(label = "Ponto (cliente)", valor = it)
            }
            peca.maquinaNome?.let {
                DetalheRow(label = "Máquina", valor = it)
            }
            peca.observacao?.takeIf { it.isNotBlank() }?.let {
                DetalheRow(label = "Observação", valor = it)
            }
        }
    }
}

@Composable
fun DetalheRow(label: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
        Text(valor, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────
// DIALOG: NOVO LOTE
// ─────────────────────────────────────────────────────────────
@Composable
fun NovoLoteDialog(viewModel: AppViewModel, onDismiss: () -> Unit) {
    val categorias by viewModel.categorias.collectAsState()
    val subCategorias by viewModel.subCategorias.collectAsState()
    val jogos by viewModel.jogos.collectAsState()
    var categoriaSelecionada by remember { mutableStateOf<CategoriaDTO?>(null) }
    var subCategoriaSelecionada by remember { mutableStateOf<com.example.data.model.SubCategoriaDTO?>(null) }
    var showNovaSubCategoriaDialog by remember { mutableStateOf(false) }
    var alias by remember { mutableStateOf("") }
    var fornecedor by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var quantidade by remember { mutableStateOf("") }
    var numeroInicial by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }

    // Modo manual (ex: fornecedor AEC) - código de fábrica + jogo por peça
    var modoManual by remember { mutableStateOf(false) }
    var qtdManual by remember { mutableStateOf("") }
    var linhasManuais by remember { mutableStateOf(listOf<Pair<String, JogoDTO?>>()) }

    LaunchedEffect(Unit) { viewModel.loadCategorias(); viewModel.loadJogos() }

    LaunchedEffect(categoriaSelecionada?.id) {
        subCategoriaSelecionada = null
        viewModel.loadSubCategorias(categoriaSelecionada?.id)
    }

    LaunchedEffect(qtdManual) {
        val qtd = qtdManual.toIntOrNull() ?: 0
        linhasManuais = if (qtd in 1..200) {
            (0 until qtd).map { i -> linhasManuais.getOrNull(i) ?: ("" to null) }
        } else emptyList()
    }

    val preview = remember(alias, numeroInicial, quantidade) {
        val a = alias.trim().uppercase()
        val n = numeroInicial.toIntOrNull()
        val q = quantidade.toIntOrNull()
        if (a.isNotEmpty() && n != null && q != null && n > 0 && q > 0) {
            val max = minOf(q, 3)
            val primeiros = (0 until max).map { "$a-${String.format("%04d", n + it)}" }
            if (q <= 3) primeiros.joinToString("  •  ")
            else primeiros.joinToString("  •  ") + "  •  ...  •  $a-${String.format("%04d", n + q - 1)}  (${q} peças)"
        } else null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SleekNavyCard)) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Novo Lote", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)

                // Seletor de Categoria
                Box {
                    OutlinedButton(
                        onClick = { showDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, if (categoriaSelecionada != null) BrandOrange.copy(alpha = 0.5f) else Color(0xFF334155)
                        )
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = categoriaSelecionada?.nome ?: "Selecionar Categoria...",
                                color = if (categoriaSelecionada != null) Color.White else Color(0xFF64748B)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B))
                        }
                    }
                    DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }, modifier = Modifier.background(SleekNavyCard)) {
                        categorias.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nome, color = Color.White) },
                                onClick = {
                                    categoriaSelecionada = cat
                                    if (alias.isBlank()) alias = cat.alias ?: ""
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }

                // Toggle de modo (Automático x Manual)
                if (categoriaSelecionada != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LoteFilterChip(text = "Automático", selected = !modoManual, onClick = { modoManual = false })
                        LoteFilterChip(text = "Manual (código de fábrica)", selected = modoManual, onClick = { modoManual = true })
                    }
                }

                // Subcategoria — só aparece se a categoria escolhida tiver
                // subcategorias cadastradas (ex: tamanhos de Monitor).
                if (categoriaSelecionada != null && subCategorias.isNotEmpty()) {
                    Column {
                        Text("Subcategoria", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            subCategorias.forEach { sub ->
                                LoteFilterChip(
                                    text = sub.nome,
                                    selected = subCategoriaSelecionada?.id == sub.id,
                                    onClick = { subCategoriaSelecionada = sub }
                                )
                            }
                            LoteFilterChip(
                                text = "+ Nova",
                                selected = false,
                                onClick = { showNovaSubCategoriaDialog = true }
                            )
                        }
                    }
                } else if (categoriaSelecionada != null) {
                    LoteFilterChip(
                        text = "+ Nova Subcategoria",
                        selected = false,
                        onClick = { showNovaSubCategoriaDialog = true }
                    )
                }

                if (!modoManual) {
                // Alias + Nº Inicial + Qtd
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = alias, onValueChange = { alias = it.take(8) },
                        label = { Text("Alias") }, placeholder = { Text("Ex: SBF") },
                        singleLine = true, modifier = Modifier.weight(1f), colors = estoqueTextFieldColors()
                    )
                    OutlinedTextField(
                        value = numeroInicial, onValueChange = { numeroInicial = it.filter { c -> c.isDigit() } },
                        label = { Text("Nº Inicial") }, placeholder = { Text("Ex: 45") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), colors = estoqueTextFieldColors()
                    )
                    OutlinedTextField(
                        value = quantidade, onValueChange = { quantidade = it.filter { c -> c.isDigit() } },
                        label = { Text("Qtd") }, placeholder = { Text("Ex: 10") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), colors = estoqueTextFieldColors()
                    )
                }

                // Preview
                if (preview != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrandOrange.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
                            .border(1.dp, BrandOrange.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text("Preview das peças", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(preview, fontSize = 12.sp, color = BrandOrange, fontWeight = FontWeight.Bold)
                    }
                }
                } // fim modo automático

                if (modoManual) {
                    OutlinedTextField(
                        value = qtdManual, onValueChange = { qtdManual = it.filter { c -> c.isDigit() } },
                        label = { Text("Quantidade de Peças") }, placeholder = { Text("Ex: 10") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), colors = estoqueTextFieldColors()
                    )

                    Text("Código de fábrica + jogo de cada peça", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)

                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        linhasManuais.forEachIndexed { index, (codigoLinha, jogoLinha) ->
                            var expandedJogo by remember { mutableStateOf(false) }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = codigoLinha,
                                        onValueChange = { novo ->
                                            linhasManuais = linhasManuais.toMutableList().also { it[index] = novo to jogoLinha }
                                        },
                                        placeholder = { Text("Código (ex: 6DE5)") },
                                        singleLine = true, modifier = Modifier.weight(1f), colors = estoqueTextFieldColors()
                                    )
                                    OutlinedButton(
                                        onClick = { expandedJogo = !expandedJogo },
                                        modifier = Modifier.weight(1f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                jogoLinha?.nome ?: "Jogo...",
                                                color = if (jogoLinha != null) Color.White else Color(0xFF64748B),
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = if (expandedJogo) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Lista de jogos aberta INLINE (não é popup) - funciona em qualquer
                                // aparelho, independente de estar dentro de Dialog/scroll aninhado.
                                if (expandedJogo) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 180.dp)
                                            .verticalScroll(rememberScrollState())
                                            .background(Color(0xFF0F1720), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                            .padding(vertical = 4.dp)
                                    ) {
                                        if (jogos.isEmpty()) {
                                            Text(
                                                "Carregando jogos...", color = Color(0xFF64748B), fontSize = 12.sp,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                        jogos.forEach { jogo ->
                                            Text(
                                                jogo.nome ?: "-",
                                                color = Color.White, fontSize = 13.sp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        linhasManuais = linhasManuais.toMutableList().also { it[index] = codigoLinha to jogo }
                                                        expandedJogo = false
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = fornecedor, onValueChange = { fornecedor = it },
                    label = { Text("Fornecedor") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = estoqueTextFieldColors()
                )
                OutlinedTextField(
                    value = descricao, onValueChange = { descricao = it },
                    label = { Text("Descrição") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = estoqueTextFieldColors()
                )
                OutlinedTextField(
                    value = codigo, onValueChange = { codigo = it },
                    label = { Text("Código do Lote (opcional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = estoqueTextFieldColors()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss, modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) { Text("Cancelar", color = Color(0xFF94A3B8)) }
                    Button(
                        onClick = {
                            val cat = categoriaSelecionada ?: return@Button
                            if (modoManual) {
                                if (fornecedor.isBlank()) return@Button
                                if (linhasManuais.isEmpty() || linhasManuais.any { it.first.isBlank() }) return@Button
                                isLoading = true
                                viewModel.criarLoteManual(
                                    categoriaId = cat.id,
                                    subCategoriaId = subCategoriaSelecionada?.id,
                                    fornecedor = fornecedor.trim(),
                                    descricao = descricao.trim().ifBlank { null },
                                    dataEntrada = java.time.LocalDate.now().toString(),
                                    pecas = linhasManuais.map { (cod, jogo) ->
                                        com.example.data.model.PecaManualDTO(codigo = cod.trim(), jogoId = jogo?.id)
                                    }
                                ) { isLoading = false; onDismiss() }
                            } else {
                                val qtd = quantidade.toIntOrNull() ?: return@Button
                                val numInicial = numeroInicial.toIntOrNull() ?: return@Button
                                if (alias.isBlank()) return@Button
                                isLoading = true
                                viewModel.criarLote(
                                    categoriaId = cat.id,
                                    subCategoriaId = subCategoriaSelecionada?.id,
                                    alias = alias.trim(),
                                    fornecedor = fornecedor.trim().ifBlank { null },
                                    codigo = codigo.trim().ifBlank { null },
                                    descricao = descricao.trim().ifBlank { null },
                                    quantidadeComprada = qtd,
                                    numeroInicial = numInicial,
                                    dataEntrada = java.time.LocalDate.now().toString()
                                ) { isLoading = false; onDismiss() }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                        enabled = categoriaSelecionada != null && !isLoading && if (modoManual) {
                            fornecedor.isNotBlank() && linhasManuais.isNotEmpty() && linhasManuais.none { it.first.isBlank() }
                        } else {
                            alias.isNotBlank() && quantidade.isNotBlank() && numeroInicial.isNotBlank()
                        }
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showNovaSubCategoriaDialog && categoriaSelecionada != null) {
        NovaSubCategoriaDialog(
            viewModel = viewModel,
            categoriaId = categoriaSelecionada!!.id,
            onCriada = { subCategoriaSelecionada = it },
            onDismiss = { showNovaSubCategoriaDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// COMPONENTES AUXILIARES
// ─────────────────────────────────────────────────────────────
@Composable
fun ResumoChip(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(count.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = color)
        Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.8f))
    }
}

@Composable
fun InfoChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF94A3B8),
        modifier = Modifier
            .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun estoqueTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandOrange,
    unfocusedBorderColor = Color(0xFF334155),
    focusedLabelColor = BrandOrange,
    unfocusedLabelColor = Color(0xFF64748B),
    cursorColor = BrandOrange,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)

@Composable
fun LoteFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) BrandOrange.copy(alpha = 0.15f) else Color(0xFF1E293B))
            .border(
                width = 1.dp,
                color = if (selected) BrandOrange else Color(0xFF334155),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) BrandOrange else Color(0xFF94A3B8),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
