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
import picocli.CommandLine;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurationConverterTest
{
    @Test
    void convert_nullValue()
    {
        DurationConverter converter = new DurationConverter();

        IllegalArgumentException exception = assertThrowsExactly( IllegalArgumentException.class,
                () -> converter.convert( null ));

        assertEquals( "Value must not be null.", exception.getMessage() );
    }

    @Test
    void convert_emptyValue()
    {
        DurationConverter converter = new DurationConverter();

        CommandLine.TypeConversionException exception = assertThrowsExactly( CommandLine.TypeConversionException.class,
                () -> converter.convert( "" ));

        assertEquals( "Cannot convert \"\" to a duration. Value must not be empty.", exception.getMessage() );
    }

    @Test
    void convert_invalidNumber()
    {
        DurationConverter converter = new DurationConverter();

        CommandLine.TypeConversionException exception = assertThrowsExactly( CommandLine.TypeConversionException.class,
                () -> converter.convert( "xyz" ));

        assertEquals( "Cannot convert \"xyz\" to a duration. Invalid number: \"xy\"", exception.getMessage() );
    }

    @Test
    void convert_zero()
    {
        DurationConverter converter = new DurationConverter();

        CommandLine.TypeConversionException exception = assertThrowsExactly( CommandLine.TypeConversionException.class,
                () -> converter.convert( "0s" ));

        assertEquals(
                "Cannot convert \"0s\" to a duration. Duration must be greater than zero.", exception.getMessage() );
    }

    @Test
    void convert_invalidUnit()
    {
        DurationConverter converter = new DurationConverter();

        CommandLine.TypeConversionException exception = assertThrowsExactly( CommandLine.TypeConversionException.class,
                () -> converter.convert( "5x" ));

        assertEquals(
                "Cannot convert \"5x\" to a duration. Invalid unit. Expected \"h\", \"m\", or \"s\", but was: \"x\"",
                exception.getMessage() );
    }

    @Test
    void convert_overflow()
    {
        DurationConverter converter = new DurationConverter();

        CommandLine.TypeConversionException exception = assertThrowsExactly( CommandLine.TypeConversionException.class,
                () -> converter.convert( "10000000000000000h" ));

        assertEquals(
                "Cannot convert \"10000000000000000h\" to a duration. Duration is too large.", exception.getMessage() );
    }

    @Test
    void convert_success()
    {
        DurationConverter converter = new DurationConverter();

        assertEquals( Duration.ofSeconds( 30 ), converter.convert( "30s" ));
        assertEquals( Duration.ofMinutes( 5 ), converter.convert( "5m" ));
        assertEquals( Duration.ofHours( 1 ), converter.convert( "1h" ));
    }
}
