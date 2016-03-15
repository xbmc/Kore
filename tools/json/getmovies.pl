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

sub getMovies() {
    my $jsonrequest = {
        "jsonrpc" => "2.0",
        "method" => "VideoLibrary.GetMovies",
        "params" => {
            "limits" => { "start" => 0, "end" => 300 },
            "properties" => [
				"title",
				"genre",
				"year",
				"rating",
				"director",
				"trailer",
				"tagline",
				"plot",
				"plotoutline",
				"originaltitle",
				"lastplayed",
				"playcount",
				"writer",
				"studio",
				"mpaa",
				"cast",
				"country",
				"imdbnumber",
				"runtime",
				"set",
				"showlink",
				"streamdetails",
				"top250",
				"votes",
				"fanart",
				"thumbnail",
				"file",
				"sorttitle",
				"resume",
				"setid",
				"dateadded",
				"tag",
				"art"
                    ],
            "sort" => { "order" => "ascending", "method" => "label", "ignorearticle" => Types::Serialiser::true }
        },
        "id" => "libMovies"
    };

    return sendJsonRequest("http://127.0.0.1:8080/jsonrpc", $jsonrequest);
}

writeJsonFile("Video.Details.Movie.json", getMovies);