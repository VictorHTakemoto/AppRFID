package com.honeywell.rfidsimpleexample;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.honeywell.rfidservice.EventListener;
import com.honeywell.rfidservice.RfidManager;
import com.honeywell.rfidservice.TriggerMode;
import com.honeywell.rfidservice.rfid.OnTagReadListener;
import com.honeywell.rfidservice.rfid.RfidReader;
import com.honeywell.rfidservice.rfid.TagAdditionData;
import com.honeywell.rfidservice.rfid.TagReadData;
import com.honeywell.rfidservice.rfid.TagReadOption;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ReadActivity extends AppCompatActivity
{
    private RfidManager mRfidMgr;
    private RfidReader mReader;
    private List mTagDataList = new ArrayList();
    private boolean mIsReadBtnClicked;
    private Button mBtnClear;
    private TextView mTagToWrite;
    private TextView mTitulo;
    private TextView mCampoChassis;
    private String chassisHexa;
    private String chassisCodBarras;
    private TextView mChassiToValid;
    private String mChassiPrint;
    private String mChassiConf;
    private int etapa = 0;
    private  boolean busy = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        //Gerenciadores RFID APK Honeywell
        mRfidMgr = MyApplication.getInstance().rfidMgr;
        mReader = MyApplication.getInstance().mRfidReader;

        //Views
        mBtnClear = findViewById(R.id.clear_fields);
        mTitulo = findViewById(R.id.label_titulo);
        mCampoChassis = findViewById(R.id.campoChassis);
        mTagToWrite = findViewById(R.id.select_tag);
        mChassiToValid = findViewById(R.id.data_field);

        //Link API
        mChassiPrint = "http://192.168.25.28:5068/Rfid/ImprimeEtiqueta/";//TODO: ADICIONAR DADOS EM BANCO OU VIA JSON

        //Inicialização De variáveis:
        mRfidMgr.setTriggerMode(TriggerMode.BARCODE_SCAN);
        mCampoChassis.setText("");
        mTagToWrite.setHint("Leia o codigo de barras");
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
                    //Ao ter alteração no texto, chamará a api para impressão da Etiqueta
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
            //Evento de apertar o gatilho do RFID
            String checkFieldEpc = mTagToWrite.getText().toString();// CAMPO LEITURA CODIGO DE BARRAS
            String checkFieldData = mChassiToValid.getText().toString();//CAMPO LEITURA RFID

            //Validando se a etapa é =1, ou seja ja passou pela chamada da API para impressão, se o campo nao esta vazio e se esta como ocupado.
            //A variavel Busy serve para que enquanto o sistema tiver validando uma tag RFID não consiga ler outra.
           if(etapa ==1 && !checkFieldData.isEmpty()&&!busy)
            {
                //chama o metodo que faz a validação se a Tag foi gravada corretamente
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
                //Serve para so ler RFID se o campo de codigo de barras estiver vazio.
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


    //Método para chamar a API, asyncrono
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
                        etapa=1; //Muda a etapa para validar que ja passou pela parte de impressão da etiqueta.
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
                //Se a solicitação de certo, muda o gatilho para RFID, para validar a gravação da TAG
                mRfidMgr.setTriggerMode(TriggerMode.RFID);
                chassisHexa = result.substring(102,132)+"00"; //Pega a informação retornada da API do chassis Gravado, ja convertida para HexaDecimal
                chassisCodBarras = result.substring(68,85); // Pega a informação de qual codigo de barras foi utilzado
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Muda o layout da Tela, tornado o campo de codigo de barras invisivel e o de RFID visivel
                        mTagToWrite.setVisibility(View.INVISIBLE);
                        mChassiToValid.setVisibility(View.VISIBLE);
                        mCampoChassis.setText(chassisCodBarras);
                        //Muda o texto titulo da tela
                        mTitulo.setText("Confira a gravação da TAG");
                    }
                });

                CustomToast customToast = new CustomToast(getApplicationContext(), "PROCESSO BEM SUCEDIDO ! CHASSIS: " + chassisCodBarras,true );
                customToast.setDuration(Toast.LENGTH_SHORT);
                customToast.show();

            }else{
                cleanField();
                customAlertDialog("ERRO",result);
            }
        }
    }

    private void read()
    {
        if (isReaderAvailable())
        {
            //Metodo para ler a tag
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
            if(!busy) {
                mChassiToValid.setText(t[1].getEpcHexStr());
            }
        }
    };

    public static class CustomToast extends Toast //Metodo para criar Toast personalisada. atento ao parametro booleano Yellows, que se marcado como 'true' deixa o backgorud do toast amarelo
    {
        public CustomToast(Context context, String message) {
            this(context, message, false);
        }
        public CustomToast(Context context, String message,boolean fundoAmarelo)
        {
            super(context);

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.toast_custom, null);

            TextView textView = view.findViewById(R.id.toast_text);
            textView.setText(message);
            if(fundoAmarelo){
                textView.setBackgroundColor(Color.parseColor("#FFFF00"));
            }else{
                textView.setBackgroundColor(Color.parseColor("#008000"));
            }
            setView(view);
            setDuration(Toast.LENGTH_LONG);
            setGravity(Gravity.BOTTOM,0,0);
        }
    }

    //Cria a caixa de dialogo vermelha, caso algo der errado.
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

    public void conferirEtiqueta (String chassisLido,String ChassisHexa){ //Metodo para conferir etiqueta gravada
        try {
            busy = true; //Deixa como busy para que so seja chamado novamente esse metodo apos o processo ser concluido
            if (chassisLido.equals(ChassisHexa)) {
                CustomToast customToast = new CustomToast(getApplicationContext(), "ETIQUETA CONFERIDA COM SUCESSO!");
                customToast.show();
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Chama a função cleanField() após 2 segundos (2000 milissegundos)
                        cleanField();
                    }
                }, 3500); // Atraso de 2000 milissegundos

            } else {

                customAlertDialog("Erro", "Tag não corresponde. Tente novamente ou refaça o processo: Tag lida: " + chassisHexa);
                mChassiToValid.setText("");
                mTagDataList.clear();
                busy = false; //Torna a variavel como false novamente para o metodo poder ser requisitado
                chassisLido = "";
            }
        }catch (Exception e){
            busy = false;
            customAlertDialog("Excessão", "Houve uma excessão durante a conferência - "+ e.getMessage());
        }

    }
    public void cleanField() //metodo para resetar o formulario, e zerar os campos
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
                chassisHexa = "";
                etapa = 0;
                busy = false;
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
