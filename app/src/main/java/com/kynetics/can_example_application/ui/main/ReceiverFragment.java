/*
 * Copyright © 2019 - 2024  Kynetics, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.kynetics.can_example_application.ui.main;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.kynetics.android.sdk.can.api.CanFrame;
import com.kynetics.android.sdk.can.api.CanInterface;
import com.kynetics.android.sdk.can.api.CanMode;
import com.kynetics.android.sdk.can.api.CanSdkManager;
import com.kynetics.android.sdk.can.api.CanSdkManagerFactory;
import com.kynetics.can_example_application.R;

import java.io.IOException;

/**
 * Fragment containing the CAN receiver view.
 */
public class ReceiverFragment extends Fragment {
    private static final String ARG_CAN_IFACE = "can_iface";
    private static final String notBindedStr = "not binded";
    private static final String bindedStr = "binded";
    private static final int notBindedColor = Color.parseColor("#ff8364");
    private static final int bindedColor = Color.parseColor("#8fbbaf");
    private static String TAG = "KyneticsCanExampleApplication:ReceiverFragment";
    private static CanSdkManager mCanSdkManager;
    private static CanInterface canIf;
    private static recvCanMsgAsyncTask rcvTask;
    private ToggleButton acquisitionToggleButton;
    private ProgressBar acquisitionProgressBar;
    private TextView socketStatusTextview;
    private ToggleButton bindBtn;
    private TextView canMessageTextview;

    public static ReceiverFragment newInstance(String canIface) {
        ReceiverFragment fragment = new ReceiverFragment();
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
            mCanSdkManager = CanSdkManagerFactory.INSTANCE.getInstance(CanMode.RAW);
            canIf = new CanInterface(mCanSdkManager, getArguments().getString(ARG_CAN_IFACE));
            mCanSdkManager.bind(canIf);
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
        final View root = inflater.inflate(R.layout.fragment_receiver, container, false);

        /* Setup UI elements */
        socketStatusTextview = root.findViewById(R.id.textView_socketState);
        bindBtn = root.findViewById(R.id.toggleButton_bind);
        acquisitionToggleButton = root.findViewById(R.id.toggleButton_acquisition);
        canMessageTextview = root.findViewById(R.id.textView_canMessages);
        acquisitionProgressBar = root.findViewById(R.id.progressBar_acquisition);

        if (mCanSdkManager != null) {
            Snackbar.make(root, "RAW socket created", Snackbar.LENGTH_SHORT)
                    .show();
        }
        else {
            /* Error occurred when creating/binding socket, disable interface */
            return inflater.inflate(R.layout.dialog_no_permissions, container, false);
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
                    if (mCanSdkManager == null) {
                        try {
                            mCanSdkManager = CanSdkManagerFactory.INSTANCE.getInstance(CanMode.RAW);
                            Log.i(TAG, "RAW socket created");
                            Log.d(TAG, canIf.toString());
                        } catch (IOException e) {
                            Log.e(TAG, "Error creating socket");
                            e.printStackTrace();
                            Snackbar.make(root, "Error creating socket", Snackbar.LENGTH_SHORT)
                                    .show();
                            return;
                        }
                    }

                    try {
                        mCanSdkManager.bind(canIf);
                        socketStatusTextview.setText(bindedStr);
                        socketStatusTextview.setTextColor(bindedColor);
                        Log.i(TAG, "Socket binded");
                    } catch (IOException e) {
                        Log.e(TAG, "Error binding socket");
                        e.printStackTrace();
                        Snackbar.make(root, "Error binding socket", Snackbar.LENGTH_SHORT)
                                .show();
                        return;
                    }
                    Snackbar.make(root, "Socket created and binded!", Snackbar.LENGTH_SHORT)
                            .show();
                } else {
                    try {
                        if (rcvTask != null)
                            rcvTask.cancel(true);
                        mCanSdkManager.close();
                        mCanSdkManager = null;
                        socketStatusTextview.setText(notBindedStr);
                        socketStatusTextview.setTextColor(notBindedColor);
                        Log.i(TAG, "Socket closed");
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing socket");
                        e.printStackTrace();
                        Snackbar.make(root, "Error closing socket", Snackbar.LENGTH_SHORT)
                                .show();
                        return;
                    }
                    Snackbar.make(root, "Socket closed", Snackbar.LENGTH_SHORT)
                            .show();
                }
            }
        });


        acquisitionToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    if (mCanSdkManager == null) {
                        Log.e(TAG, "Bind socket first!");
                        Snackbar.make(root, "Bind socket first!", Snackbar.LENGTH_SHORT)
                                .show();
                        acquisitionToggleButton.setChecked(false);
                        return;
                    }
                    canMessageTextview.setText("");
                    acquisitionProgressBar.setVisibility(View.VISIBLE);
                    rcvTask = new recvCanMsgAsyncTask();
                    rcvTask.execute();
                } else {
                    rcvTask.cancel(true);
                }
            }
        });
        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCanSdkManager != null) {
            try {
                mCanSdkManager.close();
                Log.i(TAG, "Socket closed.");
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket.");
                e.printStackTrace();
            }
        }
    }

    private class recvCanMsgAsyncTask extends AsyncTask<Void, String, Void> {

        protected Void doInBackground(Void... params) {
            int rcvTimeoutSec = 2;
            CanFrame rcvFrame = null;
            Log.d(TAG, "Listening...");
            String frameMsg;
            while (!isCancelled()) {
                try {
                    rcvFrame = mCanSdkManager.receive(rcvTimeoutSec);
                    publishProgress(rcvFrame.toString() +
                            "\t" + " - [ASCII: " + new String(rcvFrame.getData()) + "]\n");
                    Log.d(TAG, "Frame received");
                } catch (IOException e) {
                    Log.e(TAG, "No frame received.");
                } catch (NullPointerException e_)
                {
                    Log.e(TAG, "Socket has been closed, cancel this task");
                    break;
                }
            }
            Log.d(TAG, "receive task cancelled.");
            return null;
        }

        @Override
        protected void onProgressUpdate(String... msg) {
            if (msg != null) {
                canMessageTextview.append(msg[0]);
            }
        }

        protected void onPostExecute(Void... params) {
            acquisitionProgressBar.setVisibility(View.INVISIBLE);
        }

        @Override
        protected void onCancelled() {
            Log.d(TAG, "onCancelled()");
            acquisitionToggleButton.setChecked(false);
            acquisitionProgressBar.setVisibility(View.INVISIBLE);
        }
    }

}