package com.tek.guardian.commands;

import java.util.Arrays;
import java.util.Optional;

import com.tek.guardian.data.ServerProfile;
import com.tek.guardian.enums.BotRole;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

public class MuteCommand extends Command {

	public MuteCommand() {
		super("mute", Arrays.asList(), "<user> [reason]", "Mutes a member and specifies a reason.", true);
	}

	@Override
	public boolean call(JDA jda, ServerProfile profile, Member member, Guild guild, TextChannel channel, String label, String[] args) {
		if(args.length >= 1) {
			if(member.hasPermission(Permission.VOICE_MUTE_OTHERS)) {
				String reason;
				if(args.length >= 2) {
					StringBuilder reasonBuilder = new StringBuilder();
					for(int i = 1; i < args.length; i++) reasonBuilder.append(args[i] + " ");
					if(reasonBuilder.length() > 0) reasonBuilder.setLength(reasonBuilder.length() - 1);
					reason = reasonBuilder.toString();
				} else {
					reason = "No reason specified.";
				}
				
				Optional<Member> memberOpt = CommandHandler.fromString(guild, args[0]);
				
				if(memberOpt.isPresent()) {
					if(!memberOpt.get().equals(member) && member.canInteract(memberOpt.get())) {
						Role r = guild.getRoleById(profile.getRoleMap().get(BotRole.MUTED.name()));
						
						if(!memberOpt.get().getRoles().contains(r)) {
							memberOpt.get().getUser().openPrivateChannel().queue(pm -> {
								pm.sendMessage("You have been muted in the server **" + guild.getName() + "** for the reason: `" + reason + "`").queue(m -> {
									guild.addRoleToMember(memberOpt.get(), r).queue();
								}, e -> {
									guild.addRoleToMember(memberOpt.get(), r).queue();
								});
							}, e -> {
								guild.addRoleToMember(memberOpt.get(), r).queue();
							});
							
							channel.sendMessage("Successfully muted " + memberOpt.get().getUser().getAsMention() + ". `" + reason + "`").queue();
						} else {
							channel.sendMessage("**This person is already muted.**").queue();
						}
					} else {
						channel.sendMessage("**You cannot mute this person.**").queue();
					}
				} else {
					channel.sendMessage("**No member was found by the identifier** `" + args[0] + "`").queue();
				}
			} else {
				channel.sendMessage("**You cannot mute members.**").queue();
			}
			
			return true;
		} else {
			return false;
		}
	}

}
