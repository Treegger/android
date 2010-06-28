package com.treegger.android.im.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;


import android.content.Context;
import android.util.Log;

public class AccountStorage
{
    public static final String TAG = "AccountManager";

    private final static String FILENAME = "TreegerAccountManager";

    private static Long idCounter = System.currentTimeMillis();
    private Context context;

    private List<Account> accounts;

    protected AccountStorage( Context context )
    {
        this.context = context;
        load();
    }

    public void addAccount( Account account )
    {
        account.id = idCounter++;
        accounts.add( account );
        commit();
    }

    public void updateAccount( Account account )
    {
        commit();
    }

    public void removeAccount( Account account )
    {
        accounts.remove( account );
        commit();
    }

    public List<Account> getAccounts()
    {
        return accounts;
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

}
