package com.xs.serverinfo;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.xs.loader.util.EmbedCreator.createEmbed;

public class Main extends PluginEvent {

    private LangGetter langGetter;
    private final String[] LANG_DEFAULT = {"en_US", "zh_TW"};

    FileGetter getter;
    Logger logger;

    private static final String TAG = "ServerInfo";
    private static final String VERSION = "1.0";
    final String PATH_FOLDER_NAME = "plugins/ServerInfo";

    public Main() {
        super(TAG, VERSION);
    }

    @Override
    public void initLoad() {
        super.initLoad();
        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class.getClassLoader());
        loadConfigFile();
        loadVariables();
        loadLang();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        super.unload();
        logger.log("UnLoaded");
    }
// TODO:
//    @Override
//    public CommandData[] guildCommands() {
//        return new CommandData[]{
//                new CommandDataImpl("info", lang.get("REGISTER_NAME"));
//        };
//    }

    @Override
    public void loadConfigFile() {
        JSONObject config = new JSONObject(getter.readYml("config.yml", PATH_FOLDER_NAME));
        langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, config.getString("Lang"));
        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void loadLang() {
        // expert files
        langGetter.exportDefaultLang();
        JSONObject lang = langGetter.getLangFileData();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("info")) return;
        Guild guild = event.getGuild();
        int voiceCount = guild.getVoiceChannels().size();
        int textCount = guild.getTextChannels().size();
        int stageCount = guild.getStageChannels().size();
        int memberCount = guild.getMemberCount();

        guild.loadMembers().onSuccess(members -> {
            int realMemberCount = 0;
            int botCount = 0;
            int online = 0;
            int working = 0;
            int idle = 0;
            int offline = 0;

            for (Member i : members) {
                if (i.getUser().isBot())
                    botCount++;
                else
                    realMemberCount++;
                switch (i.getOnlineStatus()) {
                    case ONLINE: {
                        online++;
                        break;
                    }
                    case DO_NOT_DISTURB: {
                        working++;
                        break;
                    }
                    case IDLE: {
                        idle++;
                        break;
                    }
                    case OFFLINE: {
                        offline++;
                        break;
                    }
                }
                guild.unloadMember(i.getIdLong());
            }


            List<MessageEmbed.Field> fields = new ArrayList<>();
            fields.add(new MessageEmbed.Field("Members",
                    "Total " + memberCount + "\n" +
                            "Human " + realMemberCount + "\n" +
                            "Bot " + botCount, true));
            fields.add(new MessageEmbed.Field("Member Status",
                    "Online " + online + "\n" +
                            "Working " + working + "\n" +
                            "Idle " + idle + "\n" +
                            "Offline " + offline, true));
            fields.add(new MessageEmbed.Field("Roles", String.valueOf(guild.getRoles().size()), true));
            fields.add(new MessageEmbed.Field("Channels",
                    "Total " + (textCount + voiceCount + stageCount) + "\n" +
                            "Text " + textCount + "\n" +
                            "Voice " + voiceCount + "\n" +
                            "Stage " + stageCount, true));
            fields.add(new MessageEmbed.Field("Emoji", String.valueOf(guild.retrieveEmojis().complete().size()), true));
            fields.add(new MessageEmbed.Field("Sticker", String.valueOf(guild.retrieveStickers().complete().size()), true));
            fields.add(new MessageEmbed.Field("Boost", "Tier " + guild.getBoostTier() + "\nCount " + guild.getBoostCount(), true));
            fields.add(new MessageEmbed.Field("Language", guild.getLocale().getNativeName(), true));
            Member owner = event.getGuild().retrieveOwner().complete();

            event.getHook().editOriginalEmbeds(
                    createEmbed(event.getGuild().getName(), guild.getIconUrl(), "", fields, "- Server Information", owner.getUser().getAsTag(), owner.getEffectiveAvatarUrl(), 0xff0000)).queue();
        }).onError(i -> {
            logger.error("ERROR");
        });
    }
}