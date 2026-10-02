package ar.axt.servicios;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import ar.axt.animar.UndoRodoGuardado;

public class CleanupService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return 2;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) { stopSelf(); }

}
