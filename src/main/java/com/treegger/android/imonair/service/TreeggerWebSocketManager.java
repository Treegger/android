package com.treegger.android.imonair.service;

import java.io.IOException;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

import com.treegger.protobuf.WebSocketProto.AuthenticateRequest;
import com.treegger.protobuf.WebSocketProto.AuthenticateResponse;
import com.treegger.protobuf.WebSocketProto.BindResponse;
import com.treegger.protobuf.WebSocketProto.Ping;
import com.treegger.protobuf.WebSocketProto.Presence;
import com.treegger.protobuf.WebSocketProto.TextMessage;
import com.treegger.protobuf.WebSocketProto.WebSocketMessage;
import com.treegger.websocket.WSConnector;
import com.treegger.websocket.WSConnector.WSEventHandler;

public class TreeggerWebSocketManager implements WSEventHandler
{
    public static final String TAG = "WSHandler";
     
    public static final long PING_DELAY = 30*1000;

    private Integer pingId = 0;
    
    private String sessionId;
    
    private TreeggerService treeggerService;
    private WSConnector wsConnector;
    private Account account;
    
    private Timer timer;
    private long lastActivity = 0;
    
    public static final String DEFAULT_RESOURCE = "AndroidIMonAir";
    
    public boolean authenticated = false;
    
    private String fromJID;
    
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_PAUSED = 3;
    public static final int STATE_DISCONNECTING = 4;
    public static final int STATE_PAUSING = 5;
    
    private static final int TRANSITION_CONNECT = 1;
    private static final int TRANSITION_CONNECTED = 2;
    private static final int TRANSITION_PAUSE = 3;
    private static final int TRANSITION_PAUSED = 31;
    private static final int TRANSITION_RECONNECT = 4;
    private static final int TRANSITION_DISCONNECT = 5;
    private static final int TRANSITION_DISCONNECTED = 51;
    private static final int TRANSITION_RESUME = 6;
    
    private static final Lock LOCK =  new ReentrantLock();
    
    private int connectionState = STATE_DISCONNECTED;
    
    public int getState()
    {
        return connectionState;
    }
    
    private void applyTransition( int transition )
    {
        try
        {
            LOCK.lockInterruptibly();
            switch( transition )
            {
                case TRANSITION_CONNECT:
                    if( connectionState == STATE_DISCONNECTED || connectionState == STATE_PAUSED )
                    {
                        connectionState = STATE_CONNECTING;
                        LOCK.unlock();
                        doConnect();
                    }
                    else
                    {
                        LOCK.unlock();
                    }
                    break;
                case TRANSITION_CONNECTED:
                    if( connectionState == STATE_CONNECTING )
                    {
                        connectionState = STATE_CONNECTED;
                        LOCK.unlock();
                        doConnected();
                    }
                    else
                    {
                        LOCK.unlock();
                    }
                    break;
                case TRANSITION_PAUSE:
                    if( connectionState == STATE_CONNECTED )
                    {
                        connectionState = STATE_PAUSING;
                        LOCK.unlock();
                        doPause();
                    }
                    else
                    {
                        LOCK.unlock();
                    }
                    break;
                    
                case TRANSITION_PAUSED:
                    if( connectionState == STATE_PAUSING )
                    {
                        connectionState = STATE_PAUSED;
                        LOCK.unlock();
                        doPaused();
                    }
                    else
                    {
                        LOCK.unlock();
                    }
                    break;
                    
                    
                case TRANSITION_RECONNECT:
                    if( connectionState == STATE_DISCONNECTED )
                    {
                        connectionState = STATE_CONNECTING;
                        LOCK.unlock();
                        doConnect();
                    }
                    else if( connectionState == STATE_CONNECTING )
                    {
                        connectionState = STATE_CONNECTING;
                        LOCK.unlock();
                        doReconnect();
                    }
                    else if( connectionState == STATE_CONNECTED )
                    {
                        connectionState = STATE_CONNECTING;
                        LOCK.unlock();
                        doReconnect();
                    }
                    else
                    {
                        LOCK.unlock();
                    }
                    break;
                case TRANSITION_DISCONNECT:
                    if( connectionState == STATE_PAUSED || connectionState == STATE_CONNECTED || connectionState == STATE_CONNECTING )
                    {
                        connectionState = STATE_DISCONNECTING;
                        LOCK.unlock();
                        doDisconnect();
                    }
                    else
                    {
                        LOCK.unlock();
                    }
                    break;
                case TRANSITION_DISCONNECTED:
                    if( connectionState == STATE_DISCONNECTING )
                    {
                        connectionState = STATE_DISCONNECTED;
                        LOCK.unlock();
                        doDisconnected();
                    }
                    else
                    {
                        LOCK.unlock();
                    }
                    break;
                    
                case TRANSITION_RESUME:
                    if( connectionState == STATE_PAUSED )
                    {
                        connectionState = STATE_CONNECTING;
                        LOCK.unlock();
                        doResume();                    
                    }
                    else
                    {
                        LOCK.unlock();
                    }
                    break;
                default:
                   LOCK.unlock();
            }
        }
        catch (Exception e)
        {
            Log.e( TAG, e.getMessage(), e );
        }
    }
    
    private void doConnect()
    {
        lastActivity = System.currentTimeMillis();
        treeggerService.onConnecting();
        try
        {
           if( wsConnector != null ) wsConnector.connect( "wss", "xmpp.treegger.com", 443, "/tg-1.0", true, this );
        }
        catch ( IOException e )
        {
            Log.v(TAG, "Connection failed");
        }
    }
    private void doConnected()
    {
        if( timer == null )
        {
            timer = new Timer();
            timer.schedule( new PingTask(), PING_DELAY, PING_DELAY );
        }

        treeggerService.onConnectingFinished();
        treeggerService.onAuthenticating();
        authenticate( account.name, account.socialnetwork, account.password );
    }
    private void doPause()
    {
        try
        {
            if( wsConnector != null  ) wsConnector.close();
            applyTransition( TRANSITION_PAUSED );
        }
        catch ( IOException e )
        {
            Log.v(TAG, "Deactivate failed");
        }
    }
    private void doPaused()
    {
        treeggerService.onPaused();
    }

    private void doResume()
    {
        //treeggerService.handler.post( new DisplayToastRunnable( treeggerService, "Resume connection " + account.name + "@"+account.socialnetwork ) );
        try
        {
           if( wsConnector != null ) wsConnector.connect( "wss", "xmpp.treegger.com", 443, "/tg-1.0", true, this );
        }
        catch ( IOException e )
        {
            Log.v(TAG, "Resume failed");
        }
    }
    private void doReconnect()
    {
        doDisconnect();
        doConnect();
    }
    
    private void doDisconnect()
    {
        try
        {
            if( timer != null ) timer.cancel();
            timer = null;
            if( wsConnector != null ) wsConnector.close();
            applyTransition( TRANSITION_DISCONNECTED );
        }
        catch ( IOException e )
        {
            Log.v(TAG, "Disconnection failed");
        }
        
    }
    private void doDisconnected()
    {
        treeggerService.onDisconnected();
    }
    
    
    public TreeggerWebSocketManager( TreeggerService treeggerService, Account account )
    {
        this.treeggerService = treeggerService;
        this.account = account;
        
        this.wsConnector = new WSConnector();
        connect();
    }

    public void connect()
    {
        applyTransition( TRANSITION_CONNECT );
    }
    private void pause()
    {
        applyTransition( TRANSITION_PAUSE );
    }
    
    public void disconnect()
    {
        applyTransition( TRANSITION_DISCONNECT );
    }
    
    public void reconnect()
    {
        applyTransition( TRANSITION_RECONNECT );
    }
    private void resume()
    {
        applyTransition( TRANSITION_RESUME );
    }

    
    
    private void authenticate( final String name, final String socialnetwork, final String password )
    {
        WebSocketMessage.Builder message = WebSocketMessage.newBuilder();
        AuthenticateRequest.Builder authReq = AuthenticateRequest.newBuilder();
        String username = name.trim().toLowerCase()+"@"+ socialnetwork.trim().toLowerCase();
        this.fromJID = username+"/"+DEFAULT_RESOURCE;
        authReq.setUsername( username );
        authReq.setPassword( password.trim() );
        authReq.setResource( DEFAULT_RESOURCE );
        if( sessionId != null ) authReq.setSessionId( sessionId );
        message.setAuthenticateRequest( authReq );
        sendWebSocketMessage( message );
    }
    
    
    

    public void sendPresence( String type, String show, String status )
    {
        if( authenticated )
        {
            lastActivity = System.currentTimeMillis();
    
            WebSocketMessage.Builder message = WebSocketMessage.newBuilder();
            Presence.Builder presence = Presence.newBuilder();
            presence.setType( type );
            presence.setShow( show );
            presence.setStatus( status );
            presence.setFrom( fromJID );
            message.setPresence( presence );
            sendWebSocketMessage( message );
        }
    }

    public void sendMessage( String to, String text )
    {
        if( authenticated )
        {
            wakeup();
            
            lastActivity = System.currentTimeMillis();
    
            WebSocketMessage.Builder message = WebSocketMessage.newBuilder();
            TextMessage.Builder textMessage = TextMessage.newBuilder();
            textMessage.setBody( text );
            textMessage.setToUser( to );
            textMessage.setFromUser( fromJID );
            message.setTextMessage( textMessage );
            sendWebSocketMessage( message );
        }
    }
    public void sendStateNotificationMessage( String to, boolean composing, boolean paused, boolean active, boolean gone )
    {
        if( authenticated )
        {
            lastActivity = System.currentTimeMillis();
            WebSocketMessage.Builder message = WebSocketMessage.newBuilder();
            TextMessage.Builder textMessage = TextMessage.newBuilder();
            
            textMessage.setFromUser( fromJID );
            textMessage.setToUser( to );
            textMessage.setType("chat");

            textMessage.setComposing( composing );
            textMessage.setActive( active );
            textMessage.setPaused( paused );
            textMessage.setGone( gone );

            message.setTextMessage( textMessage );
            sendWebSocketMessage( message );
        }
    }

    
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    private Queue<WebSocketMessage.Builder> laterDeliveryQueue = new ConcurrentLinkedQueue<WebSocketMessage.Builder>();
    
    private void flushLaterWebSocketMessageQueue()
    {
        while( !laterDeliveryQueue.isEmpty() )
        {
            WebSocketMessage.Builder message = laterDeliveryQueue.poll();
            try
            {
                wsConnector.send( message.build().toByteArray() );
            }
            catch ( IOException e )
            {
                Log.w( TAG, e.getMessage(), e );
            }
        }
    }
    
    private void sendWebSocketMessage( final WebSocketMessage.Builder message )
    {
        try
        {
            if( connectionState == STATE_CONNECTED && wsConnector != null && !wsConnector.isClosed()  )
            {
                wsConnector.send( message.build().toByteArray() );
            }
            else
            {
                if( message.hasTextMessage() ) laterDeliveryQueue.add( message );
                connect();
            }
        }
        catch ( IOException e )
        {
            Log.w( TAG, e.getMessage(), e );
        }
    }

    
    
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    private final static long PAUSE_DELAY = 60*1000;
    private final static long PAUSE_DURATION = 5*60*1000;
    
    private boolean sleeping = false;
    private void sleep()
    {
        sendPresence( "", "away", "" );
        sleeping = true;
    }
    private void wakeup()
    {
        if( sleeping )
        {
            sleeping = false;
            sendPresence( "", "", "" );
        }
    }

    class PingTask extends TimerTask 
    {
        
        public void run() 
        {
            long now = System.currentTimeMillis();
            if( lastActivity + PAUSE_DELAY < now && connectionState == STATE_CONNECTED )
            {
                sleep();
                pause();
            }
            else if( lastActivity + PAUSE_DURATION < now && connectionState == STATE_PAUSED )
            {
                resume();
            }
            else
            {
                if( connectionState == STATE_CONNECTED && hasSession() )
                {
                    WebSocketMessage.Builder message = WebSocketMessage.newBuilder();
                    Ping.Builder ping = Ping.newBuilder();
                    ping.setId( pingId.toString() );
                    pingId++;
                    sendWebSocketMessage( message );
                }
            }
        }
    };

    
    
    private final boolean hasSession()
    {
        return sessionId != null && sessionId.length()>0;
    }
    
    
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    // ----------------------------------------------------------------------------
    @Override
    public void onOpen()
    {
        authenticated = false;
        applyTransition( TRANSITION_CONNECTED );
        
    }

    private void postAuthentication()
    {
        authenticated = true;
        if( !sleeping ) sendPresence( "", "", "" );
        flushLaterWebSocketMessageQueue();
    }
    
    @Override
    public void onMessage( byte[] message )
    {
        if( connectionState == STATE_CONNECTED ) 
        try
        {
            WebSocketMessage data = WebSocketMessage.newBuilder().mergeFrom( message ).build();
            
            if( data.hasAuthenticateResponse() )
            {
                AuthenticateResponse authenticateResponse = data.getAuthenticateResponse();
                sessionId = authenticateResponse.getSessionId();
                
                treeggerService.onAuthenticatingFinished();

                if( hasSession() )
                {
                    postAuthentication();
                }
                else
                {
                    treeggerService.handler.post( new DisplayToastRunnable( treeggerService, "Authentication failure for account: " + account.name + "@"+account.socialnetwork ) );
                    disconnect();
                }

            }
            else if( data.hasBindResponse() )
            {
                BindResponse authenticateResponse = data.getBindResponse();
                sessionId = authenticateResponse.getSessionId();
                
                treeggerService.onAuthenticatingFinished();

                if( hasSession() )
                {
                    postAuthentication();
                }
                else
                {
                    if( wsConnector != null ) wsConnector.close();
                }
            }
            else if( data.hasRoster() )
            {
                treeggerService.addRoster( account, data.getRoster() );
            }
            else if( data.hasVcardResponse() )
            {
                treeggerService.onVCard( data.getVcardResponse() );
            }
            else if( data.hasTextMessage() )
            {
                lastActivity = System.currentTimeMillis();
                treeggerService.addTextMessage( account, data.getTextMessage() );
            }
            else if( data.hasPresence() )
            {
                treeggerService.addPresence( account, data.getPresence() );
            }
        }
        catch ( Exception e )
        {
            Log.w( TAG, e.getMessage(), e );
        }
    }

    
    
    @Override
    public void onMessage( String message )
    {
    }

    
    @Override
    public void onError( Exception e )
    {
        //treeggerService.handler.post( new DisplayToastRunnable( treeggerService, "Error: " + e.getMessage() ) );
        reconnect();
    }
    
    
    @Override
    public void onClose()
    {
    }
    

    @Override
    public void onStop()
    {
        Log.w( TAG, "Remote connection stopped" );
    }
    

}
