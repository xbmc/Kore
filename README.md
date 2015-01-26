Kore - Kodi/XBMC remote for Android
-----------------------------------

GitHub repository for the [Kore][1] Android app.

Kore is a simple and easy to use Kodi/XBMC remote.


Building
---------

Git pull should get you almost all you need, as long as you have a working [Android build system][4]

You'll need to create the following files:

1. `gradle.properties`, with the value `IAP_KEY` set to something (this is used for the in-app purchase).
2. `app/keystore.properties` with the values `store`, `alias`, `pass`, `storePass` set. This is for signing the release build. Alternatively, comment the `signingConfigs` in `app/build.gradle`.


Credits
-------

[Team Kodi][5] for making an awesome media center.

**Libraries**
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


Links
-----

- [Website][2]
- [Google Play][1]
- [Google+ community][3]


License
-------

    Copyright 2014 Synced Synapse

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


[1]: https://play.google.com/store/apps/details?id=com.syncedsynapse.kore2
[2]: http://syncedsynapse.com/kore/
[3]: https://plus.google.com/u/0/communities/110340113064213296333
[4]: http://developer.android.com/sdk/installing/studio-build.html
[5]: http://kodi.tv/
