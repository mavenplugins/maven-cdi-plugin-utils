# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

<!-- Format restrictions - see https://common-changelog.org and https://keepachangelog.com/ for details -->
<!-- Each Release must start with a line for the release version of exactly this format: ## [version] -->
<!-- The subsequent comment lines start with a space - not to irritate the release scripts parser!
 ## [major.minor.micro]
 <empty line> - optional sub sections may follow like:
 ### Added:
 - This feature was added
 <empty line>
 ### Changed:
 - This feature was changed
 <empty line>
 ### Removed:
 - This feature was removed
 <empty line>
 ### Fixed:
 - This issue was fixed
 <empty line>
 <empty line> - next line is the starting of the previous release
 ## [major.minor.micro]
 <empty line>
 <...>
 !!! In addition the compare URL links are to be maintained at the end of this CHANGELOG.md as follows.
     These links provide direct access to the GitHub compare vs. the previous release.
     The particular link of a released version will be copied to the release notes of a release accordingly.
     At the end of this file appropriate compare links have to be maintained for each release version in format:
 
  +-current release version
  |
  |                   +-URL to this repo                previous release version tag-+       +-current release version tag
  |                   |                                                              |       |
 [major.minor.micro]: https://github.com/mavenplugins/maven-cdi-plugin-utils/compare/vM.N.u..vM.N.u
-->
<!--
## [Unreleased]

### Additions
- TBD

### Changes
- TBD

### Deprecated
- TBD

###	Removals
- TBD

### Fixes
- TBD

###	Security
- TBD
-->

## [Unreleased]

### Changes
- TBD


## [4.0.1]
<!-- !!! Align version in badge URLs as well !!! -->
[![4.0.1 Badge](https://img.shields.io/nexus/r/io.github.mavenplugins/cdi-plugin-utils?server=https://s01.oss.sonatype.org&label=Maven%20Central&queryOpt=:v=4.0.1)](https://central.sonatype.com/artifact/io.github.mavenplugins/cdi-plugin-utils/4.0.1)

### Summary
- Remove vulnerability warnings due to guava dependency:
  - Update dependency to guava version `19.0` -> `33.3.1-jre`
  - Update dependency to junit version `4.12` -> `4.13.2`
  - Update dependency to junit-dataprovider version `1.10.3` -> `1.13.1`
  - Update dependency to asciitable version `0.2.5` -> `0.3.2`
  - Update dependency to slf4j version `1.7.21` -> `1.7.36`
- No further functional change

### Updates
- pom.xml:
  - Update dependency `com.google.guava:guava:19.0` -> `com.google.guava:guava:33.3.1-jre`
  - Update dependency `junit:junit:4.12` -> `junit:junit:4.13.2`
  - Update dependency `com.tngtech.java:junit-dataprovider:1.10.3` -> `com.tngtech.java:junit-dataprovider:1.13.1`
  - Update dependency `de.vandermeer:asciitable:0.2.5` -> `de.vandermeer:asciitable:0.3.2`
  - Update dependency `org.slf4j:slf4j-simple:1.7.21` -> `org.slf4j:slf4j-simple:1.7.36`

- CDIUtil.java:
  - update usage of deprecated/removed guava API `Files.fileTreeTraverser().preOrderTraversal(folder)`
    to `Files.fileTraverser().depthFirstPreOrder(folder)`

- WorkflowUtil.java:
  - extract ASCII table rendering from method `printAvailableSteps(Map<String, ProcessingStep> steps, Logger log)`
    to method `renderAvailableSteps(Map<String, ProcessingStep> steps)`

- WorkflowUtilTest.java:
  - add test for `WorkflowUtil.renderAvailableSteps(Map<String, ProcessingStep> steps)`

- WorkflowValidatorTest.java:
  - Fix for junit deprecation warning raised due to junit version update


## [4.0.0]
<!-- !!! Align version in badge URLs as well !!! -->
[![4.0.0 Badge](https://img.shields.io/nexus/r/io.github.mavenplugins/cdi-plugin-utils?server=https://s01.oss.sonatype.org&label=Maven%20Central&queryOpt=:v=4.0.0)](https://central.sonatype.com/artifact/io.github.mavenplugins/cdi-plugin-utils/4.0.0)

### Summary
- Work for Java 8, 11, 17, 21 by using CDI WELD 4.0.3.Final with Jakarta Inject API
- CDI dependencies migrated from Javax to Jakarta EE (see **Compatibility** below)
- Extend the workflow execution to handle an `EnforceRollbackWithoutErrorException`
  to abort the workflow with a rollback of previous actions but with Maven success.<br>
  This is supposed for test purpose only - #4

### Compatibility
- 👉 This release requires to migrate components depending on it, since CDI dependencies did have been changed from Javax to Jakarta EE!

### Updates
- EnforceRollbackWithoutErrorException.java:
  - added

- WorkflowExecutor.java:
  - handle EnforceRollbackWithoutErrorException to:
    - perform a rollback like in an error case
    - end normal without re-throwing this exception

- AbstractCDIMojo.java,
  MojoInject.java,
  MojoProduces.java,
  CdiBeanWrapper.java,
  CdiProducerBean.java,
  CDIUtil.java,
  MavenLogWrapper.java:
  - migrate from Javax to Jakarta EE injection


## [3.4.1]
<!-- !!! Align version in badge URLs as well !!! -->
[![3.4.1 Badge](https://img.shields.io/nexus/r/io.github.mavenplugins/cdi-plugin-utils?server=https://s01.oss.sonatype.org&label=Maven%20Central&queryOpt=:v=3.4.1)](https://central.sonatype.com/artifact/io.github.mavenplugins/cdi-plugin-utils/3.4.1)

### Summary
- Update requirements:
  - JDK 1.8 or higher
  - Apache Maven 3.3.9 or higher
- Improve and precise Maven dependencies
- Fix JavaDoc warning

### Updates
- pom.xml:
  - update version.java 1.7 -> 1.8
  - update version.maven 3.2.1 -> 3.3.9
  - update version.weld-se 2.3.3.Final -> 2.4.8.Final
  - remove obsolete explicit dependencies
  - change dependency scope to `provided` for:
    - maven-core
    - maven-plugin-annotations

- CDIUtil.java:
  - fix JavaDoc warnings

- README.md:
  - Update requirements:
    - JDK 1.8
    - Apache Maven 3.3.9 or higher


## [3.4.0]
<!-- !!! Align version in badge URLs as well !!! -->
[![3.4.0 Badge](https://img.shields.io/nexus/r/io.github.mavenplugins/cdi-plugin-utils?server=https://s01.oss.sonatype.org&label=Maven%20Central&queryOpt=:v=3.4.0)](https://central.sonatype.com/artifact/io.github.mavenplugins/cdi-plugin-utils/3.4.0)

### Summary
- Initial release of this artifact with new groupId `io.github.mavenplugins`
- Codewise identical with `com.itemis.maven.plugins:cdi-plugin-utils:3.4.0`<br>No more features nor changes
- Released to Maven Central

### Updates
- pom.xml:
  - update parent pom reference
  - update groupId to io.github.mavenplugins
  - update URLs to fit with new repo location
  - remove obsolete profile disable-java8-doclint

- README.md:
  - update URLs for build tags
  - update URLs of lookup references


<!--
## []

### NeverReleased
- This is just a dummy placeholder to make the parser of GHCICD/release-notes-from-changelog@v1 happy!
-->

[Unreleased]: https://github.com/mavenplugins/maven-cdi-plugin-utils/compare/v4.0.1..HEAD
[4.0.1]: https://github.com/mavenplugins/maven-cdi-plugin-utils/compare/v4.0.0..v4.0.1
[4.0.0]: https://github.com/mavenplugins/maven-cdi-plugin-utils/compare/v3.4.1..v4.0.0
[3.4.1]: https://github.com/mavenplugins/maven-cdi-plugin-utils/compare/v3.4.0..v3.4.1
[3.4.0]: https://github.com/mavenplugins/maven-cdi-plugin-utils/releases/tag/v3.4.0
