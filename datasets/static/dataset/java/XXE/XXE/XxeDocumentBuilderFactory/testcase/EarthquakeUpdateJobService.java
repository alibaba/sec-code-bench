<filename>Earthquake_ch19_Part2/Earthquake/app/src/main/java/com/professionalandroid/apps/earthquake/EarthquakeUpdateJobService.java<fim_prefix>

package com.professionalandroid.apps.earthquake;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.SimpleJobService;
import com.firebase.jobdispatcher.Trigger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class EarthquakeUpdateJobService extends SimpleJobService {
    private static final String TAG = "EarthquakeUpdateJob ";
    private static final String UPDATE_JOB_TAG = "update_job";
    private static final String PERIODIC_JOB_TAG = "periodic_job";

    private static final String NOTIFICATION_CHANNEL = "earthquake";
    public static final int NOTIFICATION_ID = 1;


    public static void scheduleUpdateJob(Context context) {
        FirebaseJobDispatcher jobDispatcher =
                new FirebaseJobDispatcher(new GooglePlayDriver(context));

        jobDispatcher.schedule(jobDispatcher.newJobBuilder()
                .setTag(UPDATE_JOB_TAG)
                .setService(EarthquakeUpdateJobService.class)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build());
    }

    @Override
    public int onRunJob(final JobParameters job) {

        // Result ArrayList of parsed earthquakes.
        ArrayList<Earthquake> earthquakes = new ArrayList<>();

        // Get the XML
        URL url;
        try {
            String quakeFeed = getApplication().getString(R.string.earthquake_feed);

            url = new URL(quakeFeed);

            URLConnection connection;
            connection = url.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) connection;

            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = httpConnection.getInputStream();
                DocumentBuilderFactory documentBuilderFactory =
                        DocumentBuilderFactory.newInstance();
                <fim_suffix>
                // Parse the earthquake feed.
                Document dom = db.parse(in);
                Element docEle = dom.getDocumentElement();

                // Get a list of each earthquake entry.
                NodeList nl = docEle.getElementsByTagName("entry");
                if (nl != null && nl.getLength() > 0) {
                    for (int i = 0; i < nl.getLength(); i++) {
                        Element entry =
                                (Element) nl.item(i);
                        Element id =
                                (Element) entry.getElementsByTagName("id").item(0);
                        Element title =
                                (Element) entry.getElementsByTagName("title").item(0);
                        Element g =
                                (Element) entry.getElementsByTagName("georss:point")
                                        .item(0);
                        Element when =
                                (Element) entry.getElementsByTagName("updated").item(0);
                        Element link =
                                (Element) entry.getElementsByTagName("link").item(0);

                        String idString = id.getFirstChild().getNodeValue();
                        String details = title.getFirstChild().getNodeValue();
                        String hostname = "http://earthquake.usgs.gov";
                        String linkString = hostname + link.getAttribute("href");
                        String point = g.getFirstChild().getNodeValue();
                        String dt = when.getFirstChild().getNodeValue();

                        SimpleDateFormat sdf =
                                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");
                        Date qdate = new GregorianCalendar(0, 0, 0).getTime();

                        try {
                            qdate = sdf.parse(dt);
                        } catch (ParseException e) {
                            Log.e(TAG, "Date parsing exception.", e);
                        }

                        String[] location = point.split(" ");
                        Location l = new Location("dummyGPS");
                        l.setLatitude(Double.parseDouble(location[0]));
                        l.setLongitude(Double.parseDouble(location[1]));

                        String magnitudeString = details.split(" ")[1];
                        int end = magnitudeString.length() - 1;
                        double magnitude =
                                Double.parseDouble(magnitudeString.substring(0, end));

                        if (details.contains("-"))
                            details = details.split("-")[1].trim();
                        else
                            details = "";

                        final Earthquake earthquake = new Earthquake(idString,
                                qdate,
                                details, l,
                                magnitude,
                                linkString);

                        // Add the new earthquake to our result array.
                        earthquakes.add(earthquake);
                    }
                }
            }
            httpConnection.disconnect();

            if (job.getTag().equals(PERIODIC_JOB_TAG)) {
                Earthquake largestNewEarthquake = findLargestNewEarthquake(earthquakes);

                SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(this);

                int minimumMagnitude = Integer.parseInt(
                        prefs.getString(PreferencesActivity.PREF_MIN_MAG, "3"));

                if (largestNewEarthquake != null
                        && largestNewEarthquake.getMagnitude() >= minimumMagnitude) {
                    // Trigger a Notification
                    broadcastNotification(largestNewEarthquake);
                }
            }

            // Insert the newly parsed array of Earthquakes
            EarthquakeDatabaseAccessor
                    .getInstance(getApplication())
                    .earthquakeDAO()
                    .insertEarthquakes(earthquakes);

            // Update the Earthquake Widget
            Intent newEarthquake = new Intent(this, EarthquakeWidget.class);
            newEarthquake.setAction(EarthquakeWidget.NEW_QUAKE_BROADCAST);
            sendBroadcast(newEarthquake);

            // Update the Earthquake List Widget
            Intent newListEarthquake = new Intent(this, EarthquakeListWidget.class);
            newListEarthquake.setAction(EarthquakeWidget.NEW_QUAKE_BROADCAST);
            sendBroadcast(newListEarthquake);

            scheduleNextUpdate(this, job);

            return RESULT_SUCCESS;
        } catch (MalformedURLException e) {
            Log.e(TAG, "Malformed URL Exception", e);
            return RESULT_FAIL_NORETRY;
        } catch (IOException e) {
            Log.e(TAG, "IO Exception", e);
            return RESULT_FAIL_RETRY;
        } catch (ParserConfigurationException e) {
            Log.e(TAG, "Parser Configuration Exception", e);
            return RESULT_FAIL_NORETRY;
        } catch (SAXException e) {
            Log.e(TAG, "SAX Exception", e);
            return RESULT_FAIL_NORETRY;
        }
    }

    private void scheduleNextUpdate(Context context, JobParameters job) {
        if (job.getTag().equals(UPDATE_JOB_TAG)) {
            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(this);

            int updateFreq = Integer.parseInt(
                    prefs.getString(PreferencesActivity.PREF_UPDATE_FREQ, "60"));

            boolean autoUpdateChecked =
                    prefs.getBoolean(PreferencesActivity.PREF_AUTO_UPDATE, false);

            if (autoUpdateChecked) {
                FirebaseJobDispatcher jobDispatcher =
                        new FirebaseJobDispatcher(new GooglePlayDriver(context));

                jobDispatcher.schedule(jobDispatcher.newJobBuilder()
                        .setTag(PERIODIC_JOB_TAG)
                        .setService(EarthquakeUpdateJobService.class)
                        .setConstraints(Constraint.ON_ANY_NETWORK)
                        .setReplaceCurrent(true)
                        .setRecurring(true)
                        .setTrigger(Trigger.executionWindow(
                                updateFreq*60 / 2,
                                updateFreq*60))
                        .setLifetime(Lifetime.FOREVER)
                        .build());
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.earthquake_channel_name);
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL,
                    name,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.enableLights(true);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void broadcastNotification(Earthquake earthquake) {
        createNotificationChannel();

        Intent startActivityIntent = new Intent(this, EarthquakeMainActivity.class);
        PendingIntent launchIntent = PendingIntent.getActivity(this, 0,
                startActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder earthquakeNotificationBuilder
                = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL);

        earthquakeNotificationBuilder
                .setSmallIcon(R.drawable.notification_icon)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(launchIntent)
                .setAutoCancel(true)
                .setShowWhen(true);

        earthquakeNotificationBuilder
                .setWhen(earthquake.getDate().getTime())
                .setContentTitle("M:" + earthquake.getMagnitude())
                .setContentText(earthquake.getDetails())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(earthquake.getDetails()));

        NotificationManagerCompat notificationManager
                = NotificationManagerCompat.from(this);

        notificationManager.notify(NOTIFICATION_ID,
                earthquakeNotificationBuilder.build());
    }

    private Earthquake findLargestNewEarthquake(List<Earthquake> newEarthquakes) {
        List<Earthquake> earthquakes = EarthquakeDatabaseAccessor
                .getInstance(getApplicationContext())
                .earthquakeDAO()
                .loadAllEarthquakesBlocking();

        Earthquake largestNewEarthquake = null;

        for (Earthquake earthquake : newEarthquakes) {
            if (earthquakes.contains(earthquake)) {
                continue;
            }

            if (largestNewEarthquake == null
                    || earthquake.getMagnitude() > largestNewEarthquake.getMagnitude()) {
                largestNewEarthquake = earthquake;
            }
        }
        return largestNewEarthquake;
    }
}
<fim_middle>