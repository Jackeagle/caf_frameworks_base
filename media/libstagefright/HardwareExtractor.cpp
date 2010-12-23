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

#define LOG_TAG "HardwareExtractor"
#include <utils/Log.h>

#include <media/stagefright/HardwareExtractor.h>
#include <media/stagefright/MediaDefs.h>

namespace android {
#define UPPER_BOUNDARY 31
#define DIMENSION_CHANGE_COND(width, height) (((width) == 20) && ((height) == 12))
#define CONSTANT 3
#define CONSTANT1 3

  Mutex HardwareExtractor::mLock;
  uint8_t HardwareExtractor::mNumInstances;
  int HardwareExtractor::mHWVideoLoad;
  HardwareExtractor::HardwareExtractor()
  {
    init();
  }

  void HardwareExtractor::init()
  {
    mProfile = 0;
    mLevel = 0;
    mWidthInMBs = 0;
    mHeightInMBs = 0;
    mNumRefFrames = 0;
    mInterlaced = 0;
    mInBuffers = 0;
    mOutBuffers = 0;
    mInstanceWeight = 0;
    mCodecSpecificDataPtr = NULL;
    mNalUnitSize = 0;
    mMime = NULL;
  }

  HardwareExtractor::~HardwareExtractor()
  {
    Mutex::Autolock autoLock(mLock);
    mNumInstances -= mInstanceWeight;
    mHWVideoLoad -= mInstanceLoad;
    if(mCodecSpecificDataPtr != NULL) {
      free(mCodecSpecificDataPtr);
      mCodecSpecificDataPtr = NULL;
    }
    if(mMime != NULL) {
      free(mMime);
      mMime = NULL;
    }
  }

  uint8_t HardwareExtractor::getMaxDpbSize()
  {
    int32_t widthInMBs = mWidthInMBs;
    int32_t heightInMBs = mHeightInMBs;
    int32_t numMBs = heightInMBs * widthInMBs;
    int32_t frameSize = ((numMBs << 8) * 3) >> 1;
    int isLargerThanWVGA = numMBs > 1620;

    if(mLevel < 10 || mLevel > UPPER_BOUNDARY) {
      mLevel = UPPER_BOUNDARY;
    }
    int32_t dpbSize = h264dMaxDpbSizeLut[mLevel - 10];
    if (dpbSize == 0) {
      dpbSize = h264dMaxDpbSizeLut[20];
    }
    if(isLargerThanWVGA) {
      dpbSize = h264dMaxDpbSizeLut[21];
      mLevel = 31;
    }

    uint8_t maxDpbSize = dpbSize / frameSize;
    if(maxDpbSize > 16) {
      maxDpbSize = 16;
    }
    return maxDpbSize;
  }
#ifdef HW_8x50
  uint8_t HardwareExtractor::h264CalculateNumOutBuf8x50()
  {
    int32_t widthInMBs = mWidthInMBs;
    int32_t heightInMBs = mHeightInMBs;
    int32_t numMBs = heightInMBs * widthInMBs;
    int isLargerThanWVGA = numMBs > 1620;
    int isLargerThanVGA = numMBs >= 1200;

    uint8_t maxDpbSize = getMaxDpbSize();
    if(DIMENSION_CHANGE_COND(widthInMBs, heightInMBs)) {
      numMBs = 300;
    }

    if(maxDpbSize > 16 || numMBs == 300) {
      maxDpbSize = 16;
    }

    if(maxDpbSize < 5)
      maxDpbSize = 5;

    uint8_t numOutBuffers = maxDpbSize;
    if(isLargerThanWVGA) {
      numOutBuffers += CONSTANT1;
    } else if(isLargerThanVGA || mLevel >= 30) {
      numOutBuffers += CONSTANT1 - 1;
    } else {
      numOutBuffers += 1;
    }

    if(numMBs == 3600)
      return numOutBuffers;
    else
      return numOutBuffers + 2;
  }

  int HardwareExtractor::findAndSaveBufferRequirements8x50()
  {
    int32_t numMBs = mHeightInMBs * mWidthInMBs;
    int isLargerThanWVGA = numMBs > 1620;
    int isLargerThanVGA = numMBs >= 1200;
    uint8_t numInBuffers = 1;
    if(isLargerThanWVGA) {
      numInBuffers = CONSTANT;
    } else if(isLargerThanVGA || mLevel >= 30) {
      numInBuffers = CONSTANT;
    }
    numInBuffers += 1 + 1;
    uint8_t numOutBuffers = h264CalculateNumOutBuf8x50();
    mInBuffers = numInBuffers;
    mOutBuffers = numOutBuffers;

    return 0;
  }

  bool HardwareExtractor::canHWSupportThisInst8x50()
  {
    if(!strcasecmp(MEDIA_MIMETYPE_VIDEO_VP6,mMime)) {
      mInstanceWeight = MAX_INSTANCES_8x50;
    } else {
      mInstanceWeight = 1;
    }
    mNumInstances += mInstanceWeight;
    if(mNumInstances > MAX_INSTANCES_8x50) {
      LOGE("Exceeded max number of instances Max: %d, instances requested: %d", MAX_INSTANCES_8x50, mNumInstances);
      return false;
    }
    mInstanceLoad = mHeightInMBs * mWidthInMBs;
    mHWVideoLoad += mInstanceLoad;
    if(mHWVideoLoad > MAX_VIDEO_LOAD_8x50) {
      LOGE("Exceeded q6 load Max: %d load requested = %d", MAX_VIDEO_LOAD_8x50, mHWVideoLoad);
      return false;
    }

    return true;
  }
#endif
  int HardwareExtractor::findAndSaveMemRequirements()
  {
    if(!strcasecmp(MEDIA_MIMETYPE_VIDEO_AVC, mMime)) {
      parseSps(mNalUnitSize, mCodecSpecificDataPtr);
#ifdef HW_8x50
      findAndSaveBufferRequirements8x50();
#endif
#ifdef HW_7x30
      findAndSaveBufferRequirements7x30();
#endif
    }
    printConfigData();
    return 0;
  }

  bool HardwareExtractor::canHWSupportThisInst() {
    Mutex::Autolock autoLock(mLock);
    findAndSaveMemRequirements();
#ifdef HW_8x50
    return canHWSupportThisInst8x50();
#endif
    return true;
  }

#if HW_7x30
  int HardwareExtractor::findAndSaveBufferRequirements7x30() {
    int32_t widthInMBs = mWidthInMBs;
    int32_t heightInMBs = mHeightInMBs;
    int32_t numMBs = heightInMBs * widthInMBs;
    int isLargerThanEqualToWVGA = numMBs >= 1620;

    uint8_t maxDpbSize = getMaxDpbSize();
    maxDpbSize += 2;
    LOGE("Max Dpb Size = %d", maxDpbSize);
    if(isLargerThanEqualToWVGA) {
      mOutBuffers = maxDpbSize + 2;
      if(mOutBuffers < 10)
        mOutBuffers = 10;
    } else {
      mOutBuffers = maxDpbSize + 5;
    }
    mInBuffers = 4;
    return 0;
  }
#endif

  void HardwareExtractor::updateCodecSpecificData(const uint8_t *codecSpecificPtr, uint8_t naluSize, char *mime)
  {
    if(mCodecSpecificDataPtr != NULL) {
      free(mCodecSpecificDataPtr);
      mCodecSpecificDataPtr = NULL;
    }
    if(codecSpecificPtr && naluSize) {
      mCodecSpecificDataPtr = (uint8_t *)malloc(naluSize);
      memcpy(mCodecSpecificDataPtr,codecSpecificPtr, naluSize);
      mNalUnitSize = naluSize;
    }
    if(mime) {
      mMime = strdup(mime);
    }
  }

  uint8_t HardwareExtractor::getNumberOfInputBuffersReqd()
  {
    return mInBuffers;
  }

  uint8_t HardwareExtractor::getNumberOfOutputBuffersReqd()
  {
    return mOutBuffers;
  }

  void HardwareExtractor::printConfigData() {
    LOGE("mProfile = %d mLevel = %d mHeightInMBs = %d mWidthInMBs = %d mNumRefFrames = %d",
        mProfile, mLevel, mHeightInMBs, mWidthInMBs, mNumRefFrames);
    LOGE("mInterlace = %d mInputBuffers = %d mOutputBuffers = %d", mInterlaced, mInBuffers, mOutBuffers);
  }
  void HardwareExtractor::parseSps (uint8_t naluSize, uint8_t *encodedBytes) {
    if(!encodedBytes || naluSize <= 0)
      return;
    uint8_t naluType = (encodedBytes[0] & 0x1F);
    uint8_t profile_id = 0;
    uint8_t level_id = 0;
    uint8_t tmp = 0;
    uint32_t id = 0;
    uint8_t log2MaxFrameNumMinus4 = 0;
    uint8_t picOrderCntType = 0;
    uint8_t log2MaxPicOrderCntLsbMinus4 = 0;
    uint8_t deltaPicOrderAlwaysZeroFlag = 0;
    uint32_t numRefFramesInPicOrderCntCycle = 0;
    uint8_t picWidthInMbsMinus1 = 0;
    uint8_t picHeightInMapUnitsMinus1 = 0;
    uint8_t frameMbsOnlyFlag = 0;
    uint8_t cropLeft= 0, cropRight = 0, cropTop = 0, cropBot = 0;
    if(7 == naluType) {
      RbspParser rbsp(&encodedBytes[1],
          &encodedBytes[naluSize]);
      profile_id = rbsp.u(8);
      tmp = rbsp.u(8);
      level_id = rbsp.u(8);
      id = rbsp.ue();
      if(100 == profile_id) {
        tmp = rbsp.ue();
        if(3 == tmp) {
          (void)rbsp.u(1);
        }
        //bit_depth_luma_minus8
        (void)rbsp.ue();
        //bit_depth_chroma_minus8
        (void)rbsp.ue();
        //qpprime_y_zero_transform_bypass_flag
        (void)rbsp.u(1);
        // seq_scaling_matrix_present_flag
        tmp = rbsp.u(1);
        if (tmp) {
          unsigned int tmp1, t;
          //seq_scaling_list_present_flag
          for (t = 0; t < 6; t++) {
            tmp1 = rbsp.u(1);
            if (tmp1) {
              unsigned int last_scale = 8, next_scale = 8, delta_scale;
              for (int j = 0; j < 16; j++)
              {
                if (next_scale) {
                  delta_scale = rbsp.se();
                  next_scale = (last_scale + delta_scale + 256) % 256;
                }
                last_scale = next_scale?next_scale:last_scale;
              }
            }
          }
          for (t = 0; t < 2; t++) {
            tmp1 = rbsp.u(1);
            if (tmp1) {
              unsigned int last_scale = 8, next_scale = 8, delta_scale;
              for (int j = 0; j < 64; j++)
              {
                if (next_scale) {
                  delta_scale = rbsp.se();
                  next_scale = (last_scale + delta_scale + 256) % 256;
                }
                last_scale = next_scale?next_scale : last_scale;
              }
            }
          }
        }
      }

      log2MaxFrameNumMinus4 = rbsp.ue();
      picOrderCntType = rbsp.ue();
      if(0 == picOrderCntType) {
        log2MaxPicOrderCntLsbMinus4 = rbsp.ue();
      } else if(1 == picOrderCntType) {
        deltaPicOrderAlwaysZeroFlag = (rbsp.u(1) == 1);
        (void)rbsp.se();
        (void)rbsp.se();
        numRefFramesInPicOrderCntCycle = rbsp.ue();
        for (uint32_t i = 0; i < numRefFramesInPicOrderCntCycle; ++i) {
          (void)rbsp.se();
        }
      }
      mNumRefFrames = rbsp.ue();
      tmp = rbsp.u(1);
      picWidthInMbsMinus1 = rbsp.ue();
      picHeightInMapUnitsMinus1 = rbsp.ue();
      frameMbsOnlyFlag = (rbsp.u(1) == 1);
      if(!frameMbsOnlyFlag)
        (void)rbsp.u(1);
      (void)rbsp.u(1);
      tmp = rbsp.u(1);
      cropLeft = 0;
      cropRight = 0;
      cropTop = 0;
      cropBot = 0;
      if(tmp) {
        cropLeft = rbsp.ue();
        cropRight = rbsp.ue();
        cropTop = rbsp.ue();
        cropBot = rbsp.ue();
        LOGE("crop (%d,%d,%d,%d)", cropLeft, cropRight, cropTop, cropBot);
      }
      mHeightInMBs = (2 - frameMbsOnlyFlag ) * (picHeightInMapUnitsMinus1 + 1);
      mWidthInMBs = picWidthInMbsMinus1 + 1;
      mProfile = profile_id;
      mLevel = level_id;
      mInterlaced = !frameMbsOnlyFlag;
    }
  }

  RbspParser::RbspParser(const uint8_t * _begin, const uint8_t * _end)
    :begin(_begin), end(_end), pos(-1), bit(0),
    cursor(0xFFFFFF), advanceNeeded(true)
  {
  }

  // Destructor
  /*lint -e{1540}  Pointer member neither freed nor zeroed by destructor
   * No problem
   */
  RbspParser::~RbspParser()
  {
  }

  // Return next RBSP byte as a word
  uint32_t RbspParser::next()
  {
    if (advanceNeeded)
      advance();
    //return static_cast<uint32> (*pos);
    return static_cast < uint32_t > (begin[pos]);
  }

  // Advance RBSP decoder to next byte
  void RbspParser::advance()
  {
    ++pos;
    //if (pos >= stop)
    if (begin + pos == end) {
      /*lint -e{730}  Boolean argument to function
       * I don't see a problem here
       */
      //throw false;
      LOGE("H264Parser-->NEED TO THROW THE EXCEPTION...\n");
    }
    cursor <<= 8;
    //cursor |= static_cast<uint32> (*pos);
    cursor |= static_cast < uint32_t > (begin[pos]);
    if ((cursor & 0xFFFFFF) == 0x000003) {
      advance();
    }
    advanceNeeded = false;
  }

  // Decode unsigned integer
  uint32_t RbspParser::u(uint32_t n)
  {
    uint32_t i, s, x = 0;
    for (i = 0; i < n; i += s) {
      s = static_cast < uint32_t > STD_MIN(static_cast < int >(8 - bit),
          static_cast < int >(n - i));
      x <<= s;

      x |= ((next() >> ((8 - static_cast < uint32_t > (bit)) - s)) &
          ((1 << s) - 1));

      bit = (bit + s) % 8;
      if (!bit) {
        advanceNeeded = true;
      }
    }
    return x;
  }

  // Decode unsigned integer Exp-Golomb-coded syntax element
  uint32_t RbspParser::ue()
  {
    int leadingZeroBits = -1;
    for (uint32_t b = 0; !b; ++leadingZeroBits) {
      b = u(1);
    }
    return ((1 << leadingZeroBits) - 1) +
      u(static_cast < uint32_t > (leadingZeroBits));
  }

  // Decode signed integer Exp-Golomb-coded syntax element
  int32_t RbspParser::se()
  {
    const uint32_t x = ue();
    if (!x)
      return 0;
    else if (x & 1)
      return static_cast < int32_t > ((x >> 1) + 1);
    else
      return -static_cast < int32_t > (x >> 1);
  }
}
