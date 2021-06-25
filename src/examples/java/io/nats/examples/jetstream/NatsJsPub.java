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

package io.nats.examples.jetstream;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.NatsMessage;
import io.nats.examples.ExampleArgs;
import io.nats.examples.ExampleUtils;

import java.nio.charset.StandardCharsets;

/**
 * This example will demonstrate JetStream publishing.
 */
public class NatsJsPub {
    static final String usageString = "\nUsage: java -cp <classpath> NatsJsPub [-s server] [-strm stream] [-sub subject] [-mcnt msgCount] [-m messageWords+] [-r headerKey:headerValue]*"
            + "\n\nDefault Values:" + "\n   [-strm stream]    example-stream" + "\n   [-sub subject]    example-subject"
            + "\n   [-mcnt msgCount]  10" + "\n   [-m messageWords+] hello" + "\n   [-mn msgLength] 32"
            + "\n\nRun Notes:" + "\n   - msg_count < 1 is the same as 1" + "\n   - headers are optional"
            + "\n\nUse tls:// or opentls:// to require tls, via the Default SSLContext\n"
            + "\nSet the environment variable NATS_NKEY to use challenge response authentication by setting a file containing your private key.\n"
            + "\nSet the environment variable NATS_CREDS to use JWT/NKey authentication by setting a file containing your user creds.\n"
            + "\nUse the URL for user/pass/token authentication.\n";

    public static void main(String[] args) {
        ExampleArgs exArgs = ExampleArgs.builder().defaultStream("example-stream").defaultSubject("example-subject")
                .defaultMessage("hello").defaultMsgCount(10).build(args, usageString);

        String hdrNote = exArgs.hasHeaders() ? ", with " + exArgs.headers.size() + " header(s)" : "";
        System.out.printf("\nPublishing to %s%s. Server is %s\n\n", exArgs.subject, hdrNote, exArgs.server);

        try (Connection nc = Nats.connect(ExampleUtils.createExampleOptions(exArgs.server))) {

            // Create a JetStream context. This hangs off the original connection
            // allowing us to produce data to streams and consume data from
            // JetStream consumers.
            JetStream js = nc.jetStream();

            // See NatsJsManagement for examples on how to create the stream
            NatsJsUtils.createOrUpdateStream(nc, exArgs.stream, exArgs.subject);

            int stop = exArgs.msgCount < 2 ? 2 : exArgs.msgCount + 1;
            Message msg = NatsMessage.builder().subject(exArgs.subject).headers(exArgs.headers)
                    // .data(data, StandardCharsets.UTF_8)
                    .data(new byte[exArgs.msgLength]).build();
            int mod = 10000;
            for (int x = 1; x < stop; x++) {
                // make unique message data if you want more than 1 message
                // String data = exArgs.msgCount < 2 ? exArgs.message : exArgs.message + "-" +
                // x;

                // create a typical NATS message
                // Message msg = NatsMessage.builder()
                // .subject(exArgs.subject)
                // .headers(exArgs.headers)
                // // .data(data, StandardCharsets.UTF_8)
                // .data(new byte[exArgs.msgLength])
                // .build();

                // Publish a message and print the results of the publish acknowledgement.
                // We'll use the defaults for this simple example, but there are options
                // to constrain publishing to certain streams, expect sequence numbers and
                // more. See the NatsJsPubWithOptionsUseCases.java example for details.
                // An exception will be thrown if there is a failure.
                PublishAck pa = js.publish(msg);
                // System.out.printf("Published message %s on subject %s, stream %s, seqno
                // %d.\n", data, exArgs.subject,
                if ((x % mod) == 1)
                    System.out.printf("Published message.%d , seqno %d.\n", x, pa.getSeqno());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
