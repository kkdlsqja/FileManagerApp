package com.filemanager.app;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.filemanager.app.network.ApiService;
import com.filemanager.app.network.AuthRequest;
import com.filemanager.app.network.RetrofitClient;
import com.filemanager.app.network.SessionStore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnServerSettings;
    private Button btnShowPairingQr;
    private TextView tvGoToSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnServerSettings = findViewById(R.id.btnServerSettings);
        btnShowPairingQr = findViewById(R.id.btnShowPairingQr);
        tvGoToSignup = findViewById(R.id.tvGoToSignup);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(
                            LoginActivity.this,
                            "이메일과 비밀번호를 모두 입력해 주세요.",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                loginUser(email, password);
            }
        });

        btnServerSettings.setOnClickListener(
                view -> showServerAddressDialog()
        );

        btnShowPairingQr.setOnClickListener(
                view -> openPairingQrPage()
        );

        tvGoToSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(
                        LoginActivity.this,
                        SignupActivity.class
                );
                startActivity(intent);
            }
        });

        verifyExistingSession();
    }

    private void showServerAddressDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_URI
        );
        input.setHint("예: http://192.168.0.15:8080/");
        input.setText(ServerSettings.getBaseUrl(this));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle("서버 주소 설정")
                .setMessage(
                        "에뮬레이터: http://10.0.2.2:8080/\n"
                                + "휴대폰: 서버 PC의 로컬 IP 주소"
                )
                .setView(input)
                .setPositiveButton("저장", (dialog, which) -> {
                    String oldAddress = ServerSettings.getBaseUrl(this);

                    try {
                        String newAddress = ServerSettings.saveBaseUrl(
                                this,
                                input.getText().toString()
                        );

                        if (!oldAddress.equals(newAddress)) {
                            // 주소가 바뀌면 이전 서버에서 받은 토큰을 지웁니다.
                            SessionStore.clear(this);
                        }

                        Toast.makeText(
                                this,
                                "서버 주소를 저장했습니다.",
                                Toast.LENGTH_SHORT
                        ).show();
                    } catch (IllegalArgumentException exception) {
                        Toast.makeText(
                                this,
                                exception.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void openPairingQrPage() {
        Uri pairingPageUri = Uri.parse(ServerSettings.getBaseUrl(this))
                .buildUpon()
                .appendPath("pair")
                .build();

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, pairingPageUri));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(
                    this,
                    "QR 페이지를 열 브라우저를 찾을 수 없습니다.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void verifyExistingSession() {
        String authorization =
                SessionStore.getAuthorizationHeader(this);

        if (authorization == null) {
            return;
        }

        ApiService apiService =
                RetrofitClient.getClient().create(ApiService.class);

        apiService.verifyToken(authorization)
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(
                            Call<String> call,
                            Response<String> response) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (response.isSuccessful()) {
                            openMainActivity();
                        } else {
                            SessionStore.clear(LoginActivity.this);
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<String> call,
                            Throwable throwable) {
                        // 서버에 연결할 수 없으면 로그인 화면에서 다시 시도할 수 있습니다.
                    }
                });
    }

    private void openMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void loginUser(String email, String password) {
        ApiService apiService =
                RetrofitClient.getClient().create(ApiService.class);

        AuthRequest request = new AuthRequest(email, password);
        Call<String> call = apiService.login(request);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(
                    Call<String> call,
                    Response<String> response) {
                if (response.isSuccessful()) {
                    String token = response.body();

                    if (token == null || token.trim().isEmpty()) {
                        Toast.makeText(
                                LoginActivity.this,
                                "서버에서 인증 토큰을 받지 못했습니다.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    SessionStore.saveToken(
                            LoginActivity.this,
                            token.trim()
                    );

                    Toast.makeText(
                            LoginActivity.this,
                            "로그인 성공!",
                            Toast.LENGTH_SHORT
                    ).show();

                    openMainActivity();
                } else {
                    Toast.makeText(
                            LoginActivity.this,
                            "로그인 실패: 확인 필요 ("
                                    + response.code() + ")",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(
                    Call<String> call,
                    Throwable throwable) {
                Toast.makeText(
                        LoginActivity.this,
                        "네트워크 오류: " + throwable.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }
}

