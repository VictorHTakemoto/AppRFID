package com.honeywell.rfidsimpleexample;

import android.net.IpSecManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.honeywell.rfidservice.RfidManager;
import com.honeywell.rfidservice.rfid.AntennaPower;
import com.honeywell.rfidservice.rfid.RfidReader;
import com.honeywell.rfidservice.rfid.RfidReaderException;

public class ConfigActivity extends AppCompatActivity
{

    private AntennaPower[] mAntList = new AntennaPower[0];
    private RfidReader mReader;

    private Spinner mSelectReadPower;
    private Spinner mSelectWritePower;
    private TextView mAntReadPower;
    private TextView mAntWritePower;
    private String[] mPowerValues = {"Potências", "500", "600", "700", "800", "900", "1000", "1100",
            "1200", "1300", "1400", "1500", "1600", "1700", "1800", "1900",
            "2000", "2100", "2200", "2300", "2400", "2500", "2600", "2700", "2800", "2900", "3000"};
    private String fixedValue = "Potências";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        //Objeto Leitor RFID
        mReader = MyApplication.getInstance().mRfidReader;
        //Spinner para definir nova potência a antena
        mSelectReadPower = findViewById(R.id.selectReadPower);
        mSelectWritePower = findViewById(R.id.selectWritePower);
        //Busca a potência atual da antena da antena
        mAntReadPower = findViewById(R.id.antReadPower);
        mAntWritePower = findViewById(R.id.antWritePower);

        //TODO: Testes, testes e testes...

        getAntId();
        setAntennaReadPower(mSelectReadPower, mPowerValues, fixedValue);
        setAntennaReadPower(mSelectWritePower, mPowerValues, fixedValue);
    }

    public void setAntennaReadPower(final Spinner mSpinner, String[] lista, final String mFixedValue)
    {
        try
        {
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lista);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSpinner.setAdapter(adapter);
            mSpinner.setSelection(0);
            mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id)
                {
                    String selectedItem = (String) adapterView.getItemAtPosition(position);

                    if (selectedItem.equals(mFixedValue))
                    {
                        return;
                    }
                    try
                    {
                        int convertPower = Integer.parseInt(selectedItem);
                        mAntList = mReader.getAntennaPower();
                        int antId = mAntList[0].getAntennaId();
                        if (mSpinner.getId() == R.id.selectReadPower)
                        {
                            int antWritePower = mAntList[0].getWritePower();
                            mReader.setAntennaPower(new AntennaPower[]{new AntennaPower(antId, convertPower, antWritePower)});
                            mSpinner.setSelection(0);
                            getAntId();
                            ReadActivity.CustomToast customToast = new ReadActivity.CustomToast(getApplicationContext(), "Potência leitura alterada com sucesso");
                            customToast.show();
                        } else
                        {
                            int antReadPower = mAntList[0].getReadPower();
                            mReader.setAntennaPower(new AntennaPower[]{new AntennaPower(antId, antReadPower, convertPower)});
                            mSpinner.setSelection(0);
                            getAntId();
                            ReadActivity.CustomToast customToast = new ReadActivity.CustomToast(getApplicationContext(), "Potência escrita alterada com sucesso");
                            customToast.show();
                        }
                    } catch (Exception ex)
                    {
                        ReadActivity.CustomToast customToast = new ReadActivity.CustomToast(getApplicationContext(), "Não foi possível alterar potência: " + selectedItem);
                        customToast.show();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView)
                {
                }
            });
        } catch (Exception ex)
        {
            ReadActivity.CustomToast customToast = new ReadActivity.CustomToast(getApplicationContext(), "Ocorreu um Problema: " + ex);
            customToast.show();
        }
    }

    public void getAntId()
    {
        try
        {
            mAntList = mReader.getAntennaPower();
            String antReadPower = String.valueOf(mAntList[0].getReadPower());
            String antWritePower = String.valueOf(mAntList[0].getWritePower());
            mAntReadPower.setText(antReadPower);
            mAntWritePower.setText(antWritePower);
        } catch (Exception ex)
        {
            mAntReadPower.setText("");
            mAntWritePower.setText("");
            ReadActivity.CustomToast customToast = new ReadActivity.CustomToast(getApplicationContext(), "Leitor não disponivel: " + ex);
            customToast.show();
        }
    }


    //Area função de testes

    /*public void teste(View view){
        try {
            mAntList = mReader.getAntennaPower();
            int antId = mAntList[0].getAntennaId();
            mReader.setAntennaPower(new AntennaPower[]{new AntennaPower(1, 1000)});
            //mAntList[0].setReadPower(1000);

            ReadActivity.CustomToast customToast = new ReadActivity.CustomToast(getApplicationContext(), "Power antena setado");
            customToast.show();
        } catch (Exception ex){
            ReadActivity.CustomToast customToast = new ReadActivity.CustomToast(getApplicationContext(), "deu erro: " + ex);
            customToast.show();
        }

    }
    public void teste2(View view){
        try {
            mAntList = mReader.getAntennaPower();
            String antReadPower = String.valueOf(mAntList[0].getReadPower());
            ReadActivity.CustomToast customToast = new ReadActivity.CustomToast(getApplicationContext(), "Isso ai: " + antReadPower);
            customToast.show();
        } catch (Exception ex){
            ReadActivity.CustomToast customToast = new ReadActivity.CustomToast(getApplicationContext(), "deu erro: " + ex);
            customToast.show();
        }

    }*/
}
