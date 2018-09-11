package likemed.wonsms;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsMessage;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SmsService extends Service {

    private static final int NOTIFICATION_REQUEST_CODE = 0;
    private static final String CHANNEL_ID = "test_channel";
    private static final String CHANNEL_NAME = "WonSMS";
    private static final int NOTIFICATION_ID = 1;
    public static final String EXTRA_ACTION = "extra_action";
    public static final String ACTION_START = "action_start";
    public static final String ACTION_STOP = "action_stop";
    private int phoneReceiveCount = 0;
    BroadcastReceiver phoneReceiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");

        this.phoneReceiver = new BroadcastReceiver() {
            public SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            @Override
            public void onReceive(Context context, Intent intent) {
                //update the count and show it in the notification body
                //used only to see if the receiver works
                String action = intent.getAction();
                if (action != null && action.equals("android.provider.Telephony.SMS_RECEIVED")) {
                    phoneReceiveCount++;

                    SmsMessage[] messages = parseSmsMessage(intent.getExtras());
                    if (messages != null && messages.length > 0) {
                        String sender = messages[0].getOriginatingAddress();
                        String contents = messages[0].getMessageBody().toString();
                        Date receivedDate = new Date(messages[0].getTimestampMillis());

                        Toast.makeText(context,"test",Toast.LENGTH_SHORT).show();
                        sendToActivity(context, sender, contents, receivedDate);
                    }
                    createNotification(context);
                }
            }

            private SmsMessage[] parseSmsMessage(Bundle bundle) {
                Object[] objs = (Object[]) bundle.get("pdus");
                SmsMessage[] messages = new SmsMessage[objs.length];
                int smsCount = objs.length;
                for (int i = 0; i < smsCount; i++) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        messages[i] = SmsMessage.createFromPdu((byte[]) objs[i], bundle.getString("format"));
                    } else {
                        messages[i] = SmsMessage.createFromPdu((byte[]) objs[i]);
                    }
                }
                return messages;
            }

            private void sendToActivity(Context context, String sender, String contents, Date receivedDate) {
                // 메시지를 보여줄 액티비티를 띄워줍니다.
                Intent myIntent = new Intent(context, SMSActivity.class);

                // 플래그를 이용합니다.
                myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);

                myIntent.putExtra("sender", sender);
                myIntent.putExtra("contents", contents);
                myIntent.putExtra("receivedDate", format.format(receivedDate));

                context.startActivity(myIntent);
            }


        };
        registerReceiver(phoneReceiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getStringExtra(EXTRA_ACTION);
        if (action.equals(ACTION_START)) {
            createNotification(this);
        } else if (action.equals(ACTION_STOP)) {
            stopSelf();
        }
        //also tried with START_STICKY
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(phoneReceiver);
    }

    private void createNotification(Context context) {
        //intent to open app
        Intent entryActivityIntent = new Intent(context, MainActivity.class);
        entryActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingEntryActivityIntent = PendingIntent.getActivity(context, NOTIFICATION_REQUEST_CODE, entryActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //build notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableVibration(false);
            channel.enableLights(false);
            notificationManager.createNotificationChannel(channel);
        }
        String contentText = "phoneReceiveCount: " + String.valueOf(phoneReceiveCount);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                        .setSmallIcon(android.R.drawable.star_on)
                        .setContentTitle("This is the title")
                        .setContentText(contentText)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                        .setContentIntent(pendingEntryActivityIntent)
                        .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        } else {
            startForeground(NOTIFICATION_ID, notificationBuilder.build());
        }
    }
}
