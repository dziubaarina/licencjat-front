package com.licencjat.licencjat_frontend

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.KeyboardEvent
import io.kvision.state.ObservableValue

object I18n {

    val languageState = ObservableValue(window.localStorage.getItem("appLang") ?: "pl")

    var lang: String
        get() = languageState.value
        internal set(value) {
            window.localStorage.setItem("appLang", value)
            languageState.value = value
        }

    private val pl = mapOf(
        "nav.brand"              to "DANCE APP",
        "nav.myPanel"            to "Mój Panel",
        "nav.logout"             to "Wyloguj",
        "nav.login"              to "Zaloguj się",
        "nav.register"           to "Zarejestruj się",
        "home.hero.title"        to "Twoja Szkoła Tańca",
        "home.hero.subtitle"     to "Platforma do zarządzania choreografiami i postępami tancerzy",
        "home.hero.cta"          to "Zacznij teraz",
        "home.features.title"    to "Dlaczego My?",
        "home.f1.title"          to "Zadania Wideo",
        "home.f1.desc"           to "Choreografowie przypisują zadania z instrukcjami wideo wprost w aplikacji.",
        "home.f2.title"          to "Szybka Ocena",
        "home.f2.desc"           to "Komentarze czasowe i ocena z feedbackiem – wszystko w jednym miejscu.",
        "home.f3.title"          to "Śledzenie Postępów",
        "home.f3.desc"           to "Statystyki i historia nagrań dla każdego tancerza.",
        "dancer.dashboard.title" to "Panel Tancerza",
        "dancer.tasks.card"      to "Dostępne zadania",
        "dancer.tasks.desc"      to "Wybierz wyzwanie i obejrzyj instrukcje.",
        "dancer.subs.card"       to "Moje nagrania",
        "dancer.subs.desc"       to "Przeglądaj wgrane filmy i statusy ocen.",
        "dancer.stats.card"      to "Statystyki",
        "dancer.stats.desc"      to "Śledź swój progres i wyniki.",
        "choreo.dashboard.title" to "Panel Choreografa",
        "choreo.queue.card"      to "Kolejka do oceny",
        "choreo.queue.desc"      to "Przeglądaj i oceniaj przesłane nagrania.",
        "choreo.tasks.card"      to "Zarządzaj zadaniami",
        "choreo.tasks.desc"      to "Twórz i edytuj zadania dla tancerzy.",
        "choreo.archive.card"    to "Archiwum",
        "choreo.archive.desc"    to "Historia ocenionych nagrań.",
        "admin.panel.title"      to "Panel Administratora",
        "admin.users.card"       to "Użytkownicy",
        "admin.users.desc"       to "Zarządzaj kontami użytkowników.",
        "admin.moderation.card"  to "Moderacja",
        "admin.moderation.desc"  to "Przeglądaj zgłoszenia i nagrania.",
        "admin.tasks.card"       to "Zadania",
        "admin.tasks.desc"       to "Podgląd wszystkich zadań w systemie.",
        "btn.back"               to "⬅ Wróć",
        "btn.save"               to "Zapisz",
        "btn.cancel"             to "Anuluj",
        "btn.delete"             to "Usuń",
        "btn.add"                to "Dodaj",
        "btn.edit"               to "Edytuj",
        "btn.view"               to "Zobacz",
        "btn.send"               to "Wyślij",
        "btn.grade"              to "Oceniaj",
        "btn.login"              to "Zaloguj",
        "btn.register"           to "Zarejestruj",
        "status.pending"         to "Oczekuje",
        "status.graded"          to "Ocenione",
        "status.loading"         to "Ładowanie...",
        "status.empty"           to "Brak danych",
        "a11y.lang"              to "EN",
        "a11y.fontSize"          to "Czcionka:",
        "a11y.smaller"           to "A-",
        "a11y.larger"            to "A+",
        "a11y.theme"             to "Motyw:",
        "a11y.dark"              to "🌙 Ciemny",
        "a11y.light"             to "☀️ Jasny",
        "a11y.highContrast"      to "⬛ Kontrast",
        "a11y.deuteranopia"      to "👁 Daltonizm",
        "a11y.skipLink"          to "Przejdź do treści",
    )

    private val en = mapOf(
        "nav.brand"              to "DANCE APP",
        "nav.myPanel"            to "My Panel",
        "nav.logout"             to "Log out",
        "nav.login"              to "Log in",
        "nav.register"           to "Sign up",
        "home.hero.title"        to "Your Dance School",
        "home.hero.subtitle"     to "Platform for managing choreographies and dancer progress",
        "home.hero.cta"          to "Get started",
        "home.features.title"    to "Why Us?",
        "home.f1.title"          to "Video Tasks",
        "home.f1.desc"           to "Choreographers assign tasks with video instructions right in the app.",
        "home.f2.title"          to "Fast Feedback",
        "home.f2.desc"           to "Timestamped comments and graded feedback – all in one place.",
        "home.f3.title"          to "Progress Tracking",
        "home.f3.desc"           to "Stats and submission history for every dancer.",
        "dancer.dashboard.title" to "Dancer Dashboard",
        "dancer.tasks.card"      to "Available tasks",
        "dancer.tasks.desc"      to "Pick a challenge and watch the instructions.",
        "dancer.subs.card"       to "My recordings",
        "dancer.subs.desc"       to "Browse uploaded videos and grade statuses.",
        "dancer.stats.card"      to "Statistics",
        "dancer.stats.desc"      to "Track your progress and scores.",
        "choreo.dashboard.title" to "Choreographer Dashboard",
        "choreo.queue.card"      to "Grading queue",
        "choreo.queue.desc"      to "Review and grade submitted recordings.",
        "choreo.tasks.card"      to "Manage tasks",
        "choreo.tasks.desc"      to "Create and edit tasks for dancers.",
        "choreo.archive.card"    to "Archive",
        "choreo.archive.desc"    to "History of graded recordings.",
        "admin.panel.title"      to "Admin Panel",
        "admin.users.card"       to "Users",
        "admin.users.desc"       to "Manage user accounts.",
        "admin.moderation.card"  to "Moderation",
        "admin.moderation.desc"  to "Review reports and recordings.",
        "admin.tasks.card"       to "Tasks",
        "admin.tasks.desc"       to "View all tasks in the system.",
        "btn.back"               to "⬅ Back",
        "btn.save"               to "Save",
        "btn.cancel"             to "Cancel",
        "btn.delete"             to "Delete",
        "btn.add"                to "Add",
        "btn.edit"               to "Edit",
        "btn.view"               to "View",
        "btn.send"               to "Send",
        "btn.grade"              to "Grade",
        "btn.login"              to "Log in",
        "btn.register"           to "Register",
        "status.pending"         to "Pending",
        "status.graded"          to "Graded",
        "status.loading"         to "Loading...",
        "status.empty"           to "No data",
        "a11y.lang"              to "PL",
        "a11y.fontSize"          to "Font:",
        "a11y.smaller"           to "A-",
        "a11y.larger"            to "A+",
        "a11y.theme"             to "Theme:",
        "a11y.dark"              to "🌙 Dark",
        "a11y.light"             to "☀️ Light",
        "a11y.highContrast"      to "⬛ Contrast",
        "a11y.deuteranopia"      to "👁 Colour-blind",
        "a11y.skipLink"          to "Skip to content",
    )

    fun t(key: String): String {
        val map = if (lang == "pl") pl else en
        return map[key] ?: pl[key] ?: key
    }

    // Super funkcja dynamiczna do podmiany wszystkiego:
    fun tr(plText: String, enText: String): String {
        return if (lang == "pl") plText else enText
    }
}


object FontSizeManager {

    private const val STEP   = 10
    private const val MIN_PC = 80
    private const val MAX_PC = 200
    private const val KEY    = "fontSize"

    var currentPercent: Int = window.localStorage.getItem(KEY)?.toIntOrNull() ?: 100
        private set

    fun init() = apply()

    fun increase() {
        if (currentPercent < MAX_PC) { currentPercent += STEP; save(); apply() }
    }

    fun decrease() {
        if (currentPercent > MIN_PC) { currentPercent -= STEP; save(); apply() }
    }

    fun reset() { currentPercent = 100; save(); apply() }

    fun setExact(pct: Int) {
        currentPercent = pct.coerceIn(MIN_PC, MAX_PC)
        save(); apply()
    }

    private fun save()  = window.localStorage.setItem(KEY, currentPercent.toString())
    private fun apply() {
        (document.documentElement as? HTMLElement)?.style?.fontSize = "$currentPercent%"
    }
}


enum class AppTheme(val cssClass: String, val label: String) {
    DARK         ("theme-dark",          "dark"),
    LIGHT        ("theme-light",         "light"),
    HIGH_CONTRAST("theme-high-contrast", "high-contrast"),
    DEUTERANOPIA ("theme-deuteranopia",  "deuteranopia"),
}

object ThemeManager {

    private const val KEY = "appTheme"

    var current: AppTheme = AppTheme.valueOf(
        window.localStorage.getItem(KEY) ?: AppTheme.DARK.name
    )
        private set

    fun init() = apply()

    fun setTheme(theme: AppTheme) {
        current = theme
        window.localStorage.setItem(KEY, theme.name)
        apply()
    }

    private fun apply() {
        val body = document.body as? HTMLElement ?: return
        AppTheme.values().forEach { body.classList.remove(it.cssClass) }
        body.classList.add(current.cssClass)
    }
}


object KeyboardManager {

    private val escStack = ArrayDeque<() -> Unit>()

    fun init() {
        document.addEventListener("keydown", { event ->
            val e = event as? KeyboardEvent ?: return@addEventListener
            if (e.key == "Escape" && escStack.isNotEmpty()) {
                e.preventDefault()
                escStack.last().invoke()
            }
        })

        document.addEventListener("keydown", { event ->
            val e = event as? KeyboardEvent ?: return@addEventListener
            if (e.key != "Enter" && e.key != " ") return@addEventListener
            val target = e.target as? HTMLElement ?: return@addEventListener
            val role    = target.getAttribute("role")
            val tag     = target.tagName.lowercase()
            val kbdAttr = target.getAttribute("data-kbd")
            if ((role == "button" || kbdAttr == "true") && tag != "button" && tag != "a") {
                e.preventDefault()
                target.click()
            }
        })
    }

    fun push(onEsc: () -> Unit) = escStack.addLast(onEsc)
    fun pop()  { if (escStack.isNotEmpty()) escStack.removeLast() }
    fun clear() = escStack.clear()
}


fun HTMLElement.makeKeyboardFocusable(label: String? = null) {
    setAttribute("role", "button")
    setAttribute("tabindex", "0")
    setAttribute("data-kbd", "true")
    if (label != null) setAttribute("aria-label", label)
}