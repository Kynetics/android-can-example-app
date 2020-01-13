/*
 * Copyright (C)  2019 Kynetics, LLC
 * SPDX-License-Identifier: Apache-2.0
 */

package com.kynetics.can_example_application.ui.main;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.kynetics.can_example_application.R;

import java.io.IOException;

import de.entropia.can.CanSocket;

/**
 * Fragment containing the CAN sender view.
 */
public class SenderFragment extends Fragment {
    private static final String ARG_CAN_IFACE = "can_iface";
    private static final String notBindedStr = "not binded";
    private static final int notBindedColor = Color.parseColor("#ff8364");
    private static final String bindedStr = "binded";
    private static final int bindedColor = Color.parseColor("#8fbbaf");
    private static String TAG = "KyneticsCanExampleApplication:SenderFragment";
    private static CanSocket socket;
    private static CanSocket.CanInterface canIf;
    private TextView socketStatusTextview;
    private ToggleButton bindBtn;
    private CheckBox loopbackCheckbox;
    private CheckBox rcvOwnMsgCheckbox;
    private FloatingActionButton sendFab;
    private EditText frameIdEdittext;
    private RadioGroup frameTypeRadiogroup;
    private EditText frameDataEdittext;


    public static SenderFragment newInstance(String canIface) {
        SenderFragment fragment = new SenderFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_CAN_IFACE, canIface);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            Log.d(TAG, getArguments().getString(ARG_CAN_IFACE));
        }
        /* Create socket */
        try {
            socket = new CanSocket(CanSocket.Mode.RAW);
            canIf = new CanSocket.CanInterface(socket, getArguments().getString(ARG_CAN_IFACE));
            socket.bind(canIf);
            Log.i(TAG, "RAW socket created");
            Log.d(TAG, canIf.toString());
        } catch (IOException e) {
            Log.e(TAG, "Error creating and binding socket");
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_sender, container, false);

        /* Setup UI elements */
        bindBtn = root.findViewById(R.id.toggleButton_bind);
        socketStatusTextview = root.findViewById(R.id.textView_socketState);
        loopbackCheckbox = root.findViewById(R.id.checkBox_configLoopback);
        rcvOwnMsgCheckbox = root.findViewById(R.id.checkBox_configRcvOwnMsg);
        frameIdEdittext = root.findViewById(R.id.editText_frameId);
        frameTypeRadiogroup = root.findViewById(R.id.radioGroup_frameType);
        frameDataEdittext = root.findViewById(R.id.editText_frameData);
        sendFab = root.findViewById(R.id.fabSend);

        if (socket != null) {
            Snackbar.make(root, "RAW socket created", Snackbar.LENGTH_SHORT)
                    .show();
        }

        /* Setup socket state */
        socketStatusTextview.setText(bindedStr);
        socketStatusTextview.setTextColor(bindedColor);

        /* Setup bind/unbind button */
        bindBtn.setChecked(true);
        bindBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    if (socket == null) {
                        try {
                            socket = new CanSocket(CanSocket.Mode.RAW);
                            Log.i(TAG, "RAW socket created");
                            Log.d(TAG, canIf.toString());
                        } catch (IOException e) {
                            Log.e(TAG, "Error creating socket");
                            e.printStackTrace();
                        }
                    }

                    try {
                        socket.bind(canIf);
                        socketStatusTextview.setText(bindedStr);
                        socketStatusTextview.setTextColor(bindedColor);
                        Log.i(TAG, "Socket binded");
                    } catch (IOException e) {
                        Log.e(TAG, "Error binding socket");
                        e.printStackTrace();
                        Snackbar.make(getView(), "Error binding socket", Snackbar.LENGTH_SHORT)
                                .show();
                    }
                    Snackbar.make(getView(), "Socket created and binded!", Snackbar.LENGTH_SHORT)
                            .show();
                } else {
                    try {
                        socket.close();
                        socket = null;
                        socketStatusTextview.setText(notBindedStr);
                        socketStatusTextview.setTextColor(notBindedColor);
                        Log.i(TAG, "Socket closed");
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing socket");
                        e.printStackTrace();
                        Snackbar.make(getView(), "Error closing socket", Snackbar.LENGTH_SHORT)
                                .show();
                    }
                    Snackbar.make(getView(), "Socket closed", Snackbar.LENGTH_SHORT)
                            .show();
                }
            }
        });

        /* Setup configuration toggles */
        loopbackCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (socket == null) {
                    loopbackCheckbox.setChecked(!isChecked);
                    Log.e(TAG, "Bind socket first!");
                    Snackbar.make(getView(), "Bind socket first!", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }
                try {
                    socket.setLoopbackMode(isChecked);
                    Log.d(TAG, "Loopback mode: " + (socket.getLoopbackMode() ? "enabled" : "disabled"));
                } catch (IOException e) {
                    Snackbar.make(getView(), "Error setting loopback mode", Snackbar.LENGTH_SHORT)
                            .show();
                    e.printStackTrace();
                }
            }
        });

        rcvOwnMsgCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (socket == null) {
                    rcvOwnMsgCheckbox.setChecked(!isChecked);
                    Log.e(TAG, "Bind socket first!");
                    Snackbar.make(getView(), "Bind socket first!", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }
                try {
                    socket.setRecvOwnMsgsMode(isChecked);
                    Log.d(TAG, "Receive own messages: " + (socket.getRecvOwnMsgsMode() ? "enabled" : "disabled"));
                } catch (IOException e) {
                    Snackbar.make(getView(), "Error setting receive own messages mode", Snackbar.LENGTH_SHORT)
                            .show();
                    e.printStackTrace();
                }
            }
        });

        /* Setup fab button */
        sendFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (socket == null) {
                    Log.e(TAG, "Bind socket first!");
                    Snackbar.make(getView(), "Bind socket first!", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }

                /* Configure frame */
                int reqCanId;
                try {
                    reqCanId = Integer.parseInt(frameIdEdittext.getText().toString());
                } catch (NumberFormatException e) {
                    Snackbar.make(getView(), "Enter a valid ID (0 <= id < 2048)", Snackbar.LENGTH_LONG)
                            .show();
                    return;
                }

                /* Validate frame ID */
                if (reqCanId >= 2048)
                {
                    Log.e(TAG, "Invalid frame ID");
                    Snackbar.make(getView(), "Enter a valid ID (0 <= id < 2048)", Snackbar.LENGTH_LONG)
                            .show();
                    return;
                }

                CanSocket.CanId frameId = new CanSocket.CanId(reqCanId);
                Log.d(TAG, "Frame ID: " + frameId);

                /* Configure frame type */
                int frameTypeIdx = frameTypeRadiogroup.getCheckedRadioButtonId();
                switch (frameTypeIdx)
                {
                    case R.id.radioButton_dataFrame:
                        break;
                    case R.id.radioButton_rtrFrame:
                        frameId.setRTR();
                        break;
                    case R.id.radioButton_errFrame:
                        frameId.setERR();
                        break;
                }
                Log.i(TAG, "RTR: " + (frameId.isSetRTR() ? "1" : "0") + ", ERR: " + (frameId.isSetERR() ? "1" : "0"));

                /* Get frame data */
                String reqData;
                reqData = frameDataEdittext.getText().toString();
                if (reqData.matches(""))
                {
                    Snackbar.make(getView(), "Enter frame data", Snackbar.LENGTH_LONG)
                            .show();
                    return;
                }

                CanSocket.CanFrame frame = new CanSocket.CanFrame(canIf, frameId, reqData.getBytes());
                Log.d(TAG, frame.toString());

                /* Send data frame */
                try {
                    socket.send(frame);

                    Toast.makeText(getContext(), "Data sent", Toast.LENGTH_SHORT)
                            .show();
                } catch (IOException e) {
                    Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        });

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (socket != null)
        {
            try {
                socket.close();
                Log.i(TAG, "Socket closed.");
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket.");
                e.printStackTrace();
            }
        }
    }

}