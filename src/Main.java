/**
 * Runs preferences if no arguments, parser otherwise.
 * @author August Janse
 *
 */
public class Main {
	public static void main(String[] args) {
		if (args.length == 0) {
			new GUI();
		} else {
			Parser.main(null);
		}
	}
}
