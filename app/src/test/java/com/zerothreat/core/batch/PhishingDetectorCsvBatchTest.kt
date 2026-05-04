package com.zerothreat.core.batch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.zerothreat.core.detector.PhishingDetector
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhishingDetectorCsvBatchTest {

    @Test
    fun runDetectorOnProvidedCsvAndWriteOutput() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val projectRoot = File(System.getProperty("user.dir")).parentFile ?: File(System.getProperty("user.dir"))

        val inputPath = System.getProperty(
            "zerothreat.inputCsv",
            File(projectRoot, "phishing_urls.csv").absolutePath
        )
        val outputPath = System.getProperty(
            "zerothreat.outputCsv",
            File(projectRoot, "detector_results_1000.csv").absolutePath
        )

        val inputFile = File(inputPath)
        require(inputFile.exists()) { "Input CSV not found: $inputPath" }

        val lines = inputFile.readLines(Charsets.UTF_8)
        require(lines.isNotEmpty()) { "Input CSV is empty: $inputPath" }

        val records = lines
            .drop(1)
            .mapNotNull { line ->
                val parts = line.split(',', limit = 2)
                if (parts.size < 2) return@mapNotNull null
                val idx = parts[0].trim().toIntOrNull() ?: return@mapNotNull null
                val rawUrl = parts[1].trim().trim('"')
                if (rawUrl.isBlank()) return@mapNotNull null
                UrlRecord(idx, rawUrl)
            }

        assertEquals("Expected exactly 1000 URL rows", 1000, records.size)

        val outFile = File(outputPath)
        outFile.parentFile?.mkdirs()

        val builder = StringBuilder()
        builder.appendLine("index,input_url,scanner_input,result,score,description,error")

        records.forEach { record ->
            val scannerInput = ensureWebScheme(record.url)
            val row = try {
                val report = PhishingDetector.analyzeDetailed(context, scannerInput, "CSV Batch Test")
                listOf(
                    record.index.toString(),
                    csv(record.url),
                    csv(scannerInput),
                    report.result.name,
                    report.score.toString(),
                    csv(report.description),
                    ""
                )
            } catch (e: Exception) {
                listOf(
                    record.index.toString(),
                    csv(record.url),
                    csv(scannerInput),
                    "ERROR",
                    "0",
                    "",
                    csv(e.message ?: e::class.java.simpleName)
                )
            }
            builder.appendLine(row.joinToString(","))
        }

        outFile.writeText(builder.toString(), Charsets.UTF_8)
        println("Batch result CSV written: ${outFile.absolutePath}")

        assertTrue("Output CSV was not created", outFile.exists())
        assertTrue("Output CSV is empty", outFile.length() > 0)
    }

    private fun ensureWebScheme(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }
    }

    private fun csv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private data class UrlRecord(
        val index: Int,
        val url: String
    )
}

