package apps.kiosk;

import java.applet.Applet;
import util.configuration.LocalResourceLoader;
import util.ui.NativeUI;

/**
 * AppletKiosk is a version of the standard Kiosk that can run in an applet.
 * This is the first step towards having a fully web-based GGP infrastructure.
 * Future steps should be more focused on Javascript/HTML/CSS.
 * 
 * @author Sam
 */
@SuppressWarnings("serial")
public final class AppletKiosk extends Applet
{
    public void init() {
        setName("Gaming WebKiosk");
        NativeUI.setNativeUI();
    }
    
    public AppletKiosk() {
        add(new Kiosk());
        LocalResourceLoader.setLocalResourceLoader(this);
    }
}