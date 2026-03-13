package com.licencjat.licencjat_frontend

import kotlinx.browser.window
import org.w3c.files.Blob

object ApiService {

    fun uploadVideo(file: dynamic) {
        val formData = org.w3c.xhr.FormData()
        formData.append("file", file as Blob)
        formData.append("taskId", "1")
        formData.append("dancerId", "1")

        val token = window.localStorage.getItem("jwt")
        window.fetch("http://localhost:8080/submissions", org.w3c.fetch.RequestInit(
            method = "POST",
            headers = kotlin.js.json("Authorization" to "Bearer $token"),
            body = formData
        )).then {
            if (it.ok) window.alert("Wysłano wideo do oceny!")
            else window.alert("Błąd serwera: ${it.status}")
        }.catch { window.alert("Brak połączenia z backendem.") }
    }

    fun deleteSubmissionAPI(id: Int) {
        val token = window.localStorage.getItem("jwt")
        window.fetch("http://localhost:8080/submissions/$id", org.w3c.fetch.RequestInit(
            method = "DELETE",
            headers = kotlin.js.json("Authorization" to "Bearer $token")
        )).then {
            if (it.ok) window.alert("Usunięto nagranie - pomyślnie")
            else window.alert("Błąd: ${it.status}")
        }.catch { window.alert("Brak połączenia z backendem.") }
    }
}