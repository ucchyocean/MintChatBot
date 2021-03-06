/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package com.github.ucchyocean.chatbot.irc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;

import com.github.ucchyocean.chatbot.MintChatBot;
import com.github.ucchyocean.chatbot.URLResponcer;

/**
 * IRCBotのリスナー部分
 * @author ucchy
 */
public class IRCListener extends ListenerAdapter implements Listener {

    private MintChatBot plugin;
    private IRCBotConfig config;
    private PircBotX bot;

    public IRCListener(IRCBotConfig config) {
        this.config = config;
        this.plugin = MintChatBot.getInstance();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ========== IRC --> Minecraft ==========

    /**
     * @see org.pircbotx.hooks.ListenerAdapter#onConnect(org.pircbotx.hooks.events.ConnectEvent)
     */
    @Override
    public void onConnect(ConnectEvent event) throws Exception {
        bot = event.getBot();
        String format = plugin.getMessages().getResponceIfMatch("irc_connect");
        if ( format == null ) return;
        String message = format
                .replace("%server", config.getServerHostname())
                .replace("%channel", config.getChannel());

        String botname = MintChatBot.getInstance().getCBConfig().getBotName();
        String resp = IRCColor.convRES2MC(
                MintChatBot.getInstance().getCBConfig().getResponceFormat()
                    .replace("%botName", botname)
                    .replace("%responce", message));
        Bukkit.broadcastMessage(resp);
    }

    /**
     * @see org.pircbotx.hooks.ListenerAdapter#onDisconnect(org.pircbotx.hooks.events.DisconnectEvent)
     */
    @Override
    public void onDisconnect(DisconnectEvent event) throws Exception {
        String format = plugin.getMessages().getResponceIfMatch("irc_disconnect");
        if ( format == null ) return;
        String message = format
                .replace("%server", config.getServerHostname())
                .replace("%channel", config.getChannel());

        String botname = MintChatBot.getInstance().getCBConfig().getBotName();
        String resp = IRCColor.convRES2MC(
                MintChatBot.getInstance().getCBConfig().getResponceFormat()
                    .replace("%botName", botname)
                    .replace("%responce", message));
        Bukkit.broadcastMessage(resp);
    }

    /**
     * @see org.pircbotx.hooks.ListenerAdapter#onJoin(org.pircbotx.hooks.events.JoinEvent)
     */
    @Override
    public void onJoin(JoinEvent event) throws Exception {
        if ( event.getUser().getNick().equals(bot.getNick()) ) return;
        String format = plugin.getMessages().getResponceIfMatch("irc_join");
        if ( format != null ) {
            String message = IRCColor.convRES2MC(format.replace("%name", event.getUser().getNick()));
            Bukkit.broadcastMessage(message);
        }

        // 必要に応じて、サーバー参加応答を返す
        if ( config.isResponceJoinServer() ) {
            format = plugin.getMessages().getResponceIfMatch("joinResponce");
            if ( format != null ) {
                String message = IRCColor.convRES2IRC(format.replace("%player", event.getUser().getNick()));
                plugin.say(message);
            }
        }
    }

    /**
     * @see org.pircbotx.hooks.ListenerAdapter#onKick(org.pircbotx.hooks.events.KickEvent)
     */
    @Override
    public void onKick(KickEvent event) throws Exception {
        String format = plugin.getMessages().getResponceIfMatch("irc_kick");
        if ( format == null ) return;
        String message = IRCColor.convRES2MC(format
                .replace("%name", event.getRecipient().getNick())
                .replace("%reason", event.getReason())
                .replace("%kicker", event.getUser().getNick()));
        Bukkit.broadcastMessage(message);
    }

    /**
     * @see org.pircbotx.hooks.ListenerAdapter#onPart(org.pircbotx.hooks.events.PartEvent)
     */
    @Override
    public void onPart(PartEvent event) throws Exception {
        if ( event.getUser().getNick().equals(bot.getNick()) ) return;
        String format = plugin.getMessages().getResponceIfMatch("irc_part");
        if ( format == null ) return;
        String message = IRCColor.convRES2MC(
                format.replace("%name", event.getUser().getNick()).replace("%reason", event.getReason()));
        Bukkit.broadcastMessage(message);
    }

    /**
     * @see org.pircbotx.hooks.ListenerAdapter#onQuit(org.pircbotx.hooks.events.QuitEvent)
     */
    @Override
    public void onQuit(QuitEvent event) throws Exception {
        if ( event.getUser().getNick().equals(bot.getNick()) ) return;
        String format = plugin.getMessages().getResponceIfMatch("irc_quit");
        if ( format == null ) return;
        String message = IRCColor.convRES2MC(
                format.replace("%name", event.getUser().getNick()).replace("%reason", event.getReason()));
        Bukkit.broadcastMessage(message);
    }

    /**
     * @see org.pircbotx.hooks.ListenerAdapter#onMessage(org.pircbotx.hooks.events.MessageEvent)
     */
    @Override
    public void onMessage(MessageEvent event) throws Exception {

        if ( event.getUser().getNick().equals(bot.getNick()) ) return;
        String format = plugin.getMessages().getResponceIfMatch("irc_chat");
        if ( format != null ) {
            String message = IRCColor.convRES2MC(
                    format.replace("%name", event.getUser().getNick()).replace("%message",
                            IRCColor.convIRC2MC(event.getMessage())));
            Bukkit.broadcastMessage(message);

            if ( plugin.getDynmap() != null ) {
                plugin.getDynmap().broadcast(ChatColor.stripColor(message));
            }
        }

        // 必要に応じて、自動応答を返す
        if ( config.isResponceChat() ) {
            String responce = plugin.getResponceData().getResponceIfMatch(
                    event.getMessage(), event.getUser().getNick());
            if ( responce != null ) {
                plugin.say(responce);
            }
        }

        // 必要に応じて、URL応答を返す
        if ( config.isGetURLTitle() && URLResponcer.containsURL(event.getMessage()) ) {
            URLResponcer resp = new URLResponcer(event.getMessage(), event.getUser().getNick(), null);
            String responce = resp.getResponce();
            if ( responce != null ) {
                plugin.say(responce);
            }
        }
    }

    // ========== Minecraft --> IRC ==========

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if ( bot == null ) return;
        String format = plugin.getMessages().getResponceIfMatch("minecraft_chat");
        if ( format == null ) return;
        final String message = IRCColor.convRES2IRC(
                replacePlayerKeyword(format, event.getPlayer(), event.getMessage()));
        new BukkitRunnable() {
            public void run() {
                bot.sendIRC().message(config.getChannel(), message);
            }
        }.runTaskAsynchronously(MintChatBot.getInstance());
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if ( bot == null ) return;
        String format = plugin.getMessages().getResponceIfMatch("minecraft_join");
        if ( format == null ) return;
        final String message = IRCColor.convRES2IRC(
                replacePlayerKeyword(format, event.getPlayer(), ""));
        new BukkitRunnable() {
            public void run() {
                bot.sendIRC().message(config.getChannel(), message);
            }
        }.runTaskAsynchronously(MintChatBot.getInstance());
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if ( bot == null ) return;
        String format = plugin.getMessages().getResponceIfMatch("minecraft_quit");
        if ( format == null ) return;
        final String message = IRCColor.convRES2IRC(
                replacePlayerKeyword(format, event.getPlayer(), ""));
        new BukkitRunnable() {
            public void run() {
                bot.sendIRC().message(config.getChannel(), message);
            }
        }.runTaskAsynchronously(MintChatBot.getInstance());
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerKick(PlayerKickEvent event) {
        if ( bot == null ) return;
        String format = plugin.getMessages().getResponceIfMatch("minecraft_kick");
        if ( format == null ) return;
        final String message = IRCColor.convRES2IRC(
                replacePlayerKeyword(format, event.getPlayer(), ""))
                .replace("%reason", event.getReason());
        new BukkitRunnable() {
            public void run() {
                bot.sendIRC().message(config.getChannel(), message);
            }
        }.runTaskAsynchronously(MintChatBot.getInstance());
    }

    // ========== LunaChat --> IRC ==========

    public void onLunaChat(Player player, String msg) {
        if ( bot == null ) return;
        String format = plugin.getMessages().getResponceIfMatch("minecraft_chat");
        if ( format == null ) return;
        final String message = IRCColor.convRES2IRC(
                replacePlayerKeyword(format, player, msg));
        new BukkitRunnable() {
            public void run() {
                bot.sendIRC().message(config.getChannel(), message);
            }
        }.runTaskAsynchronously(MintChatBot.getInstance());
    }

    // ========== Other --> IRC ==========

    public void onOtherChat(String name, String msg) {
        if ( bot == null ) return;
        String format = plugin.getMessages().getResponceIfMatch("minecraft_chat");
        if ( format == null ) return;
        final String message = format.replace("%name", name).replace("%message", msg);
        new BukkitRunnable() {
            public void run() {
                bot.sendIRC().message(config.getChannel(), message);
            }
        }.runTaskAsynchronously(MintChatBot.getInstance());
    }

    private String replacePlayerKeyword(String original, Player player, String message) {

        String prefix, suffix;
        if ( plugin.getVaultChat() != null ) {
            prefix = plugin.getVaultChat().getPlayerPrefix(player);
            suffix = plugin.getVaultChat().getPlayerSuffix(player);
        } else {
            prefix = "";
            suffix = "";
        }

        String str = original;
        str = str.replace("%player", player.getName());
        str = str.replace("%name", player.getDisplayName());
        str = str.replace("%message", message);
        str = str.replace("%prefix", prefix);
        str = str.replace("%suffix", suffix);
        return str;
    }
}
