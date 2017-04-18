package com.patloew.rxfit;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;

import io.reactivex.SingleEmitter;
import io.reactivex.functions.Function;

/* Copyright 2016 Patrick Löwenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
class SingleResultCallBack<T extends Result, R> implements ResultCallback<T> {

    private final SingleEmitter<R> subscriber;
    private final Function<T, R> mapper;

    static <T extends Result, R> ResultCallback<T> get(@NonNull SingleEmitter<R> subscriber, @NonNull Function<T, R> mapper) {
        return new SingleResultCallBack<>(subscriber, mapper);
    }

    static <T extends Result> ResultCallback<T> get(@NonNull SingleEmitter<T> subscriber) {
        return new SingleResultCallBack<>(subscriber, input -> input);
    }

    private SingleResultCallBack(@NonNull SingleEmitter<R> subscriber, @NonNull Function<T, R> mapper) {
        this.subscriber = subscriber;
        this.mapper = mapper;
    }

    @Override
    public void onResult(@NonNull T result) {
        if (subscriber.isDisposed()) {
            return;
        }

        if (!result.getStatus().isSuccess()) {
            subscriber.onError(new StatusException(result.getStatus()));
        } else {
            try {
                subscriber.onSuccess(mapper.apply(result));
            } catch(Exception e) {
                subscriber.onError(e);
            }
        }
    }
}
