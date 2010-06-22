package com.treegger.android.im;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.treegger.protobuf.WebSocketProto.AuthenticateRequest;
import com.treegger.protobuf.WebSocketProto.WebSocketMessage;
import com.treegger.websocket.WSConnector;
import com.treegger.websocket.WSConnector.WSEventHandler;

public class AndroIM extends Activity {
    public static final String TAG = "AndroIM";

    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        
        WSConnector wsConnector = new WSConnector();
        try
        {
            wsConnector.connect( "wss", "xmpp.treegger.com", 443, "/tg-1.0", new Handler(this, wsConnector) );
        }
        catch ( IOException e )
        {
            Log.v(TAG, "Connection failed");
        }
        
        ListView rosterListView = (ListView)findViewById( R.id.roster_list );
        
        final String[] rosterNames = { "dude@TWITTER", "bill@TWITTER", "dude@TWITTER", "bill@TWITTER", "dude@TWITTER", "bill@TWITTER" ,"dude@TWITTER", "bill@TWITTER" ,"dude@TWITTER", "bill@TWITTER" ,"dude@TWITTER", "bill@TWITTER" ,"dude@TWITTER", "bill@TWITTER" ,"dude@TWITTER", "bill@TWITTER" ,"dude@TWITTER", "bill@TWITTER" ,"dude@TWITTER", "bill@TWITTER" ,"dude@TWITTER", "bill@TWITTER" };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( this, R.layout.accountsline, rosterNames );
        rosterListView.setAdapter( adapter );        

    }
    

    public static class Handler implements WSEventHandler
    {
        private Activity activity;
        private WSConnector wsConnector;
        public Handler( Activity activity, WSConnector wsConnector )
        {
            this.activity = activity;
            this.wsConnector = wsConnector;
        }
        @Override
        public void onOpen()
        {
            Toast toast = Toast.makeText(activity.getApplicationContext(), "Connected", Toast.LENGTH_LONG );
            toast.show();
            
            AccountManager accountManager = new AccountManager( activity );
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
            Toast toast = Toast.makeText(activity.getApplicationContext(), "onMessage", Toast.LENGTH_LONG );
            toast.show();
        }
        
        @Override
        public void onMessage( String message )
        {
            Toast toast = Toast.makeText(activity.getApplicationContext(), "onMessage", Toast.LENGTH_LONG );
            toast.show();
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
    
    private static final int MENU_ACCOUNTS = 1;
    private static final int MENU_SIGNOUT = 2;
    
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ACCOUNTS, 0, R.string.menu_accounts );
        menu.add(0, MENU_SIGNOUT, 0, R.string.menu_sign_out );
        return true;
    }
    

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ACCOUNTS:
            Log.v(TAG, "Starting activity");
            Intent i = new Intent( this, AccountList.class);
            startActivity( i );
            return true;
        case MENU_SIGNOUT:
            return true;
        }
        return false;
    }
}