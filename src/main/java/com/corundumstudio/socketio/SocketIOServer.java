/**
 * Copyright 2012 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio;

import java.net.InetSocketAddress;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.listener.ClientListeners;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.listener.ListenersHub;

public class SocketIOServer implements ClientListeners {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ServerBootstrap bootstrap;

    private ListenersHub listenersHub = new ListenersHub();
    private SocketIOPipelineFactory pipelineFactory = new SocketIOPipelineFactory();

    private Channel mainChannel;
    private Configuration config;
    private boolean started;

    public SocketIOServer(Configuration configuration) {
        this.config = new Configuration(configuration);
        this.config.getObjectMapper().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    }

    public void setPipelineFactory(SocketIOPipelineFactory pipelineFactory) {
        this.pipelineFactory = pipelineFactory;
    }

    public ClientOperations getBroadcastOperations() {
        if (!started) {
            throw new IllegalStateException("Server have not started!");
        }
        return new BroadcastOperations(pipelineFactory.getAllClients());
    }

    public void start() {
        ChannelFactory factory = new NioServerSocketChannelFactory(config.getBossExecutor(), config.getWorkerExecutor());
        bootstrap = new ServerBootstrap(factory);

        pipelineFactory.start(config, listenersHub);
        bootstrap.setPipelineFactory(pipelineFactory);
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        mainChannel = bootstrap.bind(new InetSocketAddress(config.getHostname(), config.getPort()));

        started = true;
        log.info("SocketIO server started at port: {}", config.getPort());
    }

    public void stop() {
        pipelineFactory.stop();
        mainChannel.close();
        bootstrap.releaseExternalResources();
        started = false;
    }

    @Override
    public void addEventListener(String eventName, DataListener<Object> listener) {
        listenersHub.addEventListener(eventName, listener);
    }

    @Override
    public void addJsonObjectListener(DataListener<Object> listener) {
        listenersHub.addJsonObjectListener(listener);
    }

    @Override
    public void addDisconnectListener(DisconnectListener listener) {
        listenersHub.addDisconnectListener(listener);
    }

    @Override
    public void addConnectListener(ConnectListener listener) {
        listenersHub.addConnectListener(listener);
    }

    @Override
    public void addMessageListener(DataListener<String> listener) {
        listenersHub.addMessageListener(listener);
    }

}
