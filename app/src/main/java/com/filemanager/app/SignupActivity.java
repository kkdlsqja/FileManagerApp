package com.filemanager.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.filemanager.app.network.ApiService;
import com.filemanager.app.network.AuthRequest;
import com.filemanager.app.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private EditText etSignupEmail;
    private EditText etSignupPassword;
    private Button btnSignupSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etSignupEmail = findViewById(R.id.etSignupEmail);
        etSignupPassword = findViewById(R.id.etSignupPassword);
        btnSignupSubmit = findViewById(R.id.btnSignupSubmit);

        btnSignupSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = etSignupEmail.getText().toString().trim();
                String password = etSignupPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(
                            SignupActivity.this,
                            "이메일과 비밀번호를 모두 입력해 주세요.",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                if (password.length() < 8) {
                    Toast.makeText(
                            SignupActivity.this,
                            "비밀번호는 8자 이상으로 입력해 주세요.",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                registerUser(email, password);
            }
        });
    }

    private void registerUser(String email, String password) {
        ApiService apiService =
                RetrofitClient.getClient().create(ApiService.class);

        AuthRequest request = new AuthRequest(email, password);
        Call<String> call = apiService.signup(request);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(
                    Call<String> call,
                    Response<String> response) {

                if (response.isSuccessful()) {
                    Toast.makeText(
                            SignupActivity.this,
                            "회원가입 완료! 로그인해 주세요.",
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                } else {
                    Toast.makeText(
                            SignupActivity.this,
                            "회원가입 실패: HTTP " + response.code(),
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(
                    Call<String> call,
                    Throwable throwable) {
                Toast.makeText(
                        SignupActivity.this,
                        "네트워크 오류: " + throwable.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }
}