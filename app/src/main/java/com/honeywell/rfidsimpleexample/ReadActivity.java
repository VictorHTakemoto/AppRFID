package com.honeywell.rfidsimpleexample;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.honeywell.rfidservice.EventListener;
import com.honeywell.rfidservice.RfidManager;
import com.honeywell.rfidservice.TriggerMode;
import com.honeywell.rfidservice.rfid.OnTagReadListener;
import com.honeywell.rfidservice.rfid.RfidReader;
import com.honeywell.rfidservice.rfid.RfidReaderException;
import com.honeywell.rfidservice.rfid.TagAdditionData;
import com.honeywell.rfidservice.rfid.TagReadData;
import com.honeywell.rfidservice.rfid.TagReadOption;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ReadActivity extends AppCompatActivity {
    private RfidManager mRfidMgr;
    private RfidReader mReader;
    private List mTagDataList = new ArrayList();

    private boolean mIsReadBtnClicked;
    private Button mBtnRead;
    private Button mBtnClear;
    private ListView mLv;
    private ArrayAdapter mAdapter;
    private int mSelectedIdx = -1;
    private TextView mTagToWrite;
    private TextView mData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRfidMgr = MyApplication.getInstance().rfidMgr;
        mReader = MyApplication.getInstance().mRfidReader;
        setContentView(R.layout.activity_read);
        mBtnRead = findViewById(R.id.btn_read);
        mBtnClear = findViewById(R.id.clear_fields);
        mLv = findViewById(R.id.lv);
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mTagDataList);
        mLv.setAdapter(mAdapter);
        mLv.setOnItemClickListener(mItemClickListenerTag);
        mTagToWrite = findViewById(R.id.select_tag);
        mData = findViewById(R.id.data_field);
        mData.requestFocus();
    }



    @Override
    protected void onResume() {
        super.onResume();
        mRfidMgr.addEventListener(mEventListener);
        showBtn();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRfidMgr.removeEventListener(mEventListener);
        mIsReadBtnClicked = false;
        stopRead();
    }

    private void showBtn() {
        if (mIsReadBtnClicked) {
            mBtnRead.setText("Stop");
            mBtnRead.setTextColor(Color.rgb(255, 128, 0));
        } else {
            mBtnRead.setText("Read");
            mBtnRead.setTextColor(Color.rgb(0, 0, 0));
        }
    }

    //Navegação de telas, desativado para validação
    /*public void WriteTag(View view) {
        Intent intent = new Intent(this, WriteTag.class);
        startActivity(intent);
    }*/

    public void clickBtnRead(View view) {
        if (mIsReadBtnClicked) {
            mIsReadBtnClicked = false;
            stopRead();
        } else {
            mIsReadBtnClicked = true;
            read();
        }

        showBtn();
    }


    private EventListener mEventListener = new EventListener() {
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
        public void onRfidTriggered(boolean trigger)
        {
            String checkFieldEpc = mTagToWrite.getText().toString();
            String checkFieldData = mData.getText().toString();
            if (mIsReadBtnClicked || !trigger)
            {
                mIsReadBtnClicked = false;
                stopRead();

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        showBtn();
                    }
                });
            }
            else
            {
                if (checkFieldEpc.isEmpty() || checkFieldEpc == null)
                {
                    read();
                }
                else
                {
                    if(checkFieldData.isEmpty() || checkFieldData == null)
                    {
                        CustomToast customToast = new CustomToast(getApplicationContext(), "Leia um codigo de barras");
                        customToast.show();
                    }
                    else
                    {
                        int size = 24;
                        int bank = 1;
                        int startAddress = 2;
                        String verifyData = checkFieldData.trim().replaceAll("\n", "");
                        String dataToWrite = padLeft(verifyData, size);
                        writeTagData(checkFieldEpc, bank, startAddress, dataToWrite);
                    }
                }
            }
        }

        @Override
        public void onTriggerModeSwitched(TriggerMode triggerMode) {
        }
    };

    private boolean isReaderAvailable() {
        return mReader != null && mReader.available();
    }

    private void read() {
        if (isReaderAvailable()) {
            mTagDataList.clear();
            mReader.setOnTagReadListener(dataListener);
            mReader.read(TagAdditionData.get("None"), new TagReadOption());
            checkList();
            mRfidMgr.setBeeper(true, 1, 2);
        }
    }

    private void stopRead() {
        if (isReaderAvailable()) {
            mReader.stopRead();
            mReader.removeOnTagReadListener(dataListener);
            mRfidMgr.setBeeper(false, 1, 2);
        }
    }

    private OnTagReadListener dataListener = new OnTagReadListener() {
        @Override
        public void onTagRead(final TagReadData[] t) {
            synchronized (mTagDataList) {
                for (TagReadData trd : t) {
                    String epc = trd.getEpcHexStr();

                    if (!mTagDataList.contains(epc)) {
                        mTagDataList.add(epc);
                    }
                }

                mHandler.sendEmptyMessage(0);
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case 0:
                    mAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    private AdapterView.OnItemClickListener mItemClickListenerTag = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            mSelectedIdx = position;
            mAdapter.notifyDataSetChanged();
            String selectedItem = (String) adapterView.getItemAtPosition(position);
            mTagToWrite.setText(selectedItem);
        }
    };

    public String padLeft(String str, int size) {
        if (str.length() <= size) {
            int zeroToAdd = size - str.length();
            for(int i = 0; i < zeroToAdd; i++) {
                str = "0" + str;
            }
        }
        return str;
    }

    private void writeTagData(String epc, int bank, int startAddr, String data) {
        int duration = Toast.LENGTH_LONG;
        try {
            mReader.writeTagData(epc, bank, startAddr, null, data);
            mTagToWrite.setText("");
            mData.setText("");
            CustomToast customToast = new CustomToast(getApplicationContext(), "Tag gravada");
            customToast.show();
        } catch (RfidReaderException e) {
            CustomToast customToast = new CustomToast(getApplicationContext(), "Não foi possível gravar a tag: " + e.getMessage());
            customToast.show();
        }
    }

    public class CustomToast extends Toast {
        public CustomToast(Context context, String message) {
            super(context);

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.toast_custom, null);

            TextView textView = view.findViewById(R.id.toast_text);
            textView.setText(message);

            setView(view);
            setDuration(Toast.LENGTH_LONG);
        }
    }

    public void checkList() {
        if(!mTagDataList.isEmpty()) {
            String getFirstValue = String.valueOf(mTagDataList.get(0));
            mTagToWrite.setText(getFirstValue);
        }
    }

    public void cleanField(View view){
        String clearTag = mTagToWrite.getText().toString();
        String clearData = mData.getText().toString();
        try {
            if(!clearTag.isEmpty() || !clearData.isEmpty()){
                mTagToWrite.setText("");
                mData.setText("");
                mTagDataList.clear();
            }
            if (!clearTag.isEmpty()) {
                mTagToWrite.setText("");
                mTagDataList.clear();
            }
            if (!clearData.isEmpty()) {
                mData.setText("");
                mTagDataList.clear();
            } else {
                CustomToast customToast = new CustomToast(getApplicationContext(), "Campos limpos");
                customToast.show();
            }
        } catch (Exception ex) {
            CustomToast customToast = new CustomToast(getApplicationContext(), "Exceção: " + ex);
            customToast.show();
        }
    }
}
