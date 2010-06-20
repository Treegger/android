package com.treegger.android.im;

import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class AccountList extends ListActivity {
    
    public static final String TAG = "AccountList";

    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Activity State: onCreate()");

        super.onCreate(savedInstanceState);
        
    }

    @Override
    public void onResume() {
        Log.v(TAG, "Activity State: onResume()");
        super.onResume();
        final String[] accountNames = getAccounts();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( this, R.layout.accountsline, accountNames );
        setListAdapter( adapter );        

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
        
        lv.setOnItemLongClickListener( new OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) 
            {
                if( position > accountNames.length - 1 ) return false;
                return true;
            }
            
        });


    
    }

    private String[] getAccounts()
    {
        AccountManager accountManager = new AccountManager( this );
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
    
}
