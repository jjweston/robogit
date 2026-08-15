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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

class Main
{
    private Main() {}

    @SuppressWarnings( "InfiniteLoopStatement" )
    static void main( String[] args )
    {
        if ( args.length < 1 )
        {
            System.err.println( "Error: Git repository not specified." );
            System.err.println();
            System.err.println( "Usage: `java -jar target/robogit-1.0.0-SNAPSHOT.jar <Git repository>`" );
            System.err.println();
            System.err.println( "Replace `<Git repository>` with the location of your Git repository." );
            System.exit( 1 );
        }

        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern( "uuuu-MM-dd HH:mm (XXX)" )
                .withZone( ZoneId.systemDefault() );

        FileLastModifiedUtil fileLastModifiedUtil = new FileLastModifiedUtil();
        File                 repository           = new File( args[ 0 ] );
        int                  pollIntervalMinutes  = 1;
        int                  idleIntervalMinutes  = 5;
        Duration             pollInterval         = Duration.ofMinutes( pollIntervalMinutes );
        Duration             idleInterval         = Duration.ofMinutes( idleIntervalMinutes );
        Instant              nextTime             = Instant.now();

        @SuppressWarnings( "ConstantValue" )
        String pollIntervalMinutesUnit = pollIntervalMinutes == 1 ? "minute" : "minutes";

        @SuppressWarnings( "ConstantValue" )
        String idleIntervalMinutesUnit = idleIntervalMinutes == 1 ? "minute" : "minutes";

        int maxIntervalMinutesLength = Stream.of( pollIntervalMinutes, idleIntervalMinutes )
                .map( Object::toString )
                .mapToInt( String::length )
                .max()
                .orElse( 1 );

        String intervalFormat = String.format( "%%s Interval : %%,%dd %%s%%n", maxIntervalMinutesLength );

        System.out.println( "RoboGit Running" );
        System.out.println();
        System.out.format( "Repository    : %s%n", repository );
        System.out.format( intervalFormat, "Poll", pollIntervalMinutes, pollIntervalMinutesUnit );
        System.out.format( intervalFormat, "Idle", idleIntervalMinutes, idleIntervalMinutesUnit );

        while ( true )
        {
            if ( nextTime.compareTo( Instant.now() ) > 0 )
            {
                try
                {
                    Thread.sleep( Duration.between( Instant.now(), nextTime ));
                }
                catch ( InterruptedException e )
                {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException( "InterruptedException occurred while sleeping.", e );
                }
            }

            Instant currentTime = nextTime;
            nextTime = currentTime.plus( pollInterval );

            ProcessRunner gitStatus = new ProcessRunner(
                    repository, "git", "status", "-z", "--porcelain=v1", "--untracked-files=all", "--no-renames" );
            gitStatus.run();

            String stdOut = gitStatus.getStdOut();
            if ( stdOut.isEmpty() ) continue;

            List< String > filenames = Arrays.stream( stdOut.split( "\0" ))
                    .map( s -> s.substring( 3 ))
                    .toList();

            List< String > displayFilenames = filenames
                    .stream()
                    .map( Util::escapeFilename )
                    .toList();

            List< Long > modificationAges = filenames
                    .stream()
                    .map( filename -> new File( repository, filename ))
                    .map( file -> fileLastModifiedUtil.getLastModified( currentTime, file ))
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

            System.out.println();
            System.out.println( formatter.format( currentTime ));
            System.out.println();

            String nameHeader = "Name";
            String ageHeader = "Age";

            if ( nameHeader.length() > maxFilenameLength ) maxFilenameLength = nameHeader.length();
            if ( ageHeader.length() > maxModificationAgeLength ) maxModificationAgeLength = ageHeader.length();

            String headerFormat = String.format( "%%-%ds | %%s%%n", maxFilenameLength );
            System.out.format( headerFormat, nameHeader, ageHeader );
            System.out.println( "-".repeat( maxFilenameLength ) + "-+-" + "-".repeat( maxModificationAgeLength ));

            String outputFormat =
                    String.format( "%%-%ds | %%,%dd%%n", maxFilenameLength, maxModificationAgeLength );
            for ( int i =  0; i < displayFilenames.size(); i++ )
            {
                String filename = displayFilenames.get( i );
                Long modificationAge = modificationAges.get( i );
                System.out.format( outputFormat, filename, modificationAge );
            }

            long minModificationAge = modificationAges
                    .stream()
                    .mapToLong( Long::valueOf )
                    .min()
                    .orElse( 0L );

            String minModificationAgeUnit = minModificationAge == 1 ? "second" : "seconds";

            System.out.println();
            System.out.format( "Minimum Age: %,d %s%n", minModificationAge, minModificationAgeUnit );

            Duration minAgeInterval = Duration.ofSeconds( minModificationAge );
            if ( minAgeInterval.compareTo( idleInterval ) >= 0 )
            {
                System.out.println();
                System.out.println( "Committing Changes" );

                ProcessRunner gitAdd = new ProcessRunner( repository, "git", "add", "-A" );
                gitAdd.run();

                String message = "RoboGit Auto Commit: " + formatter.format( currentTime );
                ProcessRunner gitCommit = new ProcessRunner( repository, "git", "commit", "-m", message );
                gitCommit.run();
                gitCommit.getStdOut().lines().forEach( line -> System.out.println( "> " + line ));

                fileLastModifiedUtil.reset();
            }
        }
    }
}
