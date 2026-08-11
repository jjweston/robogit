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
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
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

        Instant currentTime = Instant.now();

        System.out.println( "Running `git status` in: " + repository );
        ProcessRunner processRunner = new ProcessRunner(
                repository, "git", "status", "-z", "--porcelain=v1", "--untracked-files=all", "--no-renames" );
        processRunner.run();

        List< String > filenames = processRunner
                .getStdOut()
                .stream()
                .map( s -> s.split( "\0" ))
                .flatMap( Arrays::stream )
                .map( s -> s.substring( 3 ))
                .toList();

        List< String > displayFilenames = filenames
                .stream()
                .map( Util::escapeFilename )
                .toList();

        List< Long > modificationAges = filenames
                .stream()
                .map( filename -> new File( repository, filename ))
                .map( File::lastModified )
                .map( lastModified -> lastModified == 0 ? currentTime : Instant.ofEpochMilli( lastModified ))
                .map( modificationTime -> Duration.between( modificationTime, currentTime ))
                .map( Duration::toSeconds )
                .toList();

        int maxFilenameLength = displayFilenames
                .stream()
                .mapToInt( String::length )
                .max()
                .orElse( 1 );

        int maxModificationAgeLength = modificationAges
                .stream()
                .map( age -> String.format( "%,d", age ))
                .mapToInt( String::length )
                .max()
                .orElse( 1 );

        if ( !filenames.isEmpty() )
        {
            List< String > output = new LinkedList<>();

            String nameHeader = "Name";
            String ageHeader = "Age";

            if ( nameHeader.length() > maxFilenameLength ) maxFilenameLength = nameHeader.length();
            if ( ageHeader.length() > maxModificationAgeLength ) maxModificationAgeLength = ageHeader.length();

            String headerFormat = String.format( "%%-%ds | %%s", maxFilenameLength );
            output.add( String.format( headerFormat, nameHeader, ageHeader ));
            output.add( "-".repeat( maxFilenameLength ) + "-+-" + "-".repeat( maxModificationAgeLength ));

            String outputFormat = String.format( "%%-%ds | %%,%dd", maxFilenameLength, maxModificationAgeLength );
            for ( int i =  0; i < displayFilenames.size(); i++ )
            {
                String filename = displayFilenames.get( i );
                Long modificationAge = modificationAges.get( i );
                output.add( String.format( outputFormat, filename, modificationAge ));
            }

            System.out.println();
            for ( String line : output ) System.out.println( line );
        }
    }
}
