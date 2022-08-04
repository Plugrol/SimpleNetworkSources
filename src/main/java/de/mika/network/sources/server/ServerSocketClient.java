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

import de.mika.network.api.socket.DataPacket;

import java.net.Socket;

/**
 * @author Groldi
 * @since 1.0.0-SNAPSHOT
 */
public record ServerSocketClient(String id, String group, Socket socket)
{

    public void disconnect(String reason)
    {

    }

    public void sendPacket(DataPacket dataPacket)
    {
    }

    public DataPacket sendAnsweredPacket(DataPacket dataPacket)
    {
        return null;
    }

}
