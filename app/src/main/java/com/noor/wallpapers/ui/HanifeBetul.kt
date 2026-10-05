package com.noor.wallpapers.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noor.wallpapers.art.Ebced
import com.noor.wallpapers.prayer.HijriCalendar
import com.noor.wallpapers.prayer.HolyDay
import com.noor.wallpapers.prayer.HolyDayEvent
import com.noor.wallpapers.prayer.Prayer
import com.noor.wallpapers.prayer.ReligiousDays
import com.noor.wallpapers.prayer.TurkishText
import com.noor.wallpapers.service.AppSettings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.MonthDay

/**
 * Everything personal in the app. HBSnoor was made for Hanife Betül, and these
 * easter eggs are meant to be found; the dedication page keeps count. The art
 * has its own: an "HB" constellation in every night sky, a tiny H·B star at the
 * foot of every wallpaper, a ح ب seal on every levha, her own collection and
 * palette, a shooting star in the live wallpaper, and one design that stays
 * hidden until she counts her name on the tesbih.
 */
object HanifeBetul {
    const val NAME = "Hanife Betül"

    /** Picks from [options] by day, so a message changes daily but not on every redraw. */
    private fun <T> daily(options: List<T>, date: LocalDate = LocalDate.now(), salt: Int = 0): T =
        options[Math.floorMod(date.toEpochDay() * 31 + salt, options.size.toLong()).toInt()]

    /** Gallery and prayer-screen greeting: birthday, holy days, Friday, then the time of day. */
    fun greeting(now: LocalDateTime, birthday: MonthDay?, hijri: HijriCalendar): String {
        val today = now.toLocalDate()
        if (birthday == MonthDay.from(today)) return "İyi ki doğdun, $NAME 🎂"
        holyDayGreeting(today, hijri)?.let { return it }
        if (now.dayOfWeek == DayOfWeek.FRIDAY) return "Hayırlı Cumalar, $NAME 🕌"
        val options = when (now.hour) {
            in 4..10 -> listOf("Hayırlı sabahlar, $NAME ☀️", "Günaydın, $NAME 🌤️", "Güzel bir sabah dilerim, $NAME 🌸")
            in 11..16 -> listOf("İyi günler, $NAME 🌿", "Günün aydın olsun, $NAME ✨", "Kolay gelsin, $NAME 🌿")
            in 17..21 -> listOf("Hayırlı akşamlar, $NAME 🌙", "Akşamın huzurlu olsun, $NAME 🌙")
            else -> listOf("Hayırlı geceler, $NAME ✨", "Yıldızlar senin için yanıyor, $NAME ✨")
        }
        return daily(options, today, now.hour / 6)
    }

    private fun holyDayGreeting(today: LocalDate, hijri: HijriCalendar): String? = try {
        val h = hijri.of(today)
        val tonight = ReligiousDays.on(today, hijri).firstOrNull()
        when {
            tonight != null && tonight.day.night -> "${tonight.day.title} mübarek olsun, $NAME 🌙"
            tonight?.day == HolyDay.RAMAZAN_BAYRAMI || tonight?.day == HolyDay.KURBAN_BAYRAMI -> "Bayramın mübarek olsun, $NAME 🌙"
            h.month == 9 -> "Hayırlı Ramazanlar, $NAME 🏮"
            h.month in 7..8 -> "Üç aylar hayırlı olsun, $NAME 🌙"
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    /** Shown after a wallpaper is set; one picked at random each time. */
    val appliedMessages = listOf(
        "Hayırlı olsun, $NAME ✨",
        "Ekranın artık nur gibi 🌙",
        "Maşallah, çok yakıştı!",
        "Gözün aydın, yeni duvar kağıdın hazır 💚",
        "Bu ekran sana yakışır, $NAME 🌸",
        "Telefonun bayram yerine döndü 🏮",
        "Her açışta bir tebessüm olsun ✨",
    )

    /** Every seventh shuffle earns one of these. */
    val shuffleMessages = listOf(
        "Maşallah! Bu tam sana göre, $NAME ✨",
        "Yedinci karıştırma: şans yıldızın parlıyor 🌟",
        "Bunu beğendiysen kalbe dokun, kaybolmasın 💚",
    )

    const val EMPTY_FAVOURITES =
        "Henüz favorin yok, $NAME. Beğendiğin bir duvar kağıdındaki kalbe dokun, burada seni beklesin. 💚"

    /** A line under each prayer-time notification. */
    fun prayerNote(p: Prayer, date: LocalDate = LocalDate.now()): String = daily(
        when (p) {
            Prayer.OGLE -> listOf("Günün ortasında kısa bir mola: Allah kabul etsin.", "Öğlenin huzuru üzerine olsun, $NAME.")
            Prayer.IKINDI -> listOf("Günün yorgunluğunu ikindiyle bırak, $NAME.", "İkindinin serinliği gönlüne değsin.")
            Prayer.AKSAM -> listOf("Güneş battı; akşamın hayırlı olsun, $NAME.", "Akşamın nuru evine dolsun.")
            Prayer.YATSI -> listOf("Günü yatsıyla kapat; hayırlı geceler, $NAME.", "Gecen huzurlu, uykun tatlı olsun.")
            else -> listOf("Allah kabul etsin, $NAME.")
        },
        date, p.ordinal,
    )

    fun holyDayMessage(e: HolyDayEvent): Pair<String, String> = when (e.day) {
        HolyDay.KADIR -> "Bu gece Kadir Gecesi ✨" to "Bin aydan hayırlı gece. Duaların kabul olsun, $NAME."
        HolyDay.REGAIB, HolyDay.MIRAC, HolyDay.BERAT, HolyDay.MEVLID ->
            "Bu gece ${e.day.title} 🌙" to "Kandilin mübarek olsun, $NAME. Dualarında beni de unutma."
        HolyDay.RAMAZAN -> "Hayırlı Ramazanlar 🏮" to "On bir ayın sultanı geldi. Oruçların kabul olsun, $NAME."
        HolyDay.RAMAZAN_AREFE, HolyDay.KURBAN_AREFE -> "Arefe günü 🌙" to "Yarın bayram. Arefen hayırlı olsun, $NAME."
        HolyDay.RAMAZAN_BAYRAMI -> "Ramazan Bayramın mübarek olsun 🌙" to "Nice bayramlara, $NAME. Bayramın kutlu, gönlün şen olsun."
        HolyDay.KURBAN_BAYRAMI -> "Kurban Bayramın mübarek olsun ✨" to "Kurbanların kabul, bayramın kutlu olsun, $NAME."
        HolyDay.HICRI_YILBASI -> "Hicri yılbaşı 🌙" to "Yeni hicri yılın hayırlara vesile olsun, $NAME."
        HolyDay.ASURE -> "Aşure Günü" to "Bereketli bir Aşure günü dilerim, $NAME."
        HolyDay.UC_AYLAR -> "Üç aylar başladı 🌙" to "Recep, Şaban, Ramazan... Hayırlara vesile olsun, $NAME."
    }

    fun birthdayMessage(): Pair<String, String> =
        "İyi ki doğdun, $NAME 🎂" to "Nice nur dolu, huzurlu yıllara. Bugün duvar kağıdın da sana özel; canlı duvar kağıdında gül yaprakları yağıyor. 🌸"

    /** Qibla compass, when she faces the Kaaba. */
    fun qiblaMessage(distanceKm: Double): String =
        "Kıbleye döndün 🕋 Kâbe ${"%,d".format(TurkishText.TR, distanceKm.toLong())} km uzakta; kalbin ise çok yakın."

    /** Tesbih milestones: the counts that mean something. Returns a message for [count], if any. */
    fun tesbihMilestone(count: Int): String? = when (count) {
        33 -> "33 ✓ Bir tesbih tamam."
        99 -> "99 ✨ Esmâ-i Hüsnâ'nın sayısı."
        100 -> "100 ✓ Allah kabul etsin, $NAME."
        Ebced.value(Ebced.HANIFE) -> "153 ✨ Hanife adının ebced değeri."
        Ebced.value(Ebced.BETUL) -> "438 ✨ Betül adının ebced değeri."
        SECRET_COUNT -> "591 🌷 Hanife Betül! Adının sırrını buldun. Galeride gizli bir tasarım açıldı."
        1000 -> "1000 ✨ Maşallah, $NAME!"
        else -> null
    }

    /** حنيفة بتول in ebced; counting to it on the tesbih reveals the hidden design. */
    val SECRET_COUNT = Ebced.value(Ebced.HANIFE) + Ebced.value(Ebced.BETUL)

    // Günün notu --------------------------------------------------------------------------

    class Note(val text: String, val source: String?)

    /**
     * One note a day on the prayer screen: Qur'anic verses (meanings after the
     * Diyanet translation) alternating with notes written for her.
     */
    private val VERSES = listOf(
        Note("Rabbim! Göğsümü aç, işimi bana kolaylaştır.", "Tâhâ 20:25-26"),
        Note("Bilesiniz ki kalpler ancak Allah'ı anmakla huzur bulur.", "Ra'd 13:28"),
        Note("Şüphesiz zorlukla beraber bir kolaylık vardır.", "İnşirah 94:6"),
        Note("Allah hiç kimseye gücünün yetmediği bir şey yüklemez.", "Bakara 2:286"),
        Note("Kim Allah'a tevekkül ederse, O ona yeter.", "Talâk 65:3"),
        Note("Kullarım beni sana sorduklarında, bilsinler ki ben onlara çok yakınım.", "Bakara 2:186"),
        Note("Allah'ın rahmetinden ümidinizi kesmeyin.", "Zümer 39:53"),
        Note("Rabbin sana verecek, sen de hoşnut olacaksın.", "Duhâ 93:5"),
        Note("Biz insana şah damarından daha yakınız.", "Kâf 50:16"),
        Note("Bana dua edin, size icabet edeyim.", "Mü'min 40:60"),
        Note("Şüphesiz Allah sabredenlerle beraberdir.", "Bakara 2:153"),
        Note("Başarım ancak Allah'ın yardımıyladır.", "Hûd 11:88"),
        Note("Allah göklerin ve yerin nurudur.", "Nûr 24:35"),
        Note("Rabbimiz! Bize dünyada da iyilik ver, ahirette de iyilik ver ve bizi ateş azabından koru.", "Bakara 2:201"),
        Note("Beni anın ki ben de sizi anayım.", "Bakara 2:152"),
        Note("Nerede olursanız olun, O sizinle beraberdir.", "Hadîd 57:4"),
        Note("Rabbiniz rahmet etmeyi kendi üzerine yazdı.", "En'âm 6:54"),
        Note("Şüphesiz Allah iyilik yapanları sever.", "Bakara 2:195"),
        Note("Gevşemeyin, üzülmeyin; eğer inanıyorsanız en üstün olan sizsiniz.", "Âl-i İmrân 3:139"),
        Note("Şükrederseniz elbette size nimetimi artırırım.", "İbrâhîm 14:7"),
        Note("Allah'ın nimetlerini saymaya kalksanız sayamazsınız.", "İbrâhîm 14:34"),
        Note("Rabbim! İlmimi artır.", "Tâhâ 20:114"),
    )

    private val PERSONAL = listOf(
        Note("Bugün de gülümse, $NAME. Gülüşün bu ekranın en güzel nuru.", null),
        Note("Bir bardak çay, bir sayfa Kur'an, bir dua: güzel bir gün için yeter.", null),
        Note("Adın gibi: yalnız O'na yönelen bir kalp. Hayırlı bir gün olsun.", null),
        Note("Unutma: dua ettiğinde seni dinleyen biri var.", null),
        Note("Bugün birine iyilik yap; en çok sana yakışan şey bu.", null),
        Note("Bu uygulamadaki her yıldız senin için yanıyor. ✨", null),
        Note("Yorulduysan dinlen; Rabbin seni senden iyi bilir.", null),
        Note("Her vakit bir kapı: günde beş kez içeri davet.", null),
        Note("Ebru gibi: her gün başka renkler, aynı güzellik.", null),
        Note("Sabah namazının huzuru bütün güne yetsin.", null),
        Note("İçinden geçen güzel şeyler bir gün dua olarak geri döner.", null),
        Note("Pendik sahilinde akşam: güneş Adalar'ın arkasına inerken bir dua da senden.", null),
        Note("Martılar vapurun peşinde, dualar senin peşinde. Hayırlı yolculuklar.", null),
        Note("Kıble buradan 152 derece; kalbinse her yerden aynı yöne.", null),
    )

    fun noteOfTheDay(date: LocalDate = LocalDate.now()): Note {
        val day = date.toEpochDay()
        return if (day % 3 == 2L) daily(PERSONAL, date, 7) else daily(VERSES, date, 3)
    }

    // Sürprizler ----------------------------------------------------------------------------

    /** The easter eggs she can find. The dedication page counts them and hints at the rest. */
    enum class Surprise(val title: String, val hint: String) {
        WELCOME("Hoş geldin notu", "Uygulamayı ilk kez aç."),
        DEDICATION("Ad sayfası", "✦ simgesine dokun."),
        TITLE_TAPS("Beş dokunuş", "Galerideki HBSnoor yazısına art arda dokun."),
        SHUFFLE("Yedinci karıştırma", "Bir tasarımı defalarca karıştır."),
        APPLIED("Yeni duvar kağıdı", "Bir tasarımı duvar kağıdı yap."),
        EMPTY_FAVORITES("Boş favoriler", "Hiç favorin yokken favorilere bak."),
        LIVE("Kayan yıldız", "Canlı duvar kağıdını kur; her 37 saniyede bir dilek fırsatı."),
        TAP_STAR("Parmağından yıldız", "Canlı duvar kağıdında boş bir yere dokun."),
        QIBLA("Kıbleye dönüş", "Kıble pusulasında telefonu kıbleye çevir."),
        SECRET("Gizli tasarım", "Zikirmatikte adının ebced değerine kadar say."),
        BIRTHDAY("Doğum günü", "Bu sayfada doğum gününü kaydet."),
        FRIDAY("Cuma selamı", "Uygulamayı bir cuma günü aç."),
        NOTE("Günün notu", "Vakitler ekranındaki günün notuna dokun."),
        PALETTE("Kendi paletin", "Bir tasarımda kendi renk paletini yap."),
        OWN_WORDS("Kendi sözün", "Kendi Sözün tasarımına bir şey yaz."),
        THEME("Betül teması", "✦ simgesine uzun bas."),
        TESBIHAT("Tesbihat", "Zikirmatikte tesbihatı sonuna kadar çek."),
    }

    /**
     * Marks [s] as found. Returns a short "found one" message the first time, null after,
     * so callers can show it once.
     */
    fun find(context: Context, s: Surprise): String? {
        val settings = AppSettings(context)
        val found = settings.foundSurprises
        if (s.name in found) return null
        val now = found + s.name
        settings.foundSurprises = now
        val total = Surprise.entries.size
        return if (now.size == total) "Bütün sürprizleri buldun, $NAME! 🌟 ($total/$total)"
        else "Yeni bir sürpriz buldun: ${s.title} ✨ (${now.size}/$total)"
    }
}

@Composable
fun WelcomeDialog(onOpenCollection: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Hoş geldin, ${HanifeBetul.NAME} 🌙",
                color = MaterialTheme.colorScheme.primary,
            )
        },
        text = {
            Text(
                "HBSnoor senin için yapıldı. Artık yalnızca duvar kağıdı değil: Diyanet'in namaz vakitleri, " +
                    "vakit bildirimleri, kıble pusulası, zikirmatik ve dini günler de burada. İçinde sana özel bir " +
                    "koleksiyon, adını taşıyan bir renk paleti ve her köşeye saklanmış sürprizler var. " +
                    "✦ sayfası kaç tanesini bulduğunu sayıyor. ✨",
            )
        },
        confirmButton = { Button(onClick = onOpenCollection) { Text("Koleksiyonumu göster") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Sonra") } },
    )
}

/** Opened from the ✦ button or by tapping the "HBSnoor" title five times. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DedicationSheet(onOpenCollection: () -> Unit, onDismiss: () -> Unit, onThemeChanged: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    var birthday by remember { mutableStateOf(settings.birthday) }
    var found by remember { mutableStateOf(settings.foundSurprises) }
    var pickingBirthday by remember { mutableStateOf(false) }
    var theme by remember { mutableStateOf(settings.themePalette) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("حَنِيفَة بَتُول", fontSize = 40.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "Bu uygulama ${HanifeBetul.NAME} için yapıldı.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            NameMeaning(
                "Hanife · حنيفة",
                "Hanîf: Şirkten uzaklaşıp yalnızca Allah'a yönelen, tevhid ehli. Kur'an'da Hz. İbrahim'in imanı " +
                    "böyle nitelenir (ör. Bakara 2:135).",
                "فَأَقِمْ وَجْهَكَ لِلدِّينِ حَنِيفًا",
                "Yüzünü hanîf olarak dine çevir. (Rûm 30:30)",
            )
            NameMeaning(
                "Betül · بتول",
                "İffetli, Allah'a gönülden yönelmiş kadın. Hz. Meryem ve Hz. Fâtıma'nın lakaplarından biridir.",
                "وَتَبَتَّلْ إِلَيْهِ تَبْتِيلًا",
                "Bütün benliğinle O'na yönel. (Müzzemmil 73:8)",
                note = "Betül ile aynı ب-ت-ل kökünden",
            )
            EbcedCard()

            // Birthday
            Card {
                Text("Doğum günün", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(
                    birthday?.let { "${it.dayOfMonth} ${TurkishText.MONTHS[it.monthValue - 1]} · o gün sana özel bir duvar kağıdı ve sürprizler var 🎂" }
                        ?: "Doğum gününü kaydedersen o gün uygulama seni ayrıca kutlar. 🎂",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { pickingBirthday = true }) { Text(if (birthday == null) "Doğum günümü kaydet" else "Değiştir") }
            }

            // Surprises found
            Card {
                val total = HanifeBetul.Surprise.entries.size
                Text("Bulduğun sürprizler: ${found.size}/$total", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { found.size / total.toFloat() }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                for (s in HanifeBetul.Surprise.entries) {
                    val ok = s.name in found
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                        Text(if (ok) "✦" else "·", color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, modifier = Modifier.width(20.dp))
                        Column {
                            Text(
                                if (ok) s.title else "Gizli sürpriz",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!ok) {
                                Text("İpucu: ${s.hint}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f))
                            }
                        }
                    }
                }
            }

            Text(
                "Sanatın içinde de saklılar: gece göklerinde bir HB takımyıldızı, her duvar kağıdının altında minik " +
                    "bir H·B yıldızı, levhalarda ح ب mührü ve canlı duvar kağıdında her 37 saniyede bir kayan yıldız. " +
                    "Bir de dilek tut. 🌠",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onOpenCollection) { Text("${HanifeBetul.NAME} koleksiyonu ♡") }
            TextButton(onClick = {
                theme = if (theme == "betul") null else "betul"
                settings.themePalette = theme
                HanifeBetul.find(context, HanifeBetul.Surprise.THEME)
                found = settings.foundSurprises
                onThemeChanged()
            }) { Text(if (theme == "betul") "Zümrüt temaya dön" else "Uygulamayı Betül renklerine boya 🌸") }
        }
    }

    if (pickingBirthday) {
        BirthdayDialog(
            initial = birthday,
            onDone = { md ->
                settings.birthday = md
                birthday = md
                if (md != null) HanifeBetul.find(context, HanifeBetul.Surprise.BIRTHDAY)
                found = settings.foundSurprises
                pickingBirthday = false
            },
            onDismiss = { pickingBirthday = false },
        )
    }
}

/** Day and month only: the year is nobody's business. */
@Composable
private fun BirthdayDialog(initial: MonthDay?, onDone: (MonthDay?) -> Unit, onDismiss: () -> Unit) {
    var month by remember { mutableStateOf(initial?.monthValue ?: 1) }
    var day by remember { mutableStateOf(initial?.dayOfMonth ?: 1) }
    val maxDay = java.time.Month.of(month).maxLength()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Doğum günün 🎂") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ay", style = MaterialTheme.typography.labelLarge)
                ChipGrid((1..12).map { TurkishText.MONTHS[it - 1] }, month - 1) { month = it + 1; if (day > java.time.Month.of(month).maxLength()) day = 1 }
                Text("Gün", style = MaterialTheme.typography.labelLarge)
                ChipGrid((1..maxDay).map { it.toString() }, day - 1) { day = it + 1 }
            }
        },
        confirmButton = { Button(onClick = { onDone(MonthDay.of(month, day)) }) { Text("Kaydet") } },
        dismissButton = {
            if (initial != null) TextButton(onClick = { onDone(null) }) { Text("Sil") }
            else TextButton(onClick = onDismiss) { Text("Vazgeç") }
        },
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChipGrid(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { i, label ->
            FilterChip(selected = i == selected, onClick = { onSelect(i) }, label = { Text(label) })
        }
    }
}

/** Ebced (abjad): the numbers hidden in her names. */
@Composable
private fun EbcedCard() {
    var open by remember { mutableStateOf(false) }
    Card(Modifier.clickable { open = !open }) {
        Text("Adının sayısı · ebced", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            "حنيفة ${Ebced.value(Ebced.HANIFE)}  +  بتول ${Ebced.value(Ebced.BETUL)}  =  ${HanifeBetul.SECRET_COUNT}",
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
        )
        if (open) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Osmanlı şairleri harflere sayı değeri verip tarihleri mısralara saklardı (tarih düşürme). " +
                    "Senin adının değeri ${HanifeBetul.SECRET_COUNT}. Bu sayının bir sırrı var... 🌷",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text("Dokun", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun NameMeaning(title: String, meaning: String, verse: String, verseMeaning: String, note: String? = null) {
    Card {
        Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(meaning, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        Text(verse, fontSize = 24.sp, textAlign = TextAlign.Center)
        Text(
            verseMeaning,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (note != null) {
            Text(
                note,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
