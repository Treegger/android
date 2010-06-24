package com.treegger.android.im.remote;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.treegger.android.im.Account;
import com.treegger.android.im.AccountManager;
import com.treegger.protobuf.WebSocketProto.AuthenticateRequest;
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

        WSConnector wsConnector = new WSConnector();
        try
        {
            wsConnector.connect( "wss", "xmpp.treegger.com", 443, "/tg-1.0", new WSHandler( this, wsConnector ) );
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

 



    public static class WSHandler implements WSEventHandler
    {
        public static final String TAG = "WSHandler";

        private TreeggerService treeggerService;
        private WSConnector wsConnector;
        public WSHandler( TreeggerService treeggerService, WSConnector wsConnector )
        {
            this.treeggerService = treeggerService;
            this.wsConnector = wsConnector;
        }
        @Override
        public void onOpen()
        {
            Toast toast = Toast.makeText(treeggerService, "Connected", Toast.LENGTH_LONG );
            toast.show();
            
            AccountManager accountManager = new AccountManager( treeggerService );
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
                treeggerService.messagesQueue.add( data );
                treeggerService.sendBroadcast( new Intent( BROADCAST_ACTION ) );
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
        }
        
        @Override
        public void onClose()
        {
        }
    }

}
