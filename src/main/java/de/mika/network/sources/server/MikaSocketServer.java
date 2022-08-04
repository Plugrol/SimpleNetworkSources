/*
 * Copyright 2022 Mika & Groldi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.mika.network.sources.server;

import de.mika.network.api.pipeline.ChannelAction;
import de.mika.network.api.pipeline.ChannelHandler;
import de.mika.network.api.server.SocketServer;
import de.mika.network.api.socket.DataPacket;
import de.mika.network.api.socket.InternalProtocols;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Groldi
 * @since 1.0.0-SNAPSHOT
 */
public class MikaSocketServer extends SocketServer
{

    private final ServerSocket SERVER_SOCKET;

    @Getter private final Set<ServerSocketClient> clients;

    private Thread listenThread;

    @Getter private int interval;

    @Getter private boolean connectionAuthorization, packetAuthorization;

    public MikaSocketServer(final int port) throws IOException
    {
        this.clients = new HashSet<>();
        SERVER_SOCKET = new ServerSocket(port);
        startListing();
    }

    @Override public InetSocketAddress getAddress()
    {
        return new InetSocketAddress(SERVER_SOCKET.getInetAddress(), SERVER_SOCKET.getLocalPort());
    }

    @Override public ChannelHandler getChannelHandler()
    {
        return null;
    }

    @Override public void run()
    {
        try
        {
            SERVER_SOCKET.accept();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override public void shutdown()
    {
        try
        {
            SERVER_SOCKET.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override protected void setInterval(int interval)
    {
        this.interval = interval;
    }

    @Override protected void setIgnoreConnections(boolean authorization)
    {
        connectionAuthorization = authorization;
    }

    @Override public void setIgnorePackets(boolean authorization)
    {
        packetAuthorization = authorization;
    }

    @Override public void sendPacket(String clientId, DataPacket dataPacket)
    {
        if (!isClient()) return;
        getClientById(clientId).sendPacket(dataPacket);
    }

    @Override public DataPacket sendAnsweredPacket(String clientId, DataPacket dataPacket)
    {
        if (!isClient()) return null;
        return getClientById(clientId).sendAnsweredPacket(dataPacket);
    }

    @Override public void broadcastPacket(String group, DataPacket dataPacket)
    {
        if (!isClient()) return;
        getClientByGroup(group).sendPacket(dataPacket);
    }

    @Override public void broadcastPacket(DataPacket dataPacket)
    {
        if (!isClient()) return;
        for (ServerSocketClient client : this.clients)
        {
            this.sendPacket(client.id(), dataPacket);
        }
    }

    @Override public ServerSocket getSocket()
    {
        return SERVER_SOCKET;
    }

    private ServerSocketClient getClientById(String id)
    {
        for (ServerSocketClient client : this.clients)
        {
            if (client.id().equals(id))
                return client;
        }
        return null;
    }

    private ServerSocketClient getClientByGroup(String group)
    {
        for (ServerSocketClient client : this.clients)
        {
            if (client.group().equals(group))
                return client;
        }
        return null;
    }

    /**
     *
     */
    private void startListing()
    {
        if (listenThread != null && listenThread.isAlive()) return;
        listenThread = new Thread(() -> {
            while (!listenThread.isInterrupted() && SERVER_SOCKET != null)
            {
                try
                {
                    if (this.isConnectionAuthorization())
                    {
                        readyUpSocket(SERVER_SOCKET.accept());
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                catch (ClassNotFoundException e)
                {
                    throw new RuntimeException(e);
                }
            }
        });
        listenThread.start();
    }


    private void readyUpSocket(Socket socket) throws IOException, ClassNotFoundException
    {
        if (this.isPacketAuthorization()) return;
        getChannelHandler().execution(InternalProtocols.LOGIN.toString(), new ChannelAction()
        {
            @Override public Serializable[] run(DataPacket input, Socket tempSocket)
            {
                ServerSocketClient server = new ServerSocketClient(input.getSign().sender(), input.getSign().group(), tempSocket);
                clients.add(server);
                handleSocket(socket, server);
                return new Serializable[0]; //Hallo -- gesehen von mika :O
            }
        });
    }

    @SneakyThrows private void handleSocket(Socket socket, ServerSocketClient serverSocketClient)
    {
        ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
        Object rawInput = objectInputStream.readObject();
        if (rawInput instanceof DataPacket dataPacket)
        {

            if (serverSocketClient.id() == null || serverSocketClient.group() == null)
            {
                socket.close();
                handlePacket(dataPacket, socket);
            }
            else
            {
                socket.close();
            }
        }

    }

    private void handlePacket(DataPacket dataPacket, Socket socket)
    {
        if (!dataPacket.isReply()) return;

        getChannelHandler().getActions().forEach((key, channelAction) -> {
            if (!dataPacket.getSign().sender().equals("*") || !dataPacket.getSign().sender().equals(key))
            {
                return;
            }
            channelAction.run(dataPacket, socket);
        });

    }

}