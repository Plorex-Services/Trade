package it.bitrule.trade;

import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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

    TRADE_COMMAND_USAGE("command_usage"),

    PLAYER_NOT_ONLINE("player_not_online", "player"),

    CANNOT_TRADE_YOURSELF("cannot_trade_yourself"),
    SENDER_ALREADY_TRADING("sender_already_trading"),
    RECEPTOR_ALREADY_TRADING("receptor_already_trading", "player"),

    SENDER_ALREADY_SENT_REQUEST("sender_already_sent_request", "player"),
    RECEPTOR_ALREADY_SENT_REQUEST("receptor_already_sent_request", "player"),

    REQUEST_SENT("request_sent", "player"),
    REQUEST_RECEIVED("request_received.message", "player", "accept", "deny"),
    REQUEST_RECEIVED_ACCEPT_HOVER("request_received.accept_hover", "player"),
    REQUEST_RECEIVED_DENY_HOVER("request_received.deny_hover", "player"),

    NO_REQUEST_FOUND("no_request_found", "player"),
    REQUEST_ACCEPTED("request_accepted", "player"),
    REQUEST_WAS_ACCEPTED("request_was_accepted", "player"),

    TRANSACTION_CANCELLED("transaction_cancelled", "player"),
    TRANSACTION_WAS_CANCELLED("transaction_was_cancelled", "player"),

    ENDING_COUNTDOWN("ending_countdown", "player", "remaining"),

    MENU_TITLE("menu.title", "player"),

    MENU_STATE_OPTION_LORE_SELF_NOT_DONE("menu.state_option.lore.self.not_done"),
    MENU_STATE_OPTION_LORE_SELF_DONE("menu.state_option.lore.self.done"),
    MENU_STATE_OPTION_LORE_SELF_DONE_COUNTDOWN("menu.state_option.lore.self.done_countdown", "countdown"),
    MENU_STATE_OPTION_LORE_SELF_DONE_WAITING("menu.state_option.lore.self.done_waiting", "player"),

    MENU_STATE_OPTION_DISPLAY_NAME_OTHER_NOT_DONE("menu.state_option.display_name.other_not_done", "player"),
    MENU_STATE_OPTION_LORE_OTHER_NOT_DONE("menu.state_option.lore.other.not_done", "player"),
    MENU_STATE_OPTION_DISPLAY_NAME_OTHER_DONE("menu.state_option.display_name.other_done", "player"),
    MENU_STATE_OPTION_LORE_OTHER_DONE("menu.state_option.lore.other.done", "player");

    /**
     * This is the messages.yml file that contains all the messages used in the plugin.
     */
    private static @Nullable YamlConfiguration messagesYamlFile = null;

    private final static @NonNull Function<String, Component> NO_PATH = path -> Component.text("<Missing key '", NamedTextColor.WHITE)
            .append(Component.text(path).color(NamedTextColor.GREEN))
            .append(Component.text("'>", NamedTextColor.WHITE));

    private static @Nullable Component PREFIX_COMPONENT = null;

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

        if (paramCount == 0)
            return MiniMessage.miniMessage()
                    .deserialize(output)
                    .replaceText(builder -> builder.matchLiteral("<trade_prefix>")
                            .replacement(PREFIX_COMPONENT != null ? PREFIX_COMPONENT : NO_PATH.apply("prefix"))
                    );

        for (int i = 0; i < paramCount; i++) {
            Object replacement = replacements[i];
            if (replacement instanceof Component) {
                replacement = MiniMessage.miniMessage().serialize((Component) replacement);
            }

            output = output.replace("%" + this.params[i] + "%", replacement instanceof String ? (String) replacement : replacement.toString());
        }

        return MiniMessage.miniMessage()
                .deserialize(output)
                .replaceText(builder -> builder.matchLiteral("<trade_prefix>")
                        .replacement(PREFIX_COMPONENT != null ? PREFIX_COMPONENT : NO_PATH.apply("prefix"))
                );
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

    public static @NonNull List<Component> replace(@NonNull List<Component> components, @NonNull List<Component> replacements) {
        List<Component> output = new ArrayList<>();
        for (Component component : components) {
            if (!((TextComponent) component).content().contains("%content%")) {
                output.add(component);
            } else {
                output.addAll(replacements);
            }
        }

        return output;
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

        PREFIX_COMPONENT = Optional.ofNullable(messagesYamlFile.getString("prefix"))
                .map(MiniMessage.miniMessage()::deserialize)
                .orElse(NO_PATH.apply("prefix"));
    }
}