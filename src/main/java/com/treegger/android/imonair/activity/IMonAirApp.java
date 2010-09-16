package com.treegger.android.imonair.activity;

import org.acra.CrashReportingApplication;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.treegger.android.imonair.service.TreeggerService;

public class IMonAirApp extends CrashReportingApplication
//public class IMonAirApp extends Application
{

    @Override
    public String getFormId() {
        return "dG5iYmJDNVZHcVl3TldiaHhvelFsdGc6MQ";
    }

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
