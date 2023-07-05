# Atari Tools

Kotlin based tools to perform various tasks needed for Atari dev.

## Building and running

```shell-session
$ ./gradlew assemble
$ alias tt='java -Dpicocli.usage.width=140 -jar /path/to/app/build/libs/app-DEV-all.jar'

$ tt -h
```

## data2hex

Converts GIMP image saved as data/raw to pure bytes that can be loaded into Atari.
Assumes 320x24 for now.

```shell
tt data2hex -d fn320x24.data -o fn320x24.hex
```