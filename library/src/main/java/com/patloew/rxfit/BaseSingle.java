package com.patloew.rxfit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

/* Copyright (C) 2015 Michał Charmas (http://blog.charmas.pl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ---------------
 *
 * FILE MODIFIED by Patrick Löwenstein, 2016
 *
 */
abstract class BaseSingle<T> extends BaseRx<T> implements SingleOnSubscribe<T> {

    final Map<GoogleApiClient, SingleEmitter<T>> subscriptionInfoMap = new ConcurrentHashMap<>();

    protected BaseSingle(@NonNull RxFit rxFit, Long timeout, TimeUnit timeUnit) {
        super(rxFit, timeout, timeUnit);
    }

    protected BaseSingle(@NonNull Context ctx, @NonNull Api<? extends Api.ApiOptions.NotRequiredOptions>[] services, Scope[] scopes) {
        super(ctx, services, scopes);
    }

    @Override
    public final void subscribe(SingleEmitter<T> subscriber) throws Exception {
        final GoogleApiClient apiClient = createApiClient(new ApiClientConnectionCallbacks(subscriber));
        subscriptionInfoMap.put(apiClient, subscriber);

        try {
            apiClient.connect();
        } catch (Throwable ex) {
            if (!subscriber.isDisposed()) {
                subscriber.onError(ex);
            }
        }

        subscriber.setCancellable(() -> {
            if (apiClient.isConnected() || apiClient.isConnecting()) {
                onUnsubscribed(apiClient);
                apiClient.disconnect();
            }

            subscriptionInfoMap.remove(apiClient);
        });
    }

    protected abstract void onGoogleApiClientReady(GoogleApiClient apiClient, SingleEmitter<T> subscriber);

    protected final void handleResolutionResult(int resultCode, ConnectionResult connectionResult) {
        for (Map.Entry<GoogleApiClient, SingleEmitter<T>> entry : subscriptionInfoMap.entrySet()) {
            if (!entry.getValue().isDisposed()) {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        entry.getKey().connect();
                    } catch (Throwable ex) {
                        entry.getValue().onError(ex);
                    }
                } else {
                    entry.getValue().onError(new GoogleAPIConnectionException("Error connecting to GoogleApiClient, resolution was not successful.", connectionResult));
                }
            }
        }
    }

    protected class ApiClientConnectionCallbacks extends BaseRx.ApiClientConnectionCallbacks {

        final protected SingleEmitter<T> subscriber;

        private GoogleApiClient apiClient;

        private ApiClientConnectionCallbacks(SingleEmitter<T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onConnected(Bundle bundle) {
            try {
                onGoogleApiClientReady(apiClient, subscriber);
            } catch (Throwable ex) {
                if (!subscriber.isDisposed()) {
                    subscriber.onError(ex);
                }
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            if (!subscriber.isDisposed()) {
                subscriber.onError(new GoogleAPIConnectionSuspendedException(cause));
            }
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            if(handleResolution && connectionResult.hasResolution()) {
                observableSet.add(BaseSingle.this);

                if(!ResolutionActivity.isResolutionShown()) {
                    Intent intent = new Intent(ctx, ResolutionActivity.class);
                    intent.putExtra(ResolutionActivity.ARG_CONNECTION_RESULT, connectionResult);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(intent);
                }
            } else if (!subscriber.isDisposed()) {
                subscriber.onError(new GoogleAPIConnectionException(
                        "Error connecting to GoogleApiClient.", connectionResult));
            }
        }

        public void setClient(GoogleApiClient client) {
            this.apiClient = client;
        }
    }
}
