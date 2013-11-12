/*
** Copyright (c) 2012, The Linux Foundation. All rights reserved.

** Redistribution and use in source and binary forms, with or without
** modification, are permitted provided that the following conditions are
** met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

** THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
** WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
** MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
** ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
** BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
** CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
** SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
** BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
** WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
** OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
** IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.qualcomm.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import android.util.Log;

/* Utility class that reads the SOC ID of the device and
 * returns to the calling classes. Value once read is stored
 * within a static variable, thus avoiding to read from the
 * file system everytime
*/
public class MpqUtils {

   private static final String TAG = "MpqUtils";

   // file path to read the SOC ID
   private static final String SOC_ID_PATH = "/sys/devices/system/soc/soc0/id";

   // store the already read SOC ID
   private static int mSocIdValue;

   public static boolean isTargetMpq() {
      boolean retVal = false;
      BufferedReader br = null;
      String socId = null;

      // if already read soc id, check if it is mpq
      if (mSocIdValue != 0) {

         // Check if the SOC ID corresponds to MPQ8064 chip
         if (mSocIdValue == 130)
            return true;

         return false;
      }


      // read the soc id
      try {
         br = new BufferedReader(new FileReader(SOC_ID_PATH));
         StringBuilder sb = new StringBuilder();
         socId = br.readLine();

         while (socId != null) {
            mSocIdValue = Integer.parseInt(socId);
            socId = br.readLine();
         }

         // Print the content on the conoole
         Log.d(TAG, "SOC Integer : " + mSocIdValue);

         if (mSocIdValue == 130) {
            Log.d(TAG, "It is an MPQ8064 chipset !!");
            retVal = true;
         }

      } catch (FileNotFoundException e) {
         Log.e(TAG, e.toString());
      } catch (Exception e) {
         Log.e(TAG, e.toString());
      }

      finally {
         try {
            br.close();
         } catch (Exception e) {
            Log.e(TAG, e.toString());
         }
      }

      return retVal;
   }

}

