/**
 * Created by YuGang Yang on October 29, 2015.
 * Copyright 2007-2015 Laputapp.com. All rights reserved.
 */
package com.smartydroid.android.starter.kit.retrofit;

import com.smartydroid.android.starter.kit.account.AccountProvider;
import com.smartydroid.android.starter.kit.app.StarterKitApp;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;

public class DefaultHeaderInterceptor implements Interceptor {

  AccountProvider mAccountProvider;
  ApiVersion mApiVersion;

  public DefaultHeaderInterceptor(AccountProvider accountProvider, ApiVersion apiVersion) {
    mAccountProvider = accountProvider;
    mApiVersion = apiVersion;
  }

  @Override public Response intercept(Chain chain) throws IOException {
    Request originalRequest = chain.request();
    Headers.Builder builder = new Headers.Builder();
    builder.add("Content-Encoding", "gzip")
        .add("version-code", StarterKitApp.appInfo().versionCode)
        .add("version-name", StarterKitApp.appInfo().version)
        .add("device", StarterKitApp.appInfo().deviceId)
        .add("channel", StarterKitApp.appInfo().channel)
        .add("platform", "android");
    if (mAccountProvider != null && mAccountProvider.provideToken() != null) {
      builder.add("Authorization", "Bearer " + mAccountProvider.provideToken());
    }
    if (mApiVersion != null && mApiVersion.accept() != null) {
      builder.add("Accept", mApiVersion.accept());
    }
    Request compressedRequest = originalRequest.newBuilder()
        .headers(builder.build())
        .build();
    return chain.proceed(compressedRequest);
  }
}