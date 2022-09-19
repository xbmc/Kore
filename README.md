[![Build App](https://github.com/xbmc/Kore/actions/workflows/build.yml/badge.svg)](https://github.com/xbmc/Kore/actions/workflows/build.yml)
[![Translations](https://kodi.weblate.cloud/widgets/kodi-remotes/-/kore/svg-badge.svg)](https://kodi.weblate.cloud/engage/kodi-remotes/)

<a href="https://play.google.com/store/apps/details?id=org.xbmc.kore" target="_blank">
  <img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" height="80"/>
</a>
<a href="https://f-droid.org/packages/org.xbmc.kore/" target="_blank">
  <img src="https://f-droid.org/badge/get-it-on.png" height="80"/>
</a>

# Kore - Kodi/XBMC remote for Android

GitHub repository for the [Kore][1] Android app.

Kore is the official remote for [Kodi](http://kodi.tv/), and aims to be a simple and easy to use remote.


## Building

1. Make sure you have a working [Android build system](http://developer.android.com/sdk/installing/studio-build.html);
2. The version of Android SDK and Build Tools needed is specified in app/build.gradle. Make sure you have them installed;
3. Install the version of [Android support library](http://developer.android.com/tools/support-library/setup.html) that is specified in app/gradle (dependencies section);
4. Git pull
5. Gradle should be able to fetch all the other needed libraries.

## Testing

1. Make sure you are able to build Kore as described in the previous section.
2. To run the local tests see [README](https://github.com/xbmc/Kore/blob/master/app/src/test/README.md)
3. To run the instrumented tests see [README](https://github.com/xbmc/Kore/blob/master/app/src/androidTest/README.md)

We use [GitHub Actions](https://github.com/xbmc/Kore/actions) to automatically build and run the local tests for each pull request.

## Using Docker

1. Make sure you have a working [Docker installation](https://docs.docker.com/docker-for-windows/install/);
2. Check out the repository
3. Build the container image: `docker build -t kore:latest .`
4. Start container: `docker run -it -v $(pwd):/opt/kore kore:latest bash`

For listing all tasks, run `gradle tasks`, for building the app, execute `gradle assembleRelease`.
If you want to run tests, execute `gradle testDebugUnitTest`.

## Credits

**Libraries used**
- [Jackson](https://github.com/FasterXML/jackson)
- [Picasso](http://square.github.io/picasso/)
- [OkHttp](http://square.github.io/okhttp/)
- [EventBus](https://github.com/greenrobot/EventBus)
- [JmDNS](https://github.com/jmdns/jmdns)
- [ExpandableTextView](https://github.com/Blogcat/Android-ExpandableTextView)

## Links

- [Kodi forum thread](http://forum.kodi.tv/forumdisplay.php?fid=129)
- [F-Droid](https://f-droid.org/repository/browse/?fdid=org.xbmc.kore)
- [Google Play][1]
- [Google+ community](https://plus.google.com/communities/115506510322045554124)

## License

    Copyright 2022 XBMC Foundation

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

Google Play and the Google Play logo are trademarks of Google Inc.

[1]: https://play.google.com/store/apps/details?id=org.xbmc.kore
