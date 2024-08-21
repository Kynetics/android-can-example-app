/*
 * Copyright © 2019 - 2024  Kynetics, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.kynetics.can_example_application;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.kynetics.android.sdk.can.api.CanFrame;
import com.kynetics.android.sdk.can.api.CanId;
import com.kynetics.android.sdk.can.api.CanIdFactory;
import com.kynetics.android.sdk.can.api.CanInterface;
import com.kynetics.android.sdk.can.api.CanMode;
import com.kynetics.android.sdk.can.api.CanSdkManager;
import com.kynetics.android.sdk.can.api.CanSdkManagerFactory;
import com.kynetics.can_example_application.databinding.ActivityMainBinding;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "KyneticsCanExampleApplication:MainActivity";
    private static CanSdkManager mSdkManager;
    private static CanInterface canIf;
    private String selectedCanIface;
    private ActivityMainBinding binding;
    private static recvCanMsgAsyncTask rcvTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        setupCanInterfaceSelection();
        showStartDialog();
    }

    private void setupCanInterfaceSelection() {
        binding.textViewSelectedInterface.setOnClickListener(view -> showStartDialog());
    }

    private void showStartDialog() {
        /* Setup list of can interfaces */
        closeSocketIfOpen();

        List<String> netDevices = findCanDevices();

        if (netDevices == null) {
            showErrorDialog(R.layout.dialog_no_permissions);
        } else if (netDevices.isEmpty()) {
            showErrorDialog(R.layout.dialog_no_ifaces);
        } else {
            showCanInterfacesListDialog(netDevices);
        }
    }

    private void showErrorDialog(int layout) {
        View dialogView = getLayoutInflater().inflate(layout, null);
        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton("Close", (dialogInterface, i) ->
                        finish())
                .setNeutralButton(R.string.menu_about, (dialogInterface, i) ->
                        startAboutActivity())
                .setOnCancelListener(dialogInterface ->
                        finish())
                .show();
    }

    private void showCanInterfacesListDialog(final List<String> netDevices) {
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog, null);
        final Spinner dropdown = dialogView.findViewById(R.id.spinner_canIfaces);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, netDevices);
        dropdown.setAdapter(adapter);

        /* Setup dialog */
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false);

        dialogBuilder.setPositiveButton("OK", (dialogInterface, i) -> {
            Log.d(TAG, dropdown.getSelectedItem().toString() + " selected");
            selectedCanIface = dropdown.getSelectedItem().toString();
            setupCanSdkManager(selectedCanIface);
            binding.canInterfaceSelected.setText(selectedCanIface);
        });

        dialogBuilder.setNeutralButton(R.string.menu_about,
                (dialogInterface, i) -> startAboutActivity());

        final AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }


    private void setupCanSdkManager(final String selectedCanIface) {
        try {
            mSdkManager = CanSdkManagerFactory.INSTANCE.getInstance(CanMode.RAW);
            canIf = new CanInterface(mSdkManager, selectedCanIface);
            mSdkManager.bind(canIf);
            Log.i(TAG, "RAW socket created");
            Log.d(TAG, canIf.toString());
            setupViews();
        } catch (IOException e) {
            Log.e(TAG, "Error creating and binding socket");
            e.printStackTrace();
        }

    }

    private void setupViews() throws IOException {
        setupBindButton();
        setupLoopBackSwitch();
        setupReceiveOwnMsgSwitch();
        setupSendFrameBtn();
        setupDataAcquisition();
        if (mSdkManager != null) {

            binding.switchBind.setChecked(true);
            boolean isLoopbackEnabled = mSdkManager.getLoopbackMode();
            binding.checkBoxConfigLoopback.setChecked(isLoopbackEnabled);

            boolean isRcvOwnMsgEnabled = mSdkManager.getReceiveOwnMessagesMode();
            binding.checkBoxConfigRcvOwnMsg.setChecked(isRcvOwnMsgEnabled);
        }

    }

    private void setupBindButton() {
        binding.switchBind.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (mSdkManager == null) {
                    try {
                        mSdkManager = CanSdkManagerFactory.INSTANCE.getInstance(CanMode.RAW);
                        Log.i(TAG, "RAW socket created");
                        Log.d(TAG, canIf.toString());
                    } catch (IOException e) {
                        Log.e(TAG, "Error creating socket");
                        e.printStackTrace();
                        Snackbar.make(binding.getRoot(), "Error creating socket",
                                        Snackbar.LENGTH_SHORT)
                                .show();
                        return;
                    }
                }

                try {
                    mSdkManager.bind(canIf);
                    Log.i(TAG, "Socket binded");
                } catch (IOException e) {
                    Log.e(TAG, "Error binding socket");
                    e.printStackTrace();
                    Snackbar.make(binding.getRoot(), "Error binding socket",
                                    Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }
                Snackbar.make(binding.getRoot(), "Socket created and binded!",
                                Snackbar.LENGTH_SHORT)
                        .show();
            } else {
                try {
                    if (mSdkManager != null) {
                        mSdkManager.close();
                    }
                    mSdkManager = null;
                    binding.switchDataAcquisition.setChecked(false);
                    Log.i(TAG, "Socket closed");
                } catch (IOException e) {
                    Log.e(TAG, "Error closing socket");
                    e.printStackTrace();
                    Snackbar.make(binding.getRoot(), "Error closing socket",
                                    Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }
                Snackbar.make(binding.getRoot(), "Socket closed", Snackbar.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void setupLoopBackSwitch() {
        binding.checkBoxConfigLoopback.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mSdkManager == null) {
                binding.checkBoxConfigLoopback.setChecked(!isChecked);
                Log.e(TAG, "Bind socket first!");
                showToast("Bind socket first!");
                return;
            }
            try {
                mSdkManager.setLoopbackMode(isChecked);
                Log.d(TAG, "Loopback mode: " + (mSdkManager.getLoopbackMode() ? "enabled"
                        : "disabled"));
            } catch (IOException e) {
                showToast("Error setting loopback mode");
                e.printStackTrace();
            }
        });
    }

    private void setupReceiveOwnMsgSwitch() {
        binding.checkBoxConfigRcvOwnMsg.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mSdkManager == null) {
                binding.checkBoxConfigRcvOwnMsg.setChecked(!isChecked);
                Log.e(TAG, "Bind socket first!");
                showToast("Bind socket first!");
                return;
            }
            try {
                mSdkManager.setReceiveOwnMessagesMode(isChecked);
                Log.d(TAG, "Receive own messages mode: " + (
                        mSdkManager.getReceiveOwnMessagesMode() ? "enabled" : "disabled"));
            } catch (IOException e) {
                showToast("Error setting receive own messages mode");
                e.printStackTrace();
            }
        });
    }

    private void setupSendFrameBtn() {
        binding.fabSend.setOnClickListener(view -> {
            if (mSdkManager == null) {
                Log.e(TAG, "Bind socket first!");
                showToast("Bind socket first!");
                return;
            }

            /* Configure frame */
            int reqCanId;
            try {
                reqCanId = Integer.parseInt(binding.editTextFrameId.getText().toString());
            } catch (NumberFormatException e) {
                showToast("Enter a valid ID (0 <= id < 2048)");
                return;
            }

            /* Validate frame ID */
            if (reqCanId >= 2048) {
                showToast("Enter a valid ID (0 <= id < 2048)");
                return;
            }

            CanId frameId = CanIdFactory.INSTANCE.newInstance(reqCanId);

            Log.d(TAG, "Frame ID: " + frameId);

            /* Configure frame type */
            int frameTypeIdx = binding.radioGroupFrameType.getCheckedRadioButtonId();

            if (frameTypeIdx == R.id.radioButton_rtrFrame) {
                frameId.setRTR();
            } else if (frameTypeIdx == R.id.radioButton_errFrame) {
                frameId.setERR();
            }

            Log.i(TAG, "RTR: " + (frameId.isSetRTR() ? "1" : "0") + ", ERR: " + (
                    frameId.isSetERR() ? "1" : "0"));

            /* Get frame data */
            String reqData;
            reqData = binding.editTextFrameData.getText().toString();
            if (reqData.matches("")) {
                showToast("Enter frame data");
                return;
            }

            CanFrame frame = new CanFrame(canIf, frameId, reqData.getBytes());
            Log.d(TAG, frame.toString());

            /* Send data frame */
            try {
                mSdkManager.send(frame);
                showToast("Data sent");
            } catch (IOException e) {
                e.printStackTrace();
                showToast("e.getMessage()");
            }


        });
    }

    private void setupDataAcquisition() {
        binding.switchDataAcquisition.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (mSdkManager == null) {
                    Log.e(TAG, "Bind socket first!");
                    showToast("Bind socket first!");
                    binding.switchDataAcquisition.setChecked(false);
                    return;
                }
                binding.textViewCanMessages.setText("");
                binding.textViewCanMessages.setVisibility(View.VISIBLE);
                binding.progressBarAcquisition.setVisibility(View.VISIBLE);
                rcvTask = new recvCanMsgAsyncTask();
                rcvTask.execute();
            } else {
                binding.textViewCanMessages.setVisibility(View.GONE);
                binding.progressBarAcquisition.setVisibility(View.GONE);
                rcvTask.cancel(true);
            }
        });
    }

    private void showToast(String msg) {
        Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_recreate) {
            recreate();
            return true;
        } else if (item.getItemId() == R.id.menu_about) {
            startAboutActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startAboutActivity() {
        Intent intent = new Intent(MainActivity.this, AboutActivity.class);
        startActivity(intent);
    }

    private static List<String> findCanDevices() {
        /* Read all available can device names */
        List<String> devices = new ArrayList<>();
        Pattern pattern = Pattern.compile("^ *(.*):");
        try (FileReader reader = new FileReader("/proc/net/dev")) {
            BufferedReader in = new BufferedReader(reader);
            String line;
            while ((line = in.readLine()) != null) {
                Matcher m = pattern.matcher(line);
                if (m.find()) {
                    if (m.group(1).startsWith("can")) {
                        devices.add(m.group(1));
                        Log.d(TAG, "Found CAN device: " + m.group(1));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return devices;
    }


    private class recvCanMsgAsyncTask extends AsyncTask<Void, String, Void> {

        protected Void doInBackground(Void... params) {
            int rcvTimeoutSec = 2;
            CanFrame rcvFrame = null;
            Log.d(TAG, "Listening...");
            String frameMsg;
            while (!isCancelled()) {
                try {
                    rcvFrame = mSdkManager.receive(rcvTimeoutSec);
                    publishProgress(rcvFrame.toString() +
                            "\t" + " - [ASCII: " + new String(rcvFrame.getData()) + "]\n");
                    Log.d(TAG, "Frame received");
                } catch (IOException e) {
                    Log.e(TAG, "No frame received.");
                } catch (NullPointerException e_) {
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
                binding.textViewCanMessages.append(msg[0]);
            }
        }

        protected void onPostExecute(Void... params) {
            binding.progressBarAcquisition.setVisibility(View.GONE);
        }

        @Override
        protected void onCancelled() {
            Log.d(TAG, "onCancelled()");
            binding.switchDataAcquisition.setChecked(false);
            binding.progressBarAcquisition.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSdkManager != null) {
            try {
                mSdkManager.close();
                Log.i(TAG, "Socket closed.");
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket.");
                e.printStackTrace();
            }
        }
    }

    private void closeSocketIfOpen() {
        if (mSdkManager != null) {
            try {
                mSdkManager.close();
                mSdkManager = null;
                Log.i(TAG, "Socket closed.");
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket.");
                e.printStackTrace();
            }
        }
    }
}