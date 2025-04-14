package com.pricefall.FCM;

import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;

public class FCM_ApiClient {

    private static final String BASE_URL = "https://fcm.googleapis.com";
    private static final String ServerKey = "AAAAb_jUSQ0:APA91bGIpfBbTgLa5ci1fIqMFRe-Vt4dIB3yPtKVaK6UBolHP-pv43YkjSLKD9ROUNFHaWHwRvWOsbQWUBj2TxB_nK9NRTisrtcbOdyEPNsCzQiz1pP5Tag2OBP_l_GFCEpa1q7wK8TK";
    private final FCM_Interface fcm_interface;
    private static FCM_ApiClient INSTANCE;

    public FCM_ApiClient() {

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        fcm_interface = retrofit.create(FCM_Interface.class);

    }

    public static FCM_ApiClient getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new FCM_ApiClient();
        }
        return INSTANCE;
    }

    public Call<JsonObject> pushNotification(@Body() PushNotification notification) {
        return fcm_interface.pushNotification("key=" + ServerKey, notification);
    }


}
