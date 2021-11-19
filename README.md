# Noexes

A multi-platform graphical remote debugger for the Nintendo Switch.

## Quick Start Guide

### Pre-Requisites

* [Hekate](https://github.com/CTCaer/hekate) on your switch
* [Java 10](https://www.oracle.com/technetwork/java/javase/downloads/jre10-downloads-4417026.html) installed on your PC

### Downloading

Visit out [releases](/releases) page, and download the [latest](/releases/latest) version, and extract the whole zip file to a directory on your machine.

### Copying Files

Copy ```noexs.kip1``` to the root of your switch sd card.

### Editing ```hekate_ipl.ini```

Using a text editor open ```hekate_ipl.ini``` (located within the bootloader directory on your switch's sd card) and add the following:

```
[Noexes]
debugmode=1
kip1=noexs.kip1
```

And that's it switch sided! Now just select "Noexes" when booting using hekate to launch HOS with noexes running.

### Running the Client

On most operating systems all you need to do is run the jar file from the release as you would any other programs. If you're having trouble try running the following command (within the directory where you extracted Noexes):

```
java -jar JNoexsClient.jar
```

If you still have problems please double check that you're running at least Java 10 (you can verify this by using the command ``java -version``). If you are feel free to open an issue and I'll try to get back to you ASAP!

### Using the Client

**TODO**


## Building from Source


### Building the Client

**TODO**

### Building the Server

```
# load devkitpro env
# source /opt/devkitpro/switchvars.sh
cd ./server/source/noexs
make
```

## Client Dependencies

If you're using IntelliJ for building it should automatically download all the dependencies from maven, otherwise 

* [Usb4Java](http://usb4java.org/) - Used for USB communication
* [Gson](https://github.com/google/gson) - Used for serializing various objects to/from JSON
* [ASM](https://asm.ow2.io/) - For optimized pattern searching

## Server Dependencies
* [Libnx](https://github.com/switchbrew/libnx) - Without libnx there would be no homebrew on the switch, especially with what we're trying to do

## License

This project is licensed under GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details

## Acknowledgments

* L0nk, dcx2 and a bunch of others from the old WiiRd fourms
* roblabla for their near endless help, and for putting up with my inane questions
