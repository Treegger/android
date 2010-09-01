package com.treegger.android.imonair.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.treegger.android.imonair.R;
import com.treegger.android.imonair.service.Account;

public class AccountForm
    extends TreeggerActivity
{

    public static final String TAG = "AccountForm";

    public static final String EXTRA_ACCOUNT_ID = "accountId";

    static final String[] SOCIAL_NETWORKS = new String[] { "Twitter", "Foursquare" };

    private Account account = null;
    private boolean newAccount = true;
    
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        Log.v( TAG, "Activity State: onCreate()" );
        super.onCreate( savedInstanceState );
        setContentView( R.layout.accountform );

        Button cancel = (Button) findViewById( R.id.button_cancel );
        cancel.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v )
            {
                finish();
            }
        } );
    }

    @Override
    public void onTreeggerService()
    {
        super.onTreeggerService();

        Long accountId = getIntent().getLongExtra( EXTRA_ACCOUNT_ID, -1 );

        if ( accountId >= 0 )
        {
            account = treeggerService.findAccountById( accountId );
        }

        if ( account == null )
        {
            newAccount = true;
        }
        else
        {
            newAccount = false;
            ( (EditText) findViewById( R.id.input_name ) ).setText( account.name );
            ( (EditText) findViewById( R.id.input_password ) ).setText( account.password );
            for ( int i = 0; i < SOCIAL_NETWORKS.length; i++ )
            {
                if ( SOCIAL_NETWORKS[i].equalsIgnoreCase( account.socialnetwork ) )
                {
                    ( (Spinner) findViewById( R.id.input_socialnetwork ) ).setSelection( i );
                    break;
                }
            }

        }

        Spinner spinner = (Spinner) findViewById( R.id.input_socialnetwork );
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( this, android.R.layout.simple_spinner_item, SOCIAL_NETWORKS );
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
        spinner.setAdapter( adapter );

        Button addUpdateButton = (Button) findViewById( R.id.button_add );
        if( newAccount ) addUpdateButton.setText( R.string.button_add );
        else addUpdateButton.setText( R.string.button_update );
        
        addUpdateButton.setOnClickListener( new View.OnClickListener()
        {
            public void onClick( View v )
            {
                if( account == null )
                {
                    account = new Account();
                }
                account.name = ( (EditText) findViewById( R.id.input_name ) ).getText().toString();
                account.password = ( (EditText) findViewById( R.id.input_password ) ).getText().toString();
                account.socialnetwork = ( (Spinner) findViewById( R.id.input_socialnetwork ) ).getSelectedItem().toString();
                
                if( newAccount ) treeggerService.addAccount( account );
                else treeggerService.updateAccount( account );
                finish();
            }
        } );

    }

}
