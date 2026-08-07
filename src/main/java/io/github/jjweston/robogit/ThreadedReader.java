/*

Copyright 2026 Jeffrey J. Weston <jjweston@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/

package io.github.jjweston.robogit;

import java.io.BufferedReader;
import java.util.LinkedList;
import java.util.List;

class ThreadedReader implements AutoCloseable
{
    private final ThreadUtil     threadUtil;
    private final ThreadFactory  threadFactory;
    private final BufferedReader reader;
    private final List< String > lines;

    boolean started = false;

    private Thread    thread;
    private Exception exception;

    ThreadedReader( BufferedReader reader )
    {
        this( new ThreadUtil(), new ThreadFactory(), reader );
    }

    ThreadedReader( ThreadUtil threadUtil, ThreadFactory threadFactory, BufferedReader reader )
    {
        if ( reader == null ) throw new IllegalArgumentException( "Reader must not be null." );

        this.threadUtil    = threadUtil;
        this.threadFactory = threadFactory;
        this.reader        = reader;
        this.lines         = new LinkedList<>();
    }

    public void close()
    {
        this.join();
    }

    void start()
    {
        if ( this.started ) throw new IllegalStateException( "Thread has already started." );
        this.started = true;

        this.thread = threadFactory.create( () ->
        {
            try
            {
                String line;
                while (( line = reader.readLine() ) != null ) this.lines.add( line );
            }
            catch ( Exception e )
            {
                this.exception = e;
            }
        } );

        this.thread.start();
    }

    void join()
    {
        if ( thread == null ) return;

        try
        {
            this.thread.join();
            this.thread = null;
        }
        catch ( InterruptedException e )
        {
            this.threadUtil.interruptCurrentThread();
            throw new RuntimeException( "InterruptedException occurred while joining thread.", e );
        }
    }

    List< String > getLines()
    {
        this.verifyThreadStatus();
        return this.lines;
    }

    Exception getException()
    {
        this.verifyThreadStatus();
        return this.exception;
    }

    private void verifyThreadStatus()
    {
        if ( !this.started ) throw new IllegalStateException( "Thread has not started." );
        if ( this.thread != null ) throw new IllegalStateException( "Thread is still running." );
    }
}
