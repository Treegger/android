package com.treegger.android.im.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.treegger.android.im.R;
import com.treegger.android.im.service.TreeggerService;
import com.treegger.protobuf.WebSocketProto.Roster;
import com.treegger.protobuf.WebSocketProto.RosterItem;

public class AndroIM
    extends TreeggerActivity
{
    public static final String TAG = "AndroIM";

    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        public void onReceive( Context context, Intent intent )
        {
            int messageType = intent.getIntExtra( TreeggerService.MESSAGE_TYPE_EXTRA, -1 );
            if ( messageType == TreeggerService.MESSAGE_TYPE_ROSTER_UPDATE )
            {
                updateRosters();
            }

        }
    };

    private void updateRosters()
    {
        if ( treeggerService != null )
        {
            List<RosterItem> rosterItemsList = new ArrayList<RosterItem>();
            for ( Roster roster : treeggerService.getRosters().values() )
            {
                for ( RosterItem rosterItem : roster.getItemList() )
                {
                    rosterItemsList.add( rosterItem );
                }

            }

            ListView rosterListView = (ListView) findViewById( R.id.roster_list );
            rosterListView.setAdapter( new RosterItemAdapter( this, R.layout.accountsline, rosterItemsList ) );
        }
    }

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.main );

        Spinner presenceSpinner = (Spinner) findViewById( R.id.presence_bar );
        String[] presenceTypes = getResources().getStringArray( R.array.presenceType );
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( this, android.R.layout.simple_spinner_item,
                                                                 presenceTypes );
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
        presenceSpinner.setAdapter( adapter );

        presenceSpinner.setOnItemSelectedListener( new OnItemSelectedListener()
        {
            @Override
            public void onItemSelected( AdapterView<?> parent, View view, int pos, long id )
            {
                if ( treeggerService != null )
                {
                    String type = "";
                    String show = "";
                    String status = "";

                    String spinnerPresence = parent.getItemAtPosition( pos ).toString();
                    if ( spinnerPresence.equalsIgnoreCase( "Do no disturb" ) )
                    {
                        show = "dnd";
                    }
                    else if ( spinnerPresence.equalsIgnoreCase( "Away" ) )
                    {
                        show = "away";
                    }
                    treeggerService.sendPresence( type, show, status );
                }
            }

            @Override
            public void onNothingSelected( AdapterView<?> parent )
            {
            }

        } );
        
        
        
        ListView rosterListView = (ListView) findViewById( R.id.roster_list );
        rosterListView.setOnItemClickListener(  new OnItemClickListener()
        {

            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id )
            {
                RosterItem rosterItem = (RosterItem)parent.getAdapter().getItem( position );
                Intent intent = new Intent( parent.getContext(), Chat.class );
                intent.putExtra( Chat.EXTRA_ROSTER_JID, rosterItem.getJid() );
                startActivity( intent );
            }
            
        });
        
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

    public boolean onCreateOptionsMenu( Menu menu )
    {
        menu.add( 0, MENU_ACCOUNTS, 0, R.string.menu_accounts );
        menu.add( 0, MENU_SIGNOUT, 0, R.string.menu_sign_out );
        return true;
    }

    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch ( item.getItemId() )
        {
            case MENU_ACCOUNTS:
                Log.v( TAG, "Starting activity" );
                startActivity( new Intent( this, AccountList.class ) );
                return true;
            case MENU_SIGNOUT:
                return true;
        }
        return false;
    }
    
    
    
    public class RosterItemAdapter extends ArrayAdapter<RosterItem>
{

    public RosterItemAdapter( Context context, int textViewResourceId, List<RosterItem> rosterItems )
    {        
        super( context, textViewResourceId, rosterItems );
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent )
    {
        LayoutInflater inflater = getLayoutInflater();
        View row = inflater.inflate( R.layout.accountsline, parent, false );
        TextView label = (TextView) row.findViewById( R.id.label );
        
        RosterItem rosterItem = getItem( position );
        String text = rosterItem.getName();
        label.setText( text );

        //ImageView icon=(ImageView)row.findViewById(R.id.icon);
        return row;
    }
}

}