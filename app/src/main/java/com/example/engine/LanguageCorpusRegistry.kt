package com.example.engine

/**
 * Phonetically-balanced corpora and language metadata for multilingual Piper models.
 * Strictly uses clean text codes and avoids emojis.
 */
data class LanguageInfo(
    val code: String,          // e.g. "ja-JP", "es-ES", "en-US"
    val shortCode: String,     // e.g. "JA", "ES", "EN"
    val displayName: String,   // e.g. "Japanese", "Spanish", "English"
    val nativeName: String,    // e.g. "日本語", "Español", "English"
    val corpusName: String,    // e.g. "ATR 503 Phonetically Balanced"
    val previewSentence: String,
    val samplePhrases: List<String>,
    val trainingSentences: List<String>
)

object LanguageCorpusRegistry {

    val JAPANESE = LanguageInfo(
        code = "ja-JP",
        shortCode = "JA",
        displayName = "Japanese",
        nativeName = "日本語",
        corpusName = "ATR 503 Phonetically Balanced",
        previewSentence = "桜の花が咲く頃、新しい風が吹く。",
        samplePhrases = listOf(
            "桜の花が咲く頃、新しい風が吹く。",
            "本を読むことは心を豊かにする。",
            "おはようございます。今日もよろしくお願いします。",
            "AIアシスタントです。何かお手伝いできることはありますか？",
            "This Japanese model is reading English text with native cadence."
        ),
        trainingSentences = listOf(
            // ATR Phonetically Balanced Japanese Sentences (Set A & B standard)
            "あらゆる現実をすべて自分の方へねじ曲げてゆくのだ。",
            "桜の花が咲く頃、新しい風が吹く。",
            "本を読むことは心を豊かにする。",
            "彼らは旅立ちの朝を迎えていた。",
            "青い海原の彼方に小さな島が見える。",
            "秋の夕暮れは足早に過ぎ去ってゆく。",
            "冷たい雨が静かに降り続いていた。",
            "古い時計が静かに夜の刻を刻んでいる。",
            "遠くの山々が白く雪化粧を施している。",
            "小川のせせらぎが心地よく響いている。",
            "木漏れ日が柔らかく道を照らしている。",
            "夜空に満天の星が美しく輝いている。",
            "春風が心地よく頬を撫でていった。",
            "温かいお茶を飲んでひと息つきましょう。",
            "新しい本を開いた時の香りが好きだ。",
            "雨上がりの空に色鮮やかな虹がかかった。",
            "朝露に濡れた若草が青々と光っている。",
            "鈴虫の鳴き声が秋の訪れを告げている。",
            "窓辺に置かれた観葉植物が元気に育つ。",
            "白い雲がゆっくりと青空を流れていく。"
        )
    )

    val SPANISH = LanguageInfo(
        code = "es-ES",
        shortCode = "ES",
        displayName = "Spanish",
        nativeName = "Español",
        corpusName = "Corpus Fonético Español",
        previewSentence = "El veloz murciélago hindú comía feliz cardillo y kiwi.",
        samplePhrases = listOf(
            "El veloz murciélago hindú comía feliz cardillo y kiwi.",
            "El viento susurra suavemente entre los viejos pinos del bosque.",
            "Hola, soy tu asistente de voz en tiempo real.",
            "La música clásica despierta la creatividad y calma el pensamiento.",
            "Esta voz en español puede leer oraciones con acento natural."
        ),
        trainingSentences = listOf(
            "El veloz murciélago hindú comía feliz cardillo y kiwi.",
            "El viento susurra suavemente entre los viejos pinos del bosque.",
            "Un pájaro carpintero picoteaba el tronco seco del castaño.",
            "Las campanas de la torre sonaron con fuerza al mediodía.",
            "El camino de piedra serpenteaba hacia la cumbre de la colina.",
            "La luz del atardecer doraba los campos de trigo maduro.",
            "El agua cristalina del manantial descendía entre las rocas.",
            "Un aroma a café recién molido llenaba cada rincón de la casa.",
            "Los niños jugaban alegres en la plaza del pueblo.",
            "El viejo roble ofrecía una sombra generosa en días calurosos.",
            "Las olas rompían con ritmo constante sobre la arena blanca.",
            "Un relámpago iluminó por un instante el horizonte oscuro.",
            "El aroma de los jazmines perfumaba la noche templada.",
            "Los barcos pesqueros regresaban al puerto con la marea alta.",
            "El fuego de la chimenea crepitaba en la fría noche invernal."
        )
    )

    val GERMAN = LanguageInfo(
        code = "de-DE",
        shortCode = "DE",
        displayName = "German",
        nativeName = "Deutsch",
        corpusName = "Phonetisch Balancierter Korpus",
        previewSentence = "Fischers Fritz fischt frische Fische auf dem See.",
        samplePhrases = listOf(
            "Fischers Fritz fischt frische Fische auf dem ruhigen See.",
            "Guten Tag, wie kann ich Ihnen heute behilflich sein?",
            "Der dichte Nebel stieg langsam über das weite Tal empor.",
            "Die Musik im Saal klang wunderbar durch den Abend."
        ),
        trainingSentences = listOf(
            "Fischers Fritz fischt frische Fische auf dem See.",
            "Der dichte Nebel stieg langsam über das weite Tal empor.",
            "Ein kühler Wind wehte durch die alten Eichen am Hang.",
            "Die alte Brücke aus Stein führt über den reißenden Bach.",
            "Frisches Brot duftet herrlich in der kleinen Backstube.",
            "Im Garten blühen die Apfelbäume in voller Pracht.",
            "Der Wanderer erreichte die Berghütte vor Einbruch der Nacht.",
            "Silberne Sonnenstrahlen brachen durch die Wolkendecke."
        )
    )

    val FRENCH = LanguageInfo(
        code = "fr-FR",
        shortCode = "FR",
        displayName = "French",
        nativeName = "Français",
        corpusName = "Corpus Phonétique Français",
        previewSentence = "Le jeune prince marchait dans la forêt enchantée.",
        samplePhrases = listOf(
            "Le jeune prince marchait dans la forêt enchantée au crépuscule.",
            "Bonjour, je suis votre modèle vocal personnalisé.",
            "Sous le ciel d'été, les oiseaux chantaient avec allégresse.",
            "Cette voix s'exprime avec une diction claire et naturelle."
        ),
        trainingSentences = listOf(
            "Le jeune prince marchait dans la forêt enchantée au crépuscule.",
            "Sous le ciel d'été, les oiseaux chantaient avec allégresse.",
            "La douce mélodie du violon résonnait dans la grande salle.",
            "Les vagues murmurent un chant paisible sur le rivage dore.",
            "Un rayon de soleil traversait les volets entrouverts ce matin.",
            "Les feuilles d'automne dansent au gré du vent léger.",
            "Le parfum du pain chaud embaume la petite boulangerie.",
            "La brume matinale se dissipait peu à peu sur le lac paisible."
        )
    )

    val KOREAN = LanguageInfo(
        code = "ko-KR",
        shortCode = "KO",
        displayName = "Korean",
        nativeName = "한국어",
        corpusName = "Korean Phonetically Balanced Corpus",
        previewSentence = "맑은 하늘 아래 봄바람이 부드럽게 분다.",
        samplePhrases = listOf(
            "맑은 하늘 아래 봄바람이 부드럽게 분다.",
            "안녕하세요, 무엇을 도와드릴까요?",
            "새로운 기술은 우리 삶을 더욱 편리하고 풍요롭게 만듭니다.",
            "인공지능 음성 모델이 한국어로 자연스럽게 발화합니다."
        ),
        trainingSentences = listOf(
            "맑은 하늘 아래 봄바람이 부드럽게 분다.",
            "깊은 산속 맑은 샘물이 바위 틈에서 솟아난다.",
            "아침 햇살이 창문을 통해 따뜻하게 비쳐온다.",
            "아이들의 웃음소리가 놀이터 가득 울려 퍼졌다.",
            "가을 단풍이 붉고 노랗게 온 산을 물들였다.",
            "따뜻한 차 한 잔이 지친 하루의 피로를 풀어준다.",
            "달빛이 고요한 밤바다 위에 은빛 길을 만든다."
        )
    )

    val ITALIAN = LanguageInfo(
        code = "it-IT",
        shortCode = "IT",
        displayName = "Italian",
        nativeName = "Italiano",
        corpusName = "Corpus Fonetico Italiano",
        previewSentence = "La luce dorata del mattino illumina le colline.",
        samplePhrases = listOf(
            "La luce dorata del mattino illumina le colline toscane.",
            "Buongiorno, sono il tuo assistente vocale in tempo reale.",
            "Il profumo del caffè appena fatto riempie la cucina.",
            "La musica classica accompagna piacevolmente il pomeriggio."
        ),
        trainingSentences = listOf(
            "La luce dorata del mattino illumina le colline toscane.",
            "Il profumo del caffè appena fatto riempie la cucina.",
            "Un vento fresco soffia leggero tra i rami degli olivi.",
            "Le campane del campanile rintoccano a festa nella valle.",
            "Il mare azzurro si estende calmo fino all'orizzonte.",
            "La sera porta con sé il profumo dolce dei gelsomini in fiore."
        )
    )

    val ENGLISH = LanguageInfo(
        code = "en-US",
        shortCode = "EN",
        displayName = "English",
        nativeName = "English",
        corpusName = "IEEE Harvard Sentences",
        previewSentence = "The oak tree was tall and green.",
        samplePhrases = listOf(
            "The oak tree was tall and green.",
            "The birch canoe slid on the smooth planks.",
            "A salt pickle tastes fine with ham.",
            "Systems operational. Ultra low-latency neural speech online.",
            "This cloned voice synthesizes speech with clear acoustic timbre."
        ),
        trainingSentences = HarvardSentences.SENTENCES
    )

    val supportedLanguages: List<LanguageInfo> = listOf(
        JAPANESE,
        SPANISH,
        ENGLISH,
        GERMAN,
        FRENCH,
        KOREAN,
        ITALIAN
    )

    fun getByCode(code: String): LanguageInfo {
        val normalized = code.trim().lowercase().replace("_", "-")
        return supportedLanguages.firstOrNull {
            it.code.lowercase() == normalized ||
            it.shortCode.lowercase() == normalized ||
            it.code.lowercase().startsWith(normalized) ||
            normalized.startsWith(it.shortCode.lowercase())
        } ?: ENGLISH
    }

    fun getCorpus(code: String): List<String> {
        return getByCode(code).trainingSentences
    }
}
