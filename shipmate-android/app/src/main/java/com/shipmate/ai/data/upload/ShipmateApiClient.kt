package com.shipmate.ai.data.upload

import com.shipmate.ai.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ShipmateApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.SHIPMATE_API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val callRecordingApiService: CallRecordingApiService =
        retrofit.create(CallRecordingApiService::class.java)
}
