package com.treegger.android.im.activity;

import java.util.ArrayList;
import java.util.List;

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
import android.widget.Spinner;

import com.treegger.android.im.R;
import com.treegger.android.im.service.TreeggerService;
import com.treegger.protobuf.WebSocketProto.Roster;
import com.treegger.protobuf.WebSocketProto.RosterItem;

public class AndroIM extends TreeggerActivity 
{
    public static final String TAG = "AndroIM";

    
    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        public void onReceive( Context context, Intent intent )
        {
            int messageType = intent.getIntExtra( TreeggerService.MESSAGE_TYPE_EXTRA, -1 );
            if( messageType == TreeggerService.MESSAGE_TYPE_ROSTER_UPDATE )
            {
                updateRosters();
            }
            
        }
    };
    
    
    
    private void updateRosters()
    {
        if( treeggerService != null )
        {
            List<String> rosterNamesList = new ArrayList<String>();
            for( Roster roster : treeggerService.getRosters().values() )
            {
                for( RosterItem rosterItem : roster.getItemList() )
                {
                    rosterNamesList.add( rosterItem.getName() );
                }
                
            }
            final String[] rosterNames = rosterNamesList.toArray( new String[0] );
            
            ListView rosterListView = (ListView)findViewById( R.id.roster_list );
            ArrayAdapter<String> adapter = new ArrayAdapter<String>( this, R.layout.accountsline, rosterNames );
            rosterListView.setAdapter( adapter );        
        }        
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Spinner presenceSpinner = (Spinner) findViewById( R.id.presence_bar );
        String[] presenceTypes = getResources().getStringArray(R.array.presenceType );
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( this, android.R.layout.simple_spinner_item, presenceTypes );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presenceSpinner.setAdapter( adapter );
        presenceSpinner.setSelection( 0 );
    }
    
    @Override
    public void onResume()
    {
        super.onResume();
        updateRosters();
        registerReceiver( receiver, new IntentFilter( TreeggerService.BROADCAST_ACTION ) );
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