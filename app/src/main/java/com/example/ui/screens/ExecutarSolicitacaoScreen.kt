package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.CategoriaDTO
import com.example.data.model.PecaDTO
import com.example.data.model.SolicitacaoResponseDTO
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutarSolicitacaoScreen(
    viewModel: AppViewModel,
    solicitacao: SolicitacaoResponseDTO,
    onBack: () -> Unit
) {
    val categorias by viewModel.categorias.collectAsState()
    val pecasDisponiveis by viewModel.pecasDisponiveis.collectAsState()
    val pecasLoading by viewModel.pecasLoading.collectAsState()
    val execucaoLoading by viewModel.execucaoLoading.collectAsState()
    val usuarioLogado by viewModel.usuarioLogado.collectAsState()

    // Estado por problema: problemaId -> texto do que foi feito
    val descricoesPorProblema = remember {
        androidx.compose.runtime.mutableStateMapOf<Long, String>().apply {
            solicitacao.problemas?.forEach { p ->
                p.idProblema?.let { id -> put(id, "") }
            }
        }
    }

    // Peças: cada peça selecionada é atribuída a UM problema/máquina específico (idPeca -> idProblema)
    val pecaAssignments = remember { androidx.compose.runtime.mutableStateMapOf<Long, Long>() }
    // Cache de código/nome das peças já vistas, para não perder a referência ao trocar de categoria
    val pecaInfoCache = remember { androidx.compose.runtime.mutableStateMapOf<Long, PecaDTO>() }
    // idPeca atualmente mostrando o seletor de "qual máquina" (quando há mais de 1 problema)
    var pecaEmSelecao by remember { mutableStateOf<Long?>(null) }

    // Seção de peças
    var usarPecas by remember { mutableStateOf(false) }
    var categoriaSelecionada by remember { mutableStateOf<CategoriaDTO?>(null) }
    var expandedCategoriaDropdown by remember { mutableStateOf(false) }

    // Confirmação antes de enviar
    var showConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Carrega categorias ao entrar na tela
    LaunchedEffect(Unit) {
        viewModel.loadCategorias()
    }

    // Mantém o cache de código/nome de peças mesmo depois de trocar de categoria
    LaunchedEffect(pecasDisponiveis) {
        pecasDisponiveis.forEach { p -> pecaInfoCache[p.idPeca] = p }
    }

    // Quando as atribuições de peça mudam, injeta/atualiza bloco de peças no campo "O que foi feito" de CADA problema
    LaunchedEffect(pecaAssignments.toMap()) {
        val problemas = solicitacao.problemas ?: return@LaunchedEffect
        problemas.forEach { p ->
            val id = p.idProblema ?: return@forEach
            val textoAtual = descricoesPorProblema[id] ?: ""

            // Remove bloco anterior de peças (tudo depois de "\n\nPeças utilizadas:")
            val textoBase = if (textoAtual.contains("\n\nPeças utilizadas:")) {
                textoAtual.substringBefore("\n\nPeças utilizadas:").trimEnd()
            } else {
                textoAtual.trimEnd()
            }

            // Adiciona novo bloco apenas com as peças atribuídas a ESTE problema
            val idsDesteProblema = pecaAssignments.filterValues { it == id }.keys
            val novoTexto = if (idsDesteProblema.isNotEmpty()) {
                val codigos = idsDesteProblema
                    .mapNotNull { pecaInfoCache[it]?.codigo }
                    .joinToString(", ")
                if (codigos.isNotBlank()) {
                    "$textoBase\n\nPeças utilizadas: $codigos"
                } else textoBase
            } else {
                textoBase
            }

            descricoesPorProblema[id] = novoTexto
        }
    }

    // Carrega peças quando muda a categoria (mantém as atribuições já feitas em outras categorias)
    LaunchedEffect(categoriaSelecionada) {
        val cat = categoriaSelecionada
        if (cat != null) {
            viewModel.loadPecasDisponiveis(cat.id)
        } else {
            viewModel.clearPecasDisponiveis()
        }
    }

    val problemas = solicitacao.problemas ?: emptyList()
    val tecnico = usuarioLogado?.nome ?: usuarioLogado?.username ?: "Técnico"

    // Valida se todos os problemas têm descrição preenchida
    val todosPreenchidos = problemas.all { p ->
        val id = p.idProblema ?: return@all false
        descricoesPorProblema[id]?.isNotBlank() == true
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = SleekNavyCard,
            title = {
                Text(
                    "Confirmar execução",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Ao confirmar:",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Text(
                        "• A solicitação será marcada como concluída",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    if (pecaAssignments.isNotEmpty()) {
                        val qtdPecas = pecaAssignments.size
                        Text(
                            "• $qtdPecas peça(s) serão baixadas do estoque",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        "• Uma notificação será enviada ao grupo Signal",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Esta ação não pode ser desfeita.",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        val execucoesPorProblema = descricoesPorProblema
                            .filter { it.value.isNotBlank() }
                            .mapValues { (problemaId, desc) ->
                                val pecasDoProblema = pecaAssignments.filterValues { it == problemaId }.keys.toList()
                                Pair<String, List<Long>>(desc, pecasDoProblema)
                            }
                        viewModel.performRegistrarExecucao(
                            solicitacaoId = solicitacao.id ?: 0L,
                            execucoesPorProblema = execucoesPorProblema,
                            nomeCliente = solicitacao.cliente ?: "",
                            context = context,
                            onResult = { success ->
                                if (success) onBack()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Confirmar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Executar Solicitação #${solicitacao.id}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            solicitacao.cliente ?: "",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Aberto",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepDarkNavy,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(DeepDarkNavy)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = todosPreenchidos && !execucaoLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandOrange,
                        disabledContainerColor = Color(0xFF334155)
                    )
                ) {
                    if (execucaoLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar execução", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                if (!todosPreenchidos) {
                    Text(
                        "Preencha o campo 'O que foi feito' em todos os problemas",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Notificação Signal será enviada automaticamente",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        },
        containerColor = DeepDarkNavy
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- Card de info do técnico ---
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SleekNavyCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrandOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = BrandOrange,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text("Técnico responsável", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(tecnico, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }

            // --- Seção de Problemas ---
            Text(
                "PROBLEMAS (${problemas.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.08.sp
            )

            problemas.forEachIndexed { index, problema ->
                val problemaId = problema.idProblema
                val descricao = if (problemaId != null) descricoesPorProblema[problemaId] ?: "" else ""
                val preenchido = descricao.isNotBlank()

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekNavyCard),
                    border = if (preenchido)
                        androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    else
                        androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E2A3A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        // Header do problema
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F1A2C))
                                .padding(12.dp, 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (preenchido) Color(0xFF10B981) else BrandOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                if (preenchido) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Text(
                                        "${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                val numMaq = problema.maquina?.substringBefore(" - ")?.trim()
                                val nomJogo = problema.maquina?.substringAfter(" - ", "")?.trim()
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Máquina: ",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        numMaq ?: "-",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandOrange,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                                if (!nomJogo.isNullOrBlank()) {
                                    Text(
                                        nomJogo,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Problema relatado (read-only)
                            Column {
                                Text(
                                    "Problema relatado",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color(0xFF0F1623))
                                        .border(0.5.dp, Color(0xFF1E2A3A), RoundedCornerShape(7.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        problema.descricao ?: "Sem descrição",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8),
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            // Campo: O que foi feito
                            Column {
                                Row(
                                    modifier = Modifier.padding(bottom = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "O que foi feito",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        " *",
                                        fontSize = 11.sp,
                                        color = BrandOrange
                                    )
                                }
                                OutlinedTextField(
                                    value = descricao,
                                    onValueChange = { novo ->
                                        if (problemaId != null) {
                                            descricoesPorProblema[problemaId] = novo
                                        }
                                    },
                                    placeholder = {
                                        Text(
                                            "descreva o serviço executado...",
                                            color = Color(0xFF334155),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    },
                                    minLines = 3,
                                    maxLines = 6,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = Color(0xFF4ADE80),
                                        lineHeight = 18.sp
                                    ),
                                    shape = RoundedCornerShape(7.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F1623),
                                        unfocusedContainerColor = Color(0xFF0F1623),
                                        focusedBorderColor = if (preenchido) Color(0xFF10B981) else BrandOrange,
                                        unfocusedBorderColor = Color(0xFF1E2A3A),
                                        cursorColor = Color(0xFF4ADE80)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    "${descricao.length} / 500",
                                    fontSize = 10.sp,
                                    color = Color(0xFF334155),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 3.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        }
                    }
                }
            }

            // --- Seção de Peças (opcional) ---
            Text(
                "PEÇAS UTILIZADAS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.08.sp
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SleekNavyCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    // Toggle header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = null,
                                tint = BrandOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "Estoque",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFF1A2332))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text("opcional", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                        }
                        Switch(
                            checked = usarPecas,
                            onCheckedChange = { checked ->
                                usarPecas = checked
                                if (!checked) {
                                    categoriaSelecionada = null
                                    pecaAssignments.clear()
                                    pecaEmSelecao = null
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BrandOrange,
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF1E2A3A)
                            )
                        )
                    }

                    if (usarPecas) {
                        HorizontalDivider(color = Color(0xFF1E2A3A), thickness = 0.5.dp)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Dropdown de categorias
                            Column {
                                Text(
                                    "Categoria",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color(0xFF0F1623))
                                        .border(
                                            0.5.dp,
                                            if (expandedCategoriaDropdown) BrandOrange else Color(0xFF334155),
                                            RoundedCornerShape(7.dp)
                                        )
                                        .clickable { expandedCategoriaDropdown = !expandedCategoriaDropdown }
                                        .padding(horizontal = 12.dp, vertical = 11.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            categoriaSelecionada?.nome ?: "selecionar categoria...",
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (categoriaSelecionada != null) Color(0xFF4ADE80) else Color(0xFF334155)
                                        )
                                        Icon(
                                            if (expandedCategoriaDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = Color(0xFF4ADE80)
                                        )
                                    }
                                }

                                if (expandedCategoriaDropdown) {
                                    Card(
                                        shape = RoundedCornerShape(7.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1623)),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF334155)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .padding(top = 4.dp)
                                    ) {
                                        LazyColumn {
                                            items(categorias) { cat ->
                                                val isSelected = cat.id == categoriaSelecionada?.id
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(if (isSelected) Color(0xFF162035) else Color.Transparent)
                                                        .clickable {
                                                            categoriaSelecionada = cat
                                                            expandedCategoriaDropdown = false
                                                        }
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        cat.nome,
                                                        fontSize = 13.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = if (isSelected) Color(0xFF4ADE80) else Color(0xFF94A3B8)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Lista de peças disponíveis
                            if (categoriaSelecionada != null) {
                                if (pecasLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = BrandOrange,
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                } else if (pecasDisponiveis.isEmpty()) {
                                    Text(
                                        "Nenhuma peça disponível nesta categoria.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        pecasDisponiveis.forEach { peca ->
                                            val assignedProblemaId = pecaAssignments[peca.idPeca]
                                            val assignedLabel = assignedProblemaId?.let { pid ->
                                                problemas.find { it.idProblema == pid }?.maquina
                                            }
                                            Column {
                                                PecaItem(
                                                    peca = peca,
                                                    assignedLabel = assignedLabel,
                                                    onToggle = {
                                                        when {
                                                            assignedProblemaId != null -> {
                                                                // já atribuída a uma máquina -> remove
                                                                pecaAssignments.remove(peca.idPeca)
                                                                pecaEmSelecao = null
                                                            }
                                                            problemas.size <= 1 -> {
                                                                // só existe 1 máquina nesta solicitação -> atribui direto
                                                                problemas.firstOrNull()?.idProblema?.let {
                                                                    pecaAssignments[peca.idPeca] = it
                                                                }
                                                            }
                                                            else -> {
                                                                // várias máquinas -> abre seletor de qual máquina usou a peça
                                                                pecaEmSelecao = if (pecaEmSelecao == peca.idPeca) null else peca.idPeca
                                                            }
                                                        }
                                                    }
                                                )
                                                if (pecaEmSelecao == peca.idPeca && problemas.size > 1) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(start = 30.dp, top = 2.dp, bottom = 6.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        problemas.forEach { p ->
                                                            val pid = p.idProblema
                                                            if (pid != null) {
                                                                val numMaq = p.maquina?.substringBefore(" - ")?.trim() ?: "?"
                                                                AssistChip(
                                                                    onClick = {
                                                                        pecaAssignments[peca.idPeca] = pid
                                                                        pecaEmSelecao = null
                                                                    },
                                                                    label = { Text("Maq. $numMaq", fontSize = 11.sp) },
                                                                    colors = AssistChipDefaults.assistChipColors(
                                                                        containerColor = Color(0xFF0F1623),
                                                                        labelColor = Color(0xFF4ADE80)
                                                                    ),
                                                                    border = AssistChipDefaults.assistChipBorder(
                                                                        enabled = true,
                                                                        borderColor = Color(0xFF334155)
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (pecaAssignments.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(Color(0xFF0D2A1A))
                                                .border(0.5.dp, Color(0xFF1E3A2A), RoundedCornerShape(7.dp))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF4ADE80),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            val resumo = problemas.mapNotNull { p ->
                                                val pid = p.idProblema ?: return@mapNotNull null
                                                val codigos = pecaAssignments.filterValues { it == pid }.keys
                                                    .mapNotNull { id -> pecaInfoCache[id]?.codigo }
                                                if (codigos.isEmpty()) null else {
                                                    val numMaq = p.maquina?.substringBefore(" - ")?.trim() ?: "?"
                                                    "Maq.$numMaq: ${codigos.joinToString(", ")}"
                                                }
                                            }.joinToString("  •  ")
                                            Text(
                                                "${pecaAssignments.size} peça(s) — $resumo",
                                                fontSize = 11.sp,
                                                color = Color(0xFF4ADE80),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Espaço extra para o bottomBar não cobrir
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PecaItem(
    peca: PecaDTO,
    assignedLabel: String?,
    onToggle: () -> Unit
) {
    val selecionada = assignedLabel != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(if (selecionada) Color(0xFF0D1E2A) else Color.Transparent)
            .clickable { onToggle() }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (selecionada) BrandOrange else Color.Transparent)
                .border(
                    0.5.dp,
                    if (selecionada) BrandOrange else Color(0xFF334155),
                    RoundedCornerShape(5.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selecionada) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                peca.codigo,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "${peca.categoriaNome ?: ""}",
                fontSize = 10.sp,
                color = Color(0xFF64748B)
            )
        }

        val badgeText = if (assignedLabel != null) {
            "Maq. ${assignedLabel.substringBefore(" - ").trim()}"
        } else {
            "estoque"
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF0D2A1A))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                badgeText,
                fontSize = 10.sp,
                color = Color(0xFF4ADE80)
            )
        }
    }
}
