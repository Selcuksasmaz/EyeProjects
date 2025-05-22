package com.example.eyeprojects;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextUsername;
    private TextInputEditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewForgotPassword;
    private TextView textViewRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        textViewRegister = findViewById(R.id.textViewRegister);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        textViewForgotPassword.setOnClickListener(v ->
                Toast.makeText(LoginActivity.this, "Şifremi Unuttum tıklandı.", Toast.LENGTH_SHORT).show()
        );

        textViewRegister.setOnClickListener(v ->
                Toast.makeText(LoginActivity.this, "Yeni Hesap Oluştur tıklandı.", Toast.LENGTH_SHORT).show()
        );
    }

    private void loginUser() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show();
            if (TextUtils.isEmpty(username)) {
                editTextUsername.setError("Bu alan boş bırakılamaz");
            }
            if (TextUtils.isEmpty(password)) {
                editTextPassword.setError("Bu alan boş bırakılamaz");
            }
            return;
        }

        editTextUsername.setError(null);
        editTextPassword.setError(null);

        if (username.equals("admin") && password.equals("admin123")) {
            Toast.makeText(this, "Giriş başarılı!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Giriş başarısız. Bilgileri kontrol edin.", Toast.LENGTH_LONG).show();
        }
    }
}
