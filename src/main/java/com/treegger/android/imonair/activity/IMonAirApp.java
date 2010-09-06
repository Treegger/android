package com.treegger.android.imonair.activity;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.treegger.android.imonair.service.TreeggerService;

public class IMonAirApp extends Application
{
    
    private ServiceConnection onService = new ServiceConnection()
    {
        public void onServiceConnected( ComponentName className, IBinder rawBinder )
        {
        }

        public void onServiceDisconnected( ComponentName className )
        {
        }
    };

    
    @Override
    public void onCreate() 
    {
        super.onCreate();
        bindService( new Intent( this, TreeggerService.class ), onService, BIND_AUTO_CREATE );
    }
    @Override
    public void onTerminate()
    {
        super.onTerminate();
        unbindService( onService );
    }
}
