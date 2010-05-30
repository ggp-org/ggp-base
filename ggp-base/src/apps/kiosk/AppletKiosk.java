package apps.kiosk;

import java.applet.Applet;
import apps.common.NativeUI;
import util.configuration.ResourceLoader;

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
        ResourceLoader.setLocalResourceLoader(this);
    }
}