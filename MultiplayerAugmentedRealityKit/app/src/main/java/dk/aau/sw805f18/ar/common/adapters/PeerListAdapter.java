package dk.aau.sw805f18.ar.common.adapters;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import dk.aau.sw805f18.ar.R;

public class PeerListAdapter extends RecyclerView.Adapter<PeerListAdapter.ViewHolder> {


    private WifiP2pDeviceList mDataset;

    static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView mTextView;

        ViewHolder(TextView v) {
            super(v);
            mTextView = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public PeerListAdapter(WifiP2pDeviceList peers) {
        mDataset = peers;
    }

    @NonNull
    @Override
    public PeerListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        TextView v = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_course, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PeerListAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.mTextView.setText(((WifiP2pDevice) mDataset.getDeviceList().toArray()[position]).deviceAddress);
    }

    @Override
    public int getItemCount() {
        if (mDataset == null) {
            return 0;
        }

        return mDataset.getDeviceList().size();
    }

    public void setDataset(WifiP2pDeviceList dataset) {
        this.mDataset = dataset;
    }
}
