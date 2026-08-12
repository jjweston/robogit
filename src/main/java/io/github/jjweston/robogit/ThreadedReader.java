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

class ThreadedReader implements AutoCloseable
{
    private final ThreadUtil     threadUtil;
    private final ThreadFactory  threadFactory;
    private final BufferedReader reader;

    private boolean started = false;

    private Thread    thread;
    private String    content;
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
    }

    public void close()
    {
        this.join();
    }

    void start()
    {
        if ( this.started ) throw new IllegalStateException( "Thread has already started." );
        this.started = true;

        this.thread = this.threadFactory.create( () ->
        {
            try { this.content = this.reader.readAllAsString(); }
            catch ( Exception e ) { this.exception = e; }
        } );

        this.thread.start();
    }

    void join()
    {
        if ( this.thread == null ) return;

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

    String getContent()
    {
        this.verifyThreadStatus();
        return this.content;
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
