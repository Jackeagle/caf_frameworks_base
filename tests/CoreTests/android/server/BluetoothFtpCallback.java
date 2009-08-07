/*
 * Copyright (c) 2009, Code Aurora Forum. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the name of Code Aurora nor
 *      the names of its contributors may be used to endorse or promote
 *      products derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package android.server;

import android.bluetooth.obex.IBluetoothFtpCallback;
import android.os.RemoteException;

import junit.framework.Assert;

import java.lang.String;
import java.util.List;
import java.util.Map;

/**
 * IBluetoothFtp stubs--required for unit testing to remove Binder dependencies.
 */
public class BluetoothFtpCallback extends IBluetoothFtpCallback.Stub
{
    public boolean mOnCreateSessionCompleteCalled = false;
    public boolean mOnCreateSessionCompleteError = false;

    public void onCreateSessionComplete(boolean isError) throws RemoteException {
        mOnCreateSessionCompleteCalled = true;
        mOnCreateSessionCompleteError = isError;
    }

    public boolean mOnCreateFolderCompleteCalled = false;
    public boolean mOnCreateFolderCompleteError = false;
    public void onCreateFolderComplete(String folder, boolean isError) throws RemoteException {
        mOnCreateFolderCompleteCalled = true;
        mOnCreateFolderCompleteError = isError;
    }

    public boolean mOnChangeFolderCompleteCalled = false;
    public boolean mOnChangeFolderCompleteError = false;
    public void onChangeFolderComplete(String folder, boolean isError) throws RemoteException {
        mOnChangeFolderCompleteCalled = true;
        mOnChangeFolderCompleteError = isError;
    }

    public boolean mOnListFolderCompleteCalled = false;
    public boolean mOnListFolderCompleteError = false;
    public boolean mOnListFolderCompleteExpectEmpty = false;
    public void onListFolderComplete(List<Map> result, boolean isError) throws RemoteException {
        if (!mOnListFolderCompleteExpectEmpty) {
            Assert.assertEquals(result.get(0).get("Name"),"file0");
            Assert.assertEquals(result.get(1).get("Name"),"file1");
        }
        mOnListFolderCompleteCalled = true;
        mOnListFolderCompleteError = isError;
    }

    public boolean mOnGetFileCompleteCalled = false;
    public boolean mOnGetFileCompleteError = false;
    public void onGetFileComplete(String localFilename, String remoteFilename, boolean isError) throws RemoteException {
        mOnGetFileCompleteCalled = true;
        mOnGetFileCompleteError = isError;
    }

    public boolean mOnPutFileCompleteCalled = false;
    public boolean mOnPutFileCompleteError = false;
    public void onPutFileComplete(String localFilename, String remoteFilename, boolean isError) throws RemoteException {
        mOnPutFileCompleteCalled = true;
        mOnPutFileCompleteError = isError;
    }

    public boolean mOnObexSessionClosedCalled = false;
    public void onObexSessionClosed() {
        mOnObexSessionClosedCalled = true;
    }

    public boolean mOnDeleteCompleteCalled = false;
    public boolean mOnDeleteCompleteError = false;
    public void onDeleteComplete(String name, boolean isError) throws RemoteException {
        mOnDeleteCompleteCalled = true;
        mOnDeleteCompleteError = isError;
    }
}
