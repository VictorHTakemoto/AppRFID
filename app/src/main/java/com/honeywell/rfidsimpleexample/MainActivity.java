package com.honeywell.rfidsimpleexample;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.honeywell.rfidservice.EventListener;
import com.honeywell.rfidservice.RfidManager;
import com.honeywell.rfidservice.TriggerMode;
import com.honeywell.rfidservice.rfid.RfidReader;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity
{
    private static final int PERMISSION_REQUEST_CODE = 1;
    private String[] mPermissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};
    private List<String> mRequestPermissions = new ArrayList<>();

    private MyApplication mMyApplication;
    private RfidManager mRfidMgr;
    private BluetoothAdapter mBluetoothAdapter;

    private Handler mHandler = new Handler();
    private ProgressDialog mWaitDialog;
    private TextView mTvInfo;
    private Button mBtnConnect;
    private Button mBtnCreateReader;
    private ListView mLv;
    private MyAdapter mAdapter;
    private List<BtDeviceInfo> mDevices = new ArrayList();
    private int mSelectedIdx = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mMyApplication = MyApplication.getInstance();
        mRfidMgr = mMyApplication.rfidMgr;

        setContentView(R.layout.activity_main);
        mTvInfo = findViewById(R.id.tv_info);
        mBtnConnect = findViewById(R.id.btn_connect);
        mBtnCreateReader = findViewById(R.id.btn_create_reader);
        showBtn();
        mLv = findViewById(R.id.lv);
        mAdapter = new MyAdapter(this, mDevices);
        mLv.setAdapter(mAdapter);
        mLv.setOnItemClickListener(mItemClickListener);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        requestPermissions();

        //TODO: Testar todas as funçoes e implementar tratamento de erro try catch
        //TODO: Remover as ocorrencias de HardCode, definir os vores em values.strings
        //TODO: Limpar o código, comentar as funçoes
        //TODO: Veirificar beep do coletor


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
        stopScan();
        mHandler.removeCallbacksAndMessages(null);
        mRfidMgr.removeEventListener(mEventListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_bt, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.scan:
                scan();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean requestPermissions()
    {
        try
        {
            if (Build.VERSION.SDK_INT >= 23)
            {
                for (int i = 0; i < mPermissions.length; i++)
                {
                    if (ContextCompat.checkSelfPermission(this, mPermissions[i]) != PackageManager.PERMISSION_GRANTED)
                    {
                        mRequestPermissions.add(mPermissions[i]);
                    }
                }

                if (mRequestPermissions.size() > 0)
                {
                    ActivityCompat.requestPermissions(this, mPermissions, PERMISSION_REQUEST_CODE);
                    return false;
                }
            }
        } catch (Exception ex)
        {
            ReadActivity.CustomToast customToast = new ReadActivity.CustomToast(getApplicationContext(), "Ocorreu um Problema: " + ex);
            customToast.show();
        }
        return true;
    }

    private void showBtn()
    {
        try
        {
            mTvInfo.setTextColor(Color.rgb(128, 128, 128));

            if (isConnected())
            {
                mTvInfo.setText(mMyApplication.macAddress + " conectado.");
                mTvInfo.setTextColor(Color.rgb(0, 128, 0));
                mBtnConnect.setEnabled(true);
                mBtnConnect.setText("Desconectar");
                mBtnCreateReader.setEnabled(true);
            } else
            {
                mTvInfo.setText("Dispositivo não conectado.");
                mBtnConnect.setEnabled(mSelectedIdx != -1);
                mBtnConnect.setText("Conectar");
                mBtnCreateReader.setEnabled(false);
            }
        } catch (Exception ex)
        {
            ReadActivity.CustomToast customToast = new ReadActivity.CustomToast(getApplicationContext(), "Ocorreu um Problema: " + ex);
            customToast.show();
        }
    }

    private boolean isConnected()
    {
        if (mRfidMgr.isConnected())
        {
            return true;
        }

        return false;
    }

    public void clickBtnConn(View v)
    {
        if (isConnected())
        {
            disconnect();
        } else
        {
            connect();
        }
    }

    public void clickBtnCreateReader(View view)
    {
        try
        {
            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    mRfidMgr.createReader();
                }
            }, 1000);

            mWaitDialog = ProgressDialog.show(this, null, "Criando leitor...");
        } catch (Exception ex)
        {
            ReadActivity.CustomToast customToast = new ReadActivity.CustomToast(getApplicationContext(), "Ocorreu um Problema: " + ex);
            customToast.show();
        }
    }

    private void scan()
    {
        try
        {
            if (!requestPermissions())
            {
                return;
            }
            mDevices.clear();
            mSelectedIdx = -1;
            mAdapter.notifyDataSetChanged();
            mBluetoothAdapter.startLeScan(mLeScanCallback);

            mWaitDialog = ProgressDialog.show(this, null, "Verificando dispositivos Bluetooth...");
            mWaitDialog.setCancelable(false);

            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    stopScan();
                }
            }, 5 * 1000);
        } catch (Exception ex)
        {
            ReadActivity.CustomToast customToast = new ReadActivity.CustomToast(getApplicationContext(), "Ocorreu um Problema: " + ex);
            customToast.show();
        }
    }

    private void stopScan()
    {
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        closeWaitDialog();
    }

    private void connect()
    {
        try
        {
            if (mSelectedIdx == -1 || mSelectedIdx >= mDevices.size())
            {
                return;
            }

            mRfidMgr.addEventListener(mEventListener);
            mRfidMgr.connect(mDevices.get(mSelectedIdx).dev.getAddress());
            mWaitDialog = ProgressDialog.show(this, null, "Conectando...");
        } catch (Exception ex)
        {
            ReadActivity.CustomToast customToast = new ReadActivity.CustomToast(getApplicationContext(), "Ocorreu um Problema: " + ex);
            customToast.show();
        }
    }

    private void disconnect()
    {
        mRfidMgr.disconnect();
    }

    private void closeWaitDialog()
    {
        if (mWaitDialog != null)
        {
            mWaitDialog.dismiss();
            mWaitDialog = null;
        }
    }

    private EventListener mEventListener = new EventListener()
    {
        @Override
        public void onDeviceConnected(Object o)
        {
            mMyApplication.macAddress = (String) o;

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    showBtn();
                    closeWaitDialog();
                }
            });
        }

        @Override
        public void onDeviceDisconnected(Object o)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    showBtn();
                    closeWaitDialog();
                }
            });
        }

        @Override
        public void onReaderCreated(boolean b, RfidReader rfidReader)
        {
            MyApplication.getInstance().mRfidReader = rfidReader;
            Intent intent = new Intent(MainActivity.this, ReadActivity.class);
            startActivity(intent);
        }

        @Override
        public void onRfidTriggered(boolean b)
        {
        }

        @Override
        public void onTriggerModeSwitched(TriggerMode triggerMode)
        {
        }
    };

    private long mPrevListUpdateTime;
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord)
        {
            if (device.getName() != null && !device.getName().isEmpty())
            {
                synchronized (mDevices)
                {
                    boolean newDevice = true;

                    for (BtDeviceInfo info : mDevices)
                    {
                        if (device.getAddress().equals(info.dev.getAddress()))
                        {
                            newDevice = false;
                            info.rssi = rssi;
                        }
                    }

                    if (newDevice)
                    {
                        mDevices.add(new BtDeviceInfo(device, rssi));
                    }

                    long cur = System.currentTimeMillis();

                    if (newDevice || cur - mPrevListUpdateTime > 500)
                    {
                        mPrevListUpdateTime = cur;

                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            }
        }
    };

    private class BtDeviceInfo
    {
        BluetoothDevice dev;
        int rssi;

        private BtDeviceInfo(BluetoothDevice dev, int rssi)
        {
            this.dev = dev;
            this.rssi = rssi;
        }
    }

    private class MyAdapter extends ArrayAdapter
    {
        private Context ctx;

        public MyAdapter(Context context, List ls)
        {
            super(context, 0, ls);
            ctx = context;
        }

        public View getView(int position, @Nullable View v, @NonNull ViewGroup parent)
        {
            ViewHolder vh;

            if (v == null)
            {
                LayoutInflater inflater = LayoutInflater.from(ctx);
                v = inflater.inflate(R.layout.list_item_bt_device, null);
                vh = new ViewHolder();
                vh.tvName = v.findViewById(R.id.tvName);
                vh.tvAddr = v.findViewById(R.id.tvAddr);
                vh.tvRssi = v.findViewById(R.id.tvRssi);
                v.setTag(vh);
            } else
            {
                vh = (ViewHolder) v.getTag();
            }

            BtDeviceInfo item = mDevices.get(position);
            vh.tvName.setText(item.dev.getName());
            vh.tvAddr.setText(item.dev.getAddress());
            vh.tvRssi.setText(String.valueOf(item.rssi));

            if (position == mSelectedIdx)
            {
                v.setBackgroundColor(Color.rgb(220, 220, 220));
            } else
            {
                v.setBackgroundColor(Color.argb(0, 0, 0, 0));
            }

            return v;
        }

        class ViewHolder
        {
            TextView tvName;
            TextView tvAddr;
            TextView tvRssi;
        }
    }

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
        {
            mSelectedIdx = i;
            mAdapter.notifyDataSetChanged();
            showBtn();
        }
    };
}
