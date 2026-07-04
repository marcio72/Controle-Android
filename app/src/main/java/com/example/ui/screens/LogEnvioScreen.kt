package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LogEnvioDTO
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

// Senha de acesso à tela de logs — altere conforme necessário
private const val SENHA_LOG = "admin123"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEnvioScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    var autenticado by remember { mutableStateOf(false) }

    if (!autenticado) {
        TelaAutenticacaoLog(
            onAutenticado = { autenticado = true },
            onBack = onBack
        )
    } else {
        TelaListaLogs(viewModel = viewModel, onBack = onBack)
    }
}

// ─────────────────────────────────────────────────────────────
// TELA DE AUTENTICAÇÃO
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaAutenticacaoLog(
    onAutenticado: () -> Unit,
    onBack: () -> Unit
) {
    var senha by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acesso Restrito", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = DeepDarkNavy
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ícone
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BrandOrange.copy(alpha = 0.15f))
                    .border(1.dp, BrandOrange.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(36.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Log de Envios", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Digite a senha para acessar", fontSize = 14.sp, color = Color(0xFF64748B), modifier = Modifier.padding(top = 4.dp, bottom = 32.dp))

            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it; erro = false },
                label = { Text("Senha") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = erro,
                supportingText = { if (erro) Text("Senha incorreta", color = Color(0xFFEF4444)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandOrange,
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedLabelColor = BrandOrange,
                    unfocusedLabelColor = Color(0xFF64748B),
                    cursorColor = BrandOrange,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (senha == SENHA_LOG) onAutenticado()
                    else erro = true
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Entrar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TELA DE LISTA DE LOGS
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaListaLogs(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val logs by viewModel.logEnvios.collectAsState()
    val isLoading by viewModel.logEnviosLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadLogEnvios() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log de Envios", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadLogEnvios() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = BrandOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = DeepDarkNavy
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandOrange)
            }
        } else if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(48.dp))
                    Text("Nenhum log encontrado", color = Color(0xFF64748B), fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "${logs.size} registro(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
                items(logs) { log -> LogEnvioCard(log = log) }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun LogEnvioCard(log: LogEnvioDTO) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SleekNavyCard)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Cabeçalho
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandOrange.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Envio #${log.numeroEnvio}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    log.dataEnvio?.let {
                        Text(
                            formatarData(it),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF1E293B))

            // Dados
            LogDetalheRow(icon = Icons.Default.Person, label = "Técnico", valor = log.tecnico ?: "-")
            LogDetalheRow(icon = Icons.Default.Business, label = "Cliente", valor = log.nomeCliente ?: "-")

            // Localização
            val context = LocalContext.current
            if (!log.localizacao.isNullOrBlank()) {
                // Normaliza: troca vírgula decimal por ponto (ex: -23,632764 → -23.632764)
                val locNormalizada = log.localizacao
                    .split(",")
                    .mapIndexed { idx, parte ->
                        // Se for o separador entre lat e lon (posição 1), mantém vírgula → depois replace
                        parte.trim()
                    }
                    .let { partes ->
                        // Formato esperado: "lat, lon" → dois elementos após split por ", "
                        val raw = log.localizacao.replace(",", ".")
                        // Corrige: se ficou "latlon" sem separador, tenta pelo espaço
                        val semEspacos = raw.trim()
                        // Pega lat e lon separando pelo último ponto que não é decimal
                        // Estratégia: split por ". " ou ".  " após normalização
                        semEspacos
                    }

                // Converte "lat, lon" para formato Google Maps com ponto
                val partesBruta = log.localizacao.trim().split(",")
                val latStr = partesBruta.getOrNull(0)?.trim()?.replace(",", ".") ?: ""
                val lonStr = partesBruta.getOrNull(1)?.trim()?.replace(",", ".") ?: ""
                val coordExibicao = "$latStr, $lonStr"
                val coordMaps    = "$latStr,$lonStr"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A2A0A), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .clickable {
                            val uri = Uri.parse("geo:$coordMaps?q=$coordMaps")
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            // Fallback: abre no browser se Maps não estiver instalado
                            val fallback = Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://maps.google.com/?q=$coordMaps"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(fallback)
                            }
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null,
                        tint = Color(0xFF22C55E), modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Localização", fontSize = 11.sp,
                            color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        Text(coordExibicao, fontSize = 13.sp,
                            color = Color(0xFF22C55E), fontWeight = FontWeight.SemiBold)
                    }
                    Text("Ver no Mapa", fontSize = 11.sp,
                        color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.OpenInNew, contentDescription = null,
                        tint = Color(0xFF22C55E), modifier = Modifier.size(14.dp))
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.LocationOff, contentDescription = null,
                        tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    Text("Localização não disponível", fontSize = 13.sp, color = Color(0xFF64748B))
                }
            }
        }
    }
}

@Composable
fun LogDetalheRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    valor: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
        Text(label, fontSize = 11.sp, color = Color(0xFF64748B), modifier = Modifier.width(60.dp))
        Text(valor, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatarData(data: String): String {
    return try {
        val partes = data.split("T")
        val dia = partes[0].split("-").reversed().joinToString("/")
        val hora = if (partes.size > 1) partes[1].take(5) else ""
        "$dia $hora"
    } catch (e: Exception) { data }
}
