package org.me.gcu.bowieday_adam_s1831372;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Downloads and parses the two Traffic Scotland feeds on a worker thread.
 * Results go back to the main (UI) thread as Messages sent to a Handler,
 * as in HandlerTestProject1, instead of using runOnUiThread.
 *
 * Every successful download is also saved in the app's internal storage (lecture 8a),
 * so if the feed can't be reached later the last saved data is shown instead.
 * On a fresh install with nothing saved yet, the module's sample data in res/raw is used.
 */
public class TrafficFeed
{
    // Message type codes (msg.what)
    public static final int MESSAGE_INCIDENTS = 1;    // msg.obj = ArrayList<Situation>
    public static final int MESSAGE_ROADWORKS = 2;    // msg.obj = ArrayList<Situation>
    public static final int MESSAGE_FAILED = 3;       // msg.obj = String saying which feed failed

    // Where the data came from (msg.arg1). If not live, msg.getData() has "problem" (and "savedAt" if saved)
    public static final int SOURCE_LIVE = 0;
    public static final int SOURCE_SAVED = 1;
    public static final int SOURCE_SAMPLE = 2;

    private static final String INCIDENTS_URL = "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    private static final String ROADWORKS_URL = "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    // From local.properties via BuildConfig, so they never go into git
    private static final String CLIENT_ID = BuildConfig.DATEX_USERNAME;
    private static final String CLIENT_KEY = BuildConfig.DATEX_PASSWORD;

    /**
     * Starts a worker thread that downloads and parses both feeds.
     * Incidents are sent first as they're small; the roadworks feed is ~1.3MB.
     * @param uiHandler  Handler created on the main thread, receives the results
     * @param context    used for the internal storage folder and the res/raw sample files
     */
    public static void startDownload(final Handler uiHandler, final Context context)
    {
        Thread workerThread = new Thread()
        {
            @Override
            public void run()
            {
                sendResult(uiHandler, context, MESSAGE_INCIDENTS, "incidents", INCIDENTS_URL, R.raw.sample_incidents);
                sendResult(uiHandler, context, MESSAGE_ROADWORKS, "roadworks", ROADWORKS_URL, R.raw.sample_roadworks);
            }
        };
        workerThread.start();
    } // End of startDownload

    // Runs on the worker thread: download (or load the saved copy), parse, then message the UI thread
    private static void sendResult(Handler uiHandler, Context context, int what, String feedName,
                                   String feedUrl, int sampleResource)
    {
        Message message = new Message();
        message.what = what;
        File savedFile = new File(context.getFilesDir(), feedName + ".xml");
        String xml;
        try
        {
            xml = download(feedUrl);
            saveToFile(savedFile, xml); // keep a copy for when we're offline
            message.arg1 = SOURCE_LIVE;
        }
        catch (IOException e)
        {
            Log.e("TrafficFeed", "Download of " + feedName + " failed: " + e);
            // Tell the user what went wrong in plain words (HCI lecture: "Page not found", not "HTTP404")
            String problem = (e.getMessage() != null && e.getMessage().startsWith("HTTP"))
                    ? "Traffic Scotland isn't responding" : "no internet connection";
            message.getData().putString("problem", problem);
            xml = readFromFile(savedFile);
            if (xml != null)
            {
                message.arg1 = SOURCE_SAVED;
                message.getData().putLong("savedAt", savedFile.lastModified());
            }
            else
            {
                xml = readFromStream(context.getResources().openRawResource(sampleResource));
                message.arg1 = SOURCE_SAMPLE;
            }
        }

        if (xml == null)
        {
            message.what = MESSAGE_FAILED;
            message.obj = feedName;
        }
        else
        {
            message.obj = TrafficParser.parseData(xml);
        }
        uiHandler.sendMessage(message);
    }

    /**
     * Downloads one feed.
     * @param feedUrl  the feed address
     * @return the xml as a String
     * @throws IOException if it couldn't be downloaded (no internet, or "HTTP ..." if the server had a problem)
     */
    private static String download(String feedUrl) throws IOException
    {
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
                throw new IOException("HTTP " + code + " from " + feedUrl);
            }
            String result = readFromStream(urlConnection.getInputStream());

            //Clean up any leading garbage characters
            int i = (result == null) ? -1 : result.indexOf("<?"); //initial tag
            if (i < 0)
            {
                throw new IOException("HTTP response had no xml: " + feedUrl);
            }
            return result.substring(i);
        }
        finally
        {
            if (urlConnection != null)
            {
                urlConnection.disconnect();
            }
        }
    } // End of download

    // Internal storage, as in lecture 8a: FileOutputStream.write(string.getBytes())
    private static void saveToFile(File f, String xml)
    {
        try
        {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(xml.getBytes(StandardCharsets.UTF_8));
            fos.close();
        }
        catch (IOException e)
        {
            Log.e("TrafficFeed", "Couldn't save " + f.getName() + ": " + e);
        }
    }

    // Returns null if there's no saved copy yet
    private static String readFromFile(File f)
    {
        if (!f.exists())
        {
            return null;
        }
        try
        {
            return readFromStream(new FileInputStream(f));
        }
        catch (IOException e)
        {
            Log.e("TrafficFeed", "Couldn't read " + f.getName() + ": " + e);
            return null;
        }
    }

    // Reads a whole stream (download, saved file or res/raw file) into a String
    private static String readFromStream(InputStream stream)
    {
        StringBuilder result = new StringBuilder(); // much faster than String + String for the 1.3MB roadworks feed
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
            {
                result.append(inputLine).append('\n');
            }
            in.close();
        }
        catch (IOException e)
        {
            Log.e("TrafficFeed", "Read failed: " + e);
            return null;
        }
        return result.toString();
    }

} // End of class
