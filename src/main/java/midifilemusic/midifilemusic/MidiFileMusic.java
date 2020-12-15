package midifilemusic.midifilemusic;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.UUID;

public final class MidiFileMusic extends JavaPlugin {
    private static Plugin plugin;
    public static Plugin getPlugin(){
        return plugin;
    }
    public HashMap<UUID, BukkitRunnable> Music = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info(ChatColor.GREEN + "MidiFileMusicが起動しました");
        getDataFolder().mkdirs();
        plugin = this;
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.GREEN + "MidiFileMusicが停止しました");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("mfm")) {
            if(args.length < 1) {
                sender.sendMessage(ChatColor.RED + "/" + label + " [play/stop]");
                return true;
            }
            if(args[0].equalsIgnoreCase("play")) {
                if(args.length <= 3) {
                    sender.sendMessage(ChatColor.RED + "/" + label + " [play] [File名] [SoundID] [Volume]");
                    return true;
                }
                if(sender instanceof Player) {
                    Player p = (Player) sender;
                    File file = new File(getDataFolder() , args[1]);
                    Sequence sequence;
                    try {
                        sequence = MidiSystem.getSequence(file);
                    } catch (InvalidMidiDataException | IOException e) {
                        return true;
                    }
                    if(sequence.getDivisionType() == Sequence.PPQ) {
                        PriorityQueue<MidiNote> midinotes = new PriorityQueue<>();
                        int ppq = sequence.getResolution();
                        int ppt = 0;
                        for(Track track : sequence.getTracks()) {
                            for(int i = 0; i < track.size(); i++) {
                                MidiEvent midievent = track.get(i);
                                byte[] data = midievent.getMessage().getMessage();
                                if(Byte.toUnsignedInt(data[0]) == 0xff && Byte.toUnsignedInt(data[1]) == 0x51) {
                                    ppt = ppq * 50000 / ((Byte.toUnsignedInt(data[3]) << 16) + (Byte.toUnsignedInt(data[4]) << 8) + (Byte.toUnsignedInt(data[5])));
                                }
                                if((Byte.toUnsignedInt(data[0]) & 0xf0) == 0x90) {
                                    int tick = Math.round((float) midievent.getTick() / ppt);
                                    int note = Byte.toUnsignedInt(data[1]) - 54;
                                    midinotes.add(new MidiNote(tick , note , Float.parseFloat(args[3]) , Sound.valueOf(args[2])));
                                }
                            }
                        }
                        sender.sendMessage(ChatColor.GREEN + "音楽を再生しました");
                        if(Music.get(p.getUniqueId()) != null) {
                            Music.get(p.getUniqueId()).cancel();
                            Music.remove(p.getUniqueId());
                        }
                        BukkitRunnable BR = new BukkitRunnable() {
                            int tick = 0;
                            @Override
                            public void run() {
                                Location loc = p.getLocation();
                                while(midinotes.size() > 0 && midinotes.peek().tick == tick) {
                                    MidiNote midinote = midinotes.poll();
                                    float pitch = (float) Math.pow(2 , (double) (midinote.note - 12) / 12);
                                    p.playSound(loc , midinote.sound , midinote.volume , pitch);
                                }
                                if(midinotes.size() == 0) {
                                    if(Music.get(p.getUniqueId()) != null) {
                                        if(Music.get(p.getUniqueId()).equals(this)) {
                                            Music.remove(p.getUniqueId());
                                        }
                                    }
                                    cancel();
                                }
                                tick++;
                            }
                        };
                        BR.runTaskTimer(plugin , 0 , 1);
                        Music.put(p.getUniqueId() , BR);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーから実行してください");
                }
                return true;
            }
            if(args[0].equalsIgnoreCase("stop")) {
                if(sender instanceof Player) {
                    Player p = (Player) sender;
                    if(Music.get(p.getUniqueId()) == null) {
                        sender.sendMessage(ChatColor.RED + "あなたは曲を再生していません");
                        return true;
                    } else {
                        Music.get(p.getUniqueId()).cancel();
                        Music.remove(p.getUniqueId());
                        sender.sendMessage(ChatColor.GREEN + "曲の再生を止めました");
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーから実行してください");
                    return true;
                }
            }
        }
        sender.sendMessage(ChatColor.RED + "/" + label + " [play/reload] [File名]");
        return true;
    }


}
