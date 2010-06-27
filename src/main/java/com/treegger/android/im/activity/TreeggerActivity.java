package com.treegger.android.im.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.treegger.android.im.service.TreeggerService;

public abstract class TreeggerActivity extends Activity {

    protected TreeggerService treeggerService = null;

    private ServiceConnection onService = new ServiceConnection()
    {
        public void onServiceConnected( ComponentName className, IBinder rawBinder )
        {
            treeggerService = ( (TreeggerService.LocalBinder) rawBinder ).getService();
            onTreeggerService();
        }

        public void onServiceDisconnected( ComponentName className )
        {
            treeggerService = null;
        }
    };
    
    protected void onTreeggerService()
    {
        
    }
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        bindService( new Intent( this, TreeggerService.class ), onService, BIND_AUTO_CREATE );
    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        unbindService( onService );
    }

}
