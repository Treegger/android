package com.treegger.android.imonair.activity;

import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.treegger.android.imonair.R;
import com.treegger.android.imonair.component.ImageLoader;
import com.treegger.android.imonair.service.TreeggerService;
import com.treegger.protobuf.WebSocketProto.RosterItem;
import com.treegger.protobuf.WebSocketProto.VCardResponse;

public class RostersView
    extends TreeggerActivity
{
    public static final String TAG = "RosterView";

    @Override
    public void onMessageType( int messageType )
    {
        super.onMessageType( messageType );
        switch ( messageType )
        {
            case TreeggerService.MESSAGE_TYPE_ROSTER_UPDATE:
                updateRosters();
                break;
                
            case TreeggerService.MESSAGE_TYPE_ROSTER_ADAPTER_UPDATE:
            case TreeggerService.MESSAGE_TYPE_TEXTMESSAGE_UPDATE:
            case TreeggerService.MESSAGE_TYPE_VCARD_UPDATE:
            case TreeggerService.MESSAGE_TYPE_PRESENCE_UPDATE:
            case TreeggerService.MESSAGE_TYPE_COMPOSING:
                updateRosterAdapter();
                break;
        }

    }

    
    private void updateRosterAdapter()
    {
        ListView rosterListView = (ListView) findViewById( R.id.roster_list );
        RosterItemAdapter rosterAdapter = (RosterItemAdapter)rosterListView.getAdapter();
        if( rosterAdapter!=null )
        {
            rosterAdapter.sort();
            rosterAdapter.notifyDataSetChanged();                
        }
    }
    private void updateRosters()
    {
        if ( treeggerService != null )
        {
            ListView rosterListView = (ListView) findViewById( R.id.roster_list );
            RosterItemAdapter rosterAdapter = (RosterItemAdapter)rosterListView.getAdapter();
            
            if( rosterAdapter == null )
            {
                List<RosterItem> rosterItemsList = treeggerService.getAllRosterItems();
                if( rosterItemsList != null && rosterItemsList.size() > 0 )
                {
                    rosterAdapter = new RosterItemAdapter( this, R.layout.rosterline, rosterItemsList );
                    rosterAdapter.sort();
                    rosterListView.setAdapter( rosterAdapter );
                }
            }
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
        rosterListView.setOnItemClickListener( new OnItemClickListener()
        {

            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id )
            {
                
                RosterItem rosterItem = (RosterItem) parent.getAdapter().getItem( position );
                Intent intent = new Intent( parent.getContext(), Chat.class );
                intent.addFlags( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
                intent.putExtra( Chat.EXTRA_ROSTER_JID, rosterItem.getJid() );
                startActivity( intent );
            }

        } );

    }

    @Override
    public void onResume()
    {
        super.onResume();
        updateRosters();
        updateRosterAdapter();
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public void onTreeggerService()
    {
        if ( treeggerService.getAccounts().size() == 0 )
        {
            startActivity( new Intent( this, AccountForm.class ) );
        }
        else
        {
            updateRosters();
        }
    }

    public boolean onCreateOptionsMenu( Menu menu )
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.mainmenu, menu);
        return true;
    }

    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch ( item.getItemId() )
        {
            case R.id.menu_accounts:
                startActivity( new Intent( this, AccountList.class ) );
                return true;
            case R.id.menu_sign_out:
                if ( treeggerService != null )
                {
                    treeggerService.sendPresence( "unavailable", "", "" );
                    treeggerService.disconnect();
                }                

                finish();
                return true;
        }
        return false;
    }

    
    
    // ------------------------------------------------------------------------------------------------
    // ------------------------------------------------------------------------------------------------    
    public class RosterItemAdapter
        extends ArrayAdapter<RosterItem>
    {

        public RosterItemAdapter( Context context, int textViewResourceId, List<RosterItem> rosterItems )
        {
            super( context, textViewResourceId, rosterItems );
        }

        public void sort()
        {
            super.sort( new Comparator<RosterItem>()
            {
                @Override
                public int compare( RosterItem item1, RosterItem item2 )
                {
                    int presentType1 = getPresenceType( item1.getJid() );
                    int presentType2 = getPresenceType( item2.getJid() );
                    int typeDelta =  presentType2 - presentType1;
                    if ( typeDelta == 0 )
                        return item1.getName().toLowerCase().compareTo( item2.getName().toLowerCase() );
                    else
                    {
                        if( presentType1 == PRESENCE_TYPE_AVAILABLE ) return -10;
                        if( presentType2 == PRESENCE_TYPE_AVAILABLE ) return 10;
                        else return typeDelta;
                    }
                }

            } );
        }

        public void drawAvatar( ImageView image, String jid )
        {
            if( treeggerService != null )
            {
                VCardResponse vcard = treeggerService.vcards.get( jid );
                if ( vcard != null && vcard.hasPhotoExternal() )
                {
                    ImageLoader.load( getContext(), image, vcard.getPhotoExternal() );
                }
            }
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent )
        {
            LayoutInflater inflater = getLayoutInflater();
            View row = inflater.inflate( R.layout.rosterline, parent, false );
            TextView label = (TextView) row.findViewById( R.id.label );

            RosterItem rosterItem = getItem( position );

            ImageView image = (ImageView) row.findViewById( R.id.photo );
            drawAvatar( image, rosterItem.getJid() );

            ImageView bullet = (ImageView) row.findViewById( R.id.bullet );
            
            
            String text = rosterItem.getName();
            label.setText( text );
            int presenceType = getPresenceType( rosterItem.getJid() );

            switch ( presenceType )
            {
                case PRESENCE_TYPE_AVAILABLE:
                case PRESENCE_TYPE_AWAY:
                case PRESENCE_TYPE_DND:
                    row.setBackgroundColor( 0xff222222 );
                    label.setTextColor( 0xffffffff );
                    break;
                case PRESENCE_TYPE_UNAVAILABLE:
                    row.setBackgroundColor( 0x99222222 );
                    label.setTextColor( 0x88ffffff );
                    break;
            }
            
            if ( treeggerService.hasMessageFrom( rosterItem.getJid() ) )
            {
                bullet.setImageDrawable( getResources().getDrawable(R.drawable.hasmessage) );
                label.setTypeface( Typeface.DEFAULT_BOLD );
                row.setBackgroundColor( 0x440F24BF );
                label.setTextColor( 0xffffffff );
            }
            else
            {
                updatePresenceType( rosterItem.getJid(), bullet );
                label.setTypeface( Typeface.DEFAULT );
            }

            //ImageView icon=(ImageView)row.findViewById(R.id.icon);
            return row;
        }
        
        
        
        

    }


}
