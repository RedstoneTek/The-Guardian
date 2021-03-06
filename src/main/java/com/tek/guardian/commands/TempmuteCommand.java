package com.tek.guardian.commands;

import java.util.Arrays;
import java.util.Optional;

import com.tek.guardian.data.ServerProfile;
import com.tek.guardian.enums.BotRole;
import com.tek.guardian.main.Guardian;
import com.tek.guardian.main.Reference;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

public class TempmuteCommand extends Command {

	public TempmuteCommand() {
		super("tempmute", Arrays.asList("tmute"), "<user> <time> [reason]", "Mutes a member temporarily.", true);
	}

	@Override
	public boolean call(JDA jda, ServerProfile profile, Member member, Guild guild, TextChannel channel, String label, String[] args) {
		if(args.length >= 2) {
			if(member.hasPermission(Permission.VOICE_MUTE_OTHERS)) {
				String reason;
				if(args.length >= 3) {
					StringBuilder reasonBuilder = new StringBuilder();
					for(int i = 2; i < args.length; i++) reasonBuilder.append(args[i] + " ");
					if(reasonBuilder.length() > 0) reasonBuilder.setLength(reasonBuilder.length() - 1);
					reason = reasonBuilder.toString();
				} else {
					reason = "No reason specified.";
				}
				
				Optional<Member> memberOpt = Reference.memberFromString(guild, args[0]);
				
				if(memberOpt.isPresent()) {
					try {
						long time = Reference.timeToMillis(args[1]);
						
						if(!memberOpt.get().equals(member) && member.canInteract(memberOpt.get())) {
							Role r = guild.getRoleById(profile.getRoleMap().get(BotRole.MUTED.name()));
							
							if(!memberOpt.get().getRoles().contains(r)) {
								Guardian.getInstance().getActionManager().temporarilyMute(member, memberOpt.get(), profile, time, channel, reason);
							} else {
								channel.sendMessage(Reference.embedError(jda, "This person is already muted.")).queue();
							}
						} else {
							channel.sendMessage(Reference.embedError(jda, "You cannot mute this person.")).queue();
						}
					} catch(IllegalArgumentException e) {
						channel.sendMessage(Reference.embedError(jda, "Invalid time. Format: `time[s/m/h/d] Ex: 5h = 5 hours`.")).queue();
					}
				} else {
					channel.sendMessage(Reference.embedError(jda, "No member was found by the identifier `" + args[0] + "`.")).queue();
				}
			} else {
				channel.sendMessage(Reference.embedError(jda, "You cannot mute members.")).queue();
			}
			
			return true;
		} else {
			return false;
		}
	}

}
