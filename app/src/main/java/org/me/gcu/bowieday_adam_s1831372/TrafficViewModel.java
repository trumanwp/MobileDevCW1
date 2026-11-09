package org.me.gcu.bowieday_adam_s1831372;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * The ViewModel survives screen rotation, so the downloaded situations are kept
 * while the Activity/Fragments are re-created (lecture 7c).
 * It's the middleman between the View (Activity/Fragments) and the Model (TrafficFeed, TrafficParser):
 * it asks TrafficFeed for data and exposes the results as observable LiveData.
 */
public class TrafficViewModel extends ViewModel
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
            if (msg.what == TrafficFeed.MESSAGE_INCIDENTS)
            {
                incidents.setValue((ArrayList<Situation>) msg.obj);
                status.setValue("Last updated " + time);
            }
            else if (msg.what == TrafficFeed.MESSAGE_ROADWORKS)
            {
                roadworks.setValue((ArrayList<Situation>) msg.obj);
                status.setValue("Last updated " + time);
            }
            else if (msg.what == TrafficFeed.MESSAGE_FAILED)
            {
                status.setValue("Couldn't download " + msg.obj + " at " + time + " - check the internet connection");
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

    public TrafficViewModel()
    {
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

    // Which feed the list shows, kept here so it survives rotation
    private boolean showingRoadworks = false;

    public boolean isShowingRoadworks() { return showingRoadworks; }

    public void setShowingRoadworks(boolean showingRoadworks) { this.showingRoadworks = showingRoadworks; }

    /**
     * Downloads both feeds again (in the background); observers are updated when the results arrive.
     */
    public void refresh()
    {
        status.setValue("Downloading...");
        TrafficFeed.startDownload(updateHandler);
    }

} // End of class
