package org.me.gcu.bowieday_adam_s1831372;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Locale;

/**
 * Shows all the information parsed for the situation selected in the list
 * (impact, validity, locations, ...). It observes the shared ViewModel's selected situation.
 */
public class SituationDetailFragment extends Fragment
{
    private ImageView icon;
    private TextView type, comment, when, impact, where, record;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.frag_situation_detail, container, false);
        icon = (ImageView) v.findViewById(R.id.detailIcon);
        type = (TextView) v.findViewById(R.id.detailType);
        comment = (TextView) v.findViewById(R.id.detailComment);
        when = (TextView) v.findViewById(R.id.detailWhen);
        impact = (TextView) v.findViewById(R.id.detailImpact);
        where = (TextView) v.findViewById(R.id.detailWhere);
        record = (TextView) v.findViewById(R.id.detailRecord);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        TrafficViewModel viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        viewModel.getSelected().observe(getViewLifecycleOwner(), this::showSituation);
    }

    private void showSituation(Situation s)
    {
        if (s == null)
        {
            type.setText("Nothing selected");
            comment.setText("Pick an incident or roadworks from the list to see its details.");
            return;
        }
        type.setText(s.getDisplayType());
        comment.setText(s.getComment());

        // When: validity
        StringBuilder w = new StringBuilder();
        w.append("Starts: ").append(Situation.formatTime(s.getStartTime())).append('\n');
        if (s.getEndTime() != null)
        {
            w.append("Ends: ").append(Situation.formatTime(s.getEndTime())).append('\n');
        }
        w.append("Duration: ").append(s.getDurationText());
        when.setText(w.toString());

        // Impact: lanes and delays
        StringBuilder i = new StringBuilder();
        if (s.getOperationalLanes() == 0)
        {
            i.append("No lanes open\n");
        }
        else if (s.getOperationalLanes() > 0)
        {
            i.append(s.getOperationalLanes()).append(" lane(s) open\n");
        }
        if (s.getLanesRestricted() > 0)
        {
            i.append(s.getLanesRestricted()).append(" lane(s) restricted\n");
        }
        if (!s.getDelaysText().isEmpty())
        {
            i.append("Delays: ").append(s.getDelaysText()).append('\n');
        }
        if (s.isUrgentRoadworks())
        {
            i.append("Urgent roadworks\n");
        }
        if (s.isRoadworks() && !s.getWorkType().isEmpty())
        {
            i.append("Work type: ").append(s.getWorkType()).append('\n');
        }
        impact.setText(i.length() == 0 ? "No impact information given" : i.toString().trim());

        // Where: direction, roads and both location points
        StringBuilder p = new StringBuilder();
        p.append("Direction: ").append(s.getDirectionText()).append('\n');
        if (!s.getRoadNames().isEmpty())
        {
            p.append("Roads: ").append(String.join(", ", s.getRoadNames())).append('\n');
        }
        for (LocationPoint point : s.getPoints())
        {
            p.append('\n').append(point.getRole().equals("from") ? "From: " : "To: ");
            p.append(point.getDescription().isEmpty() ? String.join(", ", point.getRoadNames()) : point.getDescription());
            if (!point.getJunction().isEmpty())
            {
                p.append(" (").append(point.getJunction()).append(')');
            }
            p.append(String.format(Locale.UK, "\n%.5f, %.5f", point.getLatitude(), point.getLongitude()));
        }
        where.setText(p.toString().trim());

        // Record: where the info comes from
        record.setText("Probability: " + s.getProbability()
                + "\nCreated: " + Situation.formatTime(s.getCreationTime())
                + "\nLast updated: " + Situation.formatTime(s.getVersionTime())
                + "\nReference: " + s.getId());
    }

} // End of class
