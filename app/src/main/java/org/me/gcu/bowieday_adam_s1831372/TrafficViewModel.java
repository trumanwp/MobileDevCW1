package org.me.gcu.bowieday_adam_s1831372;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * The ViewModel survives screen rotation, so the downloaded situations are kept
 * while the Activity/Fragments are re-created (lecture 7c).
 * It's the middleman between the View (Activity/Fragments) and the Model (TrafficFeed, TrafficParser):
 * it asks TrafficFeed for data and exposes the results as observable LiveData.
 * It's an AndroidViewModel (a ViewModel that knows the Application) because TrafficFeed
 * needs a Context for the saved files in internal storage.
 */
public class TrafficViewModel extends AndroidViewModel
{
    private final MutableLiveData<ArrayList<Situation>> incidents = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ArrayList<Situation>> roadworks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> status = new MutableLiveData<>("Not downloaded yet");

    // Main thread Handler: receives the Messages from TrafficFeed's worker thread
    private final Handler updateHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        @SuppressWarnings("unchecked")
        public void handleMessage(Message msg)
        {
            // Runs on the main thread, so setValue() is safe here
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            if (msg.what == TrafficFeed.MESSAGE_FAILED)
            {
                status.setValue("Couldn't load " + msg.obj + " at " + time);
                return;
            }
            if (msg.what == TrafficFeed.MESSAGE_INCIDENTS)
            {
                incidents.setValue((ArrayList<Situation>) msg.obj);
            }
            else if (msg.what == TrafficFeed.MESSAGE_ROADWORKS)
            {
                roadworks.setValue((ArrayList<Situation>) msg.obj);
            }
            runSearch(); // keep search results in step with the new data

            // Say where the data came from
            if (msg.arg1 == TrafficFeed.SOURCE_LIVE)
            {
                status.setValue("Last updated " + time);
            }
            else if (msg.arg1 == TrafficFeed.SOURCE_SAVED)
            {
                String savedAt = Instant.ofEpochMilli(msg.getData().getLong("savedAt"))
                        .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE d MMM, HH:mm"));
                status.setValue("Offline (" + msg.getData().getString("problem") + "): showing data saved " + savedAt);
            }
            else
            {
                status.setValue("Offline (" + msg.getData().getString("problem") + "): no saved data yet, showing sample data");
            }
        }
    };

    // Auto-update: every hour (can be reduced for the demo video)
    private static final long REFRESH_INTERVAL_MS = 60 * 60 * 1000;

    // Downloads, then schedules itself to run again after REFRESH_INTERVAL_MS
    private final Runnable refreshTask = new Runnable()
    {
        @Override
        public void run()
        {
            refresh();
            updateHandler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    public TrafficViewModel(@NonNull Application application)
    {
        super(application);
        // Only runs the first time the ViewModel is created (not on rotation),
        // so the app downloads on start-up without a button press
        updateHandler.post(refreshTask);
    }

    // Called when the app is closed for good: stop the timer
    @Override
    protected void onCleared()
    {
        updateHandler.removeCallbacksAndMessages(null);
    }

    public LiveData<ArrayList<Situation>> getIncidents() { return incidents; }

    public LiveData<ArrayList<Situation>> getRoadworks() { return roadworks; }

    public LiveData<String> getStatus() { return status; }

    // What the list shows, kept here so it survives rotation
    public static final int SHOW_INCIDENTS = 0;
    public static final int SHOW_ROADWORKS = 1;
    public static final int SHOW_SEARCH = 2;
    private int listMode = SHOW_INCIDENTS;

    public int getListMode() { return listMode; }

    public void setListMode(int listMode) { this.listMode = listMode; }

    // Search: by road name (i.e. "M8") or by distance from a point (i.e. within 20 km of Glasgow)
    private String searchRoad = "";
    private double searchKm = 0;              // 0 = not a distance search
    private double searchLat, searchLon;
    private String searchPlace = "";          // i.e. "Glasgow city centre" or "your location"
    private final MutableLiveData<ArrayList<Situation>> searchResults = new MutableLiveData<>(new ArrayList<>());

    public LiveData<ArrayList<Situation>> getSearchResults() { return searchResults; }

    // Short name for the search, i.e. "M8" or "Within 20 km", or "" if nothing searched yet
    public String getSearchLabel()
    {
        if (searchKm > 0)
        {
            return "Within " + formatKm(searchKm) + " km";
        }
        return searchRoad.toUpperCase();
    }

    // Longer version for the map title, i.e. "Within 20 km of Glasgow city centre"
    public String getSearchDescription()
    {
        if (searchKm > 0)
        {
            return getSearchLabel() + " of " + searchPlace;
        }
        return "Search: " + getSearchLabel();
    }

    public void searchByRoad(String road)
    {
        searchRoad = road.trim();
        searchKm = 0;
        runSearch();
    }

    public void searchByDistance(double km, double lat, double lon, String place)
    {
        searchRoad = "";
        searchKm = km;
        searchLat = lat;
        searchLon = lon;
        searchPlace = place;
        runSearch();
    }

    // Also called after each download, so the results stay up to date
    private void runSearch()
    {
        ArrayList<Situation> results = new ArrayList<>();
        ArrayList<Situation> all = new ArrayList<>(incidents.getValue());
        all.addAll(roadworks.getValue());
        for (Situation s : all)
        {
            if (searchKm > 0 ? s.getDistanceKm(searchLat, searchLon) <= searchKm : s.isOnRoad(searchRoad))
            {
                results.add(s);
            }
        }
        if (searchKm > 0)
        {
            // Nearest first
            results.sort((a, b) -> Double.compare(a.getDistanceKm(searchLat, searchLon), b.getDistanceKm(searchLat, searchLon)));
        }
        searchResults.setValue(results);
    }

    // 20.0 -> "20", 2.5 -> "2.5"
    private static String formatKm(double km)
    {
        return km == Math.floor(km) ? String.valueOf((long) km) : String.valueOf(km);
    }

    // What the map shows: set by the list (all/search results) or the detail view (one situation)
    private ArrayList<Situation> mapSituations = new ArrayList<>();
    private String mapTitle = "";

    public ArrayList<Situation> getMapSituations() { return mapSituations; }

    public String getMapTitle() { return mapTitle; }

    public void setMapSituations(ArrayList<Situation> situations, String title)
    {
        mapSituations = new ArrayList<>(situations);
        mapTitle = title;
    }

    // The situation the user tapped in the list (shown in the detail view)
    private final MutableLiveData<Situation> selected = new MutableLiveData<>();

    public LiveData<Situation> getSelected() { return selected; }

    public void select(Situation situation) { selected.setValue(situation); }

    /**
     * Downloads both feeds again (in the background); observers are updated when the results arrive.
     */
    public void refresh()
    {
        status.setValue("Downloading...");
        TrafficFeed.startDownload(updateHandler, getApplication());
    }

} // End of class
