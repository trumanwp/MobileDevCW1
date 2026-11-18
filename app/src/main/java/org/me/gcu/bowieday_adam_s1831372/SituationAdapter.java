package org.me.gcu.bowieday_adam_s1831372;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Fills each ListView row (list_item_situation.xml) with a Situation's type and comment.
 */
public class SituationAdapter extends ArrayAdapter<Situation>
{
    public SituationAdapter(Context context, ArrayList<Situation> situations)
    {
        super(context, 0, situations);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // Reuse an old row if the ListView gives us one, otherwise create it
        if (convertView == null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_situation, parent, false);
        }
        Situation s = getItem(position);
        TextView type = (TextView) convertView.findViewById(R.id.itemType);
        TextView comment = (TextView) convertView.findViewById(R.id.itemComment);
        ImageView icon = (ImageView) convertView.findViewById(R.id.itemIcon);
        icon.setImageResource(s.getIconResource());
        type.setText(s.getDisplayType());
        comment.setText(s.getComment());
        return convertView;
    }
} // End of class
