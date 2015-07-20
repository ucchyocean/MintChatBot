/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package com.github.ucchyocean.chatbot;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.github.ucchyocean.chatbot.bridge.VaultChatBridge;

/**
 * レスポンス用のデータ管理オブジェクト
 * @author ucchy
 */
public class ResponceData {

    private static final String FILE_NAME = "responces.txt";
    private static final String FILE_NAME_USERDATA = "userdata.txt";

    private static final String RESPONCE_TIME = "HH:mm:ss";
    private static final String RESPONCE_DATE = "yyyy/MM/dd E";

    private static final String COMMAND_COOLDOWN = "@cooldown";
    private static final String COMMAND_LEARN = "@learn";
    private static final String COMMAND_FORGET = "@forget";

    private LinkedHashMap<String, String> data;
    private LinkedHashMap<String, String> userdata;
    private SimpleDateFormat time_format;
    private SimpleDateFormat date_format;
    private Pattern patternRandomGroup;
    private String prevResponceKey;
    private long prevResponceTime;
    private int responceCooldownSeconds;

    private File jarFile;
    private File file;

    /**
     * コンストラクタ
     * @param jarFile プラグインのJarファイル
     * @param dataFolder プラグインのデータフォルダ
     */
    public ResponceData(File jarFile, File dataFolder, int responceCooldownSeconds) {

        this.jarFile = jarFile;
        this.responceCooldownSeconds = responceCooldownSeconds;

        time_format = new SimpleDateFormat(RESPONCE_TIME);
        date_format = new SimpleDateFormat(RESPONCE_DATE, Locale.JAPAN);
        patternRandomGroup = Pattern.compile(".*\\(([^\\)]*)\\).*");

        file = new File(dataFolder, FILE_NAME);
        reloadData();
    }

    /**
     * 設定ファイルをリロードする
     */
    public void reloadData() {
        data = Utility.loadConfigFile(jarFile, file);
        userdata = Utility.loadConfigFile(null,
                new File(file.getParentFile(), FILE_NAME_USERDATA));
    }

    /**
     * 指定されたチャット発言に対する応答設定がある場合、応答内容を返します。
     * 応答設定が無いなら、nullが返されます。
     * @param source チャット発言内容
     * @param player チャット発言者
     * @param vaultchat valueブリッジ
     * @return 応答内容
     */
    public String getResponceIfMatch(String source, Player player,
            VaultChatBridge vaultchat) {

        String res = getRes(data, source, player, vaultchat, null);
        if ( res != null ) {
            if ( res.equals(COMMAND_COOLDOWN) ) return null;
            if ( res.startsWith(COMMAND_LEARN) ) return learn(res);
            if ( res.startsWith(COMMAND_FORGET) ) return forget(res);
            return res;
        }

        res = getRes(userdata, source, player, vaultchat, null);
        if ( res == null ) return null;
        if ( res.equals(COMMAND_COOLDOWN) ) return null;
        if ( res.startsWith(COMMAND_LEARN) ) return learn(res);
        if ( res.startsWith(COMMAND_FORGET) ) return forget(res);
        return res;
    }

    /**
     * 指定されたチャット発言に対する応答設定がある場合、応答内容を返します。
     * 応答設定が無いなら、nullが返されます。
     * @param source チャット発言内容
     * @param player チャット発言者名
     * @return 応答内容
     */
    public String getResponceIfMatch(String source, String player) {

        String res = getRes(data, source, null, null, player);
        if ( res != null ) {
            if ( res.equals(COMMAND_COOLDOWN) ) return null;
            if ( res.startsWith(COMMAND_LEARN) ) return learn(res);
            if ( res.startsWith(COMMAND_FORGET) ) return forget(res);
            return res;
        }

        res = getRes(userdata, source, null, null, player);
        if ( res == null ) return null;
        if ( res.equals(COMMAND_COOLDOWN) ) return null;
        if ( res.startsWith(COMMAND_LEARN) ) return learn(res);
        if ( res.startsWith(COMMAND_FORGET) ) return forget(res);
        return res;
    }


    /**
     * 指定されたチャット発言に対する応答設定がある場合、応答内容を返します。
     * 応答設定が無いなら、nullが返されます。
     */
    private String getRes(LinkedHashMap<String, String> data,
            String source, Player player, VaultChatBridge vaultchat, String altName) {

        for ( String key : data.keySet() ) {

            String responce = data.get(key);

            boolean isNotRepeat = false;
            if ( key.startsWith("@") ) {
                isNotRepeat = true;
                key = key.substring(1);
            }

            if ( source.matches(key) ) {

                long cooldown = responceCooldownSeconds * 1000;
                if ( isNotRepeat && prevResponceKey != null &&
                        prevResponceKey.equals(key) &&
                        (System.currentTimeMillis() - prevResponceTime) < cooldown ) {
                    return COMMAND_COOLDOWN;
                }

                if ( player != null ) {
                    responce = replaceKeywords(responce, player, vaultchat);
                } else {
                    responce = replaceKeywords(responce, altName);
                }
                responce = replaceMatchingGroups(responce, key, source);
                responce = replaceRandomGroup(responce);
                responce = responce.replace("（", "(").replace("）", ")");

                if ( isNotRepeat ) {
                    prevResponceKey = key;
                    prevResponceTime = System.currentTimeMillis();
                }

                return responce;
            }
        }

        return null;
    }

    private String learn(String source) {

        Pattern pat = Pattern.compile("@learn (.+)=(.+)");
        Matcher matcher = pat.matcher(source);
        if ( !matcher.matches() ) return null;
        String key = matcher.group(1);
        String value = matcher.group(2);
        setUserData(key, value);

        String format = MintChatBot.getInstance().getMessages()
                .getResponceIfMatch("study_learn");
        if ( format == null ) return null;
        return format.replace("%key", key).replace("%value", value);
    }

    private String forget(String source) {

        Pattern pat = Pattern.compile("@forget (.+)");
        Matcher matcher = pat.matcher(source);
        if ( !matcher.matches() ) return null;
        String key = matcher.group(1);
        boolean result = removeUserData(key);

        if ( !result ) return null;
        String format = MintChatBot.getInstance().getMessages()
                .getResponceIfMatch("study_forget");
        if ( format == null ) return null;
        return format.replace("%key", key);
    }

    /**
     * ユーザーデータを追加設定or上書き設定する
     * @param key キー
     * @param value 値
     */
    private void setUserData(String key, String value) {
        userdata.put(key, value);
        Utility.saveConfigFile(new File(file.getParentFile(), FILE_NAME_USERDATA), userdata);
    }

    /**
     * ユーザーデータから設定を削除する
     * @param key キー
     */
    private boolean removeUserData(String key) {
        String res = userdata.remove(key);
        if ( res != null ) {
            Utility.saveConfigFile(new File(file.getParentFile(), FILE_NAME_USERDATA), userdata);
            return true;
        }
        return false;
    }

    /**
     * 指定された文字列に含まれるキーワードを置き換える
     * @param source 元の文字列
     * @param player プレイヤー、不要ならnullで良い。
     * @return キーワード置き換え済みの文字列
     */
    private String replaceKeywords(String source, Player player,
            VaultChatBridge vaultchat) {

        String responce = source;

        if ( responce.contains("%player") ) {
            String name = "";
            if ( player != null ) {
                name = player.getDisplayName() + ChatColor.RESET;
            }
            responce = responce.replace("%player", name);
        }

        if ( responce.contains("%prefix") ) {
            String prefix = "";
            if ( vaultchat != null && player != null ) {
                prefix = vaultchat.getPlayerPrefix(player);
            }
            responce = responce.replace("%prefix", prefix);
        }

        if ( responce.contains("%suffix") ) {
            String suffix = "";
            if ( vaultchat != null && player != null ) {
                suffix = vaultchat.getPlayerSuffix(player);
            }
            responce = responce.replace("%suffix", suffix);
        }

        if ( responce.contains("%time") ) {
            String time = time_format.format(new Date());
            responce = responce.replace("%time", time);
        }

        if ( responce.contains("%date") ) {
            String date = date_format.format(new Date());
            responce = responce.replace("%date", date);
        }

        return responce;
    }

    /**
     * 指定された文字列に含まれるキーワードを置き換える
     * @param source 元の文字列
     * @param player プレイヤー、不要ならnullで良い。
     * @return キーワード置き換え済みの文字列
     */
    private String replaceKeywords(String source, String player) {

        String responce = source;
        responce = responce.replace("%player", player);
        responce = responce.replace("%prefix", "");
        responce = responce.replace("%suffix", "");

        if ( responce.contains("%time") ) {
            String time = time_format.format(new Date());
            responce = responce.replace("%time", time);
        }

        if ( responce.contains("%date") ) {
            String date = date_format.format(new Date());
            responce = responce.replace("%date", date);
        }

        return responce;
    }

    /**
     * 正規表現のマッチンググループを置き換える
     * @param source 元の文字列（＝応答内容）
     * @param key 正規表現パターン
     * @param org 正規表現にマッチさせる内容（＝元のチャット発言内容）
     * @return 置き換えられた文字列
     */
    private String replaceMatchingGroups(String source, String key, String org) {

        String responce = source;

        if ( responce.matches(".*%[1-9].*") ) {

            Pattern pattern = Pattern.compile(key);
            Matcher matcher = pattern.matcher(org);
            boolean isMatch = matcher.matches();
            for ( int index = 1; index <= 9; index++ ) {
                String groupkey = "%" + index;
                if ( !responce.contains(groupkey) ) {
                    continue;
                }
                if ( !isMatch || matcher.groupCount() < index ) {
                    responce = responce.replace(groupkey, "");
                } else {
                    responce = responce.replace(groupkey, matcher.group(index));
                }
            }
        }

        return responce;
    }

    /**
     * ランダムグループが設定されている場合に、ランダムに選択して置き換えして返します。
     * @param source 元の文字列
     * @return 置き換えられた文字列
     */
    private String replaceRandomGroup(String source) {

        Matcher matcher = patternRandomGroup.matcher(source);

        if ( matcher.matches() ) {

            String org = matcher.group(1);
            String[] items = org.split("\\|");

            int index = (int)(Math.random() * items.length);
            return source.replace("(" + org + ")", items[index]);
        }

        return source;
    }

    // デバッグ用のエントリポイント
    public static void main(String[] args) {

        File folder = new File("src\\main\\resources");
        ResponceData test = new ResponceData(null, folder, 15);

        for ( String key : test.data.keySet() ) {
            System.out.println(String.format("key={%s}, data={%s}", key, test.data.get(key)));
        }

        String[] testees = new String[]{"hi.", "Hi!", "おはよう", "いまなんじ？", "今日何日",
                "ごはんください", "お金をください！", "マスターいつもの", "占い", "⑨", "(´ω｀)", "(*´ω｀*)",
                "さいころ"};

        for ( String testee : testees ) {
            System.out.println(String.format("testee={%s}, responce={%s}",
                    testee, test.getResponceIfMatch(testee, "てすと")));
        }
    }

}
