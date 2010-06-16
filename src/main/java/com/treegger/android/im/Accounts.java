package com.treegger.android.im;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class Accounts extends ListActivity {
    
    public static final String TAG = "Accounts";

    static final String[] ACCOUNTS = new String[] { "Add an account..." };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
       Log.v(TAG, "Activity State: onCreate()");

      super.onCreate(savedInstanceState);
      
      setListAdapter( new ArrayAdapter<String>( this, R.layout.accountsline, ACCOUNTS ) );

      ListView lv = getListView();
      //lv.setTextFilterEnabled(true);

      final Intent i =  new Intent( this, AccountForm.class ) ;
      
      lv.setOnItemClickListener(new OnItemClickListener()
      {
        @Override
        public void onItemClick(AdapterView<?> parent, View view,
            int position, long id) 
        {
            startActivity( i );
        }

      });
    }

    
    
}
