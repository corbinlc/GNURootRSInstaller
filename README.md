# OldSchoolRS

This project is meant to utilize the GNURoot Library and allow users to install and play Old School Runescape on their android devices.

## Building

1. Clone this repository into an eclipse workspace
2. Clone the GNURoot Library into the same workspace
  * Depending on the development schedule of the GNURoot Library, you may have to a line of code in a number of places.
  If you encounter an error about implicit services then add the line `intent.setPackage("com.GNURoot.debian");` wherever a service intent is being formed in the library.
3. Import both projects into eclipse
4. Import and copy to workspace the android-support-v7-appcompat library from the android sdk
5. Build project

## Current State and Future Work

This project was designed, and built on a linux machine, and has undergone limited testing. In its current state it will work and run the game in XServer.
To run the project as of now needs both GNURootDebian and XServer to be installed on the device.
The user will run GNURoot and install a RootFS.
They may then close GNURoot, as we are done there.
Then the user should open Xserver and let it load until the blue screen appears.
After Xserver has loaded, the user can launch the OldSchoolRS app and play the game.
If the game is not installed it will inform the user.
The install follows the guide found [here] (https://www.reddit.com/r/2007scape/comments/2t0deo/guide_oldschool_runescape_running_on_pure_android/) on Reddit with some minor changes to the Java version.

I must also warn future developers that currently the port input in the UI does nothing, as it is hardcoded to connect to port :0.0, because I have yet to encounter a time when it was not port 0.
The UI was a very rough shell to allow me to get to work on the core, and I did not plan to keep it as is.
