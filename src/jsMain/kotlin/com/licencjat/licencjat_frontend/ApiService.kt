package com.licencjat.licencjat_frontend

import kotlinx.browser.window
import org.w3c.files.Blob

object ApiService {

    private const val BASE = "https://danceinsense.onrender.com"
    private const val CLOUDINARY_CLOUD = "dechlzont"
    private const val CLOUDINARY_PRESET = "danceinsense"
    private const val CLOUDINARY_URL = "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD/video/upload"

    private fun token() = window.localStorage.getItem("jwt") ?: ""

    // ==========================================
    // CLOUDINARY DIRECT UPLOAD
    // Plik idzie bezpośrednio z przeglądarki do Cloudinary,
    // backend dostaje tylko URL — omijamy limit pamięci Render.
    // ==========================================

    fun uploadToCloudinary(file: dynamic): kotlin.js.Promise<dynamic> {
        val formData = org.w3c.xhr.FormData()
        formData.append("file", file as Blob)
        formData.append("upload_preset", CLOUDINARY_PRESET)
        formData.append("resource_type", "video")

        return window.fetch(CLOUDINARY_URL, org.w3c.fetch.RequestInit(
            method = "POST",
            body = formData
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd uploadu do Cloudinary: ${response.status}")
        }
    }

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

    fun fetchUsersWithToken(jwt: String): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/users", org.w3c.fetch.RequestInit(
            method = "GET",
            headers = kotlin.js.json("Authorization" to "Bearer $jwt")
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd: ${response.status}")
        }
    }

    fun registerUser(firstName: String, lastName: String, email: String, password: String): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/users", org.w3c.fetch.RequestInit(
            method = "POST",
            headers = kotlin.js.json("Content-Type" to "application/json"),
            body = JSON.stringify(kotlin.js.json(
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to email,
                "password" to password,
                "role" to "DANCER"
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

    fun updateUserRole(userId: Int, role: String): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/admin/users/$userId/role?role=$role", org.w3c.fetch.RequestInit(
            method = "PATCH",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}")
        )).then { response ->
            if (response.ok) response
            else throw Exception("Błąd zmiany roli: ${response.status}")
        }
    }

    fun deleteUser(userId: Int): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/admin/users/$userId", org.w3c.fetch.RequestInit(
            method = "DELETE",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}")
        )).then { response ->
            if (response.ok) response
            else throw Exception("Błąd usuwania użytkownika: ${response.status}")
        }
    }

    // ==========================================
    // TASKS
    // POPRAWKA: createTask teraz najpierw uploaduje do Cloudinary,
    // potem wysyła URL do backendu jako JSON (nie multipart).
    // Eliminuje OutOfMemoryError na Render free tier.
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
        // Krok 1: upload pliku bezpośrednio do Cloudinary z przeglądarki
        return uploadToCloudinary(file).then<dynamic> { cloudinaryRes: dynamic ->
            val videoUrl = cloudinaryRes.secure_url?.toString()
                ?: throw Exception("Cloudinary nie zwróciło URL")
            // Krok 2: wyślij URL + dane do backendu jako JSON
            window.fetch("$BASE/tasks/url", org.w3c.fetch.RequestInit(
                method = "POST",
                headers = kotlin.js.json(
                    "Authorization" to "Bearer ${token()}",
                    "Content-Type" to "application/json"
                ),
                body = JSON.stringify(kotlin.js.json(
                    "title" to title,
                    "description" to desc,
                    "deadline" to deadline,
                    "choreographerId" to choreoId,
                    "instructionVideoUrl" to videoUrl
                ))
            )).then { response ->
                if (response.ok) response.json()
                else throw Exception("Błąd tworzenia zadania: ${response.status}")
            }
        }
    }

    fun updateTask(taskId: Int, title: String, desc: String, deadline: String): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/tasks/$taskId", org.w3c.fetch.RequestInit(
            method = "PUT",
            headers = kotlin.js.json(
                "Authorization" to "Bearer ${token()}",
                "Content-Type" to "application/json"
            ),
            body = JSON.stringify(kotlin.js.json(
                "title" to title,
                "description" to desc,
                "deadline" to deadline
            ))
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd edycji zadania: ${response.status}")
        }
    }

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
    // POPRAWKA: uploadVideoForTask teraz najpierw uploaduje do Cloudinary,
    // potem wysyła URL do backendu jako JSON (nie multipart).
    // ==========================================

    fun uploadVideoForTask(file: dynamic, taskId: Int, dancerId: Int): kotlin.js.Promise<dynamic> {
        // Krok 1: upload pliku bezpośrednio do Cloudinary z przeglądarki
        return uploadToCloudinary(file).then<dynamic> { cloudinaryRes: dynamic ->
            val videoUrl = cloudinaryRes.secure_url?.toString()
                ?: throw Exception("Cloudinary nie zwróciło URL")
            // Krok 2: wyślij URL + dane do backendu jako JSON
            window.fetch("$BASE/submissions/url", org.w3c.fetch.RequestInit(
                method = "POST",
                headers = kotlin.js.json(
                    "Authorization" to "Bearer ${token()}",
                    "Content-Type" to "application/json"
                ),
                body = JSON.stringify(kotlin.js.json(
                    "taskId" to taskId,
                    "dancerId" to dancerId,
                    "videoUrl" to videoUrl
                ))
            )).then { response ->
                if (response.ok) response.json()
                else throw Exception("Błąd serwera: ${response.status}")
            }
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

    fun resetSubmissionStatus(submissionId: Int): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/submissions/$submissionId/reset", org.w3c.fetch.RequestInit(
            method = "PUT",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}")
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd resetowania statusu: ${response.status}")
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

    fun deleteCommentAPI(commentId: Int): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/comments/$commentId", org.w3c.fetch.RequestInit(
            method = "DELETE",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}")
        )).then { response ->
            if (response.ok) response
            else throw Exception("Błąd usuwania komentarza: ${response.status}")
        }
    }

    fun updateComment(commentId: Int, content: String): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/comments/$commentId", org.w3c.fetch.RequestInit(
            method = "PATCH",
            headers = kotlin.js.json(
                "Authorization" to "Bearer ${token()}",
                "Content-Type" to "application/json"
            ),
            body = JSON.stringify(kotlin.js.json("content" to content))
        )).then { response ->
            if (response.ok) response
            else throw Exception("Błąd edycji komentarza: ${response.status}")
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

    // ==========================================
    // OGŁOSZENIA
    // ==========================================

    fun fetchAnnouncements(): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/announcements", org.w3c.fetch.RequestInit(
            method = "GET"
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd pobierania ogłoszeń")
        }
    }

    fun createAnnouncement(title: String, content: String, type: String, file: dynamic): kotlin.js.Promise<dynamic> {
        val formData = org.w3c.xhr.FormData()
        formData.append("title", title)
        formData.append("content", content)
        formData.append("type", type)
        if (file != null) formData.append("file", file as Blob)

        return window.fetch("$BASE/announcements", org.w3c.fetch.RequestInit(
            method = "POST",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}"),
            body = formData
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd tworzenia ogłoszenia: ${response.status}")
        }
    }

    fun updateAnnouncement(id: Int, title: String, content: String, type: String): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/announcements/$id", org.w3c.fetch.RequestInit(
            method = "PUT",
            headers = kotlin.js.json(
                "Authorization" to "Bearer ${token()}",
                "Content-Type" to "application/json"
            ),
            body = JSON.stringify(kotlin.js.json("title" to title, "content" to content, "type" to type))
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd edycji ogłoszenia: ${response.status}")
        }
    }

    fun deleteAnnouncement(id: Int): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/announcements/$id", org.w3c.fetch.RequestInit(
            method = "DELETE",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}")
        )).then { response ->
            if (response.ok) response
            else throw Exception("Błąd usuwania ogłoszenia: ${response.status}")
        }
    }

    // ==========================================
    // CZAT
    // ==========================================

    fun fetchChat(recipientId: Int? = null): kotlin.js.Promise<dynamic> {
        val url = if (recipientId != null) "$BASE/chat/private/$recipientId" else "$BASE/chat"
        return window.fetch(url, org.w3c.fetch.RequestInit(
            method = "GET",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}")
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd pobierania czatu")
        }
    }

    fun sendChatMessage(content: String, recipientIds: List<Int>? = null): kotlin.js.Promise<dynamic> {
        val bodyData = kotlin.js.json("content" to content)
        if (recipientIds != null) bodyData["recipientIds"] = recipientIds.toTypedArray()

        return window.fetch("$BASE/chat", org.w3c.fetch.RequestInit(
            method = "POST",
            headers = kotlin.js.json(
                "Authorization" to "Bearer ${token()}",
                "Content-Type" to "application/json"
            ),
            body = JSON.stringify(bodyData)
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd wysyłania wiadomości: ${response.status}")
        }
    }

    fun editChatMessage(messageId: Int, content: String): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/chat/$messageId", org.w3c.fetch.RequestInit(
            method = "PUT",
            headers = kotlin.js.json(
                "Authorization" to "Bearer ${token()}",
                "Content-Type" to "application/json"
            ),
            body = JSON.stringify(kotlin.js.json("content" to content))
        )).then { response ->
            if (response.ok) response.json()
            else throw Exception("Błąd edycji wiadomości")
        }
    }

    fun deleteChatMessage(messageId: Int): kotlin.js.Promise<dynamic> {
        return window.fetch("$BASE/chat/$messageId", org.w3c.fetch.RequestInit(
            method = "DELETE",
            headers = kotlin.js.json("Authorization" to "Bearer ${token()}")
        )).then { response ->
            if (response.ok) response
            else throw Exception("Błąd usuwania wiadomości")
        }
    }
}