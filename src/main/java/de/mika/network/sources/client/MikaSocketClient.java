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

package de.mika.network.sources.client;

import de.mika.network.api.InvalidPacketException;
import de.mika.network.api.NotConnectedException;
import de.mika.network.api.client.RejectLoginException;
import de.mika.network.api.client.SocketClient;
import de.mika.network.api.pipeline.ChannelHandler;
import de.mika.network.api.socket.DataPacket;
import de.mika.network.api.socket.ErrorPacket;
import de.mika.network.api.socket.LoginPacket;
import de.mika.network.api.socket.Sign;
import lombok.Getter;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author Groldi
 * @since 1.0.0-SNAPSHOT
 */
public class MikaSocketClient extends SocketClient
{

    private final Sign SIGN;
    private final InetSocketAddress SERVER_ADDRESS;
    private ChannelHandler CHANNEL_HANDLER;

    @Getter private int timeout;
    private long lastPing;
    private Thread listenerThread, disconnectThread;
    private Socket client;

    public MikaSocketClient(Sign sign, InetSocketAddress serverAddress)
    {
        this.SIGN = sign;
        this.SERVER_ADDRESS = serverAddress;

    }

    @Override public InetSocketAddress getAddress()
    {
        return new InetSocketAddress(client.getInetAddress(), client.getPort());
    }

    public ChannelHandler getChannelHandler()
    {
        return CHANNEL_HANDLER;
    }

    @Override public void run()
    {
    }

    @Override public void shutdown()
    {
        // ICH BIN VERSTECK HEHE
    }

    @Override public InetSocketAddress getServerAddress()
    {
        return SERVER_ADDRESS;
    }

    @Override public Sign getSign()
    {
        return SIGN;
    }

    @Override public void login(LoginPacket loginPacket) throws RejectLoginException
    {

    }

    private void startListener()
    {
        if (this.listenerThread != null && !this.listenerThread.isInterrupted())
        {
            return;
        }

        this.listenerThread = new Thread(() ->
        {
            while (isConnected())
            {
                try
                {
                    ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));
                    Object rawInput = objectInputStream.readObject();

                    if (rawInput instanceof DataPacket dataPackage)
                    {

                    }
                }
                catch (IOException | ClassNotFoundException ignored)
                {
                    sendPacket(new ErrorPacket(new InvalidPacketException()));
                }
            }
        });
    }

    @Override public void sendPacket(DataPacket dataPacket)
    {
        if (!isConnected())
        {
            throw new NotConnectedException();
        }


    }

    private DataPacket sendUnsecureMessage(Socket socket, DataPacket dataPacket) throws IOException, ClassNotFoundException
    {

        dataPacket.setSign(SIGN);
        if (socket != this.client)
        {
            socket.connect(getAddress(), timeout);
        }

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        objectOutputStream.writeObject(dataPacket);
        objectOutputStream.flush();

        DataPacket reply = null;
        if (dataPacket.isReply())
        {
            ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            Object rawInput = objectInputStream.readObject();
            if (rawInput instanceof DataPacket replyPacket)
            {
                reply = replyPacket;
            }
            objectInputStream.close();
        }
        objectOutputStream.close();

        if (socket != this.client)
        {
            socket.close();
        }
        return reply;
    }

    @Override public boolean isConnected()
    {
        return client != null && client.isConnected() && !client.isClosed();
    }

    @Override public Socket getSocket()
    {
        return client;
    }


    // ICH BIN VERSTECK HEHE 2
}
