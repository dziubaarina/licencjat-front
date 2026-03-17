package com.licencjat.licencjat_frontend

import kotlinx.browser.window
import org.w3c.files.Blob

object ApiService {

    private const val BASE = "http://localhost:8080"

    private fun token() = window.localStorage.getItem("jwt") ?: ""

    // ==========================================
    // AUTH
    // ==========================================

    // POST /auth/login -> { token, role }
    fun login(email: String, password: String): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/auth/login", org.w3c.fetch.RequestInit(
            method = "POST",
            headers = kotlin.js.json("Content-Type" to "application/json"),
            body = JSON.stringify(kotlin.js.json("email" to email, "password" to password))
        )).then { response ->
            if (response.ok) response.json()
            else {
                response.json().then { err: dynamic ->
                    throw Exception("Błąd logowania: ${response.status}")
                }
            }
        }
    }

    // ==========================================
    // TASKS
    // ==========================================

    // GET /tasks -> List<TaskResponse>
    fun fetchTasks(): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/tasks", org.w3c.fetch.RequestInit(
            method = "GET",
            headers = kotlin.js.json(
                "Authorization" to "Bearer ${token()}",
                "Content-Type" to "application/json"
            )
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd pobierania zadań: ${response.status}")
        }
    }

    // POST /tasks (multipart) -> TaskResponse
    fun createTask(
        title: String,
        description: String,
        deadline: String,
        choreographerId: Long,
        file: dynamic
    ): kotlin.js.Promise<dynamic> {
        val formData = org.w3c.xhr.FormData()
        formData.append("title", title)
        formData.append("description", description)
        formData.append("deadline", deadline)
        formData.append("choreographerId", choreographerId.toString())
        formData.append("file", file as Blob)

        return window.fetch("$BASE/tasks", org.w3c.fetch.RequestInit(
            method = "POST",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}"),
            body = formData
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd tworzenia zadania: ${response.status}")
        }
    }

    // ==========================================
    // SUBMISSIONS
    // ==========================================

    // ZMIANA: Zamiast window.alert, funkcja teraz zwraca Promise do DancerView
    fun uploadVideo(file: dynamic): kotlin.js.Promise<dynamic> {
        val formData = org.w3c.xhr.FormData()
        formData.append("file", file as Blob)
        formData.append("taskId", "1")
        formData.append("dancerId", "1")

        return window.fetch("$BASE/submissions", org.w3c.fetch.RequestInit(
            method = "POST",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}"),
            body = formData
        )).then { response ->
            if (response.ok) response
            else throw Exception("Błąd serwera: ${response.status}")
        }
    }

    fun deleteSubmissionAPI(id: Int) {
        window.fetch("$BASE/submissions/$id", org.w3c.fetch.RequestInit(
            method = "DELETE",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}")
        )).then {
            if (it.ok) window.alert("Usunięto nagranie - pomyślnie")
            else window.alert("Błąd: ${it.status}")
        }.catch { window.alert("Brak połączenia z backendem.") }
    }

    // POBIERANIE WSZYSTKICH NAGRAŃ (SUBMISSIONS)
    fun fetchSubmissions(): kotlin.js.Promise<dynamic> {
        val token = window.localStorage.getItem("jwt") ?: ""
        return window.fetch("$BASE/submissions", org.w3c.fetch.RequestInit(
            method = "GET",
            headers = kotlin.js.json("Authorization" to "Bearer $token")
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd pobierania nagrań: ${response.status}")
        }
    }
}