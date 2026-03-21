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

// Pomocnicza funkcja do wyciągnięcia userId z JWT (payload base64)
fun getUserIdFromToken(): Int? {
    return try {
        val token = window.localStorage.getItem("jwt") ?: return null
        val payload = token.split(".").getOrNull(1) ?: return null
        val decoded = window.atob(payload.replace("-", "+").replace("_", "/"))
        val json = JSON.parse<dynamic>(decoded)
        // Spring Security ustawia "sub" jako email, więc userId pobieramy z osobnego pola jeśli jest,
        // lub fallback na 1 dla konta testowego
        val sub = json.sub?.toString() ?: return null
        // Próba pobrania userId z claims jeśli backend go dodaje
        val userId = json.userId
        if (userId != null) userId.toString().toIntOrNull() else null
    } catch (e: Throwable) {
        null
    }
}

class App : Application() {

    private val appState = ObservableValue(Page.HOME)
    private val userRole = ObservableValue<String?>(null)
    private val currentUserId = ObservableValue<Int?>(null)

    private lateinit var loginModal: Modal
    private lateinit var registerModal: Modal

    override fun start() {
        // Przywróć sesję jeśli token istnieje
        val savedToken = window.localStorage.getItem("jwt")
        val savedRole = window.localStorage.getItem("userRole")
        val savedId = window.localStorage.getItem("userId")?.toIntOrNull()
        if (savedToken != null && savedRole != null) {
            userRole.value = savedRole
            currentUserId.value = savedId
        }

        loginModal = createLoginModal()
        registerModal = createRegisterModal()

        root("kvapp") {
            vPanel(spacing = 0, className = "main-container bg-dark text-white min-vh-100") {
                width = 100.vw
                buildNavbar()
                div(className = "page-content") {
                    bind(appState) { page ->
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
    // LOGOWANIE
    // ==========================================
    private fun createLoginModal(): Modal {
        val modal = Modal("Zaloguj się", closeButton = true, animation = true)
        modal.vPanel(className = "p-3") {

            label("E-mail", className = "form-label text-light mb-1")
            val emailInput = textInput(type = io.kvision.html.InputType.TEXT, className = "form-control bg-dark text-white border-secondary mb-3")

            label("Hasło", className = "form-label text-light mb-1")
            var passwordInput: io.kvision.form.text.TextInput? = null

            div(className = "input-group mb-2") {
                passwordInput = textInput(type = io.kvision.html.InputType.PASSWORD, className = "form-control bg-dark text-white border-secondary") {
                    id = "login-pass-input"
                }

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
                            window.localStorage.setItem("userRole", role)

                            // Pobierz userId po zalogowaniu — szukaj usera po emailu przez listę
                            ApiService.fetchUsers().then { users: dynamic ->
                                val list = users as Array<dynamic>
                                val found = list.find { it.email?.toString() == email }
                                val userId = found?.id?.toString()?.toIntOrNull()
                                if (userId != null) {
                                    window.localStorage.setItem("userId", userId.toString())
                                    currentUserId.value = userId
                                }
                                null
                            }.catch { _: Throwable -> null }

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
                        console.log("SZCZEGÓŁY BŁĘDU LOGOWANIA:", err)
                        errorMsg.content = "Błędne dane lub brak połączenia."
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

    // ==========================================
    // CHOREOGRAF
    // ==========================================

    private fun Container.buildChoreoDashboard() {
        div(className = "container py-5 mt-5") {
            h2("Panel Mentorski (Choreograf)", className = "fw-bold text-primary-dance mb-4 pt-4")
            div(className = "row g-4") {
                dashboardCard("fa-clock", "Kolejka do oceny", "Filmy oczekujące na feedback.") { appState.value = Page.CHOREO_QUEUE }
                dashboardCard("fa-plus-circle", "Zarządzanie zadaniami", "Dodawaj wyzwania dla tancerzy.") { appState.value = Page.CHOREO_TASKS }
                dashboardCard("fa-folder-open", "Archiwum ocen", "Przeglądaj swoje oceny.") { appState.value = Page.CHOREO_ARCHIVE }
            }
        }
    }

    private fun Container.buildChoreoQueue() {
        val subs = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)
        val errorMsg = ObservableValue("")

        // Pobierz nagrania dla WSZYSTKICH zadań z DataManager
        fun loadAllSubmissions() {
            val taskIds = DataManager.globalTasks.map { it.id }.toMutableList()
            // Dodaj też zadania z backendu — pobierz świeżo
            ApiService.fetchTasks().then { res: dynamic ->
                val backendIds = (res as Array<dynamic>).mapNotNull { it.id?.toString()?.toIntOrNull() }
                backendIds.forEach { id -> if (!taskIds.contains(id)) taskIds.add(id) }

                val promises = taskIds.map { taskId ->
                    ApiService.fetchSubmissionsForTask(taskId)
                }

                // Sekwencyjne pobieranie dla każdego taskId
                fun fetchNext(index: Int) {
                    if (index >= taskIds.size) {
                        loading.value = false
                        return
                    }
                    ApiService.fetchSubmissionsForTask(taskIds[index]).then { r: dynamic ->
                        val list = (r as Array<dynamic>).filter { it.status?.toString() == "SUBMITTED" }
                        subs.addAll(list)
                        fetchNext(index + 1)
                        null
                    }.catch { _: Throwable ->
                        fetchNext(index + 1)
                        null
                    }
                }
                fetchNext(0)
                null
            }.catch { _: Throwable ->
                // Fallback — pobierz tylko dla znanych zadań lokalnych
                fun fetchNext(index: Int) {
                    if (index >= taskIds.size) { loading.value = false; return }
                    ApiService.fetchSubmissionsForTask(taskIds[index]).then { r: dynamic ->
                        val list = (r as Array<dynamic>).filter { it.status?.toString() == "SUBMITTED" }
                        subs.addAll(list)
                        fetchNext(index + 1)
                        null
                    }.catch { _: Throwable -> fetchNext(index + 1); null }
                }
                fetchNext(0)
                null
            }
        }

        loadAllSubmissions()

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.CHOREO_DASHBOARD)
            h2("Kolejka do oceny", className = "fw-bold mb-4")
            div {
                bind(loading) { isLoading ->
                    if (isLoading) {
                        div(className = "text-center py-5") {
                            tag(TAG.DIV, className = "spinner-border text-primary-dance") { setAttribute("role", "status") }
                            p("Ładowanie nagrań...", className = "text-muted mt-3")
                        }
                    } else {
                        table(className = "table table-dark table-hover align-middle") {
                            thead { tr { th("ID Nagrania"); th("Tancerz ID"); th("Zadanie ID"); th("Data"); th("Status"); th("Akcja") } }
                            tbody {
                                bind(subs) { list ->
                                    if (list.isEmpty()) {
                                        tr { td("Hura! Brak filmów do oceny.") { setAttribute("colspan", "6") } }
                                    }
                                    list.forEach { s ->
                                        tr {
                                            td("Wideo #${s.id}")
                                            td("Tancerz #${s.dancerId}")
                                            td("Zadanie #${s.taskId}")
                                            td(s.sentAt?.toString()?.substring(0, 10) ?: "—")
                                            td { span("Oczekuje", className = "badge bg-warning text-dark") }
                                            td {
                                                button("Oceń wideo", className = "btn btn-sm dance-btn-primary") {
                                                    onClick { appState.value = Page.PLAYER }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } // end wrapper div
        }
    }

    private fun Container.buildChoreoTasks() {
        val selected = io.kvision.state.ObservableListWrapper<String>()

        div(className = "container py-5 mt-5 pt-5") {
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

    private fun Container.buildChoreoArchive() {
        val subs = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)

        fun loadGraded() {
            val taskIds = DataManager.globalTasks.map { it.id }.toMutableList()
            ApiService.fetchTasks().then { res: dynamic ->
                val backendIds = (res as Array<dynamic>).mapNotNull { it.id?.toString()?.toIntOrNull() }
                backendIds.forEach { id -> if (!taskIds.contains(id)) taskIds.add(id) }

                fun fetchNext(index: Int) {
                    if (index >= taskIds.size) { loading.value = false; return }
                    ApiService.fetchSubmissionsForTask(taskIds[index]).then { r: dynamic ->
                        val list = (r as Array<dynamic>).filter { it.status?.toString() == "GRADED" }
                        subs.addAll(list)
                        fetchNext(index + 1)
                        null
                    }.catch { _: Throwable -> fetchNext(index + 1); null }
                }
                fetchNext(0)
                null
            }.catch { _: Throwable ->
                fun fetchNext(index: Int) {
                    if (index >= taskIds.size) { loading.value = false; return }
                    ApiService.fetchSubmissionsForTask(taskIds[index]).then { r: dynamic ->
                        val list = (r as Array<dynamic>).filter { it.status?.toString() == "GRADED" }
                        subs.addAll(list)
                        fetchNext(index + 1)
                        null
                    }.catch { _: Throwable -> fetchNext(index + 1); null }
                }
                fetchNext(0)
                null
            }
        }

        loadGraded()

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.CHOREO_DASHBOARD)
            h2("Archiwum ocenionych nagrań", className = "fw-bold mb-4")
            div {
                bind(loading) { isLoading ->
                    if (isLoading) {
                        div(className = "text-center py-5") {
                            tag(TAG.DIV, className = "spinner-border text-primary-dance") { setAttribute("role", "status") }
                            p("Ładowanie archiwum...", className = "text-muted mt-3")
                        }
                    } else {
                        table(className = "table table-dark table-hover align-middle") {
                            thead { tr { th("ID Nagrania"); th("Tancerz ID"); th("Zadanie ID"); th("Ocena"); th("Akcja") } }
                            tbody {
                                bind(subs) { list ->
                                    if (list.isEmpty()) {
                                        tr { td("Jeszcze nic nie oceniłeś.") { setAttribute("colspan", "5") } }
                                    }
                                    list.forEach { s ->
                                        tr {
                                            td("Wideo #${s.id}")
                                            td("Tancerz #${s.dancerId}")
                                            td("Zadanie #${s.taskId}")
                                            td {
                                                val score = s.score?.toString()
                                                if (score != null) span(score, className = "badge bg-success")
                                                else span("Ocenione", className = "badge bg-success")
                                            }
                                            td {
                                                button("Zobacz ocenę", className = "btn btn-sm btn-outline-light") {
                                                    onClick { appState.value = Page.PLAYER }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } // end wrapper div
        }
    }

    // ==========================================
    // ADMIN
    // ==========================================

    private fun Container.buildAdminPanel() {
        div(className = "container py-5 mt-5") {
            h2("Panel Admina", className = "fw-bold text-danger mb-4 pt-4")
            div(className = "row g-4") {
                dashboardCard("fa-user-check", "Zatwierdzanie kont", "Aktywuj lub dezaktywuj użytkowników.") { appState.value = Page.ADMIN_VERIFY }
                dashboardCard("fa-users-cog", "Użytkownicy", "Lista wszystkich zarejestrowanych kont.") { appState.value = Page.ADMIN_USERS }
                dashboardCard("fa-database", "Moderacja treści", "Usuń niepożądane nagrania lub komentarze.") { appState.value = Page.ADMIN_MODERATION }
            }
        }
    }

    private fun Container.buildAdminVerify() {
        val usersList = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)

        ApiService.fetchUsers().then { res: dynamic ->
            usersList.addAll(res as Array<dynamic>)
            loading.value = false
            null
        }.catch { _: Throwable ->
            loading.value = false
            null
        }

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.ADMIN_PANEL)
            h2("Zatwierdzanie kont", className = "fw-bold mb-4")
            div {
                bind(loading) { isLoading ->
                    if (isLoading) {
                        div(className = "text-center py-5") {
                            tag(TAG.DIV, className = "spinner-border text-danger") { setAttribute("role", "status") }
                            p("Ładowanie użytkowników...", className = "text-muted mt-3")
                        }
                    } else {
                        table(className = "table table-dark table-hover") {
                            thead { tr { th("ID"); th("Email"); th("Rola"); th("Status"); th("Akcja") } }
                            tbody {
                                bind(usersList) { list ->
                                    if (list.isEmpty()) {
                                        tr { td("Brak użytkowników.") { setAttribute("colspan", "5") } }
                                    }
                                    list.forEach { u ->
                                        tr {
                                            td(u.id?.toString() ?: "—")
                                            td(u.email?.toString() ?: "—")
                                            td(u.role?.toString() ?: "—")
                                            td {
                                                val active = u.isActive == true
                                                span(if (active) "Aktywny" else "Nieaktywny",
                                                    className = if (active) "badge bg-success" else "badge bg-danger")
                                            }
                                            td {
                                                val userId = u.id?.toString()?.toIntOrNull()
                                                val active = u.isActive == true
                                                if (userId != null) {
                                                    button(if (active) "Dezaktywuj" else "Aktywuj",
                                                        className = if (active) "btn btn-sm btn-outline-danger" else "btn btn-sm btn-outline-success") {
                                                        onClick {
                                                            ApiService.setUserStatus(userId, !active).then { _: dynamic ->
                                                                // Odśwież listę
                                                                usersList.clear()
                                                                ApiService.fetchUsers().then { res: dynamic ->
                                                                    usersList.addAll(res as Array<dynamic>)
                                                                    null
                                                                }.catch { _: Throwable -> null }
                                                                null
                                                            }.catch { _: Throwable ->
                                                                window.alert("Błąd zmiany statusu użytkownika.")
                                                                null
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
            } // end wrapper div
        }
    }

    private fun Container.buildAdminUsers() {
        val usersList = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)

        ApiService.fetchUsers().then { res: dynamic ->
            usersList.addAll(res as Array<dynamic>)
            loading.value = false
            null
        }.catch { _: Throwable ->
            loading.value = false
            null
        }

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.ADMIN_PANEL)
            h2("Użytkownicy Platformy", className = "mb-4")
            div {
                bind(loading) { isLoading ->
                    if (isLoading) {
                        div(className = "text-center py-5") {
                            tag(TAG.DIV, className = "spinner-border text-danger") { setAttribute("role", "status") }
                            p("Ładowanie użytkowników...", className = "text-muted mt-3")
                        }
                    } else {
                        table(className = "table table-dark table-hover") {
                            thead { tr { th("ID"); th("Imię"); th("Nazwisko"); th("Email"); th("Rola"); th("Aktywny") } }
                            tbody {
                                bind(usersList) { list ->
                                    if (list.isEmpty()) {
                                        tr { td("Brak użytkowników lub brak dostępu.") { setAttribute("colspan", "6") } }
                                    }
                                    list.forEach { u ->
                                        tr {
                                            td(u.id?.toString() ?: "—")
                                            td(u.firstName?.toString() ?: "—")
                                            td(u.lastName?.toString() ?: "—")
                                            td(u.email?.toString() ?: "—")
                                            td(u.role?.toString() ?: "—")
                                            td(if (u.isActive == true) "Tak" else "Nie")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } // end wrapper div
        }
    }

    private fun Container.buildAdminModeration() {
        val submissionsList = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)

        // Pobierz wszystkie nagrania ze wszystkich zadań
        fun loadAll() {
            val taskIds = DataManager.globalTasks.map { it.id }.toMutableList()
            ApiService.fetchTasks().then { res: dynamic ->
                val backendIds = (res as Array<dynamic>).mapNotNull { it.id?.toString()?.toIntOrNull() }
                backendIds.forEach { id -> if (!taskIds.contains(id)) taskIds.add(id) }

                fun fetchNext(index: Int) {
                    if (index >= taskIds.size) { loading.value = false; return }
                    ApiService.fetchSubmissionsForTask(taskIds[index]).then { r: dynamic ->
                        submissionsList.addAll(r as Array<dynamic>)
                        fetchNext(index + 1)
                        null
                    }.catch { _: Throwable -> fetchNext(index + 1); null }
                }
                fetchNext(0)
                null
            }.catch { _: Throwable ->
                loading.value = false
                null
            }
        }

        loadAll()

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.ADMIN_PANEL)
            h2("Moderacja treści", className = "fw-bold mb-4")
            div {
                bind(loading) { isLoading ->
                    if (isLoading) {
                        div(className = "text-center py-5") {
                            tag(TAG.DIV, className = "spinner-border text-danger") { setAttribute("role", "status") }
                            p("Ładowanie nagrań...", className = "text-muted mt-3")
                        }
                    } else {
                        table(className = "table table-dark table-hover align-middle") {
                            thead { tr { th("ID"); th("Tancerz"); th("Zadanie"); th("Status"); th("Akcja") } }
                            tbody {
                                bind(submissionsList) { list ->
                                    if (list.isEmpty()) {
                                        tr { td("Brak nagrań w systemie.") { setAttribute("colspan", "5") } }
                                    }
                                    list.forEach { s ->
                                        tr {
                                            td("Wideo #${s.id}")
                                            td("Tancerz #${s.dancerId}")
                                            td("Zadanie #${s.taskId}")
                                            td {
                                                val status = s.status?.toString() ?: "SUBMITTED"
                                                span(status, className = if (status == "GRADED") "badge bg-success" else "badge bg-warning text-dark")
                                            }
                                            td {
                                                val subId = s.id?.toString()?.toIntOrNull()
                                                if (subId != null) {
                                                    button("Usuń", className = "btn btn-sm btn-danger") {
                                                        onClick {
                                                            if (window.confirm("Na pewno chcesz usunąć nagranie #$subId?")) {
                                                                ApiService.deleteSubmissionAPI(subId)
                                                                submissionsList.removeAll { it.id?.toString()?.toIntOrNull() == subId }
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
            } // end wrapper div
        }
    }

    // ==========================================
    // PLAYER (WSPÓLNY)
    // ==========================================

    private fun Container.buildPlayerView() {
        div(className = "container-fluid py-5 mt-5 pt-5 px-4") {
            // Przycisk Wróć zależy od roli
            val backPage = when (userRole.value) {
                "CHOREOGRAPHER" -> Page.CHOREO_QUEUE
                "DANCER" -> Page.DANCER_SUBMISSIONS
                "ADMIN" -> Page.ADMIN_MODERATION
                else -> Page.HOME
            }
            backButton(appState, backPage)
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