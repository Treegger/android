package com.treegger.android.im.remote;

import com.treegger.protobuf.WebSocketProto.Roster;

public interface OnRosterListener extends WebSocketCallBack
{
    public void onRoster( Roster roster );
}
