package com.github.games647.mcmmoaction;

import com.gmail.nossr50.datatypes.skills.AbilityType;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.datatypes.skills.ToolType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.StringUtils;
import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import static java.util.stream.Collectors.toList;

public class Configuration {

    private static final String BUNDLE_ROOT = "com.gmail.nossr50.locale.locale";
    private static final String NOTIFICATION_IDENTIFIER = "**";

    private final mcMMOAction plugin;
    private final Set<String> messages = Sets.newHashSet();
    private final Set<SkillType> disabledSkillProgress = Sets.newHashSet();

    //notification sound
    private Sound sound;
    private float volume;
    private float pitch;
    private boolean progressEnabled;

    public Configuration(mcMMOAction plugin) {
        this.plugin = plugin;
    }

    public void saveDefault() {
        this.plugin.saveDefaultConfig();
    }

    public void load() {
        FileConfiguration config = plugin.getConfig();

        loadMessages(config);
        loadNotificationSound(config.getConfigurationSection("notification-sound"));

        progressEnabled = config.getBoolean("progress");

        for (String disableSkill : config.getStringList("progress-disabled")) {
            Optional<SkillType> skillType = Enums.getIfPresent(SkillType.class, disableSkill.toUpperCase());
            if (skillType.isPresent()) {
                disabledSkillProgress.add(skillType.get());
            } else {
                plugin.getLogger()
                        .log(Level.WARNING, "The skill type {0} for disabled progress is unknown", disableSkill);
            }
        }
    }

    private void loadNotificationSound(ConfigurationSection section) {
        if (section.getBoolean("enabled")) {
            volume = (float) section.getDouble("volume");
            pitch = (float) section.getDouble("pitch");

            String soundType = section.getString("type");
            Optional<Sound> sound = Enums.getIfPresent(Sound.class, soundType.toUpperCase());
            if (sound.isPresent()) {
                this.sound = sound.get();
            } else {
                plugin.getLogger().log(Level.WARNING, "Failed to load the sound type");
            }
        }
    }

    private void loadMessages(ConfigurationSection config) {
        messages.addAll(loadingByIdentifier());

        for (SkillType skillType : SkillType.values()) {
            if (!skillType.isChildSkill()) {
                String messageKey = StringUtils.getCapitalized(skillType.toString()) + ".Skillup";
                String localizedMessage = getLocalizedMessage(messageKey);
                addOrRemove(messages, localizedMessage, config.getBoolean("ignore.levelup"));
            }

            AbilityType ability = skillType.getAbility();
            if (ability != null) {
                String abilityOn = ChatColor.stripColor(ability.getAbilityOn());
                String abilityOff = ChatColor.stripColor(ability.getAbilityOff());
                addOrRemove(messages, abilityOn, config.getBoolean("ignore.ability"));
                addOrRemove(messages, abilityOff, config.getBoolean("ignore.ability"));
            }

            ToolType tool = skillType.getTool();
            if (tool != null) {
                addOrRemove(messages, ChatColor.stripColor(tool.getRaiseTool()), config.getBoolean("ignore.tool"));
                addOrRemove(messages, ChatColor.stripColor(tool.getLowerTool()), config.getBoolean("ignore.tool"));
            }
        }

        //messages that cannot be retrieved dynamically because the message key isn't in (or equal as)
        //the enum getSkillAbilities() - SecondaryAbilities
        messages.add(getLocalizedMessage("Axes.Combat.SS.Struck"));

        messages.add(getLocalizedMessage("Axes.Combat.CriticalHit"));
        messages.add(getLocalizedMessage("Axes.Combat.CritStruck"));

        messages.add(getLocalizedMessage("Swords.Combat.Counter.Hit"));
        messages.add(getLocalizedMessage("Swords.Combat.Bleeding"));
        messages.add(getLocalizedMessage("Swords.Combat.Bleeding.Started"));
        messages.add(getLocalizedMessage("Swords.Combat.Bleeding.Stopped"));

        messages.add(getLocalizedMessage("Party.LevelUp"));

        //hardcore messages
        boolean hardcoreIgnore = config.getBoolean("ignore.hardcore");
        addOrRemove(messages, getLocalizedMessage("Hardcore.DeathStatLoss.PlayerDeath"), hardcoreIgnore);
        addOrRemove(messages, getLocalizedMessage("Hardcore.Vampirism.Killer.Failure"), hardcoreIgnore);
        addOrRemove(messages, getLocalizedMessage("Hardcore.Vampirism.Killer.Success"), hardcoreIgnore);
        addOrRemove(messages, getLocalizedMessage("Hardcore.Vampirism.Victim.Failure"), hardcoreIgnore);
        addOrRemove(messages, getLocalizedMessage("Hardcore.Vampirism.Victim.Success"), hardcoreIgnore);

        //general message
        addOrRemove(messages, getLocalizedMessage("Skills.TooTired"), config.getBoolean("ignore.tooTired"));

        //explicit added messages
        messages.addAll(config.getStringList("others").stream()
                .map(this::getLocalizedMessage)
                .collect(toList()));

        //explicit ignored messages
        config.getStringList("ignore.others").stream().map(this::getLocalizedMessage).forEach(messages::remove);
    }

    private void addOrRemove(Collection<String> messages, String message, boolean ignore) {
        if (ignore) {
            messages.remove(message);
        } else {
            messages.add(message);
        }
    }

    private Collection<String> loadingByIdentifier() {
        Set<String> builder = Sets.newHashSet();

        ClassLoader classLoader = mcMMO.p.getClass().getClassLoader();
        ResourceBundle enBundle = ResourceBundle.getBundle(BUNDLE_ROOT, Locale.US, classLoader);
        for (Enumeration<String> enumeration = enBundle.getKeys(); enumeration.hasMoreElements(); ) {
            String key = enumeration.nextElement();
            String localizedMessage = getLocalizedMessage(key);
            if (localizedMessage.endsWith(NOTIFICATION_IDENTIFIER)) {
                builder.add(localizedMessage);
            }
        }

        return builder;
    }

    private String getLocalizedMessage(String key) {
        //if the message has less arguments they will be just ignored
        String localizedMessage = LocaleLoader.getString(key, 0, 0, 0, 0);
        //strip color to match faster and easier
        return ChatColor.stripColor(localizedMessage);
    }

    public Sound getSoundType() {
        return sound;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }

    public Set<String> getMessages() {
        return messages;
    }

    public Set<SkillType> getDisabledSkillProgress() {
        return disabledSkillProgress;
    }

    public boolean isProgressEnabled() {
        return progressEnabled;
    }
}
