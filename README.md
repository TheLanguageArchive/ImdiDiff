# ImdiDiff
Tool that checks parallel IMDI hierarchies for relevant changes (with respect to IMDI-CMDI-IMDI conversion). To be used with the [cmdi-conversion-checker](https://github.com/TheLanguageArchive/cmdi-conversion-checker).

## Instructions 
* Run `mvn clean install` and find and executable JAR `ImdiDiff-1.0-SNAPSHOT-jar-with-dependencies.jar` in the build target directory
* Execute the JAR as follows: `java -jar imdi-original imdi-out/cmdi` where
 * `imdi-original` holds the original IMDI documents and 
 * `imdi-out/cmdi` has a parallel hierarchy of IMDI files converted back from CMDI
