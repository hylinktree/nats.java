// Copyright 2015-2018 The NATS Authors
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

package io.nats.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import io.nats.client.Options;

public class SocketDataPort implements DataPort {

    private NatsConnection connection;
    private String host;
    private int port;
    private Socket socket;
    private SSLSocket sslSocket;

    private InputStream in;
    private OutputStream out;
    private Certificate[] certificatesFromConnection;

    public void connect(String serverURI, NatsConnection conn) throws IOException {

        try {
            this.connection = conn;

            Options options = this.connection.getOptions();
            long timeout = options.getConnectionTimeout().toMillis();
            URI uri = options.createURIForServer(serverURI);
            this.host = uri.getHost();
            this.port = uri.getPort();

            this.socket = new Socket();

            socket.connect(new InetSocketAddress(host, port), (int) timeout);

            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Upgrade the port to SSL. If it is already secured, this is a no-op.
     * If the data port type doesn't support SSL it should throw an exception.
     */
    public void upgradeToSecure() throws IOException {
        boolean recheckReconnect = this.connection.getOptions().recheckClusterCert();
        boolean isReconnectServer = this.connection.isReconnectServer(this.host, this.port);
        SSLContext context = null;
        
        if (isReconnectServer && !recheckReconnect) {
            HashSet<Certificate> certs = new HashSet<>();
            certs.addAll(Arrays.asList(this.certificatesFromConnection));
            context = SSLUtils.createReconnectContext(certs);
        } else {
            context = this.connection.getOptions().getSslContext();
        }
        
        SSLSocketFactory factory = context.getSocketFactory();
        Duration timeout = this.connection.getOptions().getConnectionTimeout();

        this.sslSocket = (SSLSocket) factory.createSocket(socket, null, true);
        this.sslSocket.setUseClientMode(true);

        final CompletableFuture<Void> waitForHandshake = new CompletableFuture<>();
        
        this.sslSocket.addHandshakeCompletedListener((evt) -> {
            waitForHandshake.complete(null);
        });

        this.sslSocket.startHandshake();

        try {
            waitForHandshake.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (Exception ex) {
            this.connection.handleCommunicationIssue(ex);
        }

        // Reset the allowed certs on a full reconnect
        if (!isReconnectServer || recheckReconnect) {
            this.certificatesFromConnection = this.sslSocket.getSession().getPeerCertificates();
        }

        //in = Channels.newChannel(new BufferedInputStream(sslSocket.getInputStream()));
        in = sslSocket.getInputStream();
        out = sslSocket.getOutputStream();
    }

    public int read(byte[] dst, int off, int len) throws IOException {
        return in.read(dst, off, len);
    }

    public void write(byte[] src, int toWrite) throws IOException {
        out.write(src, 0, toWrite);
    }

    public void close() throws IOException {
        if (sslSocket != null) {
            sslSocket.close(); // autocloses the underlying socket
        } else {
            socket.close();
        }
    }
}