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

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

class FileLastModifiedUtil
{
    private final Map< File, Instant > lastModifiedMap = new HashMap<>();

    FileLastModifiedUtil() {}

    Instant getLastModified( Instant time, File file )
    {
        if ( time == null ) throw new IllegalArgumentException( "Time must not be null." );
        if ( file == null ) throw new IllegalArgumentException( "File must not be null." );

        long lastModified = file.lastModified();

        if ( lastModified == 0 )
        {
            if ( !this.lastModifiedMap.containsKey( file )) this.lastModifiedMap.put( file, time );
            return this.lastModifiedMap.get( file );
        }
        else
        {
            this.lastModifiedMap.remove( file );
            return Instant.ofEpochMilli( lastModified );
        }
    }
}
