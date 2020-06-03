package com.tukurutch.wifiremote;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnClickListener;
import com.orhanobut.dialogplus.ViewHolder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements Button.OnTouchListener, JoystickView.JoystickListener {

    static Boolean modeManual = false;
    static String autoAdrs = "";
    static String autoDeviceName = "";
    static String curAdrs = "";
    static int serverPort = 54322;
    static int robotPort = 54321;

    EditText editManualAdrs;
    @BindView(R.id.txtCurAdrs) TextView txtCurAdrs;

    DatagramSocket udpSocket;

  //@BindView(R.id.btnUp) Button btnUp;
    DialogPlus dialog;

  //Map<String, byte[]> Buffer = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JoystickView joystick = new JoystickView(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

      //Buffer.put("UP", new byte[]{(byte) 0, (byte) 1, (byte) 128, (byte) 128, (byte) 128, (byte) 128, 0, 0, 0, 1} );

        try {
            udpSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }

      //btnUp.setOnTouchListener(this);

        txtCurAdrs.setOnTouchListener(this);

        udpListener receiveSocket = new udpListener();
        receiveSocket.execute("");

        dialog = DialogPlus.newDialog(this)
                .setContentHolder(new ViewHolder(R.layout.picker_ip_port))
                .setCancelable(true)
                .setExpanded(false)
                .setGravity(Gravity.BOTTOM)
                .setContentHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
                .setOverlayBackgroundResource(android.R.color.transparent)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(DialogPlus dialog, View view) {
                        if (view.getId() == R.id.btnAuto) {
                            setAdrs("auto", true);
                            dialog.dismiss();

                        } else if (view.getId() == R.id.btnSet) {
                            setAdrs(editManualAdrs.getText().toString(), true);
                            dialog.dismiss();
                        } else {
                            return;
                        }

                    }
                })
                .create();
        //dialog.show();

        editManualAdrs = (EditText) dialog.findViewById(R.id.editManualAdrs);

        SharedPreferences sPref = getSharedPreferences("cache", Context.MODE_PRIVATE);
        setAdrs(sPref.getString("prefAdrs", "auto"), false);
    }

    private void setAdrs(String prefAdrs, Boolean save)
    {
        if(prefAdrs.equals("auto")) {
            modeManual = false;
            curAdrs = autoAdrs;
            if(autoAdrs.equals("")) {
                txtCurAdrs.setText("Auto:No Device");
                editManualAdrs.setText("192.168.1.100");
            } else {
                txtCurAdrs.setText("Auto:"+autoAdrs+"("+autoDeviceName+")");
                editManualAdrs.setText(autoAdrs);
            }
        } else {
            modeManual = true;
            curAdrs = prefAdrs;
            txtCurAdrs.setText("Manual:"+prefAdrs);
            editManualAdrs.setText(prefAdrs);
        }

        if(save) {
            SharedPreferences sPref = getSharedPreferences("cache", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sPref.edit();
            editor.putString("prefAdrs", prefAdrs);
            editor.apply();
        }
    }

    int lx=0, ly=0; // -255~255

    @Override
    public void onJoystickMoved(float x, float y, int id) {
        switch (id)
        {
            case R.id.joystickLeft:
                lx = (int)(x * 255);
                ly = (int)(y * 255);
                Log.d("Left:", x + "," + y);
                break;
/*
            case R.id.joystickRight:
                Log.d("Right:", x + "," + y);
                rx = (int)(x * 127 + 128);
                ry = (int)(y * 127 + 128);
                break;
*/
        }
        byte[] buffer = new byte[]{(byte) 0, (byte)(lx&0xFF), (byte)(lx>>8), (byte)(ly&0xFF), (byte)(ly>>8)};
        sendPacket(buffer);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        switch (v.getId()) {
/*
            case R.id.btnUp:
                buttonClicked("UP", action);
                break;
*/
            case R.id.txtCurAdrs:
                dialog.show();
                break;
        }
        return true;
    }
/*
    void buttonClicked(String key, int action) {
        if (action == MotionEvent.ACTION_DOWN) {
            byte[] buffer = Buffer.get(key);
            sendPacket(buffer);
        } else if (action == MotionEvent.ACTION_UP) {
            byte[] buffer = Buffer.get("OFF");
            sendPacket(buffer);
        }
    }
*/
    private void sendPacket(final byte[] buffer) {
        if(curAdrs.equals("")) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(curAdrs), serverPort);
                    udpSocket.send(packet);
                } catch (SocketException e) {
                    Log.e("Udp:", "Socket Error:", e);
                } catch (IOException e) {
                    Log.e("Udp Send:", "IO Error:", e);
                }
            }
        }).start();
    }

    private class udpListener extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... f_url) {
            try {
                byte buffer[] = new byte[2000];

                DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                try {
                    DatagramSocket ds = new DatagramSocket(robotPort);

                    while (true) {
                        ds.receive(p);
                        autoDeviceName = new String(buffer, 0, p.getLength());
                        autoAdrs = p.getAddress().toString().replace("/","");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("listen", autoAdrs + "," + autoDeviceName);
                                if(!modeManual) setAdrs("auto", false);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
