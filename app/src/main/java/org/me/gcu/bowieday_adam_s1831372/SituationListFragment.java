package org.me.gcu.bowieday_adam_s1831372;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

/**
 * Shows the incidents, the roadworks or the road search results (chosen with the radio buttons) in a ListView.
 * The data comes from the Activity's TrafficViewModel, shared by all the fragments.
 */
public class SituationListFragment extends Fragment implements RadioGroup.OnCheckedChangeListener,
        AdapterView.OnItemClickListener, View.OnClickListener
{
    private TrafficViewModel viewModel;
    private EditText roadEntry;
    private Button searchButton, clearButton;
    private RadioGroup feedChoice;
    private RadioButton incidentsButton, roadworksButton, searchResultsButton;
    private TextView durationKey, emptyText;
    private SituationAdapter adapter;
    private Button mapButton;
    private NavigationListener listener;

    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);
        if (context instanceof NavigationListener)
        {
            listener = (NavigationListener) context;
        }
        else
        {
            throw new RuntimeException(context.toString() + " must implement NavigationListener");
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
        roadEntry = (EditText) v.findViewById(R.id.roadEntry);
        searchButton = (Button) v.findViewById(R.id.searchButton);
        clearButton = (Button) v.findViewById(R.id.clearButton);
        searchButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);
        mapButton = (Button) v.findViewById(R.id.mapButton);
        mapButton.setOnClickListener(this);
        // The keyboard's search key does the same as the Search button
        roadEntry.setOnEditorActionListener((textView, actionId, event) -> {
            onClick(searchButton);
            return true;
        });

        feedChoice = (RadioGroup) v.findViewById(R.id.feedChoice);
        incidentsButton = (RadioButton) v.findViewById(R.id.incidentsButton);
        roadworksButton = (RadioButton) v.findViewById(R.id.roadworksButton);
        searchResultsButton = (RadioButton) v.findViewById(R.id.searchResultsButton);
        durationKey = (TextView) v.findViewById(R.id.durationKey);
        // Each band name is written in its own colour
        durationKey.setText(Html.fromHtml("Duration: "
                + "<font color='#2E7D32'><b>■ Short</b></font> (up to a week)  "
                + "<font color='#E68A00'><b>■ Medium</b></font> (up to a month)  "
                + "<font color='#C62828'><b>■ Long</b></font>", Html.FROM_HTML_MODE_LEGACY));

        ListView listView = (ListView) v.findViewById(R.id.situationList);
        emptyText = (TextView) v.findViewById(R.id.emptyText);
        listView.setEmptyView(emptyText);
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
    public void onClick(View v)
    {
        if (v == searchButton)
        {
            String road = roadEntry.getText().toString().trim();
            if (road.isEmpty())
            {
                roadEntry.setError("Type a road name first, i.e. M8");
                return;
            }
            viewModel.searchByRoad(road);
            searchResultsButton.setChecked(true); // shows the results (calls onCheckedChanged)
            showList();
            hideKeyboard();
        }
        else if (v == mapButton)
        {
            // Put whatever the list is showing on the map (i.e. all the M8 search results)
            ArrayList<Situation> shown = new ArrayList<>();
            for (int i = 0; i < adapter.getCount(); i++)
            {
                shown.add(adapter.getItem(i));
            }
            viewModel.setMapSituations(shown, currentListName());
            listener.onShowMap();
        }
        else if (v == clearButton)
        {
            roadEntry.setText("");
            viewModel.searchByRoad("");
            incidentsButton.setChecked(true);
            hideKeyboard();
        }
    }

    private void hideKeyboard()
    {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(roadEntry.getWindowToken(), 0);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        // requireActivity(): the fragments share the Activity's ViewModel instead of having their own
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        if (viewModel.getListMode() == TrafficViewModel.SHOW_ROADWORKS)
        {
            roadworksButton.setChecked(true);
        }
        else if (viewModel.getListMode() == TrafficViewModel.SHOW_SEARCH)
        {
            searchResultsButton.setChecked(true);
        }
        else
        {
            incidentsButton.setChecked(true);
        }
        feedChoice.setOnCheckedChangeListener(this);

        // Re-fill the list whenever new data arrives
        viewModel.getIncidents().observe(getViewLifecycleOwner(), list -> showList());
        viewModel.getRoadworks().observe(getViewLifecycleOwner(), list -> showList());
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), list -> showList());
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId)
    {
        if (checkedId == R.id.roadworksButton)
        {
            viewModel.setListMode(TrafficViewModel.SHOW_ROADWORKS);
        }
        else if (checkedId == R.id.searchResultsButton)
        {
            viewModel.setListMode(TrafficViewModel.SHOW_SEARCH);
        }
        else
        {
            viewModel.setListMode(TrafficViewModel.SHOW_INCIDENTS);
        }
        showList();
    }

    // i.e. "Roadworks" or "Search: M8", used as the map title
    private String currentListName()
    {
        int mode = viewModel.getListMode();
        if (mode == TrafficViewModel.SHOW_ROADWORKS)
        {
            return "Roadworks";
        }
        if (mode == TrafficViewModel.SHOW_SEARCH)
        {
            return "Search: " + viewModel.getSearchRoad().toUpperCase();
        }
        return "Incidents";
    }

    private void showList()
    {
        ArrayList<Situation> incidents = viewModel.getIncidents().getValue();
        ArrayList<Situation> roadworks = viewModel.getRoadworks().getValue();
        ArrayList<Situation> results = viewModel.getSearchResults().getValue();
        incidentsButton.setText("Incidents (" + incidents.size() + ")");
        roadworksButton.setText("Roadworks (" + roadworks.size() + ")");
        searchResultsButton.setText(viewModel.getSearchRoad().isEmpty()
                ? "Search" : viewModel.getSearchRoad().toUpperCase() + " (" + results.size() + ")");

        adapter.clear();
        int mode = viewModel.getListMode();
        if (mode == TrafficViewModel.SHOW_ROADWORKS)
        {
            adapter.addAll(roadworks);
            emptyText.setText("No roadworks to show");
        }
        else if (mode == TrafficViewModel.SHOW_SEARCH)
        {
            adapter.addAll(results);
            emptyText.setText(viewModel.getSearchRoad().isEmpty()
                    ? "Type a road name above and press Search"
                    : "Nothing found on " + viewModel.getSearchRoad().toUpperCase());
        }
        else
        {
            adapter.addAll(incidents);
            emptyText.setText("No incidents to show");
        }
        // The colour key is useful whenever roadworks can be in the list
        durationKey.setVisibility(mode == TrafficViewModel.SHOW_INCIDENTS ? View.GONE : View.VISIBLE);
        mapButton.setText("Show these " + adapter.getCount() + " on the map");
        mapButton.setEnabled(adapter.getCount() > 0);
    }

} // End of class
