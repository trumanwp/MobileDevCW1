package org.me.gcu.bowieday_adam_s1831372;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

/**
 * Shows the incidents or the roadworks (chosen with the radio buttons) in a ListView.
 * The data comes from the Activity's TrafficViewModel, shared by all the fragments.
 */
public class SituationListFragment extends Fragment implements RadioGroup.OnCheckedChangeListener, AdapterView.OnItemClickListener
{
    private TrafficViewModel viewModel;
    private RadioGroup feedChoice;
    private RadioButton incidentsButton, roadworksButton;
    private TextView durationKey;
    private SituationAdapter adapter;
    private SituationListListener listener;

    // The Activity implements this to know when to show the detail view (as in the Pizza fragments lab)
    public interface SituationListListener
    {
        void onSituationSelected();
    }

    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);
        if (context instanceof SituationListListener)
        {
            listener = (SituationListListener) context;
        }
        else
        {
            throw new RuntimeException(context.toString() + " must implement SituationListListener");
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        listener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.frag_situation_list, container, false);
        feedChoice = (RadioGroup) v.findViewById(R.id.feedChoice);
        incidentsButton = (RadioButton) v.findViewById(R.id.incidentsButton);
        roadworksButton = (RadioButton) v.findViewById(R.id.roadworksButton);
        durationKey = (TextView) v.findViewById(R.id.durationKey);
        // Each band name is written in its own colour
        durationKey.setText(Html.fromHtml("Duration: "
                + "<font color='#2E7D32'><b>■ Short</b></font> (up to a week)  "
                + "<font color='#E68A00'><b>■ Medium</b></font> (up to a month)  "
                + "<font color='#C62828'><b>■ Long</b></font>", Html.FROM_HTML_MODE_LEGACY));

        ListView listView = (ListView) v.findViewById(R.id.situationList);
        listView.setEmptyView(v.findViewById(R.id.emptyText));
        adapter = new SituationAdapter(requireContext(), new ArrayList<>());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        return v;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        Situation tapped = adapter.getItem(position);
        viewModel.select(tapped);
        listener.onSituationSelected();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        // requireActivity(): the fragments share the Activity's ViewModel instead of having their own
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        if (viewModel.isShowingRoadworks())
        {
            roadworksButton.setChecked(true);
        }
        else
        {
            incidentsButton.setChecked(true);
        }
        feedChoice.setOnCheckedChangeListener(this);

        // Re-fill the list whenever new data arrives
        viewModel.getIncidents().observe(getViewLifecycleOwner(), list -> showList());
        viewModel.getRoadworks().observe(getViewLifecycleOwner(), list -> showList());
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId)
    {
        viewModel.setShowingRoadworks(checkedId == R.id.roadworksButton);
        showList();
    }

    private void showList()
    {
        ArrayList<Situation> incidents = viewModel.getIncidents().getValue();
        ArrayList<Situation> roadworks = viewModel.getRoadworks().getValue();
        incidentsButton.setText("Incidents (" + incidents.size() + ")");
        roadworksButton.setText("Roadworks (" + roadworks.size() + ")");

        adapter.clear();
        adapter.addAll(viewModel.isShowingRoadworks() ? roadworks : incidents);
        durationKey.setVisibility(viewModel.isShowingRoadworks() ? View.VISIBLE : View.GONE);
    }

} // End of class
