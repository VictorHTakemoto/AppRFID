package com.honeywell.rfidsimpleexample;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class WriteTag extends AppCompatActivity {

    private Button mBtnReadTag;
    private Button mBtnwrite;
    private TextView mTagToWrite;
    private TextView mData;
    private int Bank;
    private int StartAddress;
    private ProgressDialog mTeste;

    @Override
    protected void onCreate(Bundle savedIntanceState){
        super.onCreate(savedIntanceState);

        setContentView(R.layout.activity_write);

        mBtnReadTag = findViewById(R.id.btn_read_tag);
        mBtnwrite = findViewById(R.id.btn_write);
        mTagToWrite = findViewById(R.id.select_tag);
        mData = findViewById(R.id.data_field);
        Bank = 2;
        StartAddress = 6;

    }
    public void teste(View view){

        mTeste = ProgressDialog.show(this, null, "Teste butao");
    }
}
