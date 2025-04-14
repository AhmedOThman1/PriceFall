package com.pricefall.FCM;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;


public interface FCM_Interface {


    @POST("fcm/send")
    @Headers("Content-Type: application/json")
    public Call<JsonObject> pushNotification(@Header("Authorization") String Authorization,
                                             @Body() PushNotification notification
    );
}
