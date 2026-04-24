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
        h2(I18n.tr("Panel Tancerza", "Dancer Panel"), className = "fw-bold text-primary-dance mb-4 pt-4")
        div(className = "row g-4 justify-content-center") {
            dashboardCard("fa-list-check", I18n.tr("Dostępne zadania", "Available tasks"), I18n.tr("Wybierz wyzwanie i obejrzyj instrukcje.", "Choose a challenge and watch instructions.")) { appState.value = Page.DANCER_TASKS }
            dashboardCard("fa-video", I18n.tr("Moje nagrania", "My recordings"), I18n.tr("Przeglądaj wgrane filmy i statusy ocen.", "Browse uploaded videos and grade statuses.")) { appState.value = Page.DANCER_SUBMISSIONS }
            dashboardCard("fa-chart-line", I18n.tr("Statystyki", "Statistics"), I18n.tr("Śledź swój progres i wyniki.", "Track your progress and results.")) { appState.value = Page.DANCER_STATS }
            dashboardCard("fa-comments", I18n.tr("Wiadomości", "Messages"), I18n.tr("Messenger społeczności.", "Community messenger.")) { appState.value = Page.CHAT}
        }
    }
}

// ==========================================
// DOSTĘPNE ZADANIA (Podział: Oczekujące / Wykonane)
// ==========================================
fun Container.buildDancerTasks(appState: ObservableValue<Page>) {
    val myId = window.localStorage.getItem("userId")?.toIntOrNull() ?: 1
    val tasks = io.kvision.state.ObservableListWrapper<dynamic>()
    val subs = io.kvision.state.ObservableListWrapper<dynamic>()
    val loading = ObservableValue(true)

    ApiService.fetchTasks().then<dynamic> { res: dynamic ->
        tasks.addAll(res as Array<dynamic>)
        ApiService.fetchSubmissionsForDancer(myId).then<dynamic> { res2: dynamic ->
            subs.addAll(res2 as Array<dynamic>)
            loading.value = false
            null
        }.catch<dynamic> { _: Throwable -> loading.value = false; null }
        null
    }.catch<dynamic> { _: Throwable -> loading.value = false; null }

    div(className = "container py-5 mt-5 pt-5") {
        backButton(appState, Page.DANCER_DASHBOARD)
        h2(I18n.tr("Dostępne zadania", "Available tasks"), className = "fw-bold mb-4")

        div {
            bind(loading) { isLoading ->
                if (isLoading) {
                    div(className = "text-center py-5") {
                        tag(TAG.DIV, className = "spinner-border text-primary-dance") { setAttribute("role", "status") }
                    }
                } else {
                    bind(tasks) { taskList ->
                        bind(subs) { subList ->
                            val subTaskIds = subList.mapNotNull { it.taskId?.toString()?.toIntOrNull() }.toSet()

                            val doneTasks = taskList.filter { it.id?.toString()?.toIntOrNull() in subTaskIds }
                            val pendingTasks = taskList.filter { it.id?.toString()?.toIntOrNull() !in subTaskIds }

                            div(className = "row g-4") {
                                // KOLUMNA: Oczekujące
                                div(className = "col-md-6") {
                                    h4(I18n.tr("Oczekujące wyzwania", "Pending challenges"), className = "text-warning mb-3")
                                    if (pendingTasks.isEmpty()) {
                                        p(I18n.tr("Nie masz obecnie nowych zadań do wykonania.", "You currently have no new tasks to complete."), className = "text-muted")
                                    } else {
                                        ul(className = "list-group bg-dark shadow-sm") {
                                            pendingTasks.forEach { t ->
                                                val tId = t.id?.toString()?.toIntOrNull() ?: 0
                                                val title = t.title?.toString() ?: I18n.tr("Bez tytułu", "No title")
                                                val desc = t.description?.toString() ?: ""
                                                val deadline = t.deadline?.toString()?.take(16)?.replace("T", " ") ?: ""
                                                val instrUrl = t.instructionVideoUrl?.toString() ?: ""

                                                li(className = "list-group-item bg-dark border-secondary mb-2 rounded") {
                                                    div(className = "d-flex flex-column h-100") {
                                                        div {
                                                            h6(title, className = "fw-bold text-white mb-1")
                                                            p(I18n.tr("Termin: ", "Deadline: ") + deadline, className = "text-danger small fw-bold mb-2")
                                                            p(desc, className = "text-muted small mb-3")
                                                        }
                                                        div(className = "mt-auto pt-2 border-top border-secondary") {
                                                            tag(TAG.BUTTON, I18n.tr("Szczegóły & Opublikuj Nagranie", "Details & Publish Recording"), className = "btn btn-sm dance-btn-primary w-100 fw-bold") {
                                                                onClick {
                                                                    showTaskDetailsModal(tId, title, desc, deadline, instrUrl) { appState.value = Page.DANCER_SUBMISSIONS }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // KOLUMNA: Wykonane
                                div(className = "col-md-6") {
                                    h4(I18n.tr("Przesłane (Oczekują na ocenę)", "Submitted (Awaiting grading)"), className = "text-success mb-3 mt-4 mt-md-0")
                                    if (doneTasks.isEmpty()) {
                                        p(I18n.tr("Nie przesłałeś jeszcze żadnych zadań.", "You haven't submitted any tasks yet."), className = "text-muted")
                                    } else {
                                        ul(className = "list-group bg-dark shadow-sm") {
                                            doneTasks.forEach { t ->
                                                val title = t.title?.toString() ?: I18n.tr("Zadanie", "Task")
                                                val deadline = t.deadline?.toString()?.take(16)?.replace("T", " ") ?: ""

                                                li(className = "list-group-item bg-dark border-secondary mb-2 rounded opacity-75") {
                                                    div {
                                                        h6(title, className = "fw-bold text-white mb-1")
                                                        p(I18n.tr("Termin zadania: ", "Task deadline: ") + deadline, className = "text-muted small mb-2")
                                                        span(I18n.tr("✔ Nagranie przesłane", "✔ Recording submitted"), className = "badge bg-success")
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
// MODAL SZCZEGÓŁÓW ZADANIA I WGRYWANIA
// ==========================================
private fun showTaskDetailsModal(taskId: Int, title: String, desc: String, deadline: String, instrUrl: String, onUploaded: () -> Unit) {
    val modal = Modal(I18n.tr("Zadanie: ", "Task: ") + title, closeButton = true, animation = true)
    modal.div(className = "p-3") {
        p(desc.ifBlank { I18n.tr("Brak opisu dla tego zadania.", "No description for this task.") }, className = "text-light mb-3")

        if (instrUrl.isNotBlank()) {
            h6(I18n.tr("Wideo instruktora:", "Instructor video:"), className = "fw-bold text-primary-dance mb-2")
            val fullUrl = toVideoUrl(instrUrl)
            tag(TAG.VIDEO, className = "w-100 rounded border border-secondary mb-4 bg-black") {
                setAttribute("controls", "controls")
                setAttribute("style", "max-height:300px;")
                tag(TAG.SOURCE) { setAttribute("src", fullUrl) }
            }
        }

        div(className = "bg-dark border border-secondary p-3 rounded text-center") {
            h6(I18n.tr("Twoja kolej!", "Your turn!"), className = "fw-bold text-warning mb-2")
            p(I18n.tr("Nagraj swoje wykonanie i wyślij je choreografowi. Upewnij się, że mieścisz się w terminie (", "Record your performance and send it to the choreographer. Make sure you meet the deadline (") + deadline + ").", className = "small text-muted mb-3")

            val fileInput = tag(TAG.INPUT, className = "form-control mb-3") {
                setAttribute("type", "file")
                setAttribute("accept", "video/*")
            }

            tag(TAG.BUTTON, I18n.tr("Wyślij moje nagranie", "Send my recording"), className = "btn dance-btn-primary w-100 fw-bold") {
                onClick {
                    val files = fileInput.getElement()?.asDynamic().files
                    if (files != null && files.length > 0) {
                        val file = files[0]
                        val myId = window.localStorage.getItem("userId")?.toIntOrNull() ?: 1
                        ApiService.uploadVideoForTask(file, taskId, myId).then<dynamic> {
                            modal.hide()
                            showToast(I18n.tr("✔ Super! Wideo wysłane. Oczekuj na feedback.", "✔ Great! Video sent. Wait for feedback."))
                            onUploaded()
                            null
                        }.catch<dynamic> { _: Throwable ->
                            window.alert(I18n.tr("Wystąpił błąd podczas wysyłania nagrania.", "An error occurred while sending the recording."))
                            null
                        }
                    } else {
                        window.alert(I18n.tr("Najpierw wybierz plik wideo!", "Choose a video file first!"))
                    }
                }
            }
        }
    }
    modal.show()
}

// ==========================================
// MOJE NAGRANIA (TANCERZ)
// ==========================================
fun Container.buildDancerSubmissions(appState: ObservableValue<Page>) {
    val myId = window.localStorage.getItem("userId")?.toIntOrNull() ?: 1
    val subs = io.kvision.state.ObservableListWrapper<dynamic>()
    val tasks = io.kvision.state.ObservableListWrapper<dynamic>()
    val loading = ObservableValue(true)

    ApiService.fetchTasks().then<dynamic> { resTasks: dynamic ->
        tasks.addAll(resTasks as Array<dynamic>)
        ApiService.fetchSubmissionsForDancer(myId).then<dynamic> { resSubs: dynamic ->
            subs.addAll(resSubs as Array<dynamic>)
            loading.value = false
            null
        }.catch<dynamic> { _: Throwable -> loading.value = false; null }
        null
    }.catch<dynamic> { _: Throwable -> loading.value = false; null }

    div(className = "container py-5 mt-5 pt-5") {
        backButton(appState, Page.DANCER_DASHBOARD)
        h2(I18n.tr("Moje Nagrania i Oceny", "My Recordings and Grades"), className = "fw-bold mb-4")

        div {
            bind(loading) { isLoading ->
                if (isLoading) {
                    div(className = "text-center py-5") { tag(TAG.DIV, className = "spinner-border text-primary-dance") { setAttribute("role", "status") } }
                } else {
                    bind(subs) { subList ->
                        if (subList.isEmpty()) {
                            div(className = "card bg-dark border-secondary p-5 text-center") {
                                p(I18n.tr("Jeszcze nie masz żadnych nagrań.", "You have no recordings yet."), className = "text-muted")
                                tag(TAG.BUTTON, I18n.tr("Znajdź zadanie", "Find a task"), className = "btn btn-outline-info mt-3") {
                                    onClick { appState.value = Page.DANCER_TASKS }
                                }
                            }
                        } else {
                            val sortedSubs = subList.sortedByDescending { it.id?.toString()?.toIntOrNull() ?: 0 }
                            table(className = "table table-dark table-hover align-middle") {
                                thead { tr {
                                    th("ID"); th(I18n.tr("Zadanie", "Task")); th(I18n.tr("Data", "Date")); th(I18n.tr("Status", "Status")); th(I18n.tr("Ocena", "Grade")); th(I18n.tr("Akcja", "Action"))
                                } }
                                tbody {
                                    sortedSubs.forEach { s ->
                                        val subId = s.id?.toString()?.toIntOrNull() ?: 0
                                        val taskId = s.taskId?.toString()?.toIntOrNull() ?: 0
                                        val taskObj = tasks.find { it.id?.toString()?.toIntOrNull() == taskId }
                                        val taskTitle = taskObj?.title?.toString() ?: (I18n.tr("Zadanie #", "Task #") + "$taskId")
                                        val statusStr = s.status?.toString() ?: "SUBMITTED"
                                        val score = s.score?.toString()

                                        tr {
                                            td("#$subId")
                                            td(taskTitle)
                                            td(s.sentAt?.toString()?.take(10) ?: I18n.tr("Brak", "None"))
                                            td {
                                                when (statusStr) {
                                                    "GRADED" -> span(I18n.tr("Ocenione", "Graded"), className = "badge bg-success")
                                                    "RESTORED" -> span(I18n.tr("Przywrócono choreografowi", "Restored to choreographer"), className = "badge bg-warning text-dark")
                                                    else -> span(I18n.tr("Oczekuje", "Pending"), className = "badge bg-secondary")
                                                }
                                            }
                                            td {
                                                if (statusStr == "GRADED" && score != null) span("$score/10", className = "badge bg-info text-dark fs-6")
                                                else span("—", className = "text-muted")
                                            }
                                            td {
                                                if (statusStr == "GRADED") {
                                                    tag(TAG.BUTTON, I18n.tr("Zobacz Feedback", "View Feedback"), className = "btn btn-sm dance-btn-primary") {
                                                        onClick {
                                                            PlayerState.submissionId = subId
                                                            PlayerState.submissionVideoUrl = toVideoUrl(s.videoUrl?.toString())
                                                            PlayerState.instructionVideoUrl = toVideoUrl(taskObj?.instructionVideoUrl?.toString())
                                                            PlayerState.taskTitle = taskTitle
                                                            PlayerState.taskId = taskId
                                                            PlayerState.isGraded = true
                                                            PlayerState.currentScore = score?.toIntOrNull()
                                                            PlayerState.currentFeedback = s.feedback?.toString()
                                                            appState.value = Page.PLAYER
                                                        }
                                                    }
                                                } else if (statusStr == "RESTORED") {
                                                    span(I18n.tr("Choreograf poprawia uwagi", "Choreographer is correcting notes"), className = "text-muted small fst-italic")
                                                } else {
                                                    span(I18n.tr("Czekaj na choreografa...", "Wait for choreographer..."), className = "text-muted small")
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
// STATYSTYKI (TANCERZ)
// ==========================================
fun Container.buildDancerStats(appState: ObservableValue<Page>) {
    val myId = window.localStorage.getItem("userId")?.toIntOrNull() ?: 1
    val subs = io.kvision.state.ObservableListWrapper<dynamic>()
    val loading = ObservableValue(true)

    ApiService.fetchSubmissionsForDancer(myId).then<dynamic> { res ->
        subs.addAll(res as Array<dynamic>)
        loading.value = false
        null
    }.catch<dynamic> { _: Throwable -> loading.value = false; null }

    div(className = "container py-5 mt-5 pt-5") {
        backButton(appState, Page.DANCER_DASHBOARD)
        h2(I18n.tr("Twoje Statystyki", "Your Statistics"), className = "fw-bold mb-4 text-primary-dance")

        div {
            bind(loading) { isLoading ->
                if (isLoading) {
                    div(className = "text-center py-5") { tag(TAG.DIV, className = "spinner-border text-info") }
                } else {
                    bind(subs) { subList ->
                        val allCount = subList.size
                        val gradedSubs = subList.filter { it.status?.toString() == "GRADED" }
                        val gradedCount = gradedSubs.size

                        val scores = gradedSubs.mapNotNull { it.score?.toString()?.toIntOrNull() }
                        val avgScore = if (scores.isNotEmpty()) scores.average() else 0.0
                        val avgStr = avgScore.asDynamic().toFixed(1).toString()

                        div(className = "row g-4") {
                            div(className = "col-md-4") {
                                div(className = "card bg-dark border-secondary p-4 text-center h-100") {
                                    tag(TAG.I, className = "fa-solid fa-video fa-2x text-light mb-2")
                                    h3(allCount.toString(), className = "fw-bold text-white")
                                    p(I18n.tr("Przesłane nagrania", "Submitted recordings"), className = "text-muted small mb-0 text-uppercase")
                                }
                            }
                            div(className = "col-md-4") {
                                div(className = "card bg-dark border-secondary p-4 text-center h-100") {
                                    tag(TAG.I, className = "fa-solid fa-check-double fa-2x text-success mb-2")
                                    h3(gradedCount.toString(), className = "fw-bold text-white")
                                    p(I18n.tr("Ocenione zadania", "Graded tasks"), className = "text-muted small mb-0 text-uppercase")
                                }
                            }
                            div(className = "col-md-4") {
                                div(className = "card bg-dark border-secondary p-4 text-center h-100") {
                                    tag(TAG.I, className = "fa-solid fa-star fa-2x text-warning mb-2")
                                    h3(avgStr, className = "fw-bold text-white")
                                    p(I18n.tr("Średnia ocena", "Average grade"), className = "text-muted small mb-0 text-uppercase")
                                }
                            }
                        }

                        div(className = "row mt-4") {
                            div(className = "col-12") {
                                div(className = "card bg-dark border-secondary p-4") {
                                    h4(I18n.tr("Wizualizacja Ocen", "Grades Visualization"), className = "mb-4 text-white")
                                    if (scores.isEmpty()) {
                                        p(I18n.tr("Brak ocen do wygenerowania wykresu.", "No grades to generate a chart."), className = "text-muted text-center py-4")
                                    } else {
                                        div(className = "d-flex justify-content-center align-items-end mx-auto") {
                                            setAttribute("style", "height: 150px; border-bottom: 1px solid #444; max-width: 600px; gap: 10px;")
                                            div(className = "d-flex h-100 align-items-end") {
                                                if (scores.isNotEmpty()) {
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
                                                p(I18n.tr("Chronologia od lewej do prawej", "Chronology from left to right"), className = "text-center text-muted small mt-3 mb-0")
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