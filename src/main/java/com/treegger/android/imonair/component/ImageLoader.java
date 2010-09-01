package com.treegger.android.imonair.component;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;


public class ImageLoader
{

    private static Queue<ImageHandler> urlQueue = new ConcurrentLinkedQueue<ImageHandler>();
    private static Map<String, Drawable> imageMap = Collections.synchronizedMap( new HashMap<String, Drawable>() );

    private static ThreadLoader currentLoader;

    public static class ImageHandler extends Handler 
    {
        private ImageView image;
        public String url;
        public ImageHandler( ImageView image, String url )
        {
            this.image = image;
            this.url = url;
        }
        
        @Override
        public void handleMessage(Message message) {
            image.setImageDrawable((Drawable) message.obj);
        }
    };
    
    public static void load( ImageView image, String url )
    {
        Drawable drawable = imageMap.get( url );
        if( drawable == null ) loadURL( new ImageHandler( image, url ) );
        else image.setImageDrawable( drawable );
    }
    
    public static synchronized void loadURL( ImageHandler handler )
    {
        urlQueue.add( handler );
        if( currentLoader == null || !currentLoader.isAlive() )
        {
            currentLoader = new ThreadLoader();
            currentLoader.start();
        }
    }
    
    
    public static class ThreadLoader extends Thread
    {

        public void run()
        {
            
            ImageHandler handler = urlQueue.poll();
            while( handler != null )
            {
                fetch( handler );
                handler = urlQueue.poll();
            }
        }
        private void fetch( ImageHandler handler )
        {
            try
            {
                URL url = new URL( handler.url );
                Drawable drawable = Drawable.createFromStream( url.openConnection().getInputStream(), "src" );
                imageMap.put( handler.url, drawable );
                Message message = handler.obtainMessage( 1, drawable );
                handler.sendMessage(message);
            }
            catch (Exception e) 
            {
            }
        }
        
    }
}
