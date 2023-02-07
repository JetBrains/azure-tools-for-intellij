///**
// * Copyright (c) 2022 JetBrains s.r.o.
// *
// * All rights reserved.
// *
// * MIT License
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
// * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
// * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
// * the Software.
// *
// * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
// * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// * SOFTWARE.
// */
//
//package org.jetbrains.plugins.azure.functions.coreTools
//
//import com.google.gson.annotations.SerializedName
//import com.intellij.ide.ui.IdeUiService
//import com.intellij.util.net.ssl.CertificateManager
//import okhttp3.OkHttpClient
//import retrofit2.Call
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import retrofit2.http.GET
//import retrofit2.http.Headers
//import retrofit2.http.Url
//
//@Suppress("UnstableApiUsage")
//interface FunctionsCoreToolsReleaseFeedService {
//    companion object {
//        fun createInstance(): FunctionsCoreToolsReleaseFeedService =
//                Retrofit.Builder()
//                        .addConverterFactory(GsonConverterFactory.create())
//                        .baseUrl("https://functionscdn.azureedge.net/public/")
//                        .client(createHttpClient())
//                        .build()
//                        .create(FunctionsCoreToolsReleaseFeedService::class.java)
//
//        private fun createHttpClient(): OkHttpClient {
//            val httpClientBuilder = OkHttpClient.Builder()
//            runCatching { // For unit tests - IdeUiService.getInstance() throws NullReferenceException
//                IdeUiService.getInstance()?.sslSocketFactory?.let {
//                    // Inject IDEA SSL socket factory and trust manager
//                    httpClientBuilder.sslSocketFactory(it, CertificateManager.getInstance().trustManager)
//                }
//            }
//            return httpClientBuilder.build()
//        }
//    }
//
//    @GET
//    @Headers("Accept: application/json")
//    fun getReleaseFeed(@Url feedUrl: String): Call<ReleaseFeed>
//}
//
//class ReleaseFeed(@SerializedName("tags") val tags: Map<String, Tag> = mapOf(),
//                  @SerializedName("releases") val releases: Map<String, Release> = mapOf())
//
//class Tag(@SerializedName("release") val release: String?,
//          @SerializedName("releaseQuality") val releaseQuality: String?,
//          @SerializedName("hidden") val hidden: Boolean)
//
//class Release(@SerializedName("templates") val templates: String?,
//              @SerializedName("coreTools") val coreTools: List<ReleaseCoreTool> = listOf())
//
//class ReleaseCoreTool(@SerializedName("OS") val os: String?,
//                      @SerializedName("Architecture") val architecture: String?,
//                      @SerializedName("downloadLink") val downloadLink: String?,
//                      @SerializedName("sha2") val sha2: String?,
//                      @SerializedName("size") val size: String?,
//                      @SerializedName("default") val default: Boolean)