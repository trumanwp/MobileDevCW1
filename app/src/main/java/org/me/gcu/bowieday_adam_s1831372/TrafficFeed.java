package org.me.gcu.bowieday_adam_s1831372;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;

/**
 * Downloads and parses the two Traffic Scotland feeds on a worker thread.
 * Results go back to the main (UI) thread as Messages sent to a Handler,
 * as in HandlerTestProject1, instead of using runOnUiThread.
 */
public class TrafficFeed
{
    // Message type codes (msg.what)
    public static final int MESSAGE_INCIDENTS = 1;    // msg.obj = ArrayList<Situation>
    public static final int MESSAGE_ROADWORKS = 2;    // msg.obj = ArrayList<Situation>
    public static final int MESSAGE_FAILED = 3;       // msg.obj = String saying which feed failed

    private static final String INCIDENTS_URL = "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    private static final String ROADWORKS_URL = "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    // From local.properties via BuildConfig, so they never go into git
    private static final String CLIENT_ID = BuildConfig.DATEX_USERNAME;
    private static final String CLIENT_KEY = BuildConfig.DATEX_PASSWORD;

    /**
     * Starts a worker thread that downloads and parses both feeds.
     * Incidents are sent first as they're small; the roadworks feed is ~1.3MB.
     * @param uiHandler  Handler created on the main thread, receives the results
     */
    public static void startDownload(final Handler uiHandler)
    {
        Thread workerThread = new Thread()
        {
            @Override
            public void run()
            {
                sendResult(uiHandler, MESSAGE_INCIDENTS, "incidents", INCIDENTS_URL);
                sendResult(uiHandler, MESSAGE_ROADWORKS, "roadworks", ROADWORKS_URL);
            }
        };
        workerThread.start();
    } // End of startDownload

    // Runs on the worker thread: download, parse, then message the UI thread
    private static void sendResult(Handler uiHandler, int what, String feedName, String feedUrl)
    {
        Message message = new Message();
        String xml = download(feedUrl);
        if (xml == null)
        {
            message.what = MESSAGE_FAILED;
            message.obj = feedName;
        }
        else
        {
            message.what = what;
            message.obj = TrafficParser.parseData(xml);
        }
        uiHandler.sendMessage(message);
    }

    /**
     * Downloads one feed.
     * @param feedUrl  the feed address
     * @return the xml as a String, or null if it couldn't be downloaded (i.e. no internet)
     */
    private static String download(String feedUrl)
    {
        StringBuilder result = new StringBuilder(); // much faster than String + String for the 1.3MB roadworks feed
        HttpURLConnection urlConnection = null;
        try
        {
            URL url = new URL(feedUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(15000);
            urlConnection.setReadTimeout(30000);
            //ADD AUTHORISATION (string with clientId:clientKey)
            urlConnection.setRequestProperty("Authorization",
                    Base64.getEncoder().encodeToString((CLIENT_ID + ":" + CLIENT_KEY).getBytes()));

            int code = urlConnection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK)
            {
                Log.e("TrafficFeed", "HTTP " + code + " from " + feedUrl);
                return null;
            }

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
            {
                result.append(inputLine).append('\n');
            }
            in.close();
        }
        catch (IOException e)
        {
            Log.e("TrafficFeed", "Download failed: " + e);
            return null;
        }
        finally
        {
            if (urlConnection != null)
            {
                urlConnection.disconnect();
            }
        }

        //Clean up any leading garbage characters
        int i = result.indexOf("<?"); //initial tag
        if (i < 0)
        {
            Log.e("TrafficFeed", "No xml in response from " + feedUrl);
            return null;
        }
        return result.substring(i);
    } // End of download

} // End of class
