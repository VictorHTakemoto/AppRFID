package com.honeywell.rfidsimpleexample;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.RippleDrawable;
import android.hardware.TriggerEventListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ReadActivity extends AppCompatActivity
{
    private RfidManager mRfidMgr;
    private RfidReader mReader;
    private List mTagDataList = new ArrayList();

    private boolean mIsReadBtnClicked;
    private Button mBtnRead;
    private Button mBtnClear;
    private Button mBtnPrint;
    private ListView mLv;
    private ArrayAdapter mAdapter;
    private int mSelectedIdx = -1;
    private TextView mTagToWrite;
    //private TextView mData;
    private TextView mChassiToValid;
    private String mChassiPrint;
    private String mChassiConf;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        mRfidMgr = MyApplication.getInstance().rfidMgr;
        mReader = MyApplication.getInstance().mRfidReader;
        mRfidMgr.setTriggerMode(TriggerMode.BARCODE_SCAN);

        mBtnRead = findViewById(R.id.btn_read);
        mBtnClear = findViewById(R.id.clear_fields);
        mBtnPrint = findViewById(R.id.print_btn);
        mChassiPrint = "http://192.168.18.7:5068/Rfid/ImprimeEtiqueta/";
        mChassiConf = "http://192.168.18.7:5068/Rfid/ConferirEtiqueta/";

        mLv = findViewById(R.id.lv);
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mTagDataList);
        mLv.setAdapter(mAdapter);
        mLv.setOnItemClickListener(mItemClickListenerTag);
        mTagToWrite = findViewById(R.id.select_tag);
        mChassiToValid = findViewById(R.id.data_field);
        mTagToWrite.requestFocus();
        mChassiToValid.setVisibility(View.INVISIBLE);


        mBtnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String chetTxt = mTagToWrite.getText().toString();
                if (!chetTxt.isEmpty()){
                    String chassi = "93YRBB005RJ773602";
                    new callAPITask().execute(mChassiPrint, chassi);
                }
                else {
                    CustomToast customToast = new CustomToast(getApplicationContext(), "Layer vazia");
                    customToast.show();
                }
            }
        });

        mTagToWrite.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String chassis = charSequence.toString();
                String texto1 = mTagToWrite.getText().toString();
                if(!texto1.isEmpty()){
                    if((mTagToWrite.length() <= 17)){
                        new callAPITask().execute(mChassiPrint, chassis);
                        mTagToWrite.setText("");
                        mRfidMgr.setTriggerMode(TriggerMode.RFID);
                    }
                    else {
                        new callAPITask().execute(mChassiConf, chassis);
                        mTagToWrite.setText("");
                        mRfidMgr.setTriggerMode(TriggerMode.BARCODE_SCAN);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

//        mChassiToValid.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//                String txt = editable.toString();
//                new callAPITask().execute(mChassiConf, txt);
//            }
//        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mRfidMgr.addEventListener(mEventListener);
        showBtn();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mRfidMgr.removeEventListener(mEventListener);
        mIsReadBtnClicked = false;
        stopRead();
    }

    private void showBtn()
    {
        if (mIsReadBtnClicked)
        {
            mBtnRead.setText("Stop");
            mBtnRead.setTextColor(Color.rgb(255, 128, 0));
        } else
        {
            mBtnRead.setText("Read");
            mBtnRead.setTextColor(Color.rgb(0, 0, 0));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //Navegação de telas, desativado para validação
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        int itemId = item.getItemId();
        if (itemId == R.id.settings_btn)
        {
            Intent intent = new Intent(this, ConfigActivity.class);
            startActivity(intent);
            //Toast.makeText(this, "Botão clicado", Toast.LENGTH_LONG).show();
            return true;
        }
        if(itemId == R.id.print_btn)
        {
            Intent intent = new Intent(this, PrintActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void clickBtnRead(View view)
    {
        if (mIsReadBtnClicked)
        {
            mIsReadBtnClicked = false;
            stopRead();
        } else
        {
            mIsReadBtnClicked = true;
            read();
        }

        showBtn();
    }


    private EventListener mEventListener = new EventListener()
    {
        @Override
        public void onDeviceConnected(Object o)
        {
        }

        @Override
        public void onDeviceDisconnected(Object o)
        {
        }

        @Override
        public void onReaderCreated(boolean b, RfidReader rfidReader)
        {
        }

        @Override
        public void onRfidTriggered(boolean trigger)
        {
            String checkFieldEpc = mTagToWrite.getText().toString();
            String checkFieldData = mChassiToValid.getText().toString();
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
            } else
            {
                read();
//                if (checkFieldEpc.isEmpty() || checkFieldEpc == null)
//                {
//                    mRfidMgr.setTriggerMode(TriggerMode.BARCODE_SCAN);
//                }
//                else {
//                  read();
//                }
            }
        }

        @Override
        public void onTriggerModeSwitched(TriggerMode triggerMode)
        {
        }
    };

    private boolean isReaderAvailable()
    {
        return mReader != null && mReader.available();
    }

    public void imp(){
        try {
            if (mTagToWrite != null){
                String chassi = "93YRBB005RJ773602";
                AsyncTask<String, Void, String> t = new callAPITask().execute("https://192.168.18.7:7015/Rfid/ImprimeEtiqueta/", chassi);
                CustomToast customToast = new CustomToast(getApplicationContext(), "Retorno API: " + t.toString());
                customToast.show();
            }
            else {
                CustomToast customToast = new CustomToast(getApplicationContext(), "Layer vazia");
                customToast.show();
            }
        } catch (Exception e) {
            CustomToast customToast = new CustomToast(getApplicationContext(), "Layer vazia" + e.getMessage());
            customToast.show();
        }
    }

    private class callAPITask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params){
            String apiUrl = params[0];
            String chassis = params[1];
            try {
                URL url = new URL(apiUrl + chassis);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                int responseCode = connection.getResponseCode();

                if(responseCode == HttpURLConnection.HTTP_OK){
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null){
                        response.append(line);
                    }
                    in.close();

                    return "Requisição bem-sucedida:\n" + response;
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();

                    return "Erro na resposta da API. Código: " + responseCode + "\n" + errorResponse.toString();
                }
            }
            catch (IOException e){
                e.printStackTrace();
                return "Erro ao enviar solicitação para a API.\n" + e.getMessage();
            }
        }
        @Override
        protected void onPostExecute(String result){
            CustomToast customToast = new CustomToast(getApplicationContext(), "Retorno API: " + result);
            customToast.show();
        }
    }

    /*private void onReadBarCode() {
        if(isReaderAvailable()) {
            TriggerMode triggerMode = TriggerMode.BARCODE_SCAN;
            System.out.println("Teste: " + triggerMode);
        }
    }*/

    private void read()
    {
        if (isReaderAvailable())
        {
            mTagDataList.clear();
            mReader.setOnTagReadListener(dataListener);
            mReader.read(TagAdditionData.get("None"), new TagReadOption());
            mRfidMgr.setBeeper(true, 1, 2);
        }
    }

    private void stopRead()
    {
        if (isReaderAvailable())
        {
            checkList();
            mReader.stopRead();
            mReader.removeOnTagReadListener(dataListener);
            mRfidMgr.setBeeper(false, 1, 2);
        }
    }

    private OnTagReadListener dataListener = new OnTagReadListener()
    {
        @Override
        public void onTagRead(final TagReadData[] t)
        {
            synchronized (mTagDataList)
            {
                for (TagReadData trd : t)
                {
                    String epc = trd.getEpcHexStr();

                    if (!mTagDataList.contains(epc))
                    {
                        mTagDataList.add(epc);
                    }
                }

                mHandler.sendEmptyMessage(0);
            }
        }
    };

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);

            switch (msg.what)
            {
                case 0:
                    mAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    private AdapterView.OnItemClickListener mItemClickListenerTag = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
        {
            mSelectedIdx = position;
            mAdapter.notifyDataSetChanged();
            String selectedItem = (String) adapterView.getItemAtPosition(position);
            mChassiToValid.setText(selectedItem);
        }
    };

    public String padLeft(String str, int size)
    {
        if (str.length() <= size)
        {
            int zeroToAdd = size - str.length();
            for (int i = 0; i < zeroToAdd; i++)
            {
                str = "0" + str;
            }
        }
        return str;
    }

    private void writeTagData(String epc, int bank, int startAddr, String data)
    {
        int duration = Toast.LENGTH_LONG;
        try
        {
            mReader.writeTagData(epc, bank, startAddr, null, data);
            mTagToWrite.setText("");
            mChassiToValid.setText("");
            mTagDataList.clear();
            stopRead();
            CustomToast customToast = new CustomToast(getApplicationContext(), "Tag gravada");
            customToast.show();
        } catch (RfidReaderException e)
        {
            CustomToast customToast = new CustomToast(getApplicationContext(), "Não foi possível gravar a tag: " + e.getMessage());
            customToast.show();
        }
    }

    public static class CustomToast extends Toast
    {
        public CustomToast(Context context, String message)
        {
            super(context);

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.toast_custom, null);

            TextView textView = view.findViewById(R.id.toast_text);
            textView.setText(message);

            setView(view);
            setDuration(Toast.LENGTH_LONG);
        }
    }

    public void checkList()
    {
        if (!mTagDataList.isEmpty())
        {
            if (mTagDataList.size() == 1)
            {
                String getFirstValue = String.valueOf(mTagDataList.get(0));
                mTagToWrite.setText(getFirstValue);
            }
        }
    }

    public void cleanField(View view)
    {
        String clearTag = mTagToWrite.getText().toString();
        String clearData = mChassiToValid.getText().toString();
        try
        {
            if (!clearData.isEmpty() || !clearTag.isEmpty() || !mTagDataList.isEmpty())
            {
                mTagToWrite.setText("");
                mTagToWrite.invalidate();
                mTagToWrite.requestLayout();
                mChassiToValid.setText("");
                mTagDataList.clear();
            }
            else
            {
                mTagDataList.clear();
                CustomToast customToast = new CustomToast(getApplicationContext(), "Campos limpos");
                customToast.show();
            }
        } catch (Exception ex)
        {
            CustomToast customToast = new CustomToast(getApplicationContext(), "Exceção: " + ex);
            customToast.show();
        }
    }
}
