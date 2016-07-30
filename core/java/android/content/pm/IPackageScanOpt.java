/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package android.content.pm;

import android.content.pm.PackageParser;
import android.content.pm.PackageParser.PackageParserException;
import android.os.Parcel;

import java.io.File;

/**
 * The interface used to call package manager service optimization method in external jar.
 * @hide
 */
public interface IPackageScanOpt {
    /**
     * Measuring the elapsed time of each stage.
     * @param num       num of stage
     * @param lastMs    last time in millisecond
     * @return          current time in millisecond
     */
    long recordPeroidT(int num, long lastMs);
    /** Dump time usage info */
    void dumpPeroidT();

    /**
     * Parse the package at the given location.
     * If exist info file on disk, directly rebuild pkg instance;
     * Otherwise, use pp.parsePackage method to get pkg.
     */
    PackageParser.Package optParsePackage(PackageParser pp,
            File scanFile, int parseFlags, int scanFlags)
        throws PackageParserException, PackageScanOptException;

    /** Write native info to pkg.writeParcel */
    void writeNativeInfo(PackageParser.Package pkg, File scanFile);

    /** Read native info from pkg.readParcel */
    boolean readNativeInfo(PackageParser.Package pkg, PkgAbiInfo pkgAbiInfo);

    /** Clean pkg info files at PMS end */
    void cleanUselessPkgInfo();

    /** Assist class for transfer ABI info */
    public class PkgAbiInfo {
        public String primaryCpuAbi;
        public String secondaryCpuAbi;
    }

    /**
     * Exception thrown when package scan failed.
     */
    public class PackageScanOptException extends Exception {
        public PackageScanOptException() {
            super();
        }

        public PackageScanOptException(String msg) {
            super(msg);
        }

        public PackageScanOptException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
