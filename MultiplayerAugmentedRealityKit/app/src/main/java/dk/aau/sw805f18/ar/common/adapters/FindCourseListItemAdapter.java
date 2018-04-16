package dk.aau.sw805f18.ar.common.adapters;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.models.FindCourseItem;

public class FindCourseListItemAdapter extends ArrayAdapter<FindCourseItem> {
    private Context mContext;
    private ArrayList<FindCourseItem> mValues;

    public FindCourseListItemAdapter(Context context, ArrayList<FindCourseItem> values) {
        super(context, -1, values);
        mContext = context;
        mValues = values;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_item_find_course, parent, false);

        TextView name = rowView.findViewById(R.id.find_course_list_item_name_textview);
        TextView players = rowView.findViewById(R.id.find_course_list_item_players_count_textview);
        TextView age = rowView.findViewById(R.id.find_course_list_item_age_textview);
        TextView distance = rowView.findViewById(R.id.find_course_list_item_distance_textview);

        name.setText(mValues.get(position).getName());
        players.setText(String.valueOf(mValues.get(position).getPlayer()));
        age.setText(String.valueOf(mValues.get(position).getAge()));
        distance.setText(String.valueOf(mValues.get(position).getDistance()));

        return rowView;
    }
}
