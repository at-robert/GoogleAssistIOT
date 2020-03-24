package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class ScanResultAdapter extends RecyclerView.Adapter<ScanResultAdapter.ViewHolder>{

    interface OnItemClickHandler {
        // 提供onItemClick方法作為點擊事件，括號內為接受的參數
        void onItemClick(int position);
    }

    private List<ScanResult> mScanResult;

    private OnItemClickHandler mClickHandler;

    ScanResultAdapter(List<ScanResult> scanResult, OnItemClickHandler clickHandler) {
        mScanResult = scanResult;
        mClickHandler = clickHandler;
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        private TextView txtDeviceName;
        private TextView txtDeviceMAC;

        ViewHolder(View itemView) {
            super(itemView);
            txtDeviceName = (TextView) itemView.findViewById(R.id.txtDeviceName);
            txtDeviceMAC = (TextView) itemView.findViewById(R.id.txtDeviceMAC);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mClickHandler.onItemClick(getAdapterPosition());
                }
            });
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_items, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.txtDeviceName.setText(mScanResult.get(position).getDevice().getName());
        holder.txtDeviceMAC.setText(mScanResult.get(position).getDevice().getAddress());
    }

    @Override
    public int getItemCount() {
        return mScanResult.size();
    }
}