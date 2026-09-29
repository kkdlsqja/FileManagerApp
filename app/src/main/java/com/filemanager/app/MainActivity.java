package com.filemanager.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.filemanager.app.network.ApiService;
import com.filemanager.app.network.PcDevice;
import com.filemanager.app.network.RetrofitClient;
import com.filemanager.app.network.SessionStore;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PC_NAME = "FolderHelper PC";
    private static final String PC_IDENTIFIER = "folderhelper-computer-01";

    private Button btnLoadList;
    private Button btnLogout;
    private RecyclerView recyclerView;
    private PcListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLoadList = findViewById(R.id.btnLoadList);
        btnLogout = findViewById(R.id.btnLogout);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnLoadList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerPcThenLoadList();
            }
        });

        btnLogout.setOnClickListener(view -> logout());

        registerPcThenLoadList();
    }

    private void logout() {
        String authorization = SessionStore.getAuthorizationHeader(this);
        if (authorization == null) {
            finishLogout("로그아웃했습니다.");
            return;
        }

        ApiService apiService =
                RetrofitClient.getClient().create(ApiService.class);

        apiService.logout(authorization).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                finishLogout("로그아웃했습니다.");
            }

            @Override
            public void onFailure(Call<String> call, Throwable throwable) {
                Log.e(TAG, "서버 로그아웃 요청 실패", throwable);
                finishLogout("서버 연결은 실패했지만 앱에서 로그아웃했습니다.");
            }
        });
    }

    private void finishLogout(String message) {
        SessionStore.clear(this);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void registerPcThenLoadList() {
        String authorization =
                SessionStore.getAuthorizationHeader(this);

        if (authorization == null) {
            Toast.makeText(
                    this,
                    "로그인이 필요합니다. 다시 로그인해 주세요.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        ApiService apiService =
                RetrofitClient.getClient().create(ApiService.class);

        apiService.registerPc(
                authorization,
                PC_NAME,
                PC_IDENTIFIER
        ).enqueue(new Callback<PcDevice>() {
            @Override
            public void onResponse(
                    Call<PcDevice> call,
                    Response<PcDevice> response) {

                if (response.isSuccessful()) {
                    loadPcList(authorization);
                } else {
                    Toast.makeText(
                            MainActivity.this,
                            "PC 등록 실패: HTTP " + response.code()
                                    + ". 다른 계정에 이미 등록된 PC인지 확인해 주세요.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onFailure(
                    Call<PcDevice> call,
                    Throwable throwable) {
                Log.e(TAG, "PC 등록 요청 실패", throwable);
                Toast.makeText(
                        MainActivity.this,
                        "PC 등록 네트워크 오류: " + throwable.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void loadPcList(String authorization) {
        ApiService apiService =
                RetrofitClient.getClient().create(ApiService.class);

        apiService.getPcList(authorization)
                .enqueue(new Callback<List<PcDevice>>() {
                    @Override
                    public void onResponse(
                            Call<List<PcDevice>> call,
                            Response<List<PcDevice>> response) {

                        if (response.isSuccessful()
                                && response.body() != null) {
                            List<PcDevice> pcList = response.body();

                            adapter = new PcListAdapter(pcList);
                            adapter.setOnItemClickListener(device -> {
                                Toast.makeText(
                                        MainActivity.this,
                                        device.getPcName() + " 연결 시도 중...",
                                        Toast.LENGTH_SHORT
                                ).show();
                                connectToPc(device.getId());
                            });

                            recyclerView.setAdapter(adapter);

                            Toast.makeText(
                                    MainActivity.this,
                                    pcList.size() + "개의 PC를 불러왔습니다.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            Log.e(TAG, "PC 목록 요청 실패: HTTP " + response.code());
                            Toast.makeText(
                                    MainActivity.this,
                                    "PC 목록 불러오기 실패: HTTP "
                                            + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<PcDevice>> call,
                            Throwable throwable) {
                        Log.e(TAG, "PC 목록 요청 실패", throwable);
                        Toast.makeText(
                                MainActivity.this,
                                "네트워크 오류: " + throwable.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void connectToPc(Long pcId) {
        String authorization =
                SessionStore.getAuthorizationHeader(this);

        if (authorization == null) {
            Toast.makeText(
                    this,
                    "로그인이 필요합니다. 다시 로그인해 주세요.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        ApiService apiService =
                RetrofitClient.getClient().create(ApiService.class);

        apiService.connectPc(authorization, pcId)
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(
                            Call<String> call,
                            Response<String> response) {

                        if (response.isSuccessful()) {
                            Intent intent = new Intent(
                                    MainActivity.this,
                                    FileListActivity.class
                            );
                            intent.putExtra("PC_ID", pcId);
                            startActivity(intent);
                        } else {
                            Toast.makeText(
                                    MainActivity.this,
                                    "PC 연결 실패: HTTP " + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<String> call,
                            Throwable throwable) {
                        Toast.makeText(
                                MainActivity.this,
                                "연결 오류: " + throwable.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}
