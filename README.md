Travis CI status [![Build Status](https://travis-ci.org/xbmc/Kore.svg?branch=master)](https://travis-ci.org/xbmc/Kore/)

Codacy analysis [![Codacy Badge](https://api.codacy.com/project/badge/grade/d7a03e5dff8840918b9d9ae069a7645e)](https://www.codacy.com/app/Kodi/Kore)

Doxygen documentation [![Documentation](https://codedocs.xyz/xbmc/Kore.svg)](https://codedocs.xyz/xbmc/Kore/)

Kore - Kodi/XBMC remote for Android
-----------------------------------

GitHub repository for the [Kore][1] Android app.

Kore is the official remote for [Kodi](http://kodi.tv/), and aims to be a simple and easy to use  remote.


Building
---------

1. Make sure you have a working [Android build system](http://developer.android.com/sdk/installing/studio-build.html);
2. The version of Android SDK and Build Tools needed is specified in app/build.gradle. Make sure you have them installed;
3. Install the version of [Android support library](http://developer.android.com/tools/support-library/setup.html) that is specified in app/gradle (dependencies section);
4. Git pull
5. Gradle should be able to fetch all the other needed libraries.


Credits
-------

**Libraries used**
- [Jackson](https://github.com/FasterXML/jackson)
- [Butterknife](http://jakewharton.github.io/butterknife/)
- [Picasso](http://square.github.io/picasso/)
- [EventBus](https://github.com/greenrobot/EventBus)
- [JmDNS](http://jmdns.sourceforge.net/)
- [PagerSlidingTabStrip](https://github.com/astuetz/PagerSlidingTabStrip)
- [FloatingActionButton](https://github.com/makovkastar/FloatingActionButton)

**Translations**
- French - Kowalski
- Bulgarian - NEOhidra
- German - jonas2515
- Italian - Enrico Strocchi

Links
-----

- [Kodi forum thread](http://forum.kodi.tv/forumdisplay.php?fid=129)
- [Google Play][1]
- [Google+ community](https://plus.google.com/communities/115506510322045554124)


License
-------

    Copyright 2015 XBMC Foundation

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


[1]: https://play.google.com/store/apps/details?id=org.xbmc.kore
