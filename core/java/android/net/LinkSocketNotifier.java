/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2010-2011, Code Aurora Forum. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net;

/**
 * Interface used to get feedback about a {@link android.net.LinkSocket} or
 * {@link android.net.LinkDatagramSocket}.  Instance is optionally
 * passed when a Link[Datagram]Socket is constructed.  Multiple Link[Datagram]Sockets may
 * use the same notifier.
 * @hide
 */
public interface LinkSocketNotifier {
    /**
     * This callback function will be called if the system determines
     * an application will get a better link if it creates a new
     * LinkSocket right now.
     *
     * If a new LinkSocket is created, it is important to close the
     * old LinkSocket once it is no longer in use.
     *
     * @param socket the original LinkSocket whose connection could be improved
     */
    public void onBetterLinkAvailable(LinkSocket socket);

    /**
     * This callback function will be called if the system determines
     * an application will get a better link if it creates a new
     * LinkDatagramSocket right now.
     *
     * Note: This callback is not applicable for QoS needs
     *
     * If a new LinkDatagramSocket is created, it is important to
     * close the old LinkDatagramSocket once it is no longer in use.
     *
     * @param socket the original LinkDatagramSocket whose connection
     * could be improved
     */
    public void onBetterLinkAvailable(LinkDatagramSocket socket);

    /**
     * This callback function will be called when a LinkSocket no longer has
     * an active link.
     *
     * @param socket the LinkSocket that lost its link
     */
    public void onLinkLost(LinkSocket socket);

    /**
     * This callback function will be called when a LinkDatagramSocket no
     * longer has an active link.
     *
     * @param socket the LinkDatagramSocket that lost its link
     */
    public void onLinkLost(LinkDatagramSocket socket);

    /**
     * This callback function will be called when any of the notification-marked
     * capabilities of the LinkSocket (e.g. upstream bandwidth) have changed.
     *
     * @param socket the LinkSocket for which capabilities have changed
     * @param changedCapabilities the set of capabilities that the application
     *          is interested in and have changed (with new values)
     */
    public void onCapabilitiesChanged(LinkSocket socket, LinkCapabilities changedCapabilities);

    /**
     * This callback function will be called when any of the notification-marked
     * QoS capabilities of the LinkDatagramSocket (e.g. upstream bandwidth) have
     * changed.
     *
     * @param socket the LinkDatagramSocket for which capabilities have changed
     * @param changedCapabilities the set of capabilities that the application
     *          is interested in and have changed (with new values)
     */
    public void onCapabilitiesChanged(LinkDatagramSocket socket, LinkCapabilities changedCapabilities);
}
