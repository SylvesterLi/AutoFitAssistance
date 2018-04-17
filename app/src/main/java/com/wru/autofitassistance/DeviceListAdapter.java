package com.wru.autofitassistance;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by symlith on 2018/4/11.
 */

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceItemVH> {
    private Context mContext;
    public DataSource dataSource;
    public Delegate delegate;

    public DeviceListAdapter(Context context) {
        mContext = context;
    }
    @NonNull
    @Override
    public DeviceItemVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.device_item, parent, false);
        return new DeviceItemVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceItemVH holder, int position) {
        holder.config(dataSource.deviceAtIndex(position));
    }

    @Override
    public int getItemCount() {
        return dataSource != null ? dataSource.numberOfDevice() : 0;
    }

    public class DeviceItemVH extends RecyclerView.ViewHolder {
        private TextView deviceName;
        private TextView deviceAddress;
        private BluetoothDevice device;
        public DeviceItemVH(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceAddress = itemView.findViewById(R.id.device_address);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (delegate != null) {
                        delegate.didSelectDevice(device);
                    }
                }
            });
        }

        public void config(BluetoothDevice device) {
            this.device = device;
            deviceName.setText(device.getName());
            deviceAddress.setText(device.getAddress());
        }
    }

    public interface DataSource {
        int numberOfDevice();
        BluetoothDevice deviceAtIndex(int index);
    }

    public interface Delegate {
        void didSelectDevice(BluetoothDevice device);
    }

}
