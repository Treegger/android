package com.treegger.android.imonair.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.treegger.android.imonair.R;
import com.treegger.android.imonair.service.Account;

public class AccountList
    extends TreeggerActivity
{

    public static final String TAG = "AccountList";

    private static final int CONTEXT_MENU_DELETE_ACCOUNT = 1;

    private static final int CONTEXT_MENU_CANCEL = 2;

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        Log.v( TAG, "Activity State: onCreate()" );

        super.onCreate( savedInstanceState );
        setContentView( R.layout.accountslist );

    }

    @Override
    public void onResume()
    {
        Log.v( TAG, "Activity State: onResume()" );
        super.onResume();

        final Intent intent = new Intent( this, AccountForm.class );

        final ListView lv = updateListView();

        lv.setOnItemClickListener( new OnItemClickListener()
        {
            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id )
            {
                Account account = (Account)lv.getAdapter().getItem( position );
                intent.putExtra( AccountForm.EXTRA_ACCOUNT_ID, account.id );
                
                startActivity( intent );
            }

        } );

        lv.setOnCreateContextMenuListener( new OnCreateContextMenuListener()
        {
            @Override
            public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo )
            {
                AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) menuInfo;

                if ( adapterMenuInfo.position != lv.getAdapter().getCount() - 1 )
                {
                    menu.add( 0, CONTEXT_MENU_DELETE_ACCOUNT, 0, "Delete Account" );
                    menu.add( 0, CONTEXT_MENU_CANCEL, 0, "Cancel" );
                }
            }
        } );

    }

    @Override
    public void onTreeggerService()
    {
        super.onTreeggerService();
        updateListView();
    }

    private ListView updateListView()
    {
        ListView lv = (ListView) findViewById( R.id.accounts_list );
        if ( treeggerService != null )
        {
            List<Account> accounts = treeggerService.getAccounts();
            Account addAccountItem = new Account();
            addAccountItem.name = "Add account...";
            List<Account> adapterList = new ArrayList<Account>( accounts );
            adapterList.add( addAccountItem );
            lv.setAdapter( new AccountAdapter( this,  R.layout.accountline, adapterList ) );
        }
        return lv;
    }


    @Override
    public boolean onContextItemSelected( MenuItem menuItem )
    {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) menuItem.getMenuInfo();

        switch ( menuItem.getItemId() )
        {
            case CONTEXT_MENU_DELETE_ACCOUNT:
                ListView lv = (ListView) findViewById( R.id.accounts_list );
                Account account = (Account)lv.getAdapter().getItem( menuInfo.position );
                if( account.socialnetwork != null ) treeggerService.removeAccount( account );
                updateListView();
                return true;
        }
        return false;
    }

    public class AccountAdapter
        extends ArrayAdapter<Account>
    {

        public AccountAdapter( Context context, int textViewResourceId, List<Account> accounts )
        {        
            super( context, textViewResourceId, accounts );
        }

        @Override
        public View getView( int position, View convertView, ViewGroup parent )
        {
            LayoutInflater inflater = getLayoutInflater();
            View row = inflater.inflate( R.layout.accountline, parent, false );
            TextView label = (TextView) row.findViewById( R.id.label );
            
            Account account = getItem( position );
            String text = account.name;
            if( account.socialnetwork != null ) text += "@" + getItem( position ).socialnetwork.toLowerCase();
            label.setText( text );

            //ImageView icon=(ImageView)row.findViewById(R.id.icon);
            return row;
        }
    }
}
