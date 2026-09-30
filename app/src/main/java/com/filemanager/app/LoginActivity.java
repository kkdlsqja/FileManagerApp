package com.filemanager.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.filemanager.app.network.ApiService;
import com.filemanager.app.network.AuthRequest;
import com.filemanager.app.network.RetrofitClient;
import com.filemanager.app.network.SessionStore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnScanPcQr;
    private Button btnServerSettings;
    private TextView tvGoToSignup;

    private ActivityResultLauncher<ScanOptions> qrScannerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnScanPcQr = findViewById(R.id.btnScanPcQr);
        btnServerSettings = findViewById(R.id.btnServerSettings);
        tvGoToSignup = findViewById(R.id.tvGoToSignup);

        qrScannerLauncher = registerForActivityResult(
                new ScanContract(),
                result -> {
                    String scannedText = result.getContents();

                    if (scannedText == null) {
                        Toast.makeText(
                                LoginActivity.this,
                                "QR 스캔을 취소했습니다.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    saveScannedServerAddress(scannedText);
                }
        );

        btnScanPcQr.setOnClickListener(view -> startPcQrScan());
        btnServerSettings.setOnClickListener(
                view -> showServerAddressDialog()
        );

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

    private void startPcQrScan() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("PC 화면의 연결 QR 코드를 비춰 주세요.");
        options.setBeepEnabled(false);
        options.setOrientationLocked(false);
        qrScannerLauncher.launch(options);
    }

    private void saveScannedServerAddress(String scannedText) {
        try {
            Uri scannedUri = Uri.parse(scannedText.trim());
            String path = scannedUri.getPath();

            if (path != null && !path.isEmpty() && !"/".equals(path)) {
                throw new IllegalArgumentException(
                        "FolderHelper PC 연결 QR 코드가 아닙니다."
                );
            }

            String oldAddress = ServerSettings.getBaseUrl(this);
            String newAddress = ServerSettings.saveBaseUrl(
                    this,
                    scannedText
            );

            if (!oldAddress.equals(newAddress)) {
                SessionStore.clear(this);
            }

            Toast.makeText(
                    this,
                    "PC 주소를 저장했습니다: " + newAddress,
                    Toast.LENGTH_LONG
            ).show();
        } catch (IllegalArgumentException exception) {
            Toast.makeText(
                    this,
                    exception.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
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
                .setTitle("서버 주소 직접 입력")
                .setMessage(
                        "에뮬레이터: http://10.0.2.2:8080/\n"
                                + "휴대폰: QR 아래 PC IP 주소"
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
                        // 서버에 연결할 수 없어도 로그인 화면에서 다시 시도할 수 있습니다.
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

