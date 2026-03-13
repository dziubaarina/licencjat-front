package com.licencjat.licencjat_frontend

import io.kvision.core.*
import io.kvision.html.*
import io.kvision.panel.*
import io.kvision.state.ObservableValue

fun Container.buildDancerDashboard(appState: ObservableValue<Page>) {
    div(className = "container py-5 mt-5") {
        h2("Panel Tancerza", className = "fw-bold mb-4")
        div(className = "row g-4") {
            dashboardCard("fa-list-check", "Dostępne zadania", "Wybierz wyzwanie i wgraj nagranie.") { appState.value = Page.DANCER_TASKS }
            dashboardCard("fa-video", "Moje nagrania", "Przeglądaj wgrane filmy i sprawdź status.") { appState.value = Page.DANCER_SUBMISSIONS }
            dashboardCard("fa-chart-line", "Statystyki", "Śledź swój progres ocen.") { appState.value = Page.DANCER_STATS }
        }
    }
}

fun Container.buildDancerTasks(appState: ObservableValue<Page>) {
    div(className = "container py-5 mt-5") {
        backButton(appState, Page.DANCER_DASHBOARD)
        h2("Dostępne zadania", className = "fw-bold mb-4")

        div(className = "card bg-dark text-white border-secondary p-4 mb-4") {
            h4("Zadanie #1: Izolacje klatki piersiowej", className = "fw-bold text-primary-dance mb-2")
            p("Nagraj 30-sekundowy film do dowolnego utworu.", className = "text-muted mb-4")

            val fileInput = tag(TAG.INPUT) { setAttribute("type", "file"); setAttribute("accept", "video/*"); setStyle("display", "none") }
            button("Wgraj rozwiązanie (Wideo)", className = "btn dance-btn-primary rounded-pill px-4 py-2 fw-bold w-auto") {
                onClick { fileInput.getElement()?.asDynamic().click() }
            }

            // UWAGA: Tu wywołujemy nasze API z nowego pliku!
            fileInput.onEvent { change = { val f = fileInput.getElement()?.asDynamic().files; if (f != null && f.length > 0) ApiService.uploadVideo(f[0]) } }
        }
    }
}

fun Container.buildDancerSubmissions(appState: ObservableValue<Page>) {
    div(className = "container py-5 mt-5") {
        backButton(appState, Page.DANCER_DASHBOARD)
        h2("Moje nagrania", className = "fw-bold mb-4")
        table(className = "table table-dark table-hover align-middle") {
            thead { tr { th("Zadanie"); th("Data wysłania"); th("Status"); th("Akcja") } }
            tbody {
                tr {
                    td("Izolacje klatki"); td("12.03.2026")
                    td { span("Ocenione", className = "badge bg-success") }
                    td { button("Zobacz Feedback", className="btn btn-sm dance-btn-primary") { onClick { appState.value = Page.PLAYER } } }
                }
                tr {
                    td("Footwork Basics"); td("Dziś")
                    td { span("Oczekuje", className = "badge bg-warning text-dark") }
                    td { button("Zobacz", className="btn btn-sm btn-secondary disabled") }
                }
            }
        }
    }
}

fun Container.buildDancerStats(appState: ObservableValue<Page>) {
    div(className = "container py-5 mt-5") {
        backButton(appState, Page.DANCER_DASHBOARD)
        h2("Twoje Statystyki", className = "fw-bold mb-4")
        div(className = "row text-center g-4") {
            div(className = "col-md-4") { div(className = "card bg-dark border-secondary p-4") { h3("12"); p("Wgranych filmów", className="text-muted") } }
            div(className = "col-md-4") { div(className = "card bg-dark border-secondary p-4") { h3("8"); p("Otrzymanych ocen", className="text-muted") } }
            div(className = "col-md-4") { div(className = "card bg-dark border-secondary p-4") { h3("B+", className="text-primary-dance"); p("Średnia ocena", className="text-muted") } }
        }
    }
}