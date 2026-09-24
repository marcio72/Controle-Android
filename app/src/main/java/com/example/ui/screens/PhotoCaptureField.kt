package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.ImageCaptureUtil

/**
 * Campo reutilizável de "tirar foto": mostra um botão que abre a câmera do sistema;
 * depois de capturada, mostra uma miniatura com opção de remover ou tirar outra.
 *
 * [fotoBase64] é o estado atual (null = sem foto ainda). Ao capturar e comprimir
 * com sucesso, chama [onFotoCapturada] com a string Base64 pronta pra enviar no JSON.
 */
@Composable
fun PhotoCaptureField(
    fotoBase64: String?,
    onFotoCapturada: (String?) -> Unit,
    label: String = "Foto (opcional)",
    keyId: String? = null
) {
    val context = LocalContext.current
    // rememberSaveable (não remember simples): a Uri precisa sobreviver caso o
    // Android recrie a Activity/processo enquanto a câmera do sistema está aberta.
    // key = keyId: essencial quando este componente é usado várias vezes na mesma
    // tela (ex: uma foto por problema/máquina numa solicitação com várias
    // máquinas) — sem uma chave própria por instância, o Compose tenta salvar
    // duas Uris sob a mesma chave ao abrir a câmera (backgrounding => onSaveInstanceState)
    // e derruba o app com "IllegalArgumentException: key already used".
    var uriTemp by rememberSaveable(key = keyId?.let { "foto_uri_$it" }) { mutableStateOf<Uri?>(null) }
    var processando by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { sucesso ->
        val uri = uriTemp
        if (sucesso && uri != null) {
            processando = true
            val base64 = ImageCaptureUtil.comprimirParaBase64(context, uri)
            processando = false
            onFotoCapturada(base64)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            val novaUri = ImageCaptureUtil.criarUriTemporaria(context)
            uriTemp = novaUri
            cameraLauncher.launch(novaUri)
        }
    }

    fun iniciarCaptura() {
        val temPermissao = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (temPermissao) {
            val novaUri = ImageCaptureUtil.criarUriTemporaria(context)
            uriTemp = novaUri
            cameraLauncher.launch(novaUri)
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Column {
        Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))

        if (fotoBase64 != null) {
            val bytes = remember(fotoBase64) {
                try { Base64.decode(fotoBase64, Base64.NO_WRAP) } catch (e: Exception) { null }
            }
            val bitmap = remember(bytes) {
                bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Foto capturada",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Foto anexada",
                        color = Color(0xFF4ADE80),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { iniciarCaptura() }, contentPadding = PaddingValues(0.dp)) {
                        Text("Tirar outra", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                }
                IconButton(onClick = { onFotoCapturada(null) }) {
                    Icon(Icons.Default.Close, contentDescription = "Remover foto", tint = Color(0xFFEF4444))
                }
            }
        } else {
            OutlinedButton(
                onClick = { iniciarCaptura() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !processando,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (processando) "Processando..." else "Tirar Foto")
            }
        }
    }
}
