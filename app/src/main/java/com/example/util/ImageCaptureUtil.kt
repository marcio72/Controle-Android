package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Utilitário para captura de foto via câmera do sistema e compressão antes do envio.
 *
 * Fluxo:
 *  1. [criarUriTemporaria] cria um arquivo temporário (via FileProvider) para a câmera salvar a foto
 *  2. Depois que a câmera retorna sucesso, [comprimirParaBase64] lê esse arquivo, redimensiona,
 *     corrige rotação (EXIF) e comprime pra JPEG, devolvendo já em Base64 pronto pra mandar pro backend
 */
object ImageCaptureUtil {

    private const val MAX_DIMENSAO_PX = 1280
    private const val QUALIDADE_JPEG = 75

    /**
     * Cria um arquivo temporário na pasta de cache do app e retorna a Uri (via FileProvider)
     * que deve ser passada pro Intent/launcher de captura de foto.
     */
    fun criarUriTemporaria(context: Context): Uri {
        val pastaCache = File(context.cacheDir, "fotos_temp").apply { mkdirs() }
        val arquivo = File(pastaCache, "foto_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            arquivo
        )
    }

    /**
     * Lê a foto capturada na Uri informada, corrige a rotação (fotos de câmera costumam vir
     * com metadado EXIF de rotação em vez de já rotacionadas), redimensiona pra no máximo
     * 1280px no maior lado, comprime em JPEG e retorna como string Base64.
     *
     * Retorna null se não conseguir ler/processar a imagem.
     */
    fun comprimirParaBase64(context: Context, uri: Uri): String? {
        return try {
            val resolver = context.contentResolver

            // Lê os bytes originais
            val bytesOriginais = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

            // Decodifica em Bitmap
            var bitmap = BitmapFactory.decodeByteArray(bytesOriginais, 0, bytesOriginais.size) ?: return null

            // Corrige rotação com base no EXIF
            val rotacao = try {
                resolver.openInputStream(uri)?.use { input ->
                    val exif = ExifInterface(input)
                    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f
            } catch (e: Exception) {
                0f
            }

            if (rotacao != 0f) {
                val matrix = Matrix().apply { postRotate(rotacao) }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }

            // Redimensiona mantendo proporção, se necessário
            val maiorLado = maxOf(bitmap.width, bitmap.height)
            if (maiorLado > MAX_DIMENSAO_PX) {
                val escala = MAX_DIMENSAO_PX.toFloat() / maiorLado
                val novaLargura = (bitmap.width * escala).toInt()
                val novaAltura = (bitmap.height * escala).toInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, novaLargura, novaAltura, true)
            }

            // Comprime em JPEG
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, outputStream)
            val bytesComprimidos = outputStream.toByteArray()

            android.util.Base64.encodeToString(bytesComprimidos, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
