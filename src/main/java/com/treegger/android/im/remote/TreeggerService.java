package com.treegger.android.im.remote;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.treegger.android.im.Account;
import com.treegger.android.im.AccountManager;
import com.treegger.protobuf.WebSocketProto.AuthenticateRequest;
import com.treegger.protobuf.WebSocketProto.AuthenticateResponse;
import com.treegger.protobuf.WebSocketProto.BindRequest;
import com.treegger.protobuf.WebSocketProto.BindResponse;
import com.treegger.protobuf.WebSocketProto.Ping;
import com.treegger.protobuf.WebSocketProto.WebSocketMessage;
import com.treegger.websocket.WSConnector;
import com.treegger.websocket.WSConnector.WSEventHandler;

public class TreeggerService
    extends Service
{
    public static final String TAG = "TreeggerService";
    
    public static final String BROADCAST_ACTION = WebSocketMessage.class.getName();
    
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

    public static class WebSocketManager implements WSEventHandler
    {
        public static final String TAG = "WSHandler";
        
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
                    wsConnector.connect( "wss", "xmpp.treegger.com", 443, "/tg-1.0", this );
                }
                catch ( IOException e )
                {
                    Log.v(TAG, "Connection failed");
                }
                
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
            if( sessionId != null )
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
                if( wsConnector.isConnected() )
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
                if( sessionId != null )
                {
                    WebSocketMessage.Builder message = WebSocketMessage.newBuilder();
                    Ping.Builder ping = Ping.newBuilder();
                    ping.setId( pingId.toString() );
                    pingId++;
                    sendMessage( message );
                }
            }
        }

        
        
        
        
        
        @Override
        public void onOpen()
        {
            connecting.set( false );
            
            if( sessionId == null )
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

                    handler.post( new DisplayToastRunnable( treeggerService, "Authenticated" ) );
                    
                    Timer timer = new Timer();
                    timer.schedule( new PingTask(), PING_DELAY, PING_DELAY );
                }
                else if( data.hasBindResponse() )
                {
                    BindResponse authenticateResponse = data.getBindResponse();
                    sessionId = authenticateResponse.getSessionId();
                    if( sessionId != null && sessionId.length() == 0 )
                    {
                        sessionId = null;
                        wsConnector.close();
                    }
                    else
                    {
                        handler.post( new DisplayToastRunnable( treeggerService, "Reconnected" ) );
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
            connect();
        }
        

    }

}
