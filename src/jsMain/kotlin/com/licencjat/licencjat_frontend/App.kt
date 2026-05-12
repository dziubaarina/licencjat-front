package com.licencjat.licencjat_frontend

import io.kvision.Application
import io.kvision.CoreModule
import io.kvision.BootstrapModule
import io.kvision.BootstrapCssModule
import io.kvision.FontAwesomeModule
import io.kvision.TomSelectModule
import io.kvision.core.*
import io.kvision.html.*
import io.kvision.panel.*
import io.kvision.startApplication
import io.kvision.state.ObservableValue
import io.kvision.state.bind
import io.kvision.utils.vh
import io.kvision.utils.vw
import io.kvision.modal.Modal
import io.kvision.form.text.text
import io.kvision.form.text.password
import io.kvision.form.text.textArea
import io.kvision.form.text.textInput
import io.kvision.form.check.checkBox
import io.kvision.form.select.select
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.files.Blob
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.url.URL

// --- MODELE ---
data class ChoreoTask(
    val id: Int,
    val title: String,
    val description: String,
    val instructionVideoUrl: String? = null,
    val assignedDancers: List<String>
)

object DataManager {
    val globalTasks = io.kvision.state.ObservableListWrapper<ChoreoTask>()
    val globalAnnouncements = io.kvision.state.ObservableListWrapper<dynamic>()
    val allUsers = io.kvision.state.ObservableListWrapper<dynamic>()
    val allDancers = listOf(
        "1" to "tancerz@danceapp.pl",
        "2" to "Anna Kowalska",
        "3" to "Jan Nowak"
    )
}

object PlayerState {
    var submissionId: Int = 0
    var submissionVideoUrl: String = ""
    var instructionVideoUrl: String = ""
    var taskTitle: String = ""
    var taskId: Int = 0
    var isGraded: Boolean = false
    var currentScore: Int? = null
    var currentFeedback: String? = null
}

object AdminState {
    var selectedUserId: Int = 0
    var selectedUserName: String = ""
}

enum class Page {
    HOME, DANCER_DASHBOARD, CHOREO_DASHBOARD, ADMIN_PANEL, PLAYER,
    DANCER_TASKS, DANCER_SUBMISSIONS, DANCER_STATS,
    CHOREO_QUEUE, CHOREO_TASKS, CHOREO_ARCHIVE,
    ADMIN_VERIFY, ADMIN_USERS, ADMIN_MODERATION, ADMIN_TASKS, ADMIN_USER_DETAILS,
    CHAT, ADMIN_ANNOUNCEMENTS, BLOCKED
}

class App : Application() {

    private val appState = ObservableValue(Page.HOME)
    private val userRole = ObservableValue<String?>(null)
    private val currentUserId = ObservableValue<Int?>(null)

    private var loginModal: Modal? = null
    private var registerModal: Modal? = null

    override fun start() {
        AccessibilityBar.init()
        val savedToken = window.localStorage.getItem("jwt")
        val savedRole = window.localStorage.getItem("userRole")
        val savedId = window.localStorage.getItem("userId")?.toIntOrNull()
        if (savedToken != null && savedRole != null) {
            userRole.value = savedRole
            currentUserId.value = savedId
            // POPRAWKA: Po odświeżeniu strony zablokowany użytkownik musi wrócić na Page.BLOCKED,
            // a nie na HOME. Bez tego użytkownik mógł obejść blokadę przez F5.
            val isActive = window.localStorage.getItem("isActive") != "false"
            if (!isActive) {
                appState.value = Page.BLOCKED
            }
        }

        // --- NAPRAWA STYLÓW DLA MODALI, FORMULARZY I CZATU ---
        val style = document.createElement("style")
        style.innerHTML = """
            .modal-content {
                background-color: var(--color-bg-card, #1a1a1a) !important;
                color: var(--color-text, #f5f5f5) !important;
                border: 1px solid var(--color-border, #333) !important;
            }
            .modal-header {
                border-bottom: 1px solid var(--color-border, #333) !important;
            }
            .form-control, .form-select {
                background-color: var(--color-bg, #121212) !important;
                color: var(--color-text, #f5f5f5) !important;
                border: 1px solid var(--color-border, #333) !important;
            }
            .form-control::placeholder {
                color: var(--color-text-muted, #aaaaaa) !important;
                opacity: 1 !important;
            }
            .form-control:focus, .form-select:focus {
                border-color: var(--color-primary, #a893ff) !important;
                box-shadow: 0 0 0 0.25rem rgba(168, 147, 255, 0.25) !important;
            }
            .btn-close {
                filter: invert(1) grayscale(100%) brightness(200%);
            }
            body.theme-light .btn-close {
                filter: none !important;
            }
            
            /* WYMUSZENIE CZARNEGO TEKSTU NA FIOLETOWYM ZAZNACZENIU W CZACIE */
            .bg-primary-dance.hover-card span,
            .bg-primary-dance.hover-card .fw-bold {
                color: #000000 !important;
            }
        """.trimIndent()
        document.head?.appendChild(style)

        root("kvapp") {
            bind(I18n.languageState) { _ ->
                loginModal?.dispose()
                registerModal?.dispose()
                loginModal = createLoginModal()
                registerModal = createRegisterModal()

                // ZMIANA: Dodano d-flex flex-column do głównego kontenera, aby stopka lądowała na dole
                vPanel(spacing = 0, className = "main-container bg-dark text-white min-vh-100 d-flex flex-column") {
                    width = 100.vw
                    buildNavbar()

                    // ZMIANA: flex-grow-1 rozciąga główny obszar strony, spychając stopkę w dół
                    div(className = "page-content flex-grow-1") {
                        setAttribute("id", "main-content")
                        val contentContainer = this
                        bind(appState) { page ->
                            contentContainer.apply {
                                when (page) {
                                    Page.HOME -> buildHomeView()
                                    Page.DANCER_DASHBOARD -> buildDancerDashboard(appState)
                                    Page.DANCER_TASKS -> buildDancerTasks(appState)
                                    Page.DANCER_SUBMISSIONS -> buildDancerSubmissions(appState)
                                    Page.DANCER_STATS -> buildDancerStats(appState)
                                    Page.CHOREO_DASHBOARD -> buildChoreoDashboard()
                                    Page.ADMIN_PANEL -> buildAdminPanel()
                                    Page.CHOREO_QUEUE -> buildChoreoQueue()
                                    Page.CHOREO_TASKS -> buildChoreoTasks()
                                    Page.CHOREO_ARCHIVE -> buildChoreoArchive()
                                    Page.ADMIN_VERIFY -> buildAdminVerify()
                                    Page.ADMIN_USERS -> buildAdminUsers()
                                    Page.ADMIN_MODERATION -> buildAdminModeration()
                                    Page.ADMIN_TASKS -> buildAdminTasks()
                                    Page.ADMIN_USER_DETAILS -> buildAdminUserDetails()
                                    Page.PLAYER -> buildPlayerView()
                                    Page.CHAT -> buildChatView()
                                    Page.ADMIN_ANNOUNCEMENTS -> buildAdminAnnouncements()
                                    Page.BLOCKED -> buildBlockedView()
                                }
                            }
                        }
                    }

                    // DODANA STOPKA
                    buildFooter()
                }
            }
        }
    }

    private fun Container.featureCard(icon: String, title: String, desc: String) {
        div(className = "col-md-4") {
            div(className = "card h-100 border-0 shadow-sm p-4 hover-card bg-light") {
                div(className = "mb-4") { tag(TAG.I, className = "fa-solid $icon fa-3x text-primary-dance") }
                h4(title, className = "fw-bold text-dark mb-3")
                p(desc, className = "text-muted")
            }
        }
    }

    // ==========================================
    // NAVBAR
    // ==========================================
    private fun Container.buildNavbar() {
        nav(className = "navbar navbar-expand-lg navbar-dark bg-dark fixed-top py-2 border-bottom border-secondary") {
            div(className = "container-fluid px-4") {
                link("", "javascript:void(0)", className = "navbar-brand d-flex align-items-center pe-auto") {
                    tag(TAG.SPAN) {
                        rich = true
                        content = """
            <svg height="45" viewBox="0 0 380 120" xmlns="http://www.w3.org/2000/svg" style="overflow: visible;">
                <g transform="translate(-10, -20) scale(0.38)">
                    <path d="M 958 73 L 943 78 L 934 87 L 930 97 L 930 107 L 934 116 L 948 124 L 962 118 
                             L 965 112 L 964 101 L 960 102 L 959 114 L 953 119 L 946 119 L 937 112 L 934 100 
                             L 944 83 L 954 78 L 966 78 L 975 82 L 985 93 L 989 106 L 987 117 L 978 131 
                             L 962 144 L 948 151 L 924 157 L 905 157 L 881 151 L 861 140 L 806 97 L 781 86 
                             L 760 84 L 735 94 L 655 160 L 642 166 L 635 166 L 629 159 L 627 149 L 638 118 
                             L 632 98 L 624 91 L 611 86 L 593 87 L 576 96 L 556 120 L 553 130 L 555 156 
                             L 567 183 L 615 257 L 627 293 L 624 318 L 617 331 L 604 344 L 585 353 L 558 355 
                             L 538 350 L 521 341 L 506 326 L 501 315 L 503 296 L 534 261 L 532 251 L 522 244 
                             L 509 245 L 500 250 L 464 285 L 431 308 L 407 319 L 376 327 L 347 328 L 315 323 
                             L 223 293 L 180 285 L 151 285 L 124 290 L 100 299 L 72 317 L 74 321 L 91 309 
                             L 123 295 L 147 290 L 173 289 L 224 298 L 317 328 L 341 332 L 379 331 L 412 322 
                             L 435 311 L 469 287 L 500 256 L 519 248 L 528 254 L 529 261 L 502 288 L 496 305 
                             L 497 317 L 503 330 L 517 344 L 533 353 L 554 359 L 578 359 L 605 349 L 624 329 
                             L 631 308 L 631 289 L 623 263 L 570 179 L 558 150 L 559 125 L 568 110 L 580 99 
                             L 592 92 L 608 90 L 620 94 L 629 102 L 633 110 L 633 121 L 624 140 L 623 153 
                             L 627 165 L 636 171 L 650 168 L 665 159 L 731 103 L 757 89 L 774 89 L 802 100 
                             L 857 143 L 874 153 L 901 161 L 928 161 L 963 149 L 980 136 L 991 119 L 993 102 
                             L 986 86 L 971 75 Z"
                          fill="none"
                          stroke="var(--color-text)"
                          stroke-width="8"
                          stroke-linecap="round"
                          stroke-linejoin="round"/>
                </g>
                <text x="20" y="72" font-family="'Montserrat', 'Inter', sans-serif" font-size="46" font-weight="400" fill="var(--color-text)" letter-spacing="1">Dance</text>
                <text x="155" y="72" font-family="'Montserrat', 'Inter', sans-serif" font-size="46" font-weight="400" fill="var(--color-primary)" letter-spacing="1">In</text>
                <text x="235" y="72" font-family="'Montserrat', 'Inter', sans-serif" font-size="46" font-weight="400" fill="var(--color-primary)" letter-spacing="1">ense</text>
            </svg>
        """.trimIndent()
                    }
                    onClick { appState.value = Page.HOME }
                }
                div(className = "collapse navbar-collapse justify-content-center") {
                    bind(userRole) { role ->
                        if (role != null) {
                            div(className = "navbar-nav") {
                                link(I18n.tr("Mój Panel", "My Dashboard"), "javascript:void(0)", className = "nav-link px-3 text-light fw-medium") {
                                    onClick {
                                        // POPRAWKA: Zablokowany użytkownik nie może przejść do panelu z navbaru.
                                        val isBlocked = window.localStorage.getItem("isActive") == "false"
                                        appState.value = if (isBlocked) {
                                            Page.BLOCKED
                                        } else {
                                            when (role) {
                                                "DANCER" -> Page.DANCER_DASHBOARD
                                                "CHOREOGRAPHER" -> Page.CHOREO_DASHBOARD
                                                "ADMIN" -> Page.ADMIN_PANEL
                                                else -> Page.HOME
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                div(className = "d-flex align-items-center") {
                    bind(userRole) { role ->
                        if (role == null) {
                            link(I18n.tr("Zaloguj się", "Log in"), "javascript:void(0)", className = "nav-link text-light me-4 fw-medium") {
                                onClick { loginModal?.show() }
                            }
                            tag(TAG.BUTTON, I18n.tr("Dołącz teraz", "Join now"), className = "btn btn-light text-dark fw-bold px-4 rounded-pill") {
                                onClick { registerModal?.show() }
                            }
                        } else {
                            span(I18n.tr("Zalogowano jako: $role", "Logged in as: $role"), className = "text-muted me-3 small")
                            tag(TAG.BUTTON, I18n.tr("Wyloguj", "Log out"), className = "btn dance-btn-primary btn-sm rounded-pill px-3 fw-bold") {
                                onClick {
                                    window.localStorage.removeItem("jwt")
                                    window.localStorage.removeItem("userRole")
                                    window.localStorage.removeItem("userId")
                                    userRole.value = null
                                    currentUserId.value = null
                                    appState.value = Page.HOME
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // STOPKA (FOOTER)
    // ==========================================
    private fun Container.buildFooter() {
        // Tło bg-black tworzy delikatny kontrast względem sekcji ogłoszeń (bg-dark)
        tag(TAG.FOOTER, className = "bg-black py-4 border-top border-secondary mt-auto") {
            div(className = "container") {
                div(className = "row gy-4") {
                    // Kolumna 1 - Info
                    div(className = "col-md-4 text-center text-md-start") {
                        h5("DanceInSense", className = "text-primary-dance fw-bold mb-3")
                        p(I18n.tr("Twoja innowacyjna przestrzeń do rozwoju tanecznego. Wgrywaj nagrania, odbieraj precyzyjny feedback sekunda po sekundzie i stawaj się coraz lepszy dzięki wsparciu ekspertów.", "Your innovative space for dance development. Upload recordings, get precise second-by-second feedback, and improve with expert support."), className = "text-muted small")
                    }

                    // Kolumna 2 - Linki
                    div(className = "col-md-4 text-center") {
                        h5(I18n.tr("Szybkie linki", "Quick Links"), className = "fw-bold mb-3 text-white")
                        ul(className = "list-unstyled small") {
                            li(className = "mb-2") {
                                link(I18n.tr("Strona Główna", "Home"), "javascript:void(0)", className = "text-muted text-decoration-none") {
                                    onClick { appState.value = Page.HOME }
                                }
                            }
                            // Jeśli niezalogowany - pokaż linki logowania/rejestracji
                            bind(userRole) { role ->
                                if (role == null) {
                                    li(className = "mb-2") {
                                        link(I18n.tr("Zaloguj się", "Login"), "javascript:void(0)", className = "text-muted text-decoration-none") {
                                            onClick { loginModal?.show() }
                                        }
                                    }
                                    li(className = "mb-2") {
                                        link(I18n.tr("Zarejestruj się", "Sign up"), "javascript:void(0)", className = "text-muted text-decoration-none") {
                                            onClick { registerModal?.show() }
                                        }
                                    }
                                } else {
                                    li(className = "mb-2") {
                                        link(I18n.tr("Przejdź do Panelu", "Go to Dashboard"), "javascript:void(0)", className = "text-muted text-decoration-none") {
                                            onClick {
                                                val isBlocked = window.localStorage.getItem("isActive") == "false"
                                                appState.value = if (isBlocked) {
                                                    Page.BLOCKED
                                                } else {
                                                    when (role) {
                                                        "DANCER" -> Page.DANCER_DASHBOARD
                                                        "CHOREOGRAPHER" -> Page.CHOREO_DASHBOARD
                                                        "ADMIN" -> Page.ADMIN_PANEL
                                                        else -> Page.HOME
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Kolumna 3 - Kontakt
                    div(className = "col-md-4 text-center text-md-end") {
                        h5(I18n.tr("Kontakt", "Contact"), className = "fw-bold mb-3 text-white")
                        p(className = "text-muted small mb-2") {
                            tag(TAG.I, className = "fa-solid fa-envelope me-2")
                            span("kontakt@danceapp.pl")
                        }
                        p(className = "text-muted small mb-3") {
                            tag(TAG.I, className = "fa-solid fa-location-dot me-2")
                            span("Białystok, Polska")
                        }
                    }
                }

                // Dolny pasek Copyright
                div(className = "border-top border-secondary mt-4 pt-3 text-center text-muted small") {
                    span("© 2026 DanceInSense. " + I18n.tr("Wszelkie prawa zastrzeżone.", "All rights reserved."))
                }
            }
        }
    }

    // ==========================================
    // HOME & OGŁOSZENIA
    // ==========================================
    private fun Container.buildHomeView() {
        div {
            div(className = "hero-section") {
                height = 85.vh; display = Display.FLEX; alignItems = AlignItems.CENTER; justifyContent = JustifyContent.CENTER
                position = Position.RELATIVE; overflow = Overflow.HIDDEN
                tag(TAG.VIDEO, className = "hero-video") {
                    setAttribute("autoplay", "autoplay"); setAttribute("loop", "loop")
                    setAttribute("muted", "muted"); setAttribute("playsinline", "playsinline")
                    tag(TAG.SOURCE) { setAttribute("src", "video/dance-bg.mp4"); setAttribute("type", "video/mp4") }
                }
                div(className = "hero-overlay w-100 d-flex justify-content-center align-items-center text-center") {
                    vPanel(alignItems = AlignItems.CENTER) {
                        h1(I18n.tr("Twój taniec. Nasz feedback.", "Your dance. Our feedback."), className = "steezy-hero-title mb-3 shadow-text")
                        p(I18n.tr("Wgraj nagranie i otrzymaj wskazówki od choreografów sekunda po sekundzie.", "Upload a recording and get second-by-second feedback from choreographers."), className = "steezy-hero-subtitle mb-5 shadow-text px-3")
                        tag(TAG.BUTTON, I18n.tr("Poczuj rytm", "Feel the rhythm"), className = "btn dance-btn-primary btn-lg px-5 py-3 rounded-pill fw-bold") {
                            onClick { registerModal?.show() }
                        }
                    }
                }
            }
            buildFeaturesSection()

            // DODANA SEKCJA: JAK TO DZIAŁA
            buildHowItWorksSection()

            buildAnnouncementsSection()
        }
    }

    private fun Container.buildFeaturesSection() {
        div(className = "bg-white py-5") {
            div(className = "container py-5") {
                div(className = "row text-center mb-5") {
                    h2(I18n.tr("Dlaczego nasza platforma?", "Why our platform?"), className = "fw-bold text-dark")
                    p(I18n.tr("Zaprojektowana z myślą o rozwoju i komunikacji.", "Designed for growth and communication."), className = "text-muted")
                }
                div(className = "row g-4 text-center justify-content-center") {
                    featureCard("fa-users", I18n.tr("Dla Tancerzy", "For Dancers"), I18n.tr("Otrzymuj konkretny feedback do swoich ruchów. Wgrywaj nagrania i śledź swój progres w dedykowanym panelu.", "Get specific feedback on your moves. Upload recordings and track progress."))
                    featureCard("fa-video", I18n.tr("Dla Choreografów", "For Choreographers"), I18n.tr("Zarządzaj zadaniami. Innowacyjny odtwarzacz wideo z notatkami czasowymi ułatwi Ci szybką ocenę techniki.", "Manage tasks. Innovative video player with timestamped notes."))
                    featureCard("fa-comments", I18n.tr("Przestrzeń Komunikacji", "Communication Space"), I18n.tr("Bezpośredni kontakt trenera z tancerzem. Wymieniajcie się uwagami, aby każdy trening był jeszcze efektywniejszy.", "Direct contact between coach and dancer. Exchange notes to make training more effective."))
                }
            }
        }
    }

    // SEKCJA JAK TO DZIAŁA
    private fun Container.buildHowItWorksSection() {
        div(className = "bg-dark py-5 border-top border-secondary") {
            div(className = "container py-4") {
                div(className = "text-center mb-5") {
                    h2(I18n.tr("Jak to działa?", "How it works?"), className = "fw-bold text-white")
                    p(I18n.tr("Proces nauki jeszcze nigdy nie był tak prosty.", "Learning process has never been easier."), className = "text-muted")
                }
                div(className = "row g-4 text-center") {
                    div(className = "col-md-4") {
                        div(className = "p-4 hover-card") {
                            div(className = "bg-primary-dance text-black rounded-circle d-inline-flex align-items-center justify-content-center mb-3 shadow-sm") {
                                setStyle("width", "80px"); setStyle("height", "80px")
                                tag(TAG.I, className = "fa-solid fa-list-check fa-2x")
                            }
                            h4(I18n.tr("1. Wybierz zadanie", "1. Choose a task"), className = "fw-bold text-white mb-2")
                            p(I18n.tr("Przeglądaj wyzwania przygotowane przez Twoich choreografów i zapoznaj się z wideo wzorcowym.", "Browse challenges prepared by your choreographers and check the reference video."), className = "text-muted small")
                        }
                    }
                    div(className = "col-md-4") {
                        div(className = "p-4 hover-card") {
                            div(className = "bg-primary-dance text-black rounded-circle d-inline-flex align-items-center justify-content-center mb-3 shadow-sm") {
                                setStyle("width", "80px"); setStyle("height", "80px")
                                tag(TAG.I, className = "fa-solid fa-video fa-2x")
                            }
                            h4(I18n.tr("2. Wgraj nagranie", "2. Upload recording"), className = "fw-bold text-white mb-2")
                            p(I18n.tr("Nagraj swoje wykonanie układu i prześlij je bezpośrednio na platformę z dowolnego urządzenia.", "Record your performance and upload it directly to the platform from any device."), className = "text-muted small")
                        }
                    }
                    div(className = "col-md-4") {
                        div(className = "p-4 hover-card") {
                            div(className = "bg-primary-dance text-black rounded-circle d-inline-flex align-items-center justify-content-center mb-3 shadow-sm") {
                                setStyle("width", "80px"); setStyle("height", "80px")
                                tag(TAG.I, className = "fa-solid fa-star fa-2x")
                            }
                            h4(I18n.tr("3. Odbierz feedback", "3. Get feedback"), className = "fw-bold text-white mb-2")
                            p(I18n.tr("Instruktor oceni Twój taniec, dodając precyzyjne komentarze w konkretnych sekundach wideo.", "The instructor will grade your dance, adding precise comments at specific video seconds."), className = "text-muted small")
                        }
                    }
                }
            }
        }
    }

    private fun Container.buildAnnouncementsSection() {
        ApiService.fetchAnnouncements().then<dynamic> { res ->
            DataManager.globalAnnouncements.clear()
            DataManager.globalAnnouncements.addAll(res as Array<dynamic>)
            null
        }.catch<dynamic> { _: Throwable -> null }

        div(className = "bg-dark py-5 border-top border-secondary") {
            div(className = "container") {
                h2(I18n.tr("Aktualności i Ogłoszenia", "News & Announcements"), className = "text-center fw-bold text-white mb-4")
                div(className = "row g-3 justify-content-center") {
                    bind(DataManager.globalAnnouncements) { list ->
                        if (list.isEmpty()) p(I18n.tr("Brak nowych ogłoszeń.", "No new announcements."), className = "text-center text-muted w-100")
                        else {
                            list.forEach { ann ->
                                div(className = "col-md-6") {
                                    div(className = "card bg-dark text-white shadow-sm p-0 h-100 border-0 border-start border-5 border-primary overflow-hidden") {
                                        val media = ann.mediaUrl?.toString()
                                        if (!media.isNullOrBlank()) {
                                            val url = toVideoUrl(media)
                                            if (media.contains(".mp4") || media.contains(".mov")) {
                                                tag(TAG.VIDEO, className = "w-100 bg-black") {
                                                    setAttribute("controls", "controls")
                                                    setStyle("max-height", "200px")
                                                    tag(TAG.SOURCE) { setAttribute("src", url) }
                                                }
                                            } else {
                                                image(url, className = "img-fluid w-100") {
                                                    setStyle("max-height", "200px")
                                                    setStyle("object-fit", "cover")
                                                }
                                            }
                                        }
                                        div(className = "p-3 d-flex flex-column h-100") {
                                            span(ann.type?.toString() ?: "Info", className = "badge bg-info mb-2 align-self-start")
                                            h5(ann.title?.toString() ?: "", className = "fw-bold text-white")
                                            p(ann.content?.toString() ?: "", className = "text-muted small mb-3") {
                                                setStyle("white-space", "pre-wrap")
                                            }
                                            div(className = "mt-auto text-end small text-muted fw-bold") {
                                                span(ann.date?.toString()?.take(10) ?: "")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // LOGOWANIE
    // ==========================================
    private fun createLoginModal(): Modal {
        val modal = Modal(I18n.tr("Zaloguj się", "Log in"), closeButton = true, animation = true) {
            addCssClass("login-modal-custom")
        }

        modal.vPanel(className = "p-4") {
            h3(I18n.tr("Witaj w DANCE APP", "Welcome to DANCE APP"), className = "text-center fw-bold text-primary-dance mb-4")

            label(I18n.tr("Adres e-mail", "Email address"), className = "form-label fw-bold mb-1")
            val emailInput = textInput(type = io.kvision.html.InputType.TEXT, className = "form-control login-input rounded-3 mb-3") {
                placeholder = I18n.tr("wpisz swój e-mail", "enter your email")
            }

            label(I18n.tr("Hasło", "Password"), className = "form-label fw-bold mb-1")
            var passwordInput: io.kvision.form.text.TextInput? = null

            div(className = "input-group mb-4") {
                passwordInput = textInput(type = io.kvision.html.InputType.PASSWORD, className = "form-control login-input rounded-start-3") {
                    placeholder = I18n.tr("wpisz hasło", "enter password")
                }
                tag(TAG.BUTTON, className = "btn dance-btn-primary rounded-end-3") {
                    setAttribute("type", "button")
                    val iconTag = tag(TAG.I, className = "fa-solid fa-eye-slash text-light")
                    onClick {
                        if (passwordInput?.type == io.kvision.html.InputType.PASSWORD) {
                            passwordInput?.type = io.kvision.html.InputType.TEXT
                            iconTag.removeCssClass("fa-eye-slash"); iconTag.addCssClass("fa-eye")
                        } else {
                            passwordInput?.type = io.kvision.html.InputType.PASSWORD
                            iconTag.removeCssClass("fa-eye"); iconTag.addCssClass("fa-eye-slash")
                        }
                    }
                }
            }

            val errorText = span("", className = "small fw-bold") { setAttribute("style", "color: #000000 !important;") }
            val errorAlert = div(className = "alert alert-danger py-2 mb-3 text-center rounded-3") { visible = false; add(errorText) }

            tag(TAG.BUTTON, I18n.tr("Zaloguj się", "Log in"), className = "btn dance-btn-primary btn-lg w-100 rounded-pill fw-bold") {
                onClick {
                    val email = emailInput.getElement()?.asDynamic()?.value?.toString() ?: ""
                    val pass = passwordInput?.getElement()?.asDynamic()?.value?.toString() ?: ""
                    if (email.isBlank() || pass.isBlank()) {
                        errorText.content = I18n.tr("Podaj e-mail i hasło.", "Enter email and password.")
                        errorAlert.visible = true
                        return@onClick
                    }

                    errorAlert.visible = false

                    // NOWA LOGIKA: Czytamy isActive bezpośrednio z odpowiedzi logowania
                    ApiService.login(email, pass).then<dynamic> { res: dynamic ->
                        val token = res.token?.toString()
                        val role = res.role?.toString()
                        // POPRAWKA: Jackson odcina prefix "is" z Boolean → pole leci jako "active".
                        // Po dodaniu @JsonProperty("isActive") w AuthResponse leci jako "isActive".
                        // Czytamy OBA pola: priorytet "isActive" (po naprawie), fallback "active".
                        val isActive = when {
                            res.isActive?.toString() == "true"  -> true
                            res.isActive?.toString() == "false" -> false
                            res.active?.toString()   == "true"  -> true
                            else -> false
                        }

                        if (token != null && role != null) {
                            window.localStorage.setItem("jwt", token)
                            window.localStorage.setItem("userRole", role)
                            window.localStorage.setItem("isActive", isActive.toString())

                            // Jeśli w odpowiedzi jest ID, zapisz je (pomocne do czatu)
                            if (res.id != null) {
                                window.localStorage.setItem("userId", res.id.toString())
                                currentUserId.value = res.id.toString().toIntOrNull()
                            }

                            userRole.value = role

                            // KRYTYCZNY MOMENT: Sprawdzamy status ZANIM ustawimy widok
                            if (!isActive) {
                                appState.value = Page.BLOCKED // Przekierowanie do widoku kłódki
                            } else {
                                appState.value = when (role) {
                                    "DANCER" -> Page.DANCER_DASHBOARD
                                    "CHOREOGRAPHER" -> Page.CHOREO_DASHBOARD
                                    "ADMIN" -> Page.ADMIN_PANEL
                                    else -> Page.HOME
                                }
                            }
                            modal.hide()
                        }
                        null
                    }.catch<dynamic> {
                        errorText.content = I18n.tr("Błędny e-mail lub hasło!", "Invalid email or password!")
                        errorAlert.visible = true
                        null
                    }
                }
            }
        }
        return modal
    }

    // ==========================================
    // REJESTRACJA
    // ==========================================
    private fun createRegisterModal(): Modal {
        val modal = Modal(I18n.tr("Zarejestruj się", "Sign up"), closeButton = true, animation = true) {
            addCssClass("login-modal-custom")
        }

        modal.vPanel(className = "p-4") {
            h3(I18n.tr("Utwórz konto w DANCE APP", "Create an account"), className = "text-center fw-bold text-primary-dance mb-4")
            label(I18n.tr("Imię", "First Name"), className = "form-label fw-bold mb-1")
            val firstNameInput = textInput(className = "form-control login-input rounded-3 mb-3") { placeholder = "Wpisz imię" }
            label(I18n.tr("Nazwisko", "Last Name"), className = "form-label fw-bold mb-1")
            val lastNameInput = textInput(className = "form-control login-input rounded-3 mb-3") { placeholder = "Wpisz nazwisko" }
            label(I18n.tr("E-mail", "Email"), className = "form-label fw-bold mb-1")
            val emailInput = textInput(className = "form-control login-input rounded-3 mb-3") { placeholder = "twoj@email.com" }
            label(I18n.tr("Hasło", "Password"), className = "form-label fw-bold mb-1")
            val passInput = textInput(type = io.kvision.html.InputType.PASSWORD, className = "form-control login-input rounded-3 mb-4") { placeholder = "Wpisz hasło" }

            val errorMsg = span("", className = "text-danger small d-block mb-2 text-center") { visible = false }

            tag(TAG.BUTTON, I18n.tr("Dołącz!", "Join!"), className = "btn dance-btn-primary btn-lg w-100 rounded-pill fw-bold") {
                onClick {
                    val fn = firstNameInput.value ?: ""; val ln = lastNameInput.value ?: ""; val em = emailInput.value ?: ""; val ps = passInput.value ?: ""
                    if (fn.isBlank() || ln.isBlank() || em.isBlank() || ps.isBlank()) { errorMsg.visible = true; return@onClick }
                    ApiService.registerUser(fn, ln, em, ps).then<dynamic> { modal.hide(); null }.catch<dynamic> { errorMsg.visible = true; null }
                }
            }
        }
        return modal
    }

    // ==========================================
    // CHOREOGRAF
    // ==========================================
    private fun Container.buildChoreoDashboard() {
        div(className = "container py-5 mt-5") {
            h2(I18n.tr("Panel Mentorski (Choreograf)", "Mentoring Panel (Choreographer)"), className = "fw-bold text-primary-dance mb-4 pt-4")
            div(className = "row g-4 justify-content-center") {
                dashboardCard("fa-clock", I18n.tr("Kolejka do oceny", "Grading Queue"), I18n.tr("Filmy oczekujące na feedback.", "Videos waiting for feedback.")) { appState.value = Page.CHOREO_QUEUE }
                dashboardCard("fa-plus-circle", I18n.tr("Zarządzanie zadaniami", "Manage Tasks"), I18n.tr("Dodawaj wyzwania dla tancerzy.", "Add challenges for dancers.")) { appState.value = Page.CHOREO_TASKS }
                dashboardCard("fa-folder-open", I18n.tr("Archiwum ocen", "Grading Archive"), I18n.tr("Przeglądaj swoje oceny.", "Browse your grades.")) { appState.value = Page.CHOREO_ARCHIVE }
                dashboardCard("fa-comments", I18n.tr("Wiadomości", "Messages"), I18n.tr("Messenger społeczności.", "Community messenger.")) { appState.value = Page.CHAT }
            }
        }
    }

    private fun Container.buildChoreoQueue() {
        val subs = io.kvision.state.ObservableListWrapper<dynamic>()
        val tasks = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)

        ApiService.fetchTasks().then<dynamic> { res: dynamic ->
            val taskList = res as Array<dynamic>
            tasks.addAll(taskList)
            val taskIds = taskList.mapNotNull { it.id?.toString()?.toIntOrNull() }
            if (taskIds.isEmpty()) { loading.value = false; return@then null }
            fun fetchNext(index: Int) {
                if (index >= taskIds.size) { loading.value = false; return }
                ApiService.fetchSubmissionsForTask(taskIds[index]).then<dynamic> { r: dynamic ->
                    val list = (r as Array<dynamic>).filter { it.status?.toString() == "SUBMITTED" }
                    subs.addAll(list)
                    fetchNext(index + 1)
                    null
                }.catch<dynamic> { _: Throwable -> fetchNext(index + 1); null }
            }
            fetchNext(0)
            null
        }.catch<dynamic> { _: Throwable -> loading.value = false; null }

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.CHOREO_DASHBOARD)
            h2(I18n.tr("Kolejka do oceny", "Grading Queue"), className = "fw-bold mb-4")
            div {
                bind(loading) { isLoading ->
                    if (isLoading) {
                        div(className = "text-center py-5") {
                            tag(TAG.DIV, className = "spinner-border text-primary-dance") { setAttribute("role", "status") }
                            p(I18n.tr("Ładowanie nagrań...", "Loading recordings..."), className = "text-muted mt-3")
                        }
                    } else {
                        div {
                            bind(subs) { list ->
                                if (list.isEmpty()) {
                                    div(className = "card bg-dark border-secondary p-5 text-center") {
                                        tag(TAG.I, className = "fa-solid fa-check-circle fa-3x text-success mb-3")
                                        p(I18n.tr("Brak filmów do oceny.", "Yay! No videos to grade."), className = "text-muted")
                                    }
                                } else {
                                    table(className = "table table-dark table-hover align-middle") {
                                        thead { tr {
                                            th(I18n.tr("Nagranie", "Recording")); th(I18n.tr("Tancerz", "Dancer"))
                                            th(I18n.tr("Zadanie", "Task")); th(I18n.tr("Data", "Date"))
                                            th(I18n.tr("Status", "Status")); th(I18n.tr("Akcja", "Action"))
                                        } }
                                        tbody {
                                            list.forEach { s ->
                                                val subId = s.id?.toString()?.toIntOrNull() ?: 0
                                                val videoUrl = s.videoUrl?.toString() ?: ""
                                                val taskId = s.taskId?.toString()?.toIntOrNull() ?: 0
                                                val taskObj = tasks.find { it.id?.toString()?.toIntOrNull() == taskId }
                                                val instrUrl = taskObj?.instructionVideoUrl?.toString() ?: ""

                                                val restoredIds = window.localStorage.getItem("restored_subs")?.split(",") ?: emptyList()
                                                val isRestoredLocal = restoredIds.contains(subId.toString())

                                                tr {
                                                    td(I18n.tr("Wideo #", "Video #") + "${s.id}")
                                                    td(I18n.tr("Tancerz #", "Dancer #") + "${s.dancerId}")
                                                    td(taskObj?.title?.toString() ?: (I18n.tr("Zadanie #", "Task #") + "$taskId"))
                                                    td(s.sentAt?.toString()?.substring(0, 10) ?: "—")
                                                    td {
                                                        if (isRestoredLocal) {
                                                            span(I18n.tr("Oczekuje na zaktualizowany feedback", "Awaiting updated feedback"), className = "badge border border-warning text-warning p-2")
                                                        } else {
                                                            span(I18n.tr("Oczekuje", "Pending"), className = "badge bg-warning")
                                                        }
                                                    }
                                                    td {
                                                        tag(TAG.BUTTON, I18n.tr("Oceń wideo", "Grade video"), className = "btn btn-sm dance-btn-primary") {
                                                            onClick {
                                                                PlayerState.submissionId = subId
                                                                PlayerState.submissionVideoUrl = toVideoUrl(videoUrl)
                                                                PlayerState.instructionVideoUrl = toVideoUrl(instrUrl)
                                                                PlayerState.taskTitle = taskObj?.title?.toString() ?: (I18n.tr("Zadanie #", "Task #") + "$taskId")
                                                                PlayerState.taskId = taskId
                                                                PlayerState.isGraded = false
                                                                PlayerState.currentScore = null
                                                                PlayerState.currentFeedback = null
                                                                appState.value = Page.PLAYER
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Container.buildChoreoTasks() {
        val selected = io.kvision.state.ObservableListWrapper<String>()
        val choreoId = window.localStorage.getItem("userId")?.toLongOrNull() ?: 1L

        val activeTasks = io.kvision.state.ObservableListWrapper<dynamic>()
        ApiService.fetchTasks().then<dynamic> { res ->
            activeTasks.addAll(res as Array<dynamic>)
            null
        }.catch<dynamic> { _: Throwable -> null }

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.CHOREO_DASHBOARD)
            div(className = "row") {
                div(className = "col-md-7") {
                    h2(I18n.tr("Kreator zadań", "Task Creator"), className = "fw-bold mb-4")
                    div(className = "card bg-dark border-secondary p-4") {
                        val taskTitleInput = textInput(className = "form-control mb-3") { placeholder = I18n.tr("Tytuł zadania...", "Task Title...") }
                        val taskDescInput = textArea { addCssClass("form-control"); setAttribute("rows", "3"); placeholder = I18n.tr("Opis wymagań...", "Requirements description...") }

                        label(I18n.tr("Termin wykonania:", "Deadline:"), className = "form-label text-light fw-bold mt-3")
                        val deadlineInput = tag(TAG.INPUT, className = "form-control mb-3") {
                            setAttribute("type", "datetime-local")
                            val now = js("new Date()")
                            now.setMonth(now.getMonth() + 3)
                            val iso = now.toISOString().toString().substring(0, 16)
                            setAttribute("value", iso)
                        }

                        label(I18n.tr("Przypisz do tancerzy:", "Assign to dancers:"), className = "form-label text-light fw-bold mt-2")
                        val tagsBox = div(className = "dancer-tags-box mb-0") {}
                        val dropdownBox = div(className = "dancer-dropdown") { visible = false }

                        fun refresh() {
                            tagsBox.removeAll()
                            tagsBox.apply {
                                if (selected.isEmpty()) span(I18n.tr("Kliknij, aby wybrać tancerzy \u25BE", "Click to select dancers \u25BE"), className = "text-muted small fst-italic")
                                else {
                                    selected.forEach { id ->
                                        val name = DataManager.allDancers.find { it.first == id }?.second ?: id
                                        span(className = "dancer-tag") {
                                            span(name)
                                            span(" \u00D7", className = "dancer-tag-remove") { onClick { selected.remove(id); refresh() } }
                                        }
                                    }
                                    span(" \u25BE", className = "text-muted ms-2 small")
                                }
                            }
                            dropdownBox.removeAll()
                            dropdownBox.apply {
                                val allChosen = DataManager.allDancers.all { selected.contains(it.first) }
                                if (!allChosen) {
                                    div(className = "dancer-option dancer-option-all") {
                                        span(I18n.tr("\u2605 Wszyscy moi tancerze", "\u2605 All my dancers"))
                                        onClick { DataManager.allDancers.forEach { (id, _) -> if (!selected.contains(id)) selected.add(id) }; dropdownBox.visible = false; refresh() }
                                    }
                                }
                                DataManager.allDancers.filter { !selected.contains(it.first) }.forEach { (id, name) ->
                                    div(className = "dancer-option") {
                                        span(name)
                                        onClick { selected.add(id); dropdownBox.visible = false; refresh() }
                                    }
                                }
                                if (allChosen) div(className = "dancer-option text-muted fst-italic small") { span(I18n.tr("Wszyscy tancerze wybrani \u2713", "All dancers selected \u2713")) }
                            }
                        }
                        tagsBox.onClick { dropdownBox.visible = !dropdownBox.visible }
                        refresh()

                        div(className = "mt-4") {
                            label(I18n.tr("Wideo wzorcowe (wymagane)", "Reference video (required)"), className = "form-label text-light fw-bold")
                            val fileInput = tag(TAG.INPUT, className = "form-control mb-3") {
                                setAttribute("type", "file"); setAttribute("accept", "video/*")
                            }
                            tag(TAG.BUTTON, I18n.tr("Opublikuj zadanie", "Publish task"), className = "btn dance-btn-primary w-100 mt-3") {
                                onClick {
                                    val title = taskTitleInput.value
                                    val desc = taskDescInput.value ?: ""
                                    val files = fileInput.getElement()?.asDynamic().files
                                    val deadlineRaw = deadlineInput.getElement()?.asDynamic().value?.toString() ?: ""
                                    val deadline = if (deadlineRaw.length >= 16) {
                                        val date = deadlineRaw.substring(0, 10)
                                        val time = deadlineRaw.substring(11, 16)
                                        val parts = date.split("-")
                                        if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]} $time" else "01.01.2027 12:00"
                                    } else "01.01.2027 12:00"

                                    if (title != null && files != null && files.length > 0) {
                                        val file = files[0]
                                        ApiService.createTask(title, desc, deadline, choreoId, file).then<dynamic> { response: dynamic ->
                                            activeTasks.add(0, response)
                                            showToast(I18n.tr("✔ Zadanie zapisane w bazie!", "✔ Task saved to database!"))
                                            taskTitleInput.value = null; taskDescInput.value = null
                                            fileInput.getElement()?.asDynamic().value = ""
                                            selected.clear(); refresh()
                                            null
                                        }.catch<dynamic> { _: Throwable -> window.alert(I18n.tr("Błąd połączenia z serwerem.", "Server connection error.")); null }
                                    } else {
                                        window.alert(I18n.tr("Wypełnij tytuł i dodaj wideo!", "Fill the title and add a video!"))
                                    }
                                }
                            }
                        }
                    }
                }
                div(className = "col-md-5") {
                    h4(I18n.tr("Twoje aktywne zadania", "Your active tasks"), className = "fw-bold mb-3 mt-4 mt-md-0")
                    ul(className = "list-group bg-dark shadow-sm") {
                        bind(activeTasks) { tasks ->
                            if (tasks.isEmpty()) li(className = "list-group-item bg-dark text-muted border-secondary") { span(I18n.tr("Brak zadań.", "No tasks.")) }
                            tasks.forEach { task ->
                                val tId = task.id?.toString()?.toIntOrNull() ?: 0
                                val tTitle = task.title?.toString() ?: "Bez tytułu"
                                val tDesc = task.description?.toString() ?: ""
                                val tVid = task.instructionVideoUrl?.toString()

                                li(className = "list-group-item bg-dark border-secondary hover-card mb-2 d-flex justify-content-between align-items-center") {
                                    setStyle("cursor", "pointer")
                                    span(tTitle, className = "fw-bold text-white")
                                    tag(TAG.I, className = "fa-solid fa-chevron-right text-muted small")
                                    onClick {
                                        showTaskDetails(ChoreoTask(tId, tTitle, tDesc, tVid, emptyList()))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showTaskDetails(task: ChoreoTask) {
        val modal = Modal(I18n.tr("Szczegóły: ", "Details: ") + "${task.title}", closeButton = true, animation = true)
        modal.div(className = "p-3") {
            h6(I18n.tr("Opis zadania:", "Task description:"), className = "text-primary-dance fw-bold mb-1")
            p(task.description.ifBlank { I18n.tr("Brak opisu.", "No description.") }) { setStyle("color", "#000000"); addCssClass("mb-3") }

            if (task.instructionVideoUrl != null) {
                h6(I18n.tr("Wideo wzorcowe:", "Reference video:"), className = "text-primary-dance fw-bold mb-2")
                val fullUrl = toVideoUrl(task.instructionVideoUrl)
                tag(TAG.VIDEO, className = "w-100 rounded border border-secondary mb-3") {
                    setAttribute("controls", "controls"); setAttribute("style", "max-height:300px; background:#000;")
                    tag(TAG.SOURCE) { setAttribute("src", fullUrl) }
                }
            } else {
                p(I18n.tr("Brak wideo wzorcowego.", "No reference video.")) { setStyle("color", "#6c757d"); addCssClass("fst-italic mb-3") }
            }

            div(className = "mt-4 pt-3 border-top border-secondary text-center") {
                h6(I18n.tr("Zarządzaj zadaniem:", "Manage task:"), className = "text-danger fw-bold mb-2")
                tag(TAG.BUTTON, I18n.tr("🗑 Usuń to zadanie (dla wszystkich)", "🗑 Delete task (for everyone)"), className = "btn btn-outline-danger btn-sm w-100") {
                    onClick {
                        if (window.confirm(I18n.tr("Czy na pewno chcesz całkowicie usunąć to zadanie? Tancerze stracą do niego dostęp.", "Are you sure you want to delete this task? Dancers will lose access."))) {
                            ApiService.deleteTaskAPI(task.id).then<dynamic> { _: dynamic ->
                                modal.hide()
                                showToast(I18n.tr("✔ Odśwież stronę, by zobaczyć zmiany.", "✔ Refresh page to see changes."))
                                null
                            }.catch<dynamic> { _: Throwable ->
                                window.alert(I18n.tr("Błąd połączenia. Upewnij się, że backend ma gotowy endpoint DELETE /tasks/{id}.", "Connection error. Make sure the backend has DELETE endpoint ready."))
                                null
                            }
                        }
                    }
                }
            }
        }
        modal.show()
    }

    private fun Container.buildChoreoArchive() {
        val subs = io.kvision.state.ObservableListWrapper<dynamic>()
        val tasks = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)

        ApiService.fetchTasks().then<dynamic> { res: dynamic ->
            val taskList = res as Array<dynamic>
            tasks.addAll(taskList)
            val taskIds = taskList.mapNotNull { it.id?.toString()?.toIntOrNull() }
            if (taskIds.isEmpty()) { loading.value = false; return@then null }
            fun fetchNext(index: Int) {
                if (index >= taskIds.size) { loading.value = false; return }
                ApiService.fetchSubmissionsForTask(taskIds[index]).then<dynamic> { r: dynamic ->
                    val list = (r as Array<dynamic>).filter {
                        it.status?.toString() == "GRADED" || it.status?.toString() == "RESTORED"
                    }
                    subs.addAll(list)
                    fetchNext(index + 1)
                    null
                }.catch<dynamic> { _: Throwable -> fetchNext(index + 1); null }
            }
            fetchNext(0)
            null
        }.catch<dynamic> { _: Throwable -> loading.value = false; null }

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.CHOREO_DASHBOARD)
            h2(I18n.tr("Archiwum ocenionych nagrań", "Archive of graded recordings"), className = "fw-bold mb-4")
            div {
                bind(loading) { isLoading ->
                    if (isLoading) {
                        div(className = "text-center py-5") {
                            tag(TAG.DIV, className = "spinner-border text-primary-dance") { setAttribute("role", "status") }
                        }
                    } else {
                        div {
                            bind(subs) { list ->
                                if (list.isEmpty()) {
                                    div(className = "card bg-dark border-secondary p-5 text-center") {
                                        p(I18n.tr("Jeszcze nic nie oceniłeś.", "You haven't graded anything yet."), className = "text-muted")
                                    }
                                } else {
                                    table(className = "table table-dark table-hover align-middle") {
                                        thead { tr {
                                            th(I18n.tr("Nagranie", "Recording")); th(I18n.tr("Tancerz", "Dancer"))
                                            th(I18n.tr("Zadanie", "Task")); th(I18n.tr("Ocena", "Grade"))
                                            th(I18n.tr("Feedback", "Feedback")); th(I18n.tr("Akcja", "Action"))
                                        } }
                                        tbody {
                                            list.forEach { s ->
                                                val subId = s.id?.toString()?.toIntOrNull() ?: 0
                                                val videoUrl = s.videoUrl?.toString() ?: ""
                                                val taskId = s.taskId?.toString()?.toIntOrNull() ?: 0
                                                val taskObj = tasks.find { it.id?.toString()?.toIntOrNull() == taskId }
                                                val instrUrl = taskObj?.instructionVideoUrl?.toString() ?: ""

                                                val isRestored = s.status?.toString() == "RESTORED"

                                                tr {
                                                    td(I18n.tr("Wideo #", "Video #") + "${s.id}")
                                                    td(I18n.tr("Tancerz #", "Dancer #") + "${s.dancerId}")
                                                    td(taskObj?.title?.toString() ?: (I18n.tr("Zadanie #", "Task #") + "$taskId"))

                                                    // Kolumna Ocena
                                                    td {
                                                        if (isRestored) span(I18n.tr("Wycofano", "Withdrawn"), className = "badge bg-secondary text-light")
                                                        else span(s.score?.toString() ?: "—", className = "badge bg-success fs-6")
                                                    }

                                                    // Kolumna Feedback
                                                    td {
                                                        if (isRestored) span("—", className = "text-muted")
                                                        else span(s.feedback?.toString()?.take(40)?.let { if (it.length == 40) "$it..." else it } ?: "—")
                                                    }

                                                    // Kolumna Akcja
                                                    td {
                                                        if (isRestored) {
                                                            span(I18n.tr("⏳ Przywrócono do kolejki", "⏳ Restored to queue"), className = "badge border border-warning text-warning p-2")
                                                        } else {
                                                            div(className = "d-flex gap-2") {
                                                                tag(TAG.BUTTON, I18n.tr("Zobacz", "View"), className = "btn btn-sm dance-btn-primary") {
                                                                    onClick {
                                                                        PlayerState.submissionId = subId
                                                                        PlayerState.submissionVideoUrl = toVideoUrl(videoUrl)
                                                                        PlayerState.instructionVideoUrl = toVideoUrl(instrUrl)
                                                                        PlayerState.taskTitle = taskObj?.title?.toString() ?: (I18n.tr("Zadanie #", "Task #") + "$taskId")
                                                                        PlayerState.taskId = taskId
                                                                        PlayerState.isGraded = true
                                                                        PlayerState.currentScore = s.score?.toString()?.toIntOrNull()
                                                                        PlayerState.currentFeedback = s.feedback?.toString()
                                                                        appState.value = Page.PLAYER
                                                                    }
                                                                }
                                                                tag(TAG.BUTTON, I18n.tr("Przywróć do oceny", "Restore for grading"), className = "btn btn-sm btn-outline-warning") {
                                                                    onClick {
                                                                        if (window.confirm(I18n.tr("Czy chcesz przywrócić to nagranie do kolejki? Będziesz mógł ponownie dodać/edytować komentarze.", "Do you want to restore this to the queue? You'll be able to re-edit comments."))) {
                                                                            ApiService.resetSubmissionStatus(subId).then<dynamic> {
                                                                                showToast(I18n.tr("✔ Nagranie wróciło do kolejki!", "✔ Recording returned to queue!"))

                                                                                val restoredIds = window.localStorage.getItem("restored_subs")?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
                                                                                if (!restoredIds.contains(subId.toString())) {
                                                                                    restoredIds.add(subId.toString())
                                                                                    window.localStorage.setItem("restored_subs", restoredIds.joinToString(","))
                                                                                }

                                                                                val index = subs.indexOf(s)
                                                                                if (index >= 0) {
                                                                                    subs.removeAt(index)
                                                                                    s.status = "RESTORED"
                                                                                    subs.add(index, s)
                                                                                }

                                                                                null
                                                                            }.catch<dynamic> { _: Throwable -> window.alert(I18n.tr("Błąd połączenia z serwerem.", "Server connection error.")); null }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ZMODYFIKOWANY PANEL ADMINA
    // ==========================================
    private fun Container.buildAdminPanel() {
        div(className = "container py-5 mt-5") {
            h2(I18n.tr("Panel Admina", "Admin Panel"), className = "fw-bold text-primary-dance mb-4 pt-4")
            div(className = "row g-4 justify-content-center") {
                dashboardCard("fa-users-cog", I18n.tr("Zarządzanie Użytkownikami", "User Management"), I18n.tr("Zarządzaj kontami, zmieniaj role, blokuj i usuwaj.", "Manage accounts, change roles, block and delete.")) { appState.value = Page.ADMIN_USERS }
                dashboardCard("fa-shield", I18n.tr("Globalna Moderacja", "Global Moderation"), I18n.tr("Wgląd w postępy tancerzy i edycja komentarzy.", "Insight into dancers' progress and comment editing.")) { appState.value = Page.ADMIN_MODERATION }
                dashboardCard("fa-list-check", I18n.tr("Zarządzanie Zadaniami", "Task Management"), I18n.tr("Edycja i usuwanie wyzwań.", "Editing and deleting challenges.")) { appState.value = Page.ADMIN_TASKS }
                dashboardCard("fa-bullhorn", I18n.tr("Zarządzanie Ogłoszeniami", "Announcements Management"), I18n.tr("Tworzenie nowych wpisów dla wszystkich.", "Creating new posts for everyone.")) { appState.value = Page.ADMIN_ANNOUNCEMENTS }
                dashboardCard("fa-comments", I18n.tr("Wiadomości", "Messages"), I18n.tr("Messenger społeczności.", "Community messenger.")) { appState.value = Page.CHAT }
            }
        }
    }

    private fun Container.buildAdminVerify() {
        appState.value = Page.ADMIN_USERS
    }

    // --- FUNKCJA POMOCNICZA: OKIENKO DO ZMIANY ROLI ---
    private fun showChangeRoleModal(userId: Int, currentRole: String, onSuccess: () -> Unit) {
        val modal = Modal(I18n.tr("Zmień rolę", "Change Role"), closeButton = true, animation = true)
        modal.div(className = "p-3") {
            p(I18n.tr("Wybierz nową rolę dla tego użytkownika:", "Select a new role for this user:"), className = "text-light")
            val roleSelect = select(
                options = listOf("DANCER" to "DANCER", "CHOREOGRAPHER" to "CHOREOGRAPHER", "ADMIN" to "ADMIN"),
                value = currentRole
            ) {
                addCssClass("form-select")
                addCssClass("form-select-lg")
                addCssClass("mb-4")
            }
            tag(TAG.BUTTON, I18n.tr("Zapisz", "Save"), className = "btn dance-btn-primary w-100 fw-bold") {
                onClick {
                    val newRole = roleSelect.value ?: currentRole
                    if (newRole != currentRole) {
                        ApiService.updateUserRole(userId, newRole).then<dynamic> {
                            showToast(I18n.tr("✔ Rola zmieniona na ", "✔ Role changed to ") + newRole)
                            modal.hide()
                            onSuccess()
                            null
                        }.catch<dynamic> { _: Throwable ->
                            window.alert(I18n.tr("Błąd połączenia z serwerem.", "Server connection error."))
                            null
                        }
                    } else {
                        modal.hide()
                    }
                }
            }
        }
        modal.show()
    }

    // --- 1. ZARZĄDZANIE UŻYTKOWNIKAMI ---
    private fun Container.buildAdminUsers() {
        val usersList = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)

        fun loadUsers() {
            loading.value = true
            usersList.clear()
            ApiService.fetchUsers().then<dynamic> { res: dynamic ->
                usersList.addAll(res as Array<dynamic>)
                loading.value = false
                null
            }.catch<dynamic> { _: Throwable -> loading.value = false; null }
        }

        loadUsers()

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.ADMIN_PANEL)
            h2(I18n.tr("Zarządzanie Użytkownikami", "User Management"), className = "mb-4")
            div {
                bind(loading) { isLoading ->
                    if (isLoading) {
                        div(className = "text-center py-5") { tag(TAG.DIV, className = "spinner-border text-danger") { setAttribute("role", "status") } }
                    } else {
                        div {
                            bind(usersList) { list ->
                                if (list.isEmpty()) { p(I18n.tr("Brak użytkowników w systemie.", "No users in system."), className = "text-muted") }
                                else {
                                    table(className = "table table-dark table-hover align-middle") {
                                        thead { tr {
                                            th("ID"); th(I18n.tr("Imię i Nazwisko", "Name and Surname"))
                                            th("Email"); th(I18n.tr("Rola", "Role"))
                                            th(I18n.tr("Status", "Status")); th(I18n.tr("Akcje", "Actions"))
                                        } }
                                        tbody {
                                            list.forEach { u ->
                                                tr {
                                                    val userId = u.id?.toString()?.toIntOrNull() ?: 0
                                                    val active = u.isActive == true || u.active == true || u.isActive?.toString() == "true" || u.active?.toString() == "true"
                                                    val role = u.role?.toString() ?: "—"

                                                    td(u.id?.toString() ?: "—")
                                                    td("${u.firstName?.toString() ?: ""} ${u.lastName?.toString() ?: ""}")
                                                    td(u.email?.toString() ?: "—")
                                                    td(role)
                                                    td {
                                                        span(if (active) I18n.tr("Aktywny", "Active") else I18n.tr("Zablokowany", "Blocked"), className = if (active) "badge bg-success" else "badge bg-danger")
                                                    }
                                                    td {
                                                        div(className = "d-flex gap-2") {
                                                            tag(TAG.BUTTON, I18n.tr("Zmień Rolę", "Change Role"), className = "btn btn-sm btn-outline-info") {
                                                                onClick { showChangeRoleModal(userId, role) { loadUsers() } }
                                                            }
                                                            tag(TAG.BUTTON, if (active) I18n.tr("Zablokuj", "Block") else I18n.tr("Odblokuj", "Unblock"),
                                                                className = if (active) "btn btn-sm btn-outline-warning" else "btn btn-sm btn-outline-success") {
                                                                onClick {
                                                                    ApiService.setUserStatus(userId, !active).then<dynamic> {
                                                                        showToast(I18n.tr("✔ Status zmieniony.", "✔ Status changed."))
                                                                        val index = list.indexOf(u)
                                                                        if (index >= 0) {
                                                                            usersList.removeAt(index)
                                                                            u.isActive = !active
                                                                            u.active = !active
                                                                            usersList.add(index, u)
                                                                        }
                                                                        null
                                                                    }.catch<dynamic> { _: Throwable -> window.alert(I18n.tr("Błąd połączenia z serwerem.", "Server connection error.")); null }
                                                                }
                                                            }
                                                            tag(TAG.BUTTON, I18n.tr("Usuń", "Delete"), className = "btn btn-sm btn-danger") {
                                                                onClick {
                                                                    if (window.confirm(I18n.tr("Ostrzeżenie: Trwale usunąć tego użytkownika?", "Warning: Permanently delete this user?"))) {
                                                                        ApiService.deleteUser(userId).then<dynamic> {
                                                                            showToast(I18n.tr("✔ Użytkownik trwale usunięty.", "✔ User permanently deleted."))
                                                                            loadUsers()
                                                                            null
                                                                        }.catch<dynamic> { _: Throwable -> window.alert(I18n.tr("Błąd połączenia z serwerem.", "Server connection error.")); null }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 2. ZARZĄDZANIE ZADANIAMI ---
    private fun Container.buildAdminTasks() {
        val tasksList = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)

        fun loadTasks() {
            loading.value = true
            tasksList.clear()
            ApiService.fetchTasks().then<dynamic> { res: dynamic ->
                tasksList.addAll(res as Array<dynamic>)
                loading.value = false
                null
            }.catch<dynamic> { _: Throwable -> loading.value = false; null }
        }

        loadTasks()

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.ADMIN_PANEL)
            h2(I18n.tr("Zarządzanie Zadaniami", "Task Management"), className = "fw-bold mb-4")

            div {
                bind(loading) { isLoading ->
                    if (isLoading) {
                        div(className = "text-center py-5") { tag(TAG.DIV, className = "spinner-border text-danger") }
                    } else {
                        bind(tasksList) { list ->
                            if (list.isEmpty()) { div(className = "card bg-dark border-secondary p-5 text-center") { p(I18n.tr("Brak zadań w systemie.", "No tasks in the system."), className = "text-muted") } }
                            else {
                                table(className = "table table-dark table-hover align-middle") {
                                    thead { tr {
                                        th("ID"); th(I18n.tr("Tytuł", "Title"))
                                        th(I18n.tr("Termin", "Deadline")); th(I18n.tr("Wideo", "Video")); th(I18n.tr("Akcja", "Action"))
                                    } }
                                    tbody {
                                        list.forEach { task ->
                                            val taskId = task.id?.toString()?.toIntOrNull() ?: 0
                                            val title = task.title?.toString() ?: ""
                                            val desc = task.description?.toString() ?: ""
                                            val deadlineRaw = task.deadline?.toString() ?: ""
                                            val videoUrl = task.instructionVideoUrl?.toString()

                                            tr {
                                                td(taskId.toString())
                                                td(title)
                                                td(deadlineRaw.take(16).replace("T", " "))
                                                td {
                                                    if (!videoUrl.isNullOrBlank()) {
                                                        link(I18n.tr("Zobacz", "View"), url = toVideoUrl(videoUrl), className = "text-info") {
                                                            setAttribute("target", "_blank")
                                                        }
                                                    } else { span("Brak", className = "text-muted") }
                                                }
                                                td {
                                                    div(className = "d-flex gap-2") {
                                                        tag(TAG.BUTTON, I18n.tr("Edytuj", "Edit"), className = "btn btn-sm btn-outline-info") {
                                                            onClick { showAdminEditTaskModal(taskId, title, desc, deadlineRaw) { loadTasks() } }
                                                        }
                                                        tag(TAG.BUTTON, I18n.tr("Usuń", "Delete"), className = "btn btn-sm btn-danger") {
                                                            onClick {
                                                                if (window.confirm(I18n.tr("Usunąć to zadanie? Usunie to wszystkie powiązane z nim nagrania!", "Delete task? This removes all related recordings!"))) {
                                                                    ApiService.deleteTaskAPI(taskId).then<dynamic> {
                                                                        showToast(I18n.tr("✔ Zadanie usunięte.", "✔ Task deleted."))
                                                                        loadTasks()
                                                                        null
                                                                    }.catch<dynamic> { _: Throwable -> window.alert(I18n.tr("Błąd połączenia z serwerem.", "Server connection error.")); null }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showAdminEditTaskModal(taskId: Int, currentTitle: String, currentDesc: String, currentDeadline: String, onSuccess: () -> Unit) {
        val modal = Modal(I18n.tr("Edytuj Zadanie #", "Edit Task #") + "$taskId", closeButton = true, animation = true)

        modal.div(className = "p-3") {
            label(I18n.tr("Tytuł", "Title"), className = "form-label fw-bold mb-1")
            val titleInput = textInput(type = io.kvision.html.InputType.TEXT, className = "form-control mb-3") {
                value = currentTitle
                placeholder = I18n.tr("Wpisz tytuł...", "Enter title...")
            }

            label(I18n.tr("Opis", "Description"), className = "form-label fw-bold mb-1")
            val descInput = textInput(type = io.kvision.html.InputType.TEXT, className = "form-control mb-3") {
                value = currentDesc
                placeholder = I18n.tr("Wpisz opis...", "Enter description...")
            }

            label(I18n.tr("Termin (np. 01.01.2027 12:00)", "Deadline (e.g. 01.01.2027 12:00)"), className = "form-label fw-bold mb-1")
            val deadlineInput = textInput(type = io.kvision.html.InputType.TEXT, className = "form-control mb-4") {
                value = currentDeadline.take(16).replace("T", " ")
                placeholder = "01.01.2027 12:00"
            }

            tag(TAG.BUTTON, I18n.tr("Zapisz Zmiany", "Save Changes"), className = "btn dance-btn-primary w-100") {
                onClick {
                    val newTitle = titleInput.value ?: ""
                    val newDesc = descInput.value ?: ""
                    val newDeadline = deadlineInput.value ?: ""

                    ApiService.updateTask(taskId, newTitle, newDesc, newDeadline).then<dynamic> {
                        showToast(I18n.tr("✔ Zaktualizowano zadanie.", "✔ Task updated."))
                        modal.hide()
                        onSuccess()
                        null
                    }.catch<dynamic> { _: Throwable -> window.alert(I18n.tr("Błąd połączenia z serwerem.", "Server connection error.")); null }
                }
            }
        }
        modal.show()
    }

    // --- 3. GLOBALNA MODERACJA ---
    private fun Container.buildAdminModeration() {
        val dancersList = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)

        ApiService.fetchUsers().then<dynamic> { res: dynamic ->
            val allUsers = res as Array<dynamic>
            dancersList.addAll(allUsers.filter { it.role?.toString() == "DANCER" })
            loading.value = false
            null
        }.catch<dynamic> { _: Throwable -> loading.value = false; null }

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.ADMIN_PANEL)
            h2(I18n.tr("Wybierz Tancerza do moderacji", "Select Dancer for moderation"), className = "fw-bold mb-4")
            div {
                bind(loading) { isLoading ->
                    if (isLoading) {
                        div(className = "text-center py-5") { tag(TAG.DIV, className = "spinner-border text-danger") }
                    } else {
                        bind(dancersList) { list ->
                            if (list.isEmpty()) { div(className = "card bg-dark border-secondary p-5 text-center") { p(I18n.tr("Brak tancerzy w systemie.", "No dancers in system."), className = "text-muted") } }
                            else {
                                table(className = "table table-dark table-hover align-middle") {
                                    thead { tr { th("ID"); th(I18n.tr("Imię i Nazwisko", "Name and Surname")); th("Email"); th(I18n.tr("Akcja", "Action")) } }
                                    tbody {
                                        list.forEach { u ->
                                            val uId = u.id?.toString()?.toIntOrNull() ?: 0
                                            val uName = "${u.firstName?.toString() ?: ""} ${u.lastName?.toString() ?: ""}"
                                            tr {
                                                td(uId.toString())
                                                td(uName)
                                                td(u.email?.toString() ?: "")
                                                td {
                                                    tag(TAG.BUTTON, I18n.tr("Zobacz Postęp & Moderuj", "View Progress & Moderate"), className = "btn btn-sm dance-btn-primary") {
                                                        onClick {
                                                            AdminState.selectedUserId = uId
                                                            AdminState.selectedUserName = uName
                                                            appState.value = Page.ADMIN_USER_DETAILS
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Container.buildAdminUserDetails() {
        val targetId = AdminState.selectedUserId
        val subs = io.kvision.state.ObservableListWrapper<dynamic>()
        val tasks = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)

        ApiService.fetchTasks().then<dynamic> { resTasks: dynamic ->
            val taskList = resTasks as Array<dynamic>
            tasks.addAll(taskList)
            val taskIds = taskList.mapNotNull { it.id?.toString()?.toIntOrNull() }

            if (taskIds.isEmpty()) {
                loading.value = false
                return@then null
            }

            fun fetchNext(index: Int) {
                if (index >= taskIds.size) { loading.value = false; return }
                ApiService.fetchSubmissionsForTask(taskIds[index]).then<dynamic> { r: dynamic ->
                    val listForDancer = (r as Array<dynamic>).filter { it.dancerId?.toString() == targetId.toString() }
                    subs.addAll(listForDancer)
                    fetchNext(index + 1)
                    null
                }.catch<dynamic> { _: Throwable -> fetchNext(index + 1); null }
            }
            fetchNext(0)
            null
        }.catch<dynamic> { _: Throwable -> loading.value = false; null }

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.ADMIN_MODERATION)
            h2(I18n.tr("Moderacja Tancerza:", "Dancer Moderation:") + " ${AdminState.selectedUserName}", className = "fw-bold mb-4 text-primary-dance")

            div {
                bind(loading) { isLoading ->
                    if (isLoading) {
                        div(className = "text-center py-5") { tag(TAG.DIV, className = "spinner-border text-danger") }
                    } else {
                        bind(subs) { subList ->
                            val completedCount = subList.size
                            val totalAvailableTasks = tasks.size
                            val gradedList = subList.filter { it.status?.toString() == "GRADED" }
                            val pendingList = subList.filter { it.status?.toString() != "GRADED" }

                            div(className = "row g-3 mb-4") {
                                div(className = "col-md-4") {
                                    div(className = "card bg-dark border-secondary p-3 text-center") {
                                        h3(completedCount.toString(), className = "fw-bold text-white mb-2")
                                        p(I18n.tr("Wgranych Zadań", "Uploaded Tasks"), className = "text-light small fw-bold mb-0 text-uppercase")
                                    }
                                }
                                div(className = "col-md-4") {
                                    div(className = "card bg-dark border-secondary p-3 text-center") {
                                        h3(gradedList.size.toString(), className = "fw-bold text-success mb-2")
                                        p(I18n.tr("Ocenionych Nagrań", "Graded Recordings"), className = "text-light small fw-bold mb-0 text-uppercase")
                                    }
                                }
                                div(className = "col-md-4") {
                                    div(className = "card bg-dark border-secondary p-3 text-center") {
                                        h3((totalAvailableTasks - completedCount).toString(), className = "fw-bold text-warning mb-2")
                                        p(I18n.tr("Niewykonanych Zadań", "Pending Tasks"), className = "text-light small fw-bold mb-0 text-uppercase")
                                    }
                                }
                            }

                            div(className = "row") {
                                div(className = "col-md-6") {
                                    h4(I18n.tr("Ocenione Nagrania (Komentarze)", "Graded Recordings (Comments)"), className = "text-success mb-3")
                                    if (gradedList.isEmpty()) { p(I18n.tr("Brak ocenionych nagrań.", "No graded recordings."), className = "text-muted") }
                                    else {
                                        ul(className = "list-group bg-dark shadow-sm") {
                                            gradedList.forEach { s ->
                                                val subId = s.id?.toString()?.toIntOrNull() ?: 0
                                                val tId = s.taskId?.toString()?.toIntOrNull() ?: 0
                                                val taskObj = tasks.find { it.id?.toString()?.toIntOrNull() == tId }

                                                li(className = "list-group-item bg-dark text-white border-secondary mb-2 p-3") {
                                                    div(className = "d-flex justify-content-between align-items-center") {
                                                        div {
                                                            h6(taskObj?.title?.toString() ?: (I18n.tr("Zadanie #", "Task #") + "$tId"), className = "fw-bold mb-1")
                                                            span(I18n.tr("Ocena: ", "Grade: ") + "${s.score?.toString() ?: "—"}/10", className = "badge bg-success")
                                                        }
                                                        div(className = "d-flex flex-column flex-xl-row gap-2") {
                                                            tag(TAG.BUTTON, I18n.tr("Odtwórz wideo", "Play video"), className = "btn btn-sm btn-outline-success") {
                                                                onClick {
                                                                    PlayerState.submissionId = subId
                                                                    PlayerState.submissionVideoUrl = toVideoUrl(s.videoUrl?.toString())
                                                                    PlayerState.instructionVideoUrl = toVideoUrl(taskObj?.instructionVideoUrl?.toString())
                                                                    PlayerState.taskTitle = taskObj?.title?.toString() ?: (I18n.tr("Zadanie #", "Task #") + "$tId")
                                                                    PlayerState.taskId = tId
                                                                    PlayerState.isGraded = true
                                                                    PlayerState.currentScore = s.score?.toString()?.toIntOrNull()
                                                                    PlayerState.currentFeedback = s.feedback?.toString()
                                                                    appState.value = Page.PLAYER
                                                                }
                                                            }
                                                            tag(TAG.BUTTON, I18n.tr("Komentarze", "Comments"), className = "btn btn-sm btn-outline-info") {
                                                                onClick { showAdminCommentsModerationModal(subId) }
                                                            }
                                                            tag(TAG.BUTTON, I18n.tr("Usuń Wideo", "Delete Video"), className = "btn btn-sm btn-outline-danger") {
                                                                onClick {
                                                                    if (window.confirm(I18n.tr("Usunąć to nagranie?", "Delete this recording?"))) {
                                                                        ApiService.deleteSubmissionAPI(subId).then<dynamic> {
                                                                            showToast(I18n.tr("✔ Usunięto.", "✔ Deleted."))
                                                                            subs.removeAll { (it.id as? Int) == subId }
                                                                            null
                                                                        }.catch<dynamic> { _: Throwable -> window.alert(I18n.tr("Błąd połączenia z serwerem.", "Server connection error.")); null }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                div(className = "col-md-6") {
                                    h4(I18n.tr("Oczekujące na ocenę", "Awaiting grading"), className = "text-warning mb-3 mt-4 mt-md-0")
                                    if (pendingList.isEmpty()) { p(I18n.tr("Brak oczekujących nagrań.", "No pending recordings."), className = "text-muted") }
                                    else {
                                        ul(className = "list-group bg-dark shadow-sm") {
                                            pendingList.forEach { s ->
                                                val tId = s.taskId?.toString()?.toIntOrNull() ?: 0
                                                val taskObj = tasks.find { it.id?.toString()?.toIntOrNull() == tId }
                                                val subId = s.id?.toString()?.toIntOrNull() ?: 0

                                                li(className = "list-group-item bg-dark text-white border-secondary mb-2 p-3") {
                                                    div(className = "d-flex justify-content-between align-items-center") {
                                                        div {
                                                            h6(taskObj?.title?.toString() ?: (I18n.tr("Zadanie #", "Task #") + "$tId"), className = "fw-bold mb-1")
                                                            span(I18n.tr("Czeka na choreografa", "Waiting for choreographer"), className = "text-muted small")
                                                        }
                                                        div(className = "d-flex gap-2") {
                                                            tag(TAG.BUTTON, I18n.tr("Odtwórz wideo", "Play video"), className = "btn btn-sm btn-outline-warning") {
                                                                onClick {
                                                                    PlayerState.submissionId = subId
                                                                    PlayerState.submissionVideoUrl = toVideoUrl(s.videoUrl?.toString())
                                                                    PlayerState.instructionVideoUrl = toVideoUrl(taskObj?.instructionVideoUrl?.toString())
                                                                    PlayerState.taskTitle = taskObj?.title?.toString() ?: I18n.tr("Zadanie", "Task")
                                                                    PlayerState.taskId = tId
                                                                    PlayerState.isGraded = false
                                                                    PlayerState.currentScore = null
                                                                    PlayerState.currentFeedback = null
                                                                    appState.value = Page.PLAYER
                                                                }
                                                            }
                                                            tag(TAG.BUTTON, I18n.tr("Usuń Wideo", "Delete Video"), className = "btn btn-sm btn-outline-danger") {
                                                                onClick {
                                                                    if (window.confirm(I18n.tr("Usunąć to nagranie?", "Delete this recording?"))) {
                                                                        ApiService.deleteSubmissionAPI(subId).then<dynamic> {
                                                                            showToast(I18n.tr("✔ Usunięto.", "✔ Deleted."))
                                                                            subs.removeAll { (it.id as? Int) == subId }
                                                                            null
                                                                        }.catch<dynamic> { _: Throwable -> window.alert(I18n.tr("Błąd połączenia z serwerem.", "Server connection error.")); null }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showAdminCommentsModerationModal(submissionId: Int) {
        val modal = Modal(I18n.tr("Moderacja Komentarzy (Nagranie #", "Comment Moderation (Recording #") + "$submissionId)", closeButton = true, animation = true)
        val commentsList = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)

        fun loadComments() {
            loading.value = true
            commentsList.clear()
            ApiService.fetchComments(submissionId).then<dynamic> { res: dynamic ->
                commentsList.addAll(res as Array<dynamic>)
                loading.value = false
                null
            }.catch<dynamic> { _: Throwable -> loading.value = false; null }
        }

        loadComments()

        modal.div(className = "p-3") {
            bind(loading) { isLoading ->
                if (isLoading) { div(className = "text-center") { tag(TAG.DIV, className = "spinner-border text-info") } }
                else {
                    bind(commentsList) { list ->
                        if (list.isEmpty()) { p(I18n.tr("Brak komentarzy.", "No comments."), className = "text-muted") }
                        else {
                            list.sortedBy { it.timestampSeconds?.toString()?.toIntOrNull() ?: 0 }.forEach { c ->
                                val commentId = c.id?.toString()?.toIntOrNull() ?: 0
                                val sec = c.timestampSeconds?.toString()?.toIntOrNull() ?: 0
                                val originalContent = c.content?.toString() ?: ""

                                val isEditing = ObservableValue(false)

                                div(className = "card bg-dark border-secondary mb-2 p-3") {
                                    bind(isEditing) { editing ->
                                        if (editing) {
                                            val editInput = textInput(type = io.kvision.html.InputType.TEXT, className = "form-control mb-2") {
                                                value = originalContent
                                                placeholder = I18n.tr("Treść komentarza...", "Comment content...")
                                            }
                                            div(className = "d-flex gap-2") {
                                                tag(TAG.BUTTON, I18n.tr("Zapisz", "Save"), className = "btn btn-sm btn-success") {
                                                    onClick {
                                                        val newVal = editInput.value ?: ""
                                                        ApiService.updateComment(commentId, newVal).then<dynamic> {
                                                            showToast(I18n.tr("✔ Komentarz zaktualizowany.", "✔ Comment updated."))
                                                            loadComments()
                                                            null
                                                        }.catch<dynamic> { _: Throwable -> window.alert(I18n.tr("Błąd połączenia z serwerem.", "Server connection error.")); null }
                                                    }
                                                }
                                                tag(TAG.BUTTON, I18n.tr("Anuluj", "Cancel"), className = "btn btn-sm btn-secondary") {
                                                    onClick { isEditing.value = false }
                                                }
                                            }
                                        } else {
                                            div(className = "d-flex justify-content-between align-items-start") {
                                                div {
                                                    span(formatTime(sec), className = "badge bg-primary-dance me-2 font-monospace") {
                                                        setStyle("cursor", "pointer")
                                                        title = I18n.tr("Kliknij, aby przejść do tej sekundy", "Click to jump to this second")
                                                        onClick {
                                                            val v1 = document.getElementById("submission-player") as? HTMLVideoElement
                                                            val v2 = document.getElementById("instruction-player") as? HTMLVideoElement

                                                            v1?.currentTime = sec.toDouble()
                                                            v2?.currentTime = sec.toDouble()

                                                            v1?.play()
                                                            v2?.play()
                                                        }
                                                    }
                                                    small(I18n.tr("Choreograf", "Choreographer"), className = "text-muted")
                                                }
                                                // KOSZ TYLKO DLA CHOREOGRAFA I TYLKO KIEDY WŁAŚNIE OCENIA (!isGraded)
                                                if (role == "CHOREOGRAPHER" && !PlayerState.isGraded && commentId != null) {
                                                    tag(TAG.BUTTON, className = "btn btn-sm btn-outline-danger border-0 ms-2") {
                                                        tag(TAG.I, className = "fa-solid fa-trash")
                                                        setAttribute("title", I18n.tr("Usuń komentarz", "Delete comment"))
                                                        onClick {
                                                            if (window.confirm(I18n.tr("Czy na pewno chcesz usunąć ten komentarz?", "Are you sure you want to delete this comment?"))) {
                                                                ApiService.deleteCommentAPI(commentId).then<dynamic> { _: dynamic ->
                                                                    commentsList.removeAll { item: dynamic -> (item.id as? Int) == commentId }
                                                                    showToast(I18n.tr("✔ Komentarz usunięty", "✔ Comment deleted"))
                                                                    null
                                                                }.catch<dynamic> { _: Throwable ->
                                                                    window.alert(I18n.tr("Błąd usuwania komentarza.", "Error deleting comment."))
                                                                    null
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            p(c.content?.toString() ?: "", className = "mb-0 text-light small")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        modal.show()
    }

    // --- 4. ZARZĄDZANIE OGŁOSZENIAMI (ADMIN) ---
    private fun Container.buildAdminAnnouncements() {
        fun loadAnnouncements() {
            ApiService.fetchAnnouncements().then<dynamic> { res ->
                DataManager.globalAnnouncements.clear()
                DataManager.globalAnnouncements.addAll(res as Array<dynamic>)
                null
            }.catch<dynamic> { _: Throwable -> null }
        }

        loadAnnouncements()

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.ADMIN_PANEL)
            h2(I18n.tr("Zarządzanie Ogłoszeniami", "Announcements Management"), className = "fw-bold mb-4")

            div(className = "row") {
                div(className = "col-md-5 mb-4") {
                    div(className = "card bg-dark border-secondary p-4") {
                        h4(I18n.tr("Nowe ogłoszenie", "New announcement"), className = "text-primary-dance mb-3")

                        label(I18n.tr("Tytuł", "Title"), className = "fw-bold mb-1")
                        val tInput = textInput(className = "form-control mb-3") {
                            placeholder = I18n.tr("Wpisz tytuł...", "Enter title...")
                        }

                        label(I18n.tr("Treść", "Content"), className = "fw-bold mb-1")
                        val cInput = textArea {
                            addCssClass("form-control")
                            addCssClass("mb-3")
                            setAttribute("rows", "4")
                            setAttribute("placeholder", I18n.tr("Wpisz treść ogłoszenia...", "Enter announcement content..."))
                        }

                        label(I18n.tr("Typ (np. Konkurs, Praca, Wydarzenie)", "Type (e.g. Contest, Job, Event)"), className = "fw-bold mb-1")
                        val typInput = textInput(className = "form-control mb-3") {
                            value = "Info"
                            placeholder = I18n.tr("Wpisz typ...", "Enter type...")
                        }

                        label(I18n.tr("Zdjęcie / Film (Opcjonalnie):", "Photo / Video (Optional):"), className = "fw-bold small mb-1")
                        val fileInput = tag(TAG.INPUT, className = "form-control mb-4") {
                            setAttribute("type", "file")
                            setAttribute("accept", "image/*,video/*")
                        }

                        tag(TAG.BUTTON, I18n.tr("Dodaj ogłoszenie", "Add announcement"), className = "btn dance-btn-primary w-100") {
                            onClick {
                                val title = tInput.value ?: ""
                                val content = cInput.value ?: ""
                                val type = typInput.value ?: "Info"
                                val files = fileInput.getElement()?.asDynamic().files
                                val file = if (files != null && files.length > 0) files[0] else null

                                if (title.isNotBlank() && content.isNotBlank()) {
                                    ApiService.createAnnouncement(title, content, type, file).then<dynamic> {
                                        showToast(I18n.tr("✔ Ogłoszenie dodane.", "✔ Announcement added."))
                                        tInput.value = ""
                                        cInput.value = ""
                                        typInput.value = "Info"
                                        fileInput.getElement()?.asDynamic().value = ""
                                        loadAnnouncements()
                                        null
                                    }.catch<dynamic> { _: Throwable -> window.alert("Błąd serwera.") ; null }
                                }
                            }
                        }
                    }
                }

                div(className = "col-md-7") {
                    h4(I18n.tr("Aktywne ogłoszenia", "Active announcements"), className = "mb-3")
                    bind(DataManager.globalAnnouncements) { list ->
                        if (list.isEmpty()) p(I18n.tr("Brak ogłoszeń.", "No announcements."), className = "text-center text-muted w-100")
                        else {
                            ul(className = "list-group") {
                                list.forEach { ann ->
                                    val annId = ann.id?.toString()?.toIntOrNull() ?: 0
                                    li(className = "list-group-item bg-dark border-secondary mb-2") {
                                        div(className = "d-flex justify-content-between align-items-center") {
                                            div {
                                                span(ann.type?.toString() ?: "Info", className = "badge bg-info text-dark me-2")
                                                strong(ann.title?.toString() ?: "", className = "text-light")
                                                p(ann.content?.toString() ?: "", className = "text-muted small mb-0 mt-1")
                                            }
                                            div(className = "d-flex gap-2 ms-3") {
                                                tag(TAG.BUTTON, className = "btn btn-sm btn-outline-info") {
                                                    tag(TAG.I, className = "fa-solid fa-pen")
                                                    onClick { showEditAnnouncementModal(ann) { loadAnnouncements() } }
                                                }
                                                tag(TAG.BUTTON, className = "btn btn-sm btn-outline-danger") {
                                                    tag(TAG.I, className = "fa-solid fa-trash")
                                                    onClick {
                                                        if(window.confirm("Usunąć to ogłoszenie?")) {
                                                            ApiService.deleteAnnouncement(annId).then<dynamic> {
                                                                loadAnnouncements()
                                                                null
                                                            }.catch<dynamic> { _: Throwable -> null }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showEditAnnouncementModal(ann: dynamic, onSuccess: () -> Unit) {
        val modal = Modal(I18n.tr("Edytuj Ogłoszenie", "Edit Announcement"), closeButton = true, animation = true)
        modal.div(className = "p-3") {
            val tInput = textInput(className = "form-control mb-3") {
                value = ann.title?.toString()
                placeholder = I18n.tr("Wpisz tytuł...", "Enter title...")
            }
            val cInput = textArea {
                addCssClass("form-control")
                addCssClass("mb-3")
                setAttribute("rows", "4")
                setAttribute("placeholder", I18n.tr("Wpisz treść ogłoszenia...", "Enter announcement content..."))
                value = ann.content?.toString()
            }
            val typInput = textInput(className = "form-control mb-4") {
                value = ann.type?.toString() ?: "Info"
                placeholder = I18n.tr("Wpisz typ...", "Enter type...")
            }

            tag(TAG.BUTTON, I18n.tr("Zapisz", "Save"), className = "btn dance-btn-primary w-100") {
                onClick {
                    ApiService.updateAnnouncement(ann.id.toString().toInt(), tInput.value ?: "", cInput.value ?: "", typInput.value ?: "").then<dynamic> {
                        showToast(I18n.tr("✔ Ogłoszenie zaktualizowane.", "✔ Announcement updated."))
                        modal.hide()
                        onSuccess()
                        null
                    }.catch<dynamic> { _: Throwable -> window.alert("Błąd połączenia.") ; null }
                }
            }
        }
        modal.show()
    }

    // --- FUNKCJA POMOCNICZA: OKIENKO DO TWORZENIA GRUPY ---
    private fun showCreateGroupModal(users: List<dynamic>, selectedRecipients: ObservableValue<List<Int>?>) {
        val modal = Modal("Nowa Wiadomość Grupowa", closeButton = true, animation = true)
        val selectedIds = mutableSetOf<Int>()

        modal.div(className = "p-3") {
            p("Wybierz uczestników, do których chcesz wysłać wiadomość:", className = "text-light mb-3")

            div(className = "bg-dark border border-secondary p-2 rounded mb-4") {
                setStyle("max-height", "250px")
                setStyle("overflow-y", "auto")

                users.forEach { u ->
                    val uid = u.id?.toString()?.toIntOrNull() ?: 0
                    val currentUserId = window.localStorage.getItem("userId")?.toIntOrNull() ?: -1
                    if (uid != currentUserId) {
                        div(className = "form-check mb-2") {
                            checkBox(label = "${u.firstName} ${u.lastName} (${u.role})") {
                                addCssClass("text-light")
                                onClick {
                                    if (value) selectedIds.add(uid) else selectedIds.remove(uid)
                                }
                            }
                        }
                    }
                }
            }

            tag(TAG.BUTTON, "Rozpocznij konwersację", className = "btn dance-btn-primary w-100 fw-bold") {
                onClick {
                    if (selectedIds.size >= 2) {
                        selectedRecipients.value = selectedIds.toList()
                        modal.hide()
                    } else {
                        window.alert("Wybierz co najmniej 2 osoby z listy, aby wysłać wiadomość grupową.")
                    }
                }
            }
        }
        modal.show()
    }

    // ==========================================
    // CZAT SPOŁECZNOŚCIOWY (Messenger Style z Edycją i Usuwaniem)
    // ==========================================
    private fun Container.buildChatView() {
        val messages = io.kvision.state.ObservableListWrapper<dynamic>()
        val selectedRecipients = ObservableValue<List<Int>?>(null)

        fun loadMessages(recipients: List<Int>?) {
            if (recipients != null && recipients.size > 1) {
                messages.clear()
                return
            }

            val recipientId = recipients?.firstOrNull()
            ApiService.fetchChat(recipientId).then<dynamic> { res ->
                messages.clear()
                messages.addAll(res as Array<dynamic>)
                null
            }.catch<dynamic> { _: Throwable -> null }
        }

        fun loadUsers() {
            ApiService.fetchUsers().then<dynamic> { res ->
                DataManager.allUsers.clear()
                DataManager.allUsers.addAll(res as Array<dynamic>)
                null
            }.catch<dynamic> { _: Throwable -> null }
        }

        loadUsers()

        val role = window.localStorage.getItem("userRole") ?: ""
        val backPage = when (role) {
            "ADMIN" -> Page.ADMIN_PANEL
            "CHOREOGRAPHER" -> Page.CHOREO_DASHBOARD
            "DANCER" -> Page.DANCER_DASHBOARD
            else -> Page.HOME
        }

        if (role == "ADMIN") {
            selectedRecipients.value = null
        } else {
            selectedRecipients.value = emptyList()
        }

        selectedRecipients.subscribe { loadMessages(it) }

        div(className = "container py-5 mt-5 pt-4 px-4") {
            backButton(appState, backPage)

            h2(I18n.tr("Messenger Społeczności", "Community Messenger"), className = "fw-bold text-primary-dance mb-4")

            div(className = "row g-3") {
                div(className = "col-md-4") {
                    div(className = "card bg-dark border-secondary p-3 h-100") {
                        h5(I18n.tr("Kontakty", "Contacts"), className = "text-white mb-3")
                        div(className = "contacts-list pe-2") {
                            height = 50.vh
                            setStyle("overflow-y", "auto")

                            bind(DataManager.allUsers) { list ->
                                bind(selectedRecipients) { sel ->

                                    if (role == "ADMIN") {
                                        val isSelected = sel == null
                                        div(className = "d-flex align-items-center justify-content-between mb-2 p-2 rounded hover-card " + if (isSelected) "bg-primary-dance" else "bg-black border border-secondary") {
                                            setStyle("cursor", "pointer")
                                            onClick { selectedRecipients.value = null }

                                            span("🌍 " + I18n.tr("Czat Ogólny", "General"), className = "fw-bold") {
                                                if (isSelected) setAttribute("style", "color: #000000 !important;")
                                                else setAttribute("style", "color: #0dcaf0 !important;")
                                            }
                                        }
                                    }

                                    if (sel != null && sel.size > 1) {
                                        div(className = "d-flex align-items-center justify-content-between mb-2 p-2 rounded bg-primary-dance") {
                                            span("👥 Nowa Wiadomość (${sel.size} os.)", className = "fw-bold text-dark") {
                                                setAttribute("style", "color: #000000 !important;")
                                            }
                                        }
                                    }

                                    if (list.isEmpty()) {
                                        p("Brak użytkowników...", className = "text-muted small")
                                    }

                                    list.forEach { u ->
                                        val uid = u.id?.toString()?.toIntOrNull() ?: 0
                                        val currentUserId = window.localStorage.getItem("userId")?.toIntOrNull() ?: -1

                                        if (uid != currentUserId) {
                                            val isSelected = sel != null && sel.size == 1 && sel.contains(uid)
                                            div(className = "d-flex align-items-center justify-content-between mb-2 p-2 rounded hover-card " + if (isSelected) "bg-primary-dance" else "bg-black border border-secondary") {
                                                setStyle("cursor", "pointer")
                                                onClick { selectedRecipients.value = listOf(uid) }

                                                span("${u.firstName} ${u.lastName}", className = "fw-bold") {
                                                    if (isSelected) setAttribute("style", "color: #000000 !important;")
                                                    else setAttribute("style", "color: #f5f5f5 !important;")
                                                }

                                                val uRole = u.role?.toString() ?: ""
                                                val badgeClass = when(uRole) { "ADMIN" -> "bg-danger"; "CHOREOGRAPHER" -> "bg-info text-dark"; else -> "bg-secondary" }
                                                span(uRole, className = "badge $badgeClass small")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (role == "ADMIN") {
                            tag(TAG.BUTTON, I18n.tr("Stwórz Grupę", "Create Group"), className = "btn btn-outline-info w-100 mt-3 btn-sm fw-bold") {
                                onClick { showCreateGroupModal(DataManager.allUsers.toList(), selectedRecipients) }
                            }
                        }
                    }
                }

                div(className = "col-md-8") {
                    div(className = "card bg-dark border-secondary p-3 h-100") {
                        div(className = "chat-header border-bottom border-secondary pb-2 mb-3") {
                            bind(selectedRecipients) { sel ->
                                val txt = if (sel == null) {
                                    I18n.tr("Do wszystkich (Czat Ogólny)", "To everyone (General)")
                                } else if (sel.size > 1) {
                                    I18n.tr("Wysyłanie do wybranych ", "Sending to ") + "${sel.size}" + I18n.tr(" osób", " people")
                                } else if (sel.size == 1) {
                                    val u = DataManager.allUsers.find { it.id?.toString()?.toIntOrNull() == sel.first() }
                                    I18n.tr("Rozmowa z: ", "Chat with: ") + "${u?.firstName} ${u?.lastName}"
                                } else {
                                    ""
                                }
                                span(txt, className = "text-info fw-bold")
                            }
                        }

                        div(className = "chat-box mb-3 pe-2") {
                            height = 40.vh
                            setStyle("overflow-y", "auto")
                            bind(messages) { list ->
                                bind(selectedRecipients) { sel ->
                                    val currentUserId = window.localStorage.getItem("userId")?.toIntOrNull() ?: -1

                                    if (sel != null && sel.isEmpty() && role != "ADMIN") {
                                        div(className = "d-flex h-100 justify-content-center align-items-center") {
                                            p(I18n.tr("Wybierz osobę z listy po lewej stronie, aby rozpocząć rozmowę.", "Select a person from the left to start chatting."), className = "text-muted text-center")
                                        }
                                    } else if (sel != null && sel.size > 1) {
                                        div(className = "d-flex h-100 justify-content-center align-items-center") {
                                            p(I18n.tr("Napisz wiadomość na dole. Zostanie ona wysłana do wszystkich zaznaczonych osób.", "Write a message below. It will be sent to all selected people."), className = "text-muted text-center")
                                        }
                                    } else if (list.isEmpty()) {
                                        p(I18n.tr("Brak wiadomości.", "No messages."), className = "text-muted text-center mt-3")
                                    } else {
                                        list.forEach { m ->
                                            val mId = m.id?.toString()?.toIntOrNull() ?: 0
                                            val mAuthorId = m.authorId?.toString()?.toIntOrNull() ?: 0
                                            val isMine = mAuthorId == currentUserId
                                            val isEditing = ObservableValue(false)

                                            div(className = "mb-2 p-2 rounded bg-black border border-secondary") {
                                                bind(isEditing) { editing ->
                                                    if (editing) {
                                                        val editInput = textInput(className = "form-control mb-2") {
                                                            value = m.content?.toString() ?: ""
                                                        }
                                                        div(className = "d-flex gap-2") {
                                                            tag(TAG.BUTTON, I18n.tr("Zapisz", "Save"), className = "btn btn-sm btn-success") {
                                                                onClick {
                                                                    val newVal = editInput.value ?: ""
                                                                    ApiService.editChatMessage(mId, newVal).then<dynamic> {
                                                                        loadMessages(selectedRecipients.value)
                                                                        null
                                                                    }.catch<dynamic> { _: Throwable -> window.alert("Błąd edycji wiadomości."); null }
                                                                }
                                                            }
                                                            tag(TAG.BUTTON, I18n.tr("Anuluj", "Cancel"), className = "btn btn-sm btn-secondary") {
                                                                onClick { isEditing.value = false }
                                                            }
                                                        }
                                                    } else {
                                                        div(className = "d-flex justify-content-between align-items-center small text-muted mb-1") {
                                                            span(m.authorName?.toString() ?: "", className = "fw-bold text-light")

                                                            div(className = "d-flex align-items-center") {
                                                                span(m.createdAt?.toString()?.take(16)?.replace("T", " ") ?: "", className = "me-2")

                                                                if (isMine) {
                                                                    tag(TAG.I, className = "fa-solid fa-pen text-info me-2 hover-card") {
                                                                        setStyle("cursor", "pointer")
                                                                        onClick { isEditing.value = true }
                                                                    }
                                                                    tag(TAG.I, className = "fa-solid fa-trash text-danger hover-card") {
                                                                        setStyle("cursor", "pointer")
                                                                        onClick {
                                                                            if (window.confirm(I18n.tr("Czy na pewno usunąć tę wiadomość?", "Delete this message?"))) {
                                                                                ApiService.deleteChatMessage(mId).then<dynamic> {
                                                                                    loadMessages(selectedRecipients.value)
                                                                                    null
                                                                                }.catch<dynamic> { _: Throwable -> window.alert("Błąd usuwania wiadomości."); null }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        p(m.content?.toString() ?: "", className = "text-white mb-0")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        div(className = "input-group mt-auto") {
                            val mInput = textInput(className = "form-control") {
                                placeholder = I18n.tr("Wpisz wiadomość...", "Type a message...")
                            }
                            tag(TAG.BUTTON, I18n.tr("Wyślij", "Send"), className = "btn dance-btn-primary fw-bold px-4 rounded-end") {
                                onClick {
                                    val txt = mInput.value ?: ""
                                    if (txt.isNotBlank()) {
                                        val recs = selectedRecipients.value
                                        if (role != "ADMIN" && (recs == null || recs.isEmpty())) {
                                            window.alert(I18n.tr("Najpierw wybierz z kim chcesz pisać!", "Select who you want to chat with first!"))
                                            return@onClick
                                        }

                                        ApiService.sendChatMessage(txt, recs).then<dynamic> {
                                            mInput.value = ""
                                            if (recs != null && recs.size > 1) {
                                                showToast("✔ Wysłano do wszystkich zaznaczonych!")
                                                selectedRecipients.value = if (role == "ADMIN") null else emptyList()
                                            } else {
                                                loadMessages(selectedRecipients.value)
                                            }
                                            null
                                        }.catch<dynamic> { _: Throwable -> window.alert("Błąd wysyłania wiadomości.") ; null }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // PLAYER — SIDE BY SIDE + AUTO TIMESTAMP
    // ==========================================
    private fun Container.buildPlayerView() {
        val comments = io.kvision.state.ObservableListWrapper<dynamic>()
        val commentsLoading = ObservableValue(true)
        val gradeSuccess = ObservableValue(false)

        val subId = PlayerState.submissionId
        val submissionUrl = PlayerState.submissionVideoUrl
        val instructionUrl = PlayerState.instructionVideoUrl
        val isGraded = PlayerState.isGraded
        val myUserId = window.localStorage.getItem("userId")?.toIntOrNull() ?: 1
        val role = window.localStorage.getItem("userRole") ?: ""

        val hasSideBySide = instructionUrl.isNotBlank()

        if (subId > 0) {
            ApiService.fetchComments(subId).then<dynamic> { res: dynamic ->
                comments.addAll(res as Array<dynamic>)
                commentsLoading.value = false
                null
            }.catch<dynamic> { _: Throwable -> commentsLoading.value = false; null }
        } else {
            commentsLoading.value = false
        }

        val backPage = when (role) {
            "CHOREOGRAPHER" -> if (isGraded) Page.CHOREO_ARCHIVE else Page.CHOREO_QUEUE
            "DANCER" -> Page.DANCER_SUBMISSIONS
            "ADMIN" -> Page.ADMIN_USER_DETAILS
            else -> Page.HOME
        }

        div(className = "container-fluid py-5 mt-5 pt-4 px-4") {
            backButton(appState, backPage)

            h2(PlayerState.taskTitle.ifBlank { I18n.tr("Analiza Video", "Video Analysis") }, className = "fw-bold mb-3")

            // === WIDEO(A) ===
            if (hasSideBySide) {
                div(className = "row g-2 mb-3") {
                    div(className = "col-md-6") {
                        div(className = "text-center mb-2") {
                            h5(I18n.tr("Nagranie tancerza", "Dancer's recording"), className = "fw-bold text-primary-dance mb-0")
                        }
                        tag(TAG.VIDEO, className = "w-100 rounded border border-primary-dance bg-black") {
                            setAttribute("controls", "controls")
                            setAttribute("style", "max-height: 380px;")
                            setAttribute("id", "submission-player")
                            tag(TAG.SOURCE) { setAttribute("src", submissionUrl); setAttribute("type", "video/mp4") }
                        }
                    }
                    div(className = "col-md-6") {
                        div(className = "text-center mb-2") {
                            h5(I18n.tr("Wideo instruktora", "Instructor's video"), className = "fw-bold text-light mb-0")
                        }
                        tag(TAG.VIDEO, className = "w-100 rounded border border-secondary bg-black") {
                            setAttribute("controls", "controls")
                            setAttribute("style", "max-height: 380px;")
                            setAttribute("id", "instruction-player")
                            tag(TAG.SOURCE) { setAttribute("src", instructionUrl); setAttribute("type", "video/mp4") }
                        }
                    }
                }
                div(className = "d-flex gap-2 mb-3") {
                    tag(TAG.BUTTON, I18n.tr("▶ Odtwórz oba", "▶ Play both"), className = "btn btn-sm dance-btn-primary fw-bold") {
                        onClick {
                            val v1 = document.getElementById("submission-player") as? HTMLVideoElement
                            val v2 = document.getElementById("instruction-player") as? HTMLVideoElement
                            v1?.play(); v2?.play()
                        }
                    }
                    tag(TAG.BUTTON, I18n.tr("⏸ Pauza obu", "⏸ Pause both"), className = "btn btn-sm btn-light text-dark fw-bold") {
                        onClick {
                            val v1 = document.getElementById("submission-player") as? HTMLVideoElement
                            val v2 = document.getElementById("instruction-player") as? HTMLVideoElement
                            v1?.pause(); v2?.pause()
                        }
                    }
                    tag(TAG.BUTTON, I18n.tr("↺ Reset", "↺ Reset"), className = "btn btn-sm btn-light text-dark fw-bold") {
                        onClick {
                            val v1 = document.getElementById("submission-player") as? HTMLVideoElement
                            val v2 = document.getElementById("instruction-player") as? HTMLVideoElement
                            if (v1 != null) v1.currentTime = 0.0
                            if (v2 != null) v2.currentTime = 0.0
                        }
                    }
                }
            } else {
                div(className = "text-center mb-2") {
                    h5(I18n.tr("🕺 Nagranie tancerza", "🕺 Dancer's recording"), className = "fw-bold text-primary-dance mb-0")
                }
                if (submissionUrl.isNotBlank()) {
                    tag(TAG.VIDEO, className = "w-100 rounded border border-secondary bg-black mb-3") {
                        setAttribute("controls", "controls")
                        setAttribute("style", "max-height: 420px;")
                        setAttribute("id", "submission-player")
                        tag(TAG.SOURCE) { setAttribute("src", submissionUrl); setAttribute("type", "video/mp4") }
                    }
                } else {
                    div(className = "ratio ratio-16x9 bg-black rounded border border-secondary mb-3 d-flex align-items-center justify-content-center") {
                        div(className = "text-center text-muted") {
                            tag(TAG.I, className = "fa-solid fa-film fa-3x mb-3"); p(I18n.tr("Brak pliku wideo", "No video file"))
                        }
                    }
                }
                if (isGraded) {
                    div(className = "card bg-dark border-success mb-3 p-3") {
                        div(className = "d-flex align-items-center mb-2") {
                            tag(TAG.I, className = "fa-solid fa-star text-warning me-2")
                            h5(I18n.tr("Ocena: ", "Grade: ") + "${PlayerState.currentScore ?: "—"}", className = "mb-0 text-white")
                        }
                        p(PlayerState.currentFeedback ?: I18n.tr("Brak feedbacku.", "No feedback."), className = "text-light mb-0")
                    }
                }
            }

            // === PRAWA KOLUMNA: Ocenianie + Komentarze ===
            div(className = "row g-3") {
                if (role == "CHOREOGRAPHER" && !isGraded && subId > 0) {
                    div(className = "col-lg-4") {
                        div(className = "card bg-dark border-primary-dance p-3") {
                            h5(I18n.tr("Oceń nagranie", "Grade recording"), className = "fw-bold mb-3 text-primary-dance")
                            label(I18n.tr("Ocena (1-10):", "Grade (1-10):"), className = "form-label text-light small")
                            val scoreInput = tag(TAG.INPUT, className = "form-control mb-2") {
                                setAttribute("type", "number"); setAttribute("min", "1"); setAttribute("max", "10"); setAttribute("placeholder", "np. 8")
                            }
                            label(I18n.tr("Feedback:", "Feedback:"), className = "form-label text-light small")
                            val feedbackInput = tag(TAG.TEXTAREA, className = "form-control mb-3") {
                                setAttribute("rows", "3"); setAttribute("placeholder", "Napisz swoje uwagi...")
                            }
                            div { bind(gradeSuccess) { ok -> if (ok) div(className = "alert alert-success py-2 mb-2") { span(I18n.tr("✔ Ocena zapisana!", "✔ Grade saved!")) } } }
                            tag(TAG.BUTTON, I18n.tr("Zapisz ocenę", "Save grade"), className = "btn dance-btn-primary w-100 fw-bold") {
                                onClick {
                                    val scoreVal = scoreInput.getElement()?.asDynamic().value?.toString()?.toIntOrNull()
                                    val feedbackVal = feedbackInput.getElement()?.asDynamic()?.value?.toString() ?: ""
                                    if (scoreVal == null || scoreVal < 1 || scoreVal > 10) { window.alert(I18n.tr("Podaj ocenę 1-10!", "Provide a grade between 1-10!")); return@onClick }
                                    ApiService.gradeSubmission(subId, scoreVal, feedbackVal).then<dynamic> { _: dynamic ->
                                        gradeSuccess.value = true
                                        PlayerState.isGraded = true; PlayerState.currentScore = scoreVal; PlayerState.currentFeedback = feedbackVal

                                        val restoredIds = window.localStorage.getItem("restored_subs")?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
                                        if (restoredIds.contains(subId.toString())) {
                                            restoredIds.remove(subId.toString())
                                            window.localStorage.setItem("restored_subs", restoredIds.joinToString(","))
                                        }

                                        null
                                    }.catch<dynamic> { _: Throwable -> window.alert(I18n.tr("Błąd zapisywania oceny.", "Grade save error.")); null }
                                }
                            }
                        }
                    }
                }

                div(className = if (role == "CHOREOGRAPHER" && !isGraded) "col-lg-8" else "col-12") {
                    div(className = "card bg-dark border-secondary p-3") {
                        h5(I18n.tr("Komentarze czasowe", "Timestamped comments"), className = "fw-bold mb-3")

                        if (role == "CHOREOGRAPHER" && subId > 0 && !isGraded) {
                            div(className = "mb-3 p-3 rounded border border-secondary bg-dark") {
                                label(I18n.tr("Napisz komentarz (sekunda zapisze się automatycznie):", "Write a comment (second saves automatically):"), className = "form-label text-white fw-bold mb-2")

                                val commentInput = tag(TAG.TEXTAREA, className = "form-control mb-3") {
                                    setAttribute("id", "comment-input")
                                    setAttribute("placeholder", I18n.tr("np. wyprostuj nogę, trzymaj tempo...", "e.g. straighten your leg, keep the tempo..."))
                                    setAttribute("rows", "2")
                                }

                                tag(TAG.BUTTON, I18n.tr("Dodaj komentarz do bieżącej sekundy", "Add comment at current second"), className = "btn dance-btn-primary btn-sm w-100 fw-bold") {
                                    onClick {
                                        val content = commentInput.getElement()?.asDynamic()?.value?.toString() ?: ""
                                        if (content.isBlank()) { window.alert(I18n.tr("Wpisz treść komentarza!", "Enter comment content!")); return@onClick }

                                        val video = document.getElementById("submission-player") as? HTMLVideoElement
                                        val currentSec = video?.currentTime?.toInt() ?: 0

                                        ApiService.addComment(subId, myUserId, currentSec, content).then<dynamic> { res: dynamic ->
                                            comments.add(res)
                                            commentInput.getElement()?.asDynamic()?.value = ""
                                            showToast(I18n.tr("✔ Komentarz dodany do ", "✔ Comment added at ") + "${formatTime(currentSec)}")
                                            null
                                        }.catch<dynamic> { _: Throwable -> window.alert(I18n.tr("Błąd dodawania komentarza.", "Error adding comment.")); null }
                                    }
                                }
                            }
                        }

                        div(className = "comments-list") {
                            div {
                                bind(commentsLoading) { loading ->
                                    if (loading) {
                                        p(I18n.tr("Ładowanie komentarzy...", "Loading comments..."), className = "text-muted small")
                                    } else {
                                        div {
                                            bind(comments) { list ->
                                                if (list.isEmpty()) {
                                                    p(I18n.tr("Brak komentarzy.", "No comments."), className = "text-muted small fst-italic")
                                                } else {
                                                    val sorted = list.sortedBy { it.timestampSeconds?.toString()?.toLongOrNull() ?: 0L }
                                                    sorted.forEach { c ->
                                                        val sec = c.timestampSeconds?.toString()?.toIntOrNull() ?: 0
                                                        val time = c.formattedTime?.toString() ?: formatTime(sec)
                                                        val content = c.content?.toString() ?: ""
                                                        val commentId = c.id?.toString()?.toIntOrNull()

                                                        div(className = "comment-item mb-2 p-2 rounded border border-secondary hover-card") {
                                                            div(className = "d-flex align-items-center justify-content-between mb-1") {
                                                                div(className = "d-flex align-items-center") {
                                                                    span(time, className = "badge bg-primary-dance me-2 font-monospace") {
                                                                        setStyle("cursor", "pointer")
                                                                        title = I18n.tr("Kliknij, aby przejść do tej sekundy", "Click to jump to this second")
                                                                        onClick {
                                                                            val v1 = document.getElementById("submission-player") as? HTMLVideoElement
                                                                            val v2 = document.getElementById("instruction-player") as? HTMLVideoElement

                                                                            v1?.currentTime = sec.toDouble()
                                                                            v2?.currentTime = sec.toDouble()

                                                                            v1?.play()
                                                                            v2?.play()
                                                                        }
                                                                    }
                                                                    small(I18n.tr("Choreograf", "Choreographer"), className = "text-muted")
                                                                }
                                                                // KOSZ TYLKO DLA CHOREOGRAFA I TYLKO KIEDY WŁAŚNIE OCENIA (!isGraded)
                                                                if (role == "CHOREOGRAPHER" && !PlayerState.isGraded && commentId != null) {
                                                                    tag(TAG.BUTTON, className = "btn btn-sm btn-outline-danger border-0 ms-2") {
                                                                        tag(TAG.I, className = "fa-solid fa-trash")
                                                                        setAttribute("title", I18n.tr("Usuń komentarz", "Delete comment"))
                                                                        onClick {
                                                                            if (window.confirm(I18n.tr("Czy na pewno chcesz usunąć ten komentarz?", "Are you sure you want to delete this comment?"))) {
                                                                                ApiService.deleteCommentAPI(commentId).then<dynamic> { _: dynamic ->
                                                                                    comments.removeAll { item: dynamic -> (item.id as? Int) == commentId }
                                                                                    showToast(I18n.tr("✔ Komentarz usunięty", "✔ Comment deleted"))
                                                                                    null
                                                                                }.catch<dynamic> { _: Throwable ->
                                                                                    window.alert(I18n.tr("Błąd usuwania komentarza.", "Error deleting comment."))
                                                                                    null
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            p(content, className = "mb-0 text-light small")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Container.buildBlockedView() {
        div(className = "d-flex flex-column justify-content-center align-items-center w-100") {
            setAttribute("style", "min-height: 70vh;")

            div(className = "card bg-white p-5 text-center shadow-lg border-0") {
                setAttribute("style", "max-width: 600px; border-radius: 20px;")

                tag(TAG.I, className = "fa-solid fa-lock mb-3") {
                    setAttribute("style", "font-size: 4em; color: #E87C9A;")
                }

                h2("Twoje konto zostało", className = "fw-normal text-dark mb-0")
                h1("zablokowane :(", className = "fw-bold mb-4") {
                    setAttribute("style", "color: #E87C9A;")
                }

                p("Możesz się zalogować, ale Twoje konto jest zablokowane i nie masz dostępu do żadnych funkcji aplikacji.", className = "text-muted mb-4")

                div(className = "p-3 bg-light rounded-3 mb-4 w-100 text-dark d-flex align-items-center justify-content-center") {
                    tag(TAG.I, className = "fa-solid fa-info-circle me-2") {
                        setAttribute("style", "color: #E87C9A;")
                    }
                    span("W razie pomyłki lub pytań skontaktuj się z administratorem.", className = "small fw-bold")
                }

                tag(TAG.BUTTON, "Skontaktuj się z administratorem", className = "btn w-100 fw-bold border-0") {
                    setAttribute("style", "background-color: #E87C9A; color: white; padding: 15px; border-radius: 10px;")
                    onClick {
                        appState.value = Page.CHAT
                    }
                }
            }
        }
    }
}

private fun toVideoUrl(path: String?): String {
    if (path.isNullOrBlank()) return ""
    if (path.startsWith("http")) return path
    val normalized = path.replace("\\", "/")
    val clean = normalized.trimStart('/')
    return "https://danceinsense.onrender.com/$clean"
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    val mm = if (m < 10) "0$m" else "$m"
    val ss = if (s < 10) "0$s" else "$s"
    return "$mm:$ss"
}


fun showToast(message: String) {
    val toast = document.createElement("div")
    toast.asDynamic().className = "dance-toast"
    toast.textContent = message
    document.body?.appendChild(toast)
    window.setTimeout({
        toast.asDynamic().classList.add("dance-toast-hide")
        window.setTimeout({ document.body?.removeChild(toast) }, 400)
    }, 2500)
}

fun main() {
    startApplication(::App, null, CoreModule, BootstrapModule, BootstrapCssModule, FontAwesomeModule, TomSelectModule)
}