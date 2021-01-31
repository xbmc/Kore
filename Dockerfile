# Usage: docker build -t kore:latest .
#        docker run -it -v $(pwd):/opt/kore kore:latest bash
#        gradle
FROM ubuntu:20.04

# Install Java
ARG JDK_VERSION=8
RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-${JDK_VERSION}-jdk && \
    apt-get install -y --no-install-recommends git wget unzip

# Install Gradle
# https://services.gradle.org/distributions/
ARG GRADLE_VERSION=6.7
ARG GRADLE_DIST=bin
RUN cd /opt && \
    wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-${GRADLE_DIST}.zip && \
    unzip gradle*.zip && \
    ls -d */ | sed 's/\/*$//g' | xargs -I{} mv {} gradle && \
    rm gradle*.zip

# Install Android SDK and build-tools
# https://developer.android.com/studio#command-tools
ARG ANDROID_SDK_VERSION=6858069
ENV ANDROID_SDK_ROOT /opt/android/sdk
RUN mkdir -p ${ANDROID_SDK_ROOT}/tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip && \
    unzip *tools*linux*.zip -d ${ANDROID_SDK_ROOT} && \
    rm /commandlinetools*linux*.zip

# Install Android build-tools (should match version in ./app/build.gradle)
ARG ANDROID_BUILD_TOOLS_VERSION=29.0.2
RUN yes Y | /opt/android/sdk/cmdline-tools/bin/sdkmanager --sdk_root=${ANDROID_SDK_ROOT} --install "build-tools;${ANDROID_BUILD_TOOLS_VERSION}"
RUN yes Y | /opt/android/sdk/cmdline-tools/bin/sdkmanager --sdk_root=${ANDROID_SDK_ROOT} --licenses

# Set the environment variables
ENV JAVA_HOME /usr/lib/jvm/java-${JDK_VERSION}-openjdk-amd64
ENV GRADLE_HOME /opt/gradle
ENV PATH ${PATH}:${GRADLE_HOME}/bin:${ANDROID_SDK_ROOT}/cmdline-tools/bin:${ANDROID_SDK_ROOT}/tools/bin:${ANDROID_SDK_ROOT}/platform-tools:${ANDROID_SDK_ROOT}/build-tools/${ANDROID_BUILD_TOOLS_VERSION}

WORKDIR /opt/kore
