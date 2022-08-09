package com.thebookofcode.www.myuberclone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnKeyListener, View.OnClickListener {

    EditText txtEmail;
    EditText txtPassword;
    ImageView logoImageView;
    ConstraintLayout backgroundLayout;
    LoadingDialog loadingDialog;
    Boolean isConnected;
    ConnectivityManager cm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ParseUser.getCurrentUser() != null) {
            redirect();
        } else {
            setContentView(R.layout.activity_main);
            getSupportActionBar().hide();
            cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
            if (isConnected) {
                loadingDialog = new LoadingDialog(MainActivity.this);

                txtEmail = findViewById(R.id.txtEmail);
                txtPassword = findViewById(R.id.txtPassword);
                logoImageView = findViewById(R.id.logoImageView);
                backgroundLayout = findViewById(R.id.backgroundLayout);

                logoImageView.setOnClickListener(this);
                backgroundLayout.setOnClickListener(this);

                txtPassword.setOnKeyListener(this);

            } else {
                checkConnection();
            }
        }
    }

    public void redirect() {
        String isDriver = ParseUser.getCurrentUser().get("isdriver").toString();
        if (isDriver.equals("Y")) {
            Intent intent = new Intent(this, DriverActivity.class);
            startActivity(intent);
        } else if (isDriver.equals("N")) {
            Intent intent = new Intent(this, RiderActivity.class);
            startActivity(intent);
        }
    }

    public void login(View view) {

        String email = txtEmail.getText().toString();
        String password = txtPassword.getText().toString();


        if (email.equals("") || password.equals("")) {
            Snackbar.make(view, "Please fill all the required fields and try again", Snackbar.LENGTH_LONG).show();
        } else {


            ParseUser.logInInBackground(email, password, new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {

                    if (user != null) {

                        Snackbar.make(view, "Login Successful", Snackbar.LENGTH_LONG).show();
                        redirect();
                    } else {
                        e.printStackTrace();
                    }
                }
            });

        }

    }

    @Override
    public void onBackPressed() {

        new AlertDialog.Builder(this).setTitle("Exit")
                .setMessage("Are you sure you want to exit")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finishAffinity();
                    }
                }).setNegativeButton("NO", null).show();


    }

    public void gotoSignup(View view) {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {

        if (i == keyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            login(view);
        }

        return false;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.logoImageView || view.getId() == R.id.backgroundLayout) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            View focusView = getCurrentFocus();
            if (focusView != null) {
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        }
    }

    public void restartActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void checkConnection() {
        try {
            new AlertDialog.Builder(this).setTitle("Alert")
                    .setMessage("No Internet Connection Detected")
                    .setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            restartActivity();
                        }
                    }).setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finishAffinity();
                }
            }).show();
        } catch (Exception e) {
            Snackbar.make(logoImageView, e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

}