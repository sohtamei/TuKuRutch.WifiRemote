package com.tukurutch.wifiremote;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnClickListener;
import com.orhanobut.dialogplus.ViewHolder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements Button.OnTouchListener, JoystickView.JoystickListener {

    static String serverIP = "192.168.1.100";
    static String serverPort = "50000";

    DatagramSocket udpSocket;
/*
    @BindView(R.id.btnUp)     Button btnUp;
    @BindView(R.id.btnLeft)   Button btnLeft;
    @BindView(R.id.btnRight)  Button btnRight;
    @BindView(R.id.btnDown)   Button btnDown;
*/
    @BindView(R.id.txtIPPort) TextView txtIPPort;
    DialogPlus changePortAndIPDialog;
    EditText ip1, ip2, ip3, ip4, port;

    Map<String, byte[]> Buffer = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JoystickView joystick = new JoystickView(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Buffer.put("UP",    new byte[]{(byte) 0, (byte) 1, (byte) 128, (byte) 128, (byte) 128, (byte) 128, 0, 0, 0, 1} );
        Buffer.put("LEFT",  new byte[]{(byte) 0, (byte) 8, (byte) 128, (byte) 128, (byte) 128, (byte) 128, 0, 0, 0, 1} );
        Buffer.put("OFF",   new byte[]{(byte) 0, (byte) 0, (byte) 128, (byte) 128, (byte) 128, (byte) 128, 0, 0, 0, 1} );
        Buffer.put("RIGHT", new byte[]{(byte) 0, (byte) 4, (byte) 128, (byte) 128, (byte) 128, (byte) 128, 0, 0, 0, 1} );
        Buffer.put("DOWN",  new byte[]{(byte) 0, (byte) 2, (byte) 128, (byte) 128, (byte) 128, (byte) 128, 0, 0, 0, 1} );

        try {
            udpSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
/*
        btnUp.setOnTouchListener(this);
        btnLeft.setOnTouchListener(this);
        btnRight.setOnTouchListener(this);
        btnDown.setOnTouchListener(this);
*/
        txtIPPort.setOnTouchListener(this);
        txtIPPort.setText(serverIP + ":" + serverPort);

        changePortAndIPDialog = DialogPlus.newDialog(this)
                .setContentHolder(new ViewHolder(R.layout.picker_ip_port))
                .setCancelable(true)
                .setExpanded(false)
                .setGravity(Gravity.BOTTOM)
                .setContentHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
                .setOverlayBackgroundResource(android.R.color.transparent)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(DialogPlus dialog, View view) {
                        if (view.getId() == R.id.connect) {
                            serverIP = ip1.getText().toString() + "." + ip2.getText().toString() + "." + ip3.getText().toString() + "." + ip4.getText().toString();
                            serverPort = port.getText().toString();
                            txtIPPort.setText(serverIP + ":" + serverPort);

                            SharedPreferences sPref = getSharedPreferences("cache", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sPref.edit();
                            editor.putString("ip1", ip1.getText().toString());
                            editor.putString("ip2", ip2.getText().toString());
                            editor.putString("ip3", ip3.getText().toString());
                            editor.putString("ip4", ip4.getText().toString());
                            editor.putString("port", port.getText().toString());
                            editor.apply();

                            dialog.dismiss();
                        }
                    }
                })
                .create();
        changePortAndIPDialog.show();

        ip1 = (EditText) changePortAndIPDialog.findViewById(R.id.ip1);
        ip2 = (EditText) changePortAndIPDialog.findViewById(R.id.ip2);
        ip3 = (EditText) changePortAndIPDialog.findViewById(R.id.ip3);
        ip4 = (EditText) changePortAndIPDialog.findViewById(R.id.ip4);
        port = (EditText) changePortAndIPDialog.findViewById(R.id.port);

        SharedPreferences sPref = getSharedPreferences("cache", Context.MODE_PRIVATE);
        ip1.setText(sPref.getString("ip1", "192"));
        ip2.setText(sPref.getString("ip2", "168"));
        ip3.setText(sPref.getString("ip3", "1"));
        ip4.setText(sPref.getString("ip4", "100"));
        port.setText(sPref.getString("port", "50000"));
    }

    int lx=128, ly=128, rx=128, ry=128; // -255~255

    @Override
    public void onJoystickMoved(float x, float y, int id) {
        switch (id)
        {
            case R.id.joystickLeft:
                Log.d("Left:", x + "," + y);
                lx = (int)(x * 127 + 128);
                ly = (int)(y * 127 + 128);
                break;
/*
            case R.id.joystickRight:
                Log.d("Right:", x + "," + y);
                rx = (int)(x * 127 + 128);
                ry = (int)(y * 127 + 128);
                break;
*/
        }
        byte[] buffer = new byte[]{(byte) 0, (byte) 0, (byte) lx, (byte) ly, (byte) rx, (byte) ry, 0, 0, 0, 1};
        sendPacket(buffer);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        switch (v.getId()) {
/*
            case R.id.btnUp:     buttonClicked("UP",     action);  break;
            case R.id.btnLeft:   buttonClicked("LEFT",   action);  break;
            case R.id.btnRight:  buttonClicked("RIGHT",  action);  break;
            case R.id.btnDown:   buttonClicked("DOWN",   action);  break;
*/
            case R.id.txtIPPort:
                changePortAndIPDialog.show();
                break;
        }
        return true;
    }

    void buttonClicked(String key, int action) {
        if (action == MotionEvent.ACTION_DOWN) {
            byte[] buffer = Buffer.get(key);
            sendPacket(buffer);
        } else if (action == MotionEvent.ACTION_UP) {
            byte[] buffer = Buffer.get("OFF");
            sendPacket(buffer);
        }
    }

    private void sendPacket(final byte[] buffer) {
          new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(serverIP), Integer.parseInt(serverPort));
                    udpSocket.send(packet);
                } catch (SocketException e) {
                    Log.e("Udp:", "Socket Error:", e);
                } catch (IOException e) {
                    Log.e("Udp Send:", "IO Error:", e);
                }
            }
        }).start();
    }
}
