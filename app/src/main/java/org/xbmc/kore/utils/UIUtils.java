/*
 * Copyright 2015 Synced Synapse. All rights reserved.
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
package org.xbmc.kore.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Vibrator;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.TextViewCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.elevation.SurfaceColors;

import org.xbmc.kore.R;
import org.xbmc.kore.Settings;
import org.xbmc.kore.databinding.ItemCastBinding;
import org.xbmc.kore.host.HostInfo;
import org.xbmc.kore.host.HostManager;
import org.xbmc.kore.jsonrpc.type.GlobalType;
import org.xbmc.kore.jsonrpc.type.VideoType;
import org.xbmc.kore.ui.sections.video.AllCastActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * General UI Utils
 */
public class UIUtils {

    public static final float IMAGE_RESIZE_FACTOR = 1.0f;

    public static final int buttonRepeatInterval = 80; // ms
    public static final int buttonVibrationDuration = 50; //ms

    /**
     * Formats time based on seconds
     * @param seconds seconds
     * @return Formated string
     */
    public static String formatTime(int seconds) {
        return formatTime(seconds / 3600, (seconds % 3600) / 60, (seconds % 3600) % 60);
    }

    /**
     * Formats time
     */
    public static String formatTime(GlobalType.Time time) {
        return formatTime(time.hours, time.minutes, time.seconds);
    }

    /**
     * Formats time
     */
    public static String formatTime(int hours, int minutes, int seconds) {
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%1d:%02d",minutes, seconds);
        }
    }

    /**
     * Converts the time format from {@link #formatTime(int, int, int)} to seconds
     * @param time time, according to {@link #formatTime(int, int, int)}
     * @return seconds representation
     */
    public static int timeToSeconds(String time) {
        String[] items = time.split(":");
        if (items.length > 2) {
            return (Integer.parseInt(items[0]) * 3600) + (Integer.parseInt(items[1]) * 60) +
                   (Integer.parseInt(items[2]));
        } else {
            return (Integer.parseInt(items[0]) * 60) + (Integer.parseInt(items[1]));
        }
    }

    /**
     * Formats a file size, ISO prefixes
     */
    public static String formatFileSize(int bytes) {
        if (bytes <= 0) return null;

        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Loads an image into an imageview
     * @param hostManager Hostmanager connected to the host
     * @param imageUrl XBMC url of the image to load
     * @param imageView Image view to load into
     * @param imageWidth Width of the image, for caching purposes
     * @param imageHeight Height of the image, for caching purposes
     */
    public static void loadImageIntoImageview(HostManager hostManager,
                                              String imageUrl, ImageView imageView,
                                              int imageWidth, int imageHeight) {
//        if (TextUtils.isEmpty(imageUrl)) {
//            imageView.setImageResource(R.drawable.delete_ic_action_picture);
//            return;
//        }

        if ((imageWidth) > 0 && (imageHeight > 0)) {
            hostManager.getPicasso()
                       .load(hostManager.getHostInfo().getImageUrl(imageUrl))
                       .resize(imageWidth, imageHeight)
                       .centerCrop()
                       .into(imageView);
        } else {
            hostManager.getPicasso()
                       .load(hostManager.getHostInfo().getImageUrl(imageUrl))
                       .fit()
                       .centerCrop()
                       .into(imageView);
        }
    }

    private static TypedArray characterAvatarColors = null;
//    private static Random randomGenerator = new Random();

    /**
     * Loads an image into an imageview, presenting an alternate charater avatar if empty
     * @param hostManager Hostmanager connected to the host
     * @param imageUrl XBMC url of the image to load
     * @param stringAvatar Character avatar too present if image is null
     * @param imageView Image view to load into
     * @param imageWidth Width of the image, for caching purposes
     * @param imageHeight Height of the image, for caching purposes
     */
    public static void loadImageWithCharacterAvatar(
            Context context, HostManager hostManager,
            String imageUrl, String stringAvatar,
            ImageView imageView,
            int imageWidth, int imageHeight) {

        CharacterDrawable avatarDrawable = getCharacterAvatar(context, stringAvatar);
        if (TextUtils.isEmpty(imageUrl)) {
            imageView.setImageDrawable(avatarDrawable);
            return;
        }

        if ((imageWidth) > 0 && (imageHeight > 0)) {
            hostManager.getPicasso()
                       .load(hostManager.getHostInfo().getImageUrl(imageUrl))
                       .placeholder(avatarDrawable)
                       .resize(imageWidth, imageHeight)
                       .centerCrop()
                       .into(imageView);
        } else {
            hostManager.getPicasso()
                       .load(hostManager.getHostInfo().getImageUrl(imageUrl))
                       .fit()
                       .centerCrop()
                       .into(imageView);
        }
    }

    /**
     * Returns a CharacterDrawable that is suitable to use as an avatar
     * @param context Context
     * @param str String to use to create the avatar
     * @return Character avatar to use in a image view
     */
    public static CharacterDrawable getCharacterAvatar(Context context, String str) {
        // Load character avatar
        if (characterAvatarColors == null) {
            characterAvatarColors = context
                    .getResources()
                    .obtainTypedArray(R.array.character_avatar_colors);
        }

        char charAvatar = TextUtils.isEmpty(str) ?
                          ' ' : str.charAt(0);
        int avatarColorsIdx = TextUtils.isEmpty(str) ? 0 :
                              Math.max(Character.getNumericValue(str.charAt(0)) +
                                       Character.getNumericValue(str.charAt(str.length() - 1)) +
                                       str.length(), 0) % characterAvatarColors.length();
        int color = characterAvatarColors.getColor(avatarColorsIdx, 0xff000000);
//            avatarColorsIdx = randomGenerator.nextInt(characterAvatarColors.length());
        return new CharacterDrawable(charAvatar, color);
    }

    /**
     * Sets play/pause button icon on a ImageView
     * @param context Activity
     * @param view ImageView/ImageButton
     * @param play true if playing, false if paused
     */
    public static void setPlayPauseButtonIcon(Context context, ImageView view, boolean play) {
        view.setImageResource(play ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp );
    }

    /**
     * Fills the standard cast info list, consisting of a {@link android.widget.GridLayout}
     * with actor images and a Textview with the name and the role of the additional cast.
     * The number of actor presented on the {@link android.widget.GridLayout} is controlled
     * through the global setting, and only actors with images are presented.
     * The rest are presented in the additionalCastView TextView
     *
     * @param activity Activity
     * @param castList Cast list
     * @param castListView GridLayout on which too show actors that have images
     */
    public static void setupCastInfo(final Activity activity,
                                     List<VideoType.Cast> castList, GridLayout castListView,
                                     final Intent allCastActivityLaunchIntent) {
        HostManager hostManager = HostManager.getInstance(activity);
        Resources resources = activity.getResources();

        View.OnClickListener castListClickListener = v -> Utils.openImdbForPerson(activity, (String)v.getTag());

        castListView.removeAllViews();
        int numColumns = castListView.getColumnCount();
        int numRows = resources.getInteger(R.integer.cast_grid_view_rows);
        int maxCastPictures = numColumns * numRows;

        // Calculate image size. Note: we're assuming that the Grid fills the entire screen, otherwise this won't work
        // Furthermore, we need to make sure that the image size is less than the one available on the grid, including
        // any margins that are set, so we scale the size by a factor. This is fixed when placed on the View
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager)activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);
        int imageMarginPx = resources.getDimensionPixelSize(R.dimen.small_padding);
        int imageWidth = (displayMetrics.widthPixels - (2 + numColumns - 1) * imageMarginPx) / numColumns;
        int imageHeight = (int)(imageWidth * AllCastActivity.ART_HEIGHT_RATIO);

        for (int i = 0; i < Math.min(castList.size(), maxCastPictures); i++) {
            VideoType.Cast actor = castList.get(i);

            ItemCastBinding binding = ItemCastBinding.inflate(LayoutInflater.from(activity), castListView, false);
            View castView = binding.getRoot();

            castView.getLayoutParams().width = (int)(imageWidth * 0.9f);
            castView.getLayoutParams().height = (int)(imageHeight * 0.9f);
            castView.setTag(actor.name);

            loadImageWithCharacterAvatar(activity, hostManager,
                                         actor.thumbnail, actor.name,
                                         binding.picture, imageWidth, imageHeight);

            if ((i == maxCastPictures - 1) && (castList.size() > i + 1)) {
                binding.name.setVisibility(View.GONE);
                binding.role.setVisibility(View.GONE);
                binding.remainingCastCount.setVisibility(View.VISIBLE);
                binding.remainingCastCount.setText(String.format(activity.getString(R.string.remaining_cast_count), castList.size() - maxCastPictures + 1));

                castView.setOnClickListener(v -> {
                    activity.startActivity(allCastActivityLaunchIntent);
                    activity.overridePendingTransition(R.anim.activity_enter, R.anim.activity_exit);
                });
            } else {
                binding.name.setText(actor.name);
                binding.role.setText(actor.role);
                castView.setOnClickListener(castListClickListener);
            }

            castListView.addView(castView);
        }
    }

    /**
     * Simple wrapper to {@link NetUtils#sendWolMagicPacket(String, String, int)}
     * that sends a WoL magic packet in a new thread
     *
     * @param context Context
     * @param hostInfo Host to send WoL
     */
    public static void sendWolAsync(Context context, final HostInfo hostInfo) {
        if (hostInfo == null)
            return;

        // Send WoL magic packet on a new thread
        new Thread(() -> NetUtils.sendWolMagicPacket(hostInfo.getMacAddress(),
                                             hostInfo.getAddress(), hostInfo.getWolPort())).start();
        Toast.makeText(context, R.string.wol_sent, Toast.LENGTH_SHORT).show();
    }

    /**
     * Tints the system status and navigation bars with an appropriate Surface color for Material 3 theming
     * @param activity Activity
     */
    public static void tintSystemBars(Activity activity) {
        int color = SurfaceColors.SURFACE_2.getColor(activity);
        if (Utils.isMOrLater())
            activity.getWindow().setStatusBarColor(color);
        if (Utils.isOreoMR1OrLater())
            activity.getWindow().setNavigationBarColor(color);
    }

    public static void tintElevatedView(View v) {
        v.setBackgroundTintList(ColorStateList.valueOf(SurfaceColors.getColorForElevation(v.getContext(), v.getElevation())));
    }

    /**
     * Returns the view color with the alpha component changed.
     * @param v Ciew
     * @return Translucid view color
     */
    public static int getTranslucidViewColor(View v, float alpha) {
        return changeColorAlpha(getViewBackgroundColor(v), (int)(alpha * 0xFF));
    }

    /**
     * Returns the view background color, if the background is a color, Transparent otherwise
     * @param v View
     * @return View background color, if available, else Transparent
     */
    public static int getViewBackgroundColor(View v) {
        Drawable background = v.getBackground();
        if (background instanceof ColorDrawable) {
            return ((ColorDrawable) background).getColor();
        }
        return Color.TRANSPARENT;
    }

    /**
     * Changes the alpha component of a color
     * @param color Color to change
     * @param alpha New alpha to set
     * @return color with new alpha
     */
    public static int changeColorAlpha(int color, float alpha)
    {
        return changeColorAlpha(color, (int)(alpha * 0xff));
    }

    /**
     * Changes the alpha component of a color
     * @param color Color to change
     * @param alpha New alpha to set
     * @return color with new alpha
     */
    public static int changeColorAlpha(int color, int alpha)
    {
        return (alpha << 24) | (color & 0x00ffffff);
    }

    /**
     * Returns a theme resource Id given the value stored in Shared Preferences
     * @param prefThemeValue Shared Preferences value for the theme
     * @return Android resource id of the theme
     */
    public static int getThemeResourceId(String prefThemeValue) {
        switch (Integer.parseInt(prefThemeValue)) {
            case 0:
                return R.style.Theme_Kore_Dark;
            case 1:
                return R.style.Theme_Kore_Light;
            case 2:
                return R.style.Theme_Kore_Mist;
            case 3:
                return R.style.Theme_Kore_Sunrise;
            case 4:
                return R.style.Theme_Kore_Sunset;
            case 9:
            default:
                return R.style.Theme_Kore_SystemDefault;
        }
    }

    public static void handleVibration(Context context) {
        if(context == null) return;

        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (!vibrator.hasVibrator()) return;

        //Check if we should vibrate
        boolean vibrateOnPress = PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean(Settings.KEY_PREF_VIBRATE_REMOTE_BUTTONS,
                            Settings.DEFAULT_PREF_VIBRATE_REMOTE_BUTTONS);
        if (vibrateOnPress) {
            vibrator.vibrate(UIUtils.buttonVibrationDuration);
        }
    }

    /**
     * Returns true if {@param view} is visible within {@param container}'s bounds.
     */
    public static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }


    /**
     * Downloads a list of songs. Asks the user for confirmation if one or more songs
     * already exist on the device
     * @param context required to show the user a dialog
     * @param songInfoList the song infos for the songs that need to be downloaded
     * @param hostInfo the host info from which the songs should be downloaded
     * @param callbackHandler Thread handler that should be used to handle the download result
     */
    public static void downloadSongs(final Context context,
                                     final ArrayList<FileDownloadHelper.SongInfo> songInfoList,
                                     final HostInfo hostInfo,
                                     final Handler callbackHandler) {
        if (songInfoList == null || songInfoList.size() == 0) {
            Toast.makeText(context, R.string.no_songs_to_download, Toast.LENGTH_LONG).show();
            return;
        }

        // Check if any file exists and whether to overwrite it
        boolean someFilesExist = false;
        for (FileDownloadHelper.SongInfo songInfo : songInfoList) {
            File file = new File(songInfo.getAbsoluteFilePath());
            if (file.exists()) {
                someFilesExist = true;
                break;
            }
        }

        if (someFilesExist) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(R.string.download)
                   .setMessage(songInfoList.size() > 1 ? R.string.download_files_exists : R.string.download_file_exists)
                   .setPositiveButton(R.string.overwrite,
                                      (dialog, which) -> FileDownloadHelper.downloadFiles(context, hostInfo,
                                                                       songInfoList, FileDownloadHelper.OVERWRITE_FILES,
                                                                       callbackHandler))
                   .setNeutralButton(R.string.download_with_new_name,
                                     (dialog, which) -> FileDownloadHelper.downloadFiles(context, hostInfo,
                                                                      songInfoList, FileDownloadHelper.DOWNLOAD_WITH_NEW_NAME,
                                                                      callbackHandler))
                   .setNegativeButton(android.R.string.cancel,
                                      (dialog, which) -> { })
                   .show();
        } else {
            if (songInfoList.size() > 12) { // No scientific reason this should be 12. I just happen to like 12.
                String message = context.getResources().getString(R.string.confirm_songs_download);
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                builder.setTitle(R.string.download)
                       .setMessage(String.format(message, songInfoList.size()))
                       .setPositiveButton(android.R.string.ok,
                                          (dialog, which) -> FileDownloadHelper.downloadFiles(context, hostInfo,
                                                                           songInfoList, FileDownloadHelper.OVERWRITE_FILES,
                                                                           callbackHandler))
                       .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                       })
                       .show();
            } else {
                FileDownloadHelper.downloadFiles(context, hostInfo,
                                                 songInfoList, FileDownloadHelper.DOWNLOAD_WITH_NEW_NAME,
                                                 callbackHandler);
            }
        }
    }

    /**
     * Returns a {@link Runnable} that sets up toggleable scrolling behavior on a {@link TextView}
     * if the number of lines to be displayed exceeds the maximum lines limit supported by the TextView.
     * Can be applied by using {@link View#post(Runnable)}.
     *
     * @param textView TextView that the Runnable should apply against
     * @return Runnable
     */
    public static Runnable getMarqueeToggleableAction(final TextView textView) {
        return () -> {
            int lines = textView.getLineCount();
            int maxLines = TextViewCompat.getMaxLines(textView);
            if (lines > maxLines) {
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setClickable(true);
                textView.setOnClickListener(v -> {
                    v.setSelected(!v.isSelected());
                    TextUtils.TruncateAt ellipsize;
                    if (v.isSelected()) {
                        ellipsize = TextUtils.TruncateAt.MARQUEE;
                    } else {
                        ellipsize = TextUtils.TruncateAt.END;
                    }
                    textView.setEllipsize(ellipsize);
                    textView.setHorizontallyScrolling(v.isSelected());
                });
            }
        };
    }

    /**
     * Replaces some BBCode-ish tagged text with styled spans.
     * <p>
     * Recognizes and styles COLOR, CR, B, I, UPPERCASE, LOWERCASE and CAPITALIZE; recognizes
     * and strips out LIGHT. This is very strict/dumb, it only recognizes
     * uppercase tags with no spaces around them.
     *
     * @param context Activity context needed to resolve the style resources
     * @param src The text to style
     * @return a styled CharSequence that can be passed to a {@link TextView#setText(CharSequence)}
     * or derivatives.
     */
    public static SpannableStringBuilder applyMarkup(Context context, String src) {
        if (src == null) {
            return null;
        }
        SpannableStringBuilder sb = new SpannableStringBuilder();
        int start = src.indexOf('[');
        if (start == -1) {
            sb.append(src);
            return sb;
        }
        if (start > 0) {
            sb.append(src, 0, start);
        }
        Nestable upper = new Nestable();
        Nestable lower = new Nestable();
        Nestable title = new Nestable();
        Nestable bold = new Nestable();
        Nestable italic = new Nestable();
        Nestable light = new Nestable();
        Nestable color = new Nestable();
        Pattern colorTag = Pattern.compile("^\\[COLOR ([^\\]]+)\\]");
        String colorName = "white";
        for (int i = start, length = src.length(); i < length;) {
            String s = src.substring(i);
            int nextTag = s.indexOf('[');
            if (nextTag == -1) {
                sb.append(s);
                break;
            }
            if (nextTag > 0) {
                sb.append(s, 0, nextTag);
                i += nextTag;
            } else if (s.startsWith("[CR]")) {
                sb.append('\n');
                i += 4;
            } else if (s.startsWith("[UPPERCASE]")) {
                if (upper.start()) {
                    upper.index = sb.length();
                }
                i += 11;
            } else if (s.startsWith("[/UPPERCASE]")) {
                if (upper.end()) {
                    String sub = sb.subSequence(upper.index, sb.length()).toString();
                    sb.replace(upper.index, sb.length(), sub.toUpperCase(Locale.getDefault()));
                } else if (upper.imbalanced()) {
                    sb.append("[/UPPERCASE]");
                }
                i += 12;
            } else if (s.startsWith("[B]")) {
                if (bold.start()) {
                    bold.index = sb.length();
                }
                i += 3;
            } else if (s.startsWith("[/B]")) {
                if (bold.end()) {
                    sb.setSpan(new TextAppearanceSpan(context, R.style.TextAppearanceSpanBold),
                            bold.index, sb.length(), 0);
                } else if (bold.imbalanced()) {
                    sb.append("[/B]");
                }
                i += 4;
            } else if (s.startsWith("[I]")) {
                if (italic.start()) {
                    italic.index = sb.length();
                }
                i += 3;
            } else if (s.startsWith("[/I]")) {
                if (italic.end()) {
                    sb.setSpan(new TextAppearanceSpan(context, R.style.TextAppearanceSpanItalic),
                            italic.index, sb.length(), 0);
                } else if (italic.imbalanced()) {
                    sb.append("[/I]");
                }
                i += 4;
            } else if (s.startsWith("[LOWERCASE]")) {
                if (lower.start()) {
                    lower.index = sb.length();
                }
                i += 11;
            } else if (s.startsWith("[/LOWERCASE]")) {
                if (lower.end()) {
                    String sub = sb.subSequence(lower.index, sb.length()).toString();
                    sb.replace(lower.index, sb.length(), sub.toLowerCase(Locale.getDefault()));
                } else if (lower.imbalanced()) {
                    sb.append("[/LOWERCASE]");
                }
                i += 12;
            } else if (s.startsWith("[CAPITALIZE]")) {
                if (title.start()) {
                    title.index = sb.length();
                }
                i += 12;
            } else if (s.startsWith("[/CAPITALIZE]")) {
                if (title.end()) {
                    String sub = sb.subSequence(title.index, sb.length()).toString();
                    sb.replace(title.index, sb.length(), toTitleCase(sub));
                } else if (title.imbalanced()) {
                    sb.append("[/CAPITALIZE]");
                }
                i += 13;
            } else if (s.startsWith("[LIGHT]")) {
                light.start();
                i += 7;
            } else if (s.startsWith("[/LIGHT]")) {
                light.end();
                if (light.imbalanced()) {
                    sb.append("[/LIGHT]");
                }
                i += 8;
            } else if (s.startsWith("[/COLOR]")) {
                if(color.end()) {
                    int colorId = context.getResources().getIdentifier(colorName, "color", context.getPackageName());
                    ForegroundColorSpan foregroundColorSpan;
                    try{
                        foregroundColorSpan = new ForegroundColorSpan(context.getResources().getColor(colorId));
                    }catch (Resources.NotFoundException nfe){
                        foregroundColorSpan = new ForegroundColorSpan(Color.WHITE);
                    }
                    sb.setSpan(foregroundColorSpan, color.index, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                else if (color.imbalanced()) {
                    sb.append("[/COLOR]");
                }
                i += 8;
            } else {
                Matcher m = colorTag.matcher(s);
                if (m.find()) {
                    color.start();
                    colorName = m.group(1);
                    color.index = sb.length();
                    i += m.end();
                } else {
                    sb.append('[');
                    i += 1;
                }
            }
        }
        return sb;
    }

    private static class Nestable {
        int index = 0;
        int level = 0;

        /**
         * @return true if we just opened the first tag
         */
        boolean start() {
            return level++ == 0;
        }

        /**
         * @return true if we just closed the last open tag
         */
        boolean end() {
            return --level == 0;
        }

        /**
         * This must be called after every {@link #end()}.
         * @return true if we found a close tag when there are no open tags
         */
        boolean imbalanced() {
            if (level < 0) {
                level = 0;
                return true;
            }
            return false;
        }
    }

    /**
     * Converts the given string to Title Case
     * @param text String to convert
     * @return String in Title Case
     */
    public static String toTitleCase(String text) {
        StringBuilder sb = new StringBuilder();
        for (String word : text.toLowerCase(Locale.getDefault()).split("\\b")) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0)));
            sb.append(word, 1, word.length());
        }
        return sb.toString();
    }

    /**
     * Creates a ViewTreeObserver that fades the imageview and rounds the corners of the layout
     * @param context Context
     * @param scrollView Scrollview to get the scroll from
     * @param artView ImageView to fade
     * @param infoPanelView Panel to round corners
     * @return ViewTreeObserver.OnScrollChangedListener
     */
    public static ViewTreeObserver.OnScrollChangedListener createInfoPanelScrollChangedListener(
            Context context,
            View scrollView,
            ImageView artView,
            View infoPanelView) {
        float pixelsToFade  = 0.7f * context.getResources().getDimension(R.dimen.info_art_height);
        float finalCornerRadius = context.getResources().getDimension(R.dimen.corner_radius_image_poster);

        return new ViewTreeObserver.OnScrollChangedListener() {
            private float lastAppliedAlpha = -1, lastAppliedRadius = -1;

            @Override
            public void onScrollChanged() {
                if (scrollView == null) return;
                float scrollY = scrollView.getScrollY();

                float newAlpha = 1 - (scrollY / pixelsToFade);
                if (lastAppliedAlpha != 0) {
                    lastAppliedAlpha = newAlpha;
                    artView.setAlpha(Utils.clamp(newAlpha, 0, 1));
                    artView.setTranslationY(-0.5f * scrollY);
                }

                float newRadius = Utils.clamp(0.5f * scrollY, 0, finalCornerRadius);
                if (lastAppliedRadius <= finalCornerRadius) {
                    lastAppliedRadius = newRadius;
                    GradientDrawable panelBackground = (GradientDrawable) infoPanelView.getBackground();
                    panelBackground.setCornerRadii(new float[]{newRadius, newRadius, newRadius, newRadius,
                                                               finalCornerRadius, finalCornerRadius, finalCornerRadius, finalCornerRadius});
                }
            }
        };
    }
}
