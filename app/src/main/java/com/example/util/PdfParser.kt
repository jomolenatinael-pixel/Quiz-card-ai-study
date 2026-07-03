package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.example.data.api.InlineData
import com.example.data.api.Part
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object PdfParser {

    /**
     * Renders PDF pages into images, compresses them, and encodes them to Base64.
     * This allows Gemini 3.5 Flash to act as an on-device OCR/parser.
     */
    fun extractPdfPagesAsParts(context: Context, uri: Uri, maxPages: Int = 5): List<Part> {
        val parts = mutableListOf<Part>()
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var tempFile: File? = null

        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            
            // Write input stream to a temporary file
            tempFile = File.createTempFile("pdf_temp", ".pdf", context.cacheDir)
            tempFile.deleteOnExit()
            
            val outputStream = FileOutputStream(tempFile)
            var totalBytes: Long = 0
            val maxBytes = 15 * 1024 * 1024 // 15MB limit
            
            inputStream.use { input ->
                outputStream.use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead = input.read(buffer)
                    while (bytesRead != -1) {
                        totalBytes += bytesRead
                        if (totalBytes > maxBytes) {
                            throw Exception("PDF file exceeds the 15MB size limit. Please upload a smaller document.")
                        }
                        output.write(buffer, 0, bytesRead)
                        bytesRead = input.read(buffer)
                    }
                }
            }
            
            parcelFileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            if (parcelFileDescriptor != null) {
                try {
                    renderer = PdfRenderer(parcelFileDescriptor)
                } catch (se: Exception) {
                    throw Exception("This PDF is encrypted or password-protected. Please upload an unprotected text-based PDF.")
                }
                val pageCount = renderer.pageCount
                val pagesToRender = minOf(pageCount, maxPages)

                for (i in 0 until pagesToRender) {
                    val page = renderer.openPage(i)
                    
                    // Scale bitmap down to a reasonable size to fit within token limit and network size
                    val width = page.width
                    val height = page.height
                    val targetDimension = 1000f
                    val scale = if (width > height) targetDimension / width else targetDimension / height
                    val targetWidth = (width * scale).toInt()
                    val targetHeight = (height * scale).toInt()
                    
                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE) // Ensure opaque background

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    // Compress to JPEG (75% quality is excellent for text readability and saves massive bytes)
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                    val base64Data = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    
                    parts.add(
                        Part(
                            inlineData = InlineData(
                                mimeType = "image/jpeg",
                                data = base64Data
                            )
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            try {
                renderer?.close()
                parcelFileDescriptor?.close()
                tempFile?.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return parts
    }
}
