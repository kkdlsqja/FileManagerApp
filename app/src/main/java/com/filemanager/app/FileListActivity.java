package com.filemanager.app;

import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
    private EditText etSearchFiles;
    private LinearLayout layoutSearchFiles;
    private ImageButton btnToggleSearch;
    private Button btnRefreshFiles;
    private Button btnSearchFiles;
    private Button btnSortFiles;
    private Button btnViewHistory;
    private Button btnMoveHere;
    private Button btnCancelMove;

    private int currentSortMode = FileListAdapter.SORT_BY_NAME;
    private final String[] sortLabels = {
            "정렬: 이름순",
            "정렬: 수정일 최신순",
            "정렬: 크기 큰순"
    };

    private Long pcId;
    private String currentPath = "/";
    private String currentSearchQuery = "";
    private boolean isSearchResultMode;
    private Call<List<RemoteFile>> currentCall;
    private Call<String> moveCall;

    private boolean selectingMoveDestination;
    private String pendingMoveSourcePath;
    private String pendingMoveSourceParent = "/";
    private String pendingMoveFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        tvCurrentPath = findViewById(R.id.tvCurrentPath);
        etSearchFiles = findViewById(R.id.etSearchFiles);
        layoutSearchFiles = findViewById(R.id.layoutSearchFiles);
        btnToggleSearch = findViewById(R.id.btnToggleSearch);
        btnRefreshFiles = findViewById(R.id.btnRefreshFiles);
        btnSearchFiles = findViewById(R.id.btnSearchFiles);
        btnSortFiles = findViewById(R.id.btnSortFiles);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        btnMoveHere = findViewById(R.id.btnMoveHere);
        btnCancelMove = findViewById(R.id.btnCancelMove);
        rvFileList = findViewById(R.id.rvFileList);

        rvFileList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileListAdapter(
                new ArrayList<>(),
                this::onFileItemClicked
        );
        rvFileList.setAdapter(adapter);

        btnRefreshFiles.setOnClickListener(view -> {
            if (isSearchResultMode && !currentSearchQuery.isEmpty()) {
                searchFiles(currentSearchQuery);
            } else {
                loadFiles(currentPath);
            }
        });

        btnSearchFiles.setOnClickListener(view ->
                searchFiles(etSearchFiles.getText().toString())
        );

        btnToggleSearch.setOnClickListener(view -> toggleSearchPanel());

        etSearchFiles.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchFiles(etSearchFiles.getText().toString());
                return true;
            }
            return false;
        });

        btnSortFiles.setText(sortLabels[currentSortMode]);
        btnSortFiles.setOnClickListener(view -> {
            currentSortMode = (currentSortMode + 1) % sortLabels.length;
            adapter.setSortMode(currentSortMode);
            btnSortFiles.setText(sortLabels[currentSortMode]);
        });

        btnViewHistory.setOnClickListener(view -> {
            Intent intent = new Intent(
                    FileListActivity.this,
                    FileHistoryActivity.class
            );
            intent.putExtra("PC_ID", pcId);
            startActivity(intent);
        });

        btnMoveHere.setOnClickListener(view -> confirmMoveToCurrentFolder());
        btnCancelMove.setOnClickListener(
                view -> cancelMoveDestinationSelection()
        );

        pcId = getIntent().getLongExtra("PC_ID", -1L);
        if (pcId == null || pcId <= 0L) {
            tvCurrentPath.setText("PC 정보 없음");
            btnRefreshFiles.setEnabled(false);
            btnSearchFiles.setEnabled(false);
            showToast("PC 정보를 불러오지 못했습니다. PC 목록에서 다시 선택해 주세요.");
            return;
        }

        loadFiles(currentPath);
    }

    private void onFileItemClicked(RemoteFile file) {
        if (file == null || file.getFileName() == null) {
            return;
        }

        if (isSearchResultMode) {
            openSearchResult(file);
            return;
        }

        if (selectingMoveDestination) {
            if (file.isDirectory()) {
                navigateIntoFolder(file);
            } else {
                showToast("이동할 목적지로는 폴더를 선택해 주세요.");
            }
            return;
        }

        if (!file.isDirectory()) {
            beginMoveDestinationSelection(file);
            return;
        }

        navigateIntoFolder(file);
    }

    private void openSearchResult(RemoteFile file) {
        String resultPath = file.getPath();
        if (resultPath == null || resultPath.trim().isEmpty()) {
            showToast("검색 결과의 폴더 경로를 확인할 수 없습니다.");
            return;
        }

        resultPath = normalizeDisplayPath(resultPath);
        String foundPath = resultPath;

        isSearchResultMode = false;
        currentSearchQuery = "";
        etSearchFiles.setText("");

        if (file.isDirectory()) {
            loadFiles(foundPath);
        } else {
            int lastSlash = foundPath.lastIndexOf('/');
            String parentPath = lastSlash < 0
                    ? "/"
                    : foundPath.substring(0, lastSlash);

            showToast("파일 위치를 열었습니다: 바탕화면/" + foundPath);
            loadFiles(parentPath);
        }
    }

    private void toggleSearchPanel() {
        if (layoutSearchFiles.getVisibility() == View.VISIBLE) {
            boolean wasShowingResults = isSearchResultMode;
            layoutSearchFiles.setVisibility(View.GONE);
            btnToggleSearch.setImageResource(android.R.drawable.ic_menu_search);
            btnToggleSearch.setContentDescription("검색 열기");
            etSearchFiles.setText("");
            hideKeyboard();

            if (wasShowingResults) {
                loadFiles(currentPath);
            }
            return;
        }

        layoutSearchFiles.setVisibility(View.VISIBLE);
        btnToggleSearch.setImageResource(
                android.R.drawable.ic_menu_close_clear_cancel
        );
        btnToggleSearch.setContentDescription("검색 닫기");
        etSearchFiles.requestFocus();

        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(
                    etSearchFiles,
                    InputMethodManager.SHOW_IMPLICIT
            );
        }
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(
                    etSearchFiles.getWindowToken(),
                    0
            );
        }
    }

    private void navigateIntoFolder(RemoteFile folder) {
        String basePath = normalizeDisplayPath(currentPath);
        if ("/".equals(basePath)) {
            loadFiles(folder.getFileName());
        } else {
            loadFiles(basePath + "/" + folder.getFileName());
        }
    }

    private void searchFiles(String query) {
        String safeQuery = query == null ? "" : query.trim();
        if (safeQuery.isEmpty()) {
            showToast("검색어를 입력해 주세요.");
            return;
        }

        String authorization = SessionStore.getAuthorizationHeader(this);
        if (authorization == null) {
            showToast("로그인이 필요합니다. 다시 로그인해 주세요.");
            return;
        }

        if (currentCall != null) {
            currentCall.cancel();
        }

        currentSearchQuery = safeQuery;
        isSearchResultMode = true;
        adapter.setShowPath(true);
        adapter.updateList(new ArrayList<>());
        tvCurrentPath.setText("바탕화면 검색: " + safeQuery + " (검색 중)");
        setLoading(true);

        ApiService apiService =
                RetrofitClient.getClient().create(ApiService.class);
        currentCall = apiService.searchFiles(
                authorization,
                pcId,
                safeQuery
        );

        currentCall.enqueue(new Callback<List<RemoteFile>>() {
            @Override
            public void onResponse(
                    Call<List<RemoteFile>> call,
                    Response<List<RemoteFile>> response) {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                setLoading(false);

                if (response.isSuccessful()) {
                    List<RemoteFile> results = response.body();
                    if (results == null) {
                        isSearchResultMode = false;
                        adapter.setShowPath(false);
                        adapter.updateList(new ArrayList<>());
                        updatePathLabel(false);
                        showToast("서버에서 검색 결과를 받지 못했습니다.");
                        return;
                    }

                    isSearchResultMode = true;
                    adapter.setShowPath(true);
                    adapter.updateList(results);
                    tvCurrentPath.setText(
                            "검색 결과: " + safeQuery + " · " + results.size() + "개"
                    );

                    if (results.isEmpty()) {
                        showToast("검색 결과가 없습니다.");
                    } else if (results.size() >= 300) {
                        showToast("검색 결과는 최대 300개까지 표시됩니다.");
                    }
                    return;
                }

                isSearchResultMode = false;
                adapter.setShowPath(false);
                adapter.updateList(new ArrayList<>());
                updatePathLabel(false);
                showToast(createHttpErrorMessage(response));
            }

            @Override
            public void onFailure(
                    Call<List<RemoteFile>> call,
                    Throwable throwable) {

                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }

                setLoading(false);
                isSearchResultMode = false;
                adapter.setShowPath(false);
                adapter.updateList(new ArrayList<>());
                updatePathLabel(false);
                Log.e(TAG, "파일 검색 요청 실패", throwable);
                showToast(createNetworkErrorMessage(throwable));
            }
        });
    }

    private void beginMoveDestinationSelection(RemoteFile file) {
        pendingMoveFileName = file.getFileName();
        pendingMoveSourceParent = currentPath;
        pendingMoveSourcePath = "/".equals(currentPath)
                ? file.getFileName()
                : currentPath + "/" + file.getFileName();

        selectingMoveDestination = true;
        btnMoveHere.setVisibility(View.VISIBLE);
        btnCancelMove.setVisibility(View.VISIBLE);
        btnSortFiles.setVisibility(View.GONE);
        btnViewHistory.setVisibility(View.GONE);
        btnToggleSearch.setVisibility(View.GONE);
        layoutSearchFiles.setVisibility(View.GONE);

        showToast("목적지 폴더를 찾아 이동한 뒤 ‘이 폴더로 이동’을 누르세요.");
        loadFiles("/");
    }

    private void confirmMoveToCurrentFolder() {
        if (!selectingMoveDestination || pendingMoveSourcePath == null) {
            return;
        }

        String destinationPath = "/".equals(currentPath) ? "" : currentPath;
        String destinationLabel = destinationPath.isEmpty()
                ? "바탕화면"
                : "바탕화면/" + destinationPath;
        String message = pendingMoveFileName + " 파일을\n"
                + destinationLabel + " 폴더로 이동할까요?";

        if ("FolderHelperTest".equals(destinationPath)) {
            message += "\n\n보낸 뒤에는 파일 이름에 따라 자동 분류됩니다.";
        }

        new AlertDialog.Builder(this)
                .setTitle("파일 이동 확인")
                .setMessage(message)
                .setPositiveButton(
                        "이동",
                        (dialog, which) ->
                                moveFileTo(destinationPath, destinationLabel)
                )
                .setNegativeButton("취소", null)
                .show();
    }

    private void moveFileTo(String destinationPath, String destinationLabel) {
        String authorization = SessionStore.getAuthorizationHeader(this);
        if (authorization == null) {
            showToast("로그인이 필요합니다. 다시 로그인해 주세요.");
            return;
        }

        String sourcePath = pendingMoveSourcePath;
        String fileName = pendingMoveFileName;
        String sourceParent = pendingMoveSourceParent;

        ApiService apiService =
                RetrofitClient.getClient().create(ApiService.class);

        if (moveCall != null) {
            moveCall.cancel();
        }

        moveCall = apiService.moveFile(
                authorization,
                pcId,
                sourcePath,
                destinationPath
        );

        moveCall.enqueue(new Callback<String>() {
            @Override
            public void onResponse(
                    Call<String> call,
                    Response<String> response) {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (response.isSuccessful()) {
                    showToast(fileName + " 파일을 "
                            + destinationLabel + " 폴더로 이동했습니다.");
                    finishMoveDestinationSelection();
                    loadFiles(sourceParent);
                } else {
                    String message = "파일 이동에 실패했습니다. (HTTP "
                            + response.code() + ")";
                    ResponseBody errorBody = response.errorBody();
                    if (errorBody != null) {
                        try {
                            String serverMessage = errorBody.string().trim();
                            if (!serverMessage.isEmpty()) {
                                message = serverMessage;
                            }
                        } catch (IOException exception) {
                            Log.w(TAG, "이동 오류 내용을 읽지 못했습니다.", exception);
                        }
                    }
                    showToast(message);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable throwable) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }

                Log.e(TAG, "파일 이동 요청 실패", throwable);
                showToast(createNetworkErrorMessage(throwable));
            }
        });
    }

    private void cancelMoveDestinationSelection() {
        String sourceParent = pendingMoveSourceParent;
        finishMoveDestinationSelection();
        loadFiles(sourceParent);
    }

    private void finishMoveDestinationSelection() {
        selectingMoveDestination = false;
        pendingMoveSourcePath = null;
        pendingMoveFileName = null;

        btnMoveHere.setVisibility(View.GONE);
        btnCancelMove.setVisibility(View.GONE);
        btnSortFiles.setVisibility(View.VISIBLE);
        btnViewHistory.setVisibility(View.VISIBLE);
        btnToggleSearch.setVisibility(View.VISIBLE);
    }

    private void loadFiles(String path) {
        currentPath = normalizeDisplayPath(path);
        isSearchResultMode = false;
        currentSearchQuery = "";
        etSearchFiles.setText("");
        adapter.setShowPath(false);
        updatePathLabel(true);
        adapter.updateList(new ArrayList<>());
        setLoading(true);

        if (currentCall != null) {
            currentCall.cancel();
        }

        if (moveCall != null) {
            moveCall.cancel();
        }

        String authorization = SessionStore.getAuthorizationHeader(this);
        if (authorization == null) {
            setLoading(false);
            showToast("로그인이 필요합니다. 다시 로그인해 주세요.");
            return;
        }

        ApiService apiService =
                RetrofitClient.getClient().create(ApiService.class);

        currentCall = apiService.getFileList(
                authorization,
                pcId,
                currentPath
        );

        currentCall.enqueue(new Callback<List<RemoteFile>>() {
            @Override
            public void onResponse(
                    Call<List<RemoteFile>> call,
                    Response<List<RemoteFile>> response) {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                setLoading(false);
                updatePathLabel(false);

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

                setLoading(false);
                adapter.updateList(new ArrayList<>());
                Log.e(TAG, "파일 목록 요청 실패", throwable);
                showToast(createNetworkErrorMessage(throwable));
            }
        });
    }

    private void setLoading(boolean loading) {
        btnRefreshFiles.setEnabled(!loading);
        btnSearchFiles.setEnabled(!loading);
    }

    private void updatePathLabel(boolean loading) {
        String prefix = selectingMoveDestination
                ? "이동할 위치 선택: 바탕화면 "
                : "바탕화면 ";
        tvCurrentPath.setText(prefix + currentPath
                + (loading ? " (불러오는 중)" : ""));
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
        if (isSearchResultMode) {
            loadFiles(currentPath);
            return;
        }

        if (selectingMoveDestination) {
            if (!navigateToParentFolder()) {
                cancelMoveDestinationSelection();
            }
            return;
        }

        if (!navigateToParentFolder()) {
            super.onBackPressed();
        }
    }

    private String createHttpErrorMessage(Response<?> response) {
        int statusCode = response.code();
        String message;

        switch (statusCode) {
            case 400:
                message = "요청 경로나 검색어가 올바르지 않습니다.";
                break;
            case 401:
                message = "로그인이 필요합니다. 다시 로그인해 주세요.";
                break;
            case 403:
                message = "이 PC의 파일을 볼 권한이 없습니다.";
                break;
            case 404:
                message = "PC 또는 폴더를 찾을 수 없습니다.";
                break;
            case 500:
                message = "서버에서 요청을 처리하지 못했습니다.";
                break;
            default:
                message = "서버 응답 오류가 발생했습니다. (HTTP "
                        + statusCode + ")";
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
                return "";
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
        if (moveCall != null) {
            moveCall.cancel();
        }
        super.onDestroy();
    }
}
