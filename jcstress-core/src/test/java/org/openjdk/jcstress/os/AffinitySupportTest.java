package org.openjdk.jcstress.os;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jcstress.vm.VMSupport;

public class AffinitySupportTest {

    @Before
    public void preconditions() {
        Assume.assumeTrue(VMSupport.isLinux());
    }

    @Test
    public void tryBind() {
        AffinitySupport.tryBind();
    }
}
