package dk.aau.sw805f18.ar.common.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dk.aau.sw805f18.ar.R;

public class DialogLobbyAssignedAdapter extends RecyclerView.Adapter<DialogLobbyAssignedAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View v) {
            super(v);
        }
    }

    @NonNull
    @Override
    public DialogLobbyAssignedAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View v = inflater.inflate(R.layout.dialog_lobby_content, null);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DialogLobbyAssignedAdapter.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
