package com.xs.whoisspy;

import com.xs.loader.PluginEvent;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.PermissionERROR.permissionCheck;
import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class Main extends PluginEvent {

    private final String[] LANG_DEFAULT = {/*"en_US",*/ "zh_TW"};

    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "WhoIsSpy";
    private static final String VERSION = "1.0";
    private final String PATH_FOLDER_NAME = "plugins/WhoIsSpy";

    private final List<String> COMMANDS = new ArrayList<>(Arrays.asList("開始", "設定"));

    private boolean start = false;
    private Message message;
    private String problem;
    private String spy_problem;
    private int spyCount;
    private int whiteCount;
    private Category category = null;
    private List<Member> users = new ArrayList<net.dv8tion.jda.api.entities.Member>();
    private List<Member> admins = new ArrayList<>();
    private WhoIsSpy game;

    public Main() {
        super(TAG, VERSION);
    }

    private void reset() {
        message = null;
        problem = "";
        spy_problem = "";
        spyCount = 1;
        whiteCount = 0;
        category = null;
        users.clear();
        admins.clear();
        game = null;
        start = false;
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

    @Override
    public CommandData[] guildCommands() {
        return new CommandData[]{
//                new CommandDataImpl(lang_register.getString("cmd"), lang_register.getString("description")).addOptions(
//                        new OptionData(USER, USER_TAG, lang_register_options.getString("user"), true),
//                        new OptionData(INTEGER, DAYS, lang_register_options.getString("day")),
//                        new OptionData(STRING, REASON, lang_register_options.getString("reason"))
//                )
                new CommandDataImpl("開始", "開始誰是臥底遊戲").addOptions(
                        new OptionData(STRING, "problem", "題目", true),
                        new OptionData(STRING, "spy_problem", "臥底題目", true),
                        new OptionData(INTEGER, "spy", "臥底人數"),
                        new OptionData(INTEGER, "white", "白板人數"),
                        new OptionData(CHANNEL, "category", "開始目錄")
                )
        };
    }

    @Override
    public void loadConfigFile() {
//        JSONObject config = new JSONObject(getter.readYml("config.yml", PATH_FOLDER_NAME));
//        langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, config.getString("Lang"));
        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void loadLang() {
        // expert files
//        langGetter.exportDefaultLang();
//        final JSONObject lang = langGetter.getLangFileData();
//        lang_register = lang.getJSONObject("register");
//        lang_register_options = lang_register.getJSONObject("options");
//        lang_runtime = lang.getJSONObject("runtime");
//        lang_runtime_errors = lang_runtime.getJSONObject("errors");
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!COMMANDS.contains(event.getName())) return;
        if (!permissionCheck(Permission.ADMINISTRATOR, event))
            return;

        switch (event.getName()) {
            case "開始": {
                reset();
                event.getChannel().sendMessageEmbeds(createEmbed("誰是臥底遊戲~", String.format("" +
                                        "加入人數: %d\n" +
                                        "裁判人數: %d", users.size(), admins.size())
                                , 0x00FFFF))
                        .addActionRow(
                                new ButtonImpl("join", "加入遊戲", ButtonStyle.SUCCESS, false, null),
                                new ButtonImpl("leave", "退出遊戲", ButtonStyle.DANGER, false, null),
                                new ButtonImpl("admin", "擔任關主", ButtonStyle.SECONDARY, false, null),
                                new ButtonImpl("start", "開始遊戲", ButtonStyle.PRIMARY, false, null)
                        ).queue(i -> message = i);
                event.getHook().sendMessageEmbeds(createEmbed("已發送訊息!", 0x00FFFF)).queue();
                category = event.getOption("category") != null ? event.getOption("category").getAsChannel().asCategory() : null;
                spyCount = event.getOption("spy") != null ? event.getOption("spy").getAsInt() : 1;
                whiteCount = event.getOption("white") != null ? event.getOption("white").getAsInt() : 0;
                problem = event.getOption("problem").getAsString();
                spy_problem = event.getOption("spy_problem").getAsString();

                break;
            }
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        switch (args[0]) {
            case "join": {
                if (start) {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("遊戲已經開始，目前無法加入", 0xFF0000)).queue();
                    return;
                }
                if (users.contains(event.getMember())) {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("你已經加入!", 0xFF0000)).queue();
                } else {
                    users.add(event.getMember());
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("已加入", 0x448844)).queue();
                }
                updateMessage();
                break;
            }
            case "leave": {
                if (start) {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("遊戲已經開始，目前無法退出", 0xFF0000)).queue();
                    return;
                }
                if (users.contains(event.getMember())) {
                    users.remove(event.getMember());
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("已退出", 0x448844)).queue();
                } else {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("你並未加入!", 0xFF0000)).queue();
                }
                updateMessage();
                break;
            }
            case "admin": {
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }
                if (start) {
                    if (admins.contains(event.getMember())) {
                        admins.remove(event.getMember());
                        game.removeMember(event.getMember());
                        event.getInteraction().deferReply(true).addEmbeds(createEmbed("已經退出裁判", 0x448844)).queue();
                    } else {
                        admins.add(event.getMember());
                        game.addMember(event.getMember());
                        event.getInteraction().deferReply(true).addEmbeds(createEmbed("已加入裁判", 0x448844)).queue();
                    }

                    updateMessage();
                    return;
                }
                if (users.contains(event.getMember())) {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("請退出遊戲後再試一次!", 0xFF0000)).queue();
                    return;
                }

                if (admins.contains(event.getMember())) {
                    admins.remove(event.getMember());
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("已經退出裁判!", 0x448844)).queue();
                } else {
                    admins.add(event.getMember());
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("已加入裁判", 0x448844)).queue();
                }
                updateMessage();
                break;
            }
            case "start": {
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }

                if (start) {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("遊戲已經開始了", 0xFF0000)).queue();
                    return;
                }
                event.getInteraction().deferEdit().queue();
                message.editMessageEmbeds(createEmbed("遊戲已開始!", 0xFF0000)).setComponents().queue();
                start = true;
                game = new WhoIsSpy(
                        event.getGuild(),
                        category,
                        users,
                        admins,
                        spyCount,
                        whiteCount,
                        problem,
                        spy_problem
                );
                break;
            }
            case "getrole": {
                event.getInteraction().deferReply(true).addEmbeds(createEmbed(game.getRole(event.getMember().getIdLong()), 0x00FFFF)).queue();
                break;
            }
            case "getallrole": {
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }

                event.getInteraction().deferReply(true).addEmbeds(createEmbed(game.getAllRole(), 0x00FFFF)).queue();
                break;
            }

            case "end": {
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }

                game.endGame();
                reset();
                event.getInteraction().deferEdit().queue();
                break;
            }

            case "delete": {
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }

                event.getGuild().getTextChannelById(args[1]).delete().queue();
                event.getInteraction().deferEdit().queue();
            }
        }
    }

    public void updateMessage() {
        message.editMessageEmbeds(
                createEmbed("誰是臥底遊戲~", String.format("" +
                                "遊玩人數: %d\n" +
                                "關主人數: %d", users.size(), admins.size())
                        , 0x00FFFF)
        ).queue();
    }
}