package com.filemanager.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.filemanager.app.network.ApiService;
import com.filemanager.app.network.AuthRequest;
import com.filemanager.app.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToSignup = findViewById(R.id.tvGoToSignup);

        // 로그인 버튼 클릭 시
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if(email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "이메일과 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                loginUser(email, password);
            }
        });

        // 회원가입 글자 클릭 시
        tvGoToSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this, "회원가입 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();
                // 향후 추가할 SignupActivity로 이동하는 코드 작성 예정
            }
        });
    }

    private void loginUser(String email, String password) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        AuthRequest request = new AuthRequest(email, password);

        Call<String> call = apiService.login(request);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();

                    // 성공 시 MainActivity로 화면 이동!
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);

                    finish(); // 뒤로가기 눌렀을 때 다시 로그인 화면이 나오지 않도록 종료
                } else {
                    Toast.makeText(LoginActivity.this, "로그인 실패: 확인 필요 (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "네트워크 에러: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}