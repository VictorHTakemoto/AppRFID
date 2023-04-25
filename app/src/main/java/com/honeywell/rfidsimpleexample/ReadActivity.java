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
    private ListView mLv;
    private ArrayAdapter mAdapter;
    //private Button mBtnWriteTag;
    private int mSelectedIdx = -1;
    private TextView mTagToWrite;
    private TextView mData;
    //private TextView mToastCustom;
    //private ProgressDialog mWaitDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRfidMgr = MyApplication.getInstance().rfidMgr;
        mReader = MyApplication.getInstance().mRfidReader;
        setContentView(R.layout.activity_read);
        mBtnRead = findViewById(R.id.btn_read);
        //mBtnWriteTag = findViewById(R.id.write_tag);
        mLv = findViewById(R.id.lv);
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mTagDataList);
        mLv.setAdapter(mAdapter);
        mLv.setOnItemClickListener(mItemClickListenerTag);
        mTagToWrite = findViewById(R.id.select_tag);
        mData = findViewById(R.id.data_field);
        mData.requestFocus();
        //mToastCustom = findViewById(R.id.toast_text);
        //mRfidMgr.setBeeper(true, 100, 100);
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
            int duration = Toast.LENGTH_LONG;
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
                        Toast toast = Toast.makeText(getApplicationContext(), "Leia um codigo de barras", duration);
                        toast.show();
                    }
                    else
                    {
                        int size = 24;
                        int bank = 1;
                        int startAddress = 2;
                        String verifyData = checkFieldData.trim().replaceAll("\n", "");
                        String dataToWrite = padLeft(verifyData, size);
                        CustomToast customToast = new CustomToast(getApplicationContext(), dataToWrite);
                        customToast.show();
                        //writeTagData(checkFieldEpc, bank, startAddress, dataToWrite);
                        //Toast toast = Toast.makeText(getApplicationContext(), dataToWrite, duration);
                        //toast.show();
                    }
                }
            }
        }

        @Override
        public void onTriggerModeSwitched(TriggerMode triggerMode) {
        }
    };

    /*public void checkList() {
        int tamanho = mTagDataList.size();
        CustomToast customToast = new CustomToast(getApplicationContext(), );
    }*/

    private boolean isReaderAvailable() {
        return mReader != null && mReader.available();
    }

    private void read() {
        if (isReaderAvailable()) {
            mTagDataList.clear();
            mReader.setOnTagReadListener(dataListener);
            mReader.read(TagAdditionData.get("None"), new TagReadOption());
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

    //Botão WriteTag Desativado
    /*public void checkLengthToWrite(View view) {
        int duration = Toast.LENGTH_LONG;
        try {
            String tagToCheck = mData.getText().toString();
            if (tagToCheck.isEmpty() || tagToCheck == null) {
                mWaitDialog = ProgressDialog.show(this, null, "Leia um codigo de barras");
            } else {
                int size = 25;
                char padChar = '0';
                String tagCheck = padLeft(tagToCheck, size, padChar);
                //Desativado para validar
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                byte[] getBytes = tagCheck.getBytes(StandardCharsets.UTF_8);
                String hexa = new BigInteger(1, getBytes).toString(16);
                mWaitDialog = ProgressDialog.show(this, null, hexa);
            }
            }
        } catch (Exception exception) {
            Toast toast = Toast.makeText(getApplicationContext(), "Exceção gerada: " + exception.getMessage(), duration);
            toast.show();
        }
    }*/

    private void writeTagData(String epc, int bank, int startAddr, String data) {
        int duration = Toast.LENGTH_LONG;
        try {
            mReader.writeTagData(epc, bank, startAddr, null, data);
            mTagToWrite.setText("");
            mData.setText("");
            Toast toast = Toast.makeText(getApplicationContext(), "Tag gravada com sucesso", duration);
            toast.show();
        } catch (RfidReaderException e) {
            Toast toast = Toast.makeText(getApplicationContext(), "Não foi posivel gravar a tag,: " + e.getMessage(), duration);
            toast.show();
        }
    }

    private void cleanData(){

    }
}
