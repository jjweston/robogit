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
import java.util.Arrays;
import java.util.List;

class Main
{
    private Main() {}

    static void main( String[] args )
    {
        if ( args.length < 1 )
        {
            System.err.println( "Error: Git repository not specified." );
            System.err.println();
            System.err.println( "Usage: `mvn exec:java -Dexec.args=\"<Git repository>\"`" );
            System.err.println();
            System.err.println( "Replace `<Git repository>` with the location of your Git repository." );
            System.exit( 1 );
        }

        File repository = new File( args[ 0 ] );

        System.out.println( "Running `git status` in: " + repository );
        ProcessRunner processRunner = new ProcessRunner(
                repository, "git", "status", "-z", "--porcelain=v1", "--untracked-files=all", "--no-renames" );
        processRunner.run();

        List< String > stdOut = processRunner.getStdOut();
        List< String > stdErr = processRunner.getStdErr();

        List< String > filenames = stdOut
                .stream()
                .flatMap( s -> Arrays.stream( s.split( "\0" )))
                .map( s -> s.substring( 3 ))
                .map( Util::escapeFilename )
                .toList();

        System.out.println();
        System.out.println( "Exit Value: " + processRunner.getExitValue() );
        Main.logLines( "Output", filenames );
        Main.logLines( "Error", stdErr );
    }

    private static void logLines( String title, List< String > lines )
    {
        if ( lines.isEmpty() ) return;

        System.out.println();
        System.out.println( title + ":" );
        for ( String line : lines ) System.out.println( "> " + line );
    }
}
