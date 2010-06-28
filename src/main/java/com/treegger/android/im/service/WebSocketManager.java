package com.treegger.android.im.service;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.treegger.protobuf.WebSocketProto.AuthenticateRequest;
import com.treegger.protobuf.WebSocketProto.AuthenticateResponse;
import com.treegger.protobuf.WebSocketProto.BindRequest;
import com.treegger.protobuf.WebSocketProto.BindResponse;
import com.treegger.protobuf.WebSocketProto.Ping;
import com.treegger.protobuf.WebSocketProto.WebSocketMessage;
import com.treegger.websocket.WSConnector;
import com.treegger.websocket.WSConnector.WSEventHandler;

public class WebSocketManager implements WSEventHandler
{
    public static final String TAG = "WSHandler";
    
    public static final String BROADCAST_ACTION = WebSocketMessage.class.getName();

    
    public static final long PING_DELAY = 30*1000;

    private Integer pingId = 0;
    
    private String sessionId;
    
    private TreeggerService treeggerService;
    private WSConnector wsConnector;
    private Account account;
    
    private Handler handler;
    
    private AtomicBoolean connecting = new AtomicBoolean( false );
    
    public WebSocketManager( TreeggerService treeggerService, Account account )
    {
        this.treeggerService = treeggerService;
        this.account = account;
        
        this.handler = new Handler();
        
        this.wsConnector = new WSConnector();
        connect();
    }

    private void connect()
    {
        if( !connecting.getAndSet( true ) )
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
        authReq.setUsername( name.trim().toLowerCase()+"@"+ socialnetwork.trim().toLowerCase() );
        authReq.setPassword( password.trim() );
        authReq.setResource( "AndroIM" );
        message.setAuthenticateRequest( authReq );
        sendMessage( message );
    }
    
    private void bind()
    {
        if( hasSession() )
        {
            WebSocketMessage.Builder message = WebSocketMessage.newBuilder();
            BindRequest.Builder bindRequest = BindRequest.newBuilder();
            bindRequest.setSessionId( sessionId );
            message.setBindRequest( bindRequest );
            sendMessage( message );
        }
        
    }
    
    private void sendMessage( final WebSocketMessage.Builder message )
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
                sendMessage( message );
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
        connecting.set( false );
        
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
                    Timer timer = new Timer();
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
                }
                else
                {
                    if( wsConnector != null ) wsConnector.close();
                }
            }
            else
            {
                treeggerService.messagesQueue.add( data );
                treeggerService.sendBroadcast( new Intent( BROADCAST_ACTION ) );
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
        handler.post( new DisplayToastRunnable( treeggerService, "Disconnected" ) );
        // TODO: should reconnect only if service is still running 
        connect();
    }
    


}
