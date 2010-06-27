package com.treegger.android.im.activity;

import java.util.List;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.treegger.android.im.R;
import com.treegger.android.im.service.Account;

public class AccountForm extends TreeggerActivity 
{
    
    public static final String TAG = "AccountForm";

    static final String[] SOCIAL_NETWORKS = new String[] { "TWITTER", "FOURSQUARE" };

    @Override
    public void onCreate(Bundle savedInstanceState) {
       Log.v(TAG, "Activity State: onCreate()");
       super.onCreate(savedInstanceState);
       setContentView(R.layout.accountform );

       
       
       Button cancel = (Button)findViewById(R.id.button_cancel);
       cancel.setOnClickListener(new View.OnClickListener() {
           public void onClick(View v) {
               finish();
           }
       });
    }
    
    
    @Override
    protected void onTreeggerService()
    {
        super.onTreeggerService();

        int accountPosition = getIntent().getIntExtra( "accountPosition", -1 );
        List<Account> accountsList = treeggerService.getAccounts();
        
        Account account = null;
        if( accountsList.size() > 0 ) account = treeggerService.getAccounts().get( accountPosition );
        
        if( account != null )
        {
            ( (EditText)findViewById( R.id.input_name ) ).setText( account.name );
            ( (EditText)findViewById( R.id.input_password ) ).setText( account.password );
            for( int i = 0; i<SOCIAL_NETWORKS.length; i++ )
            {
                if( SOCIAL_NETWORKS[i].equalsIgnoreCase( account.socialnetwork ) )
                {
                    ( (Spinner)findViewById( R.id.input_socialnetwork ) ).setSelection( i );
                    break;
                }
            }
            
        }
        
        
        Spinner spinner = (Spinner) findViewById(R.id.input_socialnetwork);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( this, android.R.layout.simple_spinner_item, SOCIAL_NETWORKS );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        
        Button add = (Button)findViewById(R.id.button_add);
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Account account = new Account();
                account.name = ( (EditText)findViewById( R.id.input_name ) ).getText().toString();
                account.password = ( (EditText)findViewById( R.id.input_password ) ).getText().toString();
                account.socialnetwork = ( (Spinner)findViewById( R.id.input_socialnetwork ) ).getSelectedItem().toString();
                treeggerService.addAccount( account );
                finish();
            }
        });
        
    }
    
    
    
    
}
