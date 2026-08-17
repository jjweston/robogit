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

import java.time.Duration;

class DurationFormatter
{
    private DurationFormatter() {}

    static String format( Duration duration )
    {
        if ( duration == null ) throw new IllegalArgumentException( "Duration must not be null." );

        long hours   = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        String hoursString = ( hours > 0 ) ? String.format( "%,dh ", hours ) : "";
        String minutesString = (( hours > 0 ) || ( minutes > 0 )) ? String.format( "%2dm ", minutes ) : "";
        String secondsString = String.format( "%2ds", seconds );

        return hoursString + minutesString + secondsString;
    }
}
