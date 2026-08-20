# RoboGit

Periodically check a Git repository for changes to the working tree and commit them.

## Prerequisites

### Java

You need a Java JDK and [Apache Maven](https://maven.apache.org/) to build RoboGit.
You need a Java virtual machine to run RoboGit.
We use the [Eclipse Temurin](https://adoptium.net/temurin/) Java JDK, but other JDKs may also work.

RoboGit is tested with the following versions, but other versions may also work:

- Eclipse Temurin:
  - `25.0.4+7-LTS`
- Apache Maven:
  - `3.9.12`

### Git

You need the [Git](https://git-scm.com/) command-line tools to run RoboGit.

RoboGit is tested with version `2.52.0`, but other versions may also work.

## Building and Running

To build RoboGit:
`mvn package`

To run RoboGit on Windows:
`target\appassembler\bin\robogit [options] <repository>`

To run RoboGit on Linux/macOS:
`target/appassembler/bin/robogit [options] <repository>`

RoboGit supports the following command-line options:

```text
Usage: robogit [-hV] [--dry-run] [--poll-interval=<duration>]
               [--quiet-period=<duration>] <repository>
Periodically check a Git repository for changes to the working tree and commit
them.
      <repository>   path to the Git repository
      --poll-interval=<duration>
                     how often to check for changes (default: 1m)
      --quiet-period=<duration>
                     how long changed files must remain untouched before
                       committing (default: 5m)
      --dry-run      Show list of changed files and exit without committing
                       changes.
  -h, --help         Show this help message and exit.
  -V, --version      Print version information and exit.

<duration> Durations are specified as a positive number followed by a unit:
           s = seconds, m = minutes, h = hours (for example: 30s, 5m, 1h)
```

## Release Process

The following files must be updated when changing the version number:

- [`pom.xml`](pom.xml)
- [`readme.md`](readme.md)
- [`src/assembly/readme.txt`](src/assembly/readme.txt)

For a release:

- Create an issue and branch for preparing the release:
  - Issue: `Prepare for [version] release.`
  - Branch: `issue/[#]-release-[version]`
- Remove the `SNAPSHOT` qualifier from the version number.
- Create and merge a pull request with the above change.
- Create a tag.
- Build the distribution files from the tagged commit: `mvn clean package`
- Create a release for the tag and attach the distribution files:
  - `robogit-[version].tar.gz`
  - `robogit-[version].zip`

For a release candidate:

- Follow the instructions above,
  but instead of removing the `SNAPSHOT` qualifier from the version number,
  replace `SNAPSHOT` with a release-candidate qualifier (such as `RC1`).
- Create an issue and branch for restoring the `SNAPSHOT` qualifier to the version number:
  - Issue: `Restore [version] snapshot.`
  - Branch: `issue/[#]-snapshot-[version]`
- Replace the release-candidate qualifier (such as `RC1`) with the `SNAPSHOT` qualifier in the version number.
- Create and merge a pull request with the above change.

Before starting work on the next version:

- Create an issue and branch for starting the next version:
  - Issue: `Start [version] snapshot.`
  - Branch: `issue/[#]-start-[version]`
- Update the version number to `[version]-SNAPSHOT`.
- Create and merge a pull request with the above change.

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
