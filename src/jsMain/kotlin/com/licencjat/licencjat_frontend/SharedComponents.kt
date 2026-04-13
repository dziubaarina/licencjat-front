package com.licencjat.licencjat_frontend

import io.kvision.core.*
import io.kvision.html.*
import io.kvision.panel.*
import io.kvision.state.ObservableValue

// Uniwersalny kafelek nawigacyjny
fun Container.dashboardCard(icon: String, title: String, desc: String, action: () -> Unit) {
    div(className = "col-md-4") {
        setAttribute("tabindex", "0")
        setAttribute("role", "button")
        setAttribute("aria-label", title)
        div(className = "card bg-dark text-white border-secondary h-100 p-4 hover-card text-center") {
            setStyle("cursor", "pointer")
            onClick { action() }
            tag(TAG.I, className = "fa-solid $icon fa-2x text-primary-dance mb-3")
            h5(title, className = "fw-bold")
            p(desc, className = "text-muted small")
        }
    }
}

// Uniwersalny przycisk powrotu - ZMIANA NA FIOLETOWY (dance-btn-primary)
fun Container.backButton(appState: ObservableValue<Page>, targetPage: Page) {
    div(className = "mb-4") {
        button(I18n.tr("⬅ Wróć", "⬅ Back"), className = "btn dance-btn-primary rounded-pill px-4 fw-bold") {
            onClick {
                KeyboardManager.pop()
                appState.value = targetPage
            }
        }
    }
    KeyboardManager.push { appState.value = targetPage }
}

// Tymczasowy widok dla zakładek w budowie
fun Container.buildPlaceholderView(appState: ObservableValue<Page>, title: String, backPage: Page) {
    div(className = "container py-5 mt-5 pt-5") {
        backButton(appState, backPage)
        h2(title, className = "fw-bold mb-3")
        div(className = "card bg-dark text-white border-secondary p-5 text-center") {
            h4(I18n.tr("Zakładka w budowie \uD83D\uDEA7", "Under construction \uD83D\uDEA7"), className = "text-muted")
            p(I18n.tr("Tutaj pojawią się dane z backendu.", "Backend data will appear here."), className = "text-muted small mb-0")
        }
    }
}