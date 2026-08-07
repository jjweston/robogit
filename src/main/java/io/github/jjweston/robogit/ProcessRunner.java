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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

class ProcessRunner
{
    private final ThreadUtil            threadUtil;
    private final ProcessBuilderFactory processBuilderFactory;
    private final ThreadedReaderFactory threadedReaderFactory;
    private final File                  directory;
    private final String[]              command;

    private boolean run = false;

    private int            exitValue;
    private List< String > stdOut;
    private List< String > stdErr;

    ProcessRunner( File directory, String... command )
    {
        this( new ThreadUtil(), new ProcessBuilderFactory(), new ThreadedReaderFactory(), directory, command );
    }

    ProcessRunner( ThreadUtil threadUtil, ProcessBuilderFactory processBuilderFactory,
                   ThreadedReaderFactory threadedReaderFactory, File directory, String... command )
    {
        if ( directory == null ) throw new IllegalArgumentException( "Directory must not be null." );
        if ( command == null ) throw new IllegalArgumentException( "Command must not be null." );

        for ( String argument : command )
        {
            if ( argument == null ) throw new IllegalArgumentException( "Command argument must not be null." );
        }

        this.threadUtil            = threadUtil;
        this.processBuilderFactory = processBuilderFactory;
        this.threadedReaderFactory = threadedReaderFactory;
        this.directory             = directory;
        this.command               = command;
    }

    void run()
    {
        if ( this.run ) throw new IllegalStateException( "Process has already run." );
        this.run = true;

        ProcessBuilder processBuilder = this.processBuilderFactory.create( this.command );
        processBuilder.directory( this.directory );

        Process process;
        try { process = processBuilder.start(); }
        catch ( IOException e ) { throw new RuntimeException( "IOException occurred while starting process.", e ); }

        try ( ThreadedReader stdOutReader =
                      this.threadedReaderFactory.create( process.inputReader( StandardCharsets.UTF_8 ));
              ThreadedReader stdErrReader =
                      this.threadedReaderFactory.create( process.errorReader( StandardCharsets.UTF_8 )))
        {
            stdOutReader.start();
            stdErrReader.start();

            try { this.exitValue = process.waitFor(); }
            catch ( InterruptedException e )
            {
                this.threadUtil.interruptCurrentThread();
                throw new RuntimeException( "InterruptedException occurred while waiting for process.", e );
            }

            stdOutReader.join();
            stdErrReader.join();

            List< RuntimeException > exceptions = new LinkedList<>();
            Exception stdOutException = stdOutReader.getException();
            Exception stdErrException = stdErrReader.getException();

            if ( stdOutException != null )
            {
                exceptions.add(
                        new RuntimeException( "Exception occurred while reading standard output.", stdOutException ));
            }

            if ( stdErrException != null )
            {
                exceptions.add(
                        new RuntimeException( "Exception occurred while reading standard error.", stdErrException ));
            }

            if ( !exceptions.isEmpty() )
            {
                if ( exceptions.size() == 1 ) throw exceptions.getFirst();

                RuntimeException exception =
                        new RuntimeException( "Exceptions occurred while reading standard input and standard error." );
                for ( RuntimeException e : exceptions ) exception.addSuppressed( e );
                throw exception;
            }

            this.stdOut = stdOutReader.getLines();
            this.stdErr = stdErrReader.getLines();
        }
    }

    int getExitValue()
    {
        if ( !this.run ) throw new IllegalStateException( "Process has not run." );
        return this.exitValue;
    }

    List< String > getStdOut()
    {
        if ( !this.run ) throw new IllegalStateException( "Process has not run." );
        return this.stdOut;
    }

    List< String > getStdErr()
    {
        if ( !this.run ) throw new IllegalStateException( "Process has not run." );
        return this.stdErr;
    }
}
