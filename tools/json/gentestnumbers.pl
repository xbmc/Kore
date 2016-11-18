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
use Cpanel::JSON::XS qw(encode_json decode_json);

sub printRanges($\@) {
    my $key = shift;
    my $arg = shift;
    my $count = 0;

    my @list = @{$arg};
    my $current;
    for (my $i = 1; $i < @list; $i++) {
        $current = $list[$i]->{$key};
        my $prev = $list[$i-1]->{$key};
        if ( $current - $prev == 1 ) {
            $count++;
        } else {
            if ( $count == 0 ) {
                print $prev;
            } else {
                print $prev - $count . "-" . $prev;
            }
            print " ";
            $count = 0;
        }
    }

    if ( $count == 0 ) {
        print $current if defined $current;
    } else {
        print $current - $count . "-" . $current;
    }
}

sub decodeJson($) {
    my $filename = shift;
    local $/ = undef;
    open (FH, $filename) or die "Error opening file $filename\n";
    my $json_hash = decode_json(<FH>);
    close FH;
    return $json_hash;
}

sub printSong(\%) {
    my $song = shift;
    print "title: " . $song->{"title"} . "\n";

    print "artistid: ";
    for my $artistid ( @{$song->{"artistid"}} ) {
        print $artistid . " ";
    }
    print "\n";

    print "albumid: " . $song->{"albumid"} . "\n";
    print "songid: " . $song->{"songid"} . "\n";
}


sub printAlbum(\%) {
    my $album = shift;

    print "title: " . $album->{"title"} . "\n";
    print "albumid: " . $album->{"albumid"} . "\n";
    print "displayartist: " . $album->{"displayartist"} .  "\n";
    print "year: " . $album->{"year"} . "\n";
    print "genre: " . @{$album->{"genre"}} . "\n";
}


sub getArtists($) {
    my $json_hash = shift;
    return $json_hash->{"result"}->{"artists"};
}

sub getArtist($$) {
    my $json_hash = shift;
    my $artistid = shift;

    my $artists = getArtists($json_hash);
    for my $artist (@{$artists}) {
        if ( $artistid == $artist->{"artistid"} ) {
            return $artist;
        }
    }
    return undef;
}

sub getAlbums($) {
    my $json_hash = shift;
    return $json_hash->{"result"}->{"albums"};
}

sub getAlbum($$) {
    my $json_hash = shift;
    my $albumid = shift;

    my $albums = getAlbums($json_hash);
    for my $album (@{$albums}) {
        if ( $albumid == $album->{"albumid"}) {
            return $album;
        }
    }
}

sub getAlbumsForGenre($$) {
    my $json_hash = shift;
    my $genreid = shift;

    my @result;

    my $albums = getAlbums($json_hash);
    for my $album (@{$albums}) {
        for my $albumGenreId (@{$album->{"genreid"}}) {
            if ( $albumGenreId == $genreid ) {
                push @result, $album;
            }
        }
    }

    return @result;
}

sub getSongs(%) {
    my $json_hash = shift;
    return $json_hash->{"result"}->{"songs"};
}

sub getSong(\%$) {
    my $json_hash = shift;
    my $songid = shift;

    my $songs = getSongs($json_hash);
    for my $song (@{$songs}) {
        if ( $songid == $song->{"songid"}) {
            return $song;
        }
    }
}

sub printArtistTestNumbers($) {
    my $artistid = shift;

    my $json_hash = decodeJson( "AudioLibrary.GetArtists.json" );
    my $result = getArtists($json_hash);
    print "Amount of artists: ", scalar @{$result}, "\n\n";

    print "Artist ids: ";
    my @artists = sort {$a->{"artistid"} <=> $b->{"artistid"}} @{$result};
    printRanges("artistid", @artists);
    print "\n\n";

    print "Artist with artistId $artistid\n";
    my $artist = getArtist($json_hash, $artistid);
    print "artist: " . $artist->{"artist"} . "\n";
    print "artistid: " . $artist->{"artistid"} . "\n";
    print "\n\n";
}

sub printAlbumTestNumbers($$) {
    my $albumid = shift;
    my $genreid = shift;
    my $json_hash = decodeJson( "AudioLibrary.GetAlbums.json" );
    my $result = getAlbums($json_hash);
    print "Amount of albums: ", scalar @{$result}, "\n\n";

    print "Album ids: ";
    my @albums = sort {$a->{"albumid"} <=> $b->{"albumid"}} @{$result};
    printRanges("albumid", @albums);
    print "\n\n";

    print "Albums for genre id $genreid: ";
    my @result = getAlbumsForGenre( $json_hash, $genreid );
    @albums = sort {$a->{"albumid"} <=> $b->{"albumid"}} @result;
    printRanges("albumid", @albums);
    print "\n\n";

    print "Album with albumId $albumid\n";
    my $album = getAlbum($json_hash, $albumid);
    printAlbum(%$album);
    print "\n\n";
}

sub printSongTestNumbers($$) {
    my $artistid = shift;
    my $albumid = shift;

    my $json_hash = decodeJson( "AudioLibrary.GetSongs.json" );
    my $result = getSongs($json_hash);
    print "Amount of songs: ", scalar @{$result}, "\n\n";

    my @songsforartist;
    my @songsforalbum;

    print "Song ids: ";

    my @songids = sort {$a->{"songid"} <=> $b->{"songid"}} @{$result};
    printRanges("songid", @songids);

    for my $song (@songids) {
        for my $id (@{$song->{"artistid"}}) {
            if ( $id == $artistid ) {
                push @songsforartist, $song;
            }
        }
        if ( $song->{"albumid"} == $albumid ) {
            push @songsforalbum, $song;
        }
    }
    print "\n\n";

    print "Songs for artistid " . $artistid . ": total=" . scalar @songsforartist . ": ids=";
    printRanges("songid", @songsforartist);
    print "\n\n";

    print "Songs for albumid " . $albumid . ": total=" . scalar @songsforalbum . ": ids=";
    printRanges("songid", @songsforalbum);
    print "\n\n";
}

sub printSongCornerCases() {

    my $json_hash = decodeJson( "AudioLibrary.GetSongs.json" );

    print "Song with album and artist\n";
    my $song = getSong(%$json_hash, 1487);
    printSong(%$song);
    print "\n\n";

    print "Songs with album but without artist\n";
    $song = getSong(%$json_hash, 1219);
    printSong(%$song);
    print "\n\n";

    print "Song without album but with artist\n";
    $song = getSong(%$json_hash, 1128);
    printSong(%$song);

    print "\n\n";

    print "Song with multiple artists\n";
    $song = getSong(%$json_hash, 1804);
    printSong(%$song);
}

sub printAlbumCornerCases() {
    my $json_hash = decodeJson( "AudioLibrary.GetAlbums.json" );

    print "Album without an artist\n";
    my $album = getAlbum($json_hash, 82);
    printAlbum(%$album);
    print "\n\n";

    print "Album with multiple artists\n";
    $album = getAlbum($json_hash, 234);
    printAlbum(%$album);
    print "\n\n";
}

printArtistTestNumbers(13);

printAlbumTestNumbers(13, 13);
printAlbumCornerCases();

printSongTestNumbers(13, 13);

printSongCornerCases();
