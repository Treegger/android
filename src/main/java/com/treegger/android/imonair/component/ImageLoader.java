package com.treegger.android.imonair.component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

public class ImageLoader
{

    private static Queue<ImageHandler> urlQueue = new ConcurrentLinkedQueue<ImageHandler>();

    private static Map<String, Drawable> imageMap = Collections.synchronizedMap( new HashMap<String, Drawable>() );

    private static ThreadLoader currentLoader;

    public static class ImageHandler
        extends Handler
    {
        private ImageView image;

        public String url;

        public ImageHandler( ImageView image, String url )
        {
            this.image = image;
            this.url = url;
        }

        @Override
        public void handleMessage( Message message )
        {
            image.setImageDrawable( (Drawable) message.obj );
        }
    };

    public static void load( Context context, ImageView image, String url )
    {
        Drawable drawable = imageMap.get( url );
        if ( drawable == null )
        {
            loadURL( new ImageHandler( image, url ) );
        }
        else
            image.setImageDrawable( drawable );
    }

    public static synchronized void loadURL( ImageHandler handler )
    {
        urlQueue.add( handler );
        if ( currentLoader == null || !currentLoader.isAlive() )
        {
            currentLoader = new ThreadLoader();
            currentLoader.start();
        }
    }

    public static class ThreadLoader
        extends Thread
    {
        private static Queue<ImageHandler> backgroundQueue = new LinkedList<ImageHandler>();

        public void run()
        {
            ImageHandler handler = urlQueue.poll();
            while ( handler != null )
            {
                fetch( handler );
                handler = urlQueue.poll();
                if( handler == null )
                {
                    yield();
                    handler = backgroundQueue.poll();
                    if( handler != null )
                        backgroundFetch( handler );
                }
            }
        }

        private static final int IO_BUFFER_SIZE = 4 * 1024;

        private static void copy( InputStream in, OutputStream out )
            throws IOException
        {
            byte[] b = new byte[IO_BUFFER_SIZE];
            int read;
            while ( ( read = in.read( b ) ) != -1 )
            {
                out.write( b, 0, read );
            }
            
            boolean ex = false;
            try
            {
                in.close();
            }
            catch ( IOException e )
            {
                ex = true;
            }
            try
            {
                in.close();
            }
            catch ( IOException e )
            {
                ex = true;
            }
            if( ex ) throw new IOException();
        }

        private void cacheURLToFile( String urlStr, File file )
            throws FileNotFoundException, IOException
        {
            URL url = new URL( urlStr );
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout( 5*1000 );
            connection.setReadTimeout( 5*1000 );
            File tempFile = new File( file.getParentFile(), file.getName()+".tmp" );
            if( tempFile.exists() ) tempFile.delete();
            copy( connection.getInputStream(), new FileOutputStream( tempFile ) );
            if( file.exists() ) file.delete();
            tempFile.renameTo( file );
        }

        private File getFile( ImageHandler handler )
        {
            File file = null;
            try
            {
                MessageDigest m = MessageDigest.getInstance( "MD5" );
                m.update( handler.url.getBytes(), 0, handler.url.length() );
                BigInteger i = new BigInteger( 1, m.digest() );
                String filename = "cache-" + String.format( "%1$032X", i );
                file = new File( handler.image.getContext().getCacheDir(), filename );
            }
            catch ( Exception e )
            {
            }
            return file;
        }

        private void fetch( ImageHandler handler )
        {
            File file = getFile( handler );
            try
            {
                if ( file.exists() )
                {
                    backgroundQueue.add( handler );
                }
                else
                {
                    cacheURLToFile( handler.url, file );
                }

                Drawable drawable = Drawable.createFromStream( new FileInputStream( file ), "src" );
                imageMap.put( handler.url, drawable );

                Message message = handler.obtainMessage( 1, drawable );
                handler.sendMessage( message );

            }
            catch ( Exception e )
            {
                file.delete();
                imageMap.remove( handler.url );
            }
        }

        private void backgroundFetch( ImageHandler handler )
        {
            File file = getFile( handler );
            try
            {

                WifiManager wifi = (WifiManager) handler.image.getContext().getSystemService( Context.WIFI_SERVICE );
                WifiInfo info = wifi.getConnectionInfo();
                if ( info != null && info.getIpAddress() != 0 )
                {
                    cacheURLToFile( handler.url, file );
                }
                Drawable drawable = Drawable.createFromStream( new FileInputStream( file ), "src" );
                imageMap.put( handler.url, drawable );
            }
            catch ( Exception e )
            {
                file.delete();
            }
        }
    }
}
