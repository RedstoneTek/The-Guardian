package com.tek.guardian.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.tek.guardian.enums.BotRole;
import com.tek.guardian.main.Guardian;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.ChannelManager;
import net.dv8tion.jda.api.managers.RoleManager;

@Entity("server_profiles")
public class ServerProfile {
	
	@Id
	private String serverId;
	private String prefix;
	private boolean deleteCommands;
	private boolean replyUnknown;
	private boolean saveRoles;
	private boolean moderateAdvertising;
	private boolean moderateSpam;
	private List<String> commandChannels;
	private List<String> lockedChannels;
	private Map<String, String> roleMap;
	private Map<String, Integer> inviteMap;
	private String voiceChannelCategory;
	private String suggestionChannel;
	private String flagChannel;
	private String logChannel;
	private String deletedChannel;
	private String joinRole;
	
	public ServerProfile() {
		this.commandChannels = new ArrayList<String>();
		this.lockedChannels = new ArrayList<String>();
		this.roleMap = new HashMap<String, String>();
		this.inviteMap = new HashMap<String, Integer>();
		this.voiceChannelCategory = null;
		this.suggestionChannel = null;
		this.flagChannel = null;
		this.logChannel = null;
		this.deletedChannel = null;
		this.joinRole = null;
	}
	
	public ServerProfile(String serverId) {
		this.serverId = serverId;
		this.prefix = Guardian.getInstance().getConfig().getDefaultPrefix();
		this.deleteCommands = true;
		this.replyUnknown = true;
		this.saveRoles = true;
		this.moderateAdvertising = false;
		this.moderateSpam = false;
		this.commandChannels = new ArrayList<String>();
		this.lockedChannels = new ArrayList<String>();
		this.roleMap = new HashMap<String, String>();
		this.inviteMap = new HashMap<String, Integer>();
		this.voiceChannelCategory = null;
		this.suggestionChannel = null;
		this.flagChannel = null;
		this.logChannel = null;
		this.deletedChannel = null;
		this.joinRole = null;
	}
	
	public void join(Guild guild) {
		for(BotRole role : BotRole.values()) {
			createRole(guild, role);
		}
		
		guild.retrieveInvites().queue(invites -> {
			for(Invite invite : invites) {
				inviteMap.put(invite.getCode(), invite.getUses());
			}
			
			save();
		});
		
		save();
	}
	
	public void verify(Guild guild) {
		for(BotRole role : BotRole.values()) {
			if(roleMap.containsKey(role.name())) {
				Role r = guild.getRoleById(roleMap.get(role.name()));
				if(r == null) {
					createRole(guild, role);
					System.out.println("missing role " + role.name());
				}
			} else {
				createRole(guild, role);
				System.out.println("missing " + role.name());
			}
		}
	}
	
	public void leave(String guildId) {
		Guardian.getInstance().getMongoAdapter().removeGuildTemporaryActions(guildId);
		Guardian.getInstance().getMongoAdapter().removeGuildCustomVoiceChannels(guildId);
		Guardian.getInstance().getMongoAdapter().removeGuildRoleMemory(guildId);
		Guardian.getInstance().getMongoAdapter().removeGuildReactionRoles(guildId);
		Guardian.getInstance().getMongoAdapter().removeGuildUserProfiles(guildId);
	}
	
	public void verifyInvites(Guild guild) {
		inviteMap.clear();
		
		guild.retrieveInvites().queue(invites -> {
			for(Invite invite : invites) {
				inviteMap.put(invite.getCode(), invite.getUses());
			}
			
			save();
		});
	}
	
	public void createRole(Guild guild, BotRole role) {
		Role r = guild.createRole().complete();
		
		RoleManager roleManager = r.getManager();
		roleManager.setName(role.getName());
		roleManager.queue(v -> {
			for(VoiceChannel vc : guild.getVoiceChannels()) {
				ChannelManager vcm = vc.getManager();
				vcm.putPermissionOverride(r, Arrays.asList(), 
					role.getDenies().stream().filter(Permission::isVoice).collect(Collectors.toList())).queue();
			}
				
			for(TextChannel vc : guild.getTextChannels()) {
				ChannelManager vcm = vc.getManager();
				vcm.putPermissionOverride(r, Arrays.asList(), 
						role.getDenies().stream().filter(Permission::isText).collect(Collectors.toList())).queue();
			}
		});
		
		roleMap.put(role.name(), r.getId());
		save();
	}
	
	public void save() {
		Guardian.getInstance().getMongoAdapter().saveServerProfile(this);
	}
	
	public Optional<String> resolveInvite(Guild guild) {
		List<Invite> invites = guild.retrieveInvites().complete();
		
		Iterator<String> inviteIterator = inviteMap.keySet().iterator();
		while(inviteIterator.hasNext()) {
			String inv = inviteIterator.next();
			if(!invites.stream().anyMatch(i -> i.getCode().equals(inv))) {
				inviteIterator.remove();
			}
		}
		
		int incrementCount = 0;
		String inviteIncremented = null;
		for(Invite invite : invites) {
			if(inviteMap.containsKey(invite.getCode())) {
				if(invite.getUses() > inviteMap.get(invite.getCode())) {
					inviteIncremented = invite.getCode();
					incrementCount += invite.getUses() - inviteMap.get(invite.getCode());
					inviteMap.put(invite.getCode(), invite.getUses());
				}
			} else {
				if(invite.getUses() > 0) {
					inviteIncremented = invite.getCode();
					incrementCount += invite.getUses();
					inviteMap.put(invite.getCode(), invite.getUses());
				}
			}
		}
		
		save();
		
		if(incrementCount == 1) {
			return Optional.of(inviteIncremented);
		} else {
			return Optional.empty();
		}
	}
	
	public boolean canSendCommand(String channelId, Member member) {
		if(!canMessage(channelId, member)) return false;
		if(commandChannels.isEmpty()) return true;
		return commandChannels.contains(channelId);
	}
	
	public boolean canMessage(String channelId, Member member) {
		if(lockedChannels.contains(channelId)) {
			if(member.hasPermission(Permission.MESSAGE_MANAGE) || member.hasPermission(Permission.MANAGE_CHANNEL)) {
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}
	
	public boolean isLocked(String channelId) {
		return lockedChannels.contains(channelId);
	}
	
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	public void setDeleteCommands(boolean deleteCommands) {
		this.deleteCommands = deleteCommands;
	}
	
	public void setReplyUnknown(boolean replyUnknown) {
		this.replyUnknown = replyUnknown;
	}
	
	public void setSaveRoles(boolean saveRoles) {
		this.saveRoles = saveRoles;
	}
	
	public void setVoiceChannelCategory(Guild guild, String voiceChannelCategory) {
		for(CustomVoiceChannel channel : Guardian.getInstance().getMongoAdapter().getCustomVoiceChannels()) {
			if(channel.getGuildId().equals(guild.getId())) {
				VoiceChannel vc = guild.getVoiceChannelById(channel.getChannelId());
				if(vc != null) vc.delete().queue();
				Guardian.getInstance().getMongoAdapter().removeCustomVoiceChannel(channel);
			}
		}
		
		this.voiceChannelCategory = voiceChannelCategory;
	}
	
	public void setSuggestionChannel(String suggestionChannel) {
		this.suggestionChannel = suggestionChannel;
	}
	
	public void setFlagChannel(String flagChannel) {
		this.flagChannel = flagChannel;
	}
	
	public void setLogChannel(String logChannel) {
		this.logChannel = logChannel;
	}
	
	public void setDeletedChannel(String deletedChannel) {
		this.deletedChannel = deletedChannel;
	}
	
	public void setModerateAdvertising(boolean moderateAdvertising) {
		this.moderateAdvertising = moderateAdvertising;
	}
	
	public void setModerateSpam(boolean moderateSpam) {
		this.moderateSpam = moderateSpam;
	}
	
	public void setJoinRole(String joinRole) {
		this.joinRole = joinRole;
	}
	
	public String getServerId() {
		return serverId;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public boolean doesDeleteCommands() {
		return deleteCommands;
	}
	
	public boolean doesReplyUnknown() {
		return replyUnknown;
	}
	
	public boolean isSaveRoles() {
		return saveRoles;
	}
	
	public boolean isModerateAdvertising() {
		return moderateAdvertising;
	}
	
	public boolean isModerateSpam() {
		return moderateSpam;
	}
	
	public List<String> getCommandChannels() {
		return commandChannels;
	}
	
	public List<String> getLockedChannels() {
		return lockedChannels;
	}
	
	public Map<String, String> getRoleMap() {
		return roleMap;
	}
	
	public String getVoiceChannelCategory() {
		return voiceChannelCategory;
	}
	
	public String getSuggestionChannel() {
		return suggestionChannel;
	}
	
	public String getFlagChannel() {
		return flagChannel;
	}
	
	public String getLogChannel() {
		return logChannel;
	}
	
	public String getDeletedChannel() {
		return deletedChannel;
	}
	
	public String getJoinRole() {
		return joinRole;
	}
	
	public Map<String, Integer> getInviteMap() {
		return inviteMap;
	}
	
}
