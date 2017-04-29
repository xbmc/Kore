#!/usr/bin/perl
#
# Copyright 2017 Martijn Brekhof. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

use strict;
use warnings;

use Types::Serialiser;
use JsonTools qw(sendJsonRequest writeJsonFile);

sub getTVShows() {
    my $jsonrequest = {
        "jsonrpc" => "2.0",
        "method" => "VideoLibrary.GetTVShows",
        "params" => {
            "limits" => { "start" => 0, "end" => 10 },
            "properties" => [
                "title",
                "genre",
                "year",
                "rating",
                "plot",
                "studio",
                "mpaa",
                "cast",
                "playcount",
                "episode",
                "imdbnumber",
                "premiered",
                "votes",
                "lastplayed",
                "fanart",
                "thumbnail",
                "file",
                "originaltitle",
                "sorttitle",
                "episodeguide",
                "season",
                "watchedepisodes",
                "dateadded",
                "tag",
                "art",
                "userrating",
                "ratings",
                "runtime",
                "uniqueid"
            ],
            "sort" => { "order" => "ascending", "method" => "label", "ignorearticle" => Types::Serialiser::true }
        },
        "id" => "libTVShows"
    };

    return sendJsonRequest("http://127.0.0.1:8080/jsonrpc", $jsonrequest);
}


sub getSeasons($) {
    my $tvshowid = shift;

    my $jsonrequest = {
        "jsonrpc" => "2.0",
        "method" => "VideoLibrary.GetSeasons",
        "params" => {
            "tvshowid" => $tvshowid,
            "properties" => [
                "season",
                "showtitle",
                "playcount",
                "episode",
                "fanart",
                "thumbnail",
                "tvshowid",
                "watchedepisodes",
                "art",
                "userrating"
            ],
            "sort" => { "order" => "ascending", "method" => "label", "ignorearticle" => Types::Serialiser::true }
        },
        "id" => "libTVShowSeasons"
    };

    return sendJsonRequest("http://127.0.0.1:8080/jsonrpc", $jsonrequest);
}

sub getEpisodes($) {
    my $tvshowid = shift;

    my $jsonrequest = {
        "jsonrpc" => "2.0",
        "method" => "VideoLibrary.GetEpisodes",
        "params" => {
            "tvshowid" => $tvshowid,
            "properties" => [
                "title",
                "plot",
                "votes",
                "rating",
                "writer",
                "firstaired",
                "playcount",
                "runtime",
                "director",
                "productioncode",
                "season",
                "episode",
                "originaltitle",
                "showtitle",
                "cast",
                "streamdetails",
                "lastplayed",
                "fanart",
                "thumbnail",
                "file",
                "resume",
                "tvshowid",
                "dateadded",
                "uniqueid",
                "art",
                "specialsortseason",
                "specialsortepisode",
                "userrating",
                "seasonid",
                "ratings"
            ],
            "sort" => { "order" => "ascending", "method" => "label", "ignorearticle" => Types::Serialiser::true }
        },
        "id" => "libTVShowEpisodes"
    };

    return sendJsonRequest("http://127.0.0.1:8080/jsonrpc", $jsonrequest);
}

my $tvshows_list = getTVShows();

my $json_seasons;
my $json_episodes;

for my $tvshow (@{$tvshows_list->{"result"}->{"tvshows"}}) {
    my $seasons_list = getSeasons($tvshow->{"tvshowid"});

    if (! defined $json_seasons) {
        $json_seasons = $seasons_list;
    } else {
        for my $season ( @{$seasons_list->{"result"}->{"seasons"}} ) {
            push $json_seasons->{"result"}->{"seasons"}, $season;
        }
    }

    my $episodes_list = getEpisodes($tvshow->{"tvshowid"});

    if (! defined $json_episodes) {
        $json_episodes = $episodes_list;
    } else {
        for my $episode ( @{$episodes_list->{"result"}->{"episodes"}} ) {
            push $json_episodes->{"result"}->{"episodes"}, $episode;
        }
    }
}

writeJsonFile("VideoLibrary.GetTVShows.json", $tvshows_list);

my $count = length($json_seasons->{"result"}->{"seasons"});
$json_seasons->{"result"}{"limits"} = {"end" => $count, "start" => 0, "total" => $count};
writeJsonFile("VideoLibrary.GetSeasons.json", $json_seasons);

$count = length($json_episodes->{"result"}->{"episodes"});
$json_seasons->{"result"}{"limits"} = {"end" => $count, "start" => 0, "total" => $count};
writeJsonFile("VideoLibrary.GetEpisodes.json", $json_episodes);
