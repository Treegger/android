package com.treegger.android.im.activity;

import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.treegger.android.im.R;
import com.treegger.android.im.service.Account;
import com.treegger.android.im.service.AccountStorage;

public class AccountList extends ListActivity {
    
    public static final String TAG = "AccountList";

    private static final int CONTEXT_MENU_DELETE_ACCOUNT = 1;
    private static final int CONTEXT_MENU_CANCEL = 2;
        
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Activity State: onCreate()");

        super.onCreate(savedInstanceState);
        
    }

    @Override
    public void onResume() {
        Log.v(TAG, "Activity State: onResume()");
        super.onResume();
        updateListAdatpter();
        final Intent intent =  new Intent( this, AccountForm.class ) ;
        
        ListView lv = getListView();
        lv.setOnItemClickListener(new OnItemClickListener()
        {
          @Override
          public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
          {
              intent.putExtra( "accountPosition", position );
              startActivity( intent );
          }
        
        });
        
        lv.setOnCreateContextMenuListener( new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo )
            {
                AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo)menuInfo;
                
                if( adapterMenuInfo.position != getListAdapter().getCount() -1 )
                {
                    menu.add( 0, CONTEXT_MENU_DELETE_ACCOUNT, 0, "Delete Account");
                    menu.add( 0, CONTEXT_MENU_CANCEL, 0, "Cancel");                    
                }
            }
        });

    }

    
    private void updateListAdatpter()
    {
        final String[] accountNames = getAccounts();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( this, R.layout.accountsline, accountNames );
        setListAdapter( adapter );        
    }
    
    private String[] getAccounts()
    {
        AccountStorage accountManager = new AccountStorage( this );
        List<Account> accountList = accountManager.getAccounts();
        final int size = accountList.size()+1;
        final String[] accountsArray = new String[size];
        int i = 0;
        for( Account account : accountList )
        {
            accountsArray[i] = account.name + "@" + account.socialnetwork; 
            i++;
        }
        accountsArray[i] = "Add an account..." ;
        return accountsArray;
    }

    
    @Override
    public boolean onContextItemSelected(MenuItem menuItem) 
    {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) menuItem.getMenuInfo();

        switch (menuItem.getItemId()) 
        {
            case CONTEXT_MENU_DELETE_ACCOUNT:
                AccountStorage accountManager = new AccountStorage( this );
                accountManager.removeAccount( menuInfo.position );
                updateListAdatpter();
                return true;
        }
        return false;
    }

    
}
