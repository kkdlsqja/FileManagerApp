package com.filemanager.app;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.filemanager.app.network.ApiService;
import com.filemanager.app.network.RemoteFile;
import com.filemanager.app.network.RetrofitClient;
import com.filemanager.app.network.SessionStore;

import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FileListActivity extends AppCompatActivity {

    private static final String TAG = "FileListActivity";

    private RecyclerView rvFileList;
    private FileListAdapter adapter;
    private TextView tvCurrentPath;
    private Button btnRefreshFiles;

    private Long pcId;
    private String currentPath = "/";
    private Call<List<RemoteFile>> currentCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        tvCurrentPath = findViewById(R.id.tvCurrentPath);
        rvFileList = findViewById(R.id.rvFileList);
        btnRefreshFiles = findViewById(R.id.btnRefreshFiles);

        rvFileList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FileListAdapter(new ArrayList<>(), this::onFileItemClicked);
        rvFileList.setAdapter(adapter);

        btnRefreshFiles.setOnClickListener(view -> loadFiles(currentPath));

        pcId = getIntent().getLongExtra("PC_ID", -1L);
        if (pcId == null || pcId <= 0L) {
            tvCurrentPath.setText("PC 정보 없음");
            btnRefreshFiles.setEnabled(false);
            showToast("PC 정보를 불러오지 못했습니다. PC 목록에서 다시 선택해 주세요.");
            return;
        }

        loadFiles(currentPath);
    }

    private void onFileItemClicked(RemoteFile file) {
        if (file == null || file.getFileName() == null) {
            return;
        }

        if (!file.isDirectory()) {
            return;
        }

        String basePath = normalizeDisplayPath(currentPath);
        if ("/".equals(basePath)) {
            loadFiles(file.getFileName());
        } else {
            loadFiles(basePath + "/" + file.getFileName());
        }
    }

    private void loadFiles(String path) {
        currentPath = normalizeDisplayPath(path);
        final String requestedPath = currentPath;

        tvCurrentPath.setText("경로: " + requestedPath + " (불러오는 중)");
        adapter.updateList(new ArrayList<>());
        btnRefreshFiles.setEnabled(false);

        if (currentCall != null) {
            currentCall.cancel();
        }

        String authorization = SessionStore.getAuthorizationHeader(this);
        if (authorization == null) {
            btnRefreshFiles.setEnabled(true);
            showToast("로그인이 필요합니다. 다시 로그인해 주세요.");
            return;
        }

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        currentCall = apiService.getFileList(authorization, pcId, requestedPath);

        currentCall.enqueue(new Callback<List<RemoteFile>>() {
            @Override
            public void onResponse(
                    Call<List<RemoteFile>> call,
                    Response<List<RemoteFile>> response) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                btnRefreshFiles.setEnabled(true);
                tvCurrentPath.setText("경로: " + requestedPath);

                if (response.isSuccessful()) {
                    List<RemoteFile> files = response.body();
                    if (files == null) {
                        adapter.updateList(new ArrayList<>());
                        showToast("서버에서 파일 목록을 받지 못했습니다.");
                        return;
                    }

                    adapter.updateList(files);
                    if (files.isEmpty()) {
                        showToast("이 폴더에는 파일이나 하위 폴더가 없습니다.");
                    }
                    return;
                }

                adapter.updateList(new ArrayList<>());
                showToast(createHttpErrorMessage(response));
            }

            @Override
            public void onFailure(
                    Call<List<RemoteFile>> call,
                    Throwable throwable) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }

                btnRefreshFiles.setEnabled(true);
                adapter.updateList(new ArrayList<>());
                Log.e(TAG, "파일 목록 요청 실패", throwable);
                showToast(createNetworkErrorMessage(throwable));
            }
        });
    }

    private String normalizeDisplayPath(String path) {
        if (path == null || path.trim().isEmpty() || "/".equals(path.trim())) {
            return "/";
        }

        String normalized = path.trim().replace('\\', '/');

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized.isEmpty() ? "/" : normalized;
    }

    private boolean navigateToParentFolder() {
        if ("/".equals(currentPath)) {
            return false;
        }

        int lastSlash = currentPath.lastIndexOf('/');
        if (lastSlash < 0) {
            loadFiles("/");
        } else {
            loadFiles(currentPath.substring(0, lastSlash));
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (!navigateToParentFolder()) {
            super.onBackPressed();
        }
    }

    private String createHttpErrorMessage(Response<?> response) {
        int statusCode = response.code();
        String message;

        switch (statusCode) {
            case 400:
                message = "요청한 폴더 경로가 올바르지 않습니다.";
                break;
            case 401:
                message = "로그인이 필요합니다. 다시 로그인해 주세요.";
                break;
            case 403:
                message = "이 폴더를 볼 권한이 없습니다.";
                break;
            case 404:
                message = "PC 또는 폴더를 찾을 수 없습니다.";
                break;
            case 500:
                message = "서버에서 파일 목록을 처리하지 못했습니다.";
                break;
            default:
                message = "서버 응답 오류가 발생했습니다. (HTTP " + statusCode + ")";
                break;
        }

        String serverMessage = readServerErrorMessage(response.errorBody());
        if (!serverMessage.isEmpty()) {
            message += "\n서버 안내: " + serverMessage;
        }

        return message;
    }

    private String readServerErrorMessage(ResponseBody errorBody) {
        if (errorBody == null) {
            return "";
        }

        try {
            String body = errorBody.string();
            if (body == null || body.trim().isEmpty()) {
                return "";
            }

            try {
                JSONObject json = new JSONObject(body.trim());
                String[] keys = {"message", "detail", "error"};

                for (String key : keys) {
                    String value = json.optString(key, "").trim();
                    if (!value.isEmpty() && !"null".equalsIgnoreCase(value)) {
                        return value.length() > 160
                                ? value.substring(0, 160) + "..."
                                : value;
                    }
                }
            } catch (Exception ignored) {
                // JSON 형식이 아닌 오류 응답은 표시하지 않습니다.
            }
        } catch (IOException exception) {
            Log.w(TAG, "서버 오류 내용을 읽지 못했습니다.", exception);
        }

        return "";
    }

    private String createNetworkErrorMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        if (cause instanceof SocketTimeoutException) {
            return "서버 응답 시간이 초과됐습니다. 서버가 실행 중인지 확인해 주세요.";
        }

        if (cause instanceof UnknownHostException) {
            return "서버 주소를 찾을 수 없습니다. 네트워크와 서버 주소를 확인해 주세요.";
        }

        if (cause instanceof ConnectException) {
            return "서버에 연결할 수 없습니다. 서버 실행 상태를 확인해 주세요.";
        }

        if (cause instanceof SSLException) {
            return "보안 연결에 실패했습니다. 서버 연결 설정을 확인해 주세요.";
        }

        return "네트워크 연결에 실패했습니다. 인터넷과 서버 상태를 확인해 주세요.";
    }

    private void showToast(String message) {
        if (!isFinishing() && !isDestroyed()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (currentCall != null) {
            currentCall.cancel();
        }
        super.onDestroy();
    }
}