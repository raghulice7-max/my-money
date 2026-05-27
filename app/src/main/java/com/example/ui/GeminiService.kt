package com.example.ui

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateMonthlySummary(
        userName: String,
        userEmail: String,
        transactionsList: String,
        budgetsList: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or placeholder.")
            return@withContext getFallbackSummary(userName, userEmail, transactionsList, budgetsList)
        }

        val prompt = """
            You are an expert personal finance advisor. Generate a highly insightful, visually organized and encouraging financial summary for the user's past month.
            
            User's Profile:
            - Name: $userName
            - Email: $userEmail
            
            Financial Data For This Month:
            $transactionsList
            
            Budgets Defined:
            $budgetsList
            
            Goals:
            1. Summarize the Net Balance, Total Income, and Total Expenses.
            2. Compute the savings rate (Savings / Income * 100).
            3. Highlight areas where they spent well or where they overspent their budget limits.
            4. Provide 3 specific, highly actionable, friendly financial tips based on their spending.
            
            Format: Please provide your response inside clear sections with markdown headers (##), bold text, bullet points, and nice spacing. Keep it extremely clean, friendly, and structured like a premium polished professional report. Do not use complex markup, just plain markdown. Start with a warm greeting to the user.
        """.trimIndent()

        try {
            // Construct JSON request using standard modern JSONObject
            val part = JSONObject().put("text", prompt)
            val content = JSONObject().put("parts", JSONArray().put(part))
            val contents = JSONArray().put(content)
            val requestBodyJson = JSONObject().put("contents", contents)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "No body"
                    Log.e(TAG, "Gemini Request failed with code ${response.code}: $errorBody")
                    return@withContext getFallbackSummary(userName, userEmail, transactionsList, budgetsList)
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    return@withContext getFallbackSummary(userName, userEmail, transactionsList, budgetsList)
                }

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                    if (contentObj != null) {
                        val partsArr = contentObj.optJSONArray("parts")
                        if (partsArr != null && partsArr.length() > 0) {
                            return@withContext partsArr.getJSONObject(0).optString("text", "")
                        }
                    }
                }
                return@withContext getFallbackSummary(userName, userEmail, transactionsList, budgetsList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini", e)
            return@withContext getFallbackSummary(userName, userEmail, transactionsList, budgetsList)
        }
    }

    private fun getFallbackSummary(
        userName: String,
        userEmail: String,
        transactionsList: String,
        budgetsList: String
    ): String {
        return """
            ## Monthly Financial Summary for $userName
            
            Hello $userName! Here is your custom month-end financial summary draft compiled securely:
            
            ### 📈 Performance Overview
            Based on your transactions this month, we compiled your activity:
            - **Email Destination**: $userEmail
            - **Goal**: Keep expenses below your defined budgets.
            
            ### 🔍 Inside Your Numbers
            To review your customized breakdown, please ensure your local internet is connected and your Gemini API Key is configured. Here are a few recommended tips:
            1. **Budget Awareness**: Double-check your category budgets under the "Overview" tab and allocate reasonable amounts.
            2. **Regular Tracking**: Continue logging transactions regularly or approve SMS alerts immediately to build consistent habits.
            3. **Savings Rate**: Aim to save at least 20% of your total income month-over-month.
            
            *Keep up the excellent tracking habit, $userName!*
        """.trimIndent()
    }
}
