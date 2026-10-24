# IndoTalk 印尼通 — Android 原生 App 原始碼

## 功能
- 繁體中文介面，中文／印尼文雙向文字翻譯（Google ML Kit，首次下載模型）
- Android 系統語音辨識、TextToSpeech 朗讀
- 相簿／截圖選取與 Google ML Kit OCR（中文／拉丁文字）
- 複製、Android 分享選單（可選 WhatsApp）
- 印尼聊天縮寫與語氣詞字典
- 鞋廠 QA / CE / Lab 專業術語速查

## 建置 APK
1. 安裝 Android Studio（JDK 17）。
2. File > Open，選取解壓後的 `IndoTalk` 資料夾。
3. 等待 Gradle Sync，安裝提示的 Android SDK 35。
4. Build > Build Bundle(s) / APK(s) > Build APK(s)。
5. 完成後在 `app/build/outputs/apk/debug/app-debug.apk` 取得測試 APK。
6. 將 APK 傳至 Android 手機，允許該來源安裝後安裝。

## 注意
- 目前提供完整原始碼，但此環境未配置 Android SDK/Gradle，因此**未實際編譯或測試 APK**。
- Google ML Kit 中文翻譯目標語言是 Chinese，**不保證繁體輸出**。印尼文翻成中文時可能出現簡體字；正式版可增加可靠的繁簡轉換層。
- OCR 不會自動翻譯：選取圖片後會先填入辨識文字，檢查後按「開始翻譯」。
- 印尼口語功能目前是縮寫解釋字典，不是完整語意／情緒 AI 分析。
- 翻譯模型需首次下載；語音辨識及朗讀依裝置服務與語言包可用性而異。
- 內容未經專業譯者或鞋廠技術專家逐條驗證，品質處置等正式指令需人工確認。
- 若要上架 Play Store，需要正式簽署、圖示、隱私權政策、測試及發布設定。
