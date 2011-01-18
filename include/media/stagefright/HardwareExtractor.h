/*--------------------------------------------------------------------------
Copyright (c) 2010, Code Aurora Forum. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of Code Aurora nor
      the names of its contributors may be used to endorse or promote
      products derived from this software without specific prior written
      permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NON-INFRINGEMENT ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
--------------------------------------------------------------------------*/
#include <stdint.h>

#include <utils/RefBase.h>
#include <utils/threads.h>
namespace android {
  const uint32_t h264dMaxDpbSizeLut[42] =    {
    152064          , //    10      148.5 * 1024
    345600          , //    11      337.5
    912384          , //    12      891
    912384          , //    13      891
    0                     , //      14      0
    0                     , //      15      0
    0                     , //      16      0
    0                     , //      17      0
    0                     , //      18      0
    0                     , //      19      0
    912384          , //    20      891
    1824768         , //    21      1782
    3110400         , //    22      3037.5
    0                     , //      23      0
    0                     , //      24      0
    0                     , //      25      0
    0                     , //      26      0
    0                     , //      27      0
    0                     , //      28      0
    0                     , //      29      0
    3110400         , //    30      3037.5
    6912000         , //    31      6750
    7864320         , //    32      7680
    0                     , //      33      0
    0                     , //      34      0
    0                     , //      35      0
    0                     , //      36      0
    0                     , //      37      0
    0                     , //      38      0
    0                     , //      39      0
    12582912                , //    40      12288
    12582912                , //    41      12288
    13369344                , //    42      13056
    0                     , //      43      0
    0                     , //      44      0
    0                     , //      45      0
    0                     , //      46      0
    0                     , //      47      0
    0                     , //      48      0
    0                     , //      49      0
    42393600                , //    50      41400
    70778880                  //    51      69120
  };

#define STD_MIN(x,y) (((x) < (y)) ? (x) : (y))
#define MAX_VIDEO_LOAD_8x50 45*80
#define MAX_INSTANCES_8x50 4
  class RbspParser
  {
    public:
      RbspParser(const uint8_t * begin, const uint8_t * end);

      virtual ~ RbspParser();

      uint32_t next();
      void advance();
      uint32_t u(uint32_t n);
      uint32_t ue();
      int32_t se();

    private:
      const uint8_t *begin, *end;
      int32_t pos;
      uint32_t bit;
      uint32_t cursor;
      bool advanceNeeded;
  };


  class HardwareExtractor : public RefBase {
    private:
      uint8_t mProfile;
      uint8_t mLevel;
      int32_t mHeightInMBs;
      int32_t mWidthInMBs;
      int32_t mNumRefFrames;
      int32_t mInterlaced;
      uint8_t mInBuffers;
      uint8_t mOutBuffers;
      int32_t mInstanceLoad;
      uint8_t mNalUnitSize;
      uint8_t *mCodecSpecificDataPtr;
      uint8_t mInstanceWeight;
      char*   mMime;

      static Mutex mLock;
      static uint8_t mNumInstances;
      static int mHWVideoLoad;

      void init();
      int checkProfileLevelAndDimensions();
#ifdef HW_8x50
      uint8_t h264CalculateNumOutBuf8x50();
      int findAndSaveBufferRequirements8x50();
      bool canHWSupportThisInst8x50();
#endif
#ifdef HW_7x30
      int findAndSaveBufferRequirements7x30();
#endif
      bool isReadyToGetBufferRequirements();
      void parseSps(uint8_t naluSize, uint8_t *encodedBytes);
      void printConfigData();
      int findAndSaveMemRequirements();
      uint8_t getMaxDpbSize();
    public:
      HardwareExtractor();
      ~HardwareExtractor();
      void updateCodecSpecificData(const uint8_t *codecSpecificPtr, uint8_t naluSize, char *mime);
      bool canHWSupportThisInst();
      uint8_t getNumberOfInputBuffersReqd();
      uint8_t getNumberOfOutputBuffersReqd();
  };
}
