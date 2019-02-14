import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class RenameTester {
    @Test
    public void testRenameMethod(){
        // one side rename (or move too), match and merge
        // assert the rename matching number
        assertThat("aaa").contains("a");
    }
}
