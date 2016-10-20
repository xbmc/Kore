Integration tests that need to be executed on an Android device.

## Run tests

You can run the tests as follows:

### Android Studio

1. Select build variant "instrumentationTestDebug"
2. Set the (Project view)[https://developer.android.com/studio/projects/index.html] to Android 
3. Right-click on the directory "androidTest" and select "Run tests"

### Commandline

Run the following command from the top of the project:

    ./gradlew connectedInstrumentationTestDebugAndroidTest
    
This will run the tests on all connected devices in parallel