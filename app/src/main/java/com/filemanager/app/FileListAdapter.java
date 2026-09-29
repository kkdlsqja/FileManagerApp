package com.filemanager.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.filemanager.app.network.RemoteFile;

import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(RemoteFile file);
    }

    private List<RemoteFile> fileList;
    private final OnItemClickListener listener;

    public FileListAdapter(List<RemoteFile> fileList, OnItemClickListener listener) {
        this.fileList = fileList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RemoteFile file = fileList.get(position);
        holder.tvFileName.setText(file.getFileName());

        if (file.isDirectory()) {
            holder.ivFileIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size);
            holder.itemView.setContentDescription("폴더 " + file.getFileName() + ", 열기");
        } else {
            holder.ivFileIcon.setImageResource(android.R.drawable.ic_menu_gallery);
            holder.itemView.setContentDescription("파일 " + file.getFileName());
        }

        holder.itemView.setOnClickListener(view -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onItemClick(fileList.get(currentPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileList == null ? 0 : fileList.size();
    }

    public void updateList(List<RemoteFile> newList) {
        this.fileList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvFileName;
        final ImageView ivFileIcon;

        ViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
        }
    }
}
