/*
 * Copyright 2015 DanhDroid. All rights reserved.
 * Copyright 2019 Upabjojr. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xbmc.kore.ui.sections.localfile;


import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.xbmc.kore.jsonrpc.type.PlaylistType;

import java.util.regex.Pattern;

public class LocalFileLocation implements Parcelable {
    public final String fileName;
    public final String details;
    public final String sizeDuration;

    public final String fullPath;
    public final boolean isDirectory;
    public final boolean hasParent;

    public LocalFileLocation(String fileName, String path, boolean isDir) {
        this(fileName, path, isDir, null, null);
    }

    static final Pattern noParent = Pattern.compile("/[^/]*/?");
    public LocalFileLocation(String fileName, String path, boolean isDir, String details, String sizeDuration) {
        this.fileName = fileName;
        this.fullPath = path;
        this.isDirectory = isDir;
        this.hasParent = !noParent.matcher(path).matches();

        this.details = details;
        this.sizeDuration = sizeDuration;
    }

    private LocalFileLocation(Parcel in) {
        this.fileName = in.readString();
        this.fullPath = in.readString();
        this.isDirectory = (in.readInt() != 0);
        this.hasParent = (in.readInt() != 0);
        this.details = in.readString();
        this.sizeDuration = in.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(fileName);
        out.writeString(fullPath);
        out.writeInt(isDirectory ? 1 : 0);
        out.writeInt(hasParent ? 1 : 0);
        out.writeString(details);
        out.writeString(sizeDuration);
    }

    public static final Creator<LocalFileLocation> CREATOR = new Creator<LocalFileLocation>() {
        public LocalFileLocation createFromParcel(Parcel in) {
            return new LocalFileLocation(in);
        }

        public LocalFileLocation[] newArray(int size) {
            return new LocalFileLocation[size];
        }
    };

    private String getFileMimeType(final String filename) {
        if (!TextUtils.isEmpty(filename)) {
            int dotPos = filename.lastIndexOf('.');
            if (dotPos >= 0) {
                String extension = filename.substring(dotPos + 1);
                return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
        }
        return "";
    }

    public String getMimeType() {
        return getFileMimeType(fullPath);
    }

    public int getPlaylistTypeId() {
        String mimeType = getMimeType();
        if (TextUtils.isEmpty(mimeType)) {
            return -1;
        } else if (mimeType.matches("video.*")) {
            return PlaylistType.VIDEO_PLAYLISTID;
        } else if (mimeType.matches("audio.*")) {
            return PlaylistType.MUSIC_PLAYLISTID;
        } else if (mimeType.matches("image.*")) {
            return PlaylistType.PICTURE_PLAYLISTID;
        }
        return -1;
    }
}