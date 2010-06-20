package com.treegger.android.im;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class AndroIM extends Activity {
    public static final String TAG = "AndroIM";

    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    

    private static final int MENU_ACCOUNTS = 1;
    private static final int MENU_SIGNOUT = 2;
    
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ACCOUNTS, 0, R.string.menu_accounts );
        menu.add(0, MENU_SIGNOUT, 0, R.string.menu_sign_out );
        return true;
    }
    

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ACCOUNTS:
            Log.v(TAG, "Starting activity");
            Intent i = new Intent( this, AccountList.class);
            startActivity( i );
            return true;
        case MENU_SIGNOUT:
            return true;
        }
        return false;
    }
}