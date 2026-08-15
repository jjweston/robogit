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
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

class GitStatus
{
    private final RoboGitLogger        roboGitLogger;
    private final ProcessRunnerFactory processRunnerFactory;
    private final FileLastModifiedUtil fileLastModifiedUtil;
    private final DateTimeFormatter    dateTimeFormatter;
    private final File                 repository;

    GitStatus( FileLastModifiedUtil fileLastModifiedUtil, DateTimeFormatter dateTimeFormatter, File repository )
    {
        this( new RoboGitLogger(), new ProcessRunnerFactory(), fileLastModifiedUtil, dateTimeFormatter, repository );
    }

    GitStatus( RoboGitLogger roboGitLogger, ProcessRunnerFactory processRunnerFactory,
               FileLastModifiedUtil fileLastModifiedUtil, DateTimeFormatter dateTimeFormatter, File repository )
    {
        if ( fileLastModifiedUtil == null )
            throw new IllegalArgumentException( "File last modified util must not be null." );
        if ( dateTimeFormatter == null ) throw new IllegalArgumentException( "Date time formatter must not be null." );
        if ( repository == null ) throw new IllegalArgumentException( "Repository must not be null." );

        this.roboGitLogger        = roboGitLogger;
        this.processRunnerFactory = processRunnerFactory;
        this.fileLastModifiedUtil = fileLastModifiedUtil;
        this.dateTimeFormatter    = dateTimeFormatter;
        this.repository           = repository;
    }

    Duration getModificationAge( Instant currentTime )
    {
        ProcessRunner gitStatus = this.processRunnerFactory.create(
                this.repository, "git", "status", "-z", "--porcelain=v1", "--untracked-files=all", "--no-renames" );
        gitStatus.run();

        String stdOut = gitStatus.getStdOut();
        if ( stdOut.isEmpty() ) return Duration.ZERO;

        List< String > filenames = Arrays.stream( stdOut.split( "\0" ))
                .map( s -> s.substring( 3 ))
                .sorted()
                .toList();

        List< String > displayFilenames = filenames
                .stream()
                .map( Util::escapeFilename )
                .toList();

        List< Long > modificationAges = filenames
                .stream()
                .map( filename -> new File( this.repository, filename ))
                .map( file -> this.fileLastModifiedUtil.getLastModified( currentTime, file ))
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

        this.roboGitLogger.println( "" );
        this.roboGitLogger.println( this.dateTimeFormatter.format( currentTime ));
        this.roboGitLogger.println( "" );

        String nameHeader = "Name";
        String ageHeader = "Age";

        if ( nameHeader.length() > maxFilenameLength ) maxFilenameLength = nameHeader.length();
        if ( ageHeader.length() > maxModificationAgeLength ) maxModificationAgeLength = ageHeader.length();

        String headerFormat = String.format( "%%-%ds | %%s", maxFilenameLength );
        this.roboGitLogger.println( String.format( headerFormat, nameHeader, ageHeader ));
        this.roboGitLogger.println( "-".repeat( maxFilenameLength ) + "-+-" + "-".repeat( maxModificationAgeLength ));

        String outputFormat = String.format( "%%-%ds | %%,%dd", maxFilenameLength, maxModificationAgeLength );
        for ( int i =  0; i < displayFilenames.size(); i++ )
        {
            String filename = displayFilenames.get( i );
            Long modificationAge = modificationAges.get( i );
            this.roboGitLogger.println( String.format( outputFormat, filename, modificationAge ));
        }

        long minModificationAge = modificationAges
                .stream()
                .mapToLong( Long::valueOf )
                .min()
                .orElse( 0L );

        String minModificationAgeUnit = minModificationAge == 1 ? "second" : "seconds";

        this.roboGitLogger.println( "" );
        this.roboGitLogger.println( String.format( "Minimum Age: %,d %s", minModificationAge, minModificationAgeUnit ));

        return Duration.ofSeconds( minModificationAge );
    }
}
