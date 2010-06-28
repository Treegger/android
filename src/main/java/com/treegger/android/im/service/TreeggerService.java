package com.treegger.android.im.service;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.treegger.protobuf.WebSocketProto.WebSocketMessage;

public class TreeggerService
    extends Service
{
    public static final String TAG = "TreeggerService";
    
    
    private final Binder binder = new LocalBinder();
    private AccountStorage accountStorage;
   

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
        accountStorage = new AccountStorage( this );
        for( Account account : getAccounts() )
        {
            new WebSocketManager( this, account );
        }
    }


    @Override
    public void onDestroy()
    {
        Toast.makeText( this, "Stopped", Toast.LENGTH_SHORT ).show();
    }

 
    public Account findAccountById( Long id )
    {
        if( id == null ) return null;
        for( Account account : accountStorage.getAccounts() )
        {
            if( id.longValue() == account.id.longValue() ) return account;
        }
        return null;
    }
    public void addAccount( Account account )
    {
        accountStorage.addAccount( account );
    }

    public void updateAccount( Account account )
    {
        accountStorage.updateAccount( account );
    }
    
    public void removeAccount( Account account )
    {
        accountStorage.removeAccount( account );
    }
    
    public List<Account> getAccounts()
    {
        return accountStorage.getAccounts();
    }
}
