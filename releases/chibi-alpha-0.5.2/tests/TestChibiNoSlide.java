import com.prodigytown.chibi.ChibiAnimationSupport;

public class TestChibiNoSlide {
    public static void main(String[] args) {
        int checked = 0;
        double[] speeds = {0, 0.00003, 0.000224, 0.000226, 0.001, 0.018, 0.04, 0.2};
        for (int variant = 0; variant < 4; variant++) {
            for (int tick = 0; tick < 1400; tick++) {
                for (double speed : speeds) {
                    if (ChibiAnimationSupport.choose(tick, variant, speed, 0, true) != 1)
                        throw new AssertionError("Moving must WALK");
                    if (ChibiAnimationSupport.choose(tick, variant, speed, 8, true) != 1)
                        throw new AssertionError("Hurt while moving must WALK");
                    if (speed > 0.000225 && ChibiAnimationSupport.choose(tick, variant, speed, 0, false) != 1)
                        throw new AssertionError("Horizontal speed must WALK");
                    checked++;
                }
            }
        }
        java.util.Set<Integer> idle = new java.util.TreeSet<>();
        for (int variant = 0; variant < 4; variant++) {
            for (int tick = 0; tick < 8000; tick++) {
                int choice = ChibiAnimationSupport.choose(tick, variant, 0, 0, false);
                if (choice == 1 || choice == 2)
                    throw new AssertionError("Stopped mob must not use locomotion");
                idle.add(choice);
            }
        }
        if (!idle.containsAll(java.util.Arrays.asList(0, 3, 4, 5, 6, 7, 8, 9, 10)))
            throw new AssertionError("Missing personality animation: " + idle);
        System.out.println("PASS " + checked + " locomotion cases, idle clips " + idle);
    }
}
