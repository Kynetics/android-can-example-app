/*
 * Copyright © 2019 - 2024  Kynetics, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.kynetics.can_example_application;

import android.content.DialogInterface;
import android.content.Intent;
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
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.kynetics.can_example_application.ui.main.SectionsPagerAdapter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "KyneticsCanExampleApplication:MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onStart() {
        super.onStart();

        ViewPager viewPager = findViewById(R.id.view_pager);
        if (viewPager.getAdapter() == null) {
            showStartDialog();
        }
    }

    private void showStartDialog() {
        /* Setup list of can interfaces */
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

        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d(TAG, dropdown.getSelectedItem().toString() + " selected");
                SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(
                        getApplicationContext(),
                        getSupportFragmentManager(),
                        dropdown.getSelectedItem().toString());
                ViewPager viewPager = findViewById(R.id.view_pager);
                viewPager.setAdapter(sectionsPagerAdapter);
                TabLayout tabs = findViewById(R.id.tabs);
                tabs.setupWithViewPager(viewPager);
            }
        });

        dialogBuilder.setNeutralButton(R.string.menu_about,
                (dialogInterface, i) -> startAboutActivity());

        final AlertDialog dialog = dialogBuilder.create();
        dialog.show();
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
}