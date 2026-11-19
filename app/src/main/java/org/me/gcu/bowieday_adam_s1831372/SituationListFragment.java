package org.me.gcu.bowieday_adam_s1831372;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

/**
 * Shows the incidents, the roadworks or the search results (chosen with the radio buttons) in a ListView.
 * Searches can be by road name, or by distance from Glasgow city centre / the phone's location.
 * The data comes from the Activity's TrafficViewModel, shared by all the fragments.
 */
public class SituationListFragment extends Fragment implements RadioGroup.OnCheckedChangeListener,
        AdapterView.OnItemClickListener, View.OnClickListener
{
    private TrafficViewModel viewModel;
    private EditText roadEntry;
    private Button searchButton, clearButton;
    private EditText kmEntry;
    private Button nearbyButton;
    private CheckBox myLocationBox;
    private boolean askedForLocation = false;

    // Fixed point for the distance search (the brief allows a fixed lat/long): Glasgow city centre
    private static final double GLASGOW_LAT = 55.8609;
    private static final double GLASGOW_LON = -4.2514;

    // Asks the user for the coarse location permission, then runs the search again either way
    private final ActivityResultLauncher<String> locationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> findNearby());
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
        kmEntry = (EditText) v.findViewById(R.id.kmEntry);
        nearbyButton = (Button) v.findViewById(R.id.nearbyButton);
        myLocationBox = (CheckBox) v.findViewById(R.id.myLocationBox);
        nearbyButton.setOnClickListener(this);
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
        else if (v == nearbyButton)
        {
            findNearby();
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
            kmEntry.setText("");
            viewModel.searchByRoad("");
            incidentsButton.setChecked(true);
            hideKeyboard();
        }
    }

    /**
     * Distance search: everything within the typed number of km of Glasgow city centre,
     * or of the phone's (coarse) location if "My location" is ticked.
     */
    private void findNearby()
    {
        double km;
        try
        {
            km = Double.parseDouble(kmEntry.getText().toString().trim());
        }
        catch (NumberFormatException e)
        {
            kmEntry.setError("Type a distance in km, i.e. 20");
            return;
        }
        if (km <= 0)
        {
            kmEntry.setError("Type a distance in km, i.e. 20");
            return;
        }

        hideKeyboard();
        if (!myLocationBox.isChecked())
        {
            showNearby(km, null);
            return;
        }
        boolean allowed = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!allowed && !askedForLocation)
        {
            askedForLocation = true;
            locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION); // comes back to findNearby()
            return;
        }
        if (!allowed)
        {
            showNearby(km, null);
            return;
        }
        findCurrentLocation(km);
    }

    /**
     * Asks the phone for a fresh location (the last known one can be old and far away),
     * then runs the search. Falls back to the last known location if there's no fresh one.
     */
    @SuppressLint("MissingPermission")
    private void findCurrentLocation(double km)
    {
        LocationManager manager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        String provider = null;
        for (String p : new String[] {"fused", LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER})
        {
            if (manager.getProviders(true).contains(p))
            {
                provider = p;
                break;
            }
        }
        if (provider == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
        {
            showNearby(km, lastKnownLocation()); // getCurrentLocation needs Android 11+
            return;
        }
        Toast.makeText(requireContext(), "Finding your location...", Toast.LENGTH_SHORT).show();
        manager.getCurrentLocation(provider, null, ContextCompat.getMainExecutor(requireContext()), location -> {
            if (isAdded()) // the user might have left this screen while waiting
            {
                showNearby(km, location != null ? location : lastKnownLocation());
            }
        });
    }

    // Runs the distance search from a location, or from Glasgow city centre if location is null
    private void showNearby(double km, Location here)
    {
        if (here != null && inScotland(here))
        {
            viewModel.searchByDistance(km, here.getLatitude(), here.getLongitude(), "your location");
        }
        else
        {
            if (myLocationBox.isChecked())
            {
                // Traffic Scotland only covers Scotland, so a location elsewhere would never find anything
                String reason = (here == null) ? "Couldn't get your location" : "Your location is outside Scotland";
                Toast.makeText(requireContext(), reason + ", using Glasgow city centre", Toast.LENGTH_LONG).show();
            }
            viewModel.searchByDistance(km, GLASGOW_LAT, GLASGOW_LON, "Glasgow city centre");
        }
        searchResultsButton.setChecked(true);
        showList();
    }

    // Rough box around Scotland (plus a bit of northern England for border roads like the A74(M)/M6)
    private static boolean inScotland(Location l)
    {
        return l.getLatitude() > 54.5 && l.getLatitude() < 61.0 && l.getLongitude() > -8.0 && l.getLongitude() < -0.5;
    }

    // Most recent location any provider knows about (null if none). Only called once permission is granted
    @SuppressLint("MissingPermission")
    private Location lastKnownLocation()
    {
        LocationManager manager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        for (String provider : manager.getProviders(true))
        {
            Location l = manager.getLastKnownLocation(provider);
            if (l != null && (best == null || l.getTime() > best.getTime()))
            {
                best = l;
            }
        }
        return best;
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
            return viewModel.getSearchDescription();
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
        searchResultsButton.setText(viewModel.getSearchLabel().isEmpty()
                ? "Search" : viewModel.getSearchLabel() + " (" + results.size() + ")");

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
            emptyText.setText(viewModel.getSearchLabel().isEmpty()
                    ? "Search by road name or distance above"
                    : "Nothing found (" + viewModel.getSearchDescription().replace("Search: ", "on ") + ")");
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
