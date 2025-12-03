package com.example.a_eyes

import android.graphics.BitmapFactory
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray

object ImageAnalyzer {

    private const val MISTRAL_API_KEY = "90vLx9en23tpQt7dHZUgSe3aCYtp5S5M"
    private const val MISTRAL_API_URL = "https://api.mistral.ai/v1/chat/completions"

    private val client = OkHttpClient()

    suspend fun analyzeImage(photoFile: File): String? {
        // 1) Ask BLIP VQA: "Is this a text, an object, or a scenery?"
        Log.d("VQA", "First Question Asked")
        val vqaAnswer = uploadToCloudinary(photoFile)?.also { url ->
            Log.d("VQA", "Uploaded image URL: $url")
        }?.let { url ->
            askMistralVQA(url,"In this picture, is the main subject a text, an object, or a scenery? You may only answer with the following words in lowercase (text, object, scenery)")
        }?.also { answer ->
            Log.d("VQA", "VQA answer: $answer")
        } ?: run {
            Log.e("VQA", "Upload or VQA failed, returning null")
            return null
        }

        Log.d("VQA", "Second Question Asked")

        return when {
            vqaAnswer.contains("text", ignoreCase = true) -> {
                // Run OCR on photoFile
                Log.d("OCR", "OCR done")
                val a = runOCR(photoFile)
                Log.d("OCR", "Result : $a")
                a
            }
            vqaAnswer.contains("object", ignoreCase = true) -> {
                // Ask BLIP to describe the object
                Log.d("Mistral", "Object Description done")
                val a = askMistralDescription(photoFile, "Describe the main object in this image with important details, in a sentence or two.")
                Log.d("Mistral", "Result : $a")
                a
            }
            vqaAnswer.contains("scenery", ignoreCase = true) -> {
                // Ask BLIP to describe the scenery
                Log.d("Mistral", "Scenery Description done")
                val a = askMistralDescription(photoFile, "Describe the scene in this image with details, in a sentence or two.")
                Log.d("Mistral", "Result : $a")
                a
            }
            else -> {
                "I'm not sure what this image shows."
            }
        }
    }

    private fun askMistralVQA(imageUrl: String, question: String): String? {
        val jsonBody = JSONObject()
            .put("model", "pixtral-12b-2409")
            .put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", question)
                    })
                    put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", imageUrl)
                    })
                })
            }))

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(MISTRAL_API_URL)
            .addHeader("Authorization", "Bearer $MISTRAL_API_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.d("askMistralVQA", "HTTP response code: ${response.code}")
                Log.d("askMistralVQA", "Raw response: $responseBody")

                if (!response.isSuccessful || responseBody == null) return null

                val responseJson = JSONObject(responseBody)
                val choices = responseJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    choices.getJSONObject(0).getJSONObject("message").getString("content")
                } else null
            }
        } catch (e: Exception) {
            Log.e("askMistralVQA", "Exception: ${e.message}")
            null
        }
    }

    fun translateToFrench(text: String, shouldTranslate: Boolean): String {
        if (!shouldTranslate) return text

        val jsonBody = JSONObject()
            .put("model", "mistral-small-latest")
            .put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "text")
                        put("text", "Only translate the following English text to French (keep your answer in French): \"$text\"")
                    })
                })
            }))

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(MISTRAL_API_URL)
            .addHeader("Authorization", "Bearer $MISTRAL_API_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody == null) return text

                val responseJson = JSONObject(responseBody)
                val choices = responseJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    choices.getJSONObject(0).getJSONObject("message").getString("content")
                } else text
            }
        } catch (e: Exception) {
            e.printStackTrace()
            text
        }
    }

    private fun uploadToCloudinary(imageFile: File): String? {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", imageFile.name, imageFile.asRequestBody("image/*".toMediaType()))
            .addFormDataPart("upload_preset", "aeyes_test") // config depuis ton Cloudinary dashboard
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/dehlosaa0/image/upload")
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("uploadToCloudinary", "Upload failed: ${response.code}")
                    null
                } else {
                    val responseJson = JSONObject(response.body?.string() ?: return null)
                    responseJson.getString("secure_url") // lien direct
                }
            }
        } catch (e: Exception) {
            Log.e("uploadToCloudinary", "Error: ${e.message}")
            null
        }
    }


    private fun askMistralDescription(imageFile: File, prompt: String): String? {
        // Same as askBLIPVQA but question is prompt
        return uploadToCloudinary(imageFile)?.let { askMistralVQA(it, prompt) }
    }

    private suspend fun runOCR(imageFile: File): String? {
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        return try {
            val result = recognizer.process(image).await()
            result.text.ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }
}