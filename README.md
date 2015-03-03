README.md
=========

Simple utility to extract localization strings from existing standard OS X apps like Calculator.

To extract strings from all installed applications in `/Application`
simply launch like this:

    java -jar extract-localization-${projectversion}.jar


To launch just for some applications, add arguments like this:

    java -jar extract-localization-${projectversion}.jar /Applications/Calendar.app /Applications/Calculator.app


Requires Java 8.

Enjoy!
