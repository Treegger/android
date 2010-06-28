package com.treegger.android.im.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.treegger.protobuf.WebSocketProto.Roster;
import com.treegger.protobuf.WebSocketProto.RosterItem;
import com.treegger.protobuf.WebSocketProto.TextMessage;

public class TreeggerService
    extends Service
{
    public static final String TAG = "TreeggerService";
    

    public static final String BROADCAST_ACTION = "TreeggerServiceBroadcast";
    public static final String MESSAGE_TYPE_EXTRA = "messageType";
    public static final int     MESSAGE_TYPE_ROSTER_UPDATE = 1;
    public static final int     MESSAGE_TYPE_TEXTMESSAGE_UPDATE = 2;
   
    private final Binder binder = new LocalBinder();
    private AccountStorage accountStorage;

    private Map<Account,WebSocketManager> connectionMap = new HashMap<Account, WebSocketManager>();

    private Map<Account,Roster> rosterMap = Collections.synchronizedMap( new HashMap<Account, Roster>() );
 
    public Map<Account,Roster> getRosters()
    {
        return rosterMap;
    }
    public void addRoster( Account account, Roster roster )
    {
        rosterMap.put( account, roster );
        broadcast( MESSAGE_TYPE_ROSTER_UPDATE );
    }
    public void removeRoster( Account account )
    {
        rosterMap.remove( account );
        broadcast( MESSAGE_TYPE_ROSTER_UPDATE );
    }
    
    
    private Map<String,List<String>> textMessageMap = Collections.synchronizedMap( new HashMap<String,List<String>>() );
    
    public List<String> getTextMessageList( String jid )
    {
        return textMessageMap.get( jid );
    }
    
    public void addTextMessage( String jid, String message )
    {
        List<String> messagesList = textMessageMap.get( jid );
        if( messagesList == null )
        {
            messagesList = new ArrayList<String>();
            textMessageMap.put( jid, messagesList );
        }
        messagesList.add( message );
        broadcast( MESSAGE_TYPE_TEXTMESSAGE_UPDATE );

    }
    public void addTextMessage( Account account, TextMessage textMessage )
    {
        String from = textMessage.getFromUser();
        int i = from.indexOf( '/' );
        if( i > 0 ) from = from.substring( 0, i );
        RosterItem rosterItem = getRosterItemByJID( from );
        if( rosterItem != null ) addTextMessage( from, rosterItem.getName()+": " + textMessage.getBody() );
    }

    
    final private void broadcast( final int type )
    {
        Intent broadCastIntent = new Intent( BROADCAST_ACTION );
        broadCastIntent.putExtra( MESSAGE_TYPE_EXTRA, type );
        sendBroadcast( broadCastIntent );
    }
    
    
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
            connectionMap.put( account, new WebSocketManager( this, account ) );
        }
    }


    @Override
    public void onDestroy()
    {
        Toast.makeText( this, "Stopped", Toast.LENGTH_SHORT ).show();
        for( WebSocketManager webSocketManager : connectionMap.values() )
        {
            webSocketManager.disconnect();
        }
        connectionMap.clear();
    }

 
    private Account getAccountByJID( String jid )
    {
        for( Map.Entry<Account, Roster> entry : getRosters().entrySet() )
        {
            for( RosterItem rosterItem : entry.getValue().getItemList() )
            {
                if( rosterItem.getJid().equals( jid ) )
                {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private RosterItem getRosterItemByJID( String jid )
    {
        for( Map.Entry<Account, Roster> entry : getRosters().entrySet() )
        {
            for( RosterItem rosterItem : entry.getValue().getItemList() )
            {
                if( rosterItem.getJid().equals( jid ) )
                {
                    return rosterItem;
                }
            }
        }
        return null;
    }
    
    public void sendPresence( String type, String show, String status )
    {
        for( WebSocketManager webSocketManager : connectionMap.values() )
        {
            webSocketManager.sendPresence( type, show, status );    
        }
    }
    public void sendTextMessage( String jid, String text )
    {
        
        Account account = getAccountByJID( jid );
        if( account != null )
        {
            WebSocketManager webSocketManager = connectionMap.get( account );
            webSocketManager.sendMessage( jid, text );
            addTextMessage( jid, "You: "+ text );
        }
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
        connectionMap.put( account, new WebSocketManager( this, account ) );
    }

    public void updateAccount( Account account )
    {
        accountStorage.updateAccount( account );
        WebSocketManager webSocketManager = connectionMap.remove( account );
        webSocketManager.disconnect();
        connectionMap.put( account, new WebSocketManager( this, account ) );
    }
    
    public void removeAccount( Account account )
    {
        accountStorage.removeAccount( account );
        WebSocketManager webSocketManager = connectionMap.remove( account );
        webSocketManager.disconnect();
    }
    
    public List<Account> getAccounts()
    {
        return accountStorage.getAccounts();
    }
}
