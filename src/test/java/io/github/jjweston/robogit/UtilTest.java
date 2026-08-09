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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class UtilTest
{
    @Test
    void escapeFileName_nullFilename()
    {
        @SuppressWarnings( "DataFlowIssue" )
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class, () -> Util.escapeFilename( null ));

        assertEquals( "Filename must not be null.", exception.getMessage() );
    }

    @Test
    void escapeFileName_success()
    {
        assertEquals( "foo", Util.escapeFilename( "foo" ));
        assertEquals( "\"foo bar\"", Util.escapeFilename( "foo bar" ));
        assertEquals( "\"\\b\\t\\n\\f\\r\\\"\\\\\\uD83D\\uDE0A\"", Util.escapeFilename( "\b\t\n\f\r\"\\\uD83D\uDE0A" ));
    }
}
