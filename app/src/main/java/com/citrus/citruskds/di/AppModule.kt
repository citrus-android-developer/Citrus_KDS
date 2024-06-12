package com.citrus.citruskds.di


import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.citrus.citruskds.util.Prefs
import com.citrus.citruskds.commonData.ApiService
import com.citrus.citruskds.util.Constants
import com.citrus.citruskds.util.Constants.sha3_256
import com.citrus.citruskds.util.PrintUtil
import com.citrus.citruskds.util.PrinterDetecter
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
interface AppModule {
    companion object {
        private const val DEFAULT_CONNECT_TIME = 10L
        private const val DEFAULT_WRITE_TIME = 30L
        private const val DEFAULT_READ_TIME = 30L

        @Provides
        @Singleton
        fun okHttpClient(): OkHttpClient {
            val okHttpClientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()

            return okHttpClientBuilder
                .addInterceptor {
                    val request = it.request().newBuilder()
                        .addHeader(
                            "ApiKey",
                            "CitrusCompassKDS".sha3_256()
                        ).addHeader("Content-Type", "application/json")
                        .build()
                    it.proceed(request)
                }
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .connectTimeout(DEFAULT_CONNECT_TIME, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_WRITE_TIME, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_READ_TIME, TimeUnit.SECONDS)
                .build()
        }


        @Provides
        @Singleton
        fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }


        @Provides
        @Singleton
        fun provideApiService(retrofit: Retrofit): ApiService =
            retrofit.create(ApiService::class.java)

        @Provides
        @Singleton
        fun providePref(application: Application): Prefs =
            Prefs(application)


        @Provides
        @Singleton
        fun providePrintUtil(
            application: Application,
            printerDetecter: PrinterDetecter
        ): PrintUtil =
            PrintUtil(application, printerDetecter)

        @Provides
        @Singleton
        fun provideSharedPreference(application: Application): SharedPreferences =
            application.getSharedPreferences(
                Constants.SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )

    }

}

