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

package JsonTools;

use strict;
use warnings;
use Exporter qw(import);
use Cpanel::JSON::XS qw(encode_json decode_json);
use LWP::UserAgent;

our @EXPORT_OK = qw(sendJsonRequest writeJsonFile);

sub sendJsonRequest($$) {
    my $url = shift;
    my $json = shift;

    my $jsonrequest = encode_json($json);

    my $req = HTTP::Request->new( 'POST', $url );
    $req->header( 'Content-Type' => 'application/json-rpc' );
    $req->content( $jsonrequest );
    my $ua = LWP::UserAgent->new;
    my $response = $ua->request($req);

    if (! $response->is_success) {
        die $response->status_line;
     }

    return decode_json($response->decoded_content);
}

sub writeJsonFile($$) {
	my $filename = shift;
	my $json = shift;
	open(FH, ">", $filename);
	print FH Cpanel::JSON::XS->new->pretty(1)->encode($json);
	close(FH);
}