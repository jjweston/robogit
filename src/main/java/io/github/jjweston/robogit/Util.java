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

        String result = filename
                .replace( "\\", "\\\\" )
                .replace( "\b", "\\b" )
                .replace( "\t", "\\t" )
                .replace( "\n", "\\n" )
                .replace( "\f", "\\f" )
                .replace( "\r", "\\r" )
                .replace( "\"", "\\\"" );

        if (( result.contains( " " )) || ( result.contains( "\\" ))) return "\"" + result + "\"";

        return result;
    }
}
