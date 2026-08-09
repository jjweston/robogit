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

class Util
{
    static String escapeFilename( String filename )
    {
        if ( filename == null ) throw new IllegalArgumentException( "Filename must not be null." );

        StringBuilder builder = new StringBuilder();

        for ( char c : filename.toCharArray() )
        {
            switch ( c )
            {
                case '\b' -> builder.append( "\\b" );
                case '\t' -> builder.append( "\\t" );
                case '\n' -> builder.append( "\\n" );
                case '\f' -> builder.append( "\\f" );
                case '\r' -> builder.append( "\\r" );
                case '"'  -> builder.append( "\\\"" );
                case '\\' -> builder.append( "\\\\" );

                default ->
                {
                    if ( c >= 0x20 && c <= 0x7E ) builder.append( c );
                    else builder.append( String.format( "\\u%04X", (int) c ));
                }
            }
        }

        String result = builder.toString();
        if (( result.contains( " " )) || ( result.contains( "\\" ))) return "\"" + result + "\"";
        return result;
    }
}
