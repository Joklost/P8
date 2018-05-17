package dk.aau.sw805f18.ar.common.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import dk.aau.sw805f18.ar.R;

public class SimpleRowAdapter extends RecyclerView.Adapter<SimpleRowAdapter.ViewHolder> {
    ArrayList<String> mData;

    public SimpleRowAdapter(ArrayList<String> data) {
        mData = data;
        Log.i("rowAdapter", "Current data size: " + String.valueOf(mData.size()));
    }

    @NonNull
    @Override
    public SimpleRowAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View v = inflater.inflate(R.layout.simple_row_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleRowAdapter.ViewHolder holder, int position) {
        holder.tv.setText(mData.get(position));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tv;

        public ViewHolder(View v) {
            super(v);
            tv = v.findViewById(R.id.simple_row_item_textview);
        }
    }
}
