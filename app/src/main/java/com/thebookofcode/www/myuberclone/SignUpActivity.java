package com.thebookofcode.www.myuberclone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.material.snackbar.Snackbar;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

public class SignUpActivity extends AppCompatActivity implements View.OnKeyListener, View.OnClickListener {
    private String isDriver;
    CheckBox checkBox;
    EditText txtEmail;
    EditText txtUserName;
    EditText txtPassword;
    EditText txtConfirm;
    ImageView logoImageView;
    ConstraintLayout backgroundLayout;
    Boolean isConnected;
    ConnectivityManager cm;

    LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (isConnected) {
            loadingDialog = new LoadingDialog(SignUpActivity.this);

            checkBox = findViewById(R.id.checkBox);
            txtEmail = findViewById(R.id.txtEmail);
            txtUserName = findViewById(R.id.txtUserName);
            txtPassword = findViewById(R.id.txtPassword);
            txtConfirm = findViewById(R.id.txtConfirm);
            logoImageView = findViewById(R.id.logoImageView);
            backgroundLayout = findViewById(R.id.backgroundLayout);

            logoImageView.setOnClickListener(this);
            backgroundLayout.setOnClickListener(this);

            txtConfirm.setOnKeyListener(this);
        } else {
            checkConnection();
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

    public void signup(View view) {

        isDriver = (checkBox.isChecked()) ? "Y" : "N";
        String email = txtEmail.getText().toString();
        String userName = txtUserName.getText().toString();
        String password = txtPassword.getText().toString();
        String confirm = txtConfirm.getText().toString();

        if (email.equals("") || userName.equals("") || password.equals("") || confirm.equals("")) {
            Snackbar.make(view, "Please fill all the required fields and try again", Snackbar.LENGTH_LONG).show();
        } else {
            if (password.equals(confirm)) {
                ParseUser user = new ParseUser();
                user.setEmail(email);
                user.setUsername(email);
                user.setPassword(password);
                user.put("isdriver", isDriver);
                user.put("name", userName);
                user.signUpInBackground(new SignUpCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            Log.i("Status:", "Sign-up successful");
                            Snackbar.make(view, "Sign-up successful", Snackbar.LENGTH_LONG).show();
                            redirect();
                        } else {
                            Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_LONG).show();
                        }
                    }
                });


            } else {
                Snackbar.make(view, "Password Mismatch", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {

        if (i == keyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            signup(view);
        }

        return false;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.logoImageView || view.getId() == R.id.backgroundLayout) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    public void restartActivity() {
        Intent intent = new Intent(this, SignUpActivity.class);
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
            Snackbar.make(txtConfirm, e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }


}