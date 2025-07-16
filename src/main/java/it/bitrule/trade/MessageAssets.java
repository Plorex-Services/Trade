package it.bitrule.trade;

import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum MessageAssets {

    TRADE_COMMAND_USAGE("trade.command-usage"),

    PLAYER_NOT_ONLINE("player.not_online", "target"),

    TRADE_CANNOT_TRADE_YOURSELF("trade.cannot_trade_yourself"),
    TRADE_SENDER_ALREADY_TRADING("trade.sender_already_trading", "target"),
    TRADE_RECEPTOR_ALREADY_TRADING("trade.receptor_already_trading", "target"),
    TRADE_SENDER_ALREADY_SENT_REQUEST("trade.sender_already_sent_request", "target"),
    TRADE_RECEPTOR_ALREADY_SENT_REQUEST("trade.receptor_already_sent_request", "target"),
    TRADE_REQUEST_SENT("trade.request_sent", "recipient"),
    TRADE_REQUEST_RECEIVED("trade.request_received.message", "sender", "accept", "deny"),
    TRADE_REQUEST_RECEIVED_ACCEPT_HOVER("trade.request_received.accept_hover", "sender"),
    TRADE_REQUEST_RECEIVED_DENY_HOVER("trade.request_received.deny_hover", "sender"),



    ;

    /**
     * This is the messages.yml file that contains all the messages used in the plugin.
     */
    private static @Nullable YamlConfiguration messagesYamlFile = null;

    private final static @NonNull Function<String, Component> NO_PATH = path -> Component.text("<Missing key '", NamedTextColor.WHITE)
            .append(Component.text(path).color(NamedTextColor.GREEN))
            .append(Component.text("'>", NamedTextColor.WHITE));

    private final @NonNull String path;
    private final @NonNull String[] params;

    MessageAssets(@NonNull String path, @NonNull String... params) {
        this.path = path;
        this.params = params;
    }

    /**
     * Builds the path as many components with the given replacements.
     * @param replacements the replacements to be used in the message
     * @return a list of components built from the path with the given replacements
     */
    public @NonNull List<Component> buildMany(@NonNull Object... replacements) {
        if (this.params.length > replacements.length) {
            throw new IllegalArgumentException("The replacements length is less than the params length. Expected: " + this.params.length + ", got: " + replacements.length);
        }

        if (messagesYamlFile == null) {
            throw new NullPointerException("The messages.yml file is not loaded");
        }

        List<String> output = messagesYamlFile.getStringList(this.path);
        if (output.isEmpty()) return new ArrayList<>() {{ this.add(NO_PATH.apply(MessageAssets.this.path)); }};

        return output.stream()
                .map(line -> this.internalBuild(line, replacements))
                .collect(Collectors.toList());
    }

    /**
     * Builds the path as a single component with the given replacements.
     * @param replacements the replacements to be used in the message
     * @return a component built from the path with the given replacements
     */
    public @NonNull Component build(@NonNull Object... replacements) {
        if (this.params.length > replacements.length) {
            throw new IllegalArgumentException("The replacements length is less than the params length. Expected: " + this.params.length + ", got: " + replacements.length);
        }

        if (messagesYamlFile == null) {
            throw new NullPointerException("The messages.yml file is not loaded");
        }

        return Optional.ofNullable(messagesYamlFile.getString(this.path))
                .map(output -> this.internalBuild(output, replacements))
                .orElseGet(() -> NO_PATH.apply(this.path));
    }

    private @NonNull Component internalBuild(@Nullable String output, @NonNull Object... replacements) {
        if (output == null) {
            throw new NullPointerException("The path " + this.path + " is not found in the messages.yml file");
        }

        int paramCount = this.params.length;
        if (paramCount > replacements.length) {
            throw new IllegalArgumentException("The replacements length is less than the params length. Expected: " + paramCount + ", got: " + replacements.length);
        }

        if (paramCount == 0) return MiniMessage.miniMessage().deserialize(output);

        for (int i = 0; i < paramCount; i++) {
            Object replacement = replacements[i];
            if (replacement instanceof Component) {
                replacement = MiniMessage.miniMessage().serialize((Component) replacement);
            }

            output = output.replace("%" + this.params[i] + "%", replacement instanceof String ? (String) replacement : replacement.toString());
        }

        return MiniMessage.miniMessage().deserialize(output);
    }

    /**
     * This method is used to get a message from the internal messages.yml file.
     * @param path the path to the message in the messages.yml file
     * @return a component representing the message at the given path
     */
    public static @NonNull Component internal(@NonNull String path) {
        if (messagesYamlFile == null) {
            throw new NullPointerException("The messages.yml file is not loaded");
        }

        String output = messagesYamlFile.getString(path);
        if (output != null) return MiniMessage.miniMessage().deserialize(output);

        return NO_PATH.apply(path);
    }

    /**
     * This method is used to adjust the messages.yml file that is used in the plugin.
     * @param yamlFile the YamlFile instance that contains the messages
     */
    public static void adjustInternal(@NonNull YamlConfiguration yamlFile) {
        if (messagesYamlFile != null) {
            throw new IllegalStateException("The messages.yml file is already loaded");
        }

        messagesYamlFile = yamlFile;
    }
}