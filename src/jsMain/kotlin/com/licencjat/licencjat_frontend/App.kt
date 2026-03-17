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
import io.kvision.form.text.textInput // NOWY IMPORT - pozwala na czyste inputy bez wrapperów!
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.files.Blob
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL

// --- MAGAZYN DANYCH ---
data class ChoreoTask(
    val id: Int,
    val title: String,
    val description: String,
    val instructionVideoUrl: String? = null,
    val assignedDancers: List<String>
)

object DataManager {
    val globalTasks = io.kvision.state.ObservableListWrapper<ChoreoTask>()

    val allDancers = listOf(
        "1" to "tancerz@danceapp.pl (Konto Testowe)",
        "2" to "Anna Kowalska",
        "3" to "Jan Nowak"
    )

    init {
        globalTasks.add(ChoreoTask(1, "Izolacje klatki piersiowej", "Nagraj 30-sekundowy film do dowolnego utworu.", null, listOf("1", "2")))
        globalTasks.add(ChoreoTask(2, "Footwork Basics", "Skup się na precyzji kroków i szybkich przejściach.", null, listOf("1", "3")))
    }
}

enum class Page {
    HOME, DANCER_DASHBOARD, CHOREO_DASHBOARD, ADMIN_PANEL, PLAYER,
    DANCER_TASKS, DANCER_SUBMISSIONS, DANCER_STATS,
    CHOREO_QUEUE, CHOREO_TASKS, CHOREO_ARCHIVE,
    ADMIN_VERIFY, ADMIN_USERS, ADMIN_MODERATION
}

class App : Application() {

    private val appState = ObservableValue(Page.HOME)
    private val userRole = ObservableValue<String?>(null)

    private lateinit var loginModal: Modal
    private lateinit var registerModal: Modal

    override fun start() {
        loginModal = createLoginModal()
        registerModal = createRegisterModal()

        root("kvapp") {
            bind(appState) { page ->
                vPanel(spacing = 0, className = "main-container bg-dark text-white min-vh-100") {
                    width = 100.vw
                    buildNavbar()

                    when (page) {
                        Page.HOME -> buildHomeView()
                        Page.DANCER_DASHBOARD -> buildDancerDashboard(appState)
                        Page.CHOREO_DASHBOARD -> buildChoreoDashboard()
                        Page.ADMIN_PANEL -> buildAdminPanel()
                        Page.DANCER_TASKS -> buildDancerTasks(appState)
                        Page.DANCER_SUBMISSIONS -> buildDancerSubmissions(appState)
                        Page.DANCER_STATS -> buildDancerStats(appState)
                        Page.CHOREO_QUEUE -> buildChoreoQueue()
                        Page.CHOREO_TASKS -> buildChoreoTasks()
                        Page.CHOREO_ARCHIVE -> buildChoreoArchive()
                        Page.ADMIN_VERIFY -> buildAdminVerify()
                        Page.ADMIN_USERS -> buildAdminUsers()
                        Page.ADMIN_MODERATION -> buildAdminModeration()
                        Page.PLAYER -> buildPlayerView()
                    }
                }
            }
        }
    }

    private fun Container.buildNavbar() {
        nav(className = "navbar navbar-expand-lg navbar-dark bg-dark fixed-top py-2 border-bottom border-secondary") {
            div(className = "container-fluid px-4") {
                link("DANCE APP", "javascript:void(0)", className = "navbar-brand fs-4 fw-bold tracking-wide text-primary-dance") {
                    onClick { appState.value = Page.HOME }
                }

                div(className = "collapse navbar-collapse justify-content-center") {
                    bind(userRole) { role ->
                        if (role != null) {
                            div(className = "navbar-nav") {
                                link("Mój Panel", "javascript:void(0)", className = "nav-link px-3 text-light fw-medium") {
                                    onClick {
                                        appState.value = when (role) {
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

                div(className = "d-flex align-items-center") {
                    bind(userRole) { role ->
                        if (role == null) {
                            link("Zaloguj się", "javascript:void(0)", className = "nav-link text-light me-4 fw-medium") {
                                onClick { loginModal.show() }
                            }
                            button("Dołącz teraz", className = "btn btn-light text-dark fw-bold px-4 rounded-pill") {
                                onClick { registerModal.show() }
                            }
                        } else {
                            span("Zalogowano jako: $role", className = "text-muted me-3 small")
                            button("Wyloguj", className = "btn btn-outline-danger btn-sm rounded-pill") {
                                onClick {
                                    window.localStorage.removeItem("jwt")
                                    userRole.value = null
                                    appState.value = Page.HOME
                                }
                            }
                        }
                    }
                }
            }
        }
    }

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
                        h1("Twój taniec. Nasz feedback.", className = "steezy-hero-title mb-3 shadow-text")
                        p("Wgraj nagranie i otrzymaj wskazówki od choreografów sekunda po sekundzie.", className = "steezy-hero-subtitle mb-5 shadow-text px-3")
                        button("Poczuj rytm", className = "btn dance-btn-primary btn-lg px-5 py-3 rounded-pill fw-bold") {
                            onClick { registerModal.show() }
                        }
                    }
                }
            }
            buildFeaturesSection()
        }
    }

    private fun Container.buildFeaturesSection() {
        div(className = "bg-white py-5") {
            div(className = "container py-5") {
                div(className = "row text-center mb-5") {
                    h2("Dlaczego nasza platforma?", className = "fw-bold text-dark")
                    p("Zaprojektowana z myślą o rozwoju i komunikacji.", className = "text-muted")
                }
                div(className = "row g-4 text-center") {
                    featureCard("fa-users", "Dla Tancerzy", "Otrzymuj konkretny feedback do swoich ruchów. Wgrywaj nagrania i śledź swój progres w dedykowanym panelu.")
                    featureCard("fa-video", "Dla Choreografów", "Zarządzaj zadaniami. Innowacyjny odtwarzacz wideo z notatkami czasowymi ułatwi Ci szybką ocenę techniki.")
                    featureCard("fa-comments", "Przestrzeń Komunikacji", "Bezpośredni kontakt trenera z tancerzem. Wymieniajcie się uwagami, aby każdy trening był jeszcze efektywniejszy.")
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
    // LOGOWANIE Z IDEALNIE WYRÓWNANYM OKIEM
    // ==========================================
    private fun createLoginModal(): Modal {
        val modal = Modal("Zaloguj się", closeButton = true, animation = true)
        modal.vPanel(className = "p-3") {

            // Czysty input dla E-maila
            label("E-mail", className = "form-label text-light mb-1")
            val emailInput = textInput(type = io.kvision.html.InputType.TEXT, className = "form-control bg-dark text-white border-secondary mb-3")

            // Czysty input dla Hasła spięty w Bootstrapowy Input Group
            label("Hasło", className = "form-label text-light mb-1")
            var passwordInput: io.kvision.form.text.TextInput? = null

            div(className = "input-group mb-2") {
                passwordInput = textInput(type = io.kvision.html.InputType.PASSWORD, className = "form-control bg-dark text-white border-secondary") {
                    id = "login-pass-input"
                }

                // Przycisk z okiem idealnie doczepiony z prawej strony
                button("", icon = "fa-solid fa-eye-slash", className = "btn btn-outline-secondary border-secondary text-muted") {
                    onClick {
                        val input = document.getElementById("login-pass-input") as? HTMLInputElement
                        if (input != null) {
                            if (input.type == "password") {
                                input.type = "text"
                                icon = "fa-solid fa-eye"
                            } else {
                                input.type = "password"
                                icon = "fa-solid fa-eye-slash"
                            }
                        }
                    }
                }
            }

            val errorMsg = span("", className = "text-danger small mt-1 d-block") { visible = false }

            button("Zaloguj", className = "btn dance-btn-primary btn-lg w-100 rounded-pill mt-4") {
                onClick {
                    val email = emailInput.value ?: ""
                    val pass = passwordInput?.value ?: ""

                    if (email.isBlank() || pass.isBlank()) {
                        errorMsg.content = "Podaj e-mail i hasło."
                        errorMsg.visible = true
                        return@onClick
                    }

                    errorMsg.visible = false

                    ApiService.login(email, pass).then { res: dynamic ->
                        val token = res.token?.toString()
                        val role = res.role?.toString()

                        if (token != null && role != null) {
                            window.localStorage.setItem("jwt", token)
                            userRole.value = role
                            appState.value = when (role) {
                                "DANCER" -> Page.DANCER_DASHBOARD
                                "CHOREOGRAPHER" -> Page.CHOREO_DASHBOARD
                                "ADMIN" -> Page.ADMIN_PANEL
                                else -> Page.HOME
                            }
                            modal.hide()
                        } else {
                            errorMsg.content = "Błąd autoryzacji z serwerem."
                            errorMsg.visible = true
                        }
                        null
                    }.catch { err: Throwable ->
                        // Wypisujemy błąd do konsoli, żeby wiedzieć co dokładnie padło!
                        console.log("SZCZEGÓŁY BŁĘDU LOGOWANIA:", err)
                        errorMsg.content = "Błędne dane. (Wciśnij F12 i sprawdź Konsolę by poznać powód!)"
                        errorMsg.visible = true
                        null
                    }
                }
            }
        }
        return modal
    }

    private fun createRegisterModal(): Modal {
        val modal = Modal("Zarejestruj się", closeButton = true, animation = true)
        modal.vPanel(spacing = 15, className = "p-3") {
            text(label = "Imię i nazwisko") { addCssClass("bg-dark"); addCssClass("text-white") }
            text(label = "E-mail") { addCssClass("bg-dark"); addCssClass("text-white") }
            password(label = "Hasło") { addCssClass("bg-dark"); addCssClass("text-white") }
            button("Utwórz konto", className = "btn btn-light btn-lg w-100 rounded-pill mt-3 text-dark fw-bold") {
                onClick { window.alert("Zgłoszenie wysłane!"); modal.hide() }
            }
        }
        return modal
    }

    private fun Container.buildChoreoDashboard() {
        div(className = "container py-5 mt-5") {
            h2("Panel Mentorski (Choreograf)", className = "fw-bold text-primary-dance mb-4")
            div(className = "row g-4") {
                dashboardCard("fa-clock", "Kolejka do oceny", "Filmy oczekujące na feedback.") { appState.value = Page.CHOREO_QUEUE }
                dashboardCard("fa-plus-circle", "Zarządzanie zadaniami", "Dodawaj wyzwania dla tancerzy.") { appState.value = Page.CHOREO_TASKS }
                dashboardCard("fa-folder-open", "Archiwum ocen", "Przeglądaj swoje oceny.") { appState.value = Page.CHOREO_ARCHIVE }
            }
        }
    }

    private fun Container.buildChoreoQueue() {
        div(className = "container py-5 mt-5") {
            backButton(appState, Page.CHOREO_DASHBOARD)
            h2("Kolejka do oceny", className = "fw-bold mb-4")
            table(className = "table table-dark table-hover align-middle") {
                thead { tr { th("Tancerz"); th("Zadanie"); th("Data"); th("Akcja") } }
                tbody {
                    tr {
                        td("tancerz@danceapp.pl"); td("Izolacje klatki"); td("Dziś, 14:30")
                        td { button("Oceń wideo", className = "btn btn-sm dance-btn-primary") { onClick { appState.value = Page.PLAYER } } }
                    }
                }
            }
        }
    }

    private fun Container.buildChoreoTasks() {
        val selected = io.kvision.state.ObservableListWrapper<String>()

        div(className = "container py-5 mt-5") {
            backButton(appState, Page.CHOREO_DASHBOARD)
            div(className = "row") {
                div(className = "col-md-7") {
                    h2("Kreator zadań", className = "fw-bold mb-4")
                    div(className = "card bg-dark border-secondary p-4") {
                        val taskTitleInput = text(label = "Tytuł zadania") { addCssClass("bg-dark"); addCssClass("text-white") }
                        val taskDescInput = textArea(label = "Opis wymagań") { addCssClass("bg-dark"); addCssClass("text-white") }

                        label("Przypisz do tancerzy:", className = "form-label text-light fw-bold mt-3")

                        val tagsBox = div(className = "dancer-tags-box mb-0") {}
                        val dropdownBox = div(className = "dancer-dropdown") { visible = false }

                        fun refresh() {
                            tagsBox.removeAll()
                            tagsBox.apply {
                                if (selected.isEmpty()) {
                                    span("Kliknij, aby wybrać tancerzy \u25BE", className = "text-muted small fst-italic")
                                } else {
                                    selected.forEach { id ->
                                        val name = DataManager.allDancers.find { it.first == id }?.second ?: id
                                        span(className = "dancer-tag") {
                                            span(name)
                                            span(" \u00D7", className = "dancer-tag-remove") {
                                                onClick { selected.remove(id); refresh() }
                                            }
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
                                        span("\u2605 Wszyscy moi tancerze")
                                        onClick {
                                            DataManager.allDancers.forEach { (id, _) ->
                                                if (!selected.contains(id)) selected.add(id)
                                            }
                                            dropdownBox.visible = false
                                            refresh()
                                        }
                                    }
                                }
                                DataManager.allDancers.filter { !selected.contains(it.first) }.forEach { (id, name) ->
                                    div(className = "dancer-option") {
                                        span(name)
                                        onClick { selected.add(id); dropdownBox.visible = false; refresh() }
                                    }
                                }
                                if (allChosen) {
                                    div(className = "dancer-option text-muted fst-italic small") {
                                        span("Wszyscy tancerze wybrani \u2713")
                                    }
                                }
                            }
                        }

                        tagsBox.onClick { dropdownBox.visible = !dropdownBox.visible }
                        refresh()

                        div(className = "mt-4") {
                            label("Wideo wzorcowe (wymagane)", className = "form-label text-light fw-bold")

                            val fileInput = tag(TAG.INPUT, className = "form-control bg-dark text-white border-secondary mb-3") {
                                setAttribute("type", "file")
                                setAttribute("accept", "video/*")
                            }

                            button("Opublikuj zadanie", className = "btn dance-btn-primary w-100 mt-3") {
                                onClick {
                                    val title = taskTitleInput.value
                                    val desc = taskDescInput.value ?: ""
                                    val files = fileInput.getElement()?.asDynamic().files

                                    if (title != null && selected.isNotEmpty() && files != null && files.length > 0) {
                                        val file = files[0]
                                        val deadline = "01.01.2026 15:00"
                                        val choreoId = 1L

                                        ApiService.createTask(title, desc, deadline, choreoId, file).then { response: dynamic ->
                                            val localUrl = URL.createObjectURL(file as Blob)

                                            val newId = if (response.id != null) response.id.toString().toInt() else DataManager.globalTasks.size + 1
                                            val newTitle = if (response.title != null) response.title.toString() else title
                                            val newDesc = if (response.description != null) response.description.toString() else desc

                                            DataManager.globalTasks.add(0, ChoreoTask(
                                                id = newId,
                                                title = newTitle,
                                                description = newDesc,
                                                instructionVideoUrl = localUrl,
                                                assignedDancers = selected.toList()
                                            ))

                                            val toast = document.createElement("div")
                                            toast.asDynamic().className = "dance-toast"
                                            toast.textContent = "✔ Zadanie zapisane w bazie!"
                                            document.body?.appendChild(toast)
                                            window.setTimeout({
                                                toast.asDynamic().classList.add("dance-toast-hide")
                                                window.setTimeout({ document.body?.removeChild(toast) }, 400)
                                            }, 2500)

                                            taskTitleInput.value = null
                                            taskDescInput.value = null
                                            fileInput.getElement()?.asDynamic().value = ""
                                            selected.clear()
                                            refresh()

                                            null
                                        }.catch { _: Throwable ->
                                            window.alert("Błąd połączenia z serwerem: Spróbuj włączyć backend Spring Boot.")
                                            null
                                        }

                                    } else {
                                        window.alert("Wypełnij tytuł, dodaj wideo i wybierz tancerzy!")
                                    }
                                }
                            }
                        }
                    }
                }
                div(className = "col-md-5") {
                    h4("Twoje aktywne zadania", className = "fw-bold mb-3 mt-4 mt-md-0")
                    ul(className = "list-group bg-dark shadow-sm") {
                        bind(DataManager.globalTasks) { tasks ->
                            tasks.forEach { task ->
                                li(className = "list-group-item bg-dark text-white border-secondary hover-card mb-2 d-flex justify-content-between align-items-center") {
                                    setStyle("cursor", "pointer")
                                    span(task.title, className = "fw-bold")
                                    tag(TAG.I, className = "fa-solid fa-chevron-right text-muted small")
                                    onClick { showTaskDetails(task) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showTaskDetails(task: ChoreoTask) {
        val modal = Modal("Szczegóły: ${task.title}", closeButton = true, animation = true)
        modal.div(className = "p-3") {
            h6("Opis zadania:", className = "text-primary-dance fw-bold mb-1")
            p(task.description.ifBlank { "Brak opisu." }) {
                setStyle("color", "#000000")
                addCssClass("mb-3")
            }

            if (task.instructionVideoUrl != null) {
                h6("Wideo wzorcowe:", className = "text-primary-dance fw-bold mb-2")
                tag(TAG.VIDEO, className = "w-100 rounded border border-secondary mb-3") {
                    setAttribute("controls", "controls")
                    setAttribute("style", "max-height:300px; background:#000;")
                    tag(TAG.SOURCE) { setAttribute("src", task.instructionVideoUrl) }
                }
            } else {
                p("Brak wideo wzorcowego.") {
                    setStyle("color", "#6c757d")
                    addCssClass("fst-italic")
                    addCssClass("mb-3")
                }
            }

            h6("Przypisani tancerze:", className = "text-primary-dance fw-bold mb-1")
            val dancerNames = task.assignedDancers.map { id ->
                DataManager.allDancers.find { it.first == id }?.second ?: "ID: $id"
            }
            p(dancerNames.joinToString(", ")) {
                setStyle("color", "#000000")
                addCssClass("mb-0")
            }
        }
        modal.show()
    }

    private fun Container.buildChoreoArchive() { buildPlaceholderView(appState, "Archiwum", Page.CHOREO_DASHBOARD) }

    private fun Container.buildAdminPanel() {
        div(className = "container py-5 mt-5") {
            h2("Panel Admina", className = "fw-bold text-danger mb-4")
            div(className = "row g-4") {
                dashboardCard("fa-user-check", "Zatwierdzanie", "") { appState.value = Page.ADMIN_VERIFY }
                dashboardCard("fa-users-cog", "Użytkownicy", "") { appState.value = Page.ADMIN_USERS }
                dashboardCard("fa-database", "Moderacja", "") { appState.value = Page.ADMIN_MODERATION }
            }
        }
    }

    private fun Container.buildAdminVerify() { div(className = "container py-5 mt-5") { backButton(appState, Page.ADMIN_PANEL); h2("Zatwierdzanie") } }
    private fun Container.buildAdminUsers() { div(className = "container py-5 mt-5") { backButton(appState, Page.ADMIN_PANEL); h2("Użytkownicy") } }
    private fun Container.buildAdminModeration() {
        div(className = "container py-5 mt-5") {
            backButton(appState, Page.ADMIN_PANEL); h2("Moderacja treści")
            button("Usuń nagranie #1", className = "btn btn-danger") { onClick { ApiService.deleteSubmissionAPI(1) } }
        }
    }

    private fun Container.buildPlayerView() {
        div(className = "container-fluid py-5 mt-5 px-4") {
            backButton(appState, Page.CHOREO_QUEUE)
            h2("Analiza Video", className = "fw-bold mb-4")
            div(className = "row") {
                div(className = "col-lg-8") {
                    div(className = "ratio ratio-16x9 bg-black rounded border border-secondary") {
                        div(className = "d-flex align-items-center justify-content-center text-muted") { p("Odtwarzacz Wideo") }
                    }
                }
                div(className = "col-lg-4") {
                    div(className = "card bg-dark border-secondary p-3") {
                        h5("Feedback", className = "fw-bold mb-3")
                        if (userRole.value == "CHOREOGRAPHER") {
                            button("Dodaj komentarz", className = "btn dance-btn-primary w-100") { onClick { window.alert("Zapisano!") } }
                        }
                    }
                }
            }
        }
    }
}

fun main() {
    startApplication(
        ::App,
        null,
        CoreModule,
        BootstrapModule,
        BootstrapCssModule,
        FontAwesomeModule,
        TomSelectModule
    )
}