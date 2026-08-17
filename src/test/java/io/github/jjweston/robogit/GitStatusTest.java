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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class GitStatusTest
{
    @Mock private RoboGitLogger        mockRoboGitLogger;
    @Mock private ProcessRunnerFactory mockProcessRunnerFactory;
    @Mock private FileLastModifiedUtil mockFileLastModifiedUtil;
    @Mock private ProcessRunner        mockProcessRunner;

    private final DateTimeFormatter testDateTimeFormatter = DateTimeFormatter
            .ofPattern( "uuuu-MM-dd HH:mm (xxx)" )
            .withZone( ZoneOffset.UTC );

    private final File testRepository = new File( "test" );

    @Test
    void testConstructor_nullFileLastModifiedUtil()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new GitStatus(
                        this.mockRoboGitLogger, this.mockProcessRunnerFactory,
                        null, this.testDateTimeFormatter, this.testRepository ));

        assertEquals( "File last modified util must not be null.", exception.getMessage() );
    }

    @Test
    void testConstructor_nullDateTimeFormatter()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new GitStatus(
                        this.mockRoboGitLogger, this.mockProcessRunnerFactory,
                        this.mockFileLastModifiedUtil, null, this.testRepository ));

        assertEquals( "Date time formatter must not be null.", exception.getMessage() );
    }

    @Test
    void testConstructor_nullRepository()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new GitStatus(
                        this.mockRoboGitLogger, this.mockProcessRunnerFactory,
                        this.mockFileLastModifiedUtil, this.testDateTimeFormatter, null ));

        assertEquals( "Repository must not be null.", exception.getMessage() );
    }

    @Test
    void testGetModificationAge_noFiles()
    {
        Instant time = Instant.parse( "2026-08-15T01:00:00Z" );

        GitStatus gitStatus = new GitStatus(
                this.mockRoboGitLogger, this.mockProcessRunnerFactory,
                this.mockFileLastModifiedUtil, this.testDateTimeFormatter, this.testRepository );

        when( this.mockProcessRunnerFactory.create( eq( this.testRepository ), any( String[].class )))
                .thenReturn( this.mockProcessRunner );
        when( this.mockProcessRunner.getStdOut() ).thenReturn( "" );

        assertEquals( Duration.ZERO, gitStatus.getModificationAge( time ));

        verifyNoMoreInteractions( this.mockRoboGitLogger );
    }

    @Test
    void testGetModificationAge_oneSecond()
    {
        Instant time1 = Instant.parse( "2026-08-15T01:00:01Z" );
        Instant time2 = Instant.parse( "2026-08-15T01:00:02Z" );

        File file = new File( this.testRepository, "foo.txt" );

        GitStatus gitStatus = new GitStatus(
                this.mockRoboGitLogger, this.mockProcessRunnerFactory,
                this.mockFileLastModifiedUtil, this.testDateTimeFormatter, this.testRepository );

        when( this.mockProcessRunnerFactory.create( eq( this.testRepository ), any( String[].class )))
                .thenReturn( this.mockProcessRunner );
        when( this.mockProcessRunner.getStdOut() ).thenReturn( " M foo.txt" );

        when( this.mockFileLastModifiedUtil.getLastModified( time2, file )).thenReturn( time1 );

        assertEquals( Duration.ofSeconds( 1 ), gitStatus.getModificationAge( time2 ));

        InOrder inOrder = inOrder( this.mockRoboGitLogger );

        inOrder.verify( this.mockRoboGitLogger ).println( "" );
        inOrder.verify( this.mockRoboGitLogger ).println( "2026-08-15 01:00 (+00:00)" );
        inOrder.verify( this.mockRoboGitLogger ).println( "" );
        inOrder.verify( this.mockRoboGitLogger ).println( "Name    | Age" );
        inOrder.verify( this.mockRoboGitLogger ).println( "--------+----" );
        inOrder.verify( this.mockRoboGitLogger ).println( "foo.txt |  1s" );
        inOrder.verify( this.mockRoboGitLogger ).println( "" );
        inOrder.verify( this.mockRoboGitLogger ).println( "Minimum Age:  1s" );

        inOrder.verifyNoMoreInteractions();
        verifyNoMoreInteractions( this.mockRoboGitLogger );
    }

    @Test
    void testGetModificationAge_success()
    {
        Instant time1 = Instant.parse( "2026-08-10T01:00:00Z" );
        Instant time2 = Instant.parse( "2026-08-15T02:00:00Z" );
        Instant time3 = Instant.parse( "2026-08-15T03:00:00Z" );
        Instant time4 = Instant.parse( "2026-08-15T04:00:00Z" );

        File file1 = new File( this.testRepository, "bar.txt" );
        File file2 = new File( this.testRepository, "baz.txt" );
        File file3 = new File( this.testRepository, "foo.txt" );

        GitStatus gitStatus = new GitStatus(
                this.mockRoboGitLogger, this.mockProcessRunnerFactory,
                this.mockFileLastModifiedUtil, this.testDateTimeFormatter, this.testRepository );

        when( this.mockProcessRunnerFactory.create( eq( this.testRepository ), any( String[].class )))
                .thenReturn( this.mockProcessRunner );
        when( this.mockProcessRunner.getStdOut() ).thenReturn( " M baz.txt\0 D foo.txt\0?? bar.txt" );

        when( this.mockFileLastModifiedUtil.getLastModified( time4, file1 )).thenReturn( time1 );
        when( this.mockFileLastModifiedUtil.getLastModified( time4, file2 )).thenReturn( time2 );
        when( this.mockFileLastModifiedUtil.getLastModified( time4, file3 )).thenReturn( time3 );

        assertEquals( Duration.ofSeconds( 3_600 ), gitStatus.getModificationAge( time4 ));

        InOrder inOrder = inOrder( this.mockRoboGitLogger );

        inOrder.verify( this.mockRoboGitLogger ).println( "" );
        inOrder.verify( this.mockRoboGitLogger ).println( "2026-08-15 04:00 (+00:00)" );
        inOrder.verify( this.mockRoboGitLogger ).println( "" );
        inOrder.verify( this.mockRoboGitLogger ).println( "Name    | Age" );
        inOrder.verify( this.mockRoboGitLogger ).println( "--------+-------------" );
        inOrder.verify( this.mockRoboGitLogger ).println( "bar.txt | 123h  0m  0s" );
        inOrder.verify( this.mockRoboGitLogger ).println( "baz.txt |   2h  0m  0s" );
        inOrder.verify( this.mockRoboGitLogger ).println( "foo.txt |   1h  0m  0s" );
        inOrder.verify( this.mockRoboGitLogger ).println( "" );
        inOrder.verify( this.mockRoboGitLogger ).println( "Minimum Age: 1h  0m  0s" );

        inOrder.verifyNoMoreInteractions();
        verifyNoMoreInteractions( this.mockRoboGitLogger );
    }
}
