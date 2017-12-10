package com.shiznatix.lightclubs;

import com.shiznatix.lightclubs.entities.ScriptFrame;

import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.*;

public class ScriptFileParserInstrumentedTest {
    @Test
    public void getScriptFrames() {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("vezer.xml");
        ScriptFileParser scriptFileParser = new ScriptFileParser(inputStream);

        Map<String, ArrayList<ScriptFrame>> playlists = scriptFileParser.getPlaylists();

        assertEquals(4, playlists.size());

        scriptFileParser.closeStream();
    }
}