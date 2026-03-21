package com.licencjat.licencjat_frontend

import kotlinx.browser.window
import org.w3c.files.Blob

object ApiService {

    private const val BASE = "http://localhost:8080"

    private fun token() = window.localStorage.getItem("jwt") ?: ""

    // ==========================================
    // AUTH
    // ==========================================

    fun login(email: String, password: String): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/auth/login", org.w3c.fetch.RequestInit(
            method = "POST",
            headers = kotlin.js.json("Content-Type" to "application/json"),
            body = JSON.stringify(kotlin.js.json("email" to email, "password" to password))
        )).then { response ->
            if (response.ok) response.json()
            else {
                response.json().then { _: dynamic ->
                    throw Exception("Błąd logowania: ${response.status}")
                }
            }
        }
    }

    // ==========================================
    // TASKS
    // ==========================================

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

    fun createTask(title: String, desc: String, deadline: String, choreoId: Long, file: dynamic): kotlin.js.Promise<dynamic> {
        val formData = org.w3c.xhr.FormData()
        formData.append("title", title)
        formData.append("description", desc)
        formData.append("deadline", deadline)
        formData.append("choreographerId", choreoId.toString())
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
    // SUBMISSIONS (NAGRANIA)
    // ==========================================

    // NAPRAWIONO: przyjmuje prawdziwy taskId i dancerId
    fun uploadVideoForTask(file: dynamic, taskId: Int, dancerId: Int): kotlin.js.Promise<dynamic> {
        val formData = org.w3c.xhr.FormData()
        formData.append("file", file as Blob)
        formData.append("taskId", taskId.toString())
        formData.append("dancerId", dancerId.toString())

        return window.fetch("$BASE/submissions", org.w3c.fetch.RequestInit(
            method = "POST",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}"),
            body = formData
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd serwera: ${response.status}")
        }
    }

    // Zachowane dla kompatybilności wstecznej (używa taskId=1, dancerId=1)
    fun uploadVideo(file: dynamic): kotlin.js.Promise<dynamic> {
        return uploadVideoForTask(file, 1, 1)
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

    fun fetchSubmissionsForDancer(dancerId: Int): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/submissions/dancer/$dancerId", org.w3c.fetch.RequestInit(
            method = "GET",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}")
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd: ${response.status}")
        }
    }

    fun fetchSubmissionsForTask(taskId: Int): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/submissions/task/$taskId", org.w3c.fetch.RequestInit(
            method = "GET",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}")
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd: ${response.status}")
        }
    }

    // ==========================================
    // USERS (DLA ADMINA)
    // ==========================================

    fun fetchUsers(): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/users", org.w3c.fetch.RequestInit(
            method = "GET",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}")
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd: ${response.status}")
        }
    }

    // NOWE: zmiana statusu aktywności użytkownika (dla admina)
    fun setUserStatus(userId: Int, active: Boolean): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/admin/users/$userId/status?active=$active", org.w3c.fetch.RequestInit(
            method = "PATCH",
            headers = kotlin.js.json(
                "Authorization" to "Bearer ${token()}",
                "Content-Type" to "application/json"
            )
        )).then { response ->
            if (response.ok) response
            else throw Exception("Błąd zmiany statusu: ${response.status}")
        }
    }
}