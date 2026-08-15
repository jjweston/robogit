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

import java.io.File;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class FileLastModifiedUtilTest
{
    @Test
    void getLastModified_nullTime()
    {
        File file = new File( "test.txt" );

        FileLastModifiedUtil fileLastModifiedUtil = new FileLastModifiedUtil();

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> fileLastModifiedUtil.getLastModified( null, file ));

        assertEquals( "Time must not be null.", exception.getMessage() );
    }

    @Test
    void getLastModified_nullFile()
    {
        Instant time = Instant.now();

        FileLastModifiedUtil fileLastModifiedUtil = new FileLastModifiedUtil();

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> fileLastModifiedUtil.getLastModified( time, null ));

        assertEquals( "File must not be null.", exception.getMessage() );
    }

    @Test
    void getLastModified_success()
    {
        Instant time1 = Instant.parse( "2026-08-14T01:00:00Z" );
        Instant time2 = Instant.parse( "2026-08-14T02:00:00Z" );
        Instant time3 = Instant.parse( "2026-08-14T03:00:00Z" );
        Instant time4 = Instant.parse( "2026-08-14T04:00:00Z" );
        Instant time5 = Instant.parse( "2026-08-14T05:00:00Z" );
        Instant time6 = Instant.parse( "2026-08-14T06:00:00Z" );
        Instant time7 = Instant.parse( "2026-08-14T07:00:00Z" );
        Instant time8 = Instant.parse( "2026-08-14T08:00:00Z" );

        File file1 = spy( new File( "test-1.txt" ));
        File file2 = spy( new File( "test-2.txt" ));
        File file3 = spy( new File( "test-3.txt" ));

        doReturn(         time1.toEpochMilli()     ).when( file1 ).lastModified();
        doReturn( 0L, 0L, time4.toEpochMilli(), 0L ).when( file2 ).lastModified();
        doReturn( 0L                               ).when( file3 ).lastModified();

        FileLastModifiedUtil fileLastModifiedUtil = new FileLastModifiedUtil();

        assertEquals( time1, fileLastModifiedUtil.getLastModified( time2, file1 ));
        assertEquals( time2, fileLastModifiedUtil.getLastModified( time2, file2 ));
        assertEquals( time2, fileLastModifiedUtil.getLastModified( time3, file2 ));
        assertEquals( time4, fileLastModifiedUtil.getLastModified( time5, file2 ));
        assertEquals( time6, fileLastModifiedUtil.getLastModified( time6, file2 ));
        assertEquals( time6, fileLastModifiedUtil.getLastModified( time7, file2 ));
        assertEquals( time7, fileLastModifiedUtil.getLastModified( time7, file3 ));

        fileLastModifiedUtil.reset();

        assertEquals( time8, fileLastModifiedUtil.getLastModified( time8, file2 ));
        assertEquals( time8, fileLastModifiedUtil.getLastModified( time8, file3 ));
    }
}
