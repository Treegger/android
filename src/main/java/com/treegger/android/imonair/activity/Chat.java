package com.treegger.android.imonair.activity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
        if ( messageType == TreeggerService.MESSAGE_TYPE_TEXTMESSAGE_UPDATE )
        {
            updateChatMessage();
        }
    }

    @Override
    public void updateTitle()
    {
        if( treeggerService != null )
        {
            VCardResponse vcard = treeggerService.vcards.get( jid );
            getWindow().setTitle( "IMonAir " + treeggerService.getConnectionStates() + " - "+ vcard.getFn() );
        }
    }

    
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        Log.v( TAG, "Activity State: onCreate()" );
        super.onCreate( savedInstanceState );

        setContentView( R.layout.chat );

        jid = getIntent().getStringExtra( EXTRA_ROSTER_JID );
        
    }

    @Override
    public void onResume()
    {
        Log.v( TAG, "Activity State: onResume()" );
        super.onResume();
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
    }

    @Override
    public void onTreeggerService()
    {
        super.onTreeggerService();

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

    private void sendMessage()
    {
        // Send a message using content of the edit text widget
        TextView view = (TextView) findViewById( R.id.input_chat );
        String message = view.getText().toString();
        sendMessage( message );
        view.clearComposingText();
        view.setText( null );        
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
                
                Pattern pattern = Pattern.compile("\\b((http://|www\\.)\\S+)\\b");
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
    
    private void sendMessage( String message )
    {
        // Check that there's actually something to send
        if ( message.length() > 0 )
        {
            treeggerService.sendTextMessage( jid, message );
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
                    ImageLoader.load( image, vcard.getPhotoExternal() );
                }
            }
        }

        
        
        @Override
        public View getView( int position, View convertView, ViewGroup parent )
        {
            LayoutInflater inflater = getLayoutInflater();
            View row = inflater.inflate( R.layout.chatmessage, parent, false );

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
