package com.filemanager.app;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.filemanager.app.network.ApiService;
import com.filemanager.app.network.FileOperationLogItem;
import com.filemanager.app.network.RetrofitClient;
import com.filemanager.app.network.SessionStore;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FileHistoryActivity extends AppCompatActivity {

    private static final String TAG = "FileHistoryActivity";

    private RecyclerView rvHistory;
    private TextView tvHistoryStatus;
    private Long pcId;
    private Call<List<FileOperationLogItem>> historyCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_history);

        tvHistoryStatus = findViewById(R.id.tvHistoryStatus);
        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(new FileHistoryAdapter(new ArrayList<>()));

        pcId = getIntent().getLongExtra("PC_ID", -1L);
        if (pcId == null || pcId <= 0L) {
            tvHistoryStatus.setText("PC 정보가 없습니다.");
            return;
        }

        loadHistory();
    }

    private void loadHistory() {
        String authorization = SessionStore.getAuthorizationHeader(this);
        if (authorization == null) {
            tvHistoryStatus.setText("로그인이 필요합니다.");
            return;
        }

        tvHistoryStatus.setText("최근 분류 기록을 불러오는 중...");
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        historyCall = apiService.getFileHistory(authorization, pcId);
        historyCall.enqueue(new Callback<List<FileOperationLogItem>>() {
            @Override
            public void onResponse(
                    Call<List<FileOperationLogItem>> call,
                    Response<List<FileOperationLogItem>> response) {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (!response.isSuccessful()) {
                    tvHistoryStatus.setText(
                            "기록을 불러오지 못했습니다. (HTTP " + response.code() + ")");
                    return;
                }

                List<FileOperationLogItem> entries = response.body();
                if (entries == null || entries.isEmpty()) {
                    tvHistoryStatus.setText("아직 표시할 분류 기록이 없습니다.");
                    rvHistory.setAdapter(new FileHistoryAdapter(new ArrayList<>()));
                    return;
                }

                tvHistoryStatus.setText("최근 " + entries.size() + "개의 분류 기록");
                rvHistory.setAdapter(new FileHistoryAdapter(entries));
            }

            @Override
            public void onFailure(
                    Call<List<FileOperationLogItem>> call,
                    Throwable throwable) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }
                Log.e(TAG, "분류 기록 요청 실패", throwable);
                tvHistoryStatus.setText("네트워크 오류로 기록을 불러오지 못했습니다.");
                Toast.makeText(
                        FileHistoryActivity.this,
                        "서버 연결을 확인해 주세요.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (historyCall != null) {
            historyCall.cancel();
        }
        super.onDestroy();
    }
}
