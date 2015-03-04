README.md
=========

Simple utility to extract localization strings from existing standard OS X apps like Calculator.

To extract strings from all installed applications in `/Application`
simply launch like this:

    java -jar extract-localization-VERSION.jar


(replace `VERSION` with a valid value)

To launch just for some applications, add arguments like this:

    java -jar extract-localization-VERSION.jar /Applications/Calendar.app /Applications/Calculator.app


(again, replace `VERSION` with a valid value)

To limit the emitted json files to reflect the values in
[this template](https://github.com/maremmle/localize-mainmenu/blob/master/languages/_template.json)
please add the command line flag `-f`.

The results are placed as `.json` files in a new directory called `localizations`.
Those files may be used as a starting point to create new language files for
[localize-mainmenu](https://github.com/maremmle/localize-mainmenu).


## Requirements

Requires Java 8.


Enjoy!
