package com.treegger.android.imonair.activity;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
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
import com.treegger.android.imonair.service.TreeggerService;

public class AccountList
    extends TreeggerActivity
{

    public static final String TAG = "AccountList";

    private static final int CONTEXT_MENU_DELETE_ACCOUNT = 1;

    private static final int CONTEXT_MENU_CANCEL = 2;

    
    @Override
    public void onMessageType( int messageType )
    {
        super.onMessageType( messageType );
        updateListView();
    }

    
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


        final ListView lv = updateListView();

        lv.setOnItemClickListener( new OnItemClickListener()
        {
            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id )
            {
                Account account = (Account)lv.getAdapter().getItem( position );
                Intent intent = new Intent( view.getContext(), AccountForm.class );
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
                    menu.add( 0, CONTEXT_MENU_DELETE_ACCOUNT, 0, R.string.accountlist_delete );
                    menu.add( 0, CONTEXT_MENU_CANCEL, 0, R.string.accountlist_cancel );
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

    
    private List<Account> adapterList;
    
    private ListView updateListView()
    {
        ListView lv = (ListView) findViewById( R.id.accounts_list );
        if ( treeggerService != null )
        {
            AccountAdapter accountAdapter = (AccountAdapter)lv.getAdapter();
            List<Account> accounts = treeggerService.getAccounts();
            if( accountAdapter == null )
            {
                adapterList = new ArrayList<Account>( accounts );
                Account addAccountItem = new Account();
                adapterList.add( addAccountItem );
                
                addAccountItem.name = getString( R.string.accountlist_add );
                accountAdapter = new AccountAdapter( this,  R.layout.accountline, adapterList );
                lv.setAdapter( accountAdapter );
            }
            else
            {
                for( Account account : accounts )
                {
                    if( !adapterList.contains( account ) )
                    {
                        accountAdapter.insert( account, accountAdapter.getCount()-1 );
                    }
                }
            }
            
            accountAdapter.notifyDataSetChanged();
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
                AccountAdapter accountAdapter = (AccountAdapter)lv.getAdapter();
                Account account = accountAdapter.getItem( menuInfo.position );
                if( account.socialnetwork != null )
                {
                    accountAdapter.remove( account );
                    treeggerService.removeAccount( account );
                }
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
            View row = getLayoutInflater().inflate( R.layout.accountline, parent, false );
            row.setBackgroundColor( 0xff222222 );
            
            TextView label = (TextView) row.findViewById( R.id.label );
            Account account = getItem( position );
            String text = account.name;
            if( account.socialnetwork != null ) text += "@" + getItem( position ).socialnetwork.toLowerCase();
            label.setTextColor( 0xffffffff );
            label.setText( text );

            if( treeggerService != null && account.socialnetwork != null  )
            {
                TextView status = (TextView) row.findViewById( R.id.status );
                String statusStr = treeggerService.getConnectionStateName( account );
                if( statusStr != null ) status.setText( statusStr );
            }
            
            return row;
        }
    }
}
