package com.filemanager.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.filemanager.app.network.RemoteFile;
import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.ViewHolder> {
    private List<RemoteFile> fileList;

    public FileListAdapter(List<RemoteFile> fileList) {
        this.fileList = fileList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RemoteFile file = fileList.get(position);
        holder.tvFileName.setText(file.getFileName());

        // 폴더/파일 구분에 따른 아이콘 변경
        if (file.isDirectory()) {
            holder.ivFileIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size); // 폴더 임시 아이콘
        } else {
            holder.ivFileIcon.setImageResource(android.R.drawable.ic_menu_gallery); // 파일 임시 아이콘
        }

        // 아이템 클릭 이벤트 (나중에 폴더 진입이나 다운로드 연결용)
        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), file.getFileName() + " 클릭됨", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return fileList != null ? fileList.size() : 0;
    }

    public void updateList(List<RemoteFile> newList) {
        this.fileList = newList;
        notifyDataSetChanged(); // 리스트 새로고침
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName;
        ImageView ivFileIcon;

        ViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
        }
    }
}
