package com.treegger.android.imonair.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.treegger.android.imonair.R;
import com.treegger.android.imonair.activity.Chat;
import com.treegger.protobuf.WebSocketProto.Presence;
import com.treegger.protobuf.WebSocketProto.Roster;
import com.treegger.protobuf.WebSocketProto.RosterItem;
import com.treegger.protobuf.WebSocketProto.TextMessage;

public class TreeggerService
    extends Service
{
    public static final String TAG = "TreeggerService";
    

    public static final String TREEGGER_BROADCAST_ACTION = "TreeggerServiceBroadcast";
    public static final String EXTRA_MESSAGE_TYPE = "messageType";
    
    
    public static final int     MESSAGE_TYPE_ROSTER_UPDATE = 1;
    public static final int     MESSAGE_TYPE_TEXTMESSAGE_UPDATE = 2;
    public static final int     MESSAGE_TYPE_PRESENCE_UPDATE = 3;
   
    public static final int     MESSAGE_TYPE_CONNECTING = 4;
    public static final int     MESSAGE_TYPE_CONNECTING_FINISHED = 5;
    public static final int     MESSAGE_TYPE_AUTHENTICATING = 6;
    public static final int     MESSAGE_TYPE_AUTHENTICATING_FINISHED = 7;
    
    private static final int MAX_MESSAGESLIST_SIZE = 100;
    
    private final Binder binder = new LocalBinder();
    protected Handler handler; 
    
    private AccountStorage accountStorage;

    private Map<Account,WebSocketManager> connectionMap = new HashMap<Account, WebSocketManager>();


    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        private final static int NO_NETWORK = -2; 
        private final static int UNDEFINED = -1; 
        private int prevNetworkType = UNDEFINED;
        public void onReceive( Context context, Intent intent )
        {
            if( ConnectivityManager.CONNECTIVITY_ACTION.equals( intent.getAction() ) ) 
            {
                boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false );

                boolean isFailover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
                NetworkInfo networkInfo = (NetworkInfo)intent.getParcelableExtra( ConnectivityManager.EXTRA_NETWORK_INFO );
                int networkType = networkInfo.getType();
                
                boolean networkChanged = false; 
                if( networkType != prevNetworkType )
                {
                    if( prevNetworkType != UNDEFINED ) networkChanged = true;
                    prevNetworkType = networkType;
                }
                //String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
                
                if( noConnectivity )
                {
                    disconnect();
                    prevNetworkType = NO_NETWORK;
                } 
                else 
                {
                    if( isFailover || networkChanged )
                    {
                        disconnect();
                        connect();
                    }
                }
                
                

            }
        }
    };
    
    
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
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
    
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    private String getUserAndHostFromJID( final String jid )
    {
        int i = jid.indexOf( '/' );
        if( i > 0 ) return jid.substring( 0, i );
        return jid;
    }
    
    
    private Map<String,List<String>> textMessageMap = Collections.synchronizedMap( new HashMap<String,List<String>>() );
    private Set<String> unconsumedMessageFroms = Collections.synchronizedSet( new HashSet<String>() ); 
    
    public List<String> getTextMessageList( String from )
    {
        return textMessageMap.get( from );
    }
    
    public boolean hasMessageFrom( String from )
    {
        return unconsumedMessageFroms.contains( from );
    }
    public void markHasReadMessageFrom( String from )
    {
        unconsumedMessageFroms.remove( from );
    }
    
    private String unXML( String s )
    {
        return s.replace( "&apos;", "'" ).replace( "&quot;", "\"" ).replace( "&gt;", ">" ).replace( "&lt;", "<" );
    }
    private void addTextMessage( String from, String message, boolean localMessage )
    {
        if( message != null && message.length() > 0 )
        {
            List<String> messagesList = textMessageMap.get( from );
            if( messagesList == null )
            {
                messagesList = new ArrayList<String>();
                textMessageMap.put( from, messagesList );
            }
            else
            {
                if( messagesList.size() > MAX_MESSAGESLIST_SIZE )
                {
                    messagesList.remove( 0 );
                }
            }
            messagesList.add( unXML( message ) );
            if( !localMessage )
            {
                unconsumedMessageFroms.add( from );
                messageNotification( from, message );
            }
            broadcast( MESSAGE_TYPE_TEXTMESSAGE_UPDATE );
        }
    }
    public void addTextMessage( Account account, TextMessage textMessage )
    {
        String from = getUserAndHostFromJID( textMessage.getFromUser() );
        RosterItem rosterItem = findRosterItemByJID( from );
        if( rosterItem != null &&  textMessage.hasBody() )
            addTextMessage( from, rosterItem.getName()+": " + textMessage.getBody(), false );
    }


    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    private Map<String,Presence> presenceMap = Collections.synchronizedMap( new HashMap<String,Presence>() ); 
    
    public void addPresence( Account account, Presence presence )
    {
        String from = getUserAndHostFromJID( presence.getFrom() );
        
        String presenceType = presence.getType();
        if( presenceType != null && presenceType.equalsIgnoreCase( "unavailable" ) ) presenceMap.remove( from );
        else presenceMap.put( from, presence );
        broadcast( MESSAGE_TYPE_PRESENCE_UPDATE );

    }
    public Presence getPresence( String jid )
    {
        return presenceMap.get( jid );
    }
    
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    final private void broadcast( final int type )
    {
        Intent broadCastIntent = new Intent( TREEGGER_BROADCAST_ACTION );
        broadCastIntent.putExtra( EXTRA_MESSAGE_TYPE, type );
        sendBroadcast( broadCastIntent );
    }
    

    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
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
    
    
    
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    @Override
    public void onCreate()
    {
        super.onCreate();
        
        registerReceiver( receiver, new IntentFilter( ConnectivityManager.CONNECTIVITY_ACTION ) );

        handler = new Handler();
        accountStorage = new AccountStorage( this );
        connect();
    }
    private void connect()
    {
        for( Account account : getAccounts() )
        {
            WebSocketManager webSocketManager = connectionMap.get( account );
            if( webSocketManager != null  )
            {
                webSocketManager.connect();
            }
            else
            {
                connectionMap.put( account, new WebSocketManager( this, account ) );
            }
        }        
    }


    @Override
    public void onDestroy()
    {
        sendPresence( "unavailable", "", "" );
        unregisterReceiver( receiver );
        disconnect();
    }

    private void disconnect()
    {
        for( WebSocketManager webSocketManager : connectionMap.values() )
        {
            webSocketManager.disconnect();
        }
        //connectionMap.clear();
    }
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    private Account findAccountByJID( String jid )
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

    private RosterItem findRosterItemByJID( String jid )
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
    
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    public void sendPresence( String type, String show, String status )
    {
        for( WebSocketManager webSocketManager : connectionMap.values() )
        {
            webSocketManager.sendPresence( type, show, status );    
        }
    }
    public void sendTextMessage( String jid, String text )
    {
        
        Account account = findAccountByJID( jid );
        if( account != null )
        {
            WebSocketManager webSocketManager = connectionMap.get( account );
            webSocketManager.sendMessage( jid, text );
            addTextMessage( jid, "You: "+ text, true );
        }
    }
    
    
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
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
        connect();
    }

    public void updateAccount( Account account )
    {
        accountStorage.updateAccount( account );
        WebSocketManager webSocketManager = connectionMap.remove( account );
        removeRoster( account );
        webSocketManager.disconnect();
        webSocketManager.connect();

        connectionMap.put( account, new WebSocketManager( this, account ) );
    }
    
    public void removeAccount( Account account )
    {
        accountStorage.removeAccount( account );
        WebSocketManager webSocketManager = connectionMap.remove( account );
        removeRoster( account );
        webSocketManager.disconnect();
    }
    
    public List<Account> getAccounts()
    {
        return accountStorage.getAccounts();
    }
    
    
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    public void onConnecting()
    {
        broadcast( MESSAGE_TYPE_CONNECTING );
    }
    public void onConnectingFinished()
    {
        broadcast( MESSAGE_TYPE_CONNECTING_FINISHED );
    }
    public void onAuthenticating()
    {
        broadcast( MESSAGE_TYPE_AUTHENTICATING );
    }
    public void onAuthenticatingFinished()
    {
        broadcast( MESSAGE_TYPE_AUTHENTICATING_FINISHED );
    }
    public void onDisconnected()
    {
        Toast.makeText( this, "Stopped", Toast.LENGTH_SHORT ).show();
    }
    
    
    private String visibleChatJID = null;
    
    
    public void setVisibleChat( String jid )
    {
        this.visibleChatJID = jid;
    }
    
    
    private void messageNotification( String from, String message )
    {
        if( visibleChatJID == null || !visibleChatJID.equalsIgnoreCase( from ) )
        {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            int icon = android.R.drawable.stat_notify_chat;
            
            CharSequence tickerText = getText(R.string.message_notification);
            long when = System.currentTimeMillis();
    
            Notification notification = new Notification(icon, tickerText, when);
            
            Intent intent = new Intent( this, Chat.class );
            intent.putExtra( Chat.EXTRA_ROSTER_JID, from );

            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0 );
            notification.setLatestEventInfo(this, "AndroIM", message, contentIntent);
            notification.defaults |= Notification.DEFAULT_SOUND;
            
            notificationManager.notify( R.string.message_notification, notification);
        }        
    }
}
