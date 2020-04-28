/**************************************************************************************************
  Filename:       BleDeviceInfo.java
  Revised:        $Date: 2013-08-30 12:08:11 +0200 (fr, 30 aug 2013) $
  Revision:       $Revision: 27477 $

  Copyright (c) 2013 - 2014 Texas Instruments Incorporated

  All rights reserved not granted herein.
  Limited License. 

  Texas Instruments Incorporated grants a world-wide, royalty-free,
  non-exclusive license under copyrights and patents it now or hereafter
  owns or controls to make, have made, use, import, offer to sell and sell ("Utilize")
  this software subject to the terms herein.  With respect to the foregoing patent
  license, such license is granted  solely to the extent that any such patent is necessary
  to Utilize the software alone.  The patent license shall not apply to any combinations which
  include this software, other than combinations with devices manufactured by or for TI (襎I Devices�). 
  No hardware patent is licensed hereunder.

  Redistributions must preserve existing copyright notices and reproduce this license (including the
  above copyright notice and the disclaimer and (if applicable) source code license limitations below)
  in the documentation and/or other materials provided with the distribution

  Redistribution and use in binary form, without modification, are permitted provided that the following
  conditions are met:

    * No reverse engineering, decompilation, or disassembly of this software is permitted with respect to any
      software provided in binary form.
    * any redistribution and use are licensed by TI for use only with TI Devices.
    * Nothing shall obligate TI to provide you with source code for the software licensed and provided to you in object code.

  If software source code is provided to you, modification and redistribution of the source code are permitted
  provided that the following conditions are met:

    * any redistribution and use of the source code, including any resulting derivative works, are licensed by
      TI for use only with TI Devices.
    * any redistribution and use of any object code compiled from the source code and any resulting derivative
      works, are licensed by TI for use only with TI Devices.

  Neither the name of Texas Instruments Incorporated nor the names of its suppliers may be used to endorse or
  promote products derived from this software without specific prior written permission.

  DISCLAIMER.

  THIS SOFTWARE IS PROVIDED BY TI AND TI誗 LICENSORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
  BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL TI AND TI誗 LICENSORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.


 **************************************************************************************************/
package com.ble.ocr;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class BleDeviceInfo {
  // Data
  private BluetoothDevice mBtDevice;
  private String mName = "";
  private int mRssi;
  private int mPower;
  private boolean mBeaconFlg;
  private String mUuid;
  private int mMajor;
  private int mMinor;

  private byte[] mScanData;
  
  private static final byte IBEACON_OFFSET1 = 7;
  private static final byte IBEACON_FEATURE1 = 0x02;
  private static final byte IBEACON_FEATURE2 = 0x15;
  private static final byte UUID_OFFSET = 9;
  private static final byte MAJOR_OFFSET = 25;
  private static final byte MINOR_OFFSET = 27;
  private static final byte POWER_OFFSET = 29;
  private static final byte GAP_ADTYPE_LOCAL_NAME_COMPLETE = 0x09;
  private static final byte GAP_ADTYPE_LOCAL_NAME_FLAG = 0x11;

  private static final String TAG = "BleDeviceInfo";

  public BleDeviceInfo(BluetoothDevice device, int rssi, final byte[] scanData) {
    mBtDevice = device;
    mRssi = rssi;
    mScanData = new byte[100];
    System.arraycopy(scanData, 0, mScanData, 0, scanData.length);
    parseParams(scanData, scanData.length);
  }

  public BluetoothDevice getBluetoothDevice() {
	  return mBtDevice;
  }

  public String getName() {
	  return mName;
  }
  
  public int getRssi() {
	  return mRssi;
  }
  
  public String getUuid() {
	  return mUuid;
  }
  
  public int getMajor() {
	  //BE
	  return mMajor;
  }
  
  public int getMinor() {
	  //BE
	  return mMinor; 
  }
  
  public int getPower() {
	  return mPower;
  }
  
  public boolean isIBeacon() {
	  return mBeaconFlg;
  }

  private void parseBeaconFlg() {
	  if((mScanData[IBEACON_OFFSET1] & 0xff) !=  IBEACON_FEATURE1) {
		  mBeaconFlg = false;
		  return;
	  }

	  if((mScanData[IBEACON_OFFSET1 + 1] & 0xff) !=  IBEACON_FEATURE2) {
		  mBeaconFlg = false;
		  return;
	  }

	  if((mScanData[POWER_OFFSET + 1] & 0xff) == 0) {
		  mBeaconFlg = false;
		  return;
	  }
	  
	  mBeaconFlg = true;
	  return;
  }
  
  private void parseMajor() {
	  //BE
	  mMajor = (mScanData[MAJOR_OFFSET + 1] & 0xff) + (mScanData[MAJOR_OFFSET] & 0xff) * 256;
  }
  
  private void parseMinor() {
	  //BE
	  mMinor = (mScanData[MINOR_OFFSET + 1] & 0xff)  + (mScanData[MINOR_OFFSET] & 0xff) * 256;
  }
  
  private void parseName(byte[] scanData, int len) {
	  mName = "";
	  if(len < (32+16))
	  {
		  Log.i(TAG, "length < 46 ,length="+len );
		  return;
	  }
	  if((scanData[30] != GAP_ADTYPE_LOCAL_NAME_FLAG) || scanData[31] != GAP_ADTYPE_LOCAL_NAME_COMPLETE)
	  {
		  Log.i(TAG, "cannot find 11 or 09");
		  return;
	  }
	  mName = new String(scanData, 32, 16);
	  Log.i(TAG, "mName = " + mName);
  }
  
  private void parseParams(byte[] scanData, int len)
  {
	  parseName(scanData, len);
	  parseUuid();
	  parseMajor();
	  parseMinor();
	  parsePower();
	  parseBeaconFlg();
  }
  
  private void parsePower() {
	  mPower = mScanData[POWER_OFFSET];
  }
  
  private void parseUuid()
  {
	  mUuid = "";
	  mUuid += HexUtil.byte2hex(mScanData, UUID_OFFSET, 4);
	  mUuid += "-";
	  mUuid += HexUtil.byte2hex(mScanData, UUID_OFFSET + 4, 2);
	  mUuid += "-";
	  mUuid += HexUtil.byte2hex(mScanData, UUID_OFFSET + 6, 2);
	  mUuid += "-";
	  mUuid += HexUtil.byte2hex(mScanData, UUID_OFFSET + 8, 2);
	  mUuid += "-";
	  mUuid += HexUtil.byte2hex(mScanData, UUID_OFFSET + 10, 6);
  }
  
  public void updateParameters(int rssi, final byte[] scanData) {
	  mRssi = rssi;
	  System.arraycopy(scanData, 0, mScanData, 0, scanData.length);
	  parseParams(scanData, scanData.length);
  }

}
