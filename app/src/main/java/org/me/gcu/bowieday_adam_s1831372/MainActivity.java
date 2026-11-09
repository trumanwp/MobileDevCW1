/*  Starter project for Mobile Platform Development - 1st diet 26/27
    You should use this project as the starting point for your assignment.
    This project simply reads the data from the required source and displays the
    raw data in a TextField
*/

//
// Name                 Adam Bowie-Day
// Student ID           S1831372
// Programme of Study   _________________
//

// UPDATE THE PACKAGE NAME to include your Student Identifier
package org.me.gcu.bowieday_adam_s1831372;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private TextView rawDataDisplay;
    private Button startButton;
    // This is the activity main thread Handler, receives results from TrafficFeed's worker thread
    private Handler updateUIHandler = null;
    private ArrayList<Situation> incidents = new ArrayList<>();
    private ArrayList<Situation> roadworks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Set up the raw links to the graphical components
        rawDataDisplay = (TextView)findViewById(R.id.rawDataDisplay);
        startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(this);
        createUpdateUiHandler();
    }

    public void onClick(View aview)
    {
        rawDataDisplay.setText("Downloading...");
        TrafficFeed.startDownload(updateUIHandler);
    }

    /* Create Handler object in main thread. */
    private void createUpdateUiHandler()
    {
        if (updateUIHandler == null)
        {
            updateUIHandler = new Handler(Looper.getMainLooper())
            {
                @Override
                @SuppressWarnings("unchecked")
                public void handleMessage(Message msg)
                {
                    // Runs on the main thread, so it's safe to update widgets here
                    if (msg.what == TrafficFeed.MESSAGE_INCIDENTS)
                    {
                        incidents = (ArrayList<Situation>) msg.obj;
                    }
                    else if (msg.what == TrafficFeed.MESSAGE_ROADWORKS)
                    {
                        roadworks = (ArrayList<Situation>) msg.obj;
                    }
                    else if (msg.what == TrafficFeed.MESSAGE_FAILED)
                    {
                        rawDataDisplay.setText("Couldn't download " + msg.obj + " - check the internet connection");
                        return;
                    }
                    updateText();
                }
            };
        }
    }

    // Temporary display of the parsed data until the list fragment is built
    private void updateText()
    {
        StringBuilder text = new StringBuilder();
        text.append("INCIDENTS (").append(incidents.size()).append(")\n\n");
        for (Situation s : incidents)
        {
            text.append(s.getSubType()).append(": ").append(s.getComment()).append("\n\n");
        }
        text.append("ROADWORKS (").append(roadworks.size()).append(")\n\n");
        for (Situation s : roadworks)
        {
            text.append(s.getWorkType()).append(": ").append(s.getComment()).append("\n\n");
        }
        rawDataDisplay.setText(text.toString());
    }

} // End of class
