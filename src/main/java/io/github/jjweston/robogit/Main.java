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

import picocli.CommandLine;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@CommandLine.Command( name = "robogit", versionProvider = VersionProvider.class,
        mixinStandardHelpOptions = true, usageHelpAutoWidth = true, sortOptions = false,
        description = "Periodically check a Git repository for changes to the working tree and commit them.",
        footer =
        {
            "",
            "<duration> Durations are specified as a positive number followed by a unit:",
            "           s = seconds, m = minutes, h = hours (for example: 30s, 5m, 1h)"
        } )
class Main implements Callable< Integer >
{
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
            .ofPattern( "uuuu-MM-dd HH:mm (xxx)" )
            .withZone( ZoneId.systemDefault() );

    private final FileLastModifiedUtil fileLastModifiedUtil = new FileLastModifiedUtil();

    @CommandLine.Option( names = "--poll-interval", paramLabel = "<duration>", defaultValue = "1m",
            converter = DurationConverter.class,
            description = "how often to check for changes (default: ${DEFAULT-VALUE})" )
    private Duration pollInterval;

    @CommandLine.Option( names = "--quiet-period", paramLabel = "<duration>", defaultValue = "5m",
            converter = DurationConverter.class,
            description = "how long changed files must remain untouched before committing (default: ${DEFAULT-VALUE})" )
    private Duration quietPeriod;

    @CommandLine.Parameters( description = "path to the Git repository" )
    private File repository;

    private Main() {}

    static void main( String[] args )
    {
        CommandLine commandLine = new CommandLine( new Main() );
        System.exit( commandLine.execute( args ));
    }

    @SuppressWarnings( "InfiniteLoopStatement" )
    public Integer call()
    {
        boolean  displayConfiguration = true;
        boolean  firstIteration       = true;
        Instant  nextTime             = Instant.now();

        int maxDurationLength = Stream.of( this.pollInterval, this.quietPeriod )
                .map( DurationFormatter::format )
                .mapToInt( String::length )
                .max()
                .orElse( 1 );

        String durationFormat = String.format( "%%s : %%%ds%%n", maxDurationLength );

        GitStatus gitStatus = new GitStatus( this.fileLastModifiedUtil, this.dateTimeFormatter, this.repository );

        while ( true )
        {
            if ( displayConfiguration )
            {
                displayConfiguration = false;

                if ( !firstIteration ) System.out.println();
                else firstIteration = false;

                System.out.println( "RoboGit " + VersionUtil.getVersion() );
                System.out.println();
                System.out.println( "Repository    : " + this.repository );
                System.out.format( durationFormat, "Poll Interval", DurationFormatter.format( this.pollInterval ));
                System.out.format( durationFormat, "Quiet Period ", DurationFormatter.format( this.quietPeriod  ));
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
            nextTime = currentTime.plus( this.pollInterval );

            if ( gitStatus.getModificationAge( currentTime ).compareTo( this.quietPeriod ) >= 0 )
            {
                System.out.println();
                System.out.println( "Committing Changes" );

                ProcessRunner gitAdd = new ProcessRunner( this.repository, "git", "add", "-A" );
                gitAdd.run();

                String message = "RoboGit Auto Commit: " + this.dateTimeFormatter.format( currentTime );
                ProcessRunner gitCommit = new ProcessRunner( this.repository, "git", "commit", "-m", message );
                gitCommit.run();
                gitCommit.getStdOut().lines().forEach( line -> System.out.println( "> " + line ));

                this.fileLastModifiedUtil.reset();
                displayConfiguration = true;
            }
        }
    }
}
