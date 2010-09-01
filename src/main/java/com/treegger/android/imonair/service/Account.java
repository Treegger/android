package com.treegger.android.imonair.service;

import java.io.Serializable;

public class Account implements Serializable
{
    private static final long serialVersionUID = 2L;
    
    public Long id = null;
    public String name;
    public String socialnetwork;
    public String password;
    
    public String photoURL;
    
    @Override
    public boolean equals( Object o )
    {
        if( o !=null && o instanceof Account )
        {
            return o == this || id !=null &&  id.equals( ((Account)o).id ) || id == ((Account)o).id;
        }
        return false;
    }
    @Override
    public int hashCode()
    {
        if( id != null ) return id.hashCode();
        else return 0;
    }
    
    
}
