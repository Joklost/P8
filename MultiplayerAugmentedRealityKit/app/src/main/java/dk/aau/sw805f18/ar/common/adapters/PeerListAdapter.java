package dk.aau.sw805f18.ar.common.adapters;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import dk.aau.sw805f18.ar.R;

public class PeerListAdapter extends RecyclerView.Adapter<PeerListAdapter.ViewHolder> {
    private List<WifiP2pDevice> mDataset;

    static class ViewHolder extends RecyclerView.ViewHolder {
        // each Data item is just a string in this case
        TextView mTextView;

        ViewHolder(TextView v) {
            super(v);
            mTextView = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public PeerListAdapter() {
    }

    @NonNull
    @Override
    public PeerListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        TextView v = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_find_course, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PeerListAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.mTextView.setText(mDataset.get(position).deviceAddress);
    }

    @Override
    public int getItemCount() {
        if (mDataset == null) {
            return 0;
        }

        return mDataset.size();
    }

    public void setDataset(List<WifiP2pDevice> dataset) {
        this.mDataset = dataset;
    }

    public List<WifiP2pDevice> getDataset() {
        return mDataset;
    }
}
