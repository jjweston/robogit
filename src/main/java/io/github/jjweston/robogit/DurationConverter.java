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

import java.time.Duration;

class DurationConverter implements CommandLine.ITypeConverter< Duration >
{
    public Duration convert( String value )
    {
        if ( value == null ) throw new IllegalArgumentException( "Value must not be null." );

        String message = String.format( "Cannot convert \"%s\" to a duration.", value );

        if ( value.isEmpty() )
        {
            throw new CommandLine.TypeConversionException( String.format( "%s Value must not be empty.", message ));
        }

        String number = value.substring( 0, value.length() - 1 );
        String unit = value.substring( value.length() - 1 ).toLowerCase();

        long n;

        try
        {
            n = Long.parseLong( number );
        }
        catch ( NumberFormatException e )
        {
            throw new CommandLine.TypeConversionException(
                    String.format( "%s Invalid number: \"%s\"", message, number ));
        }

        if ( n <= 0 )
        {
            throw new CommandLine.TypeConversionException(
                    String.format( "%s Duration must be greater than zero.", message ));
        }

        try
        {
            return switch ( unit )
            {
                case "h" -> Duration.ofHours( n );
                case "m" -> Duration.ofMinutes( n );
                case "s" -> Duration.ofSeconds( n );

                default -> throw new CommandLine.TypeConversionException( String.format(
                        "%s Invalid unit. Expected \"h\", \"m\", or \"s\", but was: \"%s\"", message, unit ));
            };
        }
        catch ( ArithmeticException e )
        {
            throw new CommandLine.TypeConversionException( String.format( "%s Duration is too large.", message ));
        }
    }
}
