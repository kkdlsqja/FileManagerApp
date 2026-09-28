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

    private EditText etSignupEmail, etSignupPassword;
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
            public void onClick(View v) {
                String email = etSignupEmail.getText().toString().trim();
                String password = etSignupPassword.getText().toString().trim();

                if(email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "이메일과 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                registerUser(email, password);
            }
        });
    }

    private void registerUser(String email, String password) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        AuthRequest request = new AuthRequest(email, password);

        Call<String> call = apiService.signup(email, password);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SignupActivity.this, "회원가입 완료! 로그인해주세요.", Toast.LENGTH_SHORT).show();
                    finish(); // 현재 화면(회원가입)을 종료하고 이전 화면(로그인)으로 돌아감
                } else {
                    Toast.makeText(SignupActivity.this, "회원가입 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(SignupActivity.this, "네트워크 에러: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}