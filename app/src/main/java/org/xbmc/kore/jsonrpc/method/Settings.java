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
package org.xbmc.kore.jsonrpc.method;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.xbmc.kore.jsonrpc.ApiException;
import org.xbmc.kore.jsonrpc.ApiMethod;

/**
 * All JSON RPC methods in Settings.*
 */
public class Settings {
    // Get the settings from:
    // https://github.com/xbmc/xbmc/blob/master/system/settings/settings.xml

    // The settings are only defined here on as as-needed basis
//    lookandfeel.skin
//    lookandfeel.skinsettings
//    lookandfeel.skintheme
//    lookandfeel.skincolors
//    lookandfeel.font
//    lookandfeel.skinzoom
//    lookandfeel.startupwindow
//    lookandfeel.soundskin
//    lookandfeel.stereostrength
//    lookandfeel.enablerssfeeds
//    lookandfeel.rssedit
//    locale.language
//    locale.country
//    locale.charset
//    locale.keyboardlayouts
//    locale.timezonecountry
//    locale.timezone
//    locale.shortdateformat
//    locale.longdateformat
//    locale.timeformat
//    locale.use24hourclock
//    locale.temperatureunit
//    locale.speedunit
//    filelists.showparentdiritems
//    filelists.showextensions
//    filelists.ignorethewhensorting
//    filelists.allowfiledeletion
//    filelists.showaddsourcebuttons
//    filelists.showhidden
//    screensaver.mode
//    screensaver.settings
//    screensaver.preview
//    screensaver.time
//    screensaver.usemusicvisinstead
//    screensaver.usedimonpause
//    window.width
//    window.height
//    videolibrary.showunwatchedplots
//    videolibrary.actorthumbs
//    myvideos.flatten
//    videolibrary.flattentvshows
//    videolibrary.tvshowsselectfirstunwatcheditem
//    videolibrary.tvshowsincludeallseasonsandspecials
//    videolibrary.showallitems
//    videolibrary.groupmoviesets
//    videolibrary.groupsingleitemsets
//    videolibrary.showemptytvshows
//    videolibrary.updateonstartup
//    videolibrary.backgroundupdate
//    videolibrary.cleanup
//    videolibrary.export
//    videolibrary.import
//    locale.audiolanguage
//    videoplayer.preferdefaultflag
//    videoplayer.autoplaynextitem
//    videoplayer.seeksteps
//    videoplayer.seekdelay
//    videoplayer.adjustrefreshrate
//    videoplayer.usedisplayasclock
//    videoplayer.errorinaspect
//    videoplayer.stretch43
//    videoplayer.teletextenabled
//    videoplayer.teletextscale
//    videoplayer.stereoscopicplaybackmode
//    videoplayer.quitstereomodeonstop
//    videoplayer.rendermethod
//    videoplayer.hqscalers
//    videoplayer.useamcodec
//    videoplayer.usevdpau
//    videoplayer.usevdpaumixer
//    videoplayer.usevdpaumpeg2
//    videoplayer.usevdpaumpeg4
//    videoplayer.usevdpauvc1
//    videoplayer.usevaapi
//    videoplayer.usevaapimpeg2
//    videoplayer.usevaapimpeg4
//    videoplayer.usevaapivc1
//    videoplayer.prefervaapirender
//    videoplayer.usedxva2
//    videoplayer.useomxplayer
//    videoplayer.useomx
//    videoplayer.usevideotoolbox
//    videoplayer.usevda
//    myvideos.selectaction
//    myvideos.extractflags
//    myvideos.extractchapterthumbs
//    myvideos.replacelabels
//    myvideos.extractthumb
//    myvideos.stackvideos
//    locale.subtitlelanguage
//    subtitles.parsecaptions
//    subtitles.align
//    subtitles.stereoscopicdepth
//    subtitles.font
//    subtitles.height
//    subtitles.style
//    subtitles.color
//    subtitles.charset
//    subtitles.overrideassfonts
//    subtitles.languages
//    subtitles.storagemode
//    subtitles.custompath
//    subtitles.pauseonsearch
//    subtitles.downloadfirst
//    subtitles.tv
//    subtitles.movie
//    dvds.autorun
//    dvds.playerregion
//    dvds.automenu
//    bluray.playerregion
//    disc.playback
//    accessibility.audiovisual
//    accessibility.audiohearing
//    accessibility.subhearing
//    scrapers.moviesdefault
//    scrapers.tvshowsdefault
//    scrapers.musicvideosdefault
//    pvrmanager.enabled
//    pvrmanager.hideconnectionlostwarning
//    pvrmanager.syncchannelgroups
//    pvrmanager.backendchannelorder
//    pvrmanager.usebackendchannelnumbers
//    pvrmanager.channelmanager
//    pvrmanager.groupmanager
//    pvrmanager.channelscan
//    pvrmanager.resetdb
//    pvrmenu.displaychannelinfo
//    pvrmenu.closechannelosdonswitch
//    pvrmenu.iconpath
//    pvrmenu.searchicons
//    epg.daystodisplay
//    epg.selectaction
//    epg.hidenoinfoavailable
//    epg.epgupdate
//    epg.preventupdateswhileplayingtv
//    epg.ignoredbforclient
//    epg.resetepg
//    pvrplayback.playminimized
//    pvrplayback.startlast
//    pvrplayback.signalquality
//    pvrplayback.scantime
//    pvrplayback.confirmchannelswitch
//    pvrplayback.channelentrytimeout
//    pvrplayback.fps
//    pvrplayback.enableradiords
//    pvrplayback.trafficadvisory
//    pvrplayback.trafficadvisoryvolume
//    pvrplayback.sendrdstrafficmsg
//    pvrrecord.instantrecordtime
//    pvrrecord.defaultpriority
//    pvrrecord.defaultlifetime
//    pvrrecord.marginstart
//    pvrrecord.marginend
//    pvrrecord.preventduplicateepisodes
//    pvrrecord.timernotifications
//    pvrpowermanagement.enabled
//    pvrpowermanagement.backendidletime
//    pvrpowermanagement.setwakeupcmd
//    pvrpowermanagement.prewakeup
//    pvrpowermanagement.dailywakeup
//    pvrpowermanagement.dailywakeuptime
//    pvrparental.enabled
//    pvrparental.pin
//    pvrparental.duration
//    pvrclient.menuhook
//    pvrtimers.timertypefilter
//    pvrtimers.hidedisabledtimers
//    musiclibrary.showcompilationartists
//    musiclibrary.downloadinfo
//    musiclibrary.albumsscraper
//    musiclibrary.artistsscraper
//    musiclibrary.overridetags
//    musiclibrary.showallitems
//    musiclibrary.updateonstartup
//    musiclibrary.backgroundupdate
//    musiclibrary.cleanup
//    musiclibrary.export
//    musiclibrary.import
//    musicplayer.autoplaynextitem
//    musicplayer.queuebydefault
//    musicplayer.seeksteps
//    musicplayer.seekdelay
//    musicplayer.replaygaintype
//    musicplayer.replaygainpreamp
//    musicplayer.replaygainnogainpreamp
//    musicplayer.replaygainavoidclipping
//    musicplayer.crossfade
//    musicplayer.crossfadealbumtracks
//    musicplayer.visualisation
//    musicfiles.usetags
//    musicfiles.trackformat
//    musicfiles.nowplayingtrackformat
//    musicfiles.librarytrackformat
//    musicfiles.findremotethumbs
//    audiocds.autoaction
//    audiocds.usecddb
//    audiocds.recordingpath
//    audiocds.trackpathformat
//    audiocds.encoder
//    audiocds.settings
//    audiocds.ejectonrip
//    mymusic.startwindow
//    mymusic.songthumbinvis
//    mymusic.defaultlibview
//    pictures.generatethumbs
//    pictures.showvideos
//    pictures.displayresolution
//    slideshow.staytime
//    slideshow.displayeffects
//    slideshow.shuffle
//    weather.currentlocation
//    weather.addon
//    weather.addonsettings
//    services.devicename
//    services.upnpserver
//    services.upnpannounce
//    services.upnplookforexternalsubtitles
//    services.upnpcontroller
//    services.upnprenderer
//    services.webserver
//    services.webserverport
//    services.webserverusername
//    services.webserverpassword
//    services.webskin
//    services.esenabled
//    services.esport
//    services.esportrange
//    services.esmaxclients
//    services.esallinterfaces
//    services.esinitialdelay
//    services.escontinuousdelay
//    services.zeroconf
//    services.airplay
//    services.airplayvolumecontrol
//    services.useairplaypassword
//    services.airplaypassword
//    services.airplayvideosupport
//    smb.winsserver
//    smb.workgroup
//    videoscreen.monitor
//    videoscreen.screen
//    videoscreen.resolution
//    videoscreen.screenmode
//    videoscreen.fakefullscreen
//    videoscreen.blankdisplays
//    videoscreen.delayrefreshchange
//    videoscreen.stereoscopicmode
//    videoscreen.preferedstereoscopicmode
//    videoscreen.vsync
//    videoscreen.guicalibration
//    videoscreen.testpattern
//    videoscreen.limitedrange
//    videoscreen.dither
//    videoscreen.ditherdepth
//    audiooutput.audiodevice
//    audiooutput.channels
//    audiooutput.config
//    audiooutput.samplerate
//    audiooutput.stereoupmix
//    audiooutput.maintainoriginalvolume
//    audiooutput.processquality
//    audiooutput.streamsilence
//    audiooutput.supportdtshdcpudecoding
//    audiooutput.dspaddonsenabled
//    audiooutput.dspsettings
//    audiooutput.dspresetdb
//    audiooutput.guisoundmode
//    audiooutput.passthrough
//    audiooutput.passthroughdevice
//    audiooutput.ac3passthrough
//    audiooutput.ac3transcode
//    audiooutput.eac3passthrough
//    audiooutput.dtspassthrough
//    audiooutput.truehdpassthrough
//    audiooutput.dtshdpassthrough
//    input.peripherals
//    input.enablemouse
//    input.enablejoystick
//    network.usehttpproxy
//    network.httpproxytype
//    network.httpproxyserver
//    network.httpproxyport
//    network.httpproxyusername
//    network.httpproxypassword
//    network.bandwidth
//    powermanagement.displaysoff
//    powermanagement.shutdowntime
//    powermanagement.shutdownstate
//    powermanagement.wakeonaccess
//    eventlog.enabled
//    eventlog.enablednotifications
//    eventlog.show
//    debug.showloginfo
//    debug.extralogging
//    debug.setextraloglevel
//    debug.screenshotpath
//    masterlock.lockcode
//    masterlock.startuplock
//    masterlock.maxretries
//    cache.harddisk
//    cachevideo.dvdrom
//    cachevideo.lan
//    cachevideo.internet
//    cacheaudio.dvdrom
//    cacheaudio.lan
//    cacheaudio.internet
//    cachedvd.dvdrom
//    cachedvd.lan
//    cacheunknown.internet
//    system.playlistspath
//    general.addonupdates
//    general.addonnotifications
//    general.addonforeignfilter
//    general.addonbrokenfilter

    public final static String PVRMANAGER_ENABLED = "pvrmanager.enabled";


    /**
     * Retrieves the value of a setting
     * Note, this returns a raw JsonNode. It is the responsibility of the caller to
     * retrieve and parse the value, taking into account the specific setting type
     * that was requested
     */
    public static final class GetSettingValue extends ApiMethod<JsonNode> {
        public final static String METHOD_NAME = "Settings.GetSettingValue";

        /**
         * Retrieves the value of a setting
         * Note, this returns a raw JsonNode. It is the responsibility of the caller to
         * retrieve and parse the value, taking into account the specific setting type
         * that was requested
         */
        public GetSettingValue(String setting) {
            super();
            addParameterToRequest("setting", setting);
        }

        @Override
        public String getMethodName() {
            return METHOD_NAME;
        }

        @Override
        public JsonNode resultFromJson(ObjectNode jsonObject) throws ApiException {
            return jsonObject.get(RESULT_NODE).get("value");
        }
    }

}
