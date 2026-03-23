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
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.url.URL

// --- MODEL ZADANIA ---
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

enum class Page {
    HOME, DANCER_DASHBOARD, CHOREO_DASHBOARD, ADMIN_PANEL, PLAYER,
    DANCER_TASKS, DANCER_SUBMISSIONS, DANCER_STATS,
    CHOREO_QUEUE, CHOREO_TASKS, CHOREO_ARCHIVE,
    ADMIN_VERIFY, ADMIN_USERS, ADMIN_MODERATION
}

class App : Application() {

    private val appState = ObservableValue(Page.HOME)
    private val userRole = ObservableValue<String?>(null)
    private val currentUserId = ObservableValue<Int?>(null)

    private lateinit var loginModal: Modal
    private lateinit var registerModal: Modal

    override fun start() {
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
                    val contentContainer = this
                    bind(appState) { page ->
                        contentContainer.apply {
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
    }

    // ==========================================
    // NAVBAR
    // ==========================================
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
                            tag(TAG.BUTTON, "Dołącz teraz", className = "btn btn-light text-dark fw-bold px-4 rounded-pill") {
                                onClick { registerModal.show() }
                            }
                        } else {
                            span("Zalogowano jako: $role", className = "text-muted me-3 small")
                            tag(TAG.BUTTON, "Wyloguj", className = "btn dance-btn-primary btn-sm rounded-pill px-3 fw-bold") {
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
    // HOME
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
                        h1("Twój taniec. Nasz feedback.", className = "steezy-hero-title mb-3 shadow-text")
                        p("Wgraj nagranie i otrzymaj wskazówki od choreografów sekunda po sekundzie.", className = "steezy-hero-subtitle mb-5 shadow-text px-3")
                        tag(TAG.BUTTON, "Poczuj rytm", className = "btn dance-btn-primary btn-lg px-5 py-3 rounded-pill fw-bold") {
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
        val modal = Modal("Zaloguj się", closeButton = true, animation = true) {
            addCssClass("login-modal-custom")
        }

        modal.vPanel(className = "p-4") {
            h3("Witaj w DANCE APP", className = "text-center fw-bold text-primary-dance mb-4")

            label("Adres e-mail", className = "form-label text-dark fw-bold mb-1")
            val emailInput = textInput(type = io.kvision.html.InputType.TEXT, className = "form-control bg-light text-dark login-input rounded-3 mb-3") {
                placeholder = "wpisz swój e-mail"
            }

            label("Hasło", className = "form-label text-dark fw-bold mb-1")
            var passwordInput: io.kvision.form.text.TextInput? = null

            div(className = "input-group mb-4") {
                passwordInput = textInput(type = io.kvision.html.InputType.PASSWORD, className = "form-control bg-light text-dark login-input rounded-start-3") {
                    placeholder = "wpisz hasło"
                }

                tag(TAG.BUTTON, className = "btn dance-btn-primary rounded-end-3") {
                    setAttribute("type", "button")
                    setAttribute("title", "Pokaż/Ukryj hasło")
                    val iconTag = tag(TAG.I, className = "fa-solid fa-eye-slash text-light")

                    onClick {
                        if (passwordInput?.type == io.kvision.html.InputType.PASSWORD) {
                            passwordInput?.type = io.kvision.html.InputType.TEXT
                            iconTag.removeCssClass("fa-eye-slash")
                            iconTag.addCssClass("fa-eye")
                        } else {
                            passwordInput?.type = io.kvision.html.InputType.PASSWORD
                            iconTag.removeCssClass("fa-eye")
                            iconTag.addCssClass("fa-eye-slash")
                        }
                    }
                }
            }

            val errorText = span("", className = "small fw-bold")
            val errorAlert = div(className = "alert alert-danger py-2 mb-3 text-center rounded-3") {
                visible = false
                add(errorText)
            }

            tag(TAG.BUTTON, "Zaloguj się", className = "btn dance-btn-primary btn-lg w-100 rounded-pill fw-bold") {
                onClick {
                    val email = emailInput.value ?: ""
                    val pass = passwordInput?.value ?: ""

                    if (email.isBlank() || pass.isBlank()) {
                        errorText.content = "Podaj e-mail i hasło."
                        errorAlert.visible = true
                        return@onClick
                    }

                    errorAlert.visible = false

                    ApiService.login(email, pass).then { res: dynamic ->
                        val token = res.token?.toString()
                        val role = res.role?.toString()
                        if (token != null && role != null && token.isNotBlank()) {
                            window.localStorage.setItem("jwt", token)
                            window.localStorage.setItem("userRole", role)
                            ApiService.fetchUsersWithToken(token).then { users: dynamic ->
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
                            errorText.content = "Błędne dane logowania."
                            errorAlert.visible = true
                        }
                        null
                    }.catch { err: Throwable ->
                        console.log("BŁĄD LOGOWANIA:", err)
                        errorText.content = "Błędne dane lub brak połączenia."
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
        val modal = Modal("Zarejestruj się", closeButton = true, animation = true) {
            addCssClass("login-modal-custom")
        }

        modal.vPanel(className = "p-4") {
            h3("Utwórz konto w DANCE APP", className = "text-center fw-bold text-primary-dance mb-4")

            label("Imię", className = "form-label text-dark fw-bold mb-1")
            val firstNameInput = textInput(type = io.kvision.html.InputType.TEXT, className = "form-control bg-light text-dark login-input rounded-3 mb-3") {
                placeholder = "wpisz swoje imię"
            }

            label("Nazwisko", className = "form-label text-dark fw-bold mb-1")
            val lastNameInput = textInput(type = io.kvision.html.InputType.TEXT, className = "form-control bg-light text-dark login-input rounded-3 mb-3") {
                placeholder = "wpisz swoje nazwisko"
            }

            label("E-mail", className = "form-label text-dark fw-bold mb-1")
            val emailInput = textInput(type = io.kvision.html.InputType.TEXT, className = "form-control bg-light text-dark login-input rounded-3 mb-3") {
                placeholder = "twoj@email.com"
            }

            label("Hasło", className = "form-label text-dark fw-bold mb-1")
            val passInput = textInput(type = io.kvision.html.InputType.PASSWORD, className = "form-control bg-light text-dark login-input rounded-3 mb-4") {
                placeholder = "utwórz hasło"
            }

            val errorMsg = span("", className = "text-danger small d-block mb-2 text-center") { visible = false }
            val successMsg = span("", className = "text-success small d-block mb-2 text-center") { visible = false }

            tag(TAG.BUTTON, "Dołącz!", className = "btn dance-btn-primary btn-lg w-100 rounded-pill fw-bold") {
                onClick {
                    val firstName = firstNameInput.value ?: ""
                    val lastName = lastNameInput.value ?: ""
                    val email = emailInput.value ?: ""
                    val pass = passInput.value ?: ""

                    if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || pass.isBlank()) {
                        errorMsg.content = "Proszę wypełnić wszystkie pola."
                        errorMsg.visible = true
                        successMsg.visible = false
                        return@onClick
                    }

                    errorMsg.visible = false
                    ApiService.registerUser(firstName, lastName, email, pass).then { _: dynamic ->
                        successMsg.content = "Konto utworzone pomyślnie! Możesz się teraz zalogować."
                        successMsg.visible = true
                        errorMsg.visible = false
                        window.setTimeout({ modal.hide() }, 2500)
                        null
                    }.catch { _: Throwable ->
                        errorMsg.content = "Wystąpił błąd podczas rejestracji. Ten e-mail może być już zajęty."
                        errorMsg.visible = true
                        successMsg.visible = false
                        null
                    }
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
        val tasks = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)

        ApiService.fetchTasks().then { res: dynamic ->
            val taskList = res as Array<dynamic>
            tasks.addAll(taskList)
            val taskIds = taskList.mapNotNull { it.id?.toString()?.toIntOrNull() }
            if (taskIds.isEmpty()) { loading.value = false; return@then null }
            fun fetchNext(index: Int) {
                if (index >= taskIds.size) { loading.value = false; return }
                ApiService.fetchSubmissionsForTask(taskIds[index]).then { r: dynamic ->
                    val list = (r as Array<dynamic>).filter { it.status?.toString() == "SUBMITTED" }
                    subs.addAll(list)
                    fetchNext(index + 1); null
                }.catch { _: Throwable -> fetchNext(index + 1); null }
            }
            fetchNext(0); null
        }.catch { _: Throwable -> loading.value = false; null }

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
                        div {
                            bind(subs) { list ->
                                if (list.isEmpty()) {
                                    div(className = "card bg-dark border-secondary p-5 text-center") {
                                        tag(TAG.I, className = "fa-solid fa-check-circle fa-3x text-success mb-3")
                                        p("Hura! Brak filmów do oceny.", className = "text-muted")
                                    }
                                } else {
                                    table(className = "table table-dark table-hover align-middle") {
                                        thead { tr { th("Nagranie"); th("Tancerz"); th("Zadanie"); th("Data"); th("Status"); th("Akcja") } }
                                        tbody {
                                            list.forEach { s ->
                                                val subId = s.id?.toString()?.toIntOrNull() ?: 0
                                                val videoUrl = s.videoUrl?.toString() ?: ""
                                                val taskId = s.taskId?.toString()?.toIntOrNull() ?: 0
                                                val taskObj = tasks.find { it.id?.toString()?.toIntOrNull() == taskId }
                                                val instrUrl = taskObj?.instructionVideoUrl?.toString() ?: ""
                                                tr {
                                                    td("Wideo #${s.id}")
                                                    td("Tancerz #${s.dancerId}")
                                                    td("Zadanie #${s.taskId}")
                                                    td(s.sentAt?.toString()?.substring(0, 10) ?: "—")
                                                    td { span("Oczekuje", className = "badge bg-warning text-dark") }
                                                    td {
                                                        tag(TAG.BUTTON, "Oceń wideo", className = "btn btn-sm dance-btn-primary") {
                                                            onClick {
                                                                PlayerState.submissionId = subId
                                                                PlayerState.submissionVideoUrl = toVideoUrl(videoUrl)
                                                                PlayerState.instructionVideoUrl = toVideoUrl(instrUrl)
                                                                PlayerState.taskTitle = taskObj?.title?.toString() ?: "Zadanie #$taskId"
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

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.CHOREO_DASHBOARD)
            div(className = "row") {
                div(className = "col-md-7") {
                    h2("Kreator zadań", className = "fw-bold mb-4")
                    div(className = "card bg-dark border-secondary p-4") {
                        val taskTitleInput = text(label = "Tytuł zadania") { addCssClass("bg-dark"); addCssClass("text-white") }
                        val taskDescInput = textArea(label = "Opis wymagań") { addCssClass("bg-dark"); addCssClass("text-white") }

                        label("Termin wykonania:", className = "form-label text-light fw-bold mt-3")
                        val deadlineInput = tag(TAG.INPUT, className = "form-control bg-dark text-white border-secondary mb-3") {
                            setAttribute("type", "datetime-local")
                            val now = js("new Date()")
                            now.setMonth(now.getMonth() + 3)
                            val iso = now.toISOString().toString().substring(0, 16)
                            setAttribute("value", iso)
                        }

                        label("Przypisz do tancerzy:", className = "form-label text-light fw-bold mt-2")
                        val tagsBox = div(className = "dancer-tags-box mb-0") {}
                        val dropdownBox = div(className = "dancer-dropdown") { visible = false }

                        fun refresh() {
                            tagsBox.removeAll()
                            tagsBox.apply {
                                if (selected.isEmpty()) span("Kliknij, aby wybrać tancerzy \u25BE", className = "text-muted small fst-italic")
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
                                        span("\u2605 Wszyscy moi tancerze")
                                        onClick { DataManager.allDancers.forEach { (id, _) -> if (!selected.contains(id)) selected.add(id) }; dropdownBox.visible = false; refresh() }
                                    }
                                }
                                DataManager.allDancers.filter { !selected.contains(it.first) }.forEach { (id, name) ->
                                    div(className = "dancer-option") {
                                        span(name)
                                        onClick { selected.add(id); dropdownBox.visible = false; refresh() }
                                    }
                                }
                                if (allChosen) div(className = "dancer-option text-muted fst-italic small") { span("Wszyscy tancerze wybrani \u2713") }
                            }
                        }
                        tagsBox.onClick { dropdownBox.visible = !dropdownBox.visible }
                        refresh()

                        div(className = "mt-4") {
                            label("Wideo wzorcowe (wymagane)", className = "form-label text-light fw-bold")
                            val fileInput = tag(TAG.INPUT, className = "form-control bg-dark text-white border-secondary mb-3") {
                                setAttribute("type", "file"); setAttribute("accept", "video/*")
                            }
                            tag(TAG.BUTTON, "Opublikuj zadanie", className = "btn dance-btn-primary w-100 mt-3") {
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
                                        ApiService.createTask(title, desc, deadline, choreoId, file).then { response: dynamic ->
                                            val localUrl = URL.createObjectURL(file as Blob)
                                            val newId = response.id?.toString()?.toIntOrNull() ?: (DataManager.globalTasks.size + 100)
                                            DataManager.globalTasks.add(0, ChoreoTask(
                                                id = newId,
                                                title = response.title?.toString() ?: title,
                                                description = response.description?.toString() ?: desc,
                                                instructionVideoUrl = localUrl,
                                                assignedDancers = selected.toList()
                                            ))
                                            showToast("✔ Zadanie zapisane w bazie!")
                                            taskTitleInput.value = null; taskDescInput.value = null
                                            fileInput.getElement()?.asDynamic().value = ""
                                            selected.clear(); refresh()
                                            null
                                        }.catch { _: Throwable -> window.alert("Błąd połączenia z serwerem."); null }
                                    } else {
                                        window.alert("Wypełnij tytuł i dodaj wideo!")
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
                            if (tasks.isEmpty()) li(className = "list-group-item bg-dark text-muted border-secondary") { span("Brak zadań.") }
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
            p(task.description.ifBlank { "Brak opisu." }) { setStyle("color", "#000000"); addCssClass("mb-3") }

            if (task.instructionVideoUrl != null) {
                h6("Wideo wzorcowe:", className = "text-primary-dance fw-bold mb-2")
                tag(TAG.VIDEO, className = "w-100 rounded border border-secondary mb-3") {
                    setAttribute("controls", "controls"); setAttribute("style", "max-height:300px; background:#000;")
                    tag(TAG.SOURCE) { setAttribute("src", task.instructionVideoUrl) }
                }
            } else {
                p("Brak wideo wzorcowego.") { setStyle("color", "#6c757d"); addCssClass("fst-italic mb-3") }
            }

            div(className = "mt-4 pt-3 border-top border-secondary text-center") {
                h6("Zarządzaj zadaniem:", className = "text-danger fw-bold mb-2")
                tag(TAG.BUTTON, "🗑 Usuń to zadanie (dla wszystkich)", className = "btn btn-outline-danger btn-sm w-100") {
                    onClick {
                        if (window.confirm("Czy na pewno chcesz całkowicie usunąć to zadanie? Tancerze stracą do niego dostęp.")) {
                            ApiService.deleteTaskAPI(task.id).then { _: dynamic ->
                                DataManager.globalTasks.remove(task)
                                modal.hide()
                                showToast("✔ Zadanie usunięte z bazy.")
                                null
                            }.catch { _: Throwable ->
                                window.alert("Błąd połączenia. Upewnij się, że backend ma gotowy endpoint DELETE /tasks/{id}.")
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

        ApiService.fetchTasks().then { res: dynamic ->
            val taskList = res as Array<dynamic>
            tasks.addAll(taskList)
            val taskIds = taskList.mapNotNull { it.id?.toString()?.toIntOrNull() }
            if (taskIds.isEmpty()) { loading.value = false; return@then null }
            fun fetchNext(index: Int) {
                if (index >= taskIds.size) { loading.value = false; return }
                ApiService.fetchSubmissionsForTask(taskIds[index]).then { r: dynamic ->
                    val list = (r as Array<dynamic>).filter { it.status?.toString() == "GRADED" }
                    subs.addAll(list); fetchNext(index + 1); null
                }.catch { _: Throwable -> fetchNext(index + 1); null }
            }
            fetchNext(0); null
        }.catch { _: Throwable -> loading.value = false; null }

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.CHOREO_DASHBOARD)
            h2("Archiwum ocenionych nagrań", className = "fw-bold mb-4")
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
                                        p("Jeszcze nic nie oceniłeś.", className = "text-muted")
                                    }
                                } else {
                                    table(className = "table table-dark table-hover align-middle") {
                                        thead { tr { th("Nagranie"); th("Tancerz"); th("Zadanie"); th("Ocena"); th("Feedback"); th("Akcja") } }
                                        tbody {
                                            list.forEach { s ->
                                                val subId = s.id?.toString()?.toIntOrNull() ?: 0
                                                val videoUrl = s.videoUrl?.toString() ?: ""
                                                val taskId = s.taskId?.toString()?.toIntOrNull() ?: 0
                                                val taskObj = tasks.find { it.id?.toString()?.toIntOrNull() == taskId }
                                                val instrUrl = taskObj?.instructionVideoUrl?.toString() ?: ""
                                                tr {
                                                    td("Wideo #${s.id}")
                                                    td("Tancerz #${s.dancerId}")
                                                    td(taskObj?.title?.toString() ?: "Zadanie #$taskId")
                                                    td { span(s.score?.toString() ?: "—", className = "badge bg-success fs-6") }
                                                    td(s.feedback?.toString()?.take(40)?.let { if (it.length == 40) "$it..." else it } ?: "—")
                                                    td {
                                                        tag(TAG.BUTTON, "Zobacz", className = "btn btn-sm dance-btn-primary") {
                                                            onClick {
                                                                PlayerState.submissionId = subId
                                                                PlayerState.submissionVideoUrl = toVideoUrl(videoUrl)
                                                                PlayerState.instructionVideoUrl = toVideoUrl(instrUrl)
                                                                PlayerState.taskTitle = taskObj?.title?.toString() ?: "Zadanie #$taskId"
                                                                PlayerState.taskId = taskId
                                                                PlayerState.isGraded = true
                                                                PlayerState.currentScore = s.score?.toString()?.toIntOrNull()
                                                                PlayerState.currentFeedback = s.feedback?.toString()
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

    // ==========================================
    // ADMIN
    // ==========================================
    private fun Container.buildAdminPanel() {
        div(className = "container py-5 mt-5") {
            h2("Panel Admina", className = "fw-bold text-primary-dance mb-4 pt-4")
            div(className = "row g-4") {
                dashboardCard("fa-user-check", "Zatwierdzanie kont", "Aktywuj lub dezaktywuj użytkowników.") { appState.value = Page.ADMIN_VERIFY }
                dashboardCard("fa-users-cog", "Użytkownicy", "Lista wszystkich zarejestrowanych kont.") { appState.value = Page.ADMIN_USERS }
                dashboardCard("fa-database", "Moderacja treści", "Usuń niepożądane nagrania.") { appState.value = Page.ADMIN_MODERATION }
            }
        }
    }

    private fun Container.buildAdminVerify() {
        val usersList = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)
        ApiService.fetchUsers().then { res: dynamic -> usersList.addAll(res as Array<dynamic>); loading.value = false; null }
            .catch { _: Throwable -> loading.value = false; null }

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.ADMIN_PANEL)
            h2("Zatwierdzanie kont", className = "fw-bold mb-4")
            div {
                bind(loading) { isLoading ->
                    if (isLoading) {
                        div(className = "text-center py-5") { tag(TAG.DIV, className = "spinner-border text-danger") { setAttribute("role", "status") } }
                    } else {
                        div {
                            bind(usersList) { list ->
                                if (list.isEmpty()) { p("Brak użytkowników.", className = "text-muted") }
                                else {
                                    table(className = "table table-dark table-hover") {
                                        thead { tr { th("ID"); th("Email"); th("Rola"); th("Status"); th("Akcja") } }
                                        tbody {
                                            list.forEach { u ->
                                                tr {
                                                    td(u.id?.toString() ?: "—"); td(u.email?.toString() ?: "—"); td(u.role?.toString() ?: "—")
                                                    td {
                                                        val active = u.isActive == true
                                                        span(if (active) "Aktywny" else "Nieaktywny", className = if (active) "badge bg-success" else "badge bg-danger")
                                                    }
                                                    td {
                                                        val userId = u.id?.toString()?.toIntOrNull()
                                                        val active = u.isActive == true
                                                        if (userId != null) {
                                                            tag(TAG.BUTTON, if (active) "Dezaktywuj" else "Aktywuj",
                                                                className = if (active) "btn btn-sm btn-outline-danger" else "btn btn-sm btn-outline-success") {
                                                                onClick {
                                                                    ApiService.setUserStatus(userId, !active).then { _: dynamic ->
                                                                        usersList.clear()
                                                                        ApiService.fetchUsers().then { res: dynamic -> usersList.addAll(res as Array<dynamic>); null }.catch { _: Throwable -> null }
                                                                        null
                                                                    }.catch { _: Throwable -> window.alert("Błąd zmiany statusu."); null }
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

    private fun Container.buildAdminUsers() {
        val usersList = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)
        ApiService.fetchUsers().then { res: dynamic -> usersList.addAll(res as Array<dynamic>); loading.value = false; null }
            .catch { _: Throwable -> loading.value = false; null }

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.ADMIN_PANEL)
            h2("Użytkownicy Platformy", className = "mb-4")
            div {
                bind(loading) { isLoading ->
                    if (isLoading) {
                        div(className = "text-center py-5") { tag(TAG.DIV, className = "spinner-border text-danger") { setAttribute("role", "status") } }
                    } else {
                        div {
                            bind(usersList) { list ->
                                if (list.isEmpty()) { p("Brak użytkowników.", className = "text-muted") }
                                else {
                                    table(className = "table table-dark table-hover") {
                                        thead { tr { th("ID"); th("Imię"); th("Nazwisko"); th("Email"); th("Rola"); th("Aktywny") } }
                                        tbody {
                                            list.forEach { u ->
                                                tr {
                                                    td(u.id?.toString() ?: "—"); td(u.firstName?.toString() ?: "—")
                                                    td(u.lastName?.toString() ?: "—"); td(u.email?.toString() ?: "—")
                                                    td(u.role?.toString() ?: "—"); td(if (u.isActive == true) "Tak" else "Nie")
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

    private fun Container.buildAdminModeration() {
        val submissionsList = io.kvision.state.ObservableListWrapper<dynamic>()
        val loading = ObservableValue(true)

        fun loadAll() {
            ApiService.fetchTasks().then { res: dynamic ->
                val taskIds = (res as Array<dynamic>).mapNotNull { it.id?.toString()?.toIntOrNull() }
                if (taskIds.isEmpty()) { loading.value = false; return@then null }
                fun fetchNext(index: Int) {
                    if (index >= taskIds.size) { loading.value = false; return }
                    ApiService.fetchSubmissionsForTask(taskIds[index]).then { r: dynamic ->
                        submissionsList.addAll(r as Array<dynamic>); fetchNext(index + 1); null
                    }.catch { _: Throwable -> fetchNext(index + 1); null }
                }
                fetchNext(0); null
            }.catch { _: Throwable -> loading.value = false; null }
        }
        loadAll()

        div(className = "container py-5 mt-5 pt-5") {
            backButton(appState, Page.ADMIN_PANEL)
            h2("Moderacja treści", className = "fw-bold mb-4")
            div {
                bind(loading) { isLoading ->
                    if (isLoading) {
                        div(className = "text-center py-5") { tag(TAG.DIV, className = "spinner-border text-danger") { setAttribute("role", "status") } }
                    } else {
                        div {
                            bind(submissionsList) { list ->
                                if (list.isEmpty()) { div(className = "card bg-dark border-secondary p-5 text-center") { p("Brak nagrań.", className = "text-muted") } }
                                else {
                                    table(className = "table table-dark table-hover align-middle") {
                                        thead { tr { th("ID"); th("Tancerz"); th("Zadanie"); th("Data"); th("Status"); th("Akcja") } }
                                        tbody {
                                            list.forEach { s ->
                                                val subId = s.id?.toString()?.toIntOrNull()
                                                val status = s.status?.toString() ?: "SUBMITTED"
                                                tr {
                                                    td("Wideo #${s.id}"); td("Tancerz #${s.dancerId}"); td("Zadanie #${s.taskId}")
                                                    td(s.sentAt?.toString()?.substring(0, 10) ?: "—")
                                                    td { span(status, className = if (status == "GRADED") "badge bg-success" else "badge bg-warning text-dark") }
                                                    td {
                                                        if (subId != null) {
                                                            tag(TAG.BUTTON, "Usuń", className = "btn btn-sm btn-danger") {
                                                                onClick {
                                                                    if (window.confirm("Na pewno usunąć nagranie #$subId?")) {
                                                                        ApiService.deleteSubmissionAPI(subId).then { _: dynamic ->
                                                                            submissionsList.removeAll { it.id?.toString()?.toIntOrNull() == subId }
                                                                            showToast("✔ Nagranie usunięte."); null
                                                                        }.catch { _: Throwable -> window.alert("Błąd usuwania."); null }
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
            ApiService.fetchComments(subId).then { res: dynamic ->
                comments.addAll(res as Array<dynamic>); commentsLoading.value = false; null
            }.catch { _: Throwable -> commentsLoading.value = false; null }
        } else {
            commentsLoading.value = false
        }

        val backPage = when (role) {
            "CHOREOGRAPHER" -> if (isGraded) Page.CHOREO_ARCHIVE else Page.CHOREO_QUEUE
            "DANCER" -> Page.DANCER_SUBMISSIONS
            "ADMIN" -> Page.ADMIN_MODERATION
            else -> Page.HOME
        }

        div(className = "container-fluid py-5 mt-5 pt-4 px-4") {
            backButton(appState, backPage)
            h2(PlayerState.taskTitle.ifBlank { "Analiza Video" }, className = "fw-bold mb-3")

            // === WIDEO(A) ===
            if (hasSideBySide) {
                div(className = "row g-2 mb-3") {
                    div(className = "col-md-6") {
                        // ZMIANA: Wyraźny nagłówek "Nagranie tancerza"
                        div(className = "text-center mb-2") {
                            h5("Nagranie tancerza", className = "fw-bold text-primary-dance mb-0")
                        }
                        tag(TAG.VIDEO, className = "w-100 rounded border border-primary-dance bg-black") {
                            setAttribute("controls", "controls")
                            setAttribute("style", "max-height: 380px;")
                            setAttribute("id", "submission-player")
                            tag(TAG.SOURCE) { setAttribute("src", submissionUrl); setAttribute("type", "video/mp4") }
                        }
                    }
                    div(className = "col-md-6") {
                        // ZMIANA: Wyraźny nagłówek "Wideo instruktora"
                        div(className = "text-center mb-2") {
                            h5("Wideo instruktora", className = "fw-bold text-light mb-0")
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
                    tag(TAG.BUTTON, "▶ Odtwórz oba", className = "btn btn-sm dance-btn-primary fw-bold") {
                        onClick {
                            val v1 = document.getElementById("submission-player") as? HTMLVideoElement
                            val v2 = document.getElementById("instruction-player") as? HTMLVideoElement
                            v1?.play(); v2?.play()
                        }
                    }
                    tag(TAG.BUTTON, "⏸ Pauza obu", className = "btn btn-sm btn-light text-dark fw-bold") {
                        onClick {
                            val v1 = document.getElementById("submission-player") as? HTMLVideoElement
                            val v2 = document.getElementById("instruction-player") as? HTMLVideoElement
                            v1?.pause(); v2?.pause()
                        }
                    }
                    tag(TAG.BUTTON, "↺ Reset", className = "btn btn-sm btn-light text-dark fw-bold") {
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
                    h5("🕺 Nagranie tancerza", className = "fw-bold text-primary-dance mb-0")
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
                            tag(TAG.I, className = "fa-solid fa-film fa-3x mb-3"); p("Brak pliku wideo")
                        }
                    }
                }
                if (isGraded) {
                    div(className = "card bg-dark border-success mb-3 p-3") {
                        div(className = "d-flex align-items-center mb-2") {
                            tag(TAG.I, className = "fa-solid fa-star text-warning me-2")
                            h5("Ocena: ${PlayerState.currentScore ?: "—"}", className = "mb-0 text-white")
                        }
                        p(PlayerState.currentFeedback ?: "Brak feedbacku.", className = "text-light mb-0")
                    }
                }
            }

            // === PRAWA KOLUMNA: Ocenianie + Komentarze ===
            div(className = "row g-3") {
                if (role == "CHOREOGRAPHER" && !isGraded && subId > 0) {
                    div(className = "col-lg-4") {
                        div(className = "card bg-dark border-primary-dance p-3") {
                            h5("Oceń nagranie", className = "fw-bold mb-3 text-primary-dance")
                            label("Ocena (1-10):", className = "form-label text-light small")
                            val scoreInput = tag(TAG.INPUT, className = "form-control bg-dark text-white border-secondary mb-2") {
                                setAttribute("type", "number"); setAttribute("min", "1"); setAttribute("max", "10"); setAttribute("placeholder", "np. 8")
                            }
                            label("Feedback:", className = "form-label text-light small")
                            val feedbackInput = tag(TAG.TEXTAREA, className = "form-control bg-dark text-white border-secondary mb-3") {
                                setAttribute("rows", "3"); setAttribute("placeholder", "Napisz swoje uwagi...")
                            }
                            div { bind(gradeSuccess) { ok -> if (ok) div(className = "alert alert-success py-2 mb-2") { span("✔ Ocena zapisana!") } } }
                            tag(TAG.BUTTON, "Zapisz ocenę", className = "btn dance-btn-primary w-100 fw-bold") {
                                onClick {
                                    val scoreVal = scoreInput.getElement()?.asDynamic().value?.toString()?.toIntOrNull()
                                    val feedbackVal = feedbackInput.getElement()?.asDynamic().value?.toString() ?: ""
                                    if (scoreVal == null || scoreVal < 1 || scoreVal > 10) { window.alert("Podaj ocenę 1-10!"); return@onClick }
                                    ApiService.gradeSubmission(subId, scoreVal, feedbackVal).then { _: dynamic ->
                                        gradeSuccess.value = true
                                        PlayerState.isGraded = true; PlayerState.currentScore = scoreVal; PlayerState.currentFeedback = feedbackVal
                                        null
                                    }.catch { _: Throwable -> window.alert("Błąd zapisywania oceny."); null }
                                }
                            }
                        }
                    }
                }

                div(className = if (role == "CHOREOGRAPHER" && !isGraded) "col-lg-8" else "col-12") {
                    div(className = "card bg-dark border-secondary p-3") {
                        h5("Komentarze czasowe", className = "fw-bold mb-3")

                        if (role == "CHOREOGRAPHER" && subId > 0) {
                            div(className = "mb-3 p-3 rounded border border-secondary bg-dark") {
                                label("Napisz komentarz (sekunda zapisze się automatycznie):", className = "form-label text-white fw-bold mb-2")

                                // ZMIANA: Powrót do bezpiecznego TAG.TEXTAREA aby uniknąć błędów kompilacji
                                val commentInput = tag(TAG.TEXTAREA, className = "form-control bg-dark text-white border-secondary mb-3") {
                                    setAttribute("id", "comment-input")
                                    setAttribute("placeholder", "np. wyprostuj nogę, trzymaj tempo...")
                                    setAttribute("rows", "2")
                                }

                                tag(TAG.BUTTON, "Dodaj komentarz do bieżącej sekundy", className = "btn dance-btn-primary btn-sm w-100 fw-bold") {
                                    onClick {
                                        val content = commentInput.getElement()?.asDynamic().value?.toString() ?: ""
                                        if (content.isBlank()) { window.alert("Wpisz treść komentarza!"); return@onClick }

                                        val video = document.getElementById("submission-player") as? HTMLVideoElement
                                        val currentSec = video?.currentTime?.toInt() ?: 0

                                        ApiService.addComment(subId, myUserId, currentSec, content).then { res: dynamic ->
                                            comments.add(res)
                                            commentInput.getElement()?.asDynamic().value = ""
                                            showToast("✔ Komentarz dodany do ${formatTime(currentSec)}")
                                            null
                                        }.catch { _: Throwable -> window.alert("Błąd dodawania komentarza."); null }
                                    }
                                }
                            }
                        }

                        div(className = "comments-list") {
                            div {
                                bind(commentsLoading) { loading ->
                                    if (loading) {
                                        p("Ładowanie komentarzy...", className = "text-muted small")
                                    } else {
                                        div {
                                            bind(comments) { list ->
                                                if (list.isEmpty()) {
                                                    p("Brak komentarzy.", className = "text-muted small fst-italic")
                                                } else {
                                                    val sorted = list.sortedBy { it.timestampSeconds?.toString()?.toLongOrNull() ?: 0L }
                                                    sorted.forEach { c ->
                                                        val sec = c.timestampSeconds?.toString()?.toIntOrNull() ?: 0
                                                        val time = c.formattedTime?.toString() ?: formatTime(sec)
                                                        val content = c.content?.toString() ?: ""

                                                        div(className = "comment-item mb-2 p-2 rounded border border-secondary hover-card") {
                                                            div(className = "d-flex align-items-center mb-1") {
                                                                span(time, className = "badge bg-primary-dance me-2 font-monospace") {
                                                                    setStyle("cursor", "pointer")
                                                                    title = "Kliknij, aby przejść do tej sekundy"
                                                                    onClick {
                                                                        val video = document.getElementById("submission-player") as? HTMLVideoElement
                                                                        if (video != null) {
                                                                            video.currentTime = sec.toDouble()
                                                                            video.play()
                                                                        }
                                                                    }
                                                                }
                                                                small("Choreograf", className = "text-muted")
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
}

fun toVideoUrl(path: String?): String {
    if (path.isNullOrBlank()) return ""
    if (path.startsWith("http")) return path
    val normalized = path.replace("\\", "/")
    val clean = normalized.trimStart('/')
    return "http://localhost:8080/$clean"
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