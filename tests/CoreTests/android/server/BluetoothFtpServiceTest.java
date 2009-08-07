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

/**
 * Unit testing of BluetoothFtpService (Java)
 */
package android.server;

import junit.framework.TestCase;

import android.bluetooth.obex.BluetoothObexIntent;
import android.bluetooth.obex.IBluetoothFtpCallback;
import android.content.Context;
import android.content.Intent;
import android.test.mock.MockContext;

public class BluetoothFtpServiceTest extends TestCase {
    /**
     * Unit test members
     */
    private BluetoothFtpService mFtpService = null;
    private IBluetoothFtpCallback mFtpCallback = null;

    /**
     * Extended MockContext (to catch intents)
     */
    public class FtpServiceTestContext extends MockContext {
        /** @override */
        public void sendBroadcast(Intent intent, String receiverPermission) {
            if (intent.getAction() == BluetoothObexIntent.PROGRESS_ACTION) {
                // HACK: hard-coded checks to ensure proper progress intents.
                assertEquals(intent.getStringExtra(BluetoothObexIntent.OBJECT_FILENAME), "/tmp/asdf");
                assertEquals(intent.getIntExtra(BluetoothObexIntent.BYTES_TRANSFERRED,-1), 50);
                assertEquals(intent.getIntExtra(BluetoothObexIntent.OBJECT_SIZE,-1), 150);
            } else if (intent.getAction() == BluetoothObexIntent.TX_COMPLETE_ACTION) {
                assertEquals(intent.getStringExtra(BluetoothObexIntent.OBJECT_FILENAME), "/tmp/asdf");
            } else if (intent.getAction() == BluetoothObexIntent.RX_COMPLETE_ACTION) {
                assertEquals(intent.getStringExtra(BluetoothObexIntent.OBJECT_FILENAME), "/tmp/rx_vcard");
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * Extended BluetoothOppService class useful for testing.
     */
    public class BluetoothFtpServiceStubbed extends BluetoothFtpService {
        public BluetoothFtpServiceStubbed(Context context) {
            super(context);
        }

        /* Overrides of native methods for unit testing purposes.
         */
        @Override
        protected boolean initNative() {
            return true;
        }

        @Override
        protected void cleanupNative() {
            return;
        }

        @Override
        protected boolean createSessionNative(String address) {
            assertEquals(address,"DE:AD:B3:3F:DE:AD");
            mFtpService.onCreateSessionComplete("some session", "DE:AD:B3:3F:DE:AD", false);
            return true;
        }

        @Override
        protected boolean closeSessionNative(String session) {
            assertEquals(session,"some session");
            return true;
        }

        @Override
        protected boolean changeFolderNative(String session, String folder) {
            assertEquals(session,"some session");
            assertEquals(folder,"/some/new/folder");
            mFtpService.onChangeFolderComplete(session, folder, false);
            return true;
        }

        @Override
        protected boolean createFolderNative(String session, String folder) {
            assertEquals(session,"some session");
            assertEquals(folder,"/some/new/folder");
            mFtpService.onCreateFolderComplete(session, folder, false);
            return true;
        }

        @Override
        protected boolean deleteNative(String session, String name) {
            assertEquals(session,"some session");
            assertEquals(name,"/some/new/folder");
            mFtpService.onDeleteComplete(session, name, false);
            return true;
        }

        public boolean mListFolderEmptyFolder = false;
        @Override
        protected boolean listFolderNative(String session) {
            assertEquals(session,"some session");
            if (mListFolderEmptyFolder) {
	            mFtpService.onListFolderComplete(session, null, false);
            } else {
	            ObjectProperties[] result = new ObjectProperties[2];
	            result[0] = new ObjectProperties("file0",null,0,null,0,0,0);
	            result[1] = new ObjectProperties("file1",null,0,null,0,0,0);
	            mFtpService.onListFolderComplete(session, result, false);
            }
            return true;
        }

        @Override
        protected boolean getFileNative(String session, String localFilename, String remoteFilename) {
            assertEquals(session,"some session");
            assertEquals(localFilename,"/local/asdf");
            assertEquals(remoteFilename,"/remote/asdf");
            mFtpService.onGetFileComplete(session, localFilename, remoteFilename, false);
            return true;
        }

        @Override
        protected boolean putFileNative(String session, String localFilename, String remoteFilename) {
            assertEquals(session,"some session");
            assertEquals(localFilename,"/local/asdf");
            assertEquals(remoteFilename,"/remote/asdf");
            mFtpService.onPutFileComplete(session, localFilename, remoteFilename, false);
            return true;
        }

        @Override
        protected boolean cancelTransferNative(String transfer) {
            return true;
        }

        boolean mGetPropertiesReturnObjName = false;
        @Override
        protected TransferProperties obexTransferGetPropertiesNative(String transfer) {
            TransferProperties tp = null;
            if (mGetPropertiesReturnObjName) {
		tp = new TransferProperties("asdf",150,"/remote/asdf");
            } else {
		tp = new TransferProperties("asdf",150,"/local/asdf");
            }
            return tp;
        }

        public boolean verifyGetFilePostRequestDbItem() {
            BluetoothObexDatabase.TransferDbItem dbItem = mSessionDb.getByFilename("/local/asdf");
            assertNotNull(dbItem);
            assertFalse(dbItem.mIsServer);
            assertEquals(dbItem.mDirection, BluetoothObexDatabase.TransferDirection.RX);
            assertEquals(dbItem.getFilename(), "/local/asdf");
            assertEquals(dbItem.getTransfer(), "transfer name from request");
            assertEquals(dbItem.getSession().getAddress(), "DE:AD:B3:3F:DE:AD");
            return true;
        }

        public boolean verifyPutFilePostRequestDbItem() {
            BluetoothObexDatabase.TransferDbItem dbItem = mSessionDb.getByFilename("/local/asdf");
            assertNotNull(dbItem);
            assertFalse(dbItem.mIsServer);
            assertEquals(dbItem.mDirection, BluetoothObexDatabase.TransferDirection.TX);
            assertEquals(dbItem.getFilename(), "/local/asdf");
            assertEquals(dbItem.getTransfer(), "transfer name from request");
            assertEquals(dbItem.getSession().getAddress(), "DE:AD:B3:3F:DE:AD");
            return true;
        }

        public boolean verifyCancelRequestDbItem() {
            BluetoothObexDatabase.TransferDbItem dbItem = mSessionDb.getByFilename("/local/asdf");
            assertNull(dbItem);
            return true;
        }
    }

    /**
     * FTP Service Test Fixture
     */
    @Override
    public void setUp() throws Exception {
        mFtpService = new BluetoothFtpServiceStubbed(new FtpServiceTestContext());
        mFtpCallback = new BluetoothFtpCallback();
    }

    @Override
    public void tearDown() throws Exception {
        mFtpService = null;
        mFtpCallback = null;

        // Force system to finalize all unreferenced objects
        System.gc();
        System.runFinalization();
    }

    // create session helper function
    private boolean createSession() {
        return mFtpService.createSession("DE:AD:B3:3F:DE:AD", mFtpCallback);
    }

    public void testCreateSession() throws Exception {
        assertTrue(createSession());
        assertTrue(((BluetoothFtpCallback)mFtpCallback).mOnCreateSessionCompleteCalled);
        assertFalse(((BluetoothFtpCallback)mFtpCallback).mOnCreateSessionCompleteError);
    }

    public void testCloseSession() throws Exception {
        createSession();
        assertTrue(mFtpService.closeSession("DE:AD:B3:3F:DE:AD"));
        assertNull(mFtpService.getSessionDb().getByAddress("DE:AD:B3:3F:DE:AD"));
    }

    public void testChangeFolder() throws Exception {
        createSession();
        assertTrue(mFtpService.changeFolder("DE:AD:B3:3F:DE:AD", "/some/new/folder"));
        assertTrue(((BluetoothFtpCallback)mFtpCallback).mOnChangeFolderCompleteCalled);
        assertFalse(((BluetoothFtpCallback)mFtpCallback).mOnChangeFolderCompleteError);
    }

    public void testCreateFolder() throws Exception {
        createSession();
        assertTrue(mFtpService.createFolder("DE:AD:B3:3F:DE:AD", "/some/new/folder"));
        assertTrue(((BluetoothFtpCallback)mFtpCallback).mOnCreateFolderCompleteCalled);
        assertFalse(((BluetoothFtpCallback)mFtpCallback).mOnCreateFolderCompleteError);
    }

    public void testDelete() throws Exception {
        createSession();
        assertTrue(mFtpService.delete("DE:AD:B3:3F:DE:AD", "/some/new/folder"));
        assertTrue(((BluetoothFtpCallback)mFtpCallback).mOnDeleteCompleteCalled);
        assertFalse(((BluetoothFtpCallback)mFtpCallback).mOnDeleteCompleteError);
    }

    public void testListFolder() throws Exception {
        createSession();
        assertTrue(mFtpService.listFolder("DE:AD:B3:3F:DE:AD"));
        assertTrue(((BluetoothFtpCallback)mFtpCallback).mOnListFolderCompleteCalled);
        assertFalse(((BluetoothFtpCallback)mFtpCallback).mOnListFolderCompleteError);
    }

    public void testListEmptyFolder() throws Exception {
        createSession();
        ((BluetoothFtpServiceStubbed)mFtpService).mListFolderEmptyFolder = true;
        ((BluetoothFtpCallback)mFtpCallback).mOnListFolderCompleteExpectEmpty = true;
        assertTrue(mFtpService.listFolder("DE:AD:B3:3F:DE:AD"));
        assertTrue(((BluetoothFtpCallback)mFtpCallback).mOnListFolderCompleteCalled);
        assertFalse(((BluetoothFtpCallback)mFtpCallback).mOnListFolderCompleteError);
    }

    public void testGetFile() throws Exception {
        createSession();
        assertTrue(mFtpService.getFile("DE:AD:B3:3F:DE:AD", "/local/asdf", "/remote/asdf"));
        assertTrue(((BluetoothFtpCallback)mFtpCallback).mOnGetFileCompleteCalled);
        assertFalse(((BluetoothFtpCallback)mFtpCallback).mOnGetFileCompleteError);

        assertEquals(mFtpService.onObexRequest("transfer name from request"), "/local/asdf");
        assertTrue(((BluetoothFtpServiceStubbed)mFtpService).verifyGetFilePostRequestDbItem());
    }

    public void testGetFileObjNameReq() throws Exception {
        createSession();
        assertTrue(mFtpService.getFile("DE:AD:B3:3F:DE:AD", "/local/asdf", "/remote/asdf"));
        assertTrue(((BluetoothFtpCallback)mFtpCallback).mOnGetFileCompleteCalled);
        assertFalse(((BluetoothFtpCallback)mFtpCallback).mOnGetFileCompleteError);

        ((BluetoothFtpServiceStubbed)mFtpService).mGetPropertiesReturnObjName = true;
        assertEquals(mFtpService.onObexRequest("transfer name from request"), "/local/asdf");
        assertTrue(((BluetoothFtpServiceStubbed)mFtpService).verifyGetFilePostRequestDbItem());
    }

    public void testPutFile() throws Exception {
        createSession();
        assertTrue(mFtpService.putFile("DE:AD:B3:3F:DE:AD", "/local/asdf", "/remote/asdf"));
        assertTrue(((BluetoothFtpCallback)mFtpCallback).mOnPutFileCompleteCalled);
        assertFalse(((BluetoothFtpCallback)mFtpCallback).mOnPutFileCompleteError);

        assertEquals(mFtpService.onObexRequest("transfer name from request"), "asdf");
        assertTrue(((BluetoothFtpServiceStubbed)mFtpService).verifyPutFilePostRequestDbItem());
    }

    public void testCancelTransfer() throws Exception {
        testPutFile();
        assertTrue(mFtpService.cancelTransfer("DE:AD:B3:3F:DE:AD", "/local/asdf"));
        assertTrue(((BluetoothFtpServiceStubbed)mFtpService).verifyCancelRequestDbItem());
    }

    public void testIsTransferActive() throws Exception {
        testPutFile();
        assertTrue(mFtpService.isTransferActive("/local/asdf"));
    }

    public void testIsConnectionActive() throws Exception {
        createSession();
        assertTrue(mFtpService.isConnectionActive("DE:AD:B3:3F:DE:AD"));
    }
}
