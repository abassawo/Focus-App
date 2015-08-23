package nyc.c4q.rosmaryfc.focus_app;

import android.app.Notification;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AppMonitor extends ActionBarActivity implements AppReceiver.Receiver {

    public static final int ID_FRIENDLY_NOTIFICATION = 1;

    Button save;
    ListView app_list;

    DatabaseHelper databaseHelper;

    List<App> apps;

    AppReceiver appReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_monitor);
        app_list = (ListView) findViewById(R.id.app_list);
        save = (Button) findViewById(R.id.save);

        databaseHelper = DatabaseHelper.getInstance(this);

        try {
            apps = databaseHelper.loadData();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (apps.size() == 0) {

            DBAsyncTask dbAsyncTask = new DBAsyncTask(this);

            PackageManager packageManager = getPackageManager();
            List<ApplicationInfo> applicationInfos = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            Collections.sort(applicationInfos, new ApplicationInfo.DisplayNameComparator(packageManager));

            dbAsyncTask.execute(applicationInfos);
            try {
                apps = dbAsyncTask.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        AppAdapter adapter = new AppAdapter(this, apps);
        app_list.setAdapter(adapter);

        appReceiver = new AppReceiver(new Handler());
        appReceiver.setAppReceiver(this);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), AppService.class);
                startService(intent);
//                Intent intent = new Intent(getApplicationContext(), AppTracker.class);
//                intent.putExtra("receiver", appReceiver);
//                intent.putExtra("start_hour", 12);
//                intent.putExtra("start_minute", 55);
//                intent.putExtra("end_hour", 14);
//                intent.putExtra("end_minute", 57);
//                startService(intent);
                Toast.makeText(getApplicationContext(), "Monitoring Apps", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (resultData != null) {
            switch (resultCode) {
                case AppTracker.STATUS_FINISHED_PREFOCUS:
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                    builder.setSmallIcon(R.drawable.notification_template_icon_bg);
                    builder.setContentTitle("Session Block Reminder");
                    builder.setContentText("You're focus session will begin in 1 minute");
                    Notification notification = builder.build();
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                    notificationManager.notify(ID_FRIENDLY_NOTIFICATION, notification);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "testing", Toast.LENGTH_LONG).show();
                            final Intent showApp = new Intent(getApplicationContext(), AppUsage.class);
                            showApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(showApp);
                        }
                    }, 60000);
                    break;
                case AppTracker.STATUS_FINISHED_STARTFOCUS:
                    boolean isInForeground = resultData.getBoolean("foreground");
                    if (isInForeground) {
                        Intent showApp = new Intent(getApplicationContext(), AppUsage.class);
                        showApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(showApp);
                    } else {
                        Toast.makeText(getApplicationContext(), "Good job focusing!", Toast.LENGTH_LONG).show();
                    }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_app_monitor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
