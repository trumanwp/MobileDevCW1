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

/**
 * The single Activity: shows the status line and refresh button,
 * and hosts the fragments in fragmentContainer.
 */
public class MainActivity extends AppCompatActivity implements OnClickListener, SituationListFragment.SituationListListener {
    private TextView statusText;
    private Button startButton;
    private TrafficViewModel viewModel; // Holds the data, survives screen rotation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        int margin = (int) (8 * getResources().getDisplayMetrics().density); // 8dp gap from the screen edges
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left + margin, systemBars.top, systemBars.right + margin, systemBars.bottom);
            return insets;
        });
        // Set up the raw links to the graphical components
        statusText = (TextView)findViewById(R.id.statusText);
        startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(this);

        //ViewModelProvider creates the ViewModel the first time, and reconnects to the same one after a rotation
        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);
        viewModel.getStatus().observe(this, statusText::setText);

        // Only add the fragment the first time: after a rotation Android restores it by itself
        if (savedInstanceState == null)
        {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new SituationListFragment())
                    .commit();
        }
    }

    public void onClick(View aview)
    {
        viewModel.refresh();
    }

    // A situation was tapped in the list: show its details (Back returns to the list)
    @Override
    public void onSituationSelected()
    {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new SituationDetailFragment())
                .addToBackStack(null)
                .commit();
    }

} // End of class
