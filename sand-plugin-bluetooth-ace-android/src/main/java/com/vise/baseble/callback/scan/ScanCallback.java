package com.vise.baseble.callback.scan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.vise.baseble.ViseBle;
import com.vise.baseble.common.BleConfig;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;

/**
 * @Description: 扫描设备回调
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/8/1 22:58.
 */
public class ScanCallback implements BluetoothAdapter.LeScanCallback, IScanFilter {
    protected Handler handler = new Handler(Looper.getMainLooper());
    protected boolean isScan = true;//是否开始扫描
    protected boolean isScanning = false;//是否正在扫描
    protected BluetoothLeDeviceStore bluetoothLeDeviceStore;//用来存储扫描到的设备
    protected IScanCallback scanCallback;//扫描结果回调

    public ScanCallback(IScanCallback scanCallback) {
        this.scanCallback = scanCallback;
        if (scanCallback == null) {
            throw new NullPointerException("this scanCallback is null!");
        }
        bluetoothLeDeviceStore = new BluetoothLeDeviceStore();
    }

    public ScanCallback setScan(boolean scan) {
        isScan = scan;
        return this;
    }

    public boolean isScanning() {
        return isScanning;
    }

    public void scan() {
        if (isScan) {
            if (isScanning) {
                return;
            }
            bluetoothLeDeviceStore.clear();

            Runnable stopScanRunnable = new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    if (ViseBle.getInstance().getBluetoothAdapter() != null) {
                        try {
                            ViseBle.getInstance().getBluetoothAdapter().stopLeScan(ScanCallback.this);
                        } catch (Exception e) {
                            Log.e("BluetoothScan", "Error stopping LE scan", e);
                        }
                    }

                    if (bluetoothLeDeviceStore.getDeviceMap() != null
                            && bluetoothLeDeviceStore.getDeviceMap().size() > 0) {
                        scanCallback.onScanFinish(bluetoothLeDeviceStore);
                    } else {
                        scanCallback.onScanTimeout();
                    }
                }
            };

            if (BleConfig.getInstance().getScanTimeout() > 0) {
                handler.postDelayed(stopScanRunnable, BleConfig.getInstance().getScanTimeout());
            } else if (BleConfig.getInstance().getScanRepeatInterval() > 0) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopScanRunnable.run();
                        isScanning = true;
                        if (ViseBle.getInstance().getBluetoothAdapter() != null) {
                            try {
                                ViseBle.getInstance().getBluetoothAdapter().startLeScan(ScanCallback.this);
                            } catch (Exception e) {
                                Log.e("BluetoothScan", "Error starting LE scan", e);
                            }
                        }
                        handler.postDelayed(this, BleConfig.getInstance().getScanRepeatInterval());
                    }
                }, BleConfig.getInstance().getScanRepeatInterval());
            }

            isScanning = true;
            if (ViseBle.getInstance().getBluetoothAdapter() != null) {
                try {
                    ViseBle.getInstance().getBluetoothAdapter().startLeScan(ScanCallback.this);
                } catch (Exception e) {
                    Log.e("BluetoothScan", "Error starting LE scan", e);
                }
            }
        } else {
            isScanning = false;
            if (ViseBle.getInstance().getBluetoothAdapter() != null) {
                try {
                    ViseBle.getInstance().getBluetoothAdapter().stopLeScan(ScanCallback.this);
                } catch (Exception e) {
                    Log.e("BluetoothScan", "Error stopping LE scan", e);
                }
            }
        }
    }


    public ScanCallback removeHandlerMsg() {
        handler.removeCallbacksAndMessages(null);
        bluetoothLeDeviceStore.clear();
        return this;
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
        BluetoothLeDevice bluetoothLeDevice = new BluetoothLeDevice(bluetoothDevice, rssi, scanRecord, System.currentTimeMillis());
        BluetoothLeDevice filterDevice = onFilter(bluetoothLeDevice);
        if (filterDevice != null) {
            bluetoothLeDeviceStore.addDevice(filterDevice);
            scanCallback.onDeviceFound(filterDevice);
        }
    }

    @Override
    public BluetoothLeDevice onFilter(BluetoothLeDevice bluetoothLeDevice) {
        if(bluetoothLeDevice.getName()!=null&&!bluetoothLeDevice.getName().isEmpty()){
            return bluetoothLeDevice;
        }
        return null;
    }

}
