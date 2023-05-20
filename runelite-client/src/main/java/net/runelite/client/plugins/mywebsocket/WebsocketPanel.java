package net.runelite.client.plugins.mywebsocket;

import com.google.common.collect.Lists;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.myplugin.StateDataConfig;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class WebsocketPanel extends PluginPanel
{
    private final JTextArea dataEditor = new JTextArea();
    void init(StateDataConfig config)
    {
        getParent().setLayout(new BorderLayout());
        getParent().add(this, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        dataEditor.setTabSize(2);
        dataEditor.setLineWrap(true);
        dataEditor.setWrapStyleWord(true);

        JPanel notesContainer = new JPanel();
        notesContainer.setLayout(new BorderLayout());
        notesContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        dataEditor.setOpaque(false);

        // load note text
        String data = config.gameStateData();
        dataEditor.setText(data);

        notesContainer.add(dataEditor, BorderLayout.CENTER);
        notesContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

        add(notesContainer, BorderLayout.CENTER);
    }

    void setData(String data){dataEditor.setText(data);}
}