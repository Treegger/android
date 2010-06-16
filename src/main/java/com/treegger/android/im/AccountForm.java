package com.treegger.android.im;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class AccountForm extends Activity {
    
    public static final String TAG = "AccountForm";

    static final String[] SOCIAL_NETWORKS = new String[] { "TWITTER", "FOURSQUARE" };

    @Override
    public void onCreate(Bundle savedInstanceState) {
       Log.v(TAG, "Activity State: onCreate()");
       super.onCreate(savedInstanceState);
       setContentView(R.layout.accountform);
       
       Spinner spinner = (Spinner) findViewById(R.id.spinner);
       ArrayAdapter<String> adapter = new ArrayAdapter<String>( this, android.R.layout.simple_spinner_item, SOCIAL_NETWORKS );
       adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
       spinner.setAdapter(adapter);

    }

    
    
}
