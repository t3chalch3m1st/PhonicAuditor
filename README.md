# PhonicAuditor

This is a discreate Android audio recording app that does the following...

 - Screen is blacked out on Activity with "Do not disturbed" enabled (INTERRUPTION_FILTER_PRIORITY; so alarms and such will still show) with power button and volume UI disabled.
 - Launches Foreground Service and creates notification that shows current status as well as recording time for last auditing.
 - Allows VOL UP button to control recording scenario.
 - One click on VOL UP to start recording (will make one short vibration).
 - Another click on VOL UP to stop recording (will make two short vibrations).
 - VOL UP button is available while on Home page or on Activity page.
 - Window inset buttons (soft touch native buttons) are hidden without swipe from edge of top or buttom while in Activity.
 - Only HOME button will allow for exit if not set to Preferred launcher.
 - Triple tap on screen while in Activity will unlock app (and turn background a dark gray) to use other soft touch buttons.
 - Writes GPS location (on starting to record) on audio file metadata for later use.
 - Bluetooth "Selfie" remotes that use VOL UP button will work as remote.

To close app (and Foreground Service), you must "unlock" app (triple tap on view) and click on native "running tasks" soft button and swipe up on Activity to close.

The app was only designed to work with Android API >= 34 (with Samsung Galaxy S23 Ultra as taget device) but can obviously work with any Android 34+ API with obvious tolerances.

Preferences are a little shakey but can be enabled in Settings if grant fails and will work after that. (Includes ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS as well as many other permissions) [TODO]