# PhonicAuditor

This is a discreate Android audio recording app that does the following...

 - Screen is blacked out on Activity with "Do not disturb" enabled (INTERRUPTION_FILTER_PRIORITY; so alarms and such will still show) with power button and volume UI disabled.
 - Launches Foreground Service and creates notification that shows current status as well as recording time for last auditing.
 - Allows VOL UP button to control recording scenario.
 - One click on VOL UP to start recording (will make one short vibration).  It's possible you may need to double click.
 - Another click on VOL UP to stop recording (will make two short vibrations).  It's possible you may need to double click.
 - VOL UP button is available while on Home page or on Activity page.
 - Window inset buttons (soft touch native buttons) and status bar are hidden without swipe from edge of top or buttom while in Activity.
 - Only HOME button will allow for exit if not set to Preferred launcher.
 - Triple tap on screen while in Activity will unlock app (and turn background a dark gray) to enable other soft touch buttons.
 - Writes GPS location (on record start) to audio file metadata for later use.
 - Bluetooth "Selfie" remotes that use VOL UP button will work as remote.

Currently records to "Internal Storage/Recordings/Phonic Auditor/" which will be created if it doesn't exist.

To close app (and Foreground Service), you must "unlock" app (triple tap on view while in Activity) and click on native "recent/running tasks" soft button and swipe up on Phonic Auditor Activity to close.  If you cam't get the notification service to stop...  Double check you are accessing main activity with black screen, then triple tap for light gray screeen (unlocked) and use recent activities button to show app and swip up on it.

The app was only designed to work with Android API >= 34 (with Samsung Galaxy S23 Ultra as taget device) but can obviously work with any Android 34+ API with obvious tolerances.
