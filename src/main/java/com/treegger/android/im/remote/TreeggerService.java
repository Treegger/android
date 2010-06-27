package com.treegger.android.im.remote;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.treegger.android.im.Account;
import com.treegger.android.im.AccountManager;
import com.treegger.protobuf.WebSocketProto.WebSocketMessage;

public class TreeggerService
    extends Service
{
    public static final String TAG = "TreeggerService";
    
    
    private final Binder binder = new LocalBinder();
    
   

    public ConcurrentLinkedQueue<WebSocketMessage> messagesQueue = new ConcurrentLinkedQueue<WebSocketMessage>(); 
    
    public class LocalBinder extends Binder {
        public TreeggerService getService() 
        {
            return TreeggerService.this;
        }
    }
    @Override
    public IBinder onBind( Intent intent )
    {
        return binder;
    }
    
    
    
    
    @Override
    public void onCreate()
    {
        super.onCreate();

        AccountManager accountManager = new AccountManager( this );
        List<Account> accountList = accountManager.getAccounts();
        for( Account account : accountList )
        {
            new WebSocketManager( this, account );
        }
    }


    @Override
    public void onDestroy()
    {
        Toast.makeText( this, "Stopped", Toast.LENGTH_SHORT ).show();
    }

 
    public static class DisplayToastRunnable implements Runnable
    {
        private Context context;
        private String message;
        public DisplayToastRunnable( Context context, String message )
        {
            this.context = context;
            this.message = message;
        }
        public void run() 
        {
            Toast.makeText( context, message, Toast.LENGTH_SHORT ).show();
        }
    }

}
