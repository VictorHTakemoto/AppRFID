package com.honeywell.rfidsimpleexample;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.hardware.TriggerEventListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
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

    private Button mBtnClear;

    private ArrayAdapter mAdapter;
    private int mSelectedIdx = -1;
    private TextView mTagToWrite;
    private TextView mTitulo;

    private TextView mCampoChassis;
    private String chassisHexa;
    private String chassisCodBarras;
    private TextView mChassiToValid;
    private String mChassiPrint;
    private String mChassiConf;
    private int etapa = 0;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        mRfidMgr = MyApplication.getInstance().rfidMgr;
        mReader = MyApplication.getInstance().mRfidReader;

        mBtnClear = findViewById(R.id.clear_fields);
        mChassiPrint = "http://192.168.25.28:5068/Rfid/ImprimeEtiqueta/";//TODO: ADICIONAR DADOS EM BANCO OU VIA JSON
        mChassiConf = "http://192.168.25.28:5068/Rfid/ConferirEtiqueta/";

        mRfidMgr.setTriggerMode(TriggerMode.BARCODE_SCAN);
        mTitulo = findViewById(R.id.label_titulo);


        mCampoChassis = findViewById(R.id.campoChassis);
        mCampoChassis.setText("");
        mTagToWrite = findViewById(R.id.select_tag);
        mTagToWrite.setHint("Leia o codigo de barras");
        mChassiToValid = findViewById(R.id.data_field);
        mChassiToValid.setHint("Leia a TAG");
        mChassiToValid.setVisibility(View.INVISIBLE);
        mTagToWrite.requestFocus();
        mTitulo.setText("Leia o Código de barras");


        mBtnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cleanField();
            }
        });

        mTagToWrite.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    String txt = charSequence.toString();
                    if(!txt.isEmpty()){
                        new callAPITask().execute(mChassiPrint, txt);
                    }
                }
                catch (Exception ex){
                    CustomToast customToast = new CustomToast(getApplicationContext(), "Erro " + ex.getMessage());
                    customToast.show();
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }
    @Override
    protected void onResume()
    {
        super.onResume();
        mRfidMgr.addEventListener(mEventListener);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mRfidMgr.removeEventListener(mEventListener);
        mIsReadBtnClicked = false;
        stopRead();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        int itemId = item.getItemId();
        if (itemId == R.id.settings_btn)
        {
            Intent intent = new Intent(this, ConfigActivity.class);
            startActivity(intent);
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
            String checkFieldEpc = mTagToWrite.getText().toString();// CAMPO LEITURA CODIGO DE BARRAS
            String checkFieldData = mChassiToValid.getText().toString();//CAMPO LEITURA RFID

           if(etapa ==1 && !checkFieldData.isEmpty())
            {
                conferirEtiqueta(checkFieldData,chassisHexa);
            }

            if (mIsReadBtnClicked || !trigger)
            {
                mIsReadBtnClicked = false;
                stopRead();

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                    }
                });
            }
            else
            {
                if(!checkFieldEpc.isEmpty()){
                 read();
               }
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
                        etapa=1;
                        return "Requisição bem-sucedida: impressao\n" + response;

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
            if (result.contains("Requisição bem-sucedida:")) {
                mRfidMgr.setTriggerMode(TriggerMode.RFID);
                chassisHexa = result.substring(102,132)+"00";
                chassisCodBarras = result.substring(68,85);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTagToWrite.setVisibility(View.INVISIBLE);
                        mChassiToValid.setVisibility(View.VISIBLE);
                        mCampoChassis.setText(chassisCodBarras);
                        mTitulo.setText("Confira a gravação da TAG");
                    }
                });
                CustomToast customToast = new CustomToast(getApplicationContext(), "PROCESSO BEM SUCEDIDO ! CHASSIS: " + chassisCodBarras );
                customToast.show();

            }else{

                customAlertDialog("ERRO",result);
            }
        }
    }

    private void read()
    {
        if (isReaderAvailable())
        {
            mTagDataList.clear();
            mReader.setOnTagReadListener(dataListener);
            mReader.read(TagAdditionData.get("None"), new TagReadOption());
            mRfidMgr.setBeeper(false, 1, 2);
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

            mChassiToValid.setText(t[1].getEpcHexStr());
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
            setGravity(Gravity.BOTTOM,0,0);
        }
    }
    public void customAlertDialog(String title, String message) {
        // Inflar o layout personalizado do AlertDialog
        View customView = getLayoutInflater().inflate(R.layout.alert_sucesso, null);

        // Encontrar os elementos de layout no customView
        TextView tituloTextView = customView.findViewById(R.id.title);
        TextView mensagemTextView = customView.findViewById(R.id.message);

        // Definir o título e a mensagem
        tituloTextView.setText(title);
        mensagemTextView.setText(message);

        // Criar e exibir o AlertDialog
        AlertDialog alertDialog = new AlertDialog.Builder(ReadActivity.this)
                .setView(customView)
                .create();
        alertDialog.show();
    }

    public void checkList()
    {
        if (!mTagDataList.isEmpty())
        {
            if (mTagDataList.size() == 1)
            {
                String getFirstValue = String.valueOf(mTagDataList.get(0));
                mChassiToValid.setText(getFirstValue);
            }
        }
    }

    public void conferirEtiqueta (String chassisLido,String ChassisHexa){

        if(chassisLido.equals(ChassisHexa)){

            mRfidMgr.setTriggerMode(TriggerMode.BARCODE_SCAN);
            CustomToast customToast = new CustomToast(getApplicationContext(), "ETIQUETA CONFERIDA COM SUCESSO!");
            customToast.show();
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Chama a função cleanField() após 2 segundos (2000 milissegundos)
                    cleanField();
                }
            }, 4000); // Atraso de 2000 milissegundos (2 segundos)

        }else{
            mChassiToValid.setText("");
            mTagDataList.clear();
            customAlertDialog("Erro", "Tag não corresponde. Tente novamente ou refaça o processo: Tag lida: " + chassisHexa );
        }

    }
    public void cleanField()
    {
        try
        {
            String clearTag = mTagToWrite.getText().toString();
            String clearData = mChassiToValid.getText().toString();
            if (!clearTag.isEmpty() || !clearData.isEmpty() || !mTagDataList.isEmpty())
            {
                mTitulo.setText("Leia o Código de barras");
                mTagToWrite.setVisibility(View.VISIBLE);
                mChassiToValid.setVisibility(View.INVISIBLE);
                mCampoChassis.setText("");
                mTagToWrite.setText("");
                mTagToWrite.invalidate();
                mTagToWrite.requestFocus();
                mTagToWrite.requestLayout();
                mChassiToValid.setText("");
                mChassiToValid.invalidate();
                mTagDataList.clear();
                mRfidMgr.setTriggerMode(TriggerMode.BARCODE_SCAN);
                etapa = 0;
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
