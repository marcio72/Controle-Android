package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Cliente
import com.example.data.model.ExecucaoDTO
import com.example.data.model.Maquina
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

// ─────────────────────────────────────────────────────────────
// HISTÓRICO POR CLIENTE
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoClienteScreen(
    viewModel: AppViewModel,
    cliente: Cliente,
    onBack: () -> Unit
) {
    val todasExecucoes by viewModel.execucoes.collectAsState()
    val loading by viewModel.execucoesLoading.collectAsState()

    var maquinaSelecionada by remember { mutableStateOf<String?>(null) }
    var dropdownMaquinaAberto by remember { mutableStateOf(false) }

    val historicoCliente = remember(todasExecucoes, cliente) {
        val nome = cliente.nomCliente?.trim()?.lowercase() ?: ""
        todasExecucoes
            .filter { it.nomeCliente?.trim()?.lowercase() == nome }
            .sortedByDescending { it.dataExecucao ?: "" }
    }

    val totalServicos = historicoCliente.size
    val maquinasNumeros = remember(historicoCliente) {
        historicoCliente.mapNotNull { it.nomeMaquina }.distinct().sorted()
    }
    val maquinasAtendidas = maquinasNumeros.size

    val historico = remember(historicoCliente, maquinaSelecionada) {
        if (maquinaSelecionada == null) historicoCliente
        else historicoCliente.filter { it.nomeMaquina == maquinaSelecionada }
    }

    LaunchedEffect(Unit) { viewModel.loadExecucoes() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Histórico de Serviços", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text(cliente.nomCliente ?: "", fontSize = 11.sp, color = Color(0xFF64748B),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepDarkNavy)
            )
        },
        containerColor = DeepDarkNavy
    ) { innerPadding ->

        if (loading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandOrange)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResumoCard("Serviços realizados", "$totalServicos", Icons.Default.Build, BrandOrange, Modifier.weight(1f))
                    ResumoCard("Máquinas atendidas", "$maquinasAtendidas", Icons.Default.Devices, Color(0xFF10B981), Modifier.weight(1f))
                }
            }

            if (maquinasNumeros.isNotEmpty()) {
                item {
                    Box {
                        OutlinedButton(
                            onClick = { dropdownMaquinaAberto = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(
                                1.dp,
                                if (maquinaSelecionada != null) BrandOrange.copy(alpha = 0.5f) else Color(0xFF334155)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = maquinaSelecionada?.let { "Máquina: $it" } ?: "Todas as máquinas",
                                    color = if (maquinaSelecionada != null) BrandOrange else Color.White
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B))
                            }
                        }
                        DropdownMenu(
                            expanded = dropdownMaquinaAberto,
                            onDismissRequest = { dropdownMaquinaAberto = false },
                            modifier = Modifier.background(SleekNavyCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todas as máquinas", color = if (maquinaSelecionada == null) BrandOrange else Color.White) },
                                onClick = {
                                    maquinaSelecionada = null
                                    dropdownMaquinaAberto = false
                                }
                            )
                            maquinasNumeros.forEach { numero ->
                                DropdownMenuItem(
                                    text = { Text(numero, color = if (maquinaSelecionada == numero) BrandOrange else Color.White) },
                                    onClick = {
                                        maquinaSelecionada = numero
                                        dropdownMaquinaAberto = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (historico.isEmpty()) {
                item { EmptyHistorico() }
            } else {
                item {
                    Text("SERVIÇOS EXECUTADOS", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B), letterSpacing = 0.08.sp,
                        modifier = Modifier.padding(top = 4.dp))
                }
                items(historico) { exec ->
                    ExecucaoColapsavelCard(exec)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HISTÓRICO POR MÁQUINA
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoMaquinaScreen(
    viewModel: AppViewModel,
    maquina: Maquina,
    onBack: () -> Unit
) {
    val todasExecucoes by viewModel.execucoes.collectAsState()
    val loading by viewModel.execucoesLoading.collectAsState()

    val historico = remember(todasExecucoes, maquina) {
        val nomeMaq = maquina.nom_maq?.trim()?.lowercase() ?: ""
        todasExecucoes
            .filter { it.nomeMaquina?.trim()?.lowercase() == nomeMaq }
            .sortedByDescending { it.dataExecucao ?: "" }
    }

    val totalServicos = historico.size

    // Nome do cliente dono da máquina
    val nomeCliente = remember(maquina.codCliente) {
        viewModel.repository.getClientForMachine(maquina.codCliente)?.nomCliente ?: ""
    }

    LaunchedEffect(Unit) { viewModel.loadExecucoes() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Histórico da Máquina",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1)
                        if (nomeCliente.isNotBlank()) {
                            Text(nomeCliente,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                        Text(
                            "${maquina.nom_maq ?: ""}${if (!maquina.nom_jogo.isNullOrBlank()) " · ${maquina.nom_jogo}" else ""}",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                expandedHeight = 80.dp,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepDarkNavy)
            )
        },
        containerColor = DeepDarkNavy
    ) { innerPadding ->

        if (loading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandOrange)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResumoCard("Serviços realizados", "$totalServicos", Icons.Default.Build, BrandOrange, Modifier.weight(1f))
                    ResumoCard("Número", maquina.nom_maq ?: "-", Icons.Default.Memory, Color(0xFF6366F1), Modifier.weight(1f))
                }
            }

            if (historico.isEmpty()) {
                item { EmptyHistorico() }
            } else {
                item {
                    Text("SERVIÇOS EXECUTADOS", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B), letterSpacing = 0.08.sp,
                        modifier = Modifier.padding(top = 4.dp))
                }
                items(historico) { exec ->
                    ExecucaoColapsavelCard(exec, mostrarMaquina = false)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// CARD COLAPSÁVEL — resumo → expande → execução completa
// ─────────────────────────────────────────────────────────────
@Composable
fun ExecucaoColapsavelCard(
    exec: ExecucaoDTO,
    mostrarMaquina: Boolean = true
) {
    var expandido by remember { mutableStateOf(false) }

    val dataFormatada = remember(exec.dataExecucao) {
        val raw = exec.dataExecucao ?: return@remember "Data não informada"
        try {
            val norm = raw.replace("T", " ")
            val partes = norm.split(" ")
            val dp = partes.getOrElse(0) { "" }.split("-")
            val hora = partes.getOrElse(1) { "" }.take(5)
            val dia = dp.getOrElse(2) { "" }
            val mes = dp.getOrElse(1) { "" }
            val ano = dp.getOrElse(0) { "" }
            if (hora.isNotBlank()) "$dia/$mes/$ano às $hora" else "$dia/$mes/$ano"
        } catch (e: Exception) { raw }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekNavyCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── RESUMO (sempre visível) ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandido = !expandido }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ícone com cor de status
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(BrandOrange.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Build, contentDescription = null,
                        tint = BrandOrange, modifier = Modifier.size(18.dp))
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    // Número da máquina
                    if (mostrarMaquina && !exec.nomeMaquina.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Máquina:", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text(exec.nomeMaquina, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, color = BrandOrange,
                                fontFamily = FontFamily.Monospace)
                        }
                    }
                    // Problema resumido
                    Text(
                        exec.descricaoProblema ?: "Sem descrição do problema",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Data
                    Text(dataFormatada, fontSize = 10.sp, color = Color(0xFF475569))
                }

                // Botão expandir
                Icon(
                    imageVector = if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expandido) "Recolher" else "Expandir",
                    tint = if (expandido) BrandOrange else Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }

            // ── DETALHE EXPANDIDO ────────────────────────────
            AnimatedVisibility(
                visible = expandido,
                enter = expandVertically(tween(200)) + fadeIn(),
                exit = shrinkVertically(tween(200)) + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = Color(0xFF1E2A3A), thickness = 0.5.dp)
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Problema completo
                        if (!exec.descricaoProblema.isNullOrBlank()) {
                            DetalheItem("Problema relatado", exec.descricaoProblema)
                        }

                        // Serviço executado — terminal verde
                        if (!exec.descricao.isNullOrBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Serviço executado", fontSize = 10.sp, color = Color(0xFF64748B))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color(0xFF0F1623))
                                        .border(0.5.dp, Color(0xFF1E2A3A), RoundedCornerShape(7.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(exec.descricao, fontSize = 12.sp,
                                        color = Color(0xFF4ADE80),
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 17.sp)
                                }
                            }
                        }

                        // Rodapé — técnico + PDF
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Person, contentDescription = null,
                                    tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                                Text(exec.tecnico ?: "Técnico não informado",
                                    fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            if (exec.pdfGerado == true) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null,
                                        tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                    Text("PDF gerado", fontSize = 10.sp, color = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// COMPONENTES AUXILIARES
// ─────────────────────────────────────────────────────────────
@Composable
fun ResumoCard(
    label: String,
    valor: String,
    icon: ImageVector,
    cor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SleekNavyCard),
        modifier = modifier
    ) {
        Row(modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp))
                .background(cor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = cor, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(valor, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(label, fontSize = 10.sp, color = Color(0xFF64748B), lineHeight = 13.sp)
            }
        }
    }
}

@Composable
fun EmptyHistorico() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.History, contentDescription = null,
                tint = Color(0xFF334155), modifier = Modifier.size(48.dp))
            Text("Nenhum serviço registrado.", color = Color(0xFF64748B),
                textAlign = TextAlign.Center, fontSize = 14.sp)
        }
    }
}

@Composable
fun DetalheItem(label: String, valor: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 10.sp, color = Color(0xFF64748B))
        Text(valor, fontSize = 12.sp, color = Color(0xFF94A3B8), lineHeight = 17.sp)
    }
}
