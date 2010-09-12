package com.treegger.android.imonair.activity;


import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ImageView;

import com.treegger.android.imonair.R;
import com.treegger.android.imonair.service.TreeggerService;
import com.treegger.protobuf.WebSocketProto.Presence;

public abstract class TreeggerActivity extends Activity {

    protected TreeggerService treeggerService = null;

    private ServiceConnection onService = new ServiceConnection()
    {
        public void onServiceConnected( ComponentName className, IBinder rawBinder )
        {
            treeggerService = ( (TreeggerService.LocalBinder) rawBinder ).getService();
            updateTitle();
            onTreeggerService();
        }

        public void onServiceDisconnected( ComponentName className )
        {
            treeggerService = null;
        }
    };
    
    public void onTreeggerService()
    {
    }
    
    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        public void onReceive( Context context, Intent intent )
        {
            if( TreeggerService.TREEGGER_BROADCAST_ACTION.equals( intent.getAction() ) )
            {
                updateTitle();
                int messageType = intent.getIntExtra( TreeggerService.EXTRA_MESSAGE_TYPE, -1 );
                onMessageType( messageType );
            }
        }
    };
    
    protected int currentDialog;
    public void onMessageType( int messageType )
    {
        switch( messageType )
        {
            case TreeggerService.MESSAGE_TYPE_CONNECTING:
                currentDialog = TreeggerService.MESSAGE_TYPE_CONNECTING; 
                showDialog( currentDialog );
                break;
            case TreeggerService.MESSAGE_TYPE_AUTHENTICATING:
                currentDialog = TreeggerService.MESSAGE_TYPE_AUTHENTICATING; 
                showDialog( currentDialog );
                break;

            case TreeggerService.MESSAGE_TYPE_CONNECTING_FINISHED:
            case TreeggerService.MESSAGE_TYPE_AUTHENTICATING_FINISHED:
            case TreeggerService.MESSAGE_TYPE_DISCONNECTED:
                removeDialog( currentDialog );
            
            case TreeggerService.MESSAGE_TYPE_PAUSED:
                break;
        }
    }
    
   
    public void updateTitle()
    {
        if( treeggerService != null ) getWindow().setTitle( getString( R.string.app_name )+" " + treeggerService.getConnectionStates() );
    }
    
    @Override
    public void onStart()
    {
        super.onStart();
    }
    
    
    @Override
    public void onResume()
    {
        super.onResume();
        updateTitle();
        registerReceiver( receiver, new IntentFilter( TreeggerService.TREEGGER_BROADCAST_ACTION ) );
    }

    @Override
    public void onPause()
    {
        super.onPause();
        unregisterReceiver( receiver );
    }

    @Override
    public void onStop()
    {
        super.onStop();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        bindService( new Intent( this, TreeggerService.class ), onService, BIND_AUTO_CREATE );
    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        unbindService( onService );
    }

    
    
    private ProgressDialog buildDialog( String title, String message )
    {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        return dialog;
        
    }

    @Override
    protected Dialog onCreateDialog( int dialogType ) 
    {
        switch( dialogType )
        {
            case TreeggerService.MESSAGE_TYPE_CONNECTING:
                return buildDialog( null, getString( R.string.dialog_connection) );
            case TreeggerService.MESSAGE_TYPE_AUTHENTICATING:
                return buildDialog( null, getString(R.string.dialog_authentication) );
        }
        return null;
    }
    
    
    // ------------------------------------------------------------------------------------------------
    // ------------------------------------------------------------------------------------------------
    protected static final int PRESENCE_TYPE_UNAVAILABLE = 0;
    protected static final int PRESENCE_TYPE_AVAILABLE = 1;
    protected static final int PRESENCE_TYPE_AWAY = 2;
    protected static final int PRESENCE_TYPE_DND = 3;
    
    protected int getPresenceType( String jid )
    {
        if( treeggerService != null )
        {
            Presence presence = treeggerService.getPresence( jid );
            if ( presence != null )
            {
                String presenceStatus = presence.getStatus();
                if ( presenceStatus != null )
                {
                    String presenceShow = presence.getShow();
                    
                    if ( presenceShow.equalsIgnoreCase( "away" ) || presenceShow.equalsIgnoreCase( "xa" ) )
                    {
                        return PRESENCE_TYPE_AWAY;
                    }
                    else if ( presenceShow.equalsIgnoreCase( "dnd" ) )
                    {
                        return PRESENCE_TYPE_DND;
                    }
                    else
                    {
                        return PRESENCE_TYPE_AVAILABLE;
                    }
                }
            }
        }
        return PRESENCE_TYPE_UNAVAILABLE;
        
    }
    protected void updatePresenceType( String jid, ImageView bullet )
    {
        if( treeggerService != null )
        {
            
            if( treeggerService.isComposing( jid ) )
            {
                bullet.setImageDrawable( getResources().getDrawable(R.drawable.composing) );
            }
            else
            {
                int presenceType = getPresenceType( jid );
                switch ( presenceType )
                {
                    case PRESENCE_TYPE_AVAILABLE:
                        bullet.setImageDrawable( getResources().getDrawable(R.drawable.bullet_green) );
                        break;
                    case PRESENCE_TYPE_AWAY:
                        bullet.setImageDrawable( getResources().getDrawable(R.drawable.bullet_yellow) );
                        break;
                    case PRESENCE_TYPE_DND:
                        bullet.setImageDrawable( getResources().getDrawable(R.drawable.bullet_red) );
                        break;
                    case PRESENCE_TYPE_UNAVAILABLE:
                        bullet.setImageDrawable( getResources().getDrawable(R.drawable.bullet_grey) );
                        break;
                }
            }
        }
    }

}
