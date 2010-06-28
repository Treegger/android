package com.treegger.android.im.service;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Handler;
import android.util.Log;

import com.treegger.protobuf.WebSocketProto.AuthenticateRequest;
import com.treegger.protobuf.WebSocketProto.AuthenticateResponse;
import com.treegger.protobuf.WebSocketProto.BindRequest;
import com.treegger.protobuf.WebSocketProto.BindResponse;
import com.treegger.protobuf.WebSocketProto.Ping;
import com.treegger.protobuf.WebSocketProto.Presence;
import com.treegger.protobuf.WebSocketProto.TextMessage;
import com.treegger.protobuf.WebSocketProto.WebSocketMessage;
import com.treegger.websocket.WSConnector;
import com.treegger.websocket.WSConnector.WSEventHandler;

public class WebSocketManager implements WSEventHandler
{
    public static final String TAG = "WSHandler";
     
    public static final long PING_DELAY = 30*1000;

    private Integer pingId = 0;
    
    private String sessionId;
    
    private TreeggerService treeggerService;
    private WSConnector wsConnector;
    private Account account;
    
    private Handler handler;
    private Timer timer;
    
    public static final String DEFAULT_RESOURCE = "AndroIM";
    
    private String fromJID;
    
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final int STATE_DESTROYING = 3;
    
    private AtomicInteger connectionState = new AtomicInteger(STATE_DISCONNECTED);
    
    public WebSocketManager( TreeggerService treeggerService, Account account )
    {
        this.treeggerService = treeggerService;
        this.account = account;
        
        this.handler = new Handler();

        this.timer = new Timer();
        
        
        this.wsConnector = new WSConnector();
        connect();
    }

    private void connect()
    {
        if( connectionState.getAndSet( STATE_CONNECTING ) == STATE_DISCONNECTED )
        {
            try
            {
               if( wsConnector != null ) wsConnector.connect( "wss", "xmpp.treegger.com", 443, "/tg-1.0", this );
            }
            catch ( IOException e )
            {
                Log.v(TAG, "Connection failed");
            }
            
        }
    }
    public void disconnect()
    {
        try
        {
            connectionState.set( STATE_DESTROYING );
            timer.cancel();
            treeggerService.removeRoster( account );

            if( wsConnector != null && wsConnector.isConnected() ) wsConnector.close();
            wsConnector = null;
        }
        catch ( IOException e )
        {
            Log.v(TAG, "Disconnection failed");
        }
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
        message.setAuthenticateRequest( authReq );
        sendWebSocketMessage( message );
    }
    
    
    
    
    private void bind()
    {
        if( hasSession() )
        {
            WebSocketMessage.Builder message = WebSocketMessage.newBuilder();
            BindRequest.Builder bindRequest = BindRequest.newBuilder();
            bindRequest.setSessionId( sessionId );
            message.setBindRequest( bindRequest );
            sendWebSocketMessage( message );
        }
        
    }

    public void sendPresence( String type, String show, String status )
    {
        WebSocketMessage.Builder message = WebSocketMessage.newBuilder();
        Presence.Builder presence = Presence.newBuilder();
        presence.setType( type );
        presence.setShow( show );
        presence.setStatus( status );
        presence.setFrom( fromJID );
        message.setPresence( presence );
        sendWebSocketMessage( message );        
    }

    public void sendMessage( String to, String text )
    {
        WebSocketMessage.Builder message = WebSocketMessage.newBuilder();
        TextMessage.Builder textMessage = TextMessage.newBuilder();
        textMessage.setBody( text );
        textMessage.setToUser( to );
        textMessage.setFromUser( fromJID );
        message.setTextMessage( textMessage );
        sendWebSocketMessage( message );        
    }

    
    
    
    private void sendWebSocketMessage( final WebSocketMessage.Builder message )
    {
        try
        {
            if( wsConnector != null && wsConnector.isConnected() )
            {
                wsConnector.send( message.build().toByteArray() );
            }
            else
            {
                connect();
            }
        }
        catch ( IOException e )
        {
            Log.w( TAG, e.getMessage(), e );
        }
    }

    public class PingTask extends TimerTask 
    {
        public void run() 
        {
            if( hasSession() )
            {
                WebSocketMessage.Builder message = WebSocketMessage.newBuilder();
                Ping.Builder ping = Ping.newBuilder();
                ping.setId( pingId.toString() );
                pingId++;
                sendWebSocketMessage( message );
            }
        }
    }

    
    
    private final boolean hasSession()
    {
        return sessionId != null && sessionId.length()>0;
    }
    
    
    @Override
    public void onOpen()
    {
        connectionState.set( STATE_CONNECTED );
        
        if( !hasSession() )
        {
            handler.post( new DisplayToastRunnable( treeggerService, "Authenticating" ) );
            authenticate( account.name, account.socialnetwork, account.password );
        }
        else
        {
            handler.post( new DisplayToastRunnable( treeggerService, "Reconnecting" ) );
            bind();
        }
    }
    
    
    @Override
    public void onMessage( byte[] message )
    {
        try
        {
            WebSocketMessage data = WebSocketMessage.newBuilder().mergeFrom( message ).build();
            
            if( data.hasAuthenticateResponse() )
            {
                AuthenticateResponse authenticateResponse = data.getAuthenticateResponse();
                sessionId = authenticateResponse.getSessionId();

                if( hasSession() )
                {
                    handler.post( new DisplayToastRunnable( treeggerService, "Authenticated" ) );
                    sendPresence( "", "", "" );
                    timer.schedule( new PingTask(), PING_DELAY, PING_DELAY );
                }
                else
                {
                    handler.post( new DisplayToastRunnable( treeggerService, "Authentication failure for account: " + account.name + "@"+account.socialnetwork ) );
                }

            }
            else if( data.hasBindResponse() )
            {
                BindResponse authenticateResponse = data.getBindResponse();
                sessionId = authenticateResponse.getSessionId();
                if( hasSession() )
                {
                    handler.post( new DisplayToastRunnable( treeggerService, "Reconnected" ) );
                    sendPresence( "", "", "" );
                    
                    timer.schedule( new PingTask(), PING_DELAY, PING_DELAY );
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
        handler.post( new DisplayToastRunnable( treeggerService, "Error: " + e.getMessage() ) );
    }
    
    @Override
    public void onClose()
    {
        if( connectionState.get() != STATE_DESTROYING )
        {
            timer.cancel();
            handler.post( new DisplayToastRunnable( treeggerService, "Disconnected" ) );
            connectionState.set( STATE_DISCONNECTED );
            // TODO: should reconnect only if service is still running 
            connect();
        }
    }
    

    

}
