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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class ProcessRunnerTest
{
    private final File     testDirectory = new File( "directory" );
    private final String[] testCommand   = { "command" };

    @Mock private ProcessBuilderFactory mockProcessBuilderFactory;
    @Mock private ThreadedReaderFactory mockThreadedReaderFactory;
    @Mock private ProcessBuilder        mockProcessBuilder;
    @Mock private Process               mockProcess;
    @Mock private BufferedReader        mockStdOutBufferedReader;
    @Mock private BufferedReader        mockStdErrBufferedReader;
    @Mock private ThreadedReader        mockStdOutThreadedReader;
    @Mock private ThreadedReader        mockStdErrThreadedReader;

    @Test
    void testConstructor_nullDirectory()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new ProcessRunner(
                        this.mockProcessBuilderFactory, this.mockThreadedReaderFactory, null, this.testCommand ));

        assertEquals( "Directory must not be null.", exception.getMessage() );
    }

    @Test
    void testConstructor_nullCommand()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new ProcessRunner(
                        this.mockProcessBuilderFactory, this.mockThreadedReaderFactory,
                        this.testDirectory, (String[]) null ));

        assertEquals( "Command must not be null.", exception.getMessage() );
    }

    @Test
    void testConstructor_nullCommandArgument()
    {
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new ProcessRunner(
                        this.mockProcessBuilderFactory, this.mockThreadedReaderFactory,
                        this.testDirectory, "foo", null, "bar" ));

        assertEquals( "Command argument must not be null.", exception.getMessage() );
    }

    @Test
    void testRun_previouslyStarted() throws Exception
    {
        this.mockProcessCreation();

        ProcessRunner processRunner = new ProcessRunner(
                this.mockProcessBuilderFactory, this.mockThreadedReaderFactory, this.testDirectory, this.testCommand );
        processRunner.run();
        IllegalStateException exception = assertThrowsExactly( IllegalStateException.class, processRunner::run );
        assertEquals( "Process has already run.", exception.getMessage() );
    }

    @Test
    void testRun_startException() throws Exception
    {
        IOException innerException = new IOException( "test" );

        when( this.mockProcessBuilderFactory.create( this.testCommand )).thenReturn( this.mockProcessBuilder );
        when( this.mockProcessBuilder.start() ).thenThrow( innerException );

        ProcessRunner processRunner = new ProcessRunner(
                this.mockProcessBuilderFactory, this.mockThreadedReaderFactory, this.testDirectory, this.testCommand );
        RuntimeException exception = assertThrowsExactly( RuntimeException.class, processRunner::run );
        assertEquals( "IOException starting process.", exception.getMessage() );
        assertEquals( innerException, exception.getCause() );
    }

    @Test
    void testRun_stdOutReaderException() throws Exception
    {
        Exception stdOutException = new Exception( "Exception Test: Standard Output" );

        this.mockProcessCreation();
        when( this.mockStdOutThreadedReader.getException() ).thenReturn( stdOutException );

        ProcessRunner processRunner = new ProcessRunner(
                this.mockProcessBuilderFactory, this.mockThreadedReaderFactory, this.testDirectory, this.testCommand );
        RuntimeException exception = assertThrowsExactly( RuntimeException.class, processRunner::run );
        assertEquals( "Exception occurred while reading standard output.", exception.getMessage() );
        assertEquals( stdOutException, exception.getCause() );
    }

    @Test
    void testRun_stdErrReaderException() throws Exception
    {
        Exception stdErrException = new Exception( "Exception Test: Standard Error" );

        this.mockProcessCreation();
        when( this.mockStdErrThreadedReader.getException() ).thenReturn( stdErrException );

        ProcessRunner processRunner = new ProcessRunner(
                this.mockProcessBuilderFactory, this.mockThreadedReaderFactory, this.testDirectory, this.testCommand );
        RuntimeException exception = assertThrowsExactly( RuntimeException.class, processRunner::run );
        assertEquals( "Exception occurred while reading standard error.", exception.getMessage() );
        assertEquals( stdErrException, exception.getCause() );
    }

    @Test
    void testRun_multipleReaderExceptions() throws Exception
    {
        Exception stdOutException = new Exception( "Exception Test: Standard Output" );
        Exception stdErrException = new Exception( "Exception Test: Standard Error" );

        this.mockProcessCreation();
        when( this.mockStdOutThreadedReader.getException() ).thenReturn( stdOutException );
        when( this.mockStdErrThreadedReader.getException() ).thenReturn( stdErrException );

        ProcessRunner processRunner = new ProcessRunner(
                this.mockProcessBuilderFactory, this.mockThreadedReaderFactory, this.testDirectory, this.testCommand );
        RuntimeException exception = assertThrowsExactly( RuntimeException.class, processRunner::run );

        assertEquals( "Exceptions occurred while reading standard input and standard error.", exception.getMessage() );

        Throwable[] suppressedExceptions = exception.getSuppressed();
        assertEquals( 2, suppressedExceptions.length );

        assertEquals( RuntimeException.class, suppressedExceptions[ 0 ].getClass() );
        assertEquals( "Exception occurred while reading standard output.", suppressedExceptions[ 0 ].getMessage() );
        assertEquals( stdOutException, suppressedExceptions[ 0 ].getCause() );

        assertEquals( RuntimeException.class, suppressedExceptions[ 1 ].getClass() );
        assertEquals( "Exception occurred while reading standard error.", suppressedExceptions[ 1 ].getMessage() );
        assertEquals( stdErrException, suppressedExceptions[ 1 ].getCause() );
    }

    @Test
    void testGetExitValue_notStarted()
    {
        ProcessRunner processRunner = new ProcessRunner(
                this.mockProcessBuilderFactory, this.mockThreadedReaderFactory, this.testDirectory, this.testCommand );
        IllegalStateException exception =
                assertThrowsExactly( IllegalStateException.class, processRunner::getExitValue );
        assertEquals( "Process has not run.", exception.getMessage() );
    }

    @Test
    void testGetStdOut_notStarted()
    {
        ProcessRunner processRunner = new ProcessRunner(
                this.mockProcessBuilderFactory, this.mockThreadedReaderFactory, this.testDirectory, this.testCommand );
        IllegalStateException exception = assertThrowsExactly( IllegalStateException.class, processRunner::getStdOut );
        assertEquals( "Process has not run.", exception.getMessage() );
    }

    @Test
    void testGetStdErr_notStarted()
    {
        ProcessRunner processRunner = new ProcessRunner(
                this.mockProcessBuilderFactory, this.mockThreadedReaderFactory, this.testDirectory, this.testCommand );
        IllegalStateException exception = assertThrowsExactly( IllegalStateException.class, processRunner::getStdErr );
        assertEquals( "Process has not run.", exception.getMessage() );
    }

    @Test
    void testSuccess() throws Exception
    {
        int testExitValue = 42;
        List< String > testStdOut = List.of( "Out 1", "Out 2", "Out 3" );
        List< String > testStdErr = List.of( "Err 1", "Err 2", "Err 3" );

        this.mockProcessCreation();
        when( this.mockProcess.waitFor() ).thenReturn( testExitValue );
        when( this.mockStdOutThreadedReader.getLines() ).thenReturn( testStdOut );
        when( this.mockStdErrThreadedReader.getLines() ).thenReturn( testStdErr );

        ProcessRunner processRunner = new ProcessRunner(
                this.mockProcessBuilderFactory, this.mockThreadedReaderFactory, this.testDirectory, this.testCommand );
        processRunner.run();

        assertEquals( testExitValue, processRunner.getExitValue() );
        assertEquals( testStdOut, processRunner.getStdOut() );
        assertEquals( testStdErr, processRunner.getStdErr() );

        verify( this.mockProcessBuilder ).directory( this.testDirectory );
    }

    private void mockProcessCreation() throws Exception
    {
        when( this.mockProcessBuilderFactory.create( this.testCommand )).thenReturn( this.mockProcessBuilder );
        when( this.mockProcessBuilder.start() ).thenReturn( this.mockProcess );
        when( this.mockProcess.inputReader() ).thenReturn( this.mockStdOutBufferedReader );
        when( this.mockProcess.errorReader() ).thenReturn( this.mockStdErrBufferedReader );
        when( this.mockThreadedReaderFactory.create( this.mockStdOutBufferedReader ))
                .thenReturn( this.mockStdOutThreadedReader );
        when( this.mockThreadedReaderFactory.create( this.mockStdErrBufferedReader ))
                .thenReturn( this.mockStdErrThreadedReader );
    }
}
