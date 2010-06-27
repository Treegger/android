package com.treegger.android.im.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.treegger.android.im.R;
import com.treegger.android.im.service.WebSocketManager;
import com.treegger.protobuf.WebSocketProto.Roster;
import com.treegger.protobuf.WebSocketProto.RosterItem;
import com.treegger.protobuf.WebSocketProto.WebSocketMessage;

public class AndroIM extends TreeggerActivity 
{
    public static final String TAG = "AndroIM";

    
    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        public void onReceive( Context context, Intent intent )
        {
            while( ! treeggerService.messagesQueue.isEmpty() )
            {
                WebSocketMessage message = treeggerService.messagesQueue.poll();
                
                if( message.hasRoster() )
                {
                    updateRoster( message.getRoster() );
                }
                
            }
        }
    };
    
    
    
    private void updateRoster( Roster roster )
    {
        final String[] rosterNames = new String[roster.getItemCount()];
        int i = 0;
        for( RosterItem rosterItem : roster.getItemList() )
        {
            rosterNames[i++] = rosterItem.getName();
        }
        
        ListView rosterListView = (ListView)findViewById( R.id.roster_list );
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( this, R.layout.accountsline, rosterNames );
        rosterListView.setAdapter( adapter );        
        
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
    @Override
    public void onResume()
    {
        super.onResume();
        registerReceiver( receiver, new IntentFilter( WebSocketManager.BROADCAST_ACTION ) );
    }

    @Override
    public void onPause()
    {
        super.onPause();
        unregisterReceiver( receiver );
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
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