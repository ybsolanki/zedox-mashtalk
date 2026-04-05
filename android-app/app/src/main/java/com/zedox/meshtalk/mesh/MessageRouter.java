package com.zedox.meshtalk.mesh;

import android.util.Log;
import com.zedox.meshtalk.models.Device;
import com.zedox.meshtalk.models.Message;
import com.zedox.meshtalk.utils.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Message Router for MeshTalk Mesh Network
 * Implements multi-hop routing – messages hop through intermediate devices
 * until they reach the destination or the hop limit is hit.
 * Team ZEDOX - Imagine Cup 2025
 */
public class MessageRouter {

    private static final String TAG = "MessageRouter";

    /** routing table: destinationId → next-hop Device (thread-safe) */
    private final Map<String, Device> routingTable = new ConcurrentHashMap<>();

    /** known devices in the mesh (deviceId → Device) (thread-safe) */
    private final Map<String, Device> knownDevices = new ConcurrentHashMap<>();

    private MessageRoutingListener routingListener;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Route a message to its destination.
     * Increments the hop counter and either delivers locally or forwards.
     *
     * @param message Message to route
     * @param localDeviceId This device's ID
     */
    public void routeMessage(Message message, String localDeviceId) {
        int currentHop = message.getHopCount();

        if (currentHop >= Constants.MAX_HOP_COUNT) {
            Log.w(TAG, "Message " + message.getMessageId() + " dropped – hop limit reached");
            if (routingListener != null) {
                routingListener.onMessageDropped(message, "Hop limit reached");
            }
            return;
        }

        message.setHopCount(currentHop + 1);

        if (localDeviceId.equals(message.getReceiverId())) {
            Log.d(TAG, "Message " + message.getMessageId() + " delivered locally");
            if (routingListener != null) {
                routingListener.onMessageDelivered(message);
            }
            return;
        }

        Device nextHop = findNextHop(message.getReceiverId());
        if (nextHop != null) {
            Log.d(TAG, "Forwarding message " + message.getMessageId()
                    + " to " + nextHop.getDeviceName()
                    + " (hop " + message.getHopCount() + ")");
            if (routingListener != null) {
                routingListener.onForwardMessage(message, nextHop);
            }
        } else {
            // No known path – broadcast to all healthy neighbours
            List<Device> neighbours = getHealthyNeighbours();
            if (!neighbours.isEmpty()) {
                Log.d(TAG, "No direct route – broadcasting to " + neighbours.size() + " neighbour(s)");
                for (Device neighbour : neighbours) {
                    if (routingListener != null) {
                        routingListener.onForwardMessage(message, neighbour);
                    }
                }
            } else {
                Log.w(TAG, "Message " + message.getMessageId() + " dropped – no reachable neighbours");
                if (routingListener != null) {
                    routingListener.onMessageDropped(message, "No reachable neighbours");
                }
            }
        }
    }

    /**
     * Add or update a device in the routing/neighbour tables.
     */
    public void addDevice(Device device) {
        knownDevices.put(device.getDeviceId(), device);
        // Direct neighbour → it IS the next hop for itself
        routingTable.put(device.getDeviceId(), device);
        Log.d(TAG, "Device added to routing table: " + device.getDeviceName());
    }

    /**
     * Remove a device (e.g. it disconnected).
     */
    public void removeDevice(String deviceId) {
        knownDevices.remove(deviceId);
        routingTable.remove(deviceId);
        Log.d(TAG, "Device removed from routing table: " + deviceId);
    }

    /**
     * Update routing table from a neighbour's advertisement
     * (used for multi-hop route discovery).
     *
     * @param destinationId  Final destination device ID
     * @param viaDevice      Next-hop device that can reach it
     */
    public void updateRoute(String destinationId, Device viaDevice) {
        routingTable.put(destinationId, viaDevice);
        Log.d(TAG, "Route updated: " + destinationId + " via " + viaDevice.getDeviceName());
    }

    /** Return a snapshot of all known devices. */
    public List<Device> getKnownDevices() {
        return new ArrayList<>(knownDevices.values());
    }

    public void setRoutingListener(MessageRoutingListener listener) {
        this.routingListener = listener;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Device findNextHop(String destinationId) {
        Device nextHop = routingTable.get(destinationId);
        if (nextHop != null && nextHop.isHealthy()) {
            return nextHop;
        }
        return null;
    }

    private List<Device> getHealthyNeighbours() {
        List<Device> healthy = new ArrayList<>();
        for (Device device : knownDevices.values()) {
            if (device.isHealthy()) {
                healthy.add(device);
            }
        }
        return healthy;
    }

    // -------------------------------------------------------------------------
    // Listener interface
    // -------------------------------------------------------------------------

    public interface MessageRoutingListener {
        /** Message arrived at its final destination on this device. */
        void onMessageDelivered(Message message);

        /** Message should be forwarded to the specified next-hop device. */
        void onForwardMessage(Message message, Device nextHop);

        /** Message was dropped (hop limit or no route). */
        void onMessageDropped(Message message, String reason);
    }
}
