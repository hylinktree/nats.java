// Copyright 2020 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.examples;

import io.nats.client.*;
import io.nats.client.impl.NatsMessage;

import java.nio.charset.StandardCharsets;

public class NatsJsPub {

    static final String usageString = "\nUsage: java NatsJsPub [-s server] [-stream name] [-h headerKey:headerValue]* <subject> <message>\n"
            + "\nUse tls:// or opentls:// to require tls, via the Default SSLContext\n"
            + "\nSet the environment variable NATS_NKEY to use challenge response authentication by setting a file containing your private key.\n"
            + "\nSet the environment variable NATS_CREDS to use JWT/NKey authentication by setting a file containing your user creds.\n"
            + "\nUse the URL for user/pass/token authentication.\n";

    public static void main(String[] args) {
        // circumvent the need for command line arguments by uncommenting / customizing the next line
        // args = "-s hello-stream hello-subject hello world".split(" ");
        args = "-stream hello-stream hello-subject hello world".split(" ");

        ExampleArgs exArgs = ExampleUtils.readPublishArgs(args, usageString).defaultStreamName("test-stream");

        try (Connection nc = Nats.connect(ExampleUtils.createExampleOptions(exArgs.server, false))) {

            String hdrNote = exArgs.hasHeaders() ? " with " + exArgs.headers.size() + " header(s)," : "";
            System.out.printf("\nPublishing '%s' on %s,%s server is %s\n\n", exArgs.message, exArgs.subject, hdrNote, exArgs.server);

            // Create a JetStream context.  This hangs off the original connection
            // allowing us to produce data to streams and consume data from
            // JetStream consumers.
            JetStream js = nc.jetStream();

            // See NatsJsManagement for examples on how to create the stream
            NatsJsManagement.createTestStream(nc, exArgs.stream, exArgs.subject);

            // create a typical NATS message
            Message msg = NatsMessage.builder()
                    .subject(exArgs.subject)
                    .headers(exArgs.headers)
                    .data(exArgs.message, StandardCharsets.UTF_8)
                    .build();

            // We'll use the defaults for this simple example, but there are options
            // to constrain publishing to certain streams, expect sequence numbers and
            // more. e.g.:
            //
            // PublishOptions pops = PublishOptions.builder()
            //    .stream("test-stream")
            //    .expectedLastMsgId("transaction-42")
            //    .build();
            // js.publish(msg, pops);

            // Publish a message and print the results of the publish acknowledgement.
            // An exception will be thrown if there is a failure.
            PublishAck pa = js.publish(msg);
            System.out.printf("Published message on subject %s, stream %s, seqno %d.\n",
                   exArgs.subject, pa.getStream(), pa.getSeqno());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
