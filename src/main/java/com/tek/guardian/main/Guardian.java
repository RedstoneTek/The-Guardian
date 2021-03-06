package com.tek.guardian.main;

import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.tek.guardian.cache.MessageCache;
import com.tek.guardian.chat.AdvertisingFilter;
import com.tek.guardian.chat.ChatModerator;
import com.tek.guardian.chat.SpammingFilter;
import com.tek.guardian.commands.BanCommand;
import com.tek.guardian.commands.ClearCommand;
import com.tek.guardian.commands.CommandHandler;
import com.tek.guardian.commands.ConfigCommand;
import com.tek.guardian.commands.CreditsCommand;
import com.tek.guardian.commands.CustomVoiceChannelCommand;
import com.tek.guardian.commands.DeafenCommand;
import com.tek.guardian.commands.GuideCommand;
import com.tek.guardian.commands.HelpCommand;
import com.tek.guardian.commands.KickCommand;
import com.tek.guardian.commands.LockCommand;
import com.tek.guardian.commands.MentionCommand;
import com.tek.guardian.commands.MuteCommand;
import com.tek.guardian.commands.PingCommand;
import com.tek.guardian.commands.PollCommand;
import com.tek.guardian.commands.ReactionRoleCommand;
import com.tek.guardian.commands.RoleColorCommand;
import com.tek.guardian.commands.SecurityScanCommand;
import com.tek.guardian.commands.ServerCommand;
import com.tek.guardian.commands.SlowmodeCommand;
import com.tek.guardian.commands.SuggestCommand;
import com.tek.guardian.commands.TempbanCommand;
import com.tek.guardian.commands.TempdeafenCommand;
import com.tek.guardian.commands.TempmuteCommand;
import com.tek.guardian.commands.UndeafenCommand;
import com.tek.guardian.commands.UnlockCommand;
import com.tek.guardian.commands.UnmuteCommand;
import com.tek.guardian.commands.UnwarnCommand;
import com.tek.guardian.commands.VoiceKickCommand;
import com.tek.guardian.commands.WarnCommand;
import com.tek.guardian.commands.WarningsCommand;
import com.tek.guardian.commands.WhoisCommand;
import com.tek.guardian.config.Config;
import com.tek.guardian.data.MongoAdapter;
import com.tek.guardian.events.AccountFlaggingListener;
import com.tek.guardian.events.MemberStatusListener;
import com.tek.guardian.events.MessageChangeListener;
import com.tek.guardian.events.ReactionRoleListener;
import com.tek.guardian.events.ServerStatusListener;
import com.tek.guardian.events.VoiceChannelListener;
import com.tek.guardian.timer.TaskTimer;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

public class Guardian {
	
	public static final Logger LOGGER = Logger.getLogger(Guardian.class);
	
	private static Guardian instance;
	
	private Config config;
	private MongoAdapter mongoAdapter;
	private JDA jda;
	private EventWaiter waiter;
	private CommandHandler commandHandler;
	private TaskTimer taskTimer;
	private ActionManager actionManager;
	private MessageCache messageCache;
	private ChatModerator chatModerator;
	
	public Guardian(Config config) {
		this.config = config;
	}
	
	public void start() throws LoginException, InterruptedException {
		instance = this;
		
		mongoAdapter = new MongoAdapter();
		mongoAdapter.connect(Reference.DATABASE);
		
		jda = new JDABuilder()
				.setToken(config.getToken())
				.setActivity(Activity.playing(config.getPresence()))
				.build();
		
		waiter = new EventWaiter();
		
		commandHandler = new CommandHandler();
		commandHandler.registerCommand(new HelpCommand(waiter));
		commandHandler.registerCommand(new GuideCommand(waiter));
		commandHandler.registerCommand(new WhoisCommand(waiter));
		commandHandler.registerCommand(new CustomVoiceChannelCommand());
		commandHandler.registerCommand(new SuggestCommand());
		commandHandler.registerCommand(new PollCommand(waiter));
		commandHandler.registerCommand(new ReactionRoleCommand(waiter));
		commandHandler.registerCommand(new ConfigCommand());
		commandHandler.registerCommand(new SecurityScanCommand(waiter));
		commandHandler.registerCommand(new ServerCommand());
		commandHandler.registerCommand(new LockCommand());
		commandHandler.registerCommand(new UnlockCommand());
		commandHandler.registerCommand(new ClearCommand());
		commandHandler.registerCommand(new WarnCommand());
		commandHandler.registerCommand(new WarningsCommand(waiter));
		commandHandler.registerCommand(new UnwarnCommand());
		commandHandler.registerCommand(new VoiceKickCommand());
		commandHandler.registerCommand(new MentionCommand());
		commandHandler.registerCommand(new SlowmodeCommand());
		commandHandler.registerCommand(new RoleColorCommand());
		commandHandler.registerCommand(new KickCommand());
		commandHandler.registerCommand(new BanCommand());
		commandHandler.registerCommand(new TempbanCommand());
		commandHandler.registerCommand(new MuteCommand());
		commandHandler.registerCommand(new TempmuteCommand());
		commandHandler.registerCommand(new UnmuteCommand());
		commandHandler.registerCommand(new DeafenCommand());
		commandHandler.registerCommand(new TempdeafenCommand());
		commandHandler.registerCommand(new UndeafenCommand());
		commandHandler.registerCommand(new PingCommand());
		commandHandler.registerCommand(new CreditsCommand());
		
		jda.addEventListener(waiter, commandHandler, new ServerStatusListener(), 
				new VoiceChannelListener(), new AccountFlaggingListener(), new ReactionRoleListener(),
				new MemberStatusListener(), new MessageChangeListener());
		
		taskTimer = new TaskTimer();
		taskTimer.start();
		
		actionManager = new ActionManager();
		messageCache = new MessageCache();
		
		chatModerator = new ChatModerator();
		chatModerator.registerFilter(new AdvertisingFilter());
		chatModerator.registerFilter(new SpammingFilter());
		
		jda.awaitReady();
		
		LOGGER.info("Launched Guardian successfully!");
	}
	
	public static Guardian getInstance() {
		return instance;
	}
	
	public Config getConfig() {
		return config;
	}
	
	public MongoAdapter getMongoAdapter() {
		return mongoAdapter;
	}
	
	public JDA getJDA() {
		return jda;
	}
	
	public EventWaiter getWaiter() {
		return waiter;
	}
	
	public CommandHandler getCommandHandler() {
		return commandHandler;
	}
	
	public TaskTimer getTaskTimer() {
		return taskTimer;
	}
	
	public ActionManager getActionManager() {
		return actionManager;
	}
	
	public MessageCache getMessageCache() {
		return messageCache;
	}
	
	public ChatModerator getChatModerator() {
		return chatModerator;
	}
	
}
