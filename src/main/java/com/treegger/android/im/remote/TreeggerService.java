package com.treegger.android.im.remote;

import java.io.IOException;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.protobuf.InvalidProtocolBufferException;
import com.treegger.android.im.Account;
import com.treegger.android.im.AccountManager;
import com.treegger.protobuf.WebSocketProto.AuthenticateRequest;
import com.treegger.protobuf.WebSocketProto.Roster;
import com.treegger.protobuf.WebSocketProto.WebSocketMessage;
import com.treegger.websocket.WSConnector;
import com.treegger.websocket.WSConnector.WSEventHandler;

public class TreeggerService
    extends Service
{
    public static final String TAG = "TreeggerService";
    
    private WebSocketCallBack webSocketCallBack = new OnRosterListener()
    {
        @Override
        public void onRoster( Roster roster )
        {
            Toast.makeText( TreeggerService.this, roster.toString(), Toast.LENGTH_SHORT ).show();
        }
    };
    
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class TreeggerServiceBinder
        extends Binder
    {
        TreeggerService getService()
        {
            return TreeggerService.this;
        }
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        WSConnector wsConnector = new WSConnector();
        try
        {
            wsConnector.connect( "wss", "xmpp.treegger.com", 443, "/tg-1.0", new WSHandler( webSocketCallBack, this, new Handler(), wsConnector) );
        }
        catch ( IOException e )
        {
            Log.v(TAG, "Connection failed");
        }
    }


    @Override
    public void onDestroy()
    {
        Toast.makeText( this, "Stopped", Toast.LENGTH_SHORT ).show();
    }

    @Override
    public IBinder onBind( Intent intent )
    {
        return mBinder;
    }

    public void setWebSocketCallBack( WebSocketCallBack webSocketCallBack )
    {
        this.webSocketCallBack = webSocketCallBack;
    }


    public WebSocketCallBack getWebSocketCallBack()
    {
        return webSocketCallBack;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new TreeggerServiceBinder();
    
    
    public static class UIUpdater implements Runnable  
    {
        private WebSocketCallBack webSocketCallBack;
        private WebSocketMessage data;
        public UIUpdater( WebSocketCallBack webSocketCallBack, WebSocketMessage data )
        {
            this.webSocketCallBack = webSocketCallBack;
            this.data = data;
        }
        public void run() 
        {
            if( webSocketCallBack instanceof OnRosterListener && data.hasRoster() )
            {
                ((OnRosterListener)webSocketCallBack).onRoster( data.getRoster() );
            }
        }
    };


    public static class WSHandler implements WSEventHandler
    {
        public static final String TAG = "WSHandler";

        private WebSocketCallBack webSocketCallBack;
        private Context context;
        private WSConnector wsConnector;
        private Handler handler;
        public WSHandler( WebSocketCallBack webSocketCallBack, Context context, Handler handler, WSConnector wsConnector )
        {
            this.webSocketCallBack = webSocketCallBack;
            this.context = context;
            this.wsConnector = wsConnector;
            this.handler = handler;
        }
        @Override
        public void onOpen()
        {
            Toast toast = Toast.makeText(context, "Connected", Toast.LENGTH_LONG );
            toast.show();
            
            AccountManager accountManager = new AccountManager( context );
            List<Account> accountList = accountManager.getAccounts();
            for( Account account : accountList )
            {
                WebSocketMessage.Builder message = WebSocketMessage.newBuilder();
                AuthenticateRequest.Builder authReq = AuthenticateRequest.newBuilder();
                authReq.setUsername( account.name.trim().toLowerCase()+"@"+ account.socialnetwork.trim().toLowerCase() );
                authReq.setPassword( account.password.trim() );
                authReq.setResource( "AndroIM" );

                message.setAuthenticateRequest( authReq );
                
                try
                {
                    wsConnector.send( message.build().toByteArray() );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }

            }

        }
        
        @Override
        public void onMessage( byte[] message )
        {
            try
            {
                WebSocketMessage data = WebSocketMessage.newBuilder().mergeFrom( message ).build();
                handler.post( new UIUpdater( webSocketCallBack, data ) );
                
            }
            catch ( InvalidProtocolBufferException e )
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
        }
        
        @Override
        public void onClose()
        {
        }
    }

}
