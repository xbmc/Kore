#!/usr/bin/perl
#
# Copyright 2016 Martijn Brekhof. All rights reserved.
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
use Data::Dumper;

my $url = "http://127.0.0.1:8080/jsonrpc";

sub getSongs($) {
    my $artist = shift;

    my $jsonrequest = {
        "jsonrpc" => "2.0",
        "method" => "AudioLibrary.GetSongs",
            "params" => {
                "properties" => [
                        "title",
                        "artist",
                        "albumartist",
                        "genre",
                        "year",
                        "rating",
                        "album",
                        "track",
                        "duration",
                        "comment",
                        "lyrics",
                        "musicbrainztrackid",
                        "musicbrainzartistid",
                        "musicbrainzalbumid",
                        "musicbrainzalbumartistid",
                        "playcount",
                        "fanart",
                        "thumbnail",
                        "file",
                        "albumid",
                        "lastplayed",
                        "disc",
                        "genreid",
                        "artistid",
                        "displayartist",
                        "albumartistid"
                        ],
                "sort" => { "order" => "ascending", "method" => "label", "ignorearticle" => Types::Serialiser::true }
            },
            "id" => "libSongs",
        };

    if ( defined $artist ) {
	$jsonrequest->{"params"}{"filter"} = {
                        "field" => "artist",
                        "operator" => "is",
                        "value" => [ $artist ]
                        };
    }

    return sendJsonRequest($url, $jsonrequest);
}

sub getArtists() {
    my $jsonrequest = {
        "jsonrpc" => "2.0",
        "method" => "AudioLibrary.GetArtists",
        "params" => {
            "limits" => { "start" => 0, "end" => 300 },
            "properties" => [
                    "instrument",
                    "style",
                    "mood",
                    "born",
                    "formed",
                    "description",
                    "genre",
                    "died",
                    "disbanded",
                    "yearsactive",
                    "musicbrainzartistid",
                    "fanart",
                    "thumbnail"
                    ],
            "sort" => { "order" => "ascending", "method" => "label", "ignorearticle" => Types::Serialiser::true }
        },
        "id" => "libArtists"
    };

    return sendJsonRequest($url, $jsonrequest);
}

sub getGenres() {
    my $jsonrequest = {
        "jsonrpc" => "2.0",
        "method" => "AudioLibrary.GetGenres",
        "params" => {
                "properties" => [
                    "title",
                    "thumbnail"
                    ],
            "sort" => { "order" => "ascending", "method" => "label", "ignorearticle" => Types::Serialiser::true }
        },
        "id" => "libGenres"
    };

    return sendJsonRequest($url, $jsonrequest);
}

sub getAlbums($) {
    my $artistid = shift;
    my $jsonrequest = {
        "jsonrpc" => "2.0",
        "method" => "AudioLibrary.GetAlbums",
        "params" => {
            "properties" => [
                    "title",
                    "description",
                    "artist",
                    "genre",
                    "theme",
                    "mood",
                    "style",
                    "type",
                    "albumlabel",
                    "rating",
                    "year",
                    "musicbrainzalbumid",
                    "musicbrainzalbumartistid",
                    "fanart",
                    "thumbnail",
                    "playcount",
                    "genreid",
                    "artistid",
                    "displayartist"
                    ],
            "sort" => { "order" => "ascending", "method" => "label", "ignorearticle" => Types::Serialiser::true }
        },
        "id" => "libAlbums"
    };

    if ( defined $artistid ) {
	    $jsonrequest->{"params"}->{"filter"}->{"artistid"} = $artistid;
    }

    return sendJsonRequest($url, $jsonrequest);
}

writeJsonFile("AudioLibrary.GetGenres.json", getGenres());

my $artists = getArtists();
writeJsonFile("AudioLibrary.GetArtists.json", $artists);

my $json_albums;
my %albums_seen; #Need to filter out duplicates of various artist albums
my $count = 0;
for my $artist (@{$artists->{"result"}->{"artists"}}) {
    my $albums = getAlbums($artist->{"artistid"});
    if ( ! defined $json_albums ) {
        $count = 1;
        $json_albums = $albums;
    } else {
        for my $album (@{$albums->{"result"}->{"albums"}}) {
            my $albumid = $album->{"albumid"};
            if ( ! exists $albums_seen{$albumid} ) {
                $count++;
                push $json_albums->{"result"}->{"albums"}, $album;
                $albums_seen{$albumid} = "";
            }
        }
    }
}

$json_albums->{"result"}{"limits"} = {"end" => $count, "start" => 0, "total" => $count};

writeJsonFile("AudioLibrary.GetAlbums.json", $json_albums);
writeJsonFile("AudioLibrary.GetSongs.json", getSongs(undef));
