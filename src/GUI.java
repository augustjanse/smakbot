import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * A graphical user interface that lets the user enter which product he or she wishes to order. The preferences are stored in plaintext.
 * @author August Janse
 *
 */
public class GUI {
	ArrayList<JTextField> fields = new ArrayList<>();

	public static void main(String[] args) {
		new GUI();
	}

	public GUI() {
		JFrame frame = new JFrame("Smakbot");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		Container pane = frame.getContentPane();
		pane.setLayout(new GridLayout(10, 1));

		pane.add(new JLabel(
				"Rangordna alla varor du vill ha med en siffra. Lämna resten av fälten tomma."));

		for (int i = 0; i < 8; i++) {
			JPanel panel = new JPanel();
			panel.add(new JLabel("Vara " + (i + 1)));
			JTextField field = new JTextField(1);
			panel.add(field);
			pane.add(panel);
			fields.add(field);
		}

		JButton saveButton = new JButton("Spara inställningar");
		saveButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					recordPreferences();
				} catch (IOException e1) {
					e1.printStackTrace();
					System.exit(1);
				}
				System.exit(0);
			}
		});
		pane.add(saveButton);
		
		try {
			readPreferences();
		} catch (IOException e1) {
			e1.printStackTrace();
			// proceed without old settings
		}

		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * Writes preferences to text file. Probably pretty ineffective, but no matter.
	 * @throws IOException
	 */
	private void recordPreferences() throws IOException {
		FileWriter writer = new FileWriter(new File("pref.txt"));

		for (JTextField field : fields) {
			writer.write(field.getText());
			writer.write("\n");
		}

		writer.close();
	}
	
	/**
	 * Fills the GUI with old settings.
	 * 
	 * @return
	 * @throws IOException
	 */
	private void readPreferences() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(
				"pref.txt")));

		int i = 0;
		String line = reader.readLine();
		while (line != null) {
			if (line.matches("\\d+")) {
				 fields.get(i).setText(line);
			}
			
			line = reader.readLine();
			i++;
		}

		reader.close();
	}
}
