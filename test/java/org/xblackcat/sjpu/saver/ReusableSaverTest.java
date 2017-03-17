package org.xblackcat.sjpu.saver;

import org.junit.Assert;
import org.junit.Test;

import java.net.URI;

/**
 * 10.11.2016 14:57
 *
 * @author xBlackCat
 */
public class ReusableSaverTest {
    @Test
    public void windowsPathDetector() {
        Assert.assertFalse(SaverUtils.isWindowsRootPath(URI.create("file:///test/path").getPath()));
        Assert.assertTrue(SaverUtils.isWindowsRootPath(URI.create("file:///R:/test/path").getPath()));
        Assert.assertTrue(SaverUtils.isWindowsRootPath(URI.create("file:///R:mesa").getPath()));

        Assert.assertTrue(true);
    }
}