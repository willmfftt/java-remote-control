#How to use the Java Screen Recorder
# Introduction #

The Java Screen Recorder records you desktop as a movie.
You can use Java Screen Recorder in two ways
  * With a user interface
  * As a command line tool

# Details #

## With the user interface ##
  * To record use: java -jar screen-recorder.jar
  * To play back use: java -jar screen-player.jar

**Alternatively you can double-click on the jar files to run them in user interface mode.**

## As a command line tool: ##
  * To record use: java -jar screen-recorder.jar start (file-name)
  * To stop recording: java -jar screen-recorder.jar stop (file-name)

  * To play recording: java- -jar screen-player.jar (file-name)

## Converting Recordings to Movies ##
  * Install Java Media Framework from here :http://java.sun.com/javase/technologies/desktop/media/jmf/2.1.1/download.html
  * Download [recording-converter\_r1.0.jar](http://java-remote-control.googlecode.com/files/recording-converter_r1.0.jar)
  * At the command line type: java -jar recording-converter\_r1.0.jar (file to convert)
  * The file will be converted to a .mov (QuickTime) movie file
  * Alternatively type: java -jar recording-converter\_r1.0.jar (file to convert) (movie file name) (width eg 600) (height 400)

See an example here: http://www.youtube.com/watch?v=9EXR2fDzLQ0