# Reactive Fit API Library for Android

[![Build Status](https://travis-ci.org/patloew/RxFit.svg?branch=1.x)](https://travis-ci.org/patloew/RxFit) [![codecov](https://codecov.io/gh/patloew/RxFit/branch/1.x/graph/badge.svg)](https://codecov.io/gh/patloew/RxFit/branch/1.x) [ ![Download](https://api.bintray.com/packages/patloew/maven/RxFit/images/download.svg) ](https://bintray.com/patloew/maven/RxFit/_latestVersion) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RxFit-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/3252) [![API](https://img.shields.io/badge/API-9%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=9)

This library wraps the Fit API in [RxJava](https://github.com/ReactiveX/RxJava) Observables and Singles. No more managing GoogleApiClients! Also, the authorization process for using fitness data is handled by the lib.

**The 1.x version of this library is now deprecated. Please upgrade to the RxJava 2 version of this library, available at the [2.x branch](https://github.com/patloew/RxFit/tree/2.x).**

# Usage

Create an RxFit instance once, preferably in your Application's `onCreate()` or by using a dependency injection framework. Make sure to include all the APIs and Scopes that you need for your app. The RxFit class is very similar to the Fitness class provided by the Fit API. Instead of `Fitness.HistoryApi.readData(apiClient, dataReadRequest)` you can use `rxFit.history().read(dataReadRequest)`. Make sure to have the Location and Body Sensors permission from Marshmallow on, if they are needed by your Fit API requests. If the user didn’t already authorize your app for using fitness data, the lib handles showing the authorization dialog.

Example:

```java
// Create one instance and share it
RxFit rxfit = new RxFit(
        context,
        new Api[] { Fitness.HISTORY_API },
        new Scope[] { new Scope(Scopes.FITNESS_ACTIVITY_READ) }
);

DataReadRequest dataReadRequest = new DataReadRequest.Builder()
	    .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
	    .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
	    .bucketBySession(1, TimeUnit.MINUTES)
	    .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
	    .build();

rxFit.history().read(dataReadRequest)
        .flatMapObservable(dataReadResult -> Observable.from(dataReadResult.getBuckets()))
        .subscribe(bucket -> {
        	/* do something */
        });
```

An `RxFitOnExceptionResumeNext` Transformer is available in the lib, which resumes with another Single/Observable when an Exception is thrown, except when the exception was a GoogleAPIConnectionException which was caused by an unresolved resolution.

An optional global default timeout for all Fit API requests made through the library can be set via `rxFit.setDefaultTimeout(...)`. In addition, timeouts can be set when creating a new Observable by providing timeout parameters, e.g. `rxFit.history().read(dataReadRequest, 15, TimeUnit.SECONDS)`. These parameters override the default timeout. When a timeout occurs, a StatusException is provided via `onError()`. The RxJava timeout operators can be used instead, but these do not cancel the Fit API request immediately.

You can also obtain a `Single<GoogleApiClient>`, which connects on subscribe and disconnects on unsubscribe via `GoogleAPIClientSingle.create(...)`.

The following Exceptions are thrown in the lib and provided via `onError()`:

* `StatusException`: When the call to the Fit API was not successful or timed out
* `GoogleAPIConnectionException`: When connecting to the GoogleAPIClient was not successful and the resolution (if available) was also not successful (e.g. when the user does not authorize your app to use fitness data). Resolutions are not handled when using `GoogleAPIClientObservable`.
* `GoogleAPIConnectionSuspendedException`: When the GoogleApiClient connection was suspended.
* `SecurityException`: When you try to call a Fit API without proper permissions.

# Sample

A basic sample app is available in the `sample` project. You need to create an OAuth 2.0 Client ID for the sample app, see the [guide in the Fit API docs](https://developers.google.com/fit/android/get-api-key).

# Setup

The lib is available on jCenter. Add the following to your `build.gradle`:

	dependencies {
	    compile 'com.patloew.rxfit:rxfit:1.5.0'
	    // or, for a RxJava 2 version of this library:
	    compile 'com.patloew.rxfit:rxfit2:2.0.0'
	}

# Testing

When unit testing your app's classes, RxFit behavior can be mocked easily. See the `MainPresenterTest` in the `sample` project for an example test.

# Credits

The code for managing the GoogleApiClient is taken from the [Android-ReactiveLocation](https://github.com/mcharmas/Android-ReactiveLocation) library by Michał Charmas, which is licensed under the Apache License, Version 2.0.

# License

	Copyright 2016 Patrick Löwenstein

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.