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
        h2("Panel Tancerza", className = "fw-bold mb-4")
        div(className = "row g-4") {
            dashboardCard("fa-list-check", "Dostępne zadania", "Wybierz wyzwanie i obejrzyj instrukcje.") { appState.value = Page.DANCER_TASKS }
            dashboardCard("fa-video", "Moje nagrania", "Przeglądaj wgrane filmy i statusy ocen.") { appState.value = Page.DANCER_SUBMISSIONS }
            dashboardCard("fa-chart-line", "Statystyki", "Śledź swój progres i wyniki.") { appState.value = Page.DANCER_STATS }
        }
    }
}

fun Container.buildDancerTasks(appState: ObservableValue<Page>) {
    div(className = "container py-5 mt-5") {
        div(className = "mb-4 pt-2") {
            button("⬅ Wróć do panelu", className = "btn btn-outline-light rounded-pill px-4 fw-bold") {
                onClick { appState.value = Page.DANCER_DASHBOARD }
            }
        }
        h2("Dostępne zadania", className = "fw-bold mb-4")

        bind(DataManager.globalTasks) { tasks ->
            val myTasks = tasks.filter { it.assignedDancers.contains("1") }
            if (myTasks.isEmpty()) { p("Brak nowych zadań.", className = "text-muted") }
            else {
                ul(className = "list-group bg-dark shadow-sm col-md-8") {
                    myTasks.forEach { task ->
                        li(className = "list-group-item bg-dark text-white border-secondary hover-card mb-2 d-flex justify-content-between align-items-center p-3") {
                            setStyle("cursor", "pointer")
                            div { h5(task.title, className = "fw-bold text-primary-dance mb-1") }
                            button("Zobacz i wykonaj", className = "btn btn-sm dance-btn-primary") { onClick { showDancerTaskDetails(task) } }
                        }
                    }
                }
            }
        }
    }
}

fun showDancerTaskDetails(task: ChoreoTask) {
    val modal = Modal("Wyzwanie: ${task.title}", closeButton = true, animation = true)
    modal.div(className = "p-3") {
        h6("Instrukcje:", className = "text-primary-dance fw-bold mb-1")
        p(task.description, className = "text-dark mb-3") { setStyle("color", "#000000") }

        if (task.instructionVideoUrl != null) {
            h6("Wideo instruktażowe:", className = "text-primary-dance fw-bold mb-2")
            tag(TAG.VIDEO, className = "w-100 rounded border border-secondary mb-3") {
                setAttribute("controls", "controls")
                tag(TAG.SOURCE) { setAttribute("src", task.instructionVideoUrl) }
            }
        }

        div(className = "mt-4 border-top pt-3 border-secondary text-center") {
            val fileInput = tag(TAG.INPUT) { setAttribute("type", "file"); setStyle("display", "none") }
            button("Wgraj swoje rozwiązanie \uD83C\uDFA5", className = "btn dance-btn-primary w-100 fw-bold") { onClick { fileInput.getElement()?.asDynamic().click() } }

            fileInput.onEvent {
                change = {
                    val f = fileInput.getElement()?.asDynamic().files
                    if (f != null && f.length > 0) {
                        ApiService.uploadVideo(f[0]).then { _: dynamic ->
                            val toast = kotlinx.browser.document.createElement("div")
                            toast.asDynamic().className = "dance-toast"
                            toast.textContent = "✔ Nagranie wysłane!"
                            kotlinx.browser.document.body?.appendChild(toast)
                            window.setTimeout({ toast.asDynamic().classList.add("dance-toast-hide") }, 2500)
                            modal.hide()
                            null
                        }.catch { _: Throwable -> null }
                    }
                }
            }
        }
    }
    modal.show()
}

fun Container.buildDancerSubmissions(appState: ObservableValue<Page>) {
    val subs = io.kvision.state.ObservableListWrapper<dynamic>()
    ApiService.fetchSubmissions().then { res: dynamic ->
        val list = res as Array<dynamic>
        subs.addAll(list)
        null
    }.catch { _: Throwable -> null }

    div(className = "container py-5 mt-5") {
        div(className = "mb-4 pt-2") {
            button("⬅ Wróć do panelu", className = "btn btn-outline-light rounded-pill px-4 fw-bold") { onClick { appState.value = Page.DANCER_DASHBOARD } }
        }
        h2("Moje nagrania", className = "fw-bold mb-4")
        table(className = "table table-dark table-hover") {
            thead { tr { th("Zadanie"); th("Data"); th("Status") } }
            tbody {
                bind(subs) { currentList ->
                    currentList.forEach { s ->
                        tr {
                            td("Nagranie #${s.id}"); td(s.createdAt?.toString() ?: "Dzisiaj")
                            td {
                                val status = s.status?.toString() ?: "OCZEKUJE"
                                span(status, className = if(status == "GRADED") "badge bg-success" else "badge bg-warning text-dark")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Container.buildDancerStats(appState: ObservableValue<Page>) {
    div(className = "container py-5 mt-5") {
        div(className = "mb-4 pt-2") {
            button("⬅ Wróć do panelu", className = "btn btn-outline-light rounded-pill px-4 fw-bold") { onClick { appState.value = Page.DANCER_DASHBOARD } }
        }
        h2("Twoje Statystyki", className = "fw-bold mb-4")
        div(className = "row text-center g-4 mb-5") {
            div(className = "col-md-4") { div(className = "card bg-dark border-secondary p-4") { h3("12"); p("Wgranych filmów", className="text-muted") } }
            div(className = "col-md-4") { div(className = "card bg-dark border-secondary p-4") { h3("8"); p("Otrzymanych ocen", className="text-muted") } }
            div(className = "col-md-4") { div(className = "card bg-dark border-secondary p-4") { h3("B+", className="text-primary-dance"); p("Średnia ocena", className="text-muted") } }
        }
        h4("Postępy w tym miesiącu", className = "fw-bold mb-3")
        div(className = "card bg-dark border-secondary p-4 col-md-8") {
            p("Zrealizowane zadania", className = "mb-1 text-light fw-bold")
            div(className = "progress mb-4") {
                setAttribute("style", "height: 20px; background-color: #333;")
                div(className = "progress-bar bg-primary-dance") { setAttribute("style", "width: 80%;") }
            }
            p("Pozytywne oceny", className = "mb-1 text-light fw-bold")
            div(className = "progress mb-2") {
                setAttribute("style", "height: 20px; background-color: #333;")
                div(className = "progress-bar bg-success") { setAttribute("style", "width: 65%;") }
            }
        }
    }
}