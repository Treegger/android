package com.treegger.android.im.activity;

import java.util.List;

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

public class AccountList extends TreeggerActivity {
    
    public static final String TAG = "AccountList";

    private static final int CONTEXT_MENU_DELETE_ACCOUNT = 1;
    private static final int CONTEXT_MENU_CANCEL = 2;
        
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Activity State: onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.accountslist);

    }

    @Override
    public void onResume() {
        Log.v(TAG, "Activity State: onResume()");
        super.onResume();

        final Intent intent =  new Intent( this, AccountForm.class ) ;
        
        final ListView lv = updateListView();

        
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
                
                if( adapterMenuInfo.position != lv.getAdapter().getCount() -1 )
                {
                    menu.add( 0, CONTEXT_MENU_DELETE_ACCOUNT, 0, "Delete Account");
                    menu.add( 0, CONTEXT_MENU_CANCEL, 0, "Cancel");                    
                }
            }
        });

    }

    @Override
    protected void onTreeggerService()
    {
        super.onTreeggerService();
        updateListView();
    }
    
    private ListView updateListView()
    {
        ListView lv = (ListView)findViewById( R.id.accounts_list );
        if( treeggerService != null )
        {
            final String[] accountNames = getAccounts();
            ArrayAdapter<String> adapter = new ArrayAdapter<String>( this, R.layout.accountsline, accountNames );
            lv.setAdapter( adapter );
        }
        return lv;
    }
    
    private String[] getAccounts()
    {
        List<Account> accountList = treeggerService.getAccounts();
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
                treeggerService.removeAccount( treeggerService.getAccounts().get(menuInfo.position ) );
                updateListView();
                return true;
        }
        return false;
    }

    
}
