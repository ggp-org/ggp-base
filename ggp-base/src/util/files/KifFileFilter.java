package util.files;

import java.io.File;
import java.io.FileFilter;

public class KifFileFilter implements FileFilter {
    @Override
    public boolean accept(File pathname) {
        return pathname.getName().endsWith(".kif");
    }
}
