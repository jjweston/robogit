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

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class DurationFormatterTest
{
    @Test
    void format_nullDuration()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> DurationFormatter.format( null ));

        assertEquals( "Duration must not be null.", exception.getMessage() );
    }

    @Test
    void format_success()
    {
        assertEquals(            " 0s", DurationFormatter.format( Duration.ZERO ));
        assertEquals(        " 5m  3s", DurationFormatter.format( Duration.ofSeconds( 303 )));
        assertEquals(     "1h  0m  8s", DurationFormatter.format( Duration.ofSeconds( 3_608 )));
        assertEquals( "1,388h 53m 20s", DurationFormatter.format( Duration.ofSeconds( 5_000_000 )));
    }
}
