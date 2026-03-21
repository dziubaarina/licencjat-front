package com.licencjat.licencjat_frontend

import io.kvision.core.*
import io.kvision.html.*
import io.kvision.panel.*
import io.kvision.state.ObservableValue

// Uniwersalny kafelek nawigacyjny
fun Container.dashboardCard(icon: String, title: String, desc: String, action: () -> Unit) {
    div(className = "col-md-4") {
        div(className = "card bg-dark text-white border-secondary h-100 p-4 hover-card text-center") {
            setStyle("cursor", "pointer")
            onClick { action() }
            tag(TAG.I, className = "fa-solid $icon fa-2x text-primary-dance mb-3")
            h5(title, className = "fw-bold")
            p(desc, className = "text-muted small")
        }
    }
}

// Uniwersalny przycisk powrotu (Teraz większy, ze strzałką i marginesem!)
fun Container.backButton(appState: ObservableValue<Page>, targetPage: Page) {
    div(className = "mb-4") {
        button("⬅ Wróć", className = "btn btn-outline-light rounded-pill px-4 fw-bold") {
            onClick { appState.value = targetPage }
        }
    }
}

// Tymczasowy widok dla zakładek w budowie
fun Container.buildPlaceholderView(appState: ObservableValue<Page>, title: String, backPage: Page) {
    div(className = "container py-5 mt-5 pt-5") {
        backButton(appState, backPage)
        h2(title, className = "fw-bold mb-3")
        div(className = "card bg-dark text-white border-secondary p-5 text-center") {
            h4("Zakładka w budowie \uD83D\uDEA7", className = "text-muted")
            p("Tutaj pojawią się dane z backendu.", className = "text-muted small mb-0")
        }
    }
}