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

sub getAddons() {
	my $jsonrequest = {
		"jsonrpc" => "2.0",
		"method" => "Addons.GetAddons",
			"params" => {
			"properties" => [
			"name",
	                "version",
	                "summary",
	                "description",
	                "path",
	                "author",
	                "thumbnail",
	                "disclaimer",
	                "fanart",
	                "dependencies",
	                "broken",
	                "extrainfo",
	                "rating",
	                "enabled",
	                "installed"
			],
		},
		"id" => "libAddons"
	};

	return sendJsonRequest("http://127.0.0.1:8080/jsonrpc", $jsonrequest);
}

writeJsonFile("Addons.GetAddons.json", getAddons);
