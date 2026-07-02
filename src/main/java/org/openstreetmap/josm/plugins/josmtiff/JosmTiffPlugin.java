package org.openstreetmap.josm.plugins.josmtiff;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

public class JosmTiffPlugin extends Plugin {

    public JosmTiffPlugin(PluginInformation info) {
        super(info);
        MainMenu mainMenu = MainApplication.getMenu();
        if (mainMenu != null) {
            MainMenu.add(mainMenu.toolsMenu, new AboutAction());
        }
    }
}
