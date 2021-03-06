package com.treegger.android.imonair.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.treegger.android.imonair.R;
import com.treegger.android.imonair.activity.Chat;
import com.treegger.protobuf.WebSocketProto.Presence;
import com.treegger.protobuf.WebSocketProto.Roster;
import com.treegger.protobuf.WebSocketProto.RosterItem;
import com.treegger.protobuf.WebSocketProto.TextMessage;
import com.treegger.protobuf.WebSocketProto.VCardResponse;

public class TreeggerService
    extends Service
{
    public static final String TAG = "TreeggerService";
    

    public static final String TREEGGER_BROADCAST_ACTION = "TreeggerServiceBroadcast";
    public static final String EXTRA_MESSAGE_TYPE = "messageType";
    
    
    public static final int     MESSAGE_TYPE_ROSTER_UPDATE = 1;
    public static final int     MESSAGE_TYPE_ROSTER_ADAPTER_UPDATE = 11;
    public static final int     MESSAGE_TYPE_TEXTMESSAGE_UPDATE = 2;
    public static final int     MESSAGE_TYPE_PRESENCE_UPDATE = 3;
    public static final int     MESSAGE_TYPE_VCARD_UPDATE = 33;
    public static final int     MESSAGE_TYPE_COMPOSING = 34;      
    
    
    public static final int     MESSAGE_TYPE_CONNECTING = 4;
    public static final int     MESSAGE_TYPE_CONNECTING_FINISHED = 5;
    public static final int     MESSAGE_TYPE_AUTHENTICATING = 6;
    public static final int     MESSAGE_TYPE_AUTHENTICATING_FINISHED = 7;
    public static final int     MESSAGE_TYPE_PAUSED = 8;
    public static final int     MESSAGE_TYPE_DISCONNECTED = 9;
    
    private static final int MAX_MESSAGESLIST_SIZE = 100;
    
    private final Binder binder = new LocalBinder();
    protected Handler handler; 
    
    private AccountStorage accountStorage;

    private Map<Account,TreeggerWebSocketManager> connectionMap = new HashMap<Account, TreeggerWebSocketManager>();


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
                    //disconnect();
                    prevNetworkType = NO_NETWORK;
                } 
                else 
                {
                    if( networkInfo.isConnected() && ( isFailover || networkChanged  ) )
                    {
                        reconnect();
                    }
                }
                
            }
        }
    };
    
    
    
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    private Map<Account,Roster> rosterMap = new HashMap<Account, Roster>();
 
    public synchronized Map<Account,Roster> getRosters()
    {
        return rosterMap;
    }
    
    private List<RosterItem> rosterItemsList = new ArrayList<RosterItem>();
    public synchronized List<RosterItem> getAllRosterItems()
    {
        for ( Roster roster : getRosters().values() )
        {
            for ( RosterItem rosterItem : roster.getItemList() )
            {
                boolean found = false;
                for( RosterItem item : rosterItemsList )
                {
                    if( item.getJid().equals( rosterItem.getJid() ) )
                    {
                        found = true;
                        break;
                    }
                }
                if( !found ) rosterItemsList.add( rosterItem );
            }

        }
        return rosterItemsList;
    }
    public synchronized void addRoster( Account account, Roster roster )
    {
        rosterMap.put( account, roster );
        broadcast( MESSAGE_TYPE_ROSTER_UPDATE );
    }
    public synchronized void removeRoster( Account account )
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
    
    
    private Map<String,List<ChatMessage>> textMessageMap = Collections.synchronizedMap( new HashMap<String,List<ChatMessage>>() );
    private Set<String> unconsumedMessageFroms = Collections.synchronizedSet( new HashSet<String>() ); 
    
    public List<ChatMessage> getTextMessageList( String fromUserAndHost )
    {
        return textMessageMap.get( fromUserAndHost );
    }
    
    public boolean hasMessageFrom( String fromUserAndHost )
    {
        return unconsumedMessageFroms.contains( fromUserAndHost );
    }
    public void markHasReadMessageFrom( String fromUserAndHost )
    {
        if( lastNotificationUserAndHost != null && lastNotificationUserAndHost.equalsIgnoreCase( fromUserAndHost ) )
        {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
            lastNotificationUserAndHost = null;
        }
        
        unconsumedMessageFroms.remove( fromUserAndHost );
        broadcast( MESSAGE_TYPE_ROSTER_ADAPTER_UPDATE );
    }
    
    private String unXML( String s )
    {
        return s.replace( "&apos;", "'" ).replace( "&quot;", "\"" ).replace( "&gt;", ">" ).replace( "&lt;", "<" ).replace( "&amp;", "&" );
    }
    private String toXML( String s )
    {
        return s.replace( "&", "&amp;" ).replace( "<", "&lt;" ).replace( ">", "&gt;" ).replace( "\"", "&quot;" ).replace( "'", "&apos;" );
    }
    private void addTextMessage( String targetChatJID, ChatMessage message, boolean localMessage )
    {
        if( message != null && message.text != null && message.text.length() > 0 )
        {
            message.text = unXML( message.text );
            List<ChatMessage> messagesList = textMessageMap.get( targetChatJID );
            if( messagesList == null )
            {
                messagesList = Collections.synchronizedList( new ArrayList<ChatMessage>() );
                textMessageMap.put( targetChatJID, messagesList );
            }
            else
            {
                if( messagesList.size() > MAX_MESSAGESLIST_SIZE )
                {
                    messagesList.remove( 0 );
                }
            }
            messagesList.add( message );
            if( !localMessage )
            {
                unconsumedMessageFroms.add( message.userAndHost );
                messageNotification( message );
            }
            broadcast( MESSAGE_TYPE_TEXTMESSAGE_UPDATE );
        }
    }
    
    
    
    private Map<String,Boolean> messageComposingMap = Collections.synchronizedMap( new HashMap<String,Boolean>() ); 

    public void addTextMessage( Account account, TextMessage textMessage )
    {
        String fromUserAndHost = getUserAndHostFromJID( textMessage.getFromUser() );
        RosterItem rosterItem = findRosterItemByJID( fromUserAndHost );
        if( rosterItem != null )
        {
            if( textMessage.hasBody() )
            {
                addTextMessage( fromUserAndHost, new ChatMessage( fromUserAndHost, textMessage.getBody() ), false );
                Boolean value = messageComposingMap.remove( fromUserAndHost );
                if( value != null ) broadcast( MESSAGE_TYPE_COMPOSING );
            }
            else
            {
                if( textMessage.hasComposing() && textMessage.getComposing() )
                {
                    messageComposingMap.put(  fromUserAndHost, true );
                }
                else
                {
                    messageComposingMap.remove( fromUserAndHost );
                }
                broadcast( MESSAGE_TYPE_COMPOSING );
            }
        }
    }
    public boolean isComposing( String jid )
    {
        Boolean composing = messageComposingMap.get( jid );
        if( composing == null ) return false;
        return composing;
    }


    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    private Map<String,List<Presence>> presenceMap = new HashMap<String,List<Presence>>(); 
    
    public synchronized void addPresence( Account account, Presence presence )
    {
        String userAndHost = getUserAndHostFromJID( presence.getFrom() );
        messageComposingMap.remove( userAndHost );
        
        List<Presence> presences = presenceMap.get( userAndHost );
        if( presences == null )
        {
            presences = new ArrayList<Presence>();
            presenceMap.put( userAndHost, presences );
        }
        
        Iterator<Presence> iter = presences.iterator();
        while( iter.hasNext() )
        {
            if( iter.next().getFrom().equals( presence.getFrom() ) ) iter.remove();
        }

        String presenceType = presence.getType();
        if( presenceType != null && !presenceType.equalsIgnoreCase( "unavailable" ) )
        {
            presences.add( presence );
        }
        broadcast( MESSAGE_TYPE_PRESENCE_UPDATE );

    }
    public synchronized Presence getPresence( String jid )
    {
        List<Presence> presences = presenceMap.get( jid );
        if( presences != null )
        {
            for( Presence presence : presences )
            {
                if( ( presence.getType() == null ||  presence.getType().length() == 0 ) 
                    && ( presence.getShow() == null ||  presence.getShow().length() == 0 ) )
                {
                    return presence;
                }
            }
            for( Presence presence : presences )
            {
                if( presence.getShow() == null ||  presence.getShow().length() > 0 )
                {
                    return presence;
                }
            }
            for( Presence presence : presences )
            {
                return presence;
            }
        }
        return null;
    }
    
    private int currentSelectedPresence = TreeggerWebSocketManager.PRESENCE_AVAILABLE;
    public void setCurrentSelectedPresence( int presence )
    {
        currentSelectedPresence = presence;
        for( TreeggerWebSocketManager webSocketManager : connectionMap.values() )
        {
            webSocketManager.setCurrentSelectedPresence( presence );    
        }
    }
    public int getCurrentSelectedPresence()
    {
        return currentSelectedPresence;
    }
    public void sendCurrentSelectedPresence()
    {
        for( TreeggerWebSocketManager webSocketManager : connectionMap.values() )
        {
            webSocketManager.sendCurrentSelectedPresence();    
        }        
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
            TreeggerWebSocketManager webSocketManager = connectionMap.get( account );
            if( webSocketManager != null  )
            {
                webSocketManager.connect();
            }
            else
            {
                connectionMap.put( account, new TreeggerWebSocketManager( this, account ) );
            }
        }        
    }

    private int getConnectionState( Account account )
    {
        TreeggerWebSocketManager webSocketManager = connectionMap.get( account );
        if( webSocketManager != null ) 
            return webSocketManager.getState();
        else 
            return TreeggerWebSocketManager.STATE_DISCONNECTED;
    }
    public String getConnectionStateName( Account account )
    {
        switch( getConnectionState(account) )
        {
            case TreeggerWebSocketManager.STATE_DISCONNECTED:
                return getString( R.string.state_disconnected );
            case TreeggerWebSocketManager.STATE_CONNECTING:
                return getString( R.string.state_connecting );
            case TreeggerWebSocketManager.STATE_CONNECTED:
                return getString( R.string.state_connected );
            case TreeggerWebSocketManager.STATE_PAUSED:
                return getString( R.string.state_paused );
            case TreeggerWebSocketManager.STATE_DISCONNECTING:
                return getString( R.string.state_disconnecting );
            case TreeggerWebSocketManager.STATE_PAUSING:
                return getString( R.string.state_pausing );
            default:
                return getString( R.string.state_error );
        }
    }
    public String getConnectionStates()
    {
        String states = ""; 
        for( Account account : getAccounts() )
        {
            if( states.length()>0) states+= " - ";
            states += getConnectionStateName( account );
        }
        if( states.length() > 0 ) return "("+states+")";
        else return "";
    }

    @Override
    public void onDestroy()
    {
        disconnect();
        cleanup();
        unregisterReceiver( receiver );
    }

    public void disconnect()
    {
        for( TreeggerWebSocketManager webSocketManager : connectionMap.values() )
        {
            webSocketManager.disconnect();
        }
        //connectionMap.clear();
    }
    public void reconnect()
    {
        for( TreeggerWebSocketManager webSocketManager : connectionMap.values() )
        {
            webSocketManager.reconnect();
        }
        //connectionMap.clear();
    }
    public void signOut()
    {
        for( TreeggerWebSocketManager webSocketManager : connectionMap.values() )
        {
            webSocketManager.signOut();
        }        
    }
    
    public void cleanup()
    {
        messageComposingMap.clear();
        presenceMap.clear();
        rosterMap.clear();
        rosterItemsList.clear();
        vcards.clear();
        textMessageMap.clear();
        connectionMap.clear();
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
        for( TreeggerWebSocketManager webSocketManager : connectionMap.values() )
        {
            webSocketManager.sendPresence( type, show, status );    
        }
    }
    public void sendTextMessage( String jid, String text )
    {
        Account account = findAccountByJID( jid );
        if( account != null )
        {
            TreeggerWebSocketManager webSocketManager = connectionMap.get( account );
            webSocketManager.sendMessage( jid, toXML( text ) );
            String currentJid = accountStorage.getAccounts().get( 0 ).name.toLowerCase() + "@" + accountStorage.getAccounts().get( 0 ).socialnetwork.toLowerCase();
            addTextMessage( jid, new ChatMessage( currentJid, text), true );
        }
    }
    public void sendStateNotificationMessage( String to, boolean composing, boolean paused, boolean active, boolean gone )
    {
        Account account = findAccountByJID( to );
        if( account != null )
        {
            TreeggerWebSocketManager webSocketManager = connectionMap.get( account );
            webSocketManager.sendStateNotificationMessage( to, composing, paused, active, gone );
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
        connectionMap.put( account, new TreeggerWebSocketManager( this, account ) );
        connect();
    }

    public void updateAccount( Account account )
    {
        accountStorage.updateAccount( account );
        TreeggerWebSocketManager webSocketManager = connectionMap.remove( account );
        removeRoster( account );
        webSocketManager.reconnect();

        connectionMap.put( account, new TreeggerWebSocketManager( this, account ) );
    }
    
    public void removeAccount( Account account )
    {
        accountStorage.removeAccount( account );
        TreeggerWebSocketManager webSocketManager = connectionMap.remove( account );
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
    public void onPaused()
    {
        messageComposingMap.clear();
        broadcast( MESSAGE_TYPE_PAUSED );
        //handler.post( new DisplayToastRunnable( this, "Pause connection" ) );
    }
    public void onDisconnected()
    {
        broadcast( MESSAGE_TYPE_DISCONNECTED );
    }
    public void onSignOut()
    {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
        cleanup();
        stopSelf();
        System.exit( 0 );
    }
    
    public Map<String, VCardResponse> vcards = Collections.synchronizedMap( new HashMap<String, VCardResponse>() );
    public void onVCard( VCardResponse vcard )
    {
        vcards.put( vcard.getFromUser(), vcard );
        broadcast( MESSAGE_TYPE_VCARD_UPDATE );
    }

    
    private String visibleChatUserAndHost = null;
    
    
    public void setVisibleChat( String userAndHost )
    {
        this.visibleChatUserAndHost = userAndHost;
    }
    
    private String lastNotificationUserAndHost = null;
    
    private void messageNotification( ChatMessage chatMessage )
    {
        if( visibleChatUserAndHost == null || !visibleChatUserAndHost.equalsIgnoreCase( chatMessage.userAndHost ) )
        {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            
            CharSequence tickerText = getText(R.string.message_notification);
            long when = System.currentTimeMillis();
    
            Notification notification = new Notification(android.R.drawable.stat_notify_chat, tickerText, when);
            
            Intent intent = new Intent( this, Chat.class );
            intent.addFlags( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
            lastNotificationUserAndHost = chatMessage.userAndHost;
            intent.putExtra( Chat.EXTRA_ROSTER_JID, chatMessage.userAndHost );

            PendingIntent contentIntent = PendingIntent.getActivity( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );
            
            VCardResponse vcard = vcards.get( chatMessage.userAndHost );
            notification.setLatestEventInfo(this, vcard.getFn(), chatMessage.text, contentIntent);
            notification.defaults |= Notification.DEFAULT_SOUND;

            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
            notification.ledARGB = Color.GREEN; 
            notification.ledOffMS = 500; 
            notification.ledOnMS = 500; 
            
            notificationManager.notify( R.string.message_notification, notification);
        }        
    }
}
