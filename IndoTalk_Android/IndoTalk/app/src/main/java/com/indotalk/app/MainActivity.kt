package com.indotalk.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale

private val glossary = listOf(
    "開膠" to "lem terlepas / delaminasi",
    "處理劑" to "primer / bahan treatment",
    "漏刷處理劑" to "primer tidak diaplikasikan",
    "不良品隔離" to "pisahkan produk NG",
    "拉力測試" to "uji kekuatan tarik",
    "實驗室" to "laboratorium",
    "品質稽核" to "audit kualitas",
    "混膠室" to "ruang pencampuran lem",
    "硬化劑" to "hardener / bahan pengeras",
    "膠水可使用時間" to "pot life lem",
    "鞋底" to "sol sepatu",
    "成型線" to "lini assembling sepatu",
    "退料" to "pengembalian material",
    "重工" to "rework / pengerjaan ulang"
)
private val slang = listOf(
    "gak" to "不、沒有（口語）", "nggak" to "不、沒有（口語）",
    "ga" to "不（口語）", "udah" to "已經", "belom" to "還沒",
    "gimana" to "怎麼樣", "kenapa" to "為什麼", "dong" to "強調或撒嬌語氣",
    "sih" to "語氣助詞，可能帶疑問或強調", "loh" to "驚訝或提醒語氣",
    "jg" to "也（juga 的縮寫）", "yg" to "……的（yang 的縮寫）",
    "gw" to "我（非常口語）", "lu" to "你（非常口語）",
    "makasih" to "謝謝（口語）", "otw" to "正在路上"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IndoTalkScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IndoTalkScreen() {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("首次翻譯需下載語言模型，請連接網路。") }
    var chineseToIndonesian by remember { mutableStateOf(true) }
    var tab by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val engine = TextToSpeech(context) { code -> ready = code == TextToSpeech.SUCCESS }
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            input = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        }
    }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) status = "未授予麥克風權限。"
    }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = if (chineseToIndonesian)
                    TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                else TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                status = "正在辨識圖片文字…"
                recognizer.process(image).addOnSuccessListener { text ->
                    input = text.text
                    status = if (input.isBlank()) "沒有辨識到文字，請使用清晰截圖。" else "辨識完成，請檢查文字後按翻譯。"
                }.addOnFailureListener { status = "圖片辨識失敗：${it.localizedMessage}" }
                    .addOnCompleteListener { recognizer.close() }
            } catch (e: Exception) { status = "無法讀取圖片：${e.localizedMessage}" }
        }
    }
    fun translate() {
        if (input.isBlank() || busy) return
        busy = true
        status = "正在準備翻譯模型…"
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(if (chineseToIndonesian) TranslateLanguage.CHINESE else TranslateLanguage.INDONESIAN)
            .setTargetLanguage(if (chineseToIndonesian) TranslateLanguage.INDONESIAN else TranslateLanguage.CHINESE)
            .build()
        val translator = Translation.getClient(options)
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener {
                translator.translate(input).addOnSuccessListener { translated ->
                    output = translated
                    status = "翻譯完成。印尼口語或品質專業內容請確認語境。"
                }.addOnFailureListener { status = "翻譯失敗：${it.localizedMessage}" }
                    .addOnCompleteListener { busy = false; translator.close() }
            }.addOnFailureListener {
                busy = false
                status = "語言模型下載失敗，請確認網路及儲存空間：${it.localizedMessage}"
                translator.close()
            }
    }
    MaterialTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("IndoTalk 印尼通") }) }) { padding ->
            Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("繁體中文 × Bahasa Indonesia", style = MaterialTheme.typography.titleMedium)
                TabRow(selectedTabIndex = tab) {
                    listOf("翻譯", "口語解讀", "鞋廠詞庫").forEachIndexed { i, title ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                    }
                }
                if (tab == 0) {
                    Button(onClick = { chineseToIndonesian = !chineseToIndonesian; val old = input; input = output; output = old }) {
                        Text(if (chineseToIndonesian) "繁體中文 → 印尼文  ⇄" else "印尼文 → 繁體中文  ⇄")
                    }
                    OutlinedTextField(input, { input = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("輸入或貼上文字") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { imageLauncher.launch("image/*") }) { Text("選取截圖") }
                        Button(onClick = {
                            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                try {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (chineseToIndonesian) "zh-TW" else "id-ID")
                                    }
                                    speechLauncher.launch(intent)
                                } catch (_: Exception) { status = "找不到可用的語音辨識服務。" }
                            }
                        }) { Text("語音輸入") }
                    }
                    Button(onClick = { translate() }, enabled = !busy && input.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        Text(if (busy) "翻譯中…" else "開始翻譯")
                    }
                    OutlinedTextField(output, { output = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("翻譯結果（可修改）") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            (context.getSystemService(ClipboardManager::class.java)).setPrimaryClip(ClipData.newPlainText("IndoTalk", output))
                            status = "已複製到剪貼簿。"
                        }, enabled = output.isNotBlank()) { Text("複製") }
                        Button(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, output) }
                            context.startActivity(Intent.createChooser(intent, "分享到 WhatsApp 或其他 App"))
                        }, enabled = output.isNotBlank()) { Text("分享") }
                        Button(onClick = {
                            if (ready) {
                                tts?.language = if (chineseToIndonesian) Locale.forLanguageTag("id-ID") else Locale.forLanguageTag("zh-TW")
                                tts?.speak(output, TextToSpeech.QUEUE_FLUSH, null, "translation")
                            } else status = "語音朗讀尚未準備好。"
                        }, enabled = output.isNotBlank()) { Text("朗讀") }
                    }
                } else if (tab == 1) {
                    Text("印尼口語縮寫與語氣說明（僅供語境參考）")
                    OutlinedTextField(input, { input = it }, modifier = Modifier.fillMaxWidth(), label = { Text("貼上印尼聊天訊息") })
                    val matches = slang.filter { (word, _) -> Regex("(?i)(?<![\\p{L}])${Regex.escape(word)}(?![\\p{L}])").containsMatchIn(input) }
                    if (matches.isEmpty()) Text("輸入含常見縮寫的句子，即可看到說明。")
                    matches.forEach { (word, meaning) -> Text("$word：$meaning") }
                    Button(onClick = { chineseToIndonesian = false; tab = 0 }) { Text("前往印尼文翻譯") }
                } else {
                    Text("鞋廠 QA / QIP / CE / Lab 常用術語")
                    glossary.forEach { (zh, id) ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(zh, style = MaterialTheme.typography.titleSmall)
                                Text(id)
                                TextButton(onClick = {
                                    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("術語", id))
                                    status = "已複製：$id"
                                }) { Text("複製印尼文") }
                            }
                        }
                    }
                }
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
