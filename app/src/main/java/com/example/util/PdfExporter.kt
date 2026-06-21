package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.Cliente
import com.example.data.model.Maquina
import com.example.data.model.ConvertRegiao
import com.example.data.repository.DataRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val TAG = "PdfExporter"

    fun exportClientesPdf(context: Context, clientes: List<Cliente>): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 points
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1A237E") // Deep Indigo
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#37474F")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val headerPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#1A237E")
            style = Paint.Style.FILL
        }

        val rowTextPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val rowTextBoldPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val evenRowBgPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5") // Very light grey
            style = Paint.Style.FILL
        }

        val oddRowBgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#CFD8DC")
            strokeWidth = 1f
        }

        try {
            // Title
            canvas.drawText("Relatório Comercial de Clientes", 40f, 50f, titlePaint)
            
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val dateStr = "Gerado em: ${sdf.format(Date())} | Total: ${clientes.size} clientes"
            canvas.drawText(dateStr, 40f, 68f, subtitlePaint)

            canvas.drawLine(40f, 78f, 555f, 78f, linePaint)

            // Table Headers
            val colXCoords = floatArrayOf(40f, 90f, 260f, 360f, 430f, 500f) // X points for columns
            val colHeaders = listOf("Cód.", "Razão Social / Nome", "Bairro / Endereço", "Telefone", "Regi.", "Status")

            // Draw header background
            canvas.drawRect(40f, 90f, 555f, 112f, headerBgPaint)
            for (idx in colHeaders.indices) {
                canvas.drawText(colHeaders[idx], colXCoords[idx] + 3f, 105f, headerPaint)
            }

            var currentY = 125f
            val itemHeight = 22f
            val itemsPerPage = 28
            var itemCounter = 0
            var pageNumber = 1

            for ((index, cliente) in clientes.withIndex()) {
                if (itemCounter >= itemsPerPage) {
                    // Start a new A4 page
                    pdfDocument.finishPage(page)
                    pageNumber++
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(newPageInfo)
                    canvas = page.canvas
                    currentY = 50f

                    // Header in new page
                    canvas.drawRect(40f, currentY - 15f, 555f, currentY + 7f, headerBgPaint)
                    for (idx in colHeaders.indices) {
                        canvas.drawText(colHeaders[idx], colXCoords[idx] + 3f, currentY + 1f, headerPaint)
                    }
                    currentY += 22f
                    itemCounter = 0
                }

                // Draw Row Background with alternating colors
                val rowBgPaint = if (index % 2 == 0) evenRowBgPaint else oddRowBgPaint
                canvas.drawRect(40f, currentY - 10f, 555f, currentY + 12f, rowBgPaint)

                // Fill Row Data
                val codStr = cliente.codCliente?.toString() ?: "-"
                val nameStr = truncText(cliente.nomCliente ?: "-", 34)
                val bairroStr = truncText((cliente.bairro ?: "") + " - " + (cliente.logradouro ?: ""), 22)
                val telStr = cliente.telefone ?: "-"
                val regStr = ConvertRegiao.fromCode(cliente.regiao)
                val activeStr = if (cliente.ativo == true) "ATIVO" else "INATIVO"

                canvas.drawText(codStr, colXCoords[0] + 3f, currentY, rowTextBoldPaint)
                canvas.drawText(nameStr, colXCoords[1] + 3f, currentY, rowTextPaint)
                canvas.drawText(bairroStr, colXCoords[2] + 3f, currentY, rowTextPaint)
                canvas.drawText(telStr, colXCoords[3] + 3f, currentY, rowTextPaint)
                canvas.drawText(regStr, colXCoords[4] + 3f, currentY, rowTextPaint)
                
                // Draw Active color-coded
                val activePaint = Paint(rowTextBoldPaint).apply {
                    color = if (cliente.ativo == true) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
                }
                canvas.drawText(activeStr, colXCoords[5] + 3f, currentY, activePaint)

                // Bottom line
                canvas.drawLine(40f, currentY + 12f, 555f, currentY + 12f, linePaint)

                currentY += itemHeight
                itemCounter++
            }

            // Draw a summary section on the last page if space permits, or new page
            if (currentY > 750f) {
                pdfDocument.finishPage(page)
                pageNumber++
                val lastPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(lastPageInfo)
                canvas = page.canvas
                currentY = 50f
            }

            // Summary box
            val summaryBg = Paint().apply {
                color = Color.parseColor("#ECEFF1")
                style = Paint.Style.FILL
            }
            canvas.drawRect(40f, currentY + 10f, 555f, currentY + 70f, summaryBg)
            
            val summaryTitlePaint = Paint().apply {
                color = Color.parseColor("#37474F")
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Resumo do Relatório", 50f, currentY + 28f, summaryTitlePaint)

            // Calcula o total de máquinas dos clientes ignorando as excluídas (INFORME/COMUNICADO)
            val summaryText = "Ativos: ${clientes.count { it.ativo == true }}  |  Inativos: ${clientes.count { it.ativo != true }}  |  Total de Máquinas Vinculadas: ${clientes.sumOf { it.maquinas?.count { !it.isExcluded() } ?: 0 }}"
            canvas.drawText(summaryText, 50f, currentY + 46f, subtitlePaint)
            
            // Signature / footer
            canvas.drawText("Assinatura do Responsável Técnico: ___________________________", 50f, currentY + 62f, subtitlePaint)

            pdfDocument.finishPage(page)

            // Write to file
            val file = File(context.cacheDir, "relatorio_clientes_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export PDF: ${e.message}", e)
            pdfDocument.close()
            return null
        }
    }

    fun exportMaquinasPdf(context: Context, maquinas: List<Maquina>, repository: DataRepository): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#2E7D32") // Emerald green styled for machines/hardware
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#37474F")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val headerPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#2E7D32")
            style = Paint.Style.FILL
        }

        val rowTextPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val rowTextBoldPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val evenRowBgPaint = Paint().apply {
            color = Color.parseColor("#F1F8E9") // Very light green hue
            style = Paint.Style.FILL
        }

        val oddRowBgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#CFD8DC")
            strokeWidth = 1f
        }

        try {
            canvas.drawText("Relatório de Máquinas e Equipamentos", 40f, 50f, titlePaint)
            
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val dateStr = "Gerado em: ${sdf.format(Date())} | Total: ${maquinas.size} equipamentos"
            canvas.drawText(dateStr, 40f, 68f, subtitlePaint)

            canvas.drawLine(40f, 78f, 555f, 78f, linePaint)

            // Table Headers
            val colXCoords = floatArrayOf(40f, 80f, 180f, 280f, 440f, 500f)
            val colHeaders = listOf("ID", "Máquina / Equip.", "Jogo / Tipo", "Cliente Proprietário", "Nº Placa", "Status")

            // Draw header bg
            canvas.drawRect(40f, 90f, 555f, 112f, headerBgPaint)
            for (idx in colHeaders.indices) {
                canvas.drawText(colHeaders[idx], colXCoords[idx] + 3f, 105f, headerPaint)
            }

            var currentY = 125f
            val itemHeight = 22f
            val itemsPerPage = 28
            var itemCounter = 0
            var pageNumber = 1

            for ((index, maquina) in maquinas.withIndex()) {
                if (itemCounter >= itemsPerPage) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(newPageInfo)
                    canvas = page.canvas
                    currentY = 50f

                    // Header in new page
                    canvas.drawRect(40f, currentY - 15f, 555f, currentY + 7f, headerBgPaint)
                    for (idx in colHeaders.indices) {
                        canvas.drawText(colHeaders[idx], colXCoords[idx] + 3f, currentY + 1f, headerPaint)
                    }
                    currentY += 22f
                    itemCounter = 0
                }

                // Alternate bg row
                val rowBgPaint = if (index % 2 == 0) evenRowBgPaint else oddRowBgPaint
                canvas.drawRect(40f, currentY - 10f, 555f, currentY + 12f, rowBgPaint)

                // Fill Row Data
                val idStr = maquina.id?.toString() ?: "-"
                val maqName = truncText(maquina.nom_maq ?: "-", 18)
                val jogoName = truncText(maquina.nom_jogo ?: "-", 18)
                
                // Retrieve owner client
                val owner = repository.getClientForMachine(maquina.codCliente)
                val ownerStr = if (owner != null) truncText("(${owner.codCliente}) ${owner.nomCliente}", 28) else "Cód. Cliente: ${maquina.codCliente ?: "-"}"
                
                val placaStr = maquina.numeroPlaca ?: "-"
                val activeStr = if (maquina.ativo == true) "ATIVA" else "INATIVA"

                canvas.drawText(idStr, colXCoords[0] + 3f, currentY, rowTextBoldPaint)
                canvas.drawText(maqName, colXCoords[1] + 3f, currentY, rowTextPaint)
                canvas.drawText(jogoName, colXCoords[2] + 3f, currentY, rowTextPaint)
                canvas.drawText(ownerStr, colXCoords[3] + 3f, currentY, rowTextPaint)
                canvas.drawText(placaStr, colXCoords[4] + 3f, currentY, rowTextPaint)

                // Draw Active color-coded
                val activePaint = Paint(rowTextBoldPaint).apply {
                    color = if (maquina.ativo == true) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
                }
                canvas.drawText(activeStr, colXCoords[5] + 3f, currentY, activePaint)

                // Bottom line
                canvas.drawLine(40f, currentY + 12f, 555f, currentY + 12f, linePaint)

                currentY += itemHeight
                itemCounter++
            }

            // Last page summary elements
            if (currentY > 750f) {
                pdfDocument.finishPage(page)
                pageNumber++
                val lastPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(lastPageInfo)
                canvas = page.canvas
                currentY = 50f
            }

            // Summary box
            val summaryBg = Paint().apply {
                color = Color.parseColor("#E8F5E9")
                style = Paint.Style.FILL
            }
            canvas.drawRect(40f, currentY + 10f, 555f, currentY + 70f, summaryBg)
            
            val summaryTitlePaint = Paint().apply {
                color = Color.parseColor("#2E7D32")
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Resumo das Máquinas", 50f, currentY + 28f, summaryTitlePaint)

            //  Mantido o cálculo focado exclusivamente em máquinas
            val summaryText = "Ativas: ${maquinas.count { it.ativo == true }}  |  Inativas: ${maquinas.count { it.ativo != true }}"
            canvas.drawText(summaryText, 50f, currentY + 46f, subtitlePaint)
            
            // Signature / footer
            canvas.drawText("Vistoria Técnica de Lacres e Placas realizada por: ___________________________", 50f, currentY + 62f, subtitlePaint)

            pdfDocument.finishPage(page)

            // Write to file
            val file = File(context.cacheDir, "relatorio_maquinas_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export PDF: ${e.message}", e)
            pdfDocument.close()
            return null
        }
    }

    private fun truncText(text: String, length: Int): String {
        return if (text.length > length) {
            text.substring(0, length - 3) + "..."
        } else {
            text
        }
    }

    // Trigger intent to view or share the generated PDF
    fun triggerSharePdf(context: Context, file: File) {
        try {
            // Get URI using FileProvider
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(shareIntent, "Abrir Relatório PDF")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening PDF intent: ${e.message}", e)

            // Failover to ACTION_SEND if ACTION_VIEW is not supported instantly on emulator
            try {
                val authority = "${context.packageName}.fileprovider"
                val uri: Uri = FileProvider.getUriForFile(context, authority, file)
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val chooser = Intent.createChooser(sendIntent, "Enviar Relatório PDF")
                chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.startActivity(chooser)
            } catch (e2: Exception) {
                Log.e(TAG, "Double error launching PDF share: ${e2.message}")
            }
        }
    }
}
