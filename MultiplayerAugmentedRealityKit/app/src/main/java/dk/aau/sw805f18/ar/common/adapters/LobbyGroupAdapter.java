package dk.aau.sw805f18.ar.common.adapters;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import dk.aau.sw805f18.ar.R;

public class LobbyGroupAdapter extends RecyclerView.Adapter<LobbyGroupAdapter.ViewHolder> {
    private final int PREVIEW_PLAYER_LIST_SIZE = 3;
    private Context mContext;
    private static ClickListener clickListener;


    public LobbyGroupAdapter() {

    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView groupName;
        public LinearLayout playerList;

        public ViewHolder(View v) {
            super(v);
            groupName = v.findViewById(R.id.lobby_group_name);
            playerList = v.findViewById(R.id.lobby_player_list_linearlayout);
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            //clickListener.onItemClick(getAdapterPosition(), v);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.item_lobby_group_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.groupName.setText("Group name");
        LayoutInflater inflater = LayoutInflater.from(mContext);

        for (int i = 0; i < PREVIEW_PLAYER_LIST_SIZE; i++) {
            TextView v = (TextView) inflater.inflate(R.layout.item_lobby_group_preview_player, null);
            v.setText("Jens Birbak v2");
            holder.playerList.addView(v);
        }
    }

    @Override
    public int getItemCount() {
        return 6;
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        LobbyGroupAdapter.clickListener = clickListener;
    }

    public interface ClickListener {
        void onItemClick(int position, View v);
    }
}
