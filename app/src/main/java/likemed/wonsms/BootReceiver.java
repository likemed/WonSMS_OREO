package likemed.wonsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent foregroundIntent = new Intent(context, SmsService.class);
        foregroundIntent.putExtra(SmsService.EXTRA_ACTION, SmsService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(foregroundIntent);
        } else {
            context.startService(foregroundIntent);
        }
    }
}
