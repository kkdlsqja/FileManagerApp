package com.filemanager.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.filemanager.app.network.RemoteFile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {

    public static final int SORT_BY_NAME = 0;
    public static final int SORT_BY_DATE = 1;
    public static final int SORT_BY_SIZE = 2;

    public interface OnItemClickListener {
        void onItemClick(RemoteFile file);
    }

    private List<RemoteFile> fileList;
    private final OnItemClickListener listener;
    private int sortMode = SORT_BY_NAME;
    private boolean showPath;

    public FileListAdapter(
            List<RemoteFile> fileList,
            OnItemClickListener listener) {
        this.fileList = fileList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {
        RemoteFile file = fileList.get(position);
        holder.tvFileName.setText(file.getFileName());

        String modifiedDate = formatModifiedDate(file.getLastModified());
        String details;

        if (file.isDirectory()) {
            holder.ivFileIcon.setImageResource(R.drawable.folder);
            holder.ivFileIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            details = "폴더 · 수정 " + modifiedDate;
        } else {
            holder.ivFileIcon.setImageResource(
                    android.R.drawable.ic_menu_gallery
            );
            details = formatFileSize(file.getFileSize())
                    + " · 수정 " + modifiedDate;
        }

        if (showPath && file.getPath() != null && !file.getPath().isEmpty()) {
            details = "바탕화면/" + file.getPath() + "\n" + details;
        }

        holder.tvFileDetails.setText(details);
        holder.itemView.setContentDescription(
                (file.isDirectory() ? "폴더 " : "파일 ")
                        + file.getFileName()
                        + (showPath && file.getPath() != null
                        ? ", 바탕화면/" + file.getPath()
                        : "")
        );

        holder.itemView.setOnClickListener(view -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION
                    && listener != null) {
                listener.onItemClick(fileList.get(currentPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileList == null ? 0 : fileList.size();
    }

    public void updateList(List<RemoteFile> newList) {
        this.fileList = new ArrayList<>(newList);
        sortFileList();
        notifyDataSetChanged();
    }

    public void setSortMode(int sortMode) {
        this.sortMode = sortMode;
        sortFileList();
        notifyDataSetChanged();
    }

    public void setShowPath(boolean showPath) {
        this.showPath = showPath;
        notifyDataSetChanged();
    }

    private void sortFileList() {
        if (fileList == null) {
            return;
        }

        Comparator<RemoteFile> secondaryComparator;

        if (sortMode == SORT_BY_DATE) {
            secondaryComparator = Comparator
                    .comparingLong(RemoteFile::getLastModified)
                    .reversed();
        } else if (sortMode == SORT_BY_SIZE) {
            secondaryComparator = Comparator
                    .comparingLong(RemoteFile::getFileSize)
                    .reversed();
        } else {
            secondaryComparator = Comparator.comparing(
                    RemoteFile::getFileName,
                    String.CASE_INSENSITIVE_ORDER
            );
        }

        Collections.sort(
                fileList,
                Comparator
                        .comparing((RemoteFile file) -> !file.isDirectory())
                        .thenComparing(secondaryComparator)
                        .thenComparing(
                                RemoteFile::getFileName,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );
    }

    private String formatModifiedDate(long timestamp) {
        if (timestamp <= 0L) {
            return "정보 없음";
        }

        SimpleDateFormat formatter =
                new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA);
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        return formatter.format(new Date(timestamp)) + " KST";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }

        if (bytes < 1024L * 1024L) {
            return String.format(
                    Locale.getDefault(),
                    "%.1f KB",
                    bytes / 1024.0
            );
        }

        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(
                    Locale.getDefault(),
                    "%.1f MB",
                    bytes / (1024.0 * 1024.0)
            );
        }

        return String.format(
                Locale.getDefault(),
                "%.1f GB",
                bytes / (1024.0 * 1024.0 * 1024.0)
        );
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvFileName;
        final TextView tvFileDetails;
        final ImageView ivFileIcon;

        ViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileDetails = itemView.findViewById(R.id.tvFileDetails);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
        }
    }
}
