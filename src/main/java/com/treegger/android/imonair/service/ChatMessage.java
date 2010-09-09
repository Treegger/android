package com.treegger.android.imonair.service;

import java.util.Date;

public class ChatMessage
{
    public ChatMessage( String userAndHost, String text )
    {
        this.userAndHost = userAndHost;
        this.text = text;
        this.date = new Date();
    }
    
    public Date date;
    public String userAndHost;
    public String text;
}
