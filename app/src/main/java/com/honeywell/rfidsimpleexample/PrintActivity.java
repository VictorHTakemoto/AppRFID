package com.honeywell.rfidsimpleexample;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.honeywell.rfidservice.EventListener;
import com.honeywell.rfidservice.RfidManager;
import com.honeywell.rfidservice.TriggerMode;
import com.honeywell.rfidservice.rfid.RfidReader;

public class PrintActivity extends AppCompatActivity
{
    private RfidManager mRfidMgr;
    private RfidReader mReader;

    private boolean mIsReadBtnClicked;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);
        mRfidMgr = MyApplication.getInstance().rfidMgr;
        mReader = MyApplication.getInstance().mRfidReader;

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        //mRfidMgr.addEventListener(m);
    }

    private EventListener mEventListenet = new EventListener() {
        @Override
        public void onDeviceConnected(Object o) {

        }

        @Override
        public void onDeviceDisconnected(Object o) {

        }

        @Override
        public void onReaderCreated(boolean b, RfidReader rfidReader) {

        }

        @Override
        public void onRfidTriggered(boolean b) {

        }

        @Override
        public void onTriggerModeSwitched(TriggerMode triggerMode) {

        }
    };
}
