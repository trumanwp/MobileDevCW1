package org.me.gcu.bowieday_adam_s1831372;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

/**
 * Shows the situations chosen in the list or detail view on a Google Map (based on Lab 7).
 * Each situation gets a marker at its "from" point and a line along its from -> to stretch of road.
 * Tapping a marker's label opens that situation's details.
 */
public class SituationMapFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener,
        GoogleMap.OnInfoWindowClickListener
{
    private GoogleMap mMap;
    private TrafficViewModel viewModel;
    private RadioButton normalViewButton, satelliteViewButton, terrainViewButton;
    private CheckBox panZoom;
    private NavigationListener listener;

    private static final LatLng SCOTLAND = new LatLng(56.8, -4.2); // used when there's nothing to show

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
        View v = inflater.inflate(R.layout.frag_situation_map, container, false);
        normalViewButton = (RadioButton) v.findViewById(R.id.normalViewRadio);
        satelliteViewButton = (RadioButton) v.findViewById(R.id.satelliteViewRadio);
        terrainViewButton = (RadioButton) v.findViewById(R.id.terrainViewRadio);
        panZoom = (CheckBox) v.findViewById(R.id.panZoom);
        normalViewButton.setOnClickListener(this);
        satelliteViewButton.setOnClickListener(this);
        terrainViewButton.setOnClickListener(this);
        panZoom.setOnClickListener(this);
        normalViewButton.toggle();
        panZoom.setChecked(true);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        TextView title = (TextView) view.findViewById(R.id.mapTitle);
        title.setText(viewModel.getMapTitle() + " (" + viewModel.getMapSituations().size() + " on the map)");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Called when the map is ready: add a marker + line for each situation,
     * then move the camera so they all fit on screen.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap)
    {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(panZoom.isChecked());
        mMap.setOnInfoWindowClickListener(this);

        ArrayList<Situation> situations = viewModel.getMapSituations();
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        int pointsAdded = 0;
        for (Situation s : situations)
        {
            PolylineOptions line = new PolylineOptions().width(8)
                    .color(s.isRoadworks() ? Color.rgb(230, 138, 0) : Color.rgb(198, 40, 40));
            LatLng markerPosition = null;
            for (LocationPoint p : s.getPoints())
            {
                LatLng latLng = new LatLng(p.getLatitude(), p.getLongitude());
                line.add(latLng);
                bounds.include(latLng);
                pointsAdded++;
                if (markerPosition == null || p.getRole().equals("from"))
                {
                    markerPosition = latLng;
                }
            }
            if (markerPosition == null)
            {
                continue; // no location in the feed for this one
            }
            mMap.addPolyline(line);
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(markerPosition)
                    .title(s.getDisplayType())
                    .snippet(s.getComment())
                    .icon(BitmapDescriptorFactory.defaultMarker(s.isRoadworks()
                            ? BitmapDescriptorFactory.HUE_ORANGE : BitmapDescriptorFactory.HUE_RED)));
            if (marker != null)
            {
                marker.setTag(s); // so we know which situation was tapped
            }
        }

        if (pointsAdded == 0)
        {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6));
        }
        else if (situations.size() == 1)
        {
            // One situation: zoom in close on it
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bounds.build().getCenter(), 13));
        }
        else
        {
            // Several: wait until the map has its size, then fit them all in
            LatLngBounds allPoints = bounds.build();
            mMap.setOnMapLoadedCallback(() -> mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(allPoints, 80)));
        }
    }

    // Tapping the label above a marker opens that situation's details
    @Override
    public void onInfoWindowClick(@NonNull Marker marker)
    {
        viewModel.select((Situation) marker.getTag());
        listener.onSituationSelected();
    }

    // Map type and zoom options, as in Lab 7
    @Override
    public void onClick(View v)
    {
        if (mMap == null)
        {
            return; // map not ready yet
        }
        if (v == normalViewButton)
        {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
        else if (v == satelliteViewButton)
        {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID); // satellite photo + road names
        }
        else if (v == terrainViewButton)
        {
            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }
        mMap.getUiSettings().setZoomControlsEnabled(panZoom.isChecked());
    }

} // End of class
