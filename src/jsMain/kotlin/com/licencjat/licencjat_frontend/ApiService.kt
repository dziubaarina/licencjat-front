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
            else response.json().then { _: dynamic -> throw Exception("Błąd logowania: ${response.status}") }
        }
    }

    // ==========================================
    // USERS
    // ==========================================

    fun fetchUsers(): kotlin.js.Promise<dynamic> {
        return fetchUsersWithToken(token())
    }

    // Wersja z jawnym tokenem — używana zaraz po logowaniu żeby uniknąć race condition
    fun fetchUsersWithToken(jwt: String): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/users", org.w3c.fetch.RequestInit(
            method = "GET",
            headers = kotlin.js.json("Authorization" to "Bearer $jwt")
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd: ${response.status}")
        }
    }

    // Rejestracja nowego użytkownika — POST /users (publiczny endpoint)
    fun registerUser(firstName: String, lastName: String, email: String, password: String): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/users", org.w3c.fetch.RequestInit(
            method = "POST",
            headers = kotlin.js.json("Content-Type" to "application/json"),
            body = JSON.stringify(kotlin.js.json(
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to email,
                "password" to password,
                "role" to "DANCER"   // domyślna rola przy rejestracji
            ))
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd rejestracji: ${response.status}")
        }
    }

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

    // NOWA FUNKCJA DO USUWANIA ZADAŃ PRZEZ CHOREOGRAFA
    fun deleteTaskAPI(taskId: Int): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/tasks/$taskId", org.w3c.fetch.RequestInit(
            method = "DELETE",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}")
        )).then { response ->
            if (response.ok) response
            else throw Exception("Błąd usuwania zadania: ${response.status}")
        }
    }

    // ==========================================
    // SUBMISSIONS
    // ==========================================

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

    fun gradeSubmission(submissionId: Int, score: Int, feedback: String): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/submissions/$submissionId/grade", org.w3c.fetch.RequestInit(
            method = "PUT",
            headers = kotlin.js.json(
                "Authorization" to "Bearer ${token()}",
                "Content-Type" to "application/json"
            ),
            body = JSON.stringify(kotlin.js.json(
                "submissionId" to submissionId,
                "score" to score,
                "feedback" to feedback
            ))
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd oceniania: ${response.status}")
        }
    }

    fun deleteSubmissionAPI(id: Int): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/submissions/$id", org.w3c.fetch.RequestInit(
            method = "DELETE",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}")
        )).then { response ->
            if (response.ok) response
            else throw Exception("Błąd usuwania: ${response.status}")
        }
    }

    // ==========================================
    // KOMENTARZE
    // ==========================================

    fun fetchComments(submissionId: Int): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/comments/submission/$submissionId", org.w3c.fetch.RequestInit(
            method = "GET",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}")
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd pobierania komentarzy: ${response.status}")
        }
    }

    fun addComment(submissionId: Int, authorId: Int, timestampSeconds: Int, content: String): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/comments", org.w3c.fetch.RequestInit(
            method = "POST",
            headers = kotlin.js.json(
                "Authorization" to "Bearer ${token()}",
                "Content-Type" to "application/json"
            ),
            body = JSON.stringify(kotlin.js.json(
                "submissionId" to submissionId,
                "authorId" to authorId,
                "timestampSeconds" to timestampSeconds,
                "content" to content
            ))
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd dodawania komentarza: ${response.status}")
        }
    }
}