package com.treegger.android.im;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class AccountManager
{
    public static final String TAG = "AccountManager";

    private final static String FILENAME = "TreegerAccountManager";

    private Context context;

    private List<Account> accounts;

    public AccountManager( Context context )
    {
        this.context = context;
        load();
    }

    public void addAccount( Account account )
    {
        accounts.add( account );
        commit();
    }

    public List<Account> getAccounts()
    {
        return accounts;
    }

    public Account getAccount( int position )
    {
        if( position >= 0 && position < accounts.size() ) return accounts.get( position );
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private void load()
    {
        try
        {
            FileInputStream fis = context.openFileInput( FILENAME );
            ObjectInputStream in = new ObjectInputStream( fis );
            accounts = (List<Account>) in.readObject();
            in.close();
            fis.close();
        }
        catch ( Exception e )
        {
            Log.v( TAG, e.getMessage(), e );
        }

        if ( accounts == null )
            accounts = new ArrayList<Account>();

    }

    private void commit()
    {
        try
        {
            FileOutputStream fos = context.openFileOutput( FILENAME, Context.MODE_PRIVATE );
            ObjectOutputStream out = new ObjectOutputStream( fos );
            out.writeObject( accounts );
            out.close();
            fos.close();
        }
        catch ( IOException e )
        {
            Log.v( TAG, e.getMessage(), e );
        }
    }

    public void removeAccount( int position )
    {
        if( position >= 0 && position < accounts.size() )
            accounts.remove( position );
        commit();
    }
}
