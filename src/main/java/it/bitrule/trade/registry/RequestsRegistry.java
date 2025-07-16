package it.bitrule.trade.registry;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class RequestsRegistry {

    /**
     * This map stores the recipients of requests,
     * where the key is the recipient's UUID and the value is a set of UUIDs of senders.
     * This allows tracking multiple requests sent to the same recipient.
     * Each recipient can have multiple senders, and each sender can send requests to multiple recipients.
     * This is useful for managing trade requests in a multiplayer environment,
     * where players can send trade requests to each other.
     */
    private final @NonNull Map<UUID, Set<UUID>> receptors = new ConcurrentHashMap<>();
    /**
     * This map stores the senders of requests,
     * where the key is the recipient's UUID and the value is a set of UUIDs of senders.
     * This allows tracking multiple requests sent to the same recipient.
     */
    private final @NonNull Map<UUID, Set<UUID>> sent = new ConcurrentHashMap<>();

    /**
     * Checks if a request exists in the registry for the given sender and recipient.
     * @param sender the unique ID of the player sending the request
     * @param recipient the unique ID of the player receiving the request
     * @return true if the request exists, false otherwise
     */
    public boolean has(@NonNull UUID sender, @NonNull UUID recipient) {
        return Optional.ofNullable(this.receptors.get(recipient))
                .map(senders -> senders.contains(sender))
                .orElse(false);
    }

    /**
     * Registers a new request in the registry.
     * @param sender the unique ID of the player sending the request
     * @param recipient the unique ID of the player receiving the request
     */
    public void register(@NonNull UUID sender, @NonNull UUID recipient) {
        this.receptors.computeIfAbsent(recipient, k -> ConcurrentHashMap.newKeySet()).add(sender);
        this.sent.computeIfAbsent(sender, k -> ConcurrentHashMap.newKeySet()).add(recipient);
    }

    /**
     * Unregisters a specific request from the registry.
     * @param sender the unique ID of the player who sent the request
     * @param recipient the unique ID of the player who received the request
     */
    public void unregister(@NonNull UUID sender, @NonNull UUID recipient) {
        Set<UUID> senders = this.receptors.get(recipient);
        if (senders == null) {
            throw new IllegalArgumentException("No requests found for recipient: " + recipient);
        }

        Set<UUID> recipients = this.sent.get(sender);
        if (recipients == null) {
            throw new IllegalArgumentException("No requests found for sender: " + sender);
        }

        recipients.remove(recipient);
        senders.remove(sender);

        // If there are no more senders for this recipient, remove the recipient entry
        if (senders.isEmpty()) this.receptors.remove(recipient);

        // If there are no more recipients for this sender, remove the sender entry
        if (recipients.isEmpty()) this.sent.remove(sender);
    }

    /**
     * Unregisters all requests sent by a specific player.
     * @param playerId the unique ID of the player whose requests are to be unregistered
     * @return a list of UUIDs of recipients who had received requests from the player, or null if no requests were found
     */
    public @Nullable List<UUID> clearSent(@NonNull UUID playerId) {
        Set<UUID> recipients = this.sent.remove(playerId);
        if (recipients == null) return null;

        for (UUID recipient : recipients) {
            Set<UUID> senders = this.receptors.get(recipient);
            if (senders == null) continue;

            senders.remove(playerId);

            if (!senders.isEmpty()) continue;
            // If there are no more senders for this recipient, remove the recipient entry
            this.receptors.remove(recipient);
        }

        return new ArrayList<>(recipients);
    }

    /**
     * Unregisters all requests received by a specific player.
     * @param recipientId the unique ID of the player whose requests are to be cleared
     * @return a list of UUIDs of senders who had sent requests to the recipient, or null if no requests were found
     */
    public @Nullable List<UUID> clearReceptors(@NonNull UUID recipientId) {
        Set<UUID> senders = this.receptors.remove(recipientId);
        if (senders == null) return null;

        for (UUID sender : senders) {
            Set<UUID> recipients = this.sent.get(sender);
            if (recipients == null) continue;

            recipients.remove(recipientId);

            if (!recipients.isEmpty()) continue;
            // If there are no more recipients for this sender, remove the sender entry
            this.sent.remove(sender);
        }

        return new ArrayList<>(senders);
    }
}