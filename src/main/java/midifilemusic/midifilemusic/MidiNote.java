package midifilemusic.midifilemusic;

import org.bukkit.Sound;

public class MidiNote implements Comparable<MidiNote>{
    public final int tick;
    public final int note;
    public final float volume;
    public final Sound sound;

    public MidiNote(int tick , int note , float volume ,  Sound sound){
        this.tick = tick;
        this.note = note;
        this.sound = sound;
        this.volume = volume;
    }


    @Override
    public int compareTo(MidiNote other) {
        return Integer.compare(tick , other.tick);
    }
}
