package com.filemanager.app;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.filemanager.app.network.ApiService;
import com.filemanager.app.network.RemoteFile;
import com.filemanager.app.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FileListActivity extends AppCompatActivity {

    private RecyclerView rvFileList;
    private FileListAdapter adapter;
    private TextView tvCurrentPath;

    private Long pcId;
    private String currentPath = "/"; // 처음 켰을 때는 최상위 경로(/)부터 시작

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        tvCurrentPath = findViewById(R.id.tvCurrentPath);
        rvFileList = findViewById(R.id.rvFileList);
        rvFileList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FileListAdapter(new ArrayList<>());
        rvFileList.setAdapter(adapter);

        // 이전 화면(MainActivity의 PC 목록)에서 클릭한 PC의 ID 받아오기
        pcId = getIntent().getLongExtra("PC_ID", -1L);

        if (pcId != -1L) {
            loadFiles(pcId, currentPath);
        } else {
            Toast.makeText(this, "PC 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFiles(Long pcId, String path) {
        tvCurrentPath.setText("경로: " + path);
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        Call<List<RemoteFile>> call = apiService.getFileList(pcId, path);
        call.enqueue(new Callback<List<RemoteFile>>() {
            @Override
            public void onResponse(Call<List<RemoteFile>> call, Response<List<RemoteFile>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.updateList(response.body()); // 리사이클러뷰에 데이터 넣기
                } else {
                    Toast.makeText(FileListActivity.this, "파일 목록 불러오기 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<RemoteFile>> call, Throwable t) {
                Toast.makeText(FileListActivity.this, "네트워크 에러: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}