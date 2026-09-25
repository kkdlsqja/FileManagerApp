package com.filemanager.app;

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

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Button btnLoadList;
    private RecyclerView recyclerView;
    private PcListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 화면 요소(뷰) 연결
        btnLoadList = findViewById(R.id.btnLoadList);
        recyclerView = findViewById(R.id.recyclerView);

        // 2. 리사이클러뷰(리스트)가 세로로 나열되도록 설정
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 3. 버튼 클릭 시 서버에 목록 요청
        btnLoadList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPcListFromServer();
            }
        });
    }

    // 서버에서 PC 목록을 불러오는 메서드
    private void loadPcListFromServer() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        // userId 1번 사용자의 PC 목록을 조회하도록 요청
        Call<List<PcDevice>> call = apiService.getPcList(1L);

        call.enqueue(new Callback<List<PcDevice>>() {
            @Override
            public void onResponse(Call<List<PcDevice>> call, Response<List<PcDevice>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PcDevice> pcList = response.body();

                    // 어댑터 생성
                    adapter = new PcListAdapter(pcList);

                    // 어댑터에 클릭 이벤트 리스너 달아주기
                    adapter.setOnItemClickListener(new PcListAdapter.OnItemClickListener() {
                        @Override
                        public void onItemClick(PcDevice device) {
                            Toast.makeText(MainActivity.this, device.getPcName() + " 연결 시도 중...", Toast.LENGTH_SHORT).show();
                            // 목록 중 하나를 클릭하면 해당 기기의 ID로 연결 시도
                            connectToPc(device.getId());
                        }
                    });

                    // 리사이클러뷰에 어댑터 장착
                    recyclerView.setAdapter(adapter);

                    Toast.makeText(MainActivity.this, pcList.size() + "개의 PC를 불러왔습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "서버 통신 실패 코드: " + response.code());
                    Toast.makeText(MainActivity.this, "목록 불러오기 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<PcDevice>> call, Throwable t) {
                Log.e(TAG, "네트워크 에러 발생: " + t.getMessage());
                Toast.makeText(MainActivity.this, "네트워크 에러 발생", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 선택한 PC와 연결 통신을 수행하는 메서드
    private void connectToPc(Long pcId) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<String> call = apiService.connectPc(pcId);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    String result = response.body();
                    Toast.makeText(MainActivity.this, "연결 성공: " + result, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "연결 실패: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(MainActivity.this, "연결 에러: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}