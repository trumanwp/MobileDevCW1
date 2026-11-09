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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private TextView rawDataDisplay;
    private Button startButton;
    private TrafficViewModel viewModel; // Holds the data, survives screen rotation

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

        //ViewModelProvider creates the ViewModel the first time, and reconnects to the same one after a rotation
        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);
        //Observe the LiveData: the text is updated whenever new data arrives (and straight away after a rotation)
        viewModel.getIncidents().observe(this, list -> updateText());
        viewModel.getRoadworks().observe(this, list -> updateText());
        viewModel.getStatus().observe(this, text -> updateText());
    }

    public void onClick(View aview)
    {
        viewModel.refresh();
    }

    // Temporary display of the parsed data until the list fragment is built
    private void updateText()
    {
        StringBuilder text = new StringBuilder();
        text.append(viewModel.getStatus().getValue()).append("\n\n");
        text.append("INCIDENTS (").append(viewModel.getIncidents().getValue().size()).append(")\n\n");
        for (Situation s : viewModel.getIncidents().getValue())
        {
            text.append(s.getSubType()).append(": ").append(s.getComment()).append("\n\n");
        }
        text.append("ROADWORKS (").append(viewModel.getRoadworks().getValue().size()).append(")\n\n");
        for (Situation s : viewModel.getRoadworks().getValue())
        {
            text.append(s.getWorkType()).append(": ").append(s.getComment()).append("\n\n");
        }
        rawDataDisplay.setText(text.toString());
    }

} // End of class
