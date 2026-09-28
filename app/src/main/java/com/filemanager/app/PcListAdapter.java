package com.filemanager.app;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.filemanager.app.network.PcDevice;
import java.util.List;

public class PcListAdapter extends RecyclerView.Adapter<PcListAdapter.ViewHolder> {

    private List<PcDevice> pcList;
    public interface OnItemClickListener {
        void onItemClick(PcDevice device);
    }
    private OnItemClickListener listener;
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    // 생성자: 어댑터를 만들 때 데이터를 전달받음
    public PcListAdapter(List<PcDevice> pcList) {
        this.pcList = pcList;
    }

    // 1. item_pc.xml 화면을 가져와서 한 칸의 뷰(View)로 만듦
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pc, parent, false);
        return new ViewHolder(view);
    }

    // 2. 화면의 각 칸(TextView)에 실제 서버 데이터를 입력
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PcDevice device = pcList.get(position);
        holder.tvPcName.setText(device.getPcName());
        holder.tvPcIdentifier.setText("식별자: " + device.getPcIdentifier());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(device); // 클릭된 PC 정보를 MainActivity로 쏴줍니다!
            }
            Intent intent = new Intent(v.getContext(), FileListActivity.class);
            intent.putExtra("PC_ID", device.getId());
            v.getContext().startActivity(intent);
        });
    }

    // 3. 리스트의 총 데이터 개수 반환
    @Override
    public int getItemCount() {
        return pcList == null ? 0 : pcList.size();
    }

    // 화면 안의 텍스트뷰들을 묶어두는 ViewHolder 클래스
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPcName;
        TextView tvPcIdentifier;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPcName = itemView.findViewById(R.id.tvPcName);
            tvPcIdentifier = itemView.findViewById(R.id.tvPcIdentifier);
        }
    }
}