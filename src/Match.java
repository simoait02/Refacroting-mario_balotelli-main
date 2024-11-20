public class Match {
    public int eq1, eq2;

    public Match(int e1, int e2) {
        eq1 = e1;
        eq2 = e2;
    }

    public String toString() {
        if (eq1 < eq2) {
            return "  " + eq1 + " contre " + eq2;
        } else {
            return "  " + eq2 + " contre " + eq1;
        }
    }
}