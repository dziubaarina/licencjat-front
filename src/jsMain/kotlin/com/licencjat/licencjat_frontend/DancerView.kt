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
        }
    }
}

// ==========================================
// DOSTĘPNE ZADANIA
// ==========================================
fun Container.buildDancerTasks(appState: ObservableValue<Page>) {
    val tasks = io.kvision.state.ObservableListWrapper<dynamic>()
    val loading = ObservableValue(true)
    val myDancerId = window.localStorage.getItem("userId") ?: "1"

    ApiService.fetchTasks().then { res: dynamic ->
        val list = res as Array<dynamic>
        tasks.addAll(list)
        loading.value = false
        null
    }.catch { _: Throwable ->
        loading.value = false
        null
    }

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
                            if (taskList.isEmpty()) {
                                div(className = "card bg-dark border-secondary p-5 text-center") {
                                    tag(TAG.I, className = "fa-solid fa-inbox fa-3x text-muted mb-3")
                                    p("Brak dostępnych zadań. Trener jeszcze nic nie dodał.", className = "text-muted")
                                }
                            } else {
                                ul(className = "list-group bg-dark shadow-sm col-md-10") {
                                    taskList.forEach { task ->
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
                                                        showDancerTaskDetailsFromBackend(
                                                            taskId = taskId,
                                                            title = taskTitle,
                                                            description = taskDesc,
                                                            videoUrl = videoUrl,
                                                            dancerId = myDancerId.toIntOrNull() ?: 1
                                                        )
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

fun showDancerTaskDetailsFromBackend(taskId: Int, title: String, description: String, videoUrl: String?, dancerId: Int) {
    val modal = Modal("Wyzwanie: $title", closeButton = true, animation = true)
    modal.div(className = "p-3") {
        h6("Instrukcje:", className = "text-primary-dance fw-bold mb-1")
        p(description.ifBlank { "Brak opisu." }) {
            setStyle("color", "#000000")
            addCssClass("mb-3")
        }

        if (!videoUrl.isNullOrBlank()) {
            h6("Wideo instruktażowe:", className = "text-primary-dance fw-bold mb-2")
            val fullUrl = toVideoUrl(videoUrl)
            tag(TAG.VIDEO, className = "w-100 rounded border border-secondary mb-3") {
                setAttribute("controls", "controls")
                setAttribute("style", "max-height:300px; background:#000;")
                tag(TAG.SOURCE) {
                    setAttribute("src", fullUrl)
                    setAttribute("type", "video/mp4")
                }
            }
        } else {
            p("Brak wideo wzorcowego.") {
                setStyle("color", "#6c757d")
                addCssClass("fst-italic")
                addCssClass("mb-3")
            }
        }

        div(className = "mt-4 border-top pt-3 border-secondary text-center") {
            val fileInput = tag(TAG.INPUT) {
                setAttribute("type", "file")
                setAttribute("accept", "video/*")
                setStyle("display", "none")
            }
            tag(TAG.BUTTON, "Wgraj swoje rozwiązanie \uD83C\uDFA5", className = "btn dance-btn-primary w-100 fw-bold") {
                onClick { fileInput.getElement()?.asDynamic().click() }
            }

            fileInput.onEvent {
                change = {
                    val f = fileInput.getElement()?.asDynamic().files
                    if (f != null && f.length > 0) {
                        ApiService.uploadVideoForTask(f[0], taskId, dancerId).then { _: dynamic ->
                            val toast = kotlinx.browser.document.createElement("div")
                            toast.asDynamic().className = "dance-toast"
                            toast.textContent = "✔ Nagranie wysłane do trenera!"
                            kotlinx.browser.document.body?.appendChild(toast)
                            window.setTimeout({
                                toast.asDynamic().classList.add("dance-toast-hide")
                                window.setTimeout({ kotlinx.browser.document.body?.removeChild(toast) }, 400)
                            }, 2500)
                            modal.hide()
                            null
                        }.catch { _: Throwable ->
                            window.alert("Błąd wgrywania nagrania. Sprawdź połączenie z backendem.")
                        }
                    }
                }
            }
        }
    }
    modal.show()
}

// ==========================================
// MOJE NAGRANIA
// ==========================================
fun Container.buildDancerSubmissions(appState: ObservableValue<Page>) {
    val subs = io.kvision.state.ObservableListWrapper<dynamic>()
    val tasks = io.kvision.state.ObservableListWrapper<dynamic>()
    val loading = ObservableValue(true)
    val errorText = ObservableValue("")

    val dancerId = window.localStorage.getItem("userId")?.toIntOrNull() ?: 1

    ApiService.fetchTasks().then { resTasks: dynamic ->
        tasks.addAll(resTasks as Array<dynamic>)
        ApiService.fetchSubmissionsForDancer(dancerId).then { resSubs: dynamic ->
            subs.addAll(resSubs as Array<dynamic>)
            loading.value = false
            null
        }.catch { _: Throwable ->
            errorText.value = "Nie udało się załadować nagrań."
            loading.value = false
            null
        }
        null
    }.catch { _: Throwable ->
        loading.value = false
        null
    }

    div(className = "container py-5 mt-5 pt-5") {
        backButton(appState, Page.DANCER_DASHBOARD)
        h2("Moje nagrania", className = "fw-bold mb-4")
        div {
            bind(loading) { isLoading ->
                if (isLoading) {
                    div(className = "text-center py-5") {
                        tag(TAG.DIV, className = "spinner-border text-primary-dance") { setAttribute("role", "status") }
                    }
                } else {
                    bind(errorText) { err -> if (err.isNotBlank()) div(className = "alert alert-danger") { +err } }
                    div {
                        bind(subs) { currentList ->
                            if (currentList.isEmpty()) {
                                div(className = "card bg-dark border-secondary p-5 text-center") {
                                    tag(TAG.I, className = "fa-solid fa-video-slash fa-3x text-muted mb-3")
                                    p("Nie wgrałeś jeszcze żadnych filmów.", className = "text-muted")
                                }
                            } else {
                                table(className = "table table-dark table-hover align-middle") {
                                    thead {
                                        tr { th("Zadanie"); th("Data wysłania"); th("Status"); th("Ocena"); th("Feedback"); th("Wideo") }
                                    }
                                    tbody {
                                        currentList.forEach { s ->
                                            val subId = s.id?.toString()?.toIntOrNull()
                                            val status = s.status?.toString() ?: "SUBMITTED"
                                            val videoUrl = s.videoUrl?.toString()
                                            val taskId = s.taskId?.toString()?.toIntOrNull() ?: 0
                                            val taskObj = tasks.find { it.id?.toString()?.toIntOrNull() == taskId }
                                            val taskTitle = taskObj?.title?.toString() ?: "Zadanie #$taskId"
                                            val instrUrl = taskObj?.instructionVideoUrl?.toString() ?: ""

                                            tr {
                                                td(taskTitle, className = "fw-bold")
                                                td(s.sentAt?.toString()?.substring(0, 10) ?: "—")
                                                td {
                                                    span(
                                                        if (status == "GRADED") "OCENIONE" else "OCZEKUJE",
                                                        className = if (status == "GRADED") "badge bg-success" else "badge bg-warning text-dark"
                                                    )
                                                }
                                                td(s.score?.toString() ?: "—")
                                                td(s.feedback?.toString() ?: "—")
                                                td {
                                                    if (subId != null && !videoUrl.isNullOrBlank()) {
                                                        val isGr = status == "GRADED"
                                                        tag(TAG.BUTTON, "▶ Obejrzyj", className = "btn btn-sm " + if (isGr) "btn-outline-success" else "btn-outline-light") {
                                                            onClick {
                                                                PlayerState.submissionId = subId
                                                                PlayerState.submissionVideoUrl = toVideoUrl(videoUrl)
                                                                PlayerState.instructionVideoUrl = toVideoUrl(instrUrl)
                                                                PlayerState.taskTitle = taskTitle
                                                                PlayerState.taskId = taskId
                                                                PlayerState.isGraded = isGr
                                                                PlayerState.currentScore = s.score?.toString()?.toIntOrNull()
                                                                PlayerState.currentFeedback = s.feedback?.toString()
                                                                appState.value = Page.PLAYER
                                                            }
                                                        }
                                                    } else {
                                                        span("—", className = "text-muted")
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
// STATYSTYKI (NOWY WYKRES I OPISY)
// ==========================================
fun Container.buildDancerStats(appState: ObservableValue<Page>) {
    val subs = io.kvision.state.ObservableListWrapper<dynamic>()
    val loading = ObservableValue(true)
    val dancerId = window.localStorage.getItem("userId")?.toIntOrNull() ?: 1

    ApiService.fetchSubmissionsForDancer(dancerId).then { res: dynamic ->
        subs.addAll(res as Array<dynamic>)
        loading.value = false
        null
    }.catch { _: Throwable ->
        loading.value = false
        null
    }

    div(className = "container py-5 mt-5 pt-5") {
        backButton(appState, Page.DANCER_DASHBOARD)
        h2("Twoje Statystyki", className = "fw-bold mb-4")
        div {
            bind(loading) { isLoading ->
                if (isLoading) {
                    div(className = "text-center py-5") {
                        tag(TAG.DIV, className = "spinner-border text-primary-dance") { setAttribute("role", "status") }
                    }
                } else {
                    div {
                        bind(subs) { list ->
                            val total = list.size
                            val graded = list.count { it.status?.toString() == "GRADED" }
                            val scores = list.mapNotNull { it.score?.toString()?.toIntOrNull() }
                            val avg = if (scores.isNotEmpty()) scores.average() else 0.0
                            val pending = total - graded

                            // ZMIANA: Zamiast zawodnego 'span' używamy twardego taga 'p'
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

                            // ZMIANA: Piękny wykres słupkowy oparty w 100% na CSS
                            if (total > 0) {
                                div(className = "row") {
                                    div(className = "col-md-8") {
                                        div(className = "card bg-dark border-secondary p-4") {
                                            h4("Ostatnie Oceny (Wykres Postępów)", className = "fw-bold text-white mb-4")

                                            if (scores.isEmpty()) {
                                                p("Musisz otrzymać przynajmniej jedną ocenę, aby zobaczyć wykres.", className = "text-muted")
                                            } else {
                                                // Kontener wykresu
                                                div(className = "d-flex align-items-end justify-content-around mt-2") {
                                                    setAttribute("style", "height: 150px; border-bottom: 2px solid #555; padding-bottom: 5px;")
                                                    val recentScores = scores.takeLast(7) // Maksymalnie 7 ostatnich ocen

                                                    recentScores.forEach { score ->
                                                        // Każdy słupek to kolumna z wynikiem na górze
                                                        div(className = "d-flex flex-column align-items-center w-100 px-1") {
                                                            p(score.toString(), className = "small text-light mb-2 fw-bold")
                                                            div(className = "bg-primary-dance rounded-top w-100") {
                                                                // Wysokość obliczana matematycznie: ocena 10 to 120px
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