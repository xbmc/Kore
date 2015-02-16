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
package com.syncedsynapse.kore2.utils;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.syncedsynapse.kore2.R;
import com.syncedsynapse.kore2.Settings;
import com.syncedsynapse.kore2.host.HostInfo;
import com.syncedsynapse.kore2.host.HostManager;
import com.syncedsynapse.kore2.jsonrpc.type.GlobalType;
import com.syncedsynapse.kore2.jsonrpc.type.VideoType;
import com.syncedsynapse.kore2.ui.RemoteActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * General UI Utils
 */
public class UIUtils {

    public static final float IMAGE_RESIZE_FACTOR = 1.0f;

    public static final int initialButtonRepeatInterval = 400; // ms
    public static final int buttonRepeatInterval = 80; // ms

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
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%1d:%02d",minutes, seconds);
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
    private static int avatarColorsIdx = 0;
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
        avatarColorsIdx = TextUtils.isEmpty(str) ? 0 :
                Math.max(Character.getNumericValue(str.charAt(0)) +
                        Character.getNumericValue(str.charAt(str.length() - 1)) +
                        str.length(), 0) % characterAvatarColors.length();
        int color = characterAvatarColors.getColor(avatarColorsIdx, 0xff000000);
//            avatarColorsIdx = randomGenerator.nextInt(characterAvatarColors.length());
        return new CharacterDrawable(charAvatar, color);
    }

    /**
     * Sets play/pause button icon on a ImageView, based on speed
     * @param context Activity
     * @param view ImageView/ImageButton
     * @param speed Current player speed
     */
    public static void setPlayPauseButtonIcon(Context context, ImageView view, int speed) {
        int resAttrId = (speed == 1) ? R.attr.iconPause : R.attr.iconPlay;
        int defaultResourceId = (speed == 1) ?
                                R.drawable.ic_pause_white_24dp :
                                R.drawable.ic_play_arrow_white_24dp;

        TypedArray styledAttributes = context.obtainStyledAttributes(new int[]{resAttrId});
        view.setImageResource(styledAttributes.getResourceId(0, defaultResourceId));
        styledAttributes.recycle();
    }

    /**
     * Fills the standard cast info list, consisting of a {@link android.widget.GridLayout}
     * with actor images and a Textview with the name and the role of the additional cast.
     * The number of actor presented on the {@link android.widget.GridLayout} is controlled
     * through the global setting, and only actors with images are presented.
     * The rest are presented in the additionalCastView TextView
     *
     * @param context Activity
     * @param castList Cast list
     * @param castListView GridLayout on which too show actors that have images
     * @param additionalCastTitleView View with additional cast title
     * @param additionalCastView Additional cast
     */
    public static void setupCastInfo(final Context context,
                               List<VideoType.Cast> castList, GridLayout castListView,
                               TextView additionalCastTitleView, TextView additionalCastView) {
        HostManager hostManager = HostManager.getInstance(context);
        Resources resources = context.getResources();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        View.OnClickListener castListClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.openImdbForPerson(context, (String)v.getTag());
            }
        };


        castListView.removeAllViews();
        int numColumns = castListView.getColumnCount();

        int layoutMarginPx = 2 * resources.getDimensionPixelSize(R.dimen.remote_content_hmargin);
        int imageMarginPx = 2 * resources.getDimensionPixelSize(R.dimen.image_grid_margin);
        int imageWidth = (displayMetrics.widthPixels - layoutMarginPx - numColumns * imageMarginPx) / numColumns;
        int imageHeight = (int)(imageWidth * 1.2);

        List<VideoType.Cast> noPicturesCastList = new ArrayList<VideoType.Cast>();
        int maxCastPictures = Settings.DEFAULT_MAX_CAST_PICTURES;
        int currentPictureNumber = 0;
        for (int i = 0; i < castList.size(); i++) {
            VideoType.Cast actor = castList.get(i);

            if (((maxCastPictures == -1) || (currentPictureNumber < maxCastPictures)) &&
                    (actor.thumbnail != null)) {
                // Present the picture
                currentPictureNumber++;
                View castView = LayoutInflater.from(context).inflate(R.layout.grid_item_cast, castListView, false);
                ImageView castPicture = (ImageView) castView.findViewById(R.id.picture);
                TextView castName = (TextView) castView.findViewById(R.id.name);
                TextView castRole = (TextView) castView.findViewById(R.id.role);

                castView.getLayoutParams().width = imageWidth;
                castView.getLayoutParams().height = (int) (imageHeight * 1.2);
                castView.setTag(actor.name);
                castView.setOnClickListener(castListClickListener);

                castName.setText(actor.name);
                castRole.setText(actor.role);
                UIUtils.loadImageWithCharacterAvatar(context, hostManager,
                        actor.thumbnail, actor.name,
                        castPicture, imageWidth, imageHeight);
                castListView.addView(castView);
            } else {
                noPicturesCastList.add(actor);
            }
        }

        // Additional cast
        if (noPicturesCastList.size() > 0) {
            additionalCastTitleView.setVisibility(View.VISIBLE);
            additionalCastView.setVisibility(View.VISIBLE);
            StringBuilder castListText = new StringBuilder();
            boolean first = true;
            for (VideoType.Cast cast : noPicturesCastList) {
                if (!first) castListText.append("\n");
                first = false;
                if (!TextUtils.isEmpty(cast.role)) {
                    castListText.append(String.format(context.getString(R.string.cast_list_text),
                            cast.name, cast.role));
                } else {
                    castListText.append(cast.name);
                }
            }
            additionalCastView.setText(castListText);
        } else {
            additionalCastTitleView.setVisibility(View.GONE);
            additionalCastView.setVisibility(View.GONE);
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                NetUtils.sendWolMagicPacket(hostInfo.getMacAddress(),
                        hostInfo.getAddress(), hostInfo.getWolPort());
            }
        }).start();
        Toast.makeText(context, R.string.wol_sent, Toast.LENGTH_SHORT).show();
    }

//    /**
//     * Sets the default {@link android.support.v4.widget.SwipeRefreshLayout} color scheme
//     * @param swipeRefreshLayout layout
//     */
//    public static void setSwipeRefreshLayoutColorScheme(SwipeRefreshLayout swipeRefreshLayout) {
//        Resources.Theme theme = swipeRefreshLayout.getContext().getTheme();
//        TypedArray styledAttributes = theme.obtainStyledAttributes(new int[] {
//                R.attr.refreshColor1,
//                R.attr.refreshColor2,
//                R.attr.refreshColor3,
//                R.attr.refreshColor4,
//        });
//
//        swipeRefreshLayout.setColorScheme(styledAttributes.getResourceId(0, android.R.color.holo_blue_dark),
//                styledAttributes.getResourceId(1, android.R.color.holo_purple),
//                styledAttributes.getResourceId(2, android.R.color.holo_red_dark),
//                styledAttributes.getResourceId(3, android.R.color.holo_green_dark));
//        styledAttributes.recycle();
//    }

//    /**
//     * Sets a views padding top/bottom to account for the system bars
//     * (Top status and action bar, bottom nav bar, right nav bar if in ladscape mode)
//     *
//     * @param context Context
//     * @param view View to pad
//     * @param padTop Whether to set views paddingTop
//     * @param padRight Whether to set views paddingRight (for nav bar in landscape mode)
//     * @param padBottom Whether to set views paddingBottom
//     */
//    public static void setPaddingForSystemBars(Activity context, View view,
//                                               boolean padTop, boolean padRight, boolean padBottom) {
//        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;
//        SystemBarTintManager tintManager = new SystemBarTintManager(context);
//        SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
//
//        view.setPadding(view.getPaddingLeft(),
//                padTop ? config.getPixelInsetTop(true) : view.getPaddingTop(),
//                padRight? config.getPixelInsetRight() : view.getPaddingRight(),
//                padBottom ? config.getPixelInsetBottom() : view.getPaddingBottom());
//    }

    /**
     * Returns a theme resource Id given the value stored in Shared Preferences
     * @param prefThemeValue Shared Preferences value for the theme
     * @return Android resource id of the theme
     */
    public static int getThemeResourceId(String prefThemeValue) {
        switch (Integer.valueOf(prefThemeValue)) {
            case 0:
                return R.style.NightTheme;
            case 1:
                return R.style.DayTheme;
            case 2:
                return R.style.MistTheme;
            case 3:
                return R.style.SolarizedLightTheme;
            case 4:
                return R.style.SolarizedDarkTheme;
            default:
                return R.style.NightTheme;
        }
    }

    /**
     * Launches the remote activity, performing a circular reveal animation if
     * Lollipop or later
     *
     * @param context Context
     * @param centerX Center X of the animation
     * @param centerY Center Y of the animation
     * @param exitTransitionView View to reveal. Should occupy the whole screen and
     *                           be invisible before calling this
     */
    @TargetApi(21)
    public static void switchToRemoteWithAnimation(final Context context,
                                                   int centerX, int centerY,
                                                   final View exitTransitionView) {
        final Intent launchIntent = new Intent(context, RemoteActivity.class);
        if (Utils.isLollipopOrLater()) {
            // Show the animation
            int endRadius = Math.max(exitTransitionView.getHeight(), exitTransitionView.getWidth());
            Animator exitAnim = ViewAnimationUtils.createCircularReveal(exitTransitionView,
                    centerX, centerY, 0, endRadius);

            exitAnim.setDuration(200);
            exitAnim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}

                @Override
                public void onAnimationEnd(Animator animation) {
                    // Launch remote activity
                    context.startActivity(launchIntent);
                }

                @Override public void onAnimationCancel(Animator animation) {}

                @Override
                public void onAnimationRepeat(Animator animation) {}
            });
            exitTransitionView.setVisibility(View.VISIBLE);
            exitAnim.start();
        } else {
            // No animation show, just launch the remote
            context.startActivity(launchIntent);
        }
    }
}
