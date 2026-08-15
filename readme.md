# RoboGit

RoboGit periodically checks for changes in the working tree of a Git repository and commits them.

## Prerequisites

### Java

You need a Java JDK and [Apache Maven](https://maven.apache.org/) to build and run RoboGit.
We use the [Eclipse Temurin](https://adoptium.net/temurin/) Java JDK, but other JDKs may also work.

RoboGit works with following versions, but other versions may also work:

- Eclipse Temurin:
  - `25.0.1+8-LTS`
- Apache Maven:
  - `3.9.12`

### Git

You need the [Git](https://git-scm.com/) command-line tools to run RoboGit.

RoboGit works with version `2.52.0`, but other versions may also work.

## Building and Running

To build RoboGit: `mvn package`

To run RoboGit: `mvn exec:java -Dexec.args="<Git repository>"`

Replace `<Git repository>` with the location of your Git repository.

RoboGit periodically checks for changes in the specified Git repository
using a specified poll interval (currently hard coded as one minute).
RoboGit commits changes when the modification age on every changed file
is at least as old as a specified idle interval (currently hard coded as five minutes).

## License

```text
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
```
