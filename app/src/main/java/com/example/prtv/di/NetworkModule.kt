package com.example.prtv.di

import android.content.Context
import com.example.prtv.data.ClockSync
import com.example.prtv.data.SigningCertificate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val packageName = context.packageName
        val certSha1 = SigningCertificate.sha1Hex(context)
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                // Identify the app so a key restricted to "Android apps" is accepted.
                val builder = chain.request().newBuilder().header("X-Android-Package", packageName)
                if (certSha1 != null) builder.header("X-Android-Cert", certSha1)
                val response = chain.proceed(builder.build())
                // Keep the schedule clock in step with the server clock.
                response.headers.getDate("Date")?.let {
                    ClockSync.update(it.time, System.currentTimeMillis())
                }
                response
            }
            .build()
    }
}
