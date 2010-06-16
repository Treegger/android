package com.treegger.android.im;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class Accounts extends ListActivity {
    
    public static final String TAG = "Accounts";

    static final String[] ACCOUNTS = new String[] { "TWITTER", "FOURSQUARE", "Add an account..." };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
       Log.v(TAG, "Activity State: onCreate()");

      super.onCreate(savedInstanceState);
      //setContentView(R.layout.accounts);
      
      setListAdapter( new ArrayAdapter<String>( this, R.layout.accountsline, ACCOUNTS ) );

      ListView lv = getListView();
      //lv.setTextFilterEnabled(true);

      lv.setOnItemClickListener(new OnItemClickListener()
      {
        @Override
        public void onItemClick(AdapterView<?> parent, View view,
            int position, long id) {
          // When clicked, show a toast with the TextView text
          Toast.makeText(getApplicationContext(), ((TextView) view).getText(),
              Toast.LENGTH_SHORT).show();
        }

      });
    }

    
    
}
