package com.filemanager.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.filemanager.app.network.FileOperationLogItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class FileHistoryAdapter
        extends RecyclerView.Adapter<FileHistoryAdapter.ViewHolder> {

    private final List<FileOperationLogItem> entries;

    public FileHistoryAdapter(List<FileOperationLogItem> entries) {
        this.entries = entries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileOperationLogItem item = entries.get(position);
        String result = "SUCCESS".equalsIgnoreCase(item.getStatus())
                ? "이동 완료"
                : "이동 실패";

        holder.tvFileName.setText(item.getFileName());
        holder.tvCategoryStatus.setText(item.getCategory() + " · " + result);
        holder.tvOccurredAt.setText(formatDate(item.getOccurredAt()));

        String source = item.getSourcePath() == null ? "" : item.getSourcePath();
        String destination = item.getDestinationPath() == null
                ? "(이동 위치 없음)"
                : item.getDestinationPath();
        holder.tvPaths.setText(source + "\n→ " + destination);

        if ("SUCCESS".equalsIgnoreCase(item.getStatus())) {
            holder.tvDetail.setVisibility(View.GONE);
        } else {
            holder.tvDetail.setVisibility(View.VISIBLE);
            holder.tvDetail.setText(item.getDetail() == null
                    ? "실패 원인 정보가 없습니다."
                    : item.getDetail());
        }
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0L) {
            return "시간 정보 없음";
        }
        SimpleDateFormat formatter =
                new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA);
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        return formatter.format(new Date(timestamp)) + " KST";
    }

    @Override
    public int getItemCount() {
        return entries == null ? 0 : entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvFileName;
        final TextView tvCategoryStatus;
        final TextView tvOccurredAt;
        final TextView tvPaths;
        final TextView tvDetail;

        ViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvHistoryFileName);
            tvCategoryStatus = itemView.findViewById(R.id.tvHistoryCategoryStatus);
            tvOccurredAt = itemView.findViewById(R.id.tvHistoryOccurredAt);
            tvPaths = itemView.findViewById(R.id.tvHistoryPaths);
            tvDetail = itemView.findViewById(R.id.tvHistoryDetail);
        }
    }
}
