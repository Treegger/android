package com.treegger.android.im.remote;

import android.content.Context;
import android.widget.Toast;

public class DisplayToastRunnable implements Runnable
{
    private Context context;
    private String message;
    public DisplayToastRunnable( Context context, String message )
    {
        this.context = context;
        this.message = message;
    }
    public void run() 
    {
        Toast.makeText( context, message, Toast.LENGTH_SHORT ).show();
    }
}
