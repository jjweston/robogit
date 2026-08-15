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

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                .ofPattern( "uuuu-MM-dd HH:mm (xxx)" )
                .withZone( ZoneId.systemDefault() );

        String version = Main.class.getPackage().getImplementationVersion();
        if ( version == null ) version = "unknown";

        FileLastModifiedUtil fileLastModifiedUtil = new FileLastModifiedUtil();
        File                 repository           = new File( args[ 0 ] );
        int                  pollIntervalMinutes  = 1;
        int                  idleIntervalMinutes  = 5;
        Duration             pollInterval         = Duration.ofMinutes( pollIntervalMinutes );
        Duration             idleInterval         = Duration.ofMinutes( idleIntervalMinutes );
        boolean              displayConfiguration = true;
        boolean              firstIteration       = true;
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

        String intervalFormat = String.format( "%%s Interval   : %%,%dd %%s%%n", maxIntervalMinutesLength );

        GitStatus gitStatus = new GitStatus( fileLastModifiedUtil, dateTimeFormatter, repository );

        while ( true )
        {
            if ( displayConfiguration )
            {
                displayConfiguration = false;

                if ( !firstIteration ) System.out.println();
                else firstIteration = false;

                System.out.println( "RoboGit Version : " + version );
                System.out.println( "Repository      : " + repository );
                System.out.format( intervalFormat, "Poll", pollIntervalMinutes, pollIntervalMinutesUnit );
                System.out.format( intervalFormat, "Idle", idleIntervalMinutes, idleIntervalMinutesUnit );
            }

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

            if ( gitStatus.getModificationAge( currentTime ).compareTo( idleInterval ) >= 0 )
            {
                System.out.println();
                System.out.println( "Committing Changes" );

                ProcessRunner gitAdd = new ProcessRunner( repository, "git", "add", "-A" );
                gitAdd.run();

                String message = "RoboGit Auto Commit: " + dateTimeFormatter.format( currentTime );
                ProcessRunner gitCommit = new ProcessRunner( repository, "git", "commit", "-m", message );
                gitCommit.run();
                gitCommit.getStdOut().lines().forEach( line -> System.out.println( "> " + line ));

                fileLastModifiedUtil.reset();
                displayConfiguration = true;
            }
        }
    }
}
