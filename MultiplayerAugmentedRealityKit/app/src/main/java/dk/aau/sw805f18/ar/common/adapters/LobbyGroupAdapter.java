package dk.aau.sw805f18.ar.common.adapters;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import dk.aau.sw805f18.ar.R;


public class LobbyGroupAdapter extends RecyclerView.Adapter<LobbyGroupAdapter.ViewHolder> {


    public LobbyGroupAdapter() {

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tv;

        public ViewHolder(View v) {
            super(v);

            tv = v.findViewById(R.id.lobby_row_textview);
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.item_lobby_group_row, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tv.setText("blyat");

    }

    @Override
    public int getItemCount() {
        return 30;
    }
}
