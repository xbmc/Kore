Changelog
---------

Version 3.1.0
-------------
Minor update, primarily aimed at ensuring Kore remains up to date with the latest Android versions, while also addressing a few bugs along the way.

Version 3.0.0
-------------
Major changes in this version, bringing Kore up to date with the current Android platform (well, except for migrating to Kotlin, which to be honest wouldn't bring that much benefits, given that only replacing the syntax without taking advantage of the newer facilities provided by Kotlin/Android wouldn't bring major benefits).
Anyway, the major changes since the last version are:
- Migrate to Android's Material 3 UI guidelines, namely:
  - Review themes and colors, adding support for light and dark modes based on the device's settings
  - Add support for dynamic colors, which change depending on the user's wallpaper (on Android 12+)
  - Use images with round corners and update buttons, text boxes, etc to current standards
- Update media notifications and integrate them with Android's Media Session. 
  Note: if the media notification disappears after a few minutes, even though something is playing on Kodi, that's caused by the some aggressive battery optimization settings which forcefully stop the notification. This happens with some manufaturers that don't follow Android's guidelines, in a futile and artificial attempt to extent the battery life (Xiaomi, OnePlus, Samsung, etc), and the solution is to not restrict Kore's battery usage (the way to do it depends on the specific device, more info can be obtained at https://dontkillmyapp.com/)
- Redesign most of Kore's screens, the major changes being:
  - The Remote screen, adding the current playback state and better media controls allow for more control of what's playing
  - The Now Playing panel, adding the current playback state and media control buttons
  - The Movies, TV Shows, Music and Addons information screens, doing a complete redesign, particularly on the button actions section. The IMDb link has been removed as it was seldom broken and replaced with a generic Google search, where appropriate.
    Note, the "Play locally" function is now called "Stream", which is more appropriate and concise
  - The Artist details screen, to show the artist albums beneath its general information
  - Review all icons, updating them to current ones
  - Improve showing the connection status (connecting, not connected or connected) on the various screens
  - Make top app bar collapsable, and on the remote screen allow the background image to use up all the screen
- Improve the screen transitions
- Fix access to media storage in current Android versions
- General code cleaning, remove deprecated code and update current library versions
- Lots of other small bug fixes


Version 2.5.3
-------------
- Add support for SendToKodi
- Add support for sharing from Twitch
- Allow to disable direct share on a per host basis
- Bug fixes, specifically issues with thumbnails on Kodi Matrix and errors that prevented downloading files from Kodi

Version 2.5.1
-------------
- Add support for sharing from Arte video (The European Culture Channel) to Kodi
- Add support for sharing from Amazon Prime Videos
- Fix download of media files
- Support local play of items in the "Files" section
- Various improvments and bug fixes: Fix "Play from here" in the "Files" section, refresh of playlists in the remote, sharing local filenames with spaces in the name, support for self-signed certificates, remember last used tab, etc.

Version 2.5.0
-------------
- Include search option in PVR section
- Allow sorting PVR recordings and optionally hiding watched items
- Added support for sharing local files to Kodi, either by going into the side menu option "Local Files", or by choosing Kore as the share target (when accessing the file, for instance via a file browser)
- Allow changing Kore's language in Settings
- Add support to sharing from Soundcloud to Kodi
- New sort option for albums, movies and tv shows: by year
- Added new color themes (Sunrise and Sunset) and tweaked the others
- Scroll titles, when these are too long to fit (in the Now Playing and Info screens)
- Kore now shows all the available playlists, even when nothing's playing
- Update notifications to use the default Android style
- Movie ratings added to movie list
- New translations (Korean, Slovak)
- Bug fixes and UI tweaks

Version 2.4.7
-------------
- Improved addons list
- Enable direct sharing of a URL to a specific host
- Bug fixes and UI tweaks

Version 2.4.4
-------------
- Enable playing movies locally on device
- Add new setting to use skip steps instead of seeking in the notification
- Improve sharing from youtube
- Bug fixes and UI tweaks

Version 2.3.3
-------------
- Fixes for Android Oreo
- Make control pad scalable on smaller devices
- Improve showing text with markup codes on the Now Playing and PVR sections
- Handle playlists shared from YouTube app
- Option to use volume hardware keys anywhere inside Kore
- Bug fixes

Version 2.3.2
-------------
- New slide up panel with media controls on information screens
- Added new Favourites section to the navigation side panel
- Remote bottom bar shortcuts now configurable through Settings
- Added watched indicator to movies and tv shows list
- Various UI tweaks, including new colors and icons
- Bug fixes

Version 2.2.0
-------------
- Redesign settings screen
- Redesign TV show details to include next episodes and seasons list
- Show volume level on the Now Playing screen
- Added various new sort options on movies, TV shows and albums lists
- Improved songs list, showing the artist name on each song
- Support sharing to Kodi plain video urls
- New option: keep screen on when using the remote
- Various UI tweaks
- Bug fixes

Version 2.1.0
-------------
- Add songs tab on Music section and Artist section and support for showing songs without album or artist
- Add addon browsing
- Show artist details when an artist is selected from the list
- New option: pause playing when in a phone call (requires permission to phone state on Android versions < 6.0)
- New option: keep the remote above the lockscreen
- Support for playing Vimeo URLs
- Improve library syncing
- Various UI tweaks
- Bug fixes

Version 2.0.0
-------------
- PVR support
- New animations on transitions from list to details screens
- Added option to play/queue entire album, artist or genre
- Improve library syncing
- Various tweaks
- Bug fixes

Version 1.5.0
-------------
- D-pad buttons can skip forward/backward when media is playing (if EventServer is enabled in the media center's configuration, and accessible in Kodi)
- Added new screen to show all cast in movies and tv shows
- Added vibration option to d-pad buttons
- Add stop button to remote screen
- Fix youtube share behaviour
- Czech translation
- Simplified Chinese translation
- Russian translation
- Basque translation
- Spanish translation
- Bug fixes

Version 1.4.0
-------------
- Added support for sharing from youtube app to Kodi
- Visual tweaks
- Bug fixes

Version 1.3.0
-------------
- Remote redesign
- File browsing
- Fix Wake on Lan issues
- Bug fixes

Version 1.2.1
-------------
- Fix subtitle selection

Version 1.2.0
-------------
- Prepare for Official Remote status

Version 1.1
-----------
- Brazilian Portuguese translation (by Rafael Rosário @rafaelricado)


Version 1.1.0
-------------
- Replace Codec button with Context button on remote. Codec info is now available through a long click on Info button
- Added now playing notification
- Use hardware volume keys to control volume
- Italian translation (by Enrico Strocchi)
- Improved music library sync
- Visual tweaks

Version 1.0.1
-------------
- Fixed bug with In-app purchase key that was crashing Settings screen

Version 1.0.0
-------------
- New options to sort movies and tv shows
- Bulgarian translation (by NEOhidra)
- German translation (by jonas2515)

Version 0.9.2
-------------
- Added new actions in remote: update/clean library and toggle fullscreen
- French translation (thanks Kowalski!)
- Bug fixes and visual tweaks

Version 0.9.1
-------------
- Improved library sync;
- Automatically switch to remove after media start;
- Visual tweaks.

Version 0.9.0
-------------
- First version

