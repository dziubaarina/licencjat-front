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

fun Container.buildDancerTasks(appState: ObservableValue<Page>) {
    div(className = "container py-5 mt-5 pt-5") {
        backButton(appState, Page.DANCER_DASHBOARD)
        h2("Dostępne zadania", className = "fw-bold mb-4")
        // Osobny div poza bind – bind() zastępuje całą zawartość swojego kontenera,
        // więc backButton i h2 muszą być w innym kontenerze niż bind
        div {
            bind(DataManager.globalTasks) { tasks ->
                val myDancerId = window.localStorage.getItem("userId") ?: "1"
                val myTasks = tasks.filter { it.assignedDancers.contains(myDancerId) }
                if (myTasks.isEmpty()) {
                    p("Brak nowych zadań przypisanych do Twojego konta.", className = "text-muted")
                } else {
                    ul(className = "list-group bg-dark shadow-sm col-md-8") {
                        myTasks.forEach { task ->
                            li(className = "list-group-item bg-dark text-white border-secondary hover-card mb-2 d-flex justify-content-between align-items-center p-3") {
                                setStyle("cursor", "pointer")
                                div { h5(task.title, className = "fw-bold text-primary-dance mb-1") }
                                button("Zobacz i wykonaj", className = "btn btn-sm dance-btn-primary") { onClick { showDancerTaskDetails(task, appState) } }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun showDancerTaskDetails(task: ChoreoTask, appState: ObservableValue<Page>) {
    val modal = Modal("Wyzwanie: ${task.title}", closeButton = true, animation = true)
    modal.div(className = "p-3") {
        h6("Instrukcje:", className = "text-primary-dance fw-bold mb-1")
        p(task.description, className = "text-dark mb-3") { setStyle("color", "#000000") }

        if (task.instructionVideoUrl != null) {
            h6("Wideo instruktażowe:", className = "text-primary-dance fw-bold mb-2")
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

        div(className = "mt-4 border-top pt-3 border-secondary text-center") {
            val fileInput = tag(TAG.INPUT) { setAttribute("type", "file"); setStyle("display", "none") }
            button("Wgraj swoje rozwiązanie \uD83C\uDFA5", className = "btn dance-btn-primary w-100 fw-bold") {
                onClick { fileInput.getElement()?.asDynamic().click() }
            }

            fileInput.onEvent {
                change = {
                    val f = fileInput.getElement()?.asDynamic().files
                    if (f != null && f.length > 0) {
                        val dancerId = window.localStorage.getItem("userId")?.toIntOrNull() ?: 1
                        ApiService.uploadVideoForTask(f[0], task.id, dancerId).then { _: dynamic ->
                            val toast = kotlinx.browser.document.createElement("div")
                            toast.asDynamic().className = "dance-toast"
                            toast.textContent = "✔ Nagranie wysłane do trenera!"
                            kotlinx.browser.document.body?.appendChild(toast)
                            window.setTimeout({ toast.asDynamic().classList.add("dance-toast-hide") }, 2500)
                            modal.hide()
                            null
                        }.catch { _: Throwable -> window.alert("Błąd wgrywania") }
                    }
                }
            }
        }
    }
    modal.show()
}

fun Container.buildDancerSubmissions(appState: ObservableValue<Page>) {
    val subs = io.kvision.state.ObservableListWrapper<dynamic>()
    val loading = ObservableValue(true)
    val errorText = ObservableValue("")

    val dancerId = window.localStorage.getItem("userId")?.toIntOrNull() ?: 1

    ApiService.fetchSubmissionsForDancer(dancerId).then { res: dynamic ->
        val list = res as Array<dynamic>
        subs.addAll(list)
        loading.value = false
        null
    }.catch { _: Throwable ->
        errorText.value = "Nie udało się załadować nagrań. Sprawdź połączenie z backendem."
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
                        p("Ładowanie nagrań...", className = "text-muted mt-3")
                    }
                } else {
                    bind(errorText) { err ->
                        if (err.isNotBlank()) {
                            div(className = "alert alert-danger") { +err }
                        }
                    }

                    table(className = "table table-dark table-hover") {
                        thead {
                            tr {
                                th("ID")
                                th("Zadanie")
                                th("Data wysłania")
                                th("Status")
                                th("Ocena")
                                th("Feedback")
                            }
                        }
                        tbody {
                            bind(subs) { currentList ->
                                if (currentList.isEmpty()) {
                                    tr { td("Nie wgrałeś jeszcze żadnych filmów.") { setAttribute("colspan", "6") } }
                                }
                                currentList.forEach { s ->
                                    tr {
                                        td("Nagranie #${s.id}")
                                        td("Zadanie #${s.taskId}")
                                        td(s.sentAt?.toString()?.substring(0, 10) ?: "Brak daty")
                                        td {
                                            val status = s.status?.toString() ?: "SUBMITTED"
                                            span(
                                                if (status == "GRADED") "OCENIONE" else "OCZEKUJE",
                                                className = if (status == "GRADED") "badge bg-success" else "badge bg-warning text-dark"
                                            )
                                        }
                                        td(s.score?.toString() ?: "—")
                                        td(s.feedback?.toString() ?: "—")
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

fun Container.buildDancerStats(appState: ObservableValue<Page>) {
    val subs = io.kvision.state.ObservableListWrapper<dynamic>()
    val dancerId = window.localStorage.getItem("userId")?.toIntOrNull() ?: 1

    ApiService.fetchSubmissionsForDancer(dancerId).then { res: dynamic ->
        subs.addAll(res as Array<dynamic>)
        null
    }.catch { _: Throwable -> null }

    div(className = "container py-5 mt-5 pt-5") {
        backButton(appState, Page.DANCER_DASHBOARD)
        h2("Twoje Statystyki", className = "fw-bold mb-4")
        div {
            bind(subs) { list ->
                val total = list.size
                val graded = list.count { it.status?.toString() == "GRADED" }
                val scores = list.mapNotNull { it.score?.toString()?.toIntOrNull() }
                val avg = if (scores.isNotEmpty()) scores.average() else 0.0

                div(className = "row text-center g-4 mb-5") {
                    div(className = "col-md-4") { div(className = "card bg-dark border-secondary p-4") { h3(total.toString()); p("Wgranych filmów", className = "text-muted") } }
                    div(className = "col-md-4") { div(className = "card bg-dark border-secondary p-4") { h3(graded.toString()); p("Otrzymanych ocen", className = "text-muted") } }
                    div(className = "col-md-4") { div(className = "card bg-dark border-secondary p-4") {
                        h3(if (avg > 0) (kotlin.math.round(avg * 10) / 10.0).toString() else "—", className = "text-primary-dance")
                        p("Średnia ocena", className = "text-muted")
                    } }
                }
            }

            h4("Postępy w tym miesiącu", className = "fw-bold mb-3")
            div(className = "card bg-dark border-secondary p-4 col-md-8") {
                p("Zrealizowane zadania", className = "mb-1 text-light fw-bold")
                div(className = "progress mb-4") {
                    setAttribute("style", "height: 20px; background-color: #333;")
                    div(className = "progress-bar bg-primary-dance") { setAttribute("style", "width: 80%;") }
                }
            }
        } // end wrapper div
    }
}