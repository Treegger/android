package com.treegger.android.imonair.activity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.treegger.android.imonair.R;
import com.treegger.android.imonair.component.ImageLoader;
import com.treegger.android.imonair.service.ChatMessage;
import com.treegger.android.imonair.service.TreeggerService;
import com.treegger.protobuf.WebSocketProto.VCardResponse;

public class Chat
    extends TreeggerActivity
{

    public static final String TAG = "Chat";

    public static final String EXTRA_ROSTER_JID = "rosterJID";
    
    private String jid;
    
    @Override
    public void onMessageType( int messageType )
    {
        //super.onMessageType( messageType );
        switch ( messageType )
        {
            case TreeggerService.MESSAGE_TYPE_TEXTMESSAGE_UPDATE:
                updateChatMessage();
                break;
            case TreeggerService.MESSAGE_TYPE_PRESENCE_UPDATE:
            case TreeggerService.MESSAGE_TYPE_COMPOSING:
                updatePresenceTitle();
                break;
        }

    }

    private void updatePresenceTitle()
    {
        ImageView bullet = (ImageView) findViewById( R.id.window_bullet );
        updatePresenceType( jid, bullet );    
    }

    @Override
    public void updateTitle()
    {
        if( treeggerService != null )
        {
            if( treeggerService != null )
            {
                TextView title = (TextView) findViewById( R.id.window_title );
                title.setText( getString(R.string.app_name)+" " + treeggerService.getConnectionStates() );
            }
        }
    }

    
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        Log.v( TAG, "Activity State: onCreate()" );
        super.onCreate( savedInstanceState );

        boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        
        setContentView( R.layout.chat );

        if( customTitleSupported )
        {
            getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.windowtitle);
        }
        

        
        final TextView textInput = (TextView) findViewById( R.id.input_chat );
        textInput.addTextChangedListener( new TextWatcher()
        {
            
            @Override
            public void onTextChanged( CharSequence s, int start, int before, int count )
            {
                if( s.length() > 0 )
                {
                    composingMessage();
                }
                else
                {
                    stopComposingMessage();
                }
               
            }
            
            @Override
            public void beforeTextChanged( CharSequence s, int start, int count, int after )
            {
            }
            
            @Override
            public void afterTextChanged( Editable s )
            {
            }
        });

    }

    @Override
    public void onResume()
    {
        Log.v( TAG, "Activity State: onResume()" );
        super.onResume();

        jid = getIntent().getStringExtra( EXTRA_ROSTER_JID );
        if( treeggerService != null )
        {
            treeggerService.setVisibleChat( jid );
            updateChatMessage();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if( treeggerService != null ) treeggerService.setVisibleChat( null );
        stopComposingMessage();
    }

    @Override
    public void onTreeggerService()
    {
        super.onTreeggerService();
        
        VCardResponse vcard = treeggerService.vcards.get( jid );
        TextView username = (TextView) findViewById( R.id.window_username );
        if( vcard != null ) username.setText( vcard.getFn() );
        updatePresenceTitle();

        
        treeggerService.setVisibleChat( jid );
        updateChatMessage();
        
        Button button = (Button) findViewById( R.id.button_send );
        button.setOnClickListener( new OnClickListener()
        {
            public void onClick( View v )
            {
                sendMessage();
            }
        } );
        
        TextView view = (TextView) findViewById( R.id.input_chat );
        
        view.setOnEditorActionListener(new TextView.OnEditorActionListener() { 
            @Override
            public boolean onEditorAction( TextView v, int actionId, KeyEvent event )
            {
                if (actionId == EditorInfo.IME_ACTION_SEND) { 
                    sendMessage(); 
                } 
                return false; 
            } 
        }); 
    }

    
    private Boolean composing = false;
    private Timer composingTimer;
    public AtomicLong lastComposingActivity = new AtomicLong();

    private class ComposingTask extends TimerTask
    {
        @Override
        public void run()
        {
            long now = System.currentTimeMillis();
            if( lastComposingActivity.get() + 5*1000 < now  )
                stopComposingMessage();
        }
    };

    
    private void composingMessage()
    {
        if( treeggerService != null)
        {
            synchronized ( composing )
            {                
                lastComposingActivity.set( System.currentTimeMillis() );
                if( !composing )
                {
                    composing = true;
                    composingTimer = new Timer();
                    composingTimer.schedule( new ComposingTask(), 1000, 1000 );
                    treeggerService.sendStateNotificationMessage( jid, true, false, false, false );
                }
            }
        }
    }
    private void stopComposingMessage()
    {
        if( treeggerService != null )
        {
            synchronized ( composing )
            {                
                if( composing )
                {
                    treeggerService.sendStateNotificationMessage( jid, false, false, true, false );
                    if( composingTimer != null )
                    {
                        composingTimer.cancel();
                        composingTimer.purge();
                        composingTimer = null;
                    }
                    composing = false;
                }
            }
        }
    }
    
    private void sendMessage()
    {
        // Send a message using content of the edit text widget
        TextView view = (TextView) findViewById( R.id.input_chat );
        String message = view.getText().toString();
        sendMessage( message );
        view.clearComposingText();
        view.setText( null );        
    }
    private void sendMessage( String message )
    {
        // Check that there's actually something to send
        if ( message.length() > 0 )
        {
            treeggerService.sendTextMessage( jid, message );
        }
    }

    private void updateChatMessage()
    {
        treeggerService.markHasReadMessageFrom( jid );
        
        
        
        ListView chatList = (ListView) findViewById( R.id.chat_list );
        
        
        chatList.setOnCreateContextMenuListener( new OnCreateContextMenuListener()
        {
            @Override
            public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo )
            {
                AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) menuInfo;
                
                TextView label = (TextView)adapterMenuInfo.targetView.findViewById( R.id.message );
                
                Pattern pattern = Pattern.compile("\\b((http[s]?://|www\\.)\\S+)\\b");
                Matcher matcher = pattern.matcher( label.getText().toString() );
                int i = 0;
                while (matcher.find()) 
                {
                    menu.add( 0, i++, 0, matcher.group() );
                }
            }
        } );

        
        ChatMessageAdapter chatMessageAdapter = (ChatMessageAdapter)chatList.getAdapter();
        if( chatMessageAdapter == null )
        {
            List<ChatMessage> textMessages = treeggerService.getTextMessageList( jid );
            if( textMessages != null )
            {
                chatMessageAdapter = new ChatMessageAdapter( this, R.layout.chatmessage, textMessages );
                chatList.setAdapter( chatMessageAdapter );
            }
        }
        else
        {
            chatMessageAdapter.notifyDataSetChanged();
        }
    }
    
    
    @Override
    public boolean onContextItemSelected( MenuItem menuItem )
    {
        String url = menuItem.getTitle().toString();
        Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse( url ) );
        startActivity( intent);
        return false;
    }

    
    public class ChatMessageAdapter extends ArrayAdapter<ChatMessage>
    {
        private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        
        public ChatMessageAdapter( Context context, int textViewResourceId, List<ChatMessage> messages )
        {
            super( context, textViewResourceId, messages );
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
            View row = inflater.inflate( R.layout.chatmessage, parent, false );
            row.setBackgroundColor( 0xff222222 );
            
            ChatMessage message = getItem( position );
            
            TextView label = (TextView) row.findViewById( R.id.message );
            label.setText( message.text );

            TextView dateLabel = (TextView) row.findViewById( R.id.date );
            dateLabel.setText( sdf.format( message.date ) );
            
            ImageView photo=(ImageView)row.findViewById(R.id.photo);
            drawAvatar( photo, message.userAndHost );
            return row;
        }

        
    }

}
