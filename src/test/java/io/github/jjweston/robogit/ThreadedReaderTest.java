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

import java.io.BufferedReader;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
class ThreadedReaderTest
{
    @Mock private ThreadUtil     mockThreadUtil;
    @Mock private ThreadFactory  mockThreadFactory;
    @Mock private Thread         mockThread;
    @Mock private BufferedReader mockBufferedReader;

    @Test
    void testConstructor_nullEmbeddingCacheService()
    {
        @SuppressWarnings( { "DataFlowIssue", "resource" } )
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> new ThreadedReader( this.mockThreadUtil, this.mockThreadFactory, null ));

        assertEquals( "Reader must not be null.", exception.getMessage() );
    }

    @Test
    void testStart_previouslyStarted()
    {
        when( this.mockThreadFactory.create( any() )).thenReturn( this.mockThread );

        try ( ThreadedReader threadedReader =
                      new ThreadedReader( this.mockThreadUtil, this.mockThreadFactory, this.mockBufferedReader ))
        {
            threadedReader.start();
            IllegalStateException exception = assertThrowsExactly( IllegalStateException.class, threadedReader::start );
            assertEquals( "Thread has already started.", exception.getMessage() );
        }
    }

    @Test
    void testJoin_interrupted() throws Exception
    {
        InterruptedException interruptedException = new InterruptedException( "test" );

        when( this.mockThreadFactory.create( any() )).thenReturn( this.mockThread );
        doThrow( interruptedException ).doNothing().when( this.mockThread ).join();

        try ( ThreadedReader threadedReader =
                      new ThreadedReader( this.mockThreadUtil, this.mockThreadFactory, this.mockBufferedReader ))
        {
            threadedReader.start();
            RuntimeException exception = assertThrowsExactly( RuntimeException.class, threadedReader::join );
            assertEquals( "InterruptedException occurred while joining thread.", exception.getMessage() );
            assertEquals( interruptedException, exception.getCause() );
            verify( this.mockThreadUtil ).interruptCurrentThread();
        }
    }

    @Test
    void testGetLines_notStarted()
    {
        try ( ThreadedReader threadedReader =
                      new ThreadedReader( this.mockThreadUtil, this.mockThreadFactory, this.mockBufferedReader ))
        {
            IllegalStateException exception =
                    assertThrowsExactly( IllegalStateException.class, threadedReader::getLines );
            assertEquals( "Thread has not started.", exception.getMessage() );
        }
    }

    @Test
    void testGetLines_running()
    {
        when( this.mockThreadFactory.create( any() )).thenReturn( this.mockThread );

        try ( ThreadedReader threadedReader =
                      new ThreadedReader( this.mockThreadUtil, this.mockThreadFactory, this.mockBufferedReader ))
        {
            threadedReader.start();
            IllegalStateException exception =
                    assertThrowsExactly( IllegalStateException.class, threadedReader::getLines );
            assertEquals( "Thread is still running.", exception.getMessage() );
        }
    }

    @Test
    void testGetException_notStarted()
    {
        try ( ThreadedReader threadedReader =
                      new ThreadedReader( this.mockThreadUtil, this.mockThreadFactory, this.mockBufferedReader ))
        {
            IllegalStateException exception =
                    assertThrowsExactly( IllegalStateException.class, threadedReader::getException );
            assertEquals( "Thread has not started.", exception.getMessage() );
        }
    }

    @Test
    void testGetException_running()
    {
        when( this.mockThreadFactory.create( any() )).thenReturn( this.mockThread );

        try ( ThreadedReader threadedReader =
                      new ThreadedReader( this.mockThreadUtil, this.mockThreadFactory, this.mockBufferedReader ))
        {
            threadedReader.start();
            IllegalStateException exception =
                    assertThrowsExactly( IllegalStateException.class, threadedReader::getException );
            assertEquals( "Thread is still running.", exception.getMessage() );
        }
    }

    @Test
    void testSuccess() throws Exception
    {
        when( this.mockBufferedReader.readLine() ).thenReturn( "Line 1", "Line 2", "Line 3", null );
        this.mockThreadCreation();

        try ( ThreadedReader threadedReader =
                      new ThreadedReader( this.mockThreadUtil, this.mockThreadFactory, this.mockBufferedReader ))
        {
            threadedReader.start();
            threadedReader.join();
            assertThat( threadedReader.getLines() ).as( "Lines" ).containsExactly( "Line 1", "Line 2", "Line 3" );
            assertNull( threadedReader.getException() );
        }

        InOrder inOrder = inOrder( this.mockThread );

        inOrder.verify( this.mockThread ).start();
        inOrder.verify( this.mockThread ).join();

        inOrder.verifyNoMoreInteractions();
        verifyNoMoreInteractions( this.mockThread );
    }

    @Test
    void testException() throws Exception
    {
        IOException testException = new IOException( "Test IO Exception" );
        when( this.mockBufferedReader.readLine() ).thenThrow( testException );
        this.mockThreadCreation();

        try ( ThreadedReader threadedReader =
                      new ThreadedReader( this.mockThreadUtil, this.mockThreadFactory, this.mockBufferedReader ))
        {
            threadedReader.start();
            threadedReader.join();
            assertThat( threadedReader.getLines() ).as( "Lines" ).containsExactly();
            assertEquals( testException, threadedReader.getException() );
        }
    }

    private void mockThreadCreation()
    {
        when( this.mockThreadFactory.create( any( Runnable.class ))).thenAnswer( invocation ->
        {
            Runnable task = invocation.getArgument( 0 );

            doAnswer( ignored ->
            {
                task.run();
                return null;
            } ).when( this.mockThread ).start();

            return this.mockThread;
        } );
    }
}
