Local tests that do NOT need to be executed on an Android device

## Run tests

You can run the tests as follows:

### Android Studio

1. Make sure in the Run/Debug Configurations - Android JUnit - Defaults for Working directory: `$MODULE_DIR$` is set.
2. Select build variant "fullDebug"
3. Set the [Project view](https://developer.android.com/studio/projects/index.html) to Android
4. Right-click on the directory "test" and select "Run tests"

### Commandline

Run the following command from the top of the project:

    ./gradlew testFullDebugUnitTest