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
 * Unit testing of BluetoothObexDatabase (Java)
 */

package android.server;

import junit.framework.TestCase;

public class BluetoothObexDatabaseTest extends TestCase {
    /**
     * Unit test members
     */
    private BluetoothObexDatabase mDb = null;

    /**
     * BluetoothObexDatabase test fixture
     */
    @Override
    public void setUp() throws Exception {
        mDb = new BluetoothObexDatabase();
    }

    @Override
    public void tearDown() throws Exception {
        mDb = null;

        // Force system to finalize all unreferenced objects
        System.gc();
        System.runFinalization();
    }

    /**
     * Actual test code!
     */
    public void testInsertSessionDbItem() throws Exception {
        BluetoothObexDatabase.SessionDbItem sessionDbItem = mDb.new SessionDbItem("DE:AD:B3:3F:DE:AD","test session name",null);
        mDb.insert(sessionDbItem);

        assertTrue(mDb.getBySession("test session name").equals(sessionDbItem));
    }

    public void testGetByAddress() throws Exception {
        BluetoothObexDatabase.SessionDbItem sessionDbItem = mDb.new SessionDbItem("DE:AD:B3:3F:DE:AD","test session name",null);
        mDb.insert(sessionDbItem);

        assertTrue(mDb.getByAddress("DE:AD:B3:3F:DE:AD").equals(sessionDbItem));
    }

    public void testGetBySessionMultiples() throws Exception {
        BluetoothObexDatabase.SessionDbItem sessionDbItem = mDb.new SessionDbItem("DE:AD:B3:3F:DE:AD","test session name",null);
        BluetoothObexDatabase.SessionDbItem sessionDbItem2 = mDb.new SessionDbItem("DE:AD:B3:3F:DE:AD","test session name 2",null);

        mDb.insert(sessionDbItem);
        mDb.insert(sessionDbItem2);

        assertTrue(mDb.getBySession("test session name").equals(sessionDbItem));
        assertTrue(mDb.getBySession("test session name 2").equals(sessionDbItem2));
    }

    public void testDeleteByAddress() throws Exception {
        BluetoothObexDatabase.SessionDbItem sessionDbItem = mDb.new SessionDbItem("DE:AD:B3:3F:DE:AD","test session name",null);
        mDb.insert(sessionDbItem);
        mDb.deleteByAddress("DE:AD:B3:3F:DE:AD");

        assertNull(mDb.getByAddress("DE:AD:B3:3F:DE:AD"));
    }

    public void testUpdateSessionByAddress() throws Exception {
        BluetoothObexDatabase.SessionDbItem sessionDbItem = mDb.new SessionDbItem("DE:AD:B3:3F:DE:AD","test session name",null);
        mDb.insert(sessionDbItem);

        mDb.updateSessionByAddress("DE:AD:B3:3F:DE:AD", "test session name 2");
        assertTrue(mDb.getByAddress("DE:AD:B3:3F:DE:AD").getSession().equals("test session name 2"));
    }

    public void testInsertTransferDbItem() throws Exception {
        BluetoothObexDatabase.TransferDbItem transDbItem = mDb.new TransferDbItem("/tmp/asdf",null,"test transfer name",null);
        mDb.insert(transDbItem);

        assertTrue(mDb.getByTransfer("test transfer name").equals(transDbItem));
    }

    public void testGetByFilename() throws Exception {
        BluetoothObexDatabase.TransferDbItem transDbItem = mDb.new TransferDbItem("/tmp/asdf",null,"test transfer name",null);
        mDb.insert(transDbItem);

        assertTrue(mDb.getByFilename("/tmp/asdf").equals(transDbItem));
    }

    public void testGetByTransferMultiples() throws Exception {
        BluetoothObexDatabase.TransferDbItem transDbItem = mDb.new TransferDbItem("/tmp/asdf",null,"test transfer name",null);
        BluetoothObexDatabase.TransferDbItem transDbItem2 = mDb.new TransferDbItem("/tmp/asdf2",null,"test transfer name 2",null);

        mDb.insert(transDbItem);
        mDb.insert(transDbItem2);

        assertTrue(mDb.getByTransfer("test transfer name").equals(transDbItem));
        assertTrue(mDb.getByTransfer("test transfer name 2").equals(transDbItem2));
    }

    public void testGetTransferByAddress() throws Exception {
        BluetoothObexDatabase.SessionDbItem sessionDbItem = mDb.new SessionDbItem("DE:AD:B3:3F:DE:AD","test session name",null);
        mDb.insert(sessionDbItem);

        BluetoothObexDatabase.TransferDbItem transDbItem = mDb.new TransferDbItem("/tmp/asdf",null,"test transfer name",sessionDbItem);
        mDb.insert(transDbItem);

        assertTrue(mDb.getTransferByAddress("DE:AD:B3:3F:DE:AD").equals(transDbItem));
    }

    public void testDeleteByFilename() throws Exception {
        BluetoothObexDatabase.TransferDbItem transDbItem = mDb.new TransferDbItem("/tmp/asdf",null,"test transfer name",null);
        mDb.insert(transDbItem);

        mDb.deleteByFilename("/tmp/asdf");

        assertNull(mDb.getByFilename("/tmp/asdf"));
    }

    public void testUpdateFilenameByFilename() throws Exception {
        BluetoothObexDatabase.TransferDbItem transDbItem = mDb.new TransferDbItem("/tmp/asdf",null,"test transfer name",null);
        mDb.insert(transDbItem);

        mDb.updateFilenameByFilename("/tmp/asdf", "/tmp/asdf2");

        assertEquals(mDb.getByFilename("/tmp/asdf2").getFilename(),"/tmp/asdf2");
        assertEquals(mDb.getByFilename("/tmp/asdf2").getTransfer(),"test transfer name");
    }

    public void testUpdateTransferByFilename() throws Exception {
        BluetoothObexDatabase.TransferDbItem transDbItem = mDb.new TransferDbItem("/tmp/asdf",null,"test transfer name",null);
        mDb.insert(transDbItem);

        mDb.updateTransferByFilename("/tmp/asdf", "updated transfer name");

        assertEquals(mDb.getByTransfer("updated transfer name").getFilename(),"/tmp/asdf");
        assertEquals(mDb.getByTransfer("updated transfer name").getTransfer(),"updated transfer name");
    }
}
