package com.licencjat.licencjat_frontend

import io.kvision.core.*
import io.kvision.html.*
import io.kvision.panel.*
import io.kvision.state.ObservableValue
import io.kvision.state.bind
import io.kvision.modal.Modal
import kotlinx.browser.window

fun Container.buildDancerDashboard(appState: ObservableValue<Page>) {
    div(className = "container py-5 mt-5") {
        h2("Panel Tancerza", className = "fw-bold mb-4 pt-4")
        div(className = "row g-4") {
            dashboardCard("fa-list-check", "Dostępne zadania", "Wybierz wyzwanie i obejrzyj instrukcje.") { appState.value = Page.DANCER_TASKS }
            dashboardCard("fa-video", "Moje nagrania", "Przeglądaj wgrane filmy i statusy ocen.") { appState.value = Page.DANCER_SUBMISSIONS }
            dashboardCard("fa-chart-line", "Statystyki", "Śledź swój progres i wyniki.") { appState.value = Page.DANCER_STATS }
            dashboardCard("fa-comments", I18n.tr("Wiadomości", "Messages"), I18n.tr("Messenger społeczności.", "Community messenger.")) { appState.value = Page.CHAT}
        }
    }
}

// ==========================================
// DOSTĘPNE ZADANIA (Podział: Oczekujące / Wykonane)
// ==========================================
fun Container.buildDancerTasks(appState: ObservableValue<Page>) {
    val tasks = io.kvision.state.ObservableListWrapper<dynamic>()
    val subs = io.kvision.state.ObservableListWrapper<dynamic>()
    val loading = ObservableValue(true)
    val myDancerId = window.localStorage.getItem("userId") ?: "1"

    // Pobieramy zadania i submisje by wiedzieć, co zostało już wykonane
    ApiService.fetchTasks().then<dynamic> { resTasks: dynamic ->
        tasks.addAll(resTasks as Array<dynamic>)
        ApiService.fetchSubmissionsForDancer(myDancerId.toIntOrNull() ?: 1).then<dynamic> { resSubs: dynamic ->
            subs.addAll(resSubs as Array<dynamic>)
            loading.value = false
            null
        }.catch<dynamic> { _: Throwable -> loading.value = false; null }
        null
    }.catch<dynamic> { _: Throwable -> loading.value = false; null }

    div(className = "container py-5 mt-5 pt-5") {
        backButton(appState, Page.DANCER_DASHBOARD)
        h2("Dostępne zadania", className = "fw-bold mb-4")

        div {
            bind(loading) { isLoading ->
                if (isLoading) {
                    div(className = "text-center py-5") {
                        tag(TAG.DIV, className = "spinner-border text-primary-dance") { setAttribute("role", "status") }
                        p("Ładowanie zadań...", className = "text-muted mt-3")
                    }
                } else {
                    div {
                        bind(tasks) { taskList ->
                            bind(subs) { subList ->
                                if (taskList.isEmpty()) {
                                    div(className = "card bg-dark border-secondary p-5 text-center") {
                                        tag(TAG.I, className = "fa-solid fa-inbox fa-3x text-muted mb-3")
                                        p("Brak dostępnych zadań. Trener jeszcze nic nie dodał.", className = "text-muted")
                                    }
                                } else {
                                    // Mapujemy id zadań, które tancerz już wysłał
                                    val submittedTaskIds = subList.mapNotNull { it.taskId?.toString()?.toIntOrNull() }
                                    val pendingTasks = taskList.filter { (it.id?.toString()?.toIntOrNull() ?: -1) !in submittedTaskIds }
                                    val completedTasks = taskList.filter { (it.id?.toString()?.toIntOrNull() ?: -1) in submittedTaskIds }

                                    // --- 1. OCZEKUJĄCE NA ZROBIENIE ---
                                    div(className = "mb-5") {
                                        h4("⏳ Oczekujące na zrobienie", className = "fw-bold mb-3 border-bottom border-secondary pb-2")
                                        if (pendingTasks.isEmpty()) {
                                            p("Super! Nie masz żadnych zaległych zadań.", className = "text-muted")
                                        } else {
                                            ul(className = "list-group bg-dark shadow-sm col-md-10") {
                                                pendingTasks.forEach { task ->
                                                    val taskId = task.id?.toString()?.toIntOrNull() ?: 0
                                                    val taskTitle = task.title?.toString() ?: "Zadanie #$taskId"
                                                    val taskDesc = task.description?.toString() ?: ""
                                                    val videoUrl = task.instructionVideoUrl?.toString()
                                                    val deadline = task.deadline?.toString()?.substring(0, 10) ?: "—"

                                                    li(className = "list-group-item bg-dark text-white border-secondary hover-card mb-2 p-3") {
                                                        div(className = "d-flex justify-content-between align-items-center") {
                                                            div {
                                                                h5(taskTitle, className = "fw-bold text-primary-dance mb-1")
                                                                small("Termin: $deadline", className = "text-muted")
                                                            }
                                                            tag(TAG.BUTTON, "Zobacz i wykonaj", className = "btn btn-sm dance-btn-primary") {
                                                                onClick {
                                                                    showDancerTaskDetailsFromBackend(taskId, taskTitle, taskDesc, videoUrl, myDancerId.toIntOrNull() ?: 1, appState)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // --- 2. WYKONANE ---
                                    div(className = "mb-4") {
                                        h4("✅ Wykonane", className = "fw-bold mb-3 border-bottom border-secondary pb-2 text-success")
                                        if (completedTasks.isEmpty()) {
                                            p("Brak zrobionych zadań.", className = "text-muted")
                                        } else {
                                            ul(className = "list-group bg-dark shadow-sm col-md-10") {
                                                completedTasks.forEach { task ->
                                                    val taskId = task.id?.toString()?.toIntOrNull() ?: 0
                                                    val taskTitle = task.title?.toString() ?: "Zadanie #$taskId"
                                                    val deadline = task.deadline?.toString()?.substring(0, 10) ?: "—"

                                                    li(className = "list-group-item bg-dark text-white border-secondary hover-card mb-2 p-3") {
                                                        div(className = "d-flex justify-content-between align-items-center") {
                                                            div {
                                                                h5(taskTitle, className = "fw-bold text-success mb-1")
                                                                small("Termin: $deadline", className = "text-muted")
                                                            }
                                                            tag(TAG.BUTTON, "Szczegóły", className = "btn btn-sm btn-outline-success") {
                                                                onClick {
                                                                    showCompletedTaskMessage(taskTitle, appState)
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

// Funkcja pokazująca komunikat po kliknięciu w "Wykonane" zadanie
fun showCompletedTaskMessage(taskTitle: String, appState: ObservableValue<Page>) {
    val modal = Modal("Status zadania", closeButton = true, animation = true)
    modal.div(className = "p-4 text-center") {
        tag(TAG.I, className = "fa-solid fa-check-circle fa-3x text-success mb-3")
        h5("Zadanie \"$taskTitle\" zostało już wysłane!", className = "fw-bold text-white mb-3")
        p("Oczekuj na feedback od choreografa.", className = "text-light mb-4")
        p("Możesz sprawdzić status swojego zadania i ewentualne oceny w zakładce \"Moje nagrania\".", className = "text-muted small")

        tag(TAG.BUTTON, "Przejdź do 'Moje nagrania'", className = "btn dance-btn-primary w-100") {
            onClick {
                modal.hide()
                appState.value = Page.DANCER_SUBMISSIONS
            }
        }
    }
    modal.show()
}

fun showDancerTaskDetailsFromBackend(taskId: Int, title: String, description: String, videoUrl: String?, dancerId: Int, appState: ObservableValue<Page>) {
    val modal = Modal("Wyzwanie: $title", closeButton = true, animation = true)
    modal.div(className = "p-3") {
        h6("Instrukcje:", className = "text-primary-dance fw-bold mb-1")
        p(description.ifBlank { "Brak opisu." }) { setStyle("color", "#000000"); addCssClass("mb-3") }

        if (!videoUrl.isNullOrBlank()) {
            h6("Wideo instruktażowe:", className = "text-primary-dance fw-bold mb-2")
            val fullUrl = toVideoUrl(videoUrl)
            tag(TAG.VIDEO, className = "w-100 rounded border border-secondary mb-3") {
                setAttribute("controls", "controls"); setAttribute("style", "max-height:300px; background:#000;")
                tag(TAG.SOURCE) { setAttribute("src", fullUrl); setAttribute("type", "video/mp4") }
            }
        } else {
            p("Brak wideo wzorcowego.") { setStyle("color", "#6c757d"); addCssClass("fst-italic mb-3") }
        }

        div(className = "mt-4 border-top pt-3 border-secondary text-center") {
            val fileInput = tag(TAG.INPUT) {
                setAttribute("type", "file"); setAttribute("accept", "video/*"); setStyle("display", "none")
            }
            tag(TAG.BUTTON, "Wgraj swoje rozwiązanie \uD83C\uDFA5", className = "btn dance-btn-primary w-100 fw-bold") {
                onClick { fileInput.getElement()?.asDynamic().click() }
            }

            fileInput.onEvent {
                change = {
                    val f = fileInput.getElement()?.asDynamic().files
                    if (f != null && f.length > 0) {
                        ApiService.uploadVideoForTask(f[0], taskId, dancerId).then<dynamic> { _: dynamic ->
                            showToast("✔ Nagranie wysłane do trenera!")
                            modal.hide()
                            // Automatyczne przejście do "Moje nagrania" po wysłaniu
                            appState.value = Page.DANCER_SUBMISSIONS
                            null
                        }.catch<dynamic> { _: Throwable -> window.alert("Błąd wgrywania nagrania. Sprawdź połączenie."); null }
                    }
                }
            }
        }
    }
    modal.show()
}

// ==========================================
// MOJE NAGRANIA — podzielone na ocenione/nieocenione
// ==========================================
fun Container.buildDancerSubmissions(appState: ObservableValue<Page>) {
    val subs = io.kvision.state.ObservableListWrapper<dynamic>()
    val tasks = io.kvision.state.ObservableListWrapper<dynamic>()
    val loading = ObservableValue(true)
    val errorText = ObservableValue("")

    val dancerId = window.localStorage.getItem("userId")?.toIntOrNull() ?: 1

    ApiService.fetchTasks().then<dynamic> { resTasks: dynamic ->
        val taskList = resTasks as Array<dynamic>
        tasks.addAll(taskList)
        val taskIds = taskList.mapNotNull { it.id?.toString()?.toIntOrNull() }

        if (taskIds.isEmpty()) {
            loading.value = false
            return@then null
        }

        fun fetchNext(index: Int) {
            if (index >= taskIds.size) {
                loading.value = false; return
            }
            ApiService.fetchSubmissionsForTask(taskIds[index]).then<dynamic> { r: dynamic ->
                val list = (r as Array<dynamic>).filter { it.dancerId?.toString() == dancerId.toString() }
                subs.addAll(list)
                fetchNext(index + 1)
                null
            }.catch<dynamic> { _: Throwable -> fetchNext(index + 1); null }
        }
        fetchNext(0)
        null
    }.catch<dynamic> { _: Throwable ->
        errorText.value = "Nie udało się załadować nagrań."
        loading.value = false
        null
    }

    div(className = "container py-5 mt-5 pt-5") {
        backButton(appState, Page.DANCER_DASHBOARD)
        h2("Moje nagrania", className = "fw-bold mb-4")

        div {
            bind(loading) { isLoading ->
                if (isLoading) {
                    div(className = "text-center py-5") { tag(TAG.DIV, className = "spinner-border text-primary-dance") }
                } else {
                    if (errorText.value.isNotBlank()) {
                        div(className = "alert alert-danger", content = errorText.value)
                    }
                    val currentList = subs.toList()
                    if (currentList.isEmpty()) {
                        div(className = "card bg-dark border-secondary p-5 text-center") {
                            tag(TAG.I, className = "fa-solid fa-video-slash fa-3x text-muted mb-3")
                            p("Nie wgrałeś jeszcze żadnych filmów.", className = "text-muted")
                        }
                    } else {
                        val graded = currentList.filter { it.status?.toString() == "GRADED" }
                        val pending = currentList.filter { it.status?.toString() != "GRADED" }

                        // 1. OCENIONE ZADANIA
                        if (graded.isNotEmpty()) {
                            div(className = "mb-5") {
                                div(className = "d-flex align-items-center mb-3") {
                                    tag(TAG.I, className = "fa-solid fa-check-circle text-success me-2 fa-lg")
                                    h4("Ocenione (${graded.size})", className = "fw-bold mb-0 text-success")
                                }
                                div(className = "row g-4") {
                                    graded.forEach { s ->
                                        val subId = s.id?.toString()?.toIntOrNull() ?: 0
                                        val tId = s.taskId?.toString()?.toIntOrNull() ?: 0
                                        val taskObj = tasks.find { it.id?.toString()?.toIntOrNull() == tId }
                                        val taskTitle = taskObj?.title?.toString() ?: "Zadanie #$tId"
                                        val score = s.score?.toString() ?: "—"

                                        div(className = "col-md-6") {
                                            div(className = "card bg-dark border-success h-100 p-4 hover-card d-flex flex-column") {
                                                h4(taskTitle, className = "fw-bold mb-3 text-white")
                                                span("Ocena: $score/10", className = "badge bg-success fs-5 mb-4 align-self-start")

                                                // Komentarze czasowe od choreografa
                                                val commentsData = io.kvision.state.ObservableListWrapper<dynamic>()
                                                val commLoading = ObservableValue(true)
                                                ApiService.fetchComments(subId).then<dynamic> { cRes: dynamic ->
                                                    commentsData.addAll(cRes as Array<dynamic>)
                                                    commLoading.value = false
                                                    null
                                                }.catch<dynamic> { _: Throwable -> commLoading.value = false; null }

                                                div(className = "mb-4 flex-grow-1") {
                                                    h6("Komentarze trenera:", className = "text-primary-dance fw-bold small mb-2")
                                                    div {
                                                        bind(commLoading) { cLoading ->
                                                            if (cLoading) {
                                                                p("Ładowanie uwag...", className = "text-muted small")
                                                            } else {
                                                                val cList = commentsData.toList()
                                                                if (cList.isEmpty()) {
                                                                    p("Brak uwag czasowych.", className = "text-muted small")
                                                                } else {
                                                                    div(className = "bg-black rounded p-3 border border-secondary") {
                                                                        cList.sortedBy { it.timestampSeconds?.toString()?.toIntOrNull() ?: 0 }.forEach { c ->
                                                                            val sec = c.timestampSeconds?.toString()?.toIntOrNull() ?: 0
                                                                            div(className = "d-flex mb-2 align-items-start") {
                                                                                span(formatTime(sec), className = "badge bg-secondary me-2 mt-1")
                                                                                span(c.content?.toString() ?: "", className = "small text-light")
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                tag(TAG.BUTTON, "Odtwórz wideo (Side-by-Side)", className = "btn dance-btn-primary w-100 mt-auto fw-bold") {
                                                    onClick {
                                                        PlayerState.submissionId = subId
                                                        PlayerState.submissionVideoUrl = toVideoUrl(s.videoUrl?.toString())
                                                        PlayerState.instructionVideoUrl = toVideoUrl(taskObj?.instructionVideoUrl?.toString())
                                                        PlayerState.taskTitle = taskTitle
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

                            // 2. NIEOCENIONE ZADANIA
                            if (pending.isNotEmpty()) {
                                div(className = "mb-4") {
                                    div(className = "d-flex align-items-center mb-3") {
                                        tag(TAG.I, className = "fa-solid fa-clock text-warning me-2 fa-lg")
                                        h4("Nieocenione (${pending.size})", className = "fw-bold mb-0 text-warning")
                                    }
                                    div(className = "row g-4") {
                                        pending.forEach { s ->
                                            val tId = s.taskId?.toString()?.toIntOrNull() ?: 0
                                            val taskObj = tasks.find { it.id?.toString()?.toIntOrNull() == tId }

                                            div(className = "col-md-6 col-lg-4") {
                                                div(className = "card bg-dark border-warning h-100 p-4 hover-card") {
                                                    // DODANO text-white W TYTULE
                                                    h5(taskObj?.title?.toString() ?: "Zadanie #$tId", className = "fw-bold mb-3 text-white")

                                                    span("oczekujące na feedback choreografa", className = "badge bg-warning text-dark fs-6 mb-4 text-wrap lh-base")

                                                    tag(TAG.BUTTON, "Zobacz swoje wideo", className = "btn btn-outline-warning w-100 mt-auto fw-bold") {
                                                        onClick {
                                                            PlayerState.submissionId = s.id?.toString()?.toIntOrNull() ?: 0
                                                            PlayerState.submissionVideoUrl = toVideoUrl(s.videoUrl?.toString())
                                                            PlayerState.instructionVideoUrl = toVideoUrl(taskObj?.instructionVideoUrl?.toString())
                                                            PlayerState.taskTitle = taskObj?.title?.toString() ?: "Zadanie"
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

// ==========================================
// STATYSTYKI (PRZYWRÓCONY WYKRES CSS)
// ==========================================
fun Container.buildDancerStats(appState: ObservableValue<Page>) {
    val subs = io.kvision.state.ObservableListWrapper<dynamic>()
    val loading = ObservableValue(true)
    val dancerId = window.localStorage.getItem("userId")?.toIntOrNull() ?: 1

    ApiService.fetchSubmissionsForDancer(dancerId).then<dynamic> { res: dynamic ->
        subs.addAll(res as Array<dynamic>)
        loading.value = false
        null
    }.catch<dynamic> { _: Throwable -> loading.value = false; null }

    div(className = "container py-5 mt-5 pt-5") {
        backButton(appState, Page.DANCER_DASHBOARD)
        h2("Twoje Statystyki", className = "fw-bold mb-4")
        div {
            bind(loading) { isLoading ->
                if (isLoading) {
                    div(className = "text-center py-5") { tag(TAG.DIV, className = "spinner-border text-primary-dance") }
                } else {
                    div {
                        bind(subs) { list ->
                            val total = list.size
                            val graded = list.count { it.status?.toString() == "GRADED" }
                            val scores = list.mapNotNull { it.score?.toString()?.toIntOrNull() }
                            val avg = if (scores.isNotEmpty()) scores.average() else 0.0
                            val pending = total - graded

                            div(className = "row g-4 mb-5") {
                                div(className = "col-md-3") {
                                    div(className = "card bg-dark border-secondary p-4 text-center shadow-sm hover-card h-100") {
                                        tag(TAG.I, className = "fa-solid fa-video fa-2x text-primary-dance mb-3")
                                        h2(total.toString(), className = "fw-bold text-white mb-2")
                                        p("Wgranych filmów", className = "text-light small fw-bold mb-0 text-uppercase")
                                    }
                                }
                                div(className = "col-md-3") {
                                    div(className = "card bg-dark border-secondary p-4 text-center shadow-sm hover-card h-100") {
                                        tag(TAG.I, className = "fa-solid fa-check-circle fa-2x text-success mb-3")
                                        h2(graded.toString(), className = "fw-bold text-white mb-2")
                                        p("Ocenionych", className = "text-light small fw-bold mb-0 text-uppercase")
                                    }
                                }
                                div(className = "col-md-3") {
                                    div(className = "card bg-dark border-secondary p-4 text-center shadow-sm hover-card h-100") {
                                        tag(TAG.I, className = "fa-solid fa-clock fa-2x text-warning mb-3")
                                        h2(pending.toString(), className = "fw-bold text-white mb-2")
                                        p("Oczekujących", className = "text-light small fw-bold mb-0 text-uppercase")
                                    }
                                }
                                div(className = "col-md-3") {
                                    div(className = "card bg-dark border-secondary p-4 text-center shadow-sm hover-card h-100") {
                                        tag(TAG.I, className = "fa-solid fa-star fa-2x text-primary-dance mb-3")
                                        h2(if (avg > 0) (kotlin.math.round(avg * 10) / 10.0).toString() else "—", className = "fw-bold text-white mb-2")
                                        p("Średnia ocena", className = "text-light small fw-bold mb-0 text-uppercase")
                                    }
                                }
                            }

                            // WYKRES SŁUPKOWY CSS
                            if (total > 0) {
                                div(className = "row") {
                                    div(className = "col-md-8") {
                                        div(className = "card bg-dark border-secondary p-4") {
                                            h4("Ostatnie Oceny (Wykres Postępów)", className = "fw-bold text-white mb-4")

                                            if (scores.isEmpty()) {
                                                p("Musisz otrzymać przynajmniej jedną ocenę, aby zobaczyć wykres.", className = "text-muted")
                                            } else {
                                                div(className = "d-flex align-items-end justify-content-around mt-2") {
                                                    setAttribute("style", "height: 150px; border-bottom: 2px solid #555; padding-bottom: 5px;")
                                                    val recentScores = scores.takeLast(7) // Pokazuje max 7 ostatnich ocen

                                                    recentScores.forEach { score ->
                                                        div(className = "d-flex flex-column align-items-center w-100 px-1") {
                                                            p(score.toString(), className = "small text-light mb-2 fw-bold")
                                                            div(className = "bg-primary-dance rounded-top w-100") {
                                                                // Wysokość wykresu: ocena np. 8 * 12px = 96px
                                                                setAttribute("style", "height: ${score * 12}px; min-height: 5px; max-width: 40px; transition: height 0.5s ease;")
                                                            }
                                                        }
                                                    }
                                                }
                                                p("Chronologia od lewej do prawej", className = "text-center text-muted small mt-3 mb-0")
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