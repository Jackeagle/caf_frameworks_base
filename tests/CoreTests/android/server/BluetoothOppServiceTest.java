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
 * Unit testing of BluetoothOppService (Java)
 */
package android.server;

import junit.framework.TestCase;

import android.bluetooth.obex.BluetoothObexIntent;
import android.content.Context;
import android.content.Intent;
import android.test.mock.MockContext;

/**
 * Unit testing of BluetoothOppService
 */
public class BluetoothOppServiceTest extends TestCase {
    /**
     * Unit test members
     */
    private BluetoothOppService mOppService = null;


    /**
     * Extended MockContext (to catch intents)
     */
    public class OppServiceTestContext extends MockContext {
        /** @override */
        public void sendBroadcast(Intent intent, String receiverPermission) {
            if (intent.getAction() == BluetoothObexIntent.AUTHORIZE_ACTION) {
                assertEquals(intent.getStringExtra(BluetoothObexIntent.ADDRESS), "DE:AD:B3:3F:DE:AD");
                // TODO: do more stuff here.
            } else if (intent.getAction() == BluetoothObexIntent.RX_COMPLETE_ACTION) {
                // HACK: hard-coded for obex authorize cancel case
                assertEquals(intent.getStringExtra(BluetoothObexIntent.OBJECT_FILENAME), "/some/file");
            } else if (intent.getAction() == BluetoothObexIntent.PROGRESS_ACTION) {
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
    public class BluetoothOppServiceStubbed extends BluetoothOppService {
        public BluetoothOppServiceStubbed(Context context) {
            super(context);
        }

        /* Overrides of native methods for unit testing purposes.
         */
        protected boolean initNative() {
            return true;
        }

        protected void cleanupNative() {
            return;
        }

        boolean mSendFilesSucceed = true;
        protected boolean sendFilesNative(String address, String[] txFilenames) {
            if (mSendFilesSucceed) {
                assertEquals(txFilenames.length, 1);
                assertEquals(address,"DE:AD:B3:3F:DE:AD");
                assertEquals(txFilenames[0],"/tmp/asdf");
                return true;
            } else {
                return false;
            }
        }

        boolean mPullBussinesCardSucceed = true;
        protected boolean pullBusinessCardNative(String address, String rxFilename) {
            if (mPullBussinesCardSucceed) {
                assertEquals(address,"DE:AD:B3:3F:DE:AD");
                assertEquals(rxFilename,"/tmp/rx_vcard");
                return true;
            } else {
                return false;
            }
        }

        protected boolean cancelTransferNative(String transfer, boolean isServer) {
            return true;
        }

        protected boolean obexAuthorizeCompleteNative(boolean accept, String newFilename, int nativeData) {
            return true;
        }

        protected TransferProperties obexTransferGetPropertiesNative(String transfer) {
            // TODO: figure out filename handling?
            return new TransferProperties("/tmp/asdf", 150, "/tmp/asdf");
        }

        /* Test helper methods
         */
        public boolean verifyPushObjectDbItem() {
            BluetoothObexDatabase.TransferDbItem dbItem = mTransferDb.getByFilename("/tmp/asdf");

            if (mSendFilesSucceed) {
                assertNotNull(dbItem);
                assertFalse(dbItem.mIsServer);
                assertEquals(dbItem.mDirection, BluetoothObexDatabase.TransferDirection.TX);
                assertEquals(dbItem.getFilename(), "/tmp/asdf");
                assertNull(dbItem.getTransfer());
                //assertEquals(dbItem.getSession().getAddress(), "DE:AD:B3:3F:DE:AD");
            } else {
                assertNull(dbItem);
            }

            return true;
        }

        public boolean verifyPushObjectPostRequestDbItem() {
            BluetoothObexDatabase.TransferDbItem dbItem = mTransferDb.getByFilename("/tmp/asdf");
            assertNotNull(dbItem);
            assertFalse(dbItem.mIsServer);
            assertEquals(dbItem.mDirection, BluetoothObexDatabase.TransferDirection.TX);
            assertEquals(dbItem.getFilename(), "/tmp/asdf");
            assertEquals(dbItem.getTransfer(), "transfer name from request");
            assertEquals(dbItem.getSession().getAddress(), "DE:AD:B3:3F:DE:AD");

            return true;
        }

        public boolean verifyPushCancelInDb() {
            BluetoothObexDatabase.TransferDbItem dbItem = mTransferDb.getByFilename("/tmp/asdf");
            assertNull(dbItem);
            return true;
        }

        public boolean verifyPullCancelInDb() {
            BluetoothObexDatabase.TransferDbItem dbItem = mTransferDb.getByFilename("/tmp/rx_vcard");
            assertNull(dbItem);
            return true;
        }

        public boolean verifyPullBusinessCardDbItem() {
            BluetoothObexDatabase.TransferDbItem dbItem = mTransferDb.getByFilename("/tmp/rx_vcard");

            if (mPullBussinesCardSucceed) {
                assertNotNull(dbItem);
                assertFalse(dbItem.mIsServer);
                assertEquals(dbItem.mDirection, BluetoothObexDatabase.TransferDirection.RX);
                assertEquals(dbItem.getFilename(), "/tmp/rx_vcard");
                assertNull(dbItem.getTransfer());
                assertEquals(dbItem.getSession().getAddress(), "DE:AD:B3:3F:DE:AD");
            } else {
                assertNull(dbItem);
            }

            return true;
        }

        public boolean verifyPendingAuthorizeDbItem() {
            BluetoothObexDatabase.TransferDbItem dbItem = mTransferDb.getByFilename("/some/file");

            assertNotNull(dbItem);
            assertTrue(dbItem.mIsServer);
            assertEquals(dbItem.mDirection, BluetoothObexDatabase.TransferDirection.RX);
            assertEquals(dbItem.getFilename(), "/some/file");
            assertEquals(dbItem.getTransfer(), "transfer name");
            //assertEquals(dbItem.getSession.getAddress(), "DE:AD:B3:3F:DE:AD");

            return true;
        }

        public boolean verifyAuthorizeCompleteDbItem() {
            BluetoothObexDatabase.TransferDbItem dbItem = mTransferDb.getByFilename("/some/new/filename");

            assertNotNull(dbItem);
            assertTrue(dbItem.mIsServer);
            assertEquals(dbItem.mDirection, BluetoothObexDatabase.TransferDirection.RX);
            assertEquals(dbItem.getFilename(), "/some/new/filename");
            assertEquals(dbItem.getTransfer(), "transfer name");
            //assertEquals(dbItem.getSession().getAddress(), "DE:AD:B3:3F:DE:AD");

            return true;
        }

        public boolean verifyAuthorizeCancelDbItem() {
            BluetoothObexDatabase.TransferDbItem dbItem = mTransferDb.getByFilename("/some/file");
            assertNull(dbItem);
            return true;
        }
    }

    /**
     * OPP Service Test Fixture
     */
    @Override
    public void setUp() throws Exception {
        mOppService = new BluetoothOppServiceStubbed(new OppServiceTestContext());
    }

    @Override
    public void tearDown() throws Exception {
        mOppService = null;

        // Force system to finalize all unreferenced objects
        System.gc();
        System.runFinalization();
    }

    /**
     * Helper method for pushObject testing (sunny day)
     */
    public void pushObjectSuccess() {
        assertTrue(mOppService.pushObject("DE:AD:B3:3F:DE:AD", "/tmp/asdf"));
        assertTrue(((BluetoothOppServiceStubbed)mOppService).verifyPushObjectDbItem());
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#pushObject(java.lang.String, java.lang.String)}.
     *
     * Sunny day.
     */
    public void testPushObject() throws Exception {
        pushObjectSuccess();
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#isTransferActive(java.lang.String)}.
     *
     * Transfer active.
     */
    public void testIsTransferActiveWhenTransferActive() throws Exception {
        pushObjectSuccess();
        assertTrue(mOppService.isTransferActive("/tmp/asdf"));
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#isTransferActive(java.lang.String)}.
     *
     * Transfer active, then stopped.
     */
    public void testIsTransferActiveWhenTransferActiveThenStopped() throws Exception {
        testOnObexProgress();
        assertTrue(mOppService.isTransferActive("/tmp/asdf"));
        mOppService.onObexTransferComplete("transfer name from request", true, null);
        assertFalse(mOppService.isTransferActive("/tmp/asdf"));
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#pushObject(java.lang.String, java.lang.String)}.
     *
     * SendFilesNative failure (immediate @ JNI)
     */
    public void testPushObjectNativeFail() throws Exception {
        ((BluetoothOppServiceStubbed)mOppService).mSendFilesSucceed = false;
        assertFalse(mOppService.pushObject("DE:AD:B3:3F:DE:AD", "/tmp/asdf"));
        assertTrue(((BluetoothOppServiceStubbed)mOppService).verifyPushObjectDbItem());
    }

    /**
     * Helper method for pullBusinessCard testing (sunny day)
     */
    public void pullObjectSuccess() {
        // Ensure that we can kick-off business card pull
        assertTrue(mOppService.pullBusinessCard("DE:AD:B3:3F:DE:AD", "/tmp/rx_vcard"));
        assertTrue(((BluetoothOppServiceStubbed)mOppService).verifyPullBusinessCardDbItem());
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#pullBusinessCard(java.lang.String, java.lang.String)}.
     *
     * Sunny day.
     */
    public void testPullBusinessCard() throws Exception {
        pullObjectSuccess();
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#pullBusinessCard(java.lang.String, java.lang.String)}.
     *
     * PullBusinessCardNative failure (immediate @ JNI)
     */
    public void testPullBusinessCardNativeFail() throws Exception {
        ((BluetoothOppServiceStubbed)mOppService).mPullBussinesCardSucceed = false;
        assertFalse(mOppService.pullBusinessCard("DE:AD:B3:3F:DE:AD", "/tmp/rx_vcard"));
        assertTrue(((BluetoothOppServiceStubbed)mOppService).verifyPullBusinessCardDbItem());
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#cancelTransfer(java.lang.String)}.
     *
     * Test cancelling a push
     */
    public void testCancelTransferPush() throws Exception {
        pushObjectSuccess();

        assertTrue(mOppService.cancelTransfer("/tmp/asdf"));
        assertTrue(((BluetoothOppServiceStubbed)mOppService).verifyPushCancelInDb());
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#cancelTransfer(java.lang.String)}.
     *
     * Test cancelling a pull
     */
    public void testCancelTransferPull() throws Exception {
        pullObjectSuccess();

        assertTrue(mOppService.cancelTransfer("/tmp/rx_vcard"));
        assertTrue(((BluetoothOppServiceStubbed)mOppService).verifyPullCancelInDb());
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#obexAuthorizeComplete(java.lang.String, boolean, java.lang.String)}.
     */
    public void testObexAuthorizeComplete() throws Exception {
        testOnObexAuthorize();
        assertTrue(mOppService.obexAuthorizeComplete("/some/file", true, "/some/new/filename"));
        assertTrue(((BluetoothOppServiceStubbed)mOppService).verifyAuthorizeCompleteDbItem());
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#onObexAuthorize(java.lang.String, java.lang.String, java.lang.String, java.lang.String, int, int)}.
     */
    public void testOnObexAuthorize() throws Exception {
        assertTrue(mOppService.onObexAuthorize("transfer name", "DE:AD:B3:3F:DE:AD", "/some/file", null, 150, 1234));
        assertTrue(((BluetoothOppServiceStubbed)mOppService).verifyPendingAuthorizeDbItem());
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#onObexAuthorizeCancel(java.lang.String)}.
     */
    public void testOnObexAuthorizeCancel() throws Exception {
        testOnObexAuthorize();
        assertTrue(mOppService.onObexAuthorizeCancel("transfer name"));
        assertTrue(((BluetoothOppServiceStubbed)mOppService).verifyAuthorizeCancelDbItem());
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#onObexRequest(java.lang.String)}.
     */
    public void testOnObexRequest() throws Exception {
        pushObjectSuccess();
        assertEquals(mOppService.onObexRequest("transfer name from request"), "asdf");
        assertTrue(((BluetoothOppServiceStubbed)mOppService).verifyPushObjectPostRequestDbItem());
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#onObexProgress(java.lang.String, int)}.
     */
    public void testOnObexProgress() throws Exception {
        testOnObexRequest();
        mOppService.onObexProgress("transfer name from request", 50);
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#onSendFilesComplete(java.lang.String, boolean)}.
     *
     * Sunny day.
     */
    public void testOnSendFilesComplete() throws Exception {
        pushObjectSuccess();
        mOppService.onSendFilesComplete("DE:AD:B3:3F:DE:AD", false);
        assertTrue(((BluetoothOppServiceStubbed)mOppService).verifyPushObjectDbItem());
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#onSendFilesComplete(java.lang.String, boolean)}.
     *
     * Failure (e.g., connection failure)
     */
    public void testOnSendFilesCompleteConnectionFailure() throws Exception {
        pushObjectSuccess();
        mOppService.onSendFilesComplete("DE:AD:B3:3F:DE:AD", true);

        // Verify that the database is cleaned up
        assertTrue(((BluetoothOppServiceStubbed)mOppService).verifyPushCancelInDb());
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#onPullBusinessCardComplete(java.lang.String, boolean)}.
     */
    public void testOnPullBusinessCardComplete() throws Exception {
        pullObjectSuccess();
        mOppService.onPullBusinessCardComplete("DE:AD:B3:3F:DE:AD", false);
    }

    /**
     * Test method for {@link android.server.BluetoothOppService#onObexTransferComplete(java.lang.String, boolean, java.lang.String)}.
     */
    public void testOnObexTransferComplete() throws Exception {
        testOnObexProgress();
        mOppService.onObexTransferComplete("transfer name from request", true, null);

        // Verify that the database is cleaned up
        assertTrue(((BluetoothOppServiceStubbed)mOppService).verifyPushCancelInDb());
    }
}
