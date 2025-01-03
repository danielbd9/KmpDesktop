package org.company.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import okhttp3.OkHttpClient
import okhttp3.Request
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream
import javax.sound.sampled.*

@Composable
fun AudioScreen() {
    val recognizer = remember { VoskRecognizer() }
    val completedPhrases = remember { mutableStateListOf<String>() }
    var isRecording by remember { mutableStateOf(false) }
    var currentPartial by remember { mutableStateOf("") }
    var job: Job? by remember { mutableStateOf(null) }

    // Paleta de Cores
    val textColor = Color(0xFFFFFFFF) // Branco
    val goldTextColor = Color(0xFFFFD700) // Dourado
    val recordingColor = Color(0xFFFF5252) // Vermelho

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Text(
            text = "Gravador e Tradutor de Voz",
            style = androidx.compose.ui.text.TextStyle(
                color = goldTextColor,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 20.sp
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                println("Botão clicado. isRecording atual: $isRecording")
                isRecording = !isRecording
                println("Novo estado isRecording: $isRecording")

                if (isRecording) {
                    job = CoroutineScope(Dispatchers.IO).launch {
                        try {
                            println("Iniciando a coroutine...")
                            val format = AudioFormat(16000f, 16, 1, true, false)

                            var vbCableLine: TargetDataLine? = null
                            val mixerInfos = AudioSystem.getMixerInfo()
                            for (mixerInfo in mixerInfos) {
                                val mixer = AudioSystem.getMixer(mixerInfo)
                                // Corrigindo a lógica de seleção do mixer:
                                if (mixerInfo.name.contains("CABLE Output (VB-Audio Virtual Cable)")) {
                                    println("Mixer encontrado: ${mixerInfo.name}")
                                    val targetLineInfos = mixer.targetLineInfo
                                    for (lineInfo in targetLineInfos) {
                                        if (lineInfo.lineClass == TargetDataLine::class.java) {
                                            try {
                                                val possibleLine = mixer.getLine(lineInfo) as TargetDataLine
                                                possibleLine.open(format)
                                                vbCableLine = possibleLine
                                                println("Linha aberta: $lineInfo")
                                                break
                                            } catch (e: LineUnavailableException) {
                                                println("Não foi possível abrir a linha: ${e.message}")
                                            }
                                        }
                                    }
                                }
                                if (vbCableLine != null) break
                            }

                            if (vbCableLine == null) {
                                println("Nenhuma linha do VB-Cable encontrada.")
                                withContext(Dispatchers.Main) {
                                    isRecording = false
                                }
                                return@launch
                            }

                            vbCableLine.start()
                            println("Linha iniciada.")

                            val buffer = ByteArray(vbCableLine.bufferSize)
                            while (isActive) {
                                val read = vbCableLine.read(buffer, 0, buffer.size)
                                println("Bytes lidos: $read")
                                if (read > 0) {
                                    val partialResult = recognizer.acceptAudio(buffer, read)
                                    val text = partialResult.first
                                    val isFinal = partialResult.second

                                    if (!text.isNullOrEmpty()) {
                                        withContext(Dispatchers.Swing) {
                                            if (isFinal) {
                                                println("Frase completa: $text")
                                                val translatedText = translateText(text) // Traduz a frase
                                                completedPhrases.add(0, translatedText) // Adiciona a frase traduzida
                                                currentPartial = ""
                                            } else {
                                                println("Frase parcial: $text")
                                                val translatedPartial = translateText(text) // Traduz a frase parcial
                                                currentPartial = translatedPartial
                                            }
                                        }
                                    }
                                }
                            }

                            vbCableLine.stop()
                            vbCableLine.close()
                            println("Linha parada e fechada.")
                        } catch (e: CancellationException) {
                            println("Coroutine cancelada: ${e.message}")
                        } catch (e: Exception) {
                            println("Erro na coroutine: ${e.message}")
                            e.printStackTrace()
                        } finally {
                            withContext(Dispatchers.Main) {
                                isRecording = false
                            }
                        }
                    }
                } else {
                    println("Cancelando a coroutine...")
                    job?.cancel()
                    println("Coroutine cancelada. Atualizando isRecording para false...")
                    isRecording = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = goldTextColor)
        ) {
            Text(
                text = if (isRecording) "Parar Gravação" else "Iniciar Gravação do VB-Cable",
                color = Color.Black,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
        }

        if (isRecording) {
            Text(
                text = "Gravando...",
                style = androidx.compose.ui.text.TextStyle(
                    color = recordingColor,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Text(
            text = "Texto Parcial:",
            style = androidx.compose.ui.text.TextStyle(
                color = textColor,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 16.sp
            ),
            modifier = Modifier.padding(top = 16.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxHeight(0.3f)) {
            items(listOf(currentPartial)) { text ->
                Text(
                    text = text,
                    style = androidx.compose.ui.text.TextStyle(
                        color = goldTextColor,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Text(
            text = "Frases Completas:",
            style = androidx.compose.ui.text.TextStyle(
                color = textColor,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 16.sp
            ),
            modifier = Modifier.padding(top = 16.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxHeight()) {
            items(completedPhrases) { phrase ->
                Text(
                    text = phrase,
                    style = androidx.compose.ui.text.TextStyle(
                        color = goldTextColor,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

// Função de tradução (agora fora da coroutine)
suspend fun translateText(text: String): String = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=pt&dt=t&q=${java.net.URLEncoder.encode(text, "UTF-8")}"
    val request = Request.Builder().url(url).build()

    try {
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext "Erro na tradução"
        val jsonArray = JsonParser.parseString(responseBody).asJsonArray
        val translatedText = jsonArray[0].asJsonArray.joinToString("") { it.asJsonArray[0].asString }
        return@withContext translatedText
    } catch (e: Exception) {
        println("Erro na tradução: ${e.message}")
        return@withContext "Erro na tradução"
    }
}

class VoskRecognizer {

    private lateinit var model: Model
    private lateinit var recognizer: Recognizer

    init {
        val modelFolderName = "vosk-model-small-en-us-0.15"
        val modelPath = copyModelToInternalStorage(modelFolderName)

        try {
            model = Model(modelPath)
            recognizer = Recognizer(model, 16000f)
            recognizer.setWords(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun acceptAudio(buffer: ByteArray, length: Int): Pair<String?, Boolean> {
        return if (recognizer.acceptWaveForm(buffer, length)) {
            val jsonResult = recognizer.result
            println("JSON Final: $jsonResult") // Log do JSON final
            val resultObject = parseJson(jsonResult)
            Pair(resultObject?.get("text") as? String, true)
        } else {
            val jsonPartial = recognizer.partialResult
            println("JSON Parcial: $jsonPartial") // Log do JSON parcial
            val partialObject = parseJson(jsonPartial)
            Pair(partialObject?.get("partial") as? String, false)
        }
    }

    private fun parseJson(jsonString: String): Map<String, Any?>? {
        return try {
            val jsonElement = JsonParser.parseString(jsonString)
            if (jsonElement.isJsonObject) {
                val jsonObject = jsonElement.asJsonObject
                jsonObject.entrySet().associate { entry ->
                    entry.key to parseJsonElement(entry.value)
                }
            } else {
                println("JSON não é um objeto válido: $jsonString")
                null
            }
        } catch (e: Exception) {
            println("Erro ao analisar JSON: ${e.message}")
            println("JSON inválido: $jsonString")
            null
        }
    }

    private fun parseJsonElement(element: JsonElement): Any? {
        return when {
            element.isJsonObject -> element.asJsonObject.entrySet().associate { it.key to parseJsonElement(it.value) }
            element.isJsonArray -> element.asJsonArray.map { parseJsonElement(it) }
            element.isJsonNull -> null
            else -> element.asString
        }
    }

    private fun copyModelToInternalStorage(modelFolderName: String): String {
        // Adaptado para carregar o modelo do classpath no Desktop
        val modelPath = "$modelFolderName"
        val zipPath = "$modelPath.zip"

        if (!File(modelPath).exists()) {
            try {
                val inputStream = javaClass.classLoader.getResourceAsStream("$modelFolderName.zip")
                inputStream?.use { input ->
                    FileOutputStream(zipPath).use { outputStream ->
                        input.copyTo(outputStream)
                    }
                }
                unzip(zipPath, ".") // Descompacta na pasta atual
                File(zipPath).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return modelPath
    }

    private fun unzip(zipFilePath: String, targetPath: String) {
        val buffer = ByteArray(1024)
        val zipInputStream = ZipInputStream(FileInputStream(zipFilePath))

        var zipEntry = zipInputStream.nextEntry
        while (zipEntry != null) {
            val newFile = File(targetPath, zipEntry.name)
            if (zipEntry.isDirectory) {
                newFile.mkdirs()
            } else {
                newFile.outputStream().use { output ->
                    var len: Int
                    while (zipInputStream.read(buffer).also { len = it } > 0) {
                        output.write(buffer, 0, len)
                    }
                }
            }
            zipEntry = zipInputStream.nextEntry
        }
        zipInputStream.close()
    }
}