/*
 * Copyright (C) 2016 The Android Open Source Project
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
 */

package android.telephony.mbms;

import android.content.Context;
import android.os.RemoteException;
import android.telephony.MbmsStreamingManager;

import java.util.List;

/**
 * A callback class that is used to receive information from the middleware on MBMS streaming
 * services. An instance of this object should be passed into
 * {@link android.telephony.MbmsStreamingManager#create(Context, MbmsStreamingManagerCallback)}.
 * @hide
 */
public class MbmsStreamingManagerCallback {
    /**
     * Called by the middleware when it has detected an error condition. The possible error codes
     * are listed in {@link MbmsException}.
     * @param errorCode The error code.
     * @param message A human-readable message generated by the middleware for debugging purposes.
     */
    public void onError(int errorCode, String message) {
        // default implementation empty
    }

    /**
     * Called to indicate published Streaming Services have changed.
     *
     * This will only be called after the application has requested
     * a list of streaming services and specified a service class list
     * of interest AND the results of a subsequent getStreamServices
     * call with the same service class list would return different
     * results.
     *
     * @param services a List of StreamingServiceInfos
     *
     */
    public void onStreamingServicesUpdated(List<StreamingServiceInfo> services) {
        // default implementation empty
    }

    /**
     * Called to indicate that the middleware has been initialized and is ready.
     *
     * Before this method is called, calling any method on an instance of
     * {@link android.telephony.MbmsStreamingManager} will result in an {@link MbmsException}
     * being thrown with error code {@link MbmsException#ERROR_MIDDLEWARE_NOT_BOUND}
     * or {@link MbmsException.GeneralErrors#ERROR_MIDDLEWARE_NOT_YET_READY}
     */
    public void onMiddlewareReady() {
        // default implementation empty
    }
}
